import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class balancer {
    static HashMap<String, Long> bankBalance = new HashMap<String, Long>();
    static KafkaConsumer<String, Transaction> consumerFromSuccessful;
    static KafkaConsumer<String, Transaction> consumerFromBalance;
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
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "off"); //"off", "trace", "debug", "info", "warn", "error".
        InitConsumer(Integer.parseInt(args[2]), args[8]);
        InitProducer(args[8]);
        Logger logger = LoggerFactory.getLogger(balancer.class);
        producer.initTransactions();


        //consume from successful
        while (true) {
            ConsumerRecords<String, Transaction> records = consumerFromSuccessful.poll(Duration.ofMillis(100));
            producer.beginTransaction();        //start atomically transaction
            try {
                for (ConsumerRecord<String, Transaction> record : records) {
                    logger.info("InBank: " + record.value().getInBank() + " ,OutBank: " + record.value().getOutBank() + " ,Value: " + record.value().getAmount() + " ,Offset:" + record.offset());
                    if (record.value().getCategory() != 3) {
                        PollFromBalance(record.value());
                        Process(record.value(), numOfTX);
                    }else if (record.value().getCategory() == 3) {
                        Process(record.value(), numOfTX);
                        System.out.println("Bank " + record.value().getInBank() + " has been initialized to balance " + bankBalance.get(record.value().getInBank()) + ". (balancer)");
                    }
                }
                consumerFromSuccessful.commitSync();
                producer.commitTransaction();
            } catch ( Exception e ) {
                producer.abortTransaction();//end atomically transaction
                bankBalance = new HashMap<String, Long>();
                System.out.println("Tx aborted, bankBalance been reset. (balancer)");
                //return;
            }
        }
    }


    private static void InitConsumer(int maxPoll, String bootstrapServers) {
        //consumer consume from successful
        Properties propsConsumerTx = new Properties();
        propsConsumerTx.put("bootstrap.servers", bootstrapServers);
        propsConsumerTx.put("group.id", "balancer-main-group");
        propsConsumerTx.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        propsConsumerTx.put("value.deserializer", "TxDeserializer");
        propsConsumerTx.put("isolation.level", "read_committed");
        propsConsumerTx.put("enable.auto.commit", "false");
        propsConsumerTx.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        propsConsumerTx.put("max.poll.records", maxPoll);

        String input_topic = "successfulTX";
        consumerFromSuccessful =
                new KafkaConsumer<String, Transaction>(propsConsumerTx);
        consumerFromSuccessful.subscribe(Collections.singletonList(input_topic),
                new ConsumerRebalanceListener() {
                    @Override
                    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                        //System.out.println("onPartitionsRevoked")
                    }
                    @Override
                    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                        System.out.println("bankBalance before rebalance: " + bankBalance + " (balancer)");
                        bankBalance = new HashMap<String, Long>();
                    }});


        //consumer consume from balance
        Properties propsConsumerLocalBalance = new Properties();
        propsConsumerLocalBalance.put("bootstrap.servers", bootstrapServers);
        propsConsumerLocalBalance.put("group.id", "balance-group");
        propsConsumerLocalBalance.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        propsConsumerLocalBalance.put("value.deserializer", "TxDeserializer");
        propsConsumerLocalBalance.put("isolation.level", "read_committed");
        propsConsumerLocalBalance.put("enable.auto.commit", "false");
        propsConsumerLocalBalance.put("fetch.max.bytes", 0);
        consumerFromBalance =
                new KafkaConsumer<String, Transaction>(propsConsumerLocalBalance);
    }

    private static void InitProducer(String bootstrapServers) {
        //producer produce to balance
        Properties propsTXWrite = new Properties();
        propsTXWrite.put("bootstrap.servers", bootstrapServers);
        propsTXWrite.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        propsTXWrite.put("value.serializer", "TxSerializer");
        propsTXWrite.put("transactional.id", randomString()); //Should be different between validators to avoid being fenced due to same transactional.id.
        propsTXWrite.put("enable.idempotence", "true");
        propsTXWrite.put("max.block.ms", "1000");
        propsTXWrite.put("transaction.timeout.ms", "600000");
        producer = new KafkaProducer<>(propsTXWrite);
    }

    private static void PollFromBalance(Transaction tx) {
        if (!bankBalance.containsKey(tx.getInBank())) {
            TopicPartition topicPartition = new TopicPartition("balance", tx.getInBankPartition());
            consumerFromBalance.assign(List.of(topicPartition));
            consumerFromBalance.seekToEnd(Collections.singleton(topicPartition));
            long latestOffset = consumerFromBalance.position(topicPartition);
            boolean findingLast = true;
            while (findingLast) {
                consumerFromBalance.seek(topicPartition, latestOffset);
                latestOffset -= 1;
                ConsumerRecords<String, Transaction> balanceRecords = consumerFromBalance.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, Transaction> balanceRecord : balanceRecords) {
                    bankBalance.compute(tx.getInBank(), (key, value) -> balanceRecord.value().getAmount());
                    findingLast = false;
                }
            }
        }
    }

    private static Transaction Record(Transaction tx) {
        return new Transaction(tx.getInBank(), "000", bankBalance.get(tx.getInBank()), tx.getSerialNumber(), -1, -1, -1);
    }

    private static void Process(Transaction tx, int numOfTX) throws ExecutionException, InterruptedException {
        //System.out.println(tx.getSerialNumber());
        if (tx.getCategory() == 3) {
            bankBalance.compute(tx.getInBank(), (key, value) -> tx.getAmount());
            producer.send(new ProducerRecord<String, Transaction>("balance", tx.getInBankPartition(), tx.getInBank(), Record(tx)));
        }else if (tx.getCategory() == 0 || tx.getCategory() == 1){
            bankBalance.compute(tx.getInBank(), (key, value) -> value - tx.getAmount());
            producer.send(new ProducerRecord<String, Transaction>("balance", tx.getInBankPartition(), tx.getInBank(), Record(tx)));
        }else if (tx.getCategory() == 2){
            bankBalance.compute(tx.getInBank(), (key, value) -> value + tx.getAmount());
            producer.send(new ProducerRecord<String, Transaction>("balance", tx.getInBankPartition(), tx.getInBank(), Record(tx)));
        }
        if (tx.getSerialNumber() == numOfTX) {
            System.out.println("Test finished.");
        }
    }

    public static String randomString() {
        byte[] array = new byte[32]; // length is bounded by 32
        new Random().nextBytes(array);
        return new String(array, StandardCharsets.UTF_8);
    }
}
