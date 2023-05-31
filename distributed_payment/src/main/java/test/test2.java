package test;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class test2 {
    public static void main(String[] args) throws InterruptedException {

        List<String> list = new ArrayList<>();

        list.add("A");
        list.add("B");
        list.add("C");
        list.add("C");
        list.add("B");
        list.add("A");

        System.out.println(list);

        list.remove(5);
        list.remove(1);

        System.out.println(list);


    }
}

