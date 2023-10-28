package test;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import my.avroSchema.Block;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.simple.SimpleLogger;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

public class tps {
    static KafkaConsumer<String, Block> consumer;
    static long firstRecordTime = 9999999999999L;
    static long lastUTXOTime = 0L;
    static long numOfPayments = 0L;



    public static void main(String[] args) throws Exception {

        String bootstrapServers = args[0];
        String schemaRegistryUrl = args[1];

        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "off");//"off", "trace", "debug", "info", "warn", "error"
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", UUID.randomUUID().toString());
        props.put("auto.offset.reset", "earliest");

        props.setProperty("key.deserializer", StringDeserializer.class.getName());
        props.setProperty("value.deserializer", KafkaAvroDeserializer.class.getName());
        props.setProperty("schema.registry.url", schemaRegistryUrl);
        props.setProperty("specific.avro.reader", "true");

        //order topic, record the timestamps of original and UTXO separately
        String inputTopic = "order";
        consumer =
                new KafkaConsumer<String, Block>(props);
        consumer.subscribe(Collections.singletonList(inputTopic));

        consumeOriginal();
        consumer.close();



        //transaction topic, find the timestamp of the first data
        inputTopic = "transactions";
        consumer =
                new KafkaConsumer<String, Block>(props);
        consumer.subscribe(Collections.singleton(inputTopic));
        findFirstTimestamp();
        consumer.close();

        calculateTPS();
    }

    private static void consumeOriginal() {

        long startTime = System.currentTimeMillis();

        try {
            while (startTime + (100000) > System.currentTimeMillis()) { //100s
                ConsumerRecords<String, Block> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, Block> record : records) {
                    if (record.timestamp() > lastUTXOTime) {
                        lastUTXOTime = record.timestamp();
                    }
                    for (int i = 0; i < record.value().getTransactions().size(); i++) {
                        if (record.value().getTransactions().get(i).getCategory() == 1) {
                            numOfPayments += 1;
                        }
                    }
                }
            }
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static void findFirstTimestamp() {
        long timeout = System.currentTimeMillis() + 10000;

        try {
            while (true) {
                ConsumerRecords<String, Block> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, Block> record : records) {
                    for (int i = 0; i < record.value().getTransactions().size(); i++) {
                        if (record.value().getTransactions().get(i).getSerialNumber() == 1) {
                            //there might have more than one data which SerialNumber() == 1
                            // if there are more than one generator
                            if (firstRecordTime > record.timestamp()) {
                                firstRecordTime = record.timestamp();
                            }
                        }
                        //System.out.println(record.value().getTransactions().get(i));
                        timeout = System.currentTimeMillis();
                    }
                }
                if (System.currentTimeMillis() > timeout) {
                    break;
                }
            }
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static void calculateTPS() {
        float TPS = (float) (numOfPayments / ((lastUTXOTime - firstRecordTime) / 1000));
        System.out.println("num of payments done: " + numOfPayments + "\nTPS: " + TPS);
    }

}
