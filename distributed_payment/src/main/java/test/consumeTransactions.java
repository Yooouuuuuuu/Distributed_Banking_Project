package test;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import my.avroSchema.Block;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class consumeTransactions {
    static KafkaConsumer<String, Block> consumerFromTransactions;
    public static void main(String[] args) throws InterruptedException, IOException {

        //inputs
        String bootstrapServers = args[0];
        String schemaRegistryUrl = args[1];

        /*
        String bootstrapServers = "127.0.0.1:9092";
        String schemaRegistryUrl = "http://127.0.0.1:8081";

         */

        long firstRecordTime = 0;
        long lastRecordTime = 0;

        //consumer consume from "transactions" topic
        Properties propsConsumer = new Properties();
        propsConsumer.put("bootstrap.servers", bootstrapServers);
        propsConsumer.put("group.id", "test-group" + ThreadLocalRandom.current().nextInt(0, 1000));
        propsConsumer.put("auto.offset.reset", "earliest");
        propsConsumer.put("enable.auto.commit", "false");
        propsConsumer.put("isolation.level", "read_committed");
        //avro part
        propsConsumer.setProperty("key.deserializer", StringDeserializer.class.getName());
        propsConsumer.setProperty("value.deserializer", KafkaAvroDeserializer.class.getName());
        propsConsumer.setProperty("schema.registry.url", schemaRegistryUrl);
        propsConsumer.setProperty("specific.avro.reader", "true");

        String input_topic = "transactions";
        consumerFromTransactions =
                new KafkaConsumer<String, Block>(propsConsumer);
        consumerFromTransactions.subscribe(Collections.singleton(input_topic));

        long timeout = System.currentTimeMillis();
        
        while (true) {
            ConsumerRecords<String, Block> records = consumerFromTransactions.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, Block> record : records) {
                for (int i = 0; i < record.value().getTransactions().size(); i++) {
                    if (record.value().getTransactions().get(i).getSerialNumber() == 1) {
                        firstRecordTime = record.timestamp();
                    } else if (lastRecordTime < record.timestamp()){
                        lastRecordTime = record.timestamp();
                    }
                    //System.out.println(record.value().getTransactions().get(i));
                    timeout = System.currentTimeMillis();
                }
            }
            if (System.currentTimeMillis() - timeout > 10000) {
                break;
            }
        }
        System.out.println("For transactions topic:\nfirst record end at: " + firstRecordTime + "\nlast record end at: " + lastRecordTime);
        System.in.read();
    }
}

