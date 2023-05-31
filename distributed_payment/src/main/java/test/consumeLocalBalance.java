package test;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import my.avroSchema.LocalBalance;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.*;

public class consumeLocalBalance {
    public static void main(String[] args) throws InterruptedException {

        //inputs
        String bootstrapServers = "127.0.0.1:9092";
        String schemaRegistryUrl = "http://127.0.0.1:8081";
        int numOfPartitions = 3;
        int maxPoll = 500;
        int blockSize = 500;
        long blockTimeout = 10000;

        HashMap<String, Long> bankBalance = new HashMap<String, Long>();


        // the three consumers below using the same property
        Properties propsConsumerAssign = new Properties();
        propsConsumerAssign.put("bootstrap.servers", bootstrapServers);
        propsConsumerAssign.put("isolation.level", "read_committed");
        propsConsumerAssign.put("enable.auto.commit", "false");
        //propsConsumerAssign.put("group.id", "???-group");

        propsConsumerAssign.setProperty("key.deserializer", StringDeserializer.class.getName());
        propsConsumerAssign.setProperty("value.deserializer", KafkaAvroDeserializer.class.getName());
        propsConsumerAssign.setProperty("schema.registry.url", schemaRegistryUrl);
        propsConsumerAssign.setProperty("specific.avro.reader", "true");
        //propsConsumerAssign.put("auto.offset.reset", "earliest");



        //consumer consume from "localBalance" topic
        KafkaConsumer<String, LocalBalance> consumerFromLocalBalance =
                new KafkaConsumer<>(propsConsumerAssign);



        //consumer assign to specific topicPartition
        TopicPartition topicPartition =
                new TopicPartition("localBalance", 0);
        consumerFromLocalBalance.assign(Arrays.asList(topicPartition));

        //find the latest offset, since that is all we need
        consumerFromLocalBalance.seekToEnd(Collections.singleton(topicPartition));
        long latestOffset = consumerFromLocalBalance.position(topicPartition);
        boolean lastOffset = false;
        System.out.println(latestOffset);

        //poll data of specific topic partition from beginning to end
        consumerFromLocalBalance.seek(topicPartition, 0);

        outerLoop:
        while (true) {
            ConsumerRecords<String, LocalBalance> balanceRecords = consumerFromLocalBalance.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, LocalBalance> balanceRecord : balanceRecords) {
                //System.out.println(balanceRecord);
                bankBalance.compute(balanceRecord.key(), (key, value)
                        -> balanceRecord.value().getBalance());

                System.out.println(balanceRecord.offset());
                if (balanceRecord.offset() == latestOffset-2) {
                    break outerLoop;
                }
            }
        }
        System.out.println(bankBalance);
    }
}

