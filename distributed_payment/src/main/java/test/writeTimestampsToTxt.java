package test;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import my.avroSchema.Block;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.simple.SimpleLogger;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.*;

public class writeTimestampsToTxt {
    static KafkaConsumer<String, Block> consumer;
    static HashMap<String, Long> newNumberMap = new HashMap<>();


    public static void main(String[] args) throws Exception {


        String bootstrapServers = args[0];
        String schemaRegistryUrl = args[1];
        long tokensPerSec = Long.parseLong(args[2]);
        long executionTime = Long.parseLong(args[3]);
        String log = args[4];
        String outputTxt1 = args[5];
        String outputTxt2 = args[6];
        String outputTxt3 = args[7];

        long numOfTX = 2*tokensPerSec*(executionTime/1000);

        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, log);//"off", "trace", "debug", "info", "warn", "error"
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", UUID.randomUUID().toString());
        props.put("auto.offset.reset", "earliest");

        props.setProperty("key.deserializer", StringDeserializer.class.getName());
        props.setProperty("value.deserializer", KafkaAvroDeserializer.class.getName());
        props.setProperty("schema.registry.url", schemaRegistryUrl);
        props.setProperty("specific.avro.reader", "true");

        //transaction topic, find the timestamp of the first data
        String inputTopic = "transactions";
        consumer =
                new KafkaConsumer<String, Block>(props);
        consumer.subscribe(Collections.singleton(inputTopic));
        findFirstTimestamp(outputTxt1);
        consumer.close();

        //order topic, record the timestamps of original and UTXO separately
        inputTopic = "order";
        consumer =
                new KafkaConsumer<String, Block>(props);
        consumer.subscribe(Collections.singletonList(inputTopic));

        consumeOriginal(outputTxt2, numOfTX);
        consumer.close();


        consumer =
                new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(inputTopic));
        consumer.poll(0);  // without this, the assignment will be empty.
        consumer.assignment().forEach(t -> {
            //System.out.printf("Set %s to offset 0%n", t.toString());
            consumer.seek(t, 0);
        });

        consumerUTXO(outputTxt3, numOfTX);
        consumer.close();




    }
    private static void findFirstTimestamp(String filename) throws FileNotFoundException {
        long timeout = System.currentTimeMillis() + 10000;
        long firstRecordTime = 9999999999999L;

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

        PrintWriter writer = new PrintWriter(filename);

        writer.println("first timestamp of transactions topic");
        writer.println(firstRecordTime);

        writer.flush();
        writer.close();

        System.out.println(filename + " is written complete.");

    }

    private static void consumeOriginal(String filename, long numOfTX) throws FileNotFoundException {

        long newNumber;
        String type;
        long count = 1L;
        PrintWriter writer = new PrintWriter(filename);
        long startTime = System.currentTimeMillis();


        while (startTime + (numOfTX / 100) > System.currentTimeMillis()) { //might have to set bigger if input increase
            ConsumerRecords<String, Block> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, Block> record : records) {
                for (int i = 0; i < record.value().getTransactions().size(); i++) {
                    if (record.value().getTransactions().get(i).getSerialNumber() != 0L) {
                        if (record.value().getTransactions().get(i).getCategory() == 0) { //original data
                            newNumber = count;
                            newNumberMap.put(
                                    record.value().getTransactions().get(i).getOutbank() +
                                            record.value().getTransactions().get(i).getSerialNumber(),
                                    count);
                            type = "successful";
                            writer.println(newNumber);
                            writer.println(type);
                            writer.println(record.timestamp());
                            //Since we will use multiple sourceProducer in different machine to simulate individual
                            //banks sending request (in the same sourceProducer, the outBanks will also be the same).
                            //However, this makes transactions' serial number not unique, we have to give them
                            //new number to calculate latency later.

                            //The first 3 letters of the key represent the outBank of the tx,
                            //after that is its original serial number.
                            count += 1;
                        } else if (record.value().getTransactions().get(i).getCategory() == 0) {
                            newNumber = count;
                            newNumberMap.put(
                                    record.value().getTransactions().get(i).getOutbank() +
                                            record.value().getTransactions().get(i).getSerialNumber(),
                                    count);
                            type = "rejected";
                            writer.println(newNumber);
                            writer.println(type);
                            writer.println(record.timestamp());
                        }
                    }
                }
            }
        }

        writer.flush();
        writer.close();
        System.out.println(filename + " is written complete.");
    }

    private static void consumerUTXO(String filename, long numOfTX) throws FileNotFoundException {
        long newNumber;
        String type;
        PrintWriter writer = new PrintWriter(filename);
        long startTime = System.currentTimeMillis();

        while (startTime + (numOfTX / 100) > System.currentTimeMillis()) { //might have to set bigger if input increase
            ConsumerRecords<String, Block> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, Block> record : records) {
                for (int i = 0; i < record.value().getTransactions().size(); i++) {
                    if (record.value().getTransactions().get(i).getSerialNumber() != 0L) {
                        if (record.value().getTransactions().get(i).getCategory() == 1) { //UTXO
                            newNumber = newNumberMap.get(
                                    record.value().getTransactions().get(i).getOutbank() +
                                    record.value().getTransactions().get(i).getSerialNumber()
                            );
                            type = "UTXO";
                            writer.println(newNumber);
                            writer.println(type);
                            writer.println(record.timestamp());
                        }
                    }
                }
            }
        }
        writer.flush();
        writer.close();
        System.out.println(filename + " is written complete.");
    }

}
