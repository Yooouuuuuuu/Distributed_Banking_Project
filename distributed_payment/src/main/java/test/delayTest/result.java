package test.delayTest;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import my.avroSchema.Block;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.simple.SimpleLogger;

import java.time.Duration;
import java.util.*;

public class result {
    static KafkaConsumer<String, Block> consumer;


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

        //order topic, record the timestamps of original and UTXO separately
        String inputTopic = "test2";
        consumer =
                new KafkaConsumer<String, Block>(props);
        consumer.subscribe(Collections.singletonList(inputTopic));
        pollFromCredit();
        consumer.close();

    }

    private static void pollFromCredit() {
        long latency = 0L;
        long avgLatency = 0L;
        int count = 0;
        try {
            while(count < 100) {
                ConsumerRecords<String, Block> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, Block> record : records) {
                    latency =  record.timestamp() - record.value().getTransactions().get(0).getTimestamp1();
                    System.out.println("num: " + record.value().getTransactions().get(0).getSerialNumber() +
                            " t1: " + record.value().getTransactions().get(0).getTimestamp1() +
                            " t2: " + record.timestamp() +
                            " latency: " + latency
                    );
                    avgLatency += latency;
                    count += 1;
                }
            }
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
        System.out.println("average latency of 100 data: " + avgLatency);
    }

}
