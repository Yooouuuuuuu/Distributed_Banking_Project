import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class aggregate_credit {
    static HashMap<String, Long> aggregatedCredit = new HashMap<String, Long>();
    static HashMap<String, Long> creditOffset = new HashMap<String, Long>();
    static HashMap<String, Boolean> reset = new HashMap<String, Boolean>();
    static KafkaConsumer<String, Transaction> consumerFromCredit;
    static KafkaConsumer<String, Transaction> consumerFromValidate;
    static KafkaConsumer<String, Transaction> consumerFromAgg;
    static Producer<String, Transaction> producer;

    public static void main(String[] args) throws Exception {

        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "off"); //"off", "trace", "debug", "info", "warn", "error".
        InitConsumer(Integer.parseInt(args[0]));
        InitProducer();
        Logger logger = LoggerFactory.getLogger(aggregate_credit.class);
        producer.initTransactions();

        //consume from credit
        while (true) {
            ConsumerRecords<String, Transaction> records = consumerFromCredit.poll(Duration.ofMillis(100));
            producer.beginTransaction();        //start atomically transaction
            try {
                for (ConsumerRecord<String, Transaction> record : records) {
                    logger.info("Bank: " + record.value().getOutBank() + " ,Value: " + record.value().getAmount() + " ,Offset:" + record.offset());
                    if (record.value().getCategory() != 3) {
                        System.out.println("1");
                        System.out.println(reset.get(record.key()));
                        //PollFromValidateOffset(record.value());
                        if (!reset.get(record.key())) {
                            System.out.println("2");

                            Process(record.value());
                            System.out.println("aggregatedCredit:" + aggregatedCredit);
                            System.out.println("creditOffset:" + creditOffset);
                            System.out.println("reset:" + reset);
                        }else { //if the aggregatedCredit topic had been poll, roll back the offset.
                            System.out.println("3");

                            TopicPartition tp = new TopicPartition("credit", record.value().getInBankPartition());
                            consumerFromCredit.seek(tp, record.value().getAmount());
                            reset.put(record.key(), false);
                            System.out.println("seeking the last polled TopicPartition.");
                        }
                    }else{
                        //first credit of the bank
                        reset.put(record.key(), false);
                        aggregatedCredit.put(record.key(), 0L);
                        creditOffset.put(record.key(), 1L);
                        System.out.println(record.key());
                        System.out.println("Credit of bank " + record.value().getInBank() + " has been initialized.");
                    }
                }
                consumerFromCredit.commitSync();
                producer.commitTransaction();
                //System.out.println("commitSync");

            } catch ( Exception e ) {
                producer.abortTransaction();//end atomically transaction
                aggregatedCredit = new HashMap<String, Long>();
                System.out.println("Tx aborted, credit been reset.");
                //return;
            }
        }
    }


    private static void InitConsumer(int args) {
        //consumer consume from successful
        Properties propsCredit = new Properties();
        propsCredit.put("bootstrap.servers", "localhost:9092");
        propsCredit.put("group.id", "credit-main-group");
        propsCredit.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        propsCredit.put("value.deserializer", "TxDeserializer");
        propsCredit.put("isolation.level", "read_committed");
        propsCredit.put("enable.auto.commit", "false");
        propsCredit.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        propsCredit.put("max.poll.records", args);

        String input_topic = "credit";
        consumerFromCredit =
                new KafkaConsumer<String, Transaction>(propsCredit);
        consumerFromCredit.subscribe(Collections.singletonList(input_topic),
                new ConsumerRebalanceListener() {
                    @Override
                    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                        //System.out.println("onPartitionsRevoked")
                    }
                    @Override
                    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                        System.out.println("credit before rebalance: " + aggregatedCredit);
                        aggregatedCredit = new HashMap<String, Long>();
                        creditOffset = new HashMap<String, Long>();
                        reset = new HashMap<String, Boolean>();
                    }});

        //consumer consume from Validate
        Properties propsValidate = new Properties();
        propsValidate.put("bootstrap.servers", "localhost:9092");
        propsValidate.put("group.id", "validate-offset-group");
        propsValidate.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        propsValidate.put("value.deserializer", "TxDeserializer");
        propsValidate.put("isolation.level", "read_committed");
        propsValidate.put("enable.auto.commit", "false");
        propsValidate.put("fetch.max.bytes", 0);
        consumerFromValidate =
                new KafkaConsumer<String, Transaction>(propsValidate);

        //consumer consume from Validate
        Properties propsAgg = new Properties();
        propsAgg.put("bootstrap.servers", "localhost:9092");
        propsAgg.put("group.id", "agg-group");
        propsAgg.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        propsAgg.put("value.deserializer", "TxDeserializer");
        propsAgg.put("isolation.level", "read_committed");
        propsAgg.put("enable.auto.commit", "false");
        propsAgg.put("fetch.max.bytes", 0);
        consumerFromAgg =
                new KafkaConsumer<String, Transaction>(propsAgg);
    }

    private static void InitProducer() {
        //producer produce to balance
        Properties propsTxWrite = new Properties();
        propsTxWrite.put("bootstrap.servers", "localhost:9092");
        propsTxWrite.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        propsTxWrite.put("value.serializer", "TxSerializer");
        propsTxWrite.put("transactional.id", UUID.randomUUID().toString()); //Should be different between validators to avoid being fenced due to same transactional.id.
        propsTxWrite.put("enable.idempotence", "true");
        propsTxWrite.put("max.block.ms", "1000");
        producer = new KafkaProducer<>(propsTxWrite);
    }

    private static void PollFromValidateOffset(Transaction tx) {
        TopicPartition topicPartition = new TopicPartition("validateOffset", tx.getInBankPartition());
        consumerFromValidate.assign(List.of(topicPartition));
        consumerFromValidate.seekToEnd(Collections.singleton(topicPartition));
        long latestOffset = consumerFromValidate.position(topicPartition);
        boolean findingLast = true;
        while (findingLast) {
            consumerFromValidate.seek(topicPartition, latestOffset);
            latestOffset -= 1;
            //System.out.println(consumerFromValidate.position(topicPartition));
            if (latestOffset > 0) {
                ConsumerRecords<String, Transaction> Records = consumerFromValidate.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, Transaction> Record : Records) {
                    //credit.compute(tx.getInBank(), (key, value) -> balanceRecord.value().getAmount());

                    if (Record.offset() < creditOffset.get(tx.getInBank())) {
                        creditOffset.put(tx.getInBank(), Record.value().getAmount());
                        reset.put(tx.getInBank(), true);

                        System.out.println("Resetting credit of " + tx.getInBank() + " .");
                    }
                }
            }
            findingLast = false;
        }
    }

    private static Transaction Record(Transaction tx) {
        return new Transaction(tx.getInBank(), "000", aggregatedCredit.get(tx.getInBank()), -1, tx.getInBankPartition(), -1, -1);
    }

    private static void PollFromAgg(Transaction tx) {
        if (!aggregatedCredit.containsKey(tx.getInBank()) || Objects.equals(tx.getInBank(), "000")) {  // If tx.getInBank() =="000", Tx aborted, rollback to previous local balance.
            TopicPartition topicPartition = new TopicPartition("aggregatedCredit", tx.getInBankPartition());
            consumerFromAgg.assign(List.of(topicPartition));
            consumerFromAgg.seekToEnd(Collections.singleton(topicPartition));
            long latestOffset = consumerFromAgg.position(topicPartition);
            boolean findingLast = true;
            while (findingLast) {
                consumerFromAgg.seek(topicPartition, latestOffset);
                latestOffset -= 1;
                //System.out.println(consumerFromLocalBalance.position(topicPartition));
                ConsumerRecords<String, Transaction> balanceRecords = consumerFromAgg.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, Transaction> balanceRecord : balanceRecords) {
                    aggregatedCredit.compute(tx.getInBank(), (key, value) -> balanceRecord.value().getAmount());
                    //System.out.println(tx.getInBank() + " now have " + bankBalance.get(tx.getInBank()));
                    findingLast = false;
                }
            }
        }
    }

    private static void Process(Transaction tx) throws ExecutionException, InterruptedException {
        PollFromAgg(tx);
        aggregatedCredit.compute(tx.getInBank(), (key, value) -> value + tx.getAmount());
        producer.send(new ProducerRecord<String, Transaction>("aggregatedCredit", tx.getInBankPartition(), tx.getInBank(), Record(tx)));
        }
    }
