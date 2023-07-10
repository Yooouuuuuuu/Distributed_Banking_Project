package test;

import com.google.common.util.concurrent.RateLimiter;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.subject.RecordNameStrategy;
import my.avroSchema.Block;
import my.avroSchema.Transaction;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class test3 {
    static KafkaProducer producer;
    static KafkaProducer producer2;
    public static void main(String[] args) throws InterruptedException {

        String bootstrapServers = "127.0.0.1:9092";
        String schemaRegistryUrl = "http://127.0.0.1:8081";
        int numOfPartitions = 3;
        int maxPoll = 500;
        long UTXOUpdatePeriod = 10000;
        int UTXOUpdateBreakTime = 1000;

        InitProducer(bootstrapServers, schemaRegistryUrl);

        //thread 1
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                producer.initTransactions();
                while (true) {
                    producer.beginTransaction();

                    //build block
                    Transaction detail = new Transaction(0L,
                            "t1", "t1",
                            "t1", "t1",
                            0, 0, 0L, 0);
                    List<Transaction> listOfDetail = new ArrayList<Transaction>();
                    listOfDetail.add(detail);

                    Block output = Block.newBuilder()
                            .setTransactions(listOfDetail)
                            .build();
                    producer.send(new ProducerRecord<String, Block>("transactions", output));

                    try {
                        producer.commitTransaction();
                    } catch (Exception e) {
                        producer.abortTransaction();
                    }
                }
            }
        });

        //thread 2
        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                producer2.initTransactions();
                while (true) {
                    producer2.beginTransaction();

                    //build block
                    Transaction detail = new Transaction(0L,
                            "t2", "t2",
                            "t2", "t2",
                            0, 0, 0L, 0);
                    List<Transaction> listOfDetail = new ArrayList<Transaction>();
                    listOfDetail.add(detail);

                    Block output = Block.newBuilder()
                            .setTransactions(listOfDetail)
                            .build();
                    producer2.send(new ProducerRecord<String, Block>("transactions", output));

                    try {
                        producer2.commitTransaction();
                    } catch (Exception e) {
                        producer2.abortTransaction();
                    }
                }
            }
        });

        t1.start();
        t2.start();
    }

    private static void InitProducer(String bootstrapServers, String schemaRegistryUrl) {

        Properties propsProducer = new Properties();
        propsProducer.put("bootstrap.servers", bootstrapServers);
        propsProducer.put("transactional.id", "Main");
        propsProducer.put("transaction.timeout.ms", 300000);
        propsProducer.put("enable.idempotence", "true");
        propsProducer.put("max.block.ms", "1000");
        // avro part
        propsProducer.setProperty("key.serializer", StringSerializer.class.getName());
        propsProducer.setProperty("value.serializer", KafkaAvroSerializer.class.getName());
        propsProducer.setProperty("schema.registry.url", schemaRegistryUrl);
        propsProducer.put("value.subject.name.strategy", RecordNameStrategy.class.getName());
        producer = new KafkaProducer<>(propsProducer);

        Properties propsProducer2 = new Properties();
        propsProducer2.put("bootstrap.servers", bootstrapServers);
        propsProducer2.put("transactional.id", "UTXO");
        propsProducer2.put("transaction.timeout.ms", 300000);
        propsProducer2.put("enable.idempotence", "true");
        propsProducer2.put("max.block.ms", "1000");
        // avro part
        propsProducer2.setProperty("key.serializer", StringSerializer.class.getName());
        propsProducer2.setProperty("value.serializer", KafkaAvroSerializer.class.getName());
        propsProducer2.setProperty("schema.registry.url", schemaRegistryUrl);
        propsProducer2.put("value.subject.name.strategy", RecordNameStrategy.class.getName());
        producer2 = new KafkaProducer<>(propsProducer2);
    }
}

