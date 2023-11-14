package test;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import my.avroSchema.Block;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.simple.SimpleLogger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.*;

public class test4 {



    public static void main(String[] args) throws Exception {

        float a = 1.1F;
        float b = 2.1F;
        float c = 2000000000000.99F;
        String f = a + ", " + b + ", " + c;
        System.out.println(f);

        List<String> newLines = new ArrayList<>();
        Path path = Paths.get("/home/yooouuuuuuu/project/test.txt");
        if (Files.readAllLines(path, StandardCharsets.UTF_8).size() == 0) {
            newLines.add("RPS, TPS, top 99% latency");
            newLines.add("123, 123, 123");
        } else {
            newLines.add(f);
        }

        Files.write(path, newLines, StandardCharsets.UTF_8,  StandardOpenOption.APPEND);

    }
}
