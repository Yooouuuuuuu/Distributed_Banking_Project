package test;

import my.avroSchema.Block;
import my.avroSchema.Transaction;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class test2 {
    public static void main(String[] args) throws InterruptedException {

        String a = "123" + ThreadLocalRandom.current().nextInt(0, 100);;
        System.out.println(a);



    }
}

