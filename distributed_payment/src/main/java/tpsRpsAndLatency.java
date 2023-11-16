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

public class tpsRpsAndLatency {
    static KafkaConsumer<String, Block> consumer;
    static List<Long> latency = new ArrayList<>();
    static long numOfTrades = 0L;


    public static void main(String[] args) throws Exception {

        String bootstrapServers = args[0];
        String schemaRegistryUrl = args[1];
        int executeTime = Integer.parseInt(args[2]);

        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "off");//"off", "trace", "debug", "info", "warn", "error"
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", UUID.randomUUID().toString());
        props.put("auto.offset.reset", "earliest");
        props.put("max.poll.records", 1000);

        props.setProperty("key.deserializer", StringDeserializer.class.getName());
        props.setProperty("value.deserializer", KafkaAvroDeserializer.class.getName());
        props.setProperty("schema.registry.url", schemaRegistryUrl);
        props.setProperty("specific.avro.reader", "true");

        //order topic, record the timestamps of original and UTXO separately
        String inputTopic = "order";
        consumer =
                new KafkaConsumer<String, Block>(props);
        consumer.subscribe(Collections.singletonList(inputTopic));
        pollFromCredit();
        consumer.close();

        long numOfTradesComplete = latency.size();
        long ninetyFivePercent =  Math.round(numOfTradesComplete * 0.95)-1;
        long ninetyNinePercent =  Math.round(numOfTradesComplete * 0.99)-1;
        float TPS = (float) (numOfTradesComplete / executeTime);
        latency.sort(null);



        //transaction topic, find the timestamp of the first data
        inputTopic = "transactions";
        consumer =
                new KafkaConsumer<String, Block>(props);
        consumer.subscribe(Collections.singleton(inputTopic));
        findTrades();
        consumer.close();

        float RPS = (float) (numOfTrades / executeTime);

        System.out.println(
                "\nnum of trade complete: " + numOfTradesComplete +
                "\nTPS: " + TPS +
                "\ntop 95% latency: " + latency.get(Math.toIntExact(ninetyFivePercent)) +
                "\ntop 99% latency: " + latency.get(Math.toIntExact(ninetyNinePercent))
        );

        String a = TPS + ", " +
                latency.get(Math.toIntExact(ninetyFivePercent)) + ", " +
                latency.get(Math.toIntExact(ninetyNinePercent));
        
        List<String> newLines = new ArrayList<>();
        Path path = Paths.get("/home/nsd/liang_you_git_repo/Distributed_Banking_Project/scripts/result/result.txt");
        if (Files.readAllLines(path, StandardCharsets.UTF_8).size() == 0) {
            newLines.add("TPS, top 95% latency, top 99% latency");
            newLines.add(a);
        } else {
            newLines.add(a);
        }

        Files.write(path, newLines, StandardCharsets.UTF_8,  StandardOpenOption.APPEND);

    }

    private static void pollFromCredit() {
        long timeout = System.currentTimeMillis() + 100000; //100s;

        try {
            do {
                ConsumerRecords<String, Block> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, Block> record : records) {
                    for (int i = 0; i < record.value().getTransactions().size(); i++) {
                        if (record.value().getTransactions().get(i).getCategory() == 1) {
                            latency.add(record.timestamp() - record.value().getTransactions().get(i).getTimestamp1());
                        }
                    }
                }
            } while (System.currentTimeMillis() <= timeout);
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static void findTrades() {
        long timeout = System.currentTimeMillis() + 100000; //100s;

        try {
            do {
                ConsumerRecords<String, Block> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, Block> record : records) {
                    for (int i = 0; i < record.value().getTransactions().size(); i++) {
                        if (record.value().getTransactions().get(i).getCategory() == 0) {
                            numOfTrades += 1;
                        }
                        timeout = System.currentTimeMillis();
                    }
                }
            } while (System.currentTimeMillis() <= timeout);
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
