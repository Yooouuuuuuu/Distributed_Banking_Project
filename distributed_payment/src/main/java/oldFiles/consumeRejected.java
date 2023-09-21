package oldFiles;

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

public class consumeRejected {
    public static void main(String[] args) throws InterruptedException {

        //inputs
        String bootstrapServers = "127.0.0.1:9092";
        String schemaRegistryUrl = "http://127.0.0.1:8081";

        Properties propsConsumerAssign = new Properties();
        propsConsumerAssign.put("bootstrap.servers", bootstrapServers);
        propsConsumerAssign.put("isolation.level", "read_committed");
        propsConsumerAssign.put("enable.auto.commit", "false");
        propsConsumerAssign.setProperty("key.deserializer", StringDeserializer.class.getName());
        propsConsumerAssign.setProperty("value.deserializer", KafkaAvroDeserializer.class.getName());
        propsConsumerAssign.setProperty("schema.registry.url", schemaRegistryUrl);
        propsConsumerAssign.setProperty("specific.avro.reader", "true");

        KafkaConsumer<String, Block> consumerFromRej =
                new KafkaConsumer<>(propsConsumerAssign);

        //consumer assign to specific topicPartition
        TopicPartition topicPartition =
                new TopicPartition("rejected", 0);
        consumerFromRej.assign(Arrays.asList(topicPartition));

        //find the latest offset, since that is all we need
        consumerFromRej.seekToEnd(Collections.singleton(topicPartition));
        long latestOffset = consumerFromRej.position(topicPartition);

        //poll data of specific topic partition from beginning to end
        consumerFromRej.seek(topicPartition, 0);

        //init
        long count = 0;
        long timeout = System.currentTimeMillis();

        outerLoop:
        while (true) {
            ConsumerRecords<String, Block> records = consumerFromRej.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, Block> record : records) {
                for (int i = 0; i < record.value().getTransactions().size(); i++) {
                    System.out.println(record.value().getTransactions().get(i));
                    count += 1;
                    timeout = System.currentTimeMillis();
                }
                if (record.offset() == latestOffset-2) {
                    break outerLoop;
                }
            }
            if (System.currentTimeMillis() - timeout > 10000) {
                System.out.println("Nothing in the topic.");
                break;
            }
        }
        System.out.println(count);
    }
}

