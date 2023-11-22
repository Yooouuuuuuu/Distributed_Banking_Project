package test.delayTest;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import my.avroSchema.Block;
import my.avroSchema.Transaction;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.simple.SimpleLogger;

import java.time.Duration;
import java.util.*;

public class pollAndSend {

    static KafkaConsumer<String, Block> consumerFromTransactions;
    static Producer<String, Block> producer;
    static ArrayList<List<Transaction> > listOfListOfTransactions = new ArrayList<List<Transaction> >();
    static ArrayList<Integer> listOfCounts = new ArrayList<Integer>();
    static HashMap<String, Long> bankTime = new HashMap<String, Long>();
    static HashMap<Integer, String> bankPartition = new HashMap<>();
    static HashMap<Integer, Long> partitionOffset = new HashMap<>();
    public static void main(String[] args) throws Exception {

        //inputs
        String bootstrapServers = args[0];
        String schemaRegistryUrl = args[1];
        int numOfPartitions = Integer.parseInt(args[2]);
        int maxPoll = Integer.parseInt(args[3]);

        //setups
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "off"); //"off", "trace", "debug", "info", "warn", "error".
        InitConsumer(maxPoll, bootstrapServers, schemaRegistryUrl, numOfPartitions);
        InitProducer(bootstrapServers, schemaRegistryUrl, "test");
        producer.initTransactions();

        //poll from "transactions" topic
        while (true) {
            ConsumerRecords<String, Block> records = consumerFromTransactions.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, Block> record : records) {
                producer.beginTransaction(); //Start atomically transactional write.
                try {
                    addTimestampAndSend(record.value(), record);
                    producer.commitTransaction();
                } catch (Exception e) {
                    producer.abortTransaction();
                    System.out.println("Tx aborted.");
                }
            }
        }
    }

    private static void InitConsumer(int maxPoll, String bootstrapServers, String schemaRegistryUrl, int numOfPartitions) {
        //consumer consume from "transactions" topic
        Properties propsConsumer = new Properties();
        propsConsumer.put("bootstrap.servers", bootstrapServers);
        propsConsumer.put("group.id", "aggregator-group");
        propsConsumer.put("auto.offset.reset", "earliest");
        propsConsumer.put("enable.auto.commit", "false");
        propsConsumer.put("isolation.level", "read_committed");
        propsConsumer.put("max.poll.records", maxPoll);
        //avro part
        propsConsumer.setProperty("key.deserializer", StringDeserializer.class.getName());
        propsConsumer.setProperty("value.deserializer", KafkaAvroDeserializer.class.getName());
        propsConsumer.setProperty("schema.registry.url", schemaRegistryUrl);
        propsConsumer.setProperty("specific.avro.reader", "true");

        String input_topic = "test1";
        consumerFromTransactions =
                new KafkaConsumer<String, Block>(propsConsumer);
        consumerFromTransactions.subscribe(Collections.singleton(input_topic),
                new ConsumerRebalanceListener() {
                    @Override
                    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                        //System.out.println("onPartitionsRevoked");
                    }
                    @Override
                    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                        //initialize block
                        listOfListOfTransactions.clear();
                        listOfCounts.clear();
                        List<Transaction> listOfTransactions;
                        for (int i = 0; i < numOfPartitions; i++) {
                            listOfTransactions = new ArrayList<Transaction>();
                            listOfListOfTransactions.add(listOfTransactions);
                            listOfCounts.add(0);
                        }
                        System.out.println("aggregator is rebalanced. Initialize lists of blocks and counts.");
                    }});
    }

    private static void InitProducer(String bootstrapServers, String schemaRegistryUrl, String transactionalId) {
        Properties propsProducer = new Properties();
        propsProducer.put("bootstrap.servers", bootstrapServers);
        propsProducer.put("transactional.id", transactionalId);
        propsProducer.put("enable.idempotence", "true");
        //propsProducer.put("max.block.ms", "1000");

        //avro part
        propsProducer.setProperty("key.serializer", StringSerializer.class.getName());
        propsProducer.setProperty("value.serializer", KafkaAvroSerializer.class.getName());
        propsProducer.setProperty("schema.registry.url", schemaRegistryUrl);
        producer = new KafkaProducer<>(propsProducer);
    }

    public static void addTimestampAndSend(Block recordValue, ConsumerRecord record) {
        //add transaction to current block
        recordValue.getTransactions().get(0).put("timestamp1", record.timestamp());
        producer.send(new ProducerRecord<String, Block>("test2", record.partition(), (String) record.key(), recordValue));
    }
}


