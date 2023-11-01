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
    static long numOfTrades = 0L;
    static long numOfTradesComplete = 0L;



    public static void main(String[] args) throws Exception {

        String bootstrapServers = args[0];
        String schemaRegistryUrl = args[1];
        int executeTime = Integer.parseInt(args[2]);


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
        lastCredit();
        consumer.close();



        //transaction topic, find the timestamp of the first data
        inputTopic = "transactions";
        consumer =
                new KafkaConsumer<String, Block>(props);
        consumer.subscribe(Collections.singleton(inputTopic));
        findFirstTimestamp();
        consumer.close();

        float RPS = (float) (numOfTrades / executeTime);
        System.out.println("num of trade: " + numOfTrades + "\nTPS: " + RPS);
        float TPS = (float) (numOfTradesComplete / executeTime);
        System.out.println("num of trade complete: " + numOfTradesComplete + "\nTPS: " + TPS);

    }

    private static void lastCredit() {
        long timeout = System.currentTimeMillis() + 100000; //100s;

        try {
            do {
                ConsumerRecords<String, Block> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, Block> record : records) {
                    for (int i = 0; i < record.value().getTransactions().size(); i++) {
                        if (record.value().getTransactions().get(i).getCategory() == 1) {
                            numOfTradesComplete += 1;
                        }
                    }
                }
            } while (System.currentTimeMillis() <= timeout);
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }


    private static void findFirstTimestamp() {
        long timeout = System.currentTimeMillis() + 100000; //100s;

        try {
            do {
                ConsumerRecords<String, Block> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, Block> record : records) {
                    for (int i = 0; i < record.value().getTransactions().size(); i++) {
                        if (record.value().getTransactions().get(i).getCategory() == 0) {
                            numOfTrades += 1;
                        }
                        //System.out.println(record.value().getTransactions().get(i));
                        timeout = System.currentTimeMillis();
                    }
                }
            } while (System.currentTimeMillis() <= timeout);
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
