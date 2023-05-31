package test;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class updateLocalBalance {
    public static void main(String[] args) throws IOException {


        // setting properties

        final Properties streamsProps = new Properties();
        streamsProps.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        streamsProps.setProperty(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE);
        streamsProps.setProperty(StreamsConfig.TOPOLOGY_OPTIMIZATION, StreamsConfig.OPTIMIZE);
        streamsProps.put(StreamsConfig.APPLICATION_ID_CONFIG, "ktable-application");
        StreamsBuilder builder = new StreamsBuilder();
        String inputTopic = "test";
        String outputTopic = "test1";

        KTable<String, Integer> firstKTable =
                builder.table(inputTopic, Consumed.with(Serdes.String(), Serdes.Integer()))
                        .mapValues((key, value) -> value);
        firstKTable
                .toStream().peek((key, value) -> System.out.println("key: " + key + " value: " + value))
                .to(outputTopic, Produced.with(Serdes.String(), Serdes.Integer()));



        try (KafkaStreams kafkaStreams = new KafkaStreams(builder.build(), streamsProps)) {
            final CountDownLatch shutdownLatch = new CountDownLatch(1);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                kafkaStreams.close(Duration.ofSeconds(2));
                shutdownLatch.countDown();
            }));
            //TopicLoader.runProducer();
            try {
                kafkaStreams.start();
                shutdownLatch.await();
            } catch (Throwable e) {
                System.exit(1);
            }
        }
        System.exit(0);

    }
}
