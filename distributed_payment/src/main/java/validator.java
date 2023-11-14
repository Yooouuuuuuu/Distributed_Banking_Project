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
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class validator {
    static KafkaConsumer<String, Block> consumerFromBlocks;
    static KafkaConsumer<String, Block> consumerFromUTXO;
    static KafkaConsumer<String, Block> consumerFromUTXOOffset;
    static KafkaConsumer<String, LocalBalance> consumerFromLocalBalance;
    static KafkaProducer producer;
    static KafkaProducer producer2;
    static Map<String, Long> bankBalance = new ConcurrentHashMap<>();
    static Map<Integer, String> partitionBank = new ConcurrentHashMap<>();
    static HashMap<Integer, Long> lastOffsetOfUTXO = new HashMap<>();
    static long rejectedCount = 0;
    static volatile boolean threadsStopFlag = false;
    static volatile boolean repartitionFlag = true;
    static  ArrayList<Long> consumeList = new ArrayList<Long>(); //only for testing
    private static final ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) throws Exception {

        //inputs
        String bootstrapServers = args[0];
        String schemaRegistryUrl = args[1];
        int maxPoll = Integer.parseInt(args[2]);
        int maxPollUTXO = Integer.parseInt(args[3]);

        boolean orderMultiplePartition = Boolean.parseBoolean(args[4]);
        boolean UTXODirectAdd = Boolean.parseBoolean(args[5]);
        String transactionalId = args[6];
        String log = args[7];
        boolean orderSeparateSend = true;

        //setups
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, log);//"off", "trace", "debug", "info", "warn", "error"
        InitConsumer(maxPoll, maxPollUTXO, bootstrapServers, schemaRegistryUrl);
        InitProducer(bootstrapServers, schemaRegistryUrl, transactionalId);
        Logger logger = LoggerFactory.getLogger(validator.class);

        //thread 0
        Thread pollBlocksThread = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    long transactionCounts = 0;
                    producer.initTransactions();
                    //poll from "blocks" topic
                    while (!threadsStopFlag) {
                        ConsumerRecords<String, Block> records = consumerFromBlocks.poll(Duration.ofMillis(100));
                        for (ConsumerRecord<String, Block> record : records) {
                            //logger.info(record.value().toString());
                            //Start atomically transactional write. One block per transactional write.
                            producer.beginTransaction();
                            try {
                                lock.lock();
                                //As outbank, withdraw money and create UTXO for inbank. Category 0 means it is a raw transaction.
                                if (record.value().getTransactions().get(0).getCategory() == 0) {
                                    try {
                                        ProcessBlocks(record.value(), record.timestamp(), orderMultiplePartition, UTXODirectAdd, orderSeparateSend);
                                        transactionCounts += record.value().getTransactions().size();
                                        //System.out.println("transaction counts: " + transactionCounts);

                                    } catch (InterruptedException | ExecutionException | IOException e) {
                                    }
                                } else if (record.value().getTransactions().get(0).getCategory() == 2) {
                                    //Category 2 means it is an initialize record for accounts' balance. Only do once when system start.
                                    try {
                                        InitBank(record.value(), record, orderMultiplePartition);
                                    } catch (InterruptedException | ExecutionException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                                consumerFromBlocks.commitSync();
                                producer.commitTransaction();
                                lock.unlock();
                            } catch (Exception e) {
                                //if kafka TX aborted, set the flag to end all threads.
                                producer.abortTransaction();
                                System.out.println(Thread.currentThread().getName() + " Tx aborted. Exception: " + e.getMessage());
                                threadsStopFlag = true;
                                lock.unlock();
                            }
                        }
                    }

                    //if the other thread die, end while loop thus end the thread.
                    System.out.println(Thread.currentThread().getName() + " stopped.");
                }finally {
                    //if somehow went wrong, set the flag to end the other thread.
                    threadsStopFlag = true;
                    System.out.println(Thread.currentThread().getName() + " stopped.");
                }
            }
        });

        //thread 1
        Thread pollUTXOThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    UpdateUTXO(orderSeparateSend);
                    System.out.println(Thread.currentThread().getName() + " stopped.");
                } catch (InterruptedException e) {
                    threadsStopFlag = true;
                    System.out.println(Thread.currentThread().getName() + " stopped.");
                    throw new RuntimeException(e);
                } finally {
                    threadsStopFlag = true;
                    System.out.println(Thread.currentThread().getName() + " stopped.");
                }
            }
        });
        pollBlocksThread.start();
        pollUTXOThread.start();

        System.in.read();
        pollBlocksThread.stop();
        pollUTXOThread.stop();
    }

    private static void InitConsumer(int maxPoll, int maxPollUTXO, String bootstrapServers, String schemaRegistryUrl) {
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
                        bankBalance = new ConcurrentHashMap<String, Long>();
                        partitionBank = new ConcurrentHashMap<>();
                        lastOffsetOfUTXO = new HashMap<>();
                        repartitionFlag = true;
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
        propsConsumerAssign.put("max.poll.records", maxPollUTXO);
        propsConsumerAssign.put("max.partition.fetch.bytes", 2097152);

        //consumer consume from "UTXO" topic
        consumerFromUTXO =
                new KafkaConsumer<String, Block>(propsConsumerAssign);

        //consumer consume from "UTXOOffset" topic
        consumerFromUTXOOffset =
                new KafkaConsumer<String, Block>(propsConsumerAssign);

        //consumer consume from "localBalance" topic
        consumerFromLocalBalance =
                new KafkaConsumer<String, LocalBalance>(propsConsumerAssign);
    }

    private static void InitProducer(String bootstrapServers, String schemaRegistryUrl, String transactionalId) {
        Properties propsProducer = new Properties();
        propsProducer.put("bootstrap.servers", bootstrapServers);
        propsProducer.put("transactional.id", transactionalId + "Main");
        propsProducer.put("transaction.timeout.ms", 300000);
        propsProducer.put("enable.idempotence", "true");
        propsProducer.put("max.block.ms", "1000");
        // avro part
        propsProducer.setProperty("key.serializer", StringSerializer.class.getName());
        propsProducer.setProperty("value.serializer", KafkaAvroSerializer.class.getName());
        propsProducer.setProperty("schema.registry.url", schemaRegistryUrl);
        propsProducer.put("value.subject.name.strategy", RecordNameStrategy.class.getName());
        producer = new KafkaProducer<>(propsProducer);

        Properties propsProducer2 = new Properties();
        propsProducer2.put("bootstrap.servers", bootstrapServers);
        propsProducer2.put("transactional.id", transactionalId + "UTXO");
        propsProducer2.put("transaction.timeout.ms", 300000);
        propsProducer2.put("enable.idempotence", "true");
        propsProducer2.put("max.block.ms", "1000");
        // avro part
        propsProducer2.setProperty("key.serializer", StringSerializer.class.getName());
        propsProducer2.setProperty("value.serializer", KafkaAvroSerializer.class.getName());
        propsProducer2.setProperty("schema.registry.url", schemaRegistryUrl);
        propsProducer2.put("value.subject.name.strategy", RecordNameStrategy.class.getName());
        producer2 = new KafkaProducer<>(propsProducer2);

    }

    private static void InitBank(Block recordValue,
                                 ConsumerRecord<String, Block> record,
                                 boolean orderMultiplePartition)
            throws ExecutionException, InterruptedException {

        for (int i = 0; i < recordValue.getTransactions().size(); i++) {
            partitionBank.put(record.partition(), record.key());
            bankBalance.put(recordValue.getTransactions().get(i).getOutAccount(),
                    recordValue.getTransactions().get(i).getAmount());

            lastOffsetOfUTXO = new HashMap<>();

            LocalBalance initBalance =
                    new LocalBalance(bankBalance.get(recordValue.getTransactions().get(i).getOutAccount()));
            producer.send(new ProducerRecord<String, LocalBalance>("localBalance",
                    recordValue.getTransactions().get(0).getOutbankPartition(),
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
                                      long timestamp,
                                      boolean orderMultiplePartition,
                                      boolean UTXODirectAdd,
                                      boolean orderSeparateSend)
            throws ExecutionException, IOException, InterruptedException {
        //initialize block
        Block currentBlock = recordValue;

        //validate transactions
        for (int i = 0; i < recordValue.getTransactions().size(); i++) {
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
            }

            //if the inbank account is in charge by this validator, add the money directly rather than sending UTXO
            if (UTXODirectAdd) {
                if (bankBalance.containsKey(currentBlock.getTransactions().get(i).getInAccount())) {

                    //if (partitionBank.containsValue(currentBlock.getTransactions().get(i).getInbank())) {
                    bankBalance.compute(currentBlock.getTransactions().get(i).getInAccount(), (key, value)
                            -> value + withdraw);

                    // update "localBalance" topic
                    LocalBalance newBalance =
                            new LocalBalance(bankBalance.get(currentBlock.getTransactions().get(i).getInAccount()));
                    producer.send(new ProducerRecord<String, LocalBalance>("localBalance",
                            currentBlock.getTransactions().get(i).getInbankPartition(),
                            currentBlock.getTransactions().get(i).getInAccount(),
                            newBalance));

                    // send to order topic if order records are separated (into in and out)
                    if (orderSeparateSend) {
                        Transaction orderInDetail = currentBlock.getTransactions().get(i);

                        //these are records similar to UTXO, while never really become one.
                        //However, we consider them the same while reading order topic, thus change the cat to 1 (as UTXO)
                        orderInDetail.put("category", 1);
                        orderInDetail.put("timestamp2", timestamp);
                        List<Transaction> listOfOrderInDetail = new ArrayList<Transaction>();
                        listOfOrderInDetail.add(orderInDetail);
                        Block orderIn = Block.newBuilder()
                                .setTransactions(listOfOrderInDetail)
                                .build();

                        producer.send(new ProducerRecord<String, Block>("order",
                                orderIn.getTransactions().get(0).getInbankPartition(),
                                orderIn.getTransactions().get(0).getInbank(),
                                orderIn));
                    }

                } else {
                    //send UTXO
                    Transaction UTXODetail = currentBlock.getTransactions().get(i);
                    List<Transaction> listOfUTXODetail = new ArrayList<Transaction>();
                    listOfUTXODetail.add(UTXODetail);
                    Block UTXOBlock = Block.newBuilder()
                            .setTransactions(listOfUTXODetail)
                            .build();
                    producer.send(new ProducerRecord<String, Block>("UTXO",
                            UTXOBlock.getTransactions().get(0).getInbankPartition(),
                            UTXOBlock.getTransactions().get(0).getInbank(),
                            UTXOBlock));
                }
            }else {
                //send UTXO
                Transaction UTXODetail = currentBlock.getTransactions().get(i);
                List<Transaction> listOfUTXODetail = new ArrayList<Transaction>();
                listOfUTXODetail.add(UTXODetail);
                Block UTXOBlock = Block.newBuilder()
                        .setTransactions(listOfUTXODetail)
                        .build();
                producer.send(new ProducerRecord<String, Block>("UTXO",
                        UTXOBlock.getTransactions().get(0).getInbankPartition(),
                        UTXOBlock.getTransactions().get(0).getInbank(),
                        UTXOBlock));
            }
        }

    }

    private static void UpdateUTXO(boolean orderSeparateSend) throws InterruptedException {
        producer2.initTransactions();

        //if thread-0's kafka consumer repartition, init the UTXO consumer.
        while (!threadsStopFlag) {
            while (repartitionFlag) {
                Thread.sleep(10000);
                partitionBank.forEach((key, value) -> {
                    int updatePartition = key;

                    //poll last read offset if reset
                    if (!lastOffsetOfUTXO.containsKey(updatePartition)) {
                        lastOffsetOfUTXO.put(updatePartition, -1L);

                        TopicPartition topicPartition =
                                new TopicPartition("UTXOOffset", updatePartition);
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
                            System.out.println("Poll from UTXOOffset: " + lastOffsetOfUTXO + " (partition:offset)");
                        }
                    }
                });

                //assign topicPartition
                TopicPartition partitions[] = new TopicPartition[partitionBank.size()];
                AtomicInteger count = new AtomicInteger();

                if (partitionBank.size() != 0) {
                    System.out.println(partitionBank + " " + partitionBank.size());
                    partitionBank.forEach((key, value) -> {
                        //partitions[10] = new TopicPartition("UTXO", key);
                        partitions[count.intValue()] = new TopicPartition("UTXO", key);
                        count.addAndGet(1);
                    });

                    System.out.println(Arrays.asList(partitions));
                    consumerFromUTXO.assign(Arrays.asList(partitions));
                    //assign success only if we wait for thread-0 to go first,
                    //we'll face the same problem when poll UTXOOffset

                    //seek offset for all topicPartition
                    for (int i = 0; i < partitionBank.size(); i++) {
                        consumerFromUTXO.seek(partitions[i], lastOffsetOfUTXO.get(partitions[i].partition()) + 1);
                    }
                    repartitionFlag = false;
                }
            }

            producer2.beginTransaction();
            try {
                ConsumerRecords<String, Block> UTXORecords = consumerFromUTXO.poll(Duration.ofMillis(100));
                lock.lock();
                for (ConsumerRecord<String, Block> UTXORecord : UTXORecords) {
                    consumeList.add(UTXORecord.value().getTransactions().get(0).getSerialNumber());
                    if (consumeList.size() % 10000 == 0) {
                        System.out.println("UTXO consumed counts: " + consumeList.size());
                    }

                    int updatePartition = UTXORecord.value().getTransactions().get(0).getInbankPartition();
                    //reset heartbeat since continuous polling
                    //add UTXO to account
                    long amount = UTXORecord.value().getTransactions().get(0).getAmount();
                    bankBalance.compute(UTXORecord.value().getTransactions().get(0).getInAccount(),
                            (key, value) -> value + amount);
                    // update "localBalance" topic
                    LocalBalance newBalance =
                            new LocalBalance(bankBalance.get(
                                    UTXORecord.value().getTransactions().get(0).getInAccount()));
                    producer2.send(new ProducerRecord<>("localBalance",
                            UTXORecord.value().getTransactions().get(0).getInbankPartition(),
                            UTXORecord.value().getTransactions().get(0).getInAccount(),
                            newBalance));

                    // send to order topic if order records are separated (into in and out)
                    if (orderSeparateSend) {
                        Transaction orderInDetail = UTXORecord.value().getTransactions().get(0);
                        orderInDetail.put("category", 1);
                        orderInDetail.put("timestamp2", UTXORecord.timestamp());

                        List<Transaction> listOfOrderInDetail = new ArrayList<Transaction>();
                        listOfOrderInDetail.add(orderInDetail);
                        Block orderIn = Block.newBuilder()
                                .setTransactions(listOfOrderInDetail)
                                .build();

                        producer2.send(new ProducerRecord<String, Block>("order",
                                orderIn.getTransactions().get(0).getInbankPartition(),
                                orderIn.getTransactions().get(0).getInbank(),
                                orderIn));
                    }

                    //update UTXO offset
                    lastOffsetOfUTXO.put(updatePartition, UTXORecord.offset());
                    String bank = partitionBank.get(updatePartition);
                    Transaction detail = new Transaction(-1L,
                            bank, bank, bank, bank,
                            updatePartition, updatePartition,
                            lastOffsetOfUTXO.get(updatePartition), 3, 0L, 0L);
                    List<Transaction> listOfDetail = new ArrayList<Transaction>();
                    listOfDetail.add(detail);
                    Block offsetBlock = Block.newBuilder()
                            .setTransactions(listOfDetail)
                            .build();
                    producer2.send(new ProducerRecord<>("UTXOOffset",
                            updatePartition,
                            partitionBank.get(updatePartition),
                            offsetBlock));
                }
                producer2.commitTransaction();
                lock.unlock();

            } catch (Exception e) {
                producer2.abortTransaction();
                System.out.println(Thread.currentThread().getName() + "Tx aborted(UTXO update failed). Exception: " + e.getMessage());
                threadsStopFlag = true;
                lock.unlock();
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


