package test;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import my.avroSchema.Block;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;

public class consumeSuccessful {
    public static void main(String[] args) throws InterruptedException {

        //inputs
        String bootstrapServers = "127.0.0.1:9092";
        String schemaRegistryUrl = "http://127.0.0.1:8081";
        int numOfPartitions = 3;
        int maxPoll = 500;
        int blockSize = 500;
        long blockTimeout = 10000;
        int numOfAccount = 10;

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
        KafkaConsumer<String, Block> consumerFromLocalBalance =
                new KafkaConsumer<>(propsConsumerAssign);



        //consumer assign to specific topicPartition
        TopicPartition topicPartition =
                new TopicPartition("successful", 0);
        consumerFromLocalBalance.assign(Arrays.asList(topicPartition));

        //find the latest offset, since that is all we need
        consumerFromLocalBalance.seekToEnd(Collections.singleton(topicPartition));
        long latestOffset = consumerFromLocalBalance.position(topicPartition);

        //poll data of specific topic partition from beginning to end
        consumerFromLocalBalance.seek(topicPartition, 0);

        //init record also goes into successful topic
        long count = -(numOfPartitions*numOfAccount);
        System.out.println(count);

        outerLoop:
        while (true) {
            ConsumerRecords<String, Block> records = consumerFromLocalBalance.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, Block> record : records) {
                System.out.println("Block starts:");
                for (int i = 0; i < record.value().getTransactions().size(); i++) {
                    System.out.println(record.value().getTransactions().get(i));
                    count += 1;
                }
                if (record.offset() == latestOffset-2) {
                    break outerLoop;
                }
            }
        }
        System.out.println(count);
    }
}

