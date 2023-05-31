package test;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import my.avroSchema.Block;
import my.avroSchema.Transaction;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.simple.SimpleLogger;

import java.util.*;

public class test {
    public static void main(String[] args) throws InterruptedException {

        int numOfPartitions = 3;
        short numOfReplicationFactor = 1;
        String bootstrapServers = "127.0.0.1:9092";
        String schemaRegistryUrl = "http://127.0.0.1:8081";
        long initBalance = 1000000L;
        int numOfAccount = 10;
/*
        //props
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "off"); //"off", "trace", "debug", "info", "warn", "error".
        Properties adminProps = new Properties();
        adminProps.put("bootstrap.servers", bootstrapServers);
        AdminClient adminClient = KafkaAdminClient.create(adminProps);

        // delete topics
        adminClient.deleteTopics(Arrays.asList("test"));
        //System.in.read();

        // create topics
        String topic_name1 = "test";
        NewTopic topic_01 = new NewTopic(topic_name1, numOfPartitions, numOfReplicationFactor);

        Thread.sleep(10000); //wait 10 sec in case that the topic deletion is late
        CreateTopicsResult result = adminClient.createTopics(Arrays.asList(topic_01));

        // check if topic created successfully
        for (Map.Entry entry : result.values().entrySet()) {
            String topic_name = (String) entry.getKey();
            boolean success = true;
            String error_msg = "";
            try {
                ((KafkaFuture<Void>) entry.getValue()).get();
            } catch (Exception e) {
                success = false;
                error_msg = e.getMessage();
            }
            if (success)
                System.out.println("Topic: " + topic_name + " creation completed!");
            else
                System.out.println("Topic: " + topic_name + " creation fail, due to [" + error_msg + "]");
        }
        adminClient.close();

        */
        // initialize kafka producer
        Properties propsProducer = new Properties();
        propsProducer.put("bootstrap.servers", bootstrapServers);
        propsProducer.put("enable.idempotence", "true");
        propsProducer.put("max.block.ms", "1000");
        propsProducer.setProperty("key.serializer", StringSerializer.class.getName());
        propsProducer.setProperty("value.serializer", IntegerSerializer.class.getName());

        Producer<String, Integer> producer = new KafkaProducer<>(propsProducer);

        // create and send init balances
        for (int partitionNum = 0; partitionNum < numOfPartitions; partitionNum++) {
            //producer.send(new ProducerRecord<String, Integer>("test", partitionNum, "10" + partitionNum + "0001", 100));
            //producer.send(new ProducerRecord<String, Integer>("test", partitionNum, "10" + partitionNum + "0002", 100));
            producer.send(new ProducerRecord<String, Integer>("test", partitionNum, "10" + partitionNum + "0003", 10000));
        }
        producer.flush();
        producer.close();
        System.out.println("Bank balance has been initialized.");
    }
}

