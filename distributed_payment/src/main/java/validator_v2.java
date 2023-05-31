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
import org.apache.kafka.clients.producer.Producer;
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

public class validator_v2 {
    static KafkaConsumer<String, Block> consumerFromBlocks;
    static KafkaConsumer<String, Block> consumerFromLocalBalance;
    static KafkaConsumer<String, Block> consumerFromUTXO;
    static KafkaConsumer<String, Block> consumerFromUTXOOffset;
    static KafkaProducer producer;
    static HashMap<String, Long> bankBalance = new HashMap<String, Long>();
    static HashMap<Integer, String> partitionBank = new HashMap<Integer, String>();
    static HashMap<Integer, Long> lastOffsetOfAggUTXO = new HashMap<>();
    static HashMap<String, Long> startTime = new HashMap<String, Long>();
    static boolean start = false;

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

        /*
        args[0]: # of partitions
        args[1]: # of transactions
        args[2]: "max.poll.records"
        args[3]: bootstrap.servers

        int numOfData = Integer.parseInt(args[1]);
        */

        //inputs
        String bootstrapServers = "127.0.0.1:9092";
        String schemaRegistryUrl = "http://127.0.0.1:8081";
        int numOfPartitions = 3;
        int maxPoll = 500;
        int blockSize = 500;
        int numOfData = 100;
        long updatePeriod = 5000;
        int numOfAccount = 10;
        int updateBreakTime = 10000;

        //setups
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "off");//"off", "trace", "debug", "info", "warn", "error"
        InitConsumer(maxPoll, bootstrapServers, schemaRegistryUrl);
        InitProducer(bootstrapServers, schemaRegistryUrl);
        Logger logger = LoggerFactory.getLogger(validator_v2.class);
        producer.initTransactions();

        //poll from "blocks" topic
        while (true) {
            ConsumerRecords<String, Block> records = consumerFromBlocks.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, Block> record : records) {
                logger.info(record.value().toString());
                //Start atomically transactional write. One block per transactional write.
                producer.beginTransaction();
                //As outbank, withdraw money and create UTXO for inbank. Category 0 means it is a raw transaction.
                if (record.value().getTransactions().get(0).getCategory() == 0) {
                    ProcessBlocks(record.value(), numOfPartitions, numOfAccount);
                    //Metadata for processing UpdateUTXO is ready, so set the flag to true.
                    start = true;
                    System.out.println(bankBalance);
                } else if (record.value().getTransactions().get(0).getCategory() == 2) {
                    //Category 2 means it is a initialize record for accounts' balance. Only do once when system start.
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
                    lastOffsetOfAggUTXO = new HashMap<>();
                    start = false;
                    System.out.println("Tx aborted. Reset hashmaps.");
                }
            }
            if (start) {
                //checked after every poll, update UTXO to accounts in a fixed time.
                UpdateUTXO(updatePeriod, numOfPartitions, updateBreakTime, numOfAccount);
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
                        lastOffsetOfAggUTXO = new HashMap<>();
                        start = false;
                        System.out.println("Rebalanced. Reset hashmaps.");
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
                new KafkaConsumer<String, Block>(propsConsumerAssign);
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

    private static void InitBank(Block recordValue, ConsumerRecord record) throws ExecutionException, InterruptedException {
        for (int i = 0; i < recordValue.getTransactions().size(); i++) {
            partitionBank.put(record.partition(), (String) record.key());
            startTime.put((String) record.key(), System.currentTimeMillis());
            bankBalance.put(recordValue.getTransactions().get(i).getOutAccount(),
                    recordValue.getTransactions().get(i).getAmount());

            LocalBalance initBalance =
                    new LocalBalance(bankBalance.get(recordValue.getTransactions().get(i).getOutAccount()));

            producer.send(new ProducerRecord<String, LocalBalance>("localBalance",
                    recordValue.getTransactions().get(0).getInbankPartition(),
                    recordValue.getTransactions().get(i).getOutAccount(),
                    initBalance));


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
        }
        producer.send(new ProducerRecord<String, Block>("successful",
                recordValue.getTransactions().get(0).getInbankPartition(),
                recordValue.getTransactions().get(0).getInbank(),
                recordValue));
        producer.send(new ProducerRecord<String, Block>("aggUTXOOffset",
                recordValue.getTransactions().get(0).getOutbankPartition(),
                recordValue.getTransactions().get(0).getOutbank(),
                recordValue));

        System.out.println("Initialized. Now in charge of: " + partitionBank + " (partition:bank)");
    }


    private static void ProcessBlocks(Block recordValue, int numOfPartitions, int numOfAccount) throws ExecutionException, IOException, InterruptedException {
        //initialize block
        Block currentBlock = recordValue;
        Block rejectedBlock;
        List<Transaction> listOfRejected = new ArrayList<Transaction>();
        boolean rejected = false;

        //validate transactions
        for (int i = 0; i < recordValue.getTransactions().size(); i++) {
            //check if bankBalance exist
            if (!bankBalance.containsKey(recordValue.getTransactions().get(i).getOutAccount())) {
                PollFromLocalBalance(recordValue);
            }
            //Ff the out account have enough money, pay for it.
            if (bankBalance.get(recordValue.getTransactions().get(i).getOutAccount())
                    >= recordValue.getTransactions().get(i).getAmount()) {
                long withdraw = currentBlock.getTransactions().get(i).getAmount();
                bankBalance.compute(recordValue.getTransactions().get(i).getOutAccount(), (key, value)
                        -> value - withdraw);
                producer.send(new ProducerRecord<String, Block>("localBalance",
                        currentBlock.getTransactions().get(0).getOutbankPartition(),
                        currentBlock.getTransactions().get(0).getOutbank(),
                        newBalance(recordValue, numOfAccount)));
            } else {
                //Else check the UTXO first. If still not enough, we reject the transaction.
                PollUTXO(recordValue);
                if (bankBalance.get(recordValue.getTransactions().get(i).getOutAccount())
                        >= recordValue.getTransactions().get(i).getAmount()) {
                    long withdraw = currentBlock.getTransactions().get(i).getAmount();
                    bankBalance.compute(recordValue.getTransactions().get(i).getOutAccount(), (key, value)
                            -> value - withdraw);
                    producer.send(new ProducerRecord<String, Block>("localBalance",
                            currentBlock.getTransactions().get(0).getOutbankPartition(),
                            currentBlock.getTransactions().get(0).getOutbank(),
                            newBalance(recordValue, numOfAccount)));
                } else {
                    // If balance is still not enough, reject the TX.
                    rejected = true;
                    Transaction rejectedTx = recordValue.getTransactions().get(i);
                    listOfRejected.add(rejectedTx);

                    currentBlock.getTransactions().remove(i);
                    System.out.println("Transaction No." +
                            recordValue.getTransactions().get(i).getSerialNumber() + " cancelled.");
                }
            }
        }

        //send blocks to kafka topics
        //1. If any transaction is rejected, send a block to "rejected" topic.
        //2. Send all successful transactions in the same block to "successful" topic.
        //3. Rejected block size may be more than one; and successful block may be less than raw block.
        if (rejected) {
            rejectedBlock = Block.newBuilder().setTransactions(listOfRejected).build();
            producer.send(new ProducerRecord<String, Block>("rejected",
                    currentBlock.getTransactions().get(0).getOutbankPartition(),
                    currentBlock.getTransactions().get(0).getOutbank(),
                    rejectedBlock));
        }
        producer.send(new ProducerRecord<String, Block>("successful",
                currentBlock.getTransactions().get(0).getOutbankPartition(),
                currentBlock.getTransactions().get(0).getOutbank(),
                currentBlock));

        //4. successful block send to "UTXO" topic since for the system, transactions are not complete.
        //do not assign to specific to partition nor set key for kafka to do round-robin
        producer.send(new ProducerRecord<String, Block>("UTXO", currentBlock));

        //5. local balance is updated after every transaction is confirmed.



    }

    private static void UpdateUTXO(long updatePeriod, int numOfPartitions, int updateBreakTime, int numOfAccount) {
        boolean update = false;

        //check every partition
        for (int i = 0; i < numOfPartitions; i++) {
            //check if the partition is taken charge by this consumer
            if (partitionBank.containsKey(i)) {

                //check if this bank (partition) hasn't updated UTXO for a long time
                if (System.currentTimeMillis() - startTime.get(partitionBank.get(i)) > updatePeriod) {
                    //init, and start counting update time
                    startTime.put(partitionBank.get(i), System.currentTimeMillis());
                    long heartbeat = System.currentTimeMillis();

                    //poll last read offset if reset
                    if (!lastOffsetOfAggUTXO.containsKey(i)) {
                        //if latest record is init, set to -1
                        lastOffsetOfAggUTXO.put(i, -1L);
                        TopicPartition topicPartition =
                                new TopicPartition("aggUTXOOffset", i);
                        consumerFromUTXOOffset.assign(Arrays.asList(topicPartition));
                        consumerFromUTXOOffset.seekToEnd(Collections.singleton(topicPartition));
                        long latestOffset = consumerFromUTXOOffset.position(topicPartition);
                        boolean findingLast = true;
                        while (findingLast) {
                            consumerFromUTXOOffset.seek(topicPartition, latestOffset);
                            latestOffset -= 1;
                            ConsumerRecords<String, Block> offsetRecords = consumerFromUTXOOffset.poll(Duration.ofMillis(100));
                            for (ConsumerRecord<String, Block> offsetRecord : offsetRecords) {
                                if (offsetRecord.value().getTransactions().get(0).getCategory() != 2) {
                                    lastOffsetOfAggUTXO.put(i, offsetRecord.value().getTransactions().get(0).getAmount());
                                }
                            }
                            findingLast = false;
                            System.out.println("Poll from aggUTXOOffset: " + lastOffsetOfAggUTXO + " (partition:offset)");
                        }
                    }

                    //consumer assign to specific topicPartition and seek for last read offset
                    TopicPartition topicPartition =
                            new TopicPartition("aggUTXO", i);
                    consumerFromUTXO.assign(Arrays.asList(topicPartition));
                    consumerFromUTXO.seek(topicPartition, lastOffsetOfAggUTXO.get(i) + 1);

                    //start transactional write (need to be outside the while loop)
                    producer.beginTransaction();
                    while (true) {
                        ConsumerRecords<String, Block> UTXORecords = consumerFromUTXO.poll(Duration.ofMillis(100));
                        for (ConsumerRecord<String, Block> UTXORecord : UTXORecords) {
                            //reset heartbeat since continuous polling
                            heartbeat = System.currentTimeMillis();
                            for (int j = 0; j < UTXORecord.value().getTransactions().size(); j++) {
                                //add UTXO to account
                                long amount = UTXORecord.value().getTransactions().get(j).getAmount();
                                bankBalance.compute(UTXORecord.value().getTransactions().get(j).getInAccount(), (key, value)
                                        -> value + amount);
                            }
                            //build block of offset
                            lastOffsetOfAggUTXO.put(i, UTXORecord.offset());
                            String bank = partitionBank.get(i);
                            Transaction detail = new Transaction(-1L, bank, bank, bank, bank, i, i,
                                    lastOffsetOfAggUTXO.get(i), 3);
                            List<Transaction> listOfDetail = new ArrayList<Transaction>();
                            listOfDetail.add(detail);
                            Block offsetBlock = Block.newBuilder()
                                    .setTransactions(listOfDetail)
                                    .build() ;

                            //update "aggUTXOOffset" & "localBalance" topic
                            producer.send(new ProducerRecord<String, Block>("aggUTXOOffset",
                                    i, partitionBank.get(i), offsetBlock));
                            producer.send(new ProducerRecord<String, Block>("localBalance",
                                    UTXORecord.value().getTransactions().get(0).getOutbankPartition(),
                                    UTXORecord.value().getTransactions().get(0).getOutbank(),
                                    newBalance(UTXORecord.value(), numOfAccount)));
                            update = true;
                        }
                        //if already pol the newest or wasting too much time, break the while loop.
                        if (System.currentTimeMillis() - heartbeat >= 200 ||
                                startTime.get(partitionBank.get(i)) > updateBreakTime) {
                            break;
                        }
                    }try {
                        consumerFromUTXO.commitSync();
                        producer.commitTransaction();
                        if(update) {
                            System.out.println("UTXO updated\n" + bankBalance);
                        }
                    } catch (Exception e) {
                        producer.abortTransaction();
                        System.out.println("UTXO update failed.");
                    }
                }
            }
        }
    }

    private static void PollUTXO(Block recordValue) {
        while (true) {
            long heartbeat = System.currentTimeMillis();
            ConsumerRecords<String, Block> records = consumerFromUTXO.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, Block> record : records) {
                bankBalance.compute(record.value().getTransactions().get(0).getInAccount(), (key, value)
                        -> value + record.value().getTransactions().get(0).getAmount());
                heartbeat = System.currentTimeMillis();
            }
            if (System.currentTimeMillis()- heartbeat >= 200) {
                break;
            }
        }
    }

    private static void PollFromLocalBalance(Block recordValue) {
        //change the strategy: poll from beginning


        //consumer assign to specific topicPartition
        TopicPartition topicPartition =
                new TopicPartition("localBalance", recordValue.getTransactions().get(0).getOutbankPartition());
        List<TopicPartition> partitions = new ArrayList<>();
        partitions.add(topicPartition);
        consumerFromLocalBalance.assign(partitions);

        //find the latest offset, since that is all we need
        consumerFromLocalBalance.seekToEnd(Collections.singleton(topicPartition));
        long latestOffset = consumerFromLocalBalance.position(topicPartition);
        boolean findingLast = true;

        //poll latest data from specific topic partition
        while (findingLast) {
            consumerFromLocalBalance.seek(topicPartition, latestOffset);
            latestOffset -= 1;
            ConsumerRecords<String, Block> balanceRecords = consumerFromLocalBalance.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, Block> balanceRecord : balanceRecords) {
                for (int j = 0; j < balanceRecord.value().getTransactions().size(); j++) {
                    long amount = balanceRecord.value().getTransactions().get(j).getAmount();
                    bankBalance.compute(balanceRecord.value().getTransactions().get(j).getOutAccount(), (key, value)
                            -> amount);
                    startTime.put(balanceRecord.key(), System.currentTimeMillis());
                    partitionBank.put(balanceRecord.partition(), balanceRecord.key());
                    //partition and bank No. for outbank is straight forward,
                    //while inbank metadata will be found only inside the transactions
                }
                findingLast = false;
                //System.out.println("Poll from localBalance: " + bankBalance);
                System.out.println("Poll from localBalance. Now in charge of: " + partitionBank + " (partition:bank)");
            }
        }
    }

    private static Block newBalance(Block recordValue, int numOfAccount) {
        //building the block of current local balance
        List<Transaction> listOfNewBalance = new ArrayList<Transaction>();
        String outbank = recordValue.getTransactions().get(0).getOutbank();
        int outbankPartition = recordValue.getTransactions().get(0).getOutbankPartition();

        for (int accountNum = 1; accountNum <= numOfAccount; accountNum++) {
            String account;
            if (accountNum < 10) {
                account = recordValue.getTransactions().get(0).getOutbank() +  "000" + accountNum;
            }else if (accountNum < 100){
                account = recordValue.getTransactions().get(0).getOutbank() + "00" + accountNum;
            }else if (accountNum < 1000){
                account = recordValue.getTransactions().get(0).getOutbank() + "0" + accountNum;
            }else {
                account = recordValue.getTransactions().get(0).getOutbank() + accountNum;
            }
            Transaction newBalance = new Transaction(-1L,
                    outbank, account,
                    outbank, account,
                    outbankPartition, outbankPartition,
                    bankBalance.get(account), -1);

            listOfNewBalance.add(newBalance);
        }
        return Block.newBuilder()
                .setTransactions(listOfNewBalance)
                .build();
    }



    public static String randomString() {
        byte[] array = new byte[32]; // length is bounded by 32
        new Random().nextBytes(array);
        return new String(array, StandardCharsets.UTF_8);
    }
}


