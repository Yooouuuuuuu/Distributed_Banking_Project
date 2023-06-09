package test;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import my.avroSchema.Block;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class consumeAggUTXO {
    static KafkaConsumer<String, Block> consumerFromAgg;

    public static void main(String[] args) throws InterruptedException {

        //inputs
        String bootstrapServers = "127.0.0.1:9092";
        String schemaRegistryUrl = "http://127.0.0.1:8081";
        int maxPoll = 500;
        long firstRecordTime = 0;
        long lastRecordTime = 0;

        //consumer consume from "transactions" topic
        Properties propsConsumer = new Properties();
        propsConsumer.put("bootstrap.servers", bootstrapServers);
        propsConsumer.put("group.id", "test-group" + ThreadLocalRandom.current().nextInt(0, 1000));
        propsConsumer.put("auto.offset.reset", "earliest");
        propsConsumer.put("enable.auto.commit", "false");
        propsConsumer.put("isolation.level", "read_committed");
        propsConsumer.put("max.poll.records", maxPoll);
        //avro part
        propsConsumer.setProperty("key.deserializer", StringDeserializer.class.getName());
        propsConsumer.setProperty("value.deserializer", KafkaAvroDeserializer.class.getName());
        propsConsumer.setProperty("schema.registry.url", schemaRegistryUrl);
        propsConsumer.setProperty("specific.avro.reader", "true");

        String input_topic = "aggUTXO";
        consumerFromAgg =
                new KafkaConsumer<String, Block>(propsConsumer);
        consumerFromAgg.subscribe(Collections.singleton(input_topic));

        long timeout = System.currentTimeMillis();
        HashMap<Integer, Boolean> partitionNotEmpty = new HashMap<>();


        outerLoop:
        while (true) {
            ConsumerRecords<String, Block> records = consumerFromAgg.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, Block> record : records) {
                for (int i = 0; i < record.value().getTransactions().size(); i++) {
                    if (!partitionNotEmpty.containsKey(record.partition())) {
                        partitionNotEmpty.put(record.partition(), true);
                    }
                    System.out.println(record.partition());
                    timeout = System.currentTimeMillis();
                }
            }
            if (System.currentTimeMillis() - timeout > 10000) {
                System.out.println("Nothing in the topic.");
                break;
            }
        }
        System.out.println(partitionNotEmpty);
    }
}

