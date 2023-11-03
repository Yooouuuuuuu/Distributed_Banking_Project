package test;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import my.avroSchema.Block;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.simple.SimpleLogger;

import java.time.Duration;
import java.util.*;

public class test3 {
    static KafkaConsumer<String, Block> consumer;
    static List<Long> latency = new ArrayList<>();
    static long numOfTrades = 0L;


    public static void main(String[] args) throws Exception {

        String bootstrapServers = "140.119.164.32:9092";
        String schemaRegistryUrl = "http://140.119.164.32:8081";

        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "off");//"off", "trace", "debug", "info", "warn", "error"
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", UUID.randomUUID().toString());
        props.put("auto.offset.reset", "earliest");
        props.put("max.poll.records", 1000);

        props.setProperty("key.deserializer", StringDeserializer.class.getName());
        props.setProperty("value.deserializer", KafkaAvroDeserializer.class.getName());
        props.setProperty("schema.registry.url", schemaRegistryUrl);
        props.setProperty("specific.avro.reader", "true");

        //transaction topic, find the timestamp of the first data
        String inputTopic = "UTXO";
        consumer =
                new KafkaConsumer<String, Block>(props);
        consumer.subscribe(Collections.singleton(inputTopic));
        findTrades();
        consumer.close();

    }

    private static void findTrades() {
        long timeout = System.currentTimeMillis() + 100000; //100s;

        try {
            while(true) {
                ConsumerRecords<String, Block> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, Block> record : records) {
                    for (int i = 0; i < record.value().getTransactions().size(); i++) {
                        if (record.value().getTransactions().get(i).getCategory() != 2) {
                            System.out.println("category: " + record.value().getTransactions().get(i).getCategory() +
                                    ", outbank: " + record.value().getTransactions().get(i).getOutAccount() +
                                    ", inbank: " + record.value().getTransactions().get(i).getInAccount());
                        }
                    }
                }
            }
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
