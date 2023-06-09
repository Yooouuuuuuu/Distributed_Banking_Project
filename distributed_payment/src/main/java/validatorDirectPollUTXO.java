import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.subject.RecordNameStrategy;
import my.avroSchema.Block;
import my.avroSchema.LocalBalance;
import my.avroSchema.Transaction;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.simple.SimpleLogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class validator2 {
    static KafkaConsumer<String, Block> consumerFromBlocks;
    static KafkaConsumer<String, Block> consumerFromUTXO;
    static KafkaConsumer<String, Block> consumerFromUTXOOffset;
    static KafkaConsumer<String, LocalBalance> consumerFromLocalBalance;
    static KafkaProducer producer;
    static HashMap<String, Long> bankBalance = new HashMap<String, Long>();
    static HashMap<Integer, String> partitionBank = new HashMap<Integer, String>();
    static HashMap<Integer, Long> lastOffsetOfUTXO = new HashMap<>();
    static HashMap<String, Long> startTime = new HashMap<String, Long>();
    static boolean start = false;
    static long countForUpdateUTXO = 0;
    static long rejectedCount = 0;

    public static void main(String[] args) throws Exception {
/*
        need to test 2 version of UTXOs, one write the same currentBlock intoUTXO topic,
        while the other send UTXOs depends on its inbank.
        for the first case, partition don't need to be specified.
        maybe a version 3: very big UTXOs place in special partition(or topic)

        leaderEpoch

        If we want to poll from a specific topicPartition, we have to use assign function,
        however, the assign function ignores consumer groups and will not record the last offset for us.
        It isn't a problem if we are updating the state (and only poll the latest offset),
        but while polling from topic "aggUTXO", we need another topic to store the last offset of the topicPartition.

        Kafka transaction allows only single producer, while a producer can have only one serializer.
        However,avro is a flexible type that we can define multiple avro schema for different use should be well.
 */

        //inputs
        String bootstrapServers = args[0];
        String schemaRegistryUrl = args[1];
        int numOfPartitions = Integer.parseInt(args[2]);
        int numOfAccounts = Integer.parseInt(args[3]);
        short numOfReplicationFactor = Short.parseShort(args[4]);
        long initBalance = Long.parseLong(args[5]);
        int maxPoll = Integer.parseInt(args[6]);
        int blockSize = Integer.parseInt(args[7]);
        long blockTimeout = Long.parseLong(args[8]); //aggregator only
        long aggUTXOTime = Long.parseLong(args[9]); //sumUTXO only
        long numOfData = Long.parseLong(args[10]); //sourceProducer only
        long amountPerTransaction = Long.parseLong(args[11]); //sourceProducer only
        long UTXOUpdatePeriod = Long.parseLong(args[12]); //validator only
        int UTXOUpdateBreakTime = Integer.parseInt(args[13]); //validator only
        boolean successfulMultiplePartition = Boolean.parseBoolean(args[14]);
        boolean UTXODoNotAgg = Boolean.parseBoolean(args[15]);
        boolean randomAmount = Boolean.parseBoolean(args[16]);

        /*
        String bootstrapServers = "127.0.0.1:9092";
        String schemaRegistryUrl = "http://127.0.0.1:8081";
        int numOfPartitions = 3;
        int maxPoll = 500;
        long UTXOUpdatePeriod = 10000;
        int UTXOUpdateBreakTime = 1000;
        boolean randomUpdate = true;
        */

        //setups
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "off");//"off", "trace", "debug", "info", "warn", "error"
        InitConsumer(maxPoll, bootstrapServers, schemaRegistryUrl);
        InitProducer(bootstrapServers, schemaRegistryUrl);
        Logger logger = LoggerFactory.getLogger(validator2.class);
        producer.initTransactions();

        for (int i = 0; i < numOfPartitions; i++) {
            startTime.put("10" + i, System.currentTimeMillis());
        }

        //poll from "blocks" topic
        while (true) {
            ConsumerRecords<String, Block> records = consumerFromBlocks.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, Block> record : records) {
                //logger.info(record.value().toString());
                //Start atomically transactional write. One block per transactional write.
                producer.beginTransaction();
                //As outbank, withdraw money and create UTXO for inbank. Category 0 means it is a raw transaction.
                if (record.value().getTransactions().get(0).getCategory() == 0) {
                    ProcessBlocks(record.value(), UTXOUpdatePeriod, UTXOUpdateBreakTime, successfulMultiplePartition);
                    //Metadata for processing UpdateUTXO is ready, so set the flag to true.
                    start = true;
                    //System.out.println(bankBalance);
                } else if (record.value().getTransactions().get(0).getCategory() == 2) {
                    //Category 2 means it is an initialize record for accounts' balance. Only do once when system start.
                    InitBank(record.value(), record);
                }
                try {
                    consumerFromBlocks.commitSync();
                    producer.commitTransaction();
                } catch (Exception e) {
                    //If transactional write aborted,
                    //1. Consumer group do not commit offsets, so we can start after the last transactional write later.
                    //2. Hashmaps changes are not abandoned along with transactional write abandoned, thus reset manually.
                    //3. Followed by the hashmap abandoned, "UpdateUTXO" is not functional, thus reset the flag "start".
                    producer.abortTransaction();
                    bankBalance = new HashMap<String, Long>();
                    partitionBank = new HashMap<>();
                    startTime = new HashMap<>();
                    lastOffsetOfUTXO = new HashMap<>();
                    start = false;
                    System.out.println("Tx aborted. Reset hashmaps.");
                }
            }
            if (start) {
                //Check is any partition needs to update UTXO.
                for (int updatePartition = 0; updatePartition < numOfPartitions; updatePartition ++) {

                    UpdateUTXO(UTXOUpdatePeriod, UTXOUpdateBreakTime, updatePartition, false);
                }

                /*
                if (randomUpdate) {
                    int updatePartition = ThreadLocalRandom.current().nextInt(0, numOfPartitions);
                    UpdateUTXO(UTXOUpdatePeriod, UTXOUpdateBreakTime, updatePartition, false);
                } else {
                    int updatePartition = (int) (countForUpdateUTXO % numOfPartitions);
                    countForUpdateUTXO += 1;
                    UpdateUTXO(UTXOUpdatePeriod, UTXOUpdateBreakTime, updatePartition, false);
                }

                 */

                //Ideally, I want to check every partition every round. Since mostly we only need to check individual
                //"last update time" rather than real update(poll) and the loading is not too big. However, consumer
                //need a while between each poll, thus if we use a for loop to update every partition, some partition
                //will always try to poll while consumer is unable to poll. For example, the for loop try partitions
                // 0 to 2 (for (int updatePartition = 0; updatePartition < 3; updatePartition++)), and we find out that
                //partition 1 and 2 are never update.
            }
        }
    }

    private static void InitConsumer(int maxPoll, String bootstrapServers, String schemaRegistryUrl) {
        //consumer consume from "blocks" topic
        Properties propsConsumerTx = new Properties();
        propsConsumerTx.put("bootstrap.servers", bootstrapServers);
        propsConsumerTx.put("group.id", "validator-group");
        propsConsumerTx.put("auto.offset.reset", "earliest");
        propsConsumerTx.put("enable.auto.commit", "false");
        propsConsumerTx.put("isolation.level", "read_committed");
        propsConsumerTx.put("max.poll.records", maxPoll);
        //avro part
        propsConsumerTx.setProperty("key.deserializer", StringDeserializer.class.getName());
        propsConsumerTx.setProperty("value.deserializer", KafkaAvroDeserializer.class.getName());
        propsConsumerTx.setProperty("schema.registry.url", schemaRegistryUrl);
        propsConsumerTx.setProperty("specific.avro.reader", "true");

        String input_topic = "blocks";
        consumerFromBlocks =
                new KafkaConsumer<String, Block>(propsConsumerTx);
        consumerFromBlocks.subscribe(Collections.singletonList(input_topic),
                new ConsumerRebalanceListener() {
                    @Override
                    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                        //System.out.println("onPartitionsRevoked");
                    }
                    @Override
                    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                        //reset hashmaps and flag
                        bankBalance = new HashMap<String, Long>();
                        partitionBank = new HashMap<>();
                        startTime = new HashMap<>();
                        lastOffsetOfUTXO = new HashMap<>();
                        start = false;
                        System.out.println("validator is rebalanced. Reset hashmaps.");
                    }});

        // the three consumers below using the same property
        Properties propsConsumerAssign = new Properties();
        propsConsumerAssign.put("bootstrap.servers", bootstrapServers);
        propsConsumerAssign.put("isolation.level", "read_committed");
        propsConsumerAssign.put("enable.auto.commit", "false");
        propsConsumerAssign.setProperty("key.deserializer", StringDeserializer.class.getName());
        propsConsumerAssign.setProperty("value.deserializer", KafkaAvroDeserializer.class.getName());
        propsConsumerAssign.setProperty("schema.registry.url", schemaRegistryUrl);
        propsConsumerAssign.setProperty("specific.avro.reader", "true");
        //consumer consume from "aggUTXO" topic
        consumerFromUTXO =
                new KafkaConsumer<String, Block>(propsConsumerAssign);
        // We want to poll the partitions (banks) if and only if they are relevant to this validator,
        // however it is impossible if using "subscribe".
        // "Assign" can poll the specific topicPartition thus do not "subscribe" here,
        // but the assign function ignores the consumer group and do not commit the offset,
        // so we need to save the last poll offset manually to UTXOOffset.

        //consumer consume from "aggUTXOOffset" topic
        consumerFromUTXOOffset =
                new KafkaConsumer<String, Block>(propsConsumerAssign);

        //consumer consume from "localBalance" topic
        consumerFromLocalBalance =
                new KafkaConsumer<String, LocalBalance>(propsConsumerAssign);
    }

    private static void InitProducer(String bootstrapServers, String schemaRegistryUrl) {
        Properties propsProducer = new Properties();
        propsProducer.put("bootstrap.servers", bootstrapServers);
        propsProducer.put("transactional.id", randomString());
        propsProducer.put("transaction.timeout.ms", 300000);
        propsProducer.put("enable.idempotence", "true");
        propsProducer.put("max.block.ms", "1000");
        // avro part
        propsProducer.setProperty("key.serializer", StringSerializer.class.getName());
        propsProducer.setProperty("value.serializer", KafkaAvroSerializer.class.getName());
        propsProducer.setProperty("schema.registry.url", schemaRegistryUrl);
        propsProducer.put("value.subject.name.strategy", RecordNameStrategy.class.getName());
        producer = new KafkaProducer<>(propsProducer);
    }

    private static void InitBank(Block recordValue, ConsumerRecord<String, Block> record) throws ExecutionException, InterruptedException {
        for (int i = 0; i < recordValue.getTransactions().size(); i++) {
            partitionBank.put(record.partition(), record.key());
            startTime.put(record.key(), System.currentTimeMillis());
            bankBalance.put(recordValue.getTransactions().get(i).getOutAccount(),
                    recordValue.getTransactions().get(i).getAmount());

            lastOffsetOfUTXO = new HashMap<>();
            start = false;
            // Since we want to save every change of state of local balance,
            // writing initialized data to "localBalance" topic is obvious,
            // however, writing whole block, like the other two "producer send" below is not a good choice.

            // In the first version of our system, we write the whole block while initialized and update every account
            // of the bank in every kafka transaction, but only the balance of account in the block is changed,
            // meaning for example, 10000 account in the bank with block size 500, 9500 updates is redundant.

            // In order to reduce validators' loading while every partition is responsible for many account
            // (lets say a bank has ten thousand account, which is not crazy at all),
            // we want to update only the relevant accounts of the block every kafka transaction
            // by changing the key to account number and update individually.
            // To follow the new strategy, we should write every balance to "localBalance" topic individually.
            // Though we can still use the old way (writing the whole block) as a snapshot always at offset 0,
            // the new approach cooperates with cleanup.policy better and makes consumer group rebalance lighter.

            LocalBalance initBalance =
                    new LocalBalance(bankBalance.get(recordValue.getTransactions().get(i).getOutAccount()));
            producer.send(new ProducerRecord<String, LocalBalance>("localBalance",
                    recordValue.getTransactions().get(0).getOutbankPartition(),
                    recordValue.getTransactions().get(i).getOutAccount(),
                    initBalance));
        }
        producer.send(new ProducerRecord<String, Block>("successful", recordValue));

        System.out.println("Initialized. Now in charge of: " + partitionBank + " (partition:bank)");
    }

    private static void ProcessBlocks(Block recordValue, long updatePeriod, int updateBreakTime,
                                      boolean successfulMultiplePartition)
            throws ExecutionException, IOException, InterruptedException {
        //initialize block
        Block currentBlock = recordValue;
        Block rejectedBlock;
        List<Transaction> listOfRejected = new ArrayList<Transaction>();
        boolean rejected = false;
        List<Integer> listOfRejectedIndex = new ArrayList<>();

        //validate transactions
        for (int i = 0; i < recordValue.getTransactions().size(); i++) {
            //check if bankBalance exist
            if (!bankBalance.containsKey(recordValue.getTransactions().get(i).getOutAccount())) {
                PollFromLocalBalance(recordValue.getTransactions().get(i).getOutbankPartition(),
                        recordValue.getTransactions().get(i).getOutbank());
            }

            //If the out account have enough money, pay for it.
            if (bankBalance.get(recordValue.getTransactions().get(i).getOutAccount())
                    >= recordValue.getTransactions().get(i).getAmount()) {
                long withdraw = currentBlock.getTransactions().get(i).getAmount();
                bankBalance.compute(recordValue.getTransactions().get(i).getOutAccount(), (key, value)
                        -> value - withdraw);

                // update "localBalance" topic
                LocalBalance newBalance =
                        new LocalBalance(bankBalance.get(recordValue.getTransactions().get(i).getOutAccount()));
                producer.send(new ProducerRecord<String, LocalBalance>("localBalance",
                        recordValue.getTransactions().get(0).getOutbankPartition(),
                        recordValue.getTransactions().get(i).getOutAccount(),
                        newBalance));

                //send UTXO every after transaction (if UTXODoNotAgg = true)
                //build block
                Transaction UTXOdetail = recordValue.getTransactions().get(i);
                List<Transaction> listOfUTXODetail = new ArrayList<Transaction>();
                listOfUTXODetail.add(UTXOdetail);
                Block UTXOBlock = Block.newBuilder()
                        .setTransactions(listOfUTXODetail)
                        .build();
                //send
                producer.send(new ProducerRecord<String, Block>("UTXO",
                        UTXOBlock.getTransactions().get(0).getInbankPartition(),
                        UTXOBlock.getTransactions().get(0).getInbank(),
                        UTXOBlock));

            } else {
                //Else check the UTXO first. If still not enough, we reject the transaction.
                UpdateUTXO(updatePeriod, updateBreakTime,
                        recordValue.getTransactions().get(0).getOutbankPartition(), true);

                if (bankBalance.get(recordValue.getTransactions().get(i).getOutAccount())
                        >= recordValue.getTransactions().get(i).getAmount()) {
                    long withdraw = currentBlock.getTransactions().get(i).getAmount();
                    bankBalance.compute(recordValue.getTransactions().get(i).getOutAccount(), (key, value)
                            -> value - withdraw);

                    // update "localBalance" topic
                    LocalBalance newBalance =
                            new LocalBalance(bankBalance.get(recordValue.getTransactions().get(i).getOutAccount()));
                    producer.send(new ProducerRecord<String, LocalBalance>("localBalance",
                            recordValue.getTransactions().get(0).getOutbankPartition(),
                            recordValue.getTransactions().get(i).getOutAccount(),
                            newBalance));

                    //send UTXO every after transaction (if UTXODoNotAgg = true)
                    //build block
                    Transaction UTXOdetail = recordValue.getTransactions().get(i);
                    List<Transaction> listOfUTXODetail = new ArrayList<Transaction>();
                    listOfUTXODetail.add(UTXOdetail);
                    Block UTXOBlock = Block.newBuilder()
                            .setTransactions(listOfUTXODetail)
                            .build();
                    //send
                    producer.send(new ProducerRecord<String, Block>("UTXO",
                            UTXOBlock.getTransactions().get(0).getInbankPartition(),
                            UTXOBlock.getTransactions().get(0).getInbank(),
                            UTXOBlock));
                } else {
                    // If balance is still not enough, reject the TX.
                    rejected = true;
                    Transaction rejectedTx = recordValue.getTransactions().get(i);
                    listOfRejected.add(rejectedTx);
                    listOfRejectedIndex.add(i);
                    rejectedCount += 1;
                    System.out.printf("Transaction No.%d cancelled.%n " + rejectedCount + " rejected.\n"
                            , recordValue.getTransactions().get(i).getSerialNumber());
                }
            }
        }

        //send blocks to kafka topics
        //1. If any transaction is rejected, send a block to "rejected" topic.
        //2. Send all successful transactions in the same block to "successful" topic.
        //3. Rejected block size may be more than one; and successful block may be less than raw block.
        if (rejected) {
            for (int i = 1; i <= listOfRejectedIndex.size(); i++) {
                currentBlock.getTransactions().remove((int)listOfRejectedIndex.get(listOfRejectedIndex.size() - i));
                //The casting to int is important thus list's ".remove" function is same for index or object.
            }
                rejectedBlock = Block.newBuilder().setTransactions(listOfRejected).build();
                producer.send(new ProducerRecord<String, Block>("rejected", rejectedBlock));
        }
        //4. "successful" is single partition for serialization
        if (!successfulMultiplePartition) {
            producer.send(new ProducerRecord<String, Block>("successful", currentBlock));
        } else {
            producer.send(new ProducerRecord<String, Block>("successful",
                    currentBlock.getTransactions().get(0).getOutbankPartition(),
                    currentBlock.getTransactions().get(0).getOutbank(),currentBlock));
        }
    }

    private static void UpdateUTXO(long updatePeriod, int updateBreakTime, int updatePartition, boolean inTransaction) throws InterruptedException {
        //reset flag
        boolean update = false;

        //setting heartbeat is also a choice?
        //int maxHeartbeat = 200; //ms, if heartbeat is greater than maxHeartbeat, means nothing waiting to be polled.

        //check if the partition is taken charge by this consumer
        label:
        if (partitionBank.containsKey(updatePartition)) {

            //check if this bank (partition) need to update (hasn't updated UTXO for a long time)
            if (!inTransaction &&
                    System.currentTimeMillis() - startTime.get(partitionBank.get(updatePartition)) <= updatePeriod) {
                break label;
            }

            //poll last read offset if reset
            if (!lastOffsetOfUTXO.containsKey(updatePartition)) {
                //If lastOffsetOfAggUTXO is init, set to -1:
                //1. If latestOffset of consumerFromUTXOOffset is larger than 0, means this is not the first
                //consumer process for bank(i) or being rebalanced. Use the value gain from "UTXOOffset" topic.
                //2. However, if "UTXOOffset" topic is empty or only contains some unreadable records,
                //we will start "aggUTXO" at "offset = lastOffsetOfAggUTXO + 1 = 0", thus set to -1.
                lastOffsetOfUTXO.put(updatePartition, -1L);

                TopicPartition topicPartition =
                        new TopicPartition("UTXO", updatePartition);
                consumerFromUTXOOffset.assign(Collections.singletonList(topicPartition));
                consumerFromUTXOOffset.seekToEnd(Collections.singleton(topicPartition));
                long latestOffset = consumerFromUTXOOffset.position(topicPartition);

                if (latestOffset > 0) {
                    outerLoop:
                    while (true) {
                        consumerFromUTXOOffset.seek(topicPartition, latestOffset);
                        ConsumerRecords<String, Block> offsetRecords =
                                consumerFromUTXOOffset.poll(Duration.ofMillis(100));
                        for (ConsumerRecord<String, Block> offsetRecord : offsetRecords) {
                            lastOffsetOfUTXO.put(updatePartition,
                                    offsetRecord.value().getTransactions().get(0).getAmount());
                            //break once poll anything
                            break outerLoop;
                        }
                        //latestOffset might be unreadable, thus check latestOffset -1
                        latestOffset -= 1;
                        if (latestOffset < 0) {
                            break;
                        }
                    }
                    System.out.println("Poll from aggUTXOOffset: " + lastOffsetOfUTXO + " (partition:offset)");
                }
            }

            //consumer assign to partition of "UTXO"
            TopicPartition topicPartition =
                    new TopicPartition("UTXO", updatePartition);
            consumerFromUTXO.assign(Collections.singletonList(topicPartition));

            //find the latest offset, so we know when to break the while loop
            long latestOffset = 0;
            consumerFromUTXO.seekToEnd(Collections.singleton(topicPartition));
            latestOffset = consumerFromUTXO.position(topicPartition);


            //Check if poll in needed. If not checked, we will lose poll duration time (timeout of poll)
            if (latestOffset >= lastOffsetOfUTXO.get(updatePartition)) {

                //start transactional write (need to be outside the while loop)
                if (!inTransaction) {
                    producer.beginTransaction();
                }

                //seek the last read offset
                consumerFromUTXO.seek(topicPartition, lastOffsetOfUTXO.get(updatePartition) + 1);

                outerLoop:
                while (true) {
                    //We will assign multiple partition in a very short time and timeout followed by the
                    // poll duration. So the poll duration must be bigger than normal.
                    ConsumerRecords<String, Block> UTXORecords = consumerFromUTXO.poll(Duration.ofMillis(60000));
                    for (ConsumerRecord<String, Block> UTXORecord : UTXORecords) {
                        //reset heartbeat since continuous polling
                        for (int j = 0; j < UTXORecord.value().getTransactions().size(); j++) {
                            //add UTXO to account
                            long amount = UTXORecord.value().getTransactions().get(j).getAmount();
                            bankBalance.compute(UTXORecord.value().getTransactions().get(j).getInAccount(),
                                    (key, value) -> value + amount);
                            // update "localBalance" topic
                            LocalBalance newBalance =
                                    new LocalBalance(bankBalance.get(UTXORecord.value().getTransactions().get(j).getOutAccount()));
                            producer.send(new ProducerRecord<>("localBalance",
                                    UTXORecord.value().getTransactions().get(j).getOutbankPartition(),
                                    UTXORecord.value().getTransactions().get(j).getOutAccount(),
                                    newBalance));
                        }

                        //build block of offset
                        lastOffsetOfUTXO.put(updatePartition, UTXORecord.offset());
                        String bank = partitionBank.get(updatePartition);
                        Transaction detail = new Transaction(-1L,
                                bank, bank, bank, bank,
                                updatePartition, updatePartition,
                                lastOffsetOfUTXO.get(updatePartition), 3);
                        List<Transaction> listOfDetail = new ArrayList<Transaction>();
                        listOfDetail.add(detail);
                        Block offsetBlock = Block.newBuilder()
                                .setTransactions(listOfDetail)
                                .build();

                        //update "aggUTXOOffset" topic
                        producer.send(new ProducerRecord<>("aggUTXOOffset",
                                updatePartition,
                                partitionBank.get(updatePartition),
                                offsetBlock));

                        //flag shows the consumer did poll something from "aggUTXO"
                        update = true;
                        //reset its last update time
                        startTime.put(partitionBank.get(updatePartition), System.currentTimeMillis());

                        //the last UTXO is polled, thus break
                        if (UTXORecord.offset() >= latestOffset - 2) {
                            break outerLoop;
                        }
                    }

                    //For regular updates, if already wasting too much time, break the while loop.
                    if (!inTransaction ||
                            startTime.get(partitionBank.get(updatePartition)) > updateBreakTime) {
                        break;
                    }

                    //Though waiting for consumer to poll until last record may cost a lot of time,
                    //this is crucial to separate two ways of break between the "inTransaction" flag for serialization.
                    //For the property of serialization, we must make sure the accounts lack of money update
                    //"every UTXO the transaction" before reject the transaction, thus we use seekToEnd find the latest
                    //offset. On the other hand, without the "inTransaction" flag, means it's just a regular update.
                }
                //transactional write part
                if (!inTransaction) {
                    try {
                        producer.commitTransaction();
                        if (update) {
                            System.out.println("UTXO updated: " + partitionBank.get(updatePartition) + "\n" + bankBalance);
                        }
                    } catch (Exception e) {
                        producer.abortTransaction();
                        System.out.println("UTXO update failed.");
                    }
                }
            }
        }
    }


    private static void PollFromLocalBalance(int outbankPartition, String bankID) {
        //In v3, we change the strategy of saving state in "localBalance" topic.
        //We saved every account's balance of the bank in every record, while it is mostly redundant,
        //we tried to save one account's balance per record in v3.

        //Followed the new strategy, we have to poll "localBalance" topic from beginning to end rather than latest only.
        //"cleanup.policy = compact" is used to lessen the record we need to poll from beginning to end

        //consumer assign to specific topicPartition
        TopicPartition topicPartition =
                new TopicPartition("localBalance", outbankPartition);
        consumerFromLocalBalance.assign(Collections.singletonList(topicPartition));

        //find the latest offset, so we know when to break the while loop
        consumerFromLocalBalance.seekToEnd(Collections.singleton(topicPartition));
        long latestOffset = consumerFromLocalBalance.position(topicPartition);

        //poll data of specific topic partition from beginning to end
        consumerFromLocalBalance.seek(topicPartition, 0);
        outerLoop:
        while (true) {
            ConsumerRecords<String, LocalBalance> balanceRecords = consumerFromLocalBalance.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, LocalBalance> balanceRecord : balanceRecords) {
                bankBalance.compute(balanceRecord.key(), (key, value)
                        -> balanceRecord.value().getBalance());
                //break when poll to end, while -2 for some reason, maybe the marker
                if (balanceRecord.offset() == latestOffset - 2) {
                    break outerLoop;
                }
            }
        }

        //set other hashmaps (has been reset)
        startTime.put(bankID, System.currentTimeMillis());
        partitionBank.put(outbankPartition, bankID);
        System.out.println("Poll from localBalance. Now in charge of: " + partitionBank + " (partition:bank)");
    }

    public static String randomString() {
        byte[] array = new byte[32]; // length is bounded by 32
        new Random().nextBytes(array);
        return new String(array, StandardCharsets.UTF_8);
    }
}


