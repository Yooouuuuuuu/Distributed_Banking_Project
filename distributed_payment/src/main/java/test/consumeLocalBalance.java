package test;


import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import my.avroSchema.LocalBalance;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.*;

public class consumeLocalBalance {
    static KafkaConsumer<String, LocalBalance> consumerFromLocalBalance;
    public static void main(String[] args) throws InterruptedException, IOException {

        //inputs
        String bootstrapServers = args[0];
        String schemaRegistryUrl = args[1];

        /*
        String bootstrapServers = "127.0.0.1:9092";
        String schemaRegistryUrl = "http://127.0.0.1:8081";
         */

        long lastRecordTime = 0;
        HashMap<String, Long> bankBalance = new HashMap<String, Long>();

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

        String input_topic = "localBalance";
        consumerFromLocalBalance =
                new KafkaConsumer<String, LocalBalance>(propsConsumer);
        consumerFromLocalBalance.subscribe(Collections.singleton(input_topic));

        long timeout = System.currentTimeMillis();

        outerLoop:
        while (true) {
            ConsumerRecords<String, LocalBalance> balanceRecords = consumerFromLocalBalance.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, LocalBalance> balanceRecord : balanceRecords) {
                //System.out.println(balanceRecord);
                bankBalance.compute(balanceRecord.key(), (key, value)
                        -> balanceRecord.value().getBalance());
                if (balanceRecord.timestamp() > lastRecordTime){
                    lastRecordTime = balanceRecord.timestamp();
                }
                timeout = System.currentTimeMillis();
            }
            if (System.currentTimeMillis() - timeout > 10000) {
                break;
            }
        }
        System.out.println("bank balance: " + bankBalance);
        System.out.println("For localBalance topic:\nlast record end at: " + lastRecordTime);
        System.in.read();
    }
}

