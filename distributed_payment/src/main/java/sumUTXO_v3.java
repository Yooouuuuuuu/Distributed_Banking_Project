import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import my.avroSchema.AccountInfo;
import my.avroSchema.Block;
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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

public class sumUTXO_v3 {
    static KafkaConsumer<String, Block> consumerFromUTXO;
    static KafkaConsumer<String, AccountInfo> consumerFromAccountInfo;
    static KafkaProducer producer;
    static List<Transaction> listOfUTXO = new ArrayList<Transaction>();
    static HashMap<String, Long> aggUTXO = new HashMap<String, Long>();
    static HashMap<Integer, String> partitionBank = new HashMap<Integer, String>();
    static HashMap<Integer, List<String>> account = new HashMap<>();
    static boolean empty = true;
    public static void main(String[] args) throws Exception {

        //inputs
        String bootstrapServers = "127.0.0.1:9092";
        String schemaRegistryUrl = "http://127.0.0.1:8081";
        int maxPoll = 500;
        long aggUTXOTime = 10000;

        //setups
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "off"); //"off", "trace", "debug", "info", "warn", "error".
        InitConsumer(maxPoll, bootstrapServers, schemaRegistryUrl);
        InitProducer(bootstrapServers, schemaRegistryUrl);
        Logger logger = LoggerFactory.getLogger(sumUTXO_v3.class);
        producer.initTransactions();
        long startTime = System.currentTimeMillis();

        //consume from "UTXO" topic
        while (true) {
            ConsumerRecords<String, Block> records = consumerFromUTXO.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, Block> record : records) {
                logger.info(record.value().toString());
                //aggregate UTXO
                sumTransactions(record.value());
                //once any record comes, set the flag to false
                empty = false;
            }
            // send aggregated UTXO periodically
            if (System.currentTimeMillis() > startTime + aggUTXOTime && !empty) {
                startTime = System.currentTimeMillis();
                //Start atomically transactional write.
                producer.beginTransaction();
                try {
                    //if the block is full (or time out), send it to "block" topic.
                    sendAllBlock();
                    empty = true;
                    producer.commitTransaction();
                } catch (Exception e) {
                    //If aborted, reset every local hashmap and flag.
                    producer.abortTransaction();
                    account = new HashMap<>();
                    partitionBank = new HashMap<>();
                    aggUTXO = new HashMap<String, Long>();
                    empty = true;
                    System.out.println("Tx aborted, credit been reset.");
                    //return;
                }
            }
        }
    }

    private static void InitConsumer(int maxPoll, String bootstrapServers, String schemaRegistryUrl) {
        //consumer consume from "UTXO" topic
        Properties propsConsumerUTXO = new Properties();
        propsConsumerUTXO.put("bootstrap.servers", bootstrapServers);
        propsConsumerUTXO.put("group.id", "UTXO-sum-group");
        propsConsumerUTXO.put("auto.offset.reset", "earliest");
        propsConsumerUTXO.put("enable.auto.commit", "false");
        propsConsumerUTXO.put("isolation.level", "read_committed");
        propsConsumerUTXO.put("max.poll.records", maxPoll);
        //avro part
        propsConsumerUTXO.setProperty("key.deserializer", StringDeserializer.class.getName());
        propsConsumerUTXO.setProperty("value.deserializer", KafkaAvroDeserializer.class.getName());
        propsConsumerUTXO.setProperty("schema.registry.url", schemaRegistryUrl);
        propsConsumerUTXO.setProperty("specific.avro.reader", "true");

        String input_topic = "UTXO";
        consumerFromUTXO =
                new KafkaConsumer<>(propsConsumerUTXO);
        consumerFromUTXO.subscribe(Collections.singletonList(input_topic),
                new ConsumerRebalanceListener() {
                    @Override
                    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                        //System.out.println("onPartitionsRevoked")
                    }
                    @Override
                    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                        //reset hashmaps and flag
                        account = new HashMap<>();
                        partitionBank = new HashMap<>();
                        aggUTXO = new HashMap<String, Long>();
                        empty = true;
                        System.out.println("Rebalanced.");
                    }});

        //consumer consume from "LocalBalance" topic
        Properties propsValidate = new Properties();
        propsValidate.put("bootstrap.servers", bootstrapServers);
        propsValidate.put("group.id", "UTXO-offset-group");
        propsValidate.put("isolation.level", "read_committed");
        propsValidate.put("enable.auto.commit", "false");
        propsValidate.put("fetch.max.bytes", 0);
        propsValidate.setProperty("key.deserializer", StringDeserializer.class.getName());
        propsValidate.setProperty("value.deserializer", KafkaAvroDeserializer.class.getName());
        propsValidate.setProperty("schema.registry.url", schemaRegistryUrl);
        propsValidate.setProperty("specific.avro.reader", "true");
        consumerFromAccountInfo =
                new KafkaConsumer<>(propsValidate);
        //assign to topicPartition later
    }

    private static void InitProducer(String bootstrapServers, String schemaRegistryUrl) {
        Properties propsProducer = new Properties();
        propsProducer.put("bootstrap.servers", bootstrapServers);
        propsProducer.put("transactional.id", randomString()); //Should be different between validators to avoid being fenced due to same transactional.id.
        propsProducer.put("enable.idempotence", "true");
        // avro part
        propsProducer.setProperty("key.serializer", StringSerializer.class.getName());
        propsProducer.setProperty("value.serializer", KafkaAvroSerializer.class.getName());
        propsProducer.setProperty("schema.registry.url", schemaRegistryUrl);
        producer = new KafkaProducer<>(propsProducer);
    }

    public static void sumTransactions(Block recordValue) {
        //process transactions in block
        for (int i = 0; i < recordValue.getTransactions().size(); i++) {
            //check if bank data exist
            if (!partitionBank.containsKey(recordValue.getTransactions().get(i).getInbankPartition())) {
                pollFromAccountInfo(recordValue, i);
            }

            //accumulate transactions to local hashMap
            long gain = recordValue.getTransactions().get(i).getAmount();
            aggUTXO.compute(recordValue.getTransactions().get(i).getInAccount(), (key, value) -> value + gain);
        }
    }

    public static void sendAllBlock() {
        //send aggUTXO to partitions
        for (int i = 0; i < partitionBank.size(); i++) {
            //each aggUTXO including every account's update
            listOfUTXO = new ArrayList<Transaction>();
            for (int j = 0; j < account.get(i).size(); j++) {
                Transaction UTXO = new Transaction(-1L,
                        partitionBank.get(i), account.get(i).get(j),
                        partitionBank.get(i), account.get(i).get(j),
                        i, i,
                        aggUTXO.get(account.get(i).get(j)), 1);
                listOfUTXO.add(UTXO);
                aggUTXO.put(account.get(i).get(j), 0L); //init
            }

            Block output = Block.newBuilder().setTransactions(listOfUTXO).build();
            System.out.println(output);

            //producer send
            if (output.getTransactions().get(0).getAmount() != 0) {
                producer.send(new ProducerRecord<String, Block>("aggUTXO", i, partitionBank.get(i), output));
            }
        }

        //consumer group manually commit
        consumerFromUTXO.commitSync();
        producer.flush();
    }

    private static void pollFromAccountInfo(Block recordValue, int i) {
        //set a reused emptyList
        ArrayList<String> emptyList = new ArrayList<String>();

        //consumer assign to specific topicPartition
        TopicPartition topicPartition =
                new TopicPartition("accountInfo", recordValue.getTransactions().get(i).getInbankPartition());
        consumerFromAccountInfo.assign(Collections.singletonList(topicPartition));

        //find the latest offset, since that is all we need
        consumerFromAccountInfo.seekToEnd(Collections.singleton(topicPartition));
        long latestOffset = consumerFromAccountInfo.position(topicPartition);

        //poll latest data from specific topic partition
        outerLoop:
        while (true) {
            consumerFromAccountInfo.seek(topicPartition, latestOffset);
            latestOffset -= 1;
            ConsumerRecords<String, AccountInfo> accountRecords = consumerFromAccountInfo.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, AccountInfo> accountRecord : accountRecords) {

                account.put(accountRecord.value().getBankPartition(), emptyList);
                partitionBank.put(accountRecord.value().getBankPartition(),
                        accountRecord.value().getBank());
                for (int j = 0; j < accountRecord.value().getAccounts().size(); j++) {
                    aggUTXO.put(accountRecord.value().getAccounts().get(j).getAccountNo(), 0L);
                    account.get(accountRecord.value().getBankPartition())
                            .add(accountRecord.value().getAccounts().get(j).getAccountNo());
                }
                break outerLoop;
            }
        }
    }

    public static String randomString() {
        byte[] array = new byte[32]; // length is bounded by 32
        new Random().nextBytes(array);
        return new String(array, StandardCharsets.UTF_8);
    }
}
