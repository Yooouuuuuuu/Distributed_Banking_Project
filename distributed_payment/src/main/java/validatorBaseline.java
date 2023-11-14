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
import org.slf4j.simple.SimpleLogger;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class validatorBaseline {
    static KafkaConsumer<String, Block> consumerFromBlocks;
    static KafkaConsumer<String, LocalBalance> consumerFromLocalBalance;
    static KafkaProducer producer;
    static HashMap<String, Long> bankBalance = new HashMap<String, Long>();
    static HashMap<Integer, String> partitionBank = new HashMap<Integer, String>();
    static long rejectedCount = 0;
    static long UTXOCount = 0; //only for testing

    public static void main(String[] args) throws Exception {

        //inputs
        String bootstrapServers = args[0];
        String schemaRegistryUrl = args[1];
        int maxPoll = Integer.parseInt(args[2]);
        boolean orderMultiplePartition = Boolean.parseBoolean(args[3]);
        boolean UTXODirectAdd = Boolean.parseBoolean(args[4]);
        String transactionalId = args[5];
        String log = args[6];
        boolean orderSeparateSend = true;

        //setups
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, log);//"off", "trace", "debug", "info", "warn", "error"
        InitConsumer(maxPoll, bootstrapServers, schemaRegistryUrl);
        InitProducer(bootstrapServers, schemaRegistryUrl, transactionalId);
        //Logger logger = LoggerFactory.getLogger(oldValidators.validatorDirectPollUTXO.class);
        producer.initTransactions();

        //poll from "blocks" topic
        while (true) {
            ConsumerRecords<String, Block> records = consumerFromBlocks.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, Block> record : records) {
                //Start atomically transactional write. One block per transactional write.
                producer.beginTransaction();
                try {
                    if (record.value().getTransactions().get(0).getCategory() != 2) {
                        ProcessBlocks(record.value(), orderMultiplePartition, UTXODirectAdd, orderSeparateSend);

                    } else {
                        //Category 2 means it is an initialize record for accounts' balance. Only do once when system start.
                        InitBank(record.value(), record, orderMultiplePartition);
                    }
                    consumerFromBlocks.commitSync();
                    producer.commitTransaction();

                } catch (Exception e) {
                    producer.abortTransaction();
                    bankBalance = new HashMap<String, Long>();
                    partitionBank = new HashMap<>();
                    System.out.println("Tx aborted. Reset hashmaps. Exception: " + e.getMessage());
                }
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
        propsConsumerTx.put("max.partition.fetch.bytes", 2097152);


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
                        System.out.println("validator(Baseline) is rebalanced. Reset hashmaps.");
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
        //consumer consume from "localBalance" topic
        consumerFromLocalBalance =
                new KafkaConsumer<String, LocalBalance>(propsConsumerAssign);
    }

    private static void InitProducer(String bootstrapServers, String schemaRegistryUrl, String transactionalId) {
        Properties propsProducer = new Properties();
        propsProducer.put("bootstrap.servers", bootstrapServers);
        propsProducer.put("transactional.id", transactionalId);
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

    private static void InitBank(Block recordValue,
                                 ConsumerRecord<String, Block> record,
                                 boolean orderMultiplePartition)
            throws ExecutionException, InterruptedException {

        for (int i = 0; i < recordValue.getTransactions().size(); i++) {
            partitionBank.put(record.partition(), record.key());
            bankBalance.put(recordValue.getTransactions().get(i).getOutAccount(),
                    recordValue.getTransactions().get(i).getAmount());

            LocalBalance initBalance =
                    new LocalBalance(bankBalance.get(recordValue.getTransactions().get(i).getOutAccount()));
            producer.send(new ProducerRecord<String, LocalBalance>("localBalance",
                    recordValue.getTransactions().get(i).getOutbankPartition(),
                    recordValue.getTransactions().get(i).getOutAccount(),
                    initBalance));

            if (orderMultiplePartition) {
                Transaction initDetail = recordValue.getTransactions().get(i);
                List<Transaction> listOfInitDetail = new ArrayList<Transaction>();
                listOfInitDetail.add(initDetail);
                Block initBlock = Block.newBuilder()
                        .setTransactions(listOfInitDetail)
                        .build();

                producer.send(new ProducerRecord<String, Block>("order",
                        initBlock.getTransactions().get(0).getOutbankPartition(),
                        initBlock.getTransactions().get(0).getOutbank(),initBlock));
            }
        }
        if (!orderMultiplePartition) {
            producer.send(new ProducerRecord<String, Block>("order", recordValue));
        }

        System.out.println("Initialized. Now in charge of: " + partitionBank + " (partition:bank)");
    }

    private static void ProcessBlocks(Block recordValue,
                                      boolean orderMultiplePartition,
                                      boolean UTXODirectAdd,
                                      boolean orderSeparateSend)
            throws ExecutionException, IOException, InterruptedException {

        //initialize block
        Block currentBlock = recordValue;

        //validate transactions
        for (int i = 0; i < recordValue.getTransactions().size(); i++) {
            //in this version, we will also see UTXO in blocks, since they don't need to be validated, skip them.
            if (recordValue.getTransactions().get(i).getCategory() == 0){
                //check if bankBalance exist
                if (!bankBalance.containsKey(recordValue.getTransactions().get(i).getOutAccount())) {
                    PollFromLocalBalance(recordValue.getTransactions().get(i).getOutbankPartition(),
                            recordValue.getTransactions().get(i).getOutbank());
                }

                //If the out account have enough money, if not, mark it.
                if (bankBalance.get(recordValue.getTransactions().get(i).getOutAccount())
                        < recordValue.getTransactions().get(i).getAmount()) {

                    //while reading order topic, cat = 0 is successful, 3 is rejected, 1 is UTXO, and 2 is the init data
                    currentBlock.getTransactions().get(i).put("category", 3);
                    rejectedCount += 1;
                    System.out.printf("Transaction No.%d cancelled.%n " + rejectedCount + " rejected.\n"
                            , recordValue.getTransactions().get(i).getSerialNumber());
                }
            }
        }

        //send to order topic
        // (if orderMultiplePartition == true && orderSeparateSend == true,
        // this means only the out account side of the transaction.
        // We do separate the order records for in account in order to make it serializable.)
        if (orderMultiplePartition) {
            producer.send(new ProducerRecord<String, Block>("order",
                    currentBlock.getTransactions().get(0).getOutbankPartition(),
                    currentBlock.getTransactions().get(0).getOutbank(),currentBlock));
        } else {
            producer.send(new ProducerRecord<String, Block>("order", currentBlock));
        }

        //process and send due to the result of validation
        for (int i = 0; i < currentBlock.getTransactions().size(); i++) {
            //this variable might be used in the two following if conditions
            long withdraw = currentBlock.getTransactions().get(i).getAmount();

            if (currentBlock.getTransactions().get(i).getCategory() == 0) { // 0 is successful, while 3 is rejected
                bankBalance.compute(currentBlock.getTransactions().get(i).getOutAccount(), (key, value)
                        -> value - withdraw);

                // update "localBalance" topic
                LocalBalance newBalance =
                        new LocalBalance(bankBalance.get(currentBlock.getTransactions().get(i).getOutAccount()));
                producer.send(new ProducerRecord<String, LocalBalance>("localBalance",
                        currentBlock.getTransactions().get(i).getOutbankPartition(),
                        currentBlock.getTransactions().get(i).getOutAccount(),
                        newBalance));

                //if the inbank account is in charge by this validator, add the money directly rather than sending UTXO
                if (bankBalance.containsKey(currentBlock.getTransactions().get(i).getInAccount())) {
                    bankBalance.compute(currentBlock.getTransactions().get(i).getInAccount(), (key, value)
                            -> value + withdraw);

                    // update "localBalance" topic
                    newBalance =
                            new LocalBalance(bankBalance.get(currentBlock.getTransactions().get(i).getInAccount()));
                    producer.send(new ProducerRecord<String, LocalBalance>("localBalance",
                            currentBlock.getTransactions().get(i).getInbankPartition(),
                            currentBlock.getTransactions().get(i).getInAccount(),
                            newBalance));

                    // send to order topic
                    Transaction orderInDetail = currentBlock.getTransactions().get(i);
                    orderInDetail.put("category", 1);

                    List<Transaction> listOfOrderInDetail = new ArrayList<Transaction>();
                    listOfOrderInDetail.add(orderInDetail);
                    Block orderIn = Block.newBuilder()
                            .setTransactions(listOfOrderInDetail)
                            .build();

                    producer.send(new ProducerRecord<String, Block>("order",
                            orderIn.getTransactions().get(0).getInbankPartition(),
                            orderIn.getTransactions().get(0).getInbank(),
                            orderIn));

                } else {
                    //send UTXO
                    Transaction UTXODetail = currentBlock.getTransactions().get(i);
                    UTXODetail.put("category", 1);
                    List<Transaction> listOfUTXODetail = new ArrayList<Transaction>();
                    listOfUTXODetail.add(UTXODetail);
                    Block UTXOBlock = Block.newBuilder()
                            .setTransactions(listOfUTXODetail)
                            .build();
                    producer.send(new ProducerRecord<String, Block>("transactions",
                            UTXOBlock.getTransactions().get(0).getInbankPartition(),
                            UTXOBlock.getTransactions().get(0).getInbank(),
                            UTXOBlock));
                }

            } else if (currentBlock.getTransactions().get(i).getCategory() == 1) {
                //check if bankBalance exist
                if (!bankBalance.containsKey(recordValue.getTransactions().get(i).getInAccount())) {
                    PollFromLocalBalance(recordValue.getTransactions().get(i).getInbankPartition(),
                            recordValue.getTransactions().get(i).getInbank());
                }

                //add money to inbank
                bankBalance.compute(recordValue.getTransactions().get(i).getInAccount(), (key, value)
                        -> value + withdraw);

                // update "localBalance" topic
                LocalBalance newBalance =
                        new LocalBalance(bankBalance.get(recordValue.getTransactions().get(i).getInAccount()));
                producer.send(new ProducerRecord<String, LocalBalance>("localBalance",
                        recordValue.getTransactions().get(i).getInbankPartition(),
                        recordValue.getTransactions().get(i).getInAccount(),
                        newBalance));

                Transaction orderInDetail = recordValue.getTransactions().get(i);
                List<Transaction> listOfOrderInDetail = new ArrayList<Transaction>();
                listOfOrderInDetail.add(orderInDetail);
                Block orderIn = Block.newBuilder()
                        .setTransactions(listOfOrderInDetail)
                        .build();

                //we have to send UTXO records in the "validator" version around here,
                //but since we sent the whole block before, it had already been done.

                //just for testing, check if every UTXO is consumed.
                UTXOCount += 1;
                if (UTXOCount % 10000 == 0) {
                    System.out.println("numbers of UTXO consumed: " + UTXOCount);
                }
            }
        }

    }

    private static void PollFromLocalBalance(int outbankPartition, String bankID) {

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
        partitionBank.put(outbankPartition, bankID);
        System.out.println("Poll from localBalance. Now in charge of: " + partitionBank + " (partition:bank)");
    }
}
