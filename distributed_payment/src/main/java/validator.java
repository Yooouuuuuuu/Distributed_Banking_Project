import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import static java.lang.Boolean.parseBoolean;

public class validator {
    static HashMap<String, Long> bankBalance = new HashMap<String, Long>();
    static HashMap<String, Long> creditOffset = new HashMap<String, Long>();
    static KafkaConsumer<String, Transaction> consumerFromBig;
    static KafkaConsumer<String, Transaction> consumerFromLocalBalance;
    static KafkaConsumer<String, Transaction> consumerFromCredit;
    static Producer<String, Transaction> producer;

    public static void main(String[] args) throws Exception {

        /*
        args[0]: # of partitions
        args[1]: # of transactions
        args[2]: "max.poll.records"
        args[3]: batch processing
        args[4]: poll from localBalance while repartition
        args[5]: credit topic exist
        args[6]: direct write to successful
        args[7]: one partition only, skip balancer.
        args[8]: bootstrap.servers
        */

        int numOfTX = Integer.parseInt(args[1]);
        boolean batchProcessing = parseBoolean(args[3]);
        boolean notPollLocal = parseBoolean(args[4]);
        boolean creditTopicExist = parseBoolean(args[5]);

        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "off"); //"off", "trace", "debug", "info", "warn", "error".
        InitConsumer(Integer.parseInt(args[2]), args[8]);
        InitProducer(args[8]);
        Logger logger = LoggerFactory.getLogger(validator.class);
        producer.initTransactions();

        //poll from bigTX
        if (batchProcessing) {
            while (true) {
                ConsumerRecords<String, Transaction> records = consumerFromBig.poll(Duration.ofMillis(100));
                producer.beginTransaction();        //start atomically transaction
                try {
                    for (ConsumerRecord<String, Transaction> record : records) {
                        //System.out.println(record.value().getSerialNumber());
                        logger.info("InBank: " + record.value().getInBank() + " ,OutBank: " + record.value().getOutBank() + " ,Value: " + record.value().getAmount() + " ,Offset:" + record.offset());
                        if (record.value().getCategory() == 0) {
                            ProcessBig(record.value(), notPollLocal, creditTopicExist);
                        } else if (record.value().getCategory() == 1) {
                            ProcessAggregated(record.value(), notPollLocal, creditTopicExist);
                        } else if (record.value().getCategory() == 2) {
                            ProcessCompensate(record.value(), notPollLocal);
                        } else if (record.value().getCategory() == 3) {
                            InitBank(record.value());
                        }
                        if (record.value().getSerialNumber() == numOfTX) {
                            System.out.println("Test finished.");
                        }
                    }
                    consumerFromBig.commitSync();
                    producer.commitTransaction();
                    //System.out.println("one poll finish with " + records.count() + " records");
                } catch (Exception e) {
                    //try to catch Exception
                    producer.abortTransaction();
                    //end atomically transaction
                    bankBalance = new HashMap<String, Long>();
                    System.out.println("Tx aborted, bankBalance been reset.");
                    //return;
                }
            }
        }else {
            while (true) {
                ConsumerRecords<String, Transaction> records = consumerFromBig.poll(Duration.ofMillis(100));
                try {
                    for (ConsumerRecord<String, Transaction> record : records) {
                        producer.beginTransaction();        //start atomically transaction
                        logger.info("InBank: " + record.value().getInBank() + " ,OutBank: " + record.value().getOutBank() + " ,Value: " + record.value().getAmount() + " ,Offset:" + record.offset());
                        if (record.value().getCategory() == 0) {
                            ProcessBig(record.value(), notPollLocal, creditTopicExist);
                        } else if (record.value().getCategory() == 1) {
                            ProcessAggregated(record.value(), notPollLocal, creditTopicExist);
                        } else if (record.value().getCategory() == 2) {
                            ProcessCompensate(record.value(), notPollLocal);
                        } else if (record.value().getCategory() == 3) {
                            InitBank(record.value());
                        }
                        consumerFromBig.commitSync();
                        producer.commitTransaction();
                    }
                } catch ( Exception e ) {
                    //try to catch Exception
                    producer.abortTransaction();
                    //end atomically transaction
                    bankBalance = new HashMap<String, Long>();
                    System.out.println("Tx aborted, bankBalance been reset.");
                    //return;
                }
            }
        }
    }

    private static void InitConsumer(int maxPoll, String bootstrapServers) {
        //consumer consume from big
        Properties propsConsumerTx = new Properties();
        propsConsumerTx.put("bootstrap.servers", bootstrapServers);
        propsConsumerTx.put("group.id", "validator-main-group");
        propsConsumerTx.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        propsConsumerTx.put("value.deserializer", "TxDeserializer");
        propsConsumerTx.put("isolation.level", "read_committed");
        propsConsumerTx.put("enable.auto.commit", "false");
        propsConsumerTx.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        propsConsumerTx.put("max.poll.records", maxPoll);

        String input_topic = "bigTX";
        consumerFromBig =
                new KafkaConsumer<String, Transaction>(propsConsumerTx);
        consumerFromBig.subscribe(Collections.singletonList(input_topic),
                new ConsumerRebalanceListener() {
                    @Override
                    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                        //System.out.println("onPartitionsRevoked");
                    }
                    @Override
                    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                        System.out.println("bankBalance before rebalance: " + bankBalance);
                        bankBalance = new HashMap<String, Long>();
                    }});

        //consumer consume from localBalance
        Properties propsConsumerLocalBalance = new Properties();
        propsConsumerLocalBalance.put("bootstrap.servers", bootstrapServers);
        propsConsumerLocalBalance.put("group.id", "localBalance-group");
        propsConsumerLocalBalance.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        propsConsumerLocalBalance.put("value.deserializer", "TxDeserializer");
        propsConsumerLocalBalance.put("isolation.level", "read_committed");
        propsConsumerLocalBalance.put("enable.auto.commit", "false");
        propsConsumerLocalBalance.put("fetch.max.bytes", 0);
        consumerFromLocalBalance =
                new KafkaConsumer<String, Transaction>(propsConsumerLocalBalance);
        //seek(ToEnd) to topicPartition later, thus no subscribe.

        //consumer consume from aggregatedCredit
        Properties propsConsumerCredit = new Properties();
        propsConsumerCredit.put("bootstrap.servers", bootstrapServers);
        propsConsumerCredit.put("group.id", "Credit-group");
        propsConsumerCredit.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        propsConsumerCredit.put("value.deserializer", "TxDeserializer");
        propsConsumerCredit.put("isolation.level", "read_committed");
        propsConsumerCredit.put("enable.auto.commit", "false");
        consumerFromCredit =
                new KafkaConsumer<String, Transaction>(propsConsumerCredit);
        //consumerFromCredit.subscribe(Collections.singletonList("credit"));

    }

    private static void InitProducer(String bootstrapServers) {
        Properties propsTxWrite = new Properties();
        propsTxWrite.put("bootstrap.servers", bootstrapServers);
        propsTxWrite.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        propsTxWrite.put("value.serializer", "TxSerializer");
        propsTxWrite.put("transactional.id", randomString()); //Should be different between validators to avoid being fenced due to same transactional.id.
        propsTxWrite.put("enable.idempotence", "true");
        propsTxWrite.put("max.block.ms", "1000");
        //propsTxWrite.put("transaction.timeout.ms", "600000");
        producer = new KafkaProducer<>(propsTxWrite);
    }

    private static Transaction CompensationRecord(Transaction tx) {
        return new Transaction(tx.getOutBank(), tx.getInBank(), tx.getAmount(), tx.getSerialNumber(), tx.getOutBankPartition(), -1, 2);
    }

    private static Transaction ValidateOffsetRecord(Transaction tx) {
        return new Transaction(tx.getInBank(), "000", creditOffset.get(tx.getInBank()), -1, tx.getInBankPartition(), -1, 2);
    }

    private static Transaction BalanceRecord(Transaction tx) {
        return new Transaction(tx.getInBank(), "000", bankBalance.get(tx.getInBank()), -1, -1, -1, -1);
    }

    private static void PollFromTmp(Transaction tx, boolean notPollLocal) {
        if (notPollLocal) {
            if (!bankBalance.containsKey(tx.getInBank()) || Objects.equals(tx.getInBank(), "000")) {  // If tx.getInBank() =="000", Tx aborted, rollback to previous local balance.
                TopicPartition topicPartition = new TopicPartition("localBalance", tx.getInBankPartition());
                consumerFromLocalBalance.assign(List.of(topicPartition));
                consumerFromLocalBalance.seekToEnd(Collections.singleton(topicPartition));
                long latestOffset = consumerFromLocalBalance.position(topicPartition);
                boolean findingLast = true;
                while (findingLast) {
                    consumerFromLocalBalance.seek(topicPartition, latestOffset);
                    latestOffset -= 1;
                    ConsumerRecords<String, Transaction> balanceRecords = consumerFromLocalBalance.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, Transaction> balanceRecord : balanceRecords) {
                        bankBalance.compute(tx.getInBank(), (key, value) -> balanceRecord.value().getAmount());
                        System.out.println(tx.getInBank() + " now have " + bankBalance.get(tx.getInBank()));
                        findingLast = false;
                    }
                }
            }
        }else {
            TopicPartition topicPartition = new TopicPartition("localBalance", tx.getInBankPartition());
            consumerFromLocalBalance.assign(List.of(topicPartition));
            consumerFromLocalBalance.seekToEnd(Collections.singleton(topicPartition));
            long latestOffset = consumerFromLocalBalance.position(topicPartition);
            boolean findingLast = true;
            while (findingLast) {
                consumerFromLocalBalance.seek(topicPartition, latestOffset);
                latestOffset -= 1;
                ConsumerRecords<String, Transaction> balanceRecords = consumerFromLocalBalance.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, Transaction> balanceRecord : balanceRecords) {
                    bankBalance.compute(tx.getInBank(), (key, value) -> balanceRecord.value().getAmount());
                    findingLast = false;
                }
            }
        }
    }

    private static void PollFromCredit(Transaction tx) {
        TopicPartition topicPartition = new TopicPartition("credit", tx.getInBankPartition());
        consumerFromCredit.assign(List.of(topicPartition));
        long latestOffset = consumerFromCredit.position(topicPartition);
        consumerFromCredit.seekToEnd(Collections.singleton(topicPartition));
        long lastOffset = consumerFromCredit.position(topicPartition);
        while (lastOffset - latestOffset > 0) {
            ConsumerRecords<String, Transaction> creditRecords = consumerFromCredit.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, Transaction> creditRecord : creditRecords) {
                bankBalance.compute(tx.getInBank(), (key, value) -> value + creditRecord.value().getAmount());
                latestOffset += 1;
            }
        }
        System.out.println(tx.getInBank() + "PollFromCredit complete.");
    }

    private static void ProcessBig(Transaction tx, boolean notPollLocal, boolean creditTopicExist) throws ExecutionException, IOException, InterruptedException {
        if (creditTopicExist) {
            if (bankBalance.get(tx.getInBank()) >= tx.getAmount()) {
                bankBalance.compute(tx.getInBank(), (key, value) -> value - tx.getAmount());
                producer.send(new ProducerRecord<String, Transaction>("credit", tx.getOutBankPartition(), tx.getOutBank(), CompensationRecord(tx)));
                producer.send(new ProducerRecord<String, Transaction>("successfulTX", tx.getInBankPartition(), tx.getInBank(), tx));
                producer.send(new ProducerRecord<String, Transaction>("successfulTX", tx.getOutBankPartition(), tx.getOutBank(), CompensationRecord(tx)));
                producer.send(new ProducerRecord<String, Transaction>("localBalance", tx.getInBankPartition(), tx.getInBank(), BalanceRecord(tx)));
            }else {
                PollFromCredit(tx);
                if (bankBalance.get(tx.getInBank()) >= tx.getAmount()) {
                    bankBalance.compute(tx.getInBank(), (key, value) -> value - tx.getAmount());
                    producer.send(new ProducerRecord<String, Transaction>("credit", tx.getOutBankPartition(), tx.getOutBank(), CompensationRecord(tx)));
                    producer.send(new ProducerRecord<String, Transaction>("successfulTX", tx.getInBankPartition(), tx.getInBank(), tx));
                    producer.send(new ProducerRecord<String, Transaction>("successfulTX", tx.getOutBankPartition(), tx.getOutBank(), CompensationRecord(tx)));
                    producer.send(new ProducerRecord<String, Transaction>("localBalance", tx.getInBankPartition(), tx.getInBank(), BalanceRecord(tx)));
                } else {
                    // If balance is still not enough, reject the TX.
                    producer.send(new ProducerRecord<String, Transaction>("rejectedTX", tx.getInBank(), tx));
                    System.out.println("Big transaction cancelled.");
                }
            }
        } else {
            PollFromTmp(tx, notPollLocal);
            if (bankBalance.get(tx.getInBank()) >= tx.getAmount()) {
                bankBalance.compute(tx.getInBank(), (key, value) -> value - tx.getAmount());
                producer.send(new ProducerRecord<String, Transaction>("bigTX", tx.getOutBankPartition(), tx.getOutBank(), CompensationRecord(tx)));
                producer.send(new ProducerRecord<String, Transaction>("successfulTX", tx.getInBankPartition(), tx.getInBank(), tx));
                producer.send(new ProducerRecord<String, Transaction>("successfulTX", tx.getOutBankPartition(), tx.getOutBank(), CompensationRecord(tx)));
                producer.send(new ProducerRecord<String, Transaction>("localBalance", tx.getInBankPartition(), tx.getInBank(), BalanceRecord(tx)));
            } else {
                producer.send(new ProducerRecord<String, Transaction>("rejectedTX", tx.getInBank(), tx));
                System.out.println("Big transaction cancelled.");
            }
        }
    }

    private static void ProcessAggregated(Transaction tx, boolean notPollLocal, boolean creditTopicExist) throws ExecutionException, InterruptedException {
        if (creditTopicExist) {
            if (bankBalance.get(tx.getInBank()) >= tx.getAmount()) {
                bankBalance.compute(tx.getInBank(), (key, value) -> value - tx.getAmount());
                producer.send(new ProducerRecord<String, Transaction>("credit", tx.getOutBankPartition(), tx.getOutBank(), CompensationRecord(tx)));
                producer.send(new ProducerRecord<String, Transaction>("successfulTX", tx.getInBankPartition(), tx.getInBank(), tx));
                producer.send(new ProducerRecord<String, Transaction>("successfulTX", tx.getOutBankPartition(), tx.getOutBank(), CompensationRecord(tx)));
                producer.send(new ProducerRecord<String, Transaction>("localBalance", tx.getInBankPartition(), tx.getInBank(), BalanceRecord(tx)));
            } else {
                PollFromCredit(tx);
                if (bankBalance.get(tx.getInBank()) >= tx.getAmount()) {
                    bankBalance.compute(tx.getInBank(), (key, value) -> value - tx.getAmount());
                    producer.send(new ProducerRecord<String, Transaction>("credit", tx.getOutBankPartition(), tx.getOutBank(), CompensationRecord(tx)));
                    producer.send(new ProducerRecord<String, Transaction>("successfulTX", tx.getInBankPartition(), tx.getInBank(), tx));
                    producer.send(new ProducerRecord<String, Transaction>("successfulTX", tx.getOutBankPartition(), tx.getOutBank(), CompensationRecord(tx)));
                    producer.send(new ProducerRecord<String, Transaction>("localBalance", tx.getInBankPartition(), tx.getInBank(), BalanceRecord(tx)));
                } else {
                    // If balance is still not enough, reassign the TX.
                    producer.send(new ProducerRecord<String, Transaction>("bigTX", tx.getInBankPartition(), tx.getInBank(), tx));
                    System.out.println("Aggregated transaction suspended. Sent back to big topic.");
                }
            }
        } else {
            PollFromTmp(tx, notPollLocal);
            if (bankBalance.get(tx.getInBank()) >= tx.getAmount()) {
                bankBalance.compute(tx.getInBank(), (key, value) -> value - tx.getAmount());
                producer.send(new ProducerRecord<String, Transaction>("bigTX", tx.getOutBankPartition(), tx.getOutBank(), CompensationRecord(tx)));
                producer.send(new ProducerRecord<String, Transaction>("successfulTX", tx.getInBankPartition(), tx.getInBank(), tx));
                producer.send(new ProducerRecord<String, Transaction>("successfulTX", tx.getOutBankPartition(), tx.getOutBank(), CompensationRecord(tx)));
                producer.send(new ProducerRecord<String, Transaction>("localBalance", tx.getInBankPartition(), tx.getInBank(), BalanceRecord(tx)));
            } else {
                producer.send(new ProducerRecord<String, Transaction>("bigTX", tx.getInBankPartition(), tx.getInBank(), tx));
                System.out.println("Aggregated transaction suspended. Sent back to big topic.");
            }
        }
    }

    private static void ProcessCompensate(Transaction tx, boolean notPollLocal) throws ExecutionException, InterruptedException {
        PollFromTmp(tx, notPollLocal);
        bankBalance.compute(tx.getInBank(), (key, value) -> value + tx.getAmount());
        producer.send(new ProducerRecord<String, Transaction>("localBalance", tx.getInBankPartition(), tx.getInBank(), BalanceRecord(tx)));
    }

    private static void InitBank(Transaction tx) throws ExecutionException, InterruptedException {
        bankBalance.put(tx.getInBank(), tx.getAmount());
        producer.send(new ProducerRecord<String, Transaction>("localBalance", tx.getInBankPartition(), tx.getInBank(), BalanceRecord(tx)));
        producer.send(new ProducerRecord<String, Transaction>("successfulTX", tx.getInBank(), tx));
        //producer.send(new ProducerRecord<String, Transaction>("credit", tx.getInBank(), tx));
        System.out.println("Bank " + tx.getInBank() + " has been initialized to balance " + tx.getAmount() + ".");
    }

    public static String randomString() {
        byte[] array = new byte[32]; // length is bounded by 32
        new Random().nextBytes(array);
        return new String(array, StandardCharsets.UTF_8);
    }
}
