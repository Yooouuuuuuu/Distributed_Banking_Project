package test.others;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import my.avroSchema.Block;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.simple.SimpleLogger;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class consumeOrder {
    static KafkaConsumer<String, Block> consumerFromOrder;
    static HashMap<String, Long> bankBalance = new HashMap<String, Long>();

    public static void main(String[] args) throws InterruptedException, IOException {

        //for order topic with multiple partitions, and separate records from in and out

        //inputs
        String bootstrapServers = args[0];
        String schemaRegistryUrl = args[1];
        int numOfAccount = Integer.parseInt(args[2]);
        int partition = Integer.parseInt(args[3]);

        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "off");//"off", "trace", "debug", "info", "warn", "error"

        long firstRecordTime = 9999999999999L;
        long lastRecordTime = 0;

        //consumer consume from "transactions" topic
        Properties propsConsumer = new Properties();
        propsConsumer.put("bootstrap.servers", bootstrapServers);
        propsConsumer.put("group.id", "test-group" + ThreadLocalRandom.current().nextInt(0, 1000));
        propsConsumer.put("auto.offset.reset", "earliest");
        propsConsumer.put("enable.auto.commit", "false");
        propsConsumer.put("isolation.level", "read_committed");
        //avro part
        propsConsumer.setProperty("key.deserializer", StringDeserializer.class.getName());
        propsConsumer.setProperty("value.deserializer", KafkaAvroDeserializer.class.getName());
        propsConsumer.setProperty("schema.registry.url", schemaRegistryUrl);
        propsConsumer.setProperty("specific.avro.reader", "true");

        consumerFromOrder =
                new KafkaConsumer<String, Block>(propsConsumer);

        TopicPartition topicPartition =
                new TopicPartition("order", partition);
        consumerFromOrder.assign(Collections.singletonList(topicPartition));


        //init record also goes into order topic, thus start at numOfPartitions*numOfAccount
        long count = -numOfAccount;
        System.out.println(count);

        long timeout = System.currentTimeMillis();

        while (true) {
            ConsumerRecords<String, Block> records = consumerFromOrder.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, Block> record : records) {
                for (int i = 0; i < record.value().getTransactions().size(); i++) {
                    //calculate bank balance
                    long amount = record.value().getTransactions().get(i).getAmount();
                    if (record.value().getTransactions().get(i).getSerialNumber() != 0) {
                        if (record.value().getTransactions().get(i).getCategory() == 0) {
                            bankBalance.compute(record.value().getTransactions().get(i).getOutAccount(),
                                    (key, value) -> value - amount);
                        } else if (record.value().getTransactions().get(i).getCategory() == 1) {
                            bankBalance.compute(record.value().getTransactions().get(i).getInAccount(),
                                    (key, value) -> value + amount);
                        }
                    } else {
                        // if serial number == 0, it is the data for init
                        bankBalance.put(record.value().getTransactions().get(i).getOutAccount(), amount);
                    }

                    //record timestamps
                    if (record.timestamp() < firstRecordTime) {
                        firstRecordTime = record.timestamp();
                    } else if (record.timestamp() > lastRecordTime) {
                        lastRecordTime = record.timestamp();
                    }

                    //count successful records
                    count += 1;

                    //reset timeout for breaking while loop
                    timeout = System.currentTimeMillis();
                }
            }
            if (System.currentTimeMillis() - timeout > 10000) {
                break;
            }
        }
        System.out.println("bank balance: " + bankBalance);
        System.out.println("successful records counts: " + count);
        System.out.println("For order topic:\nfirst record end at: " + firstRecordTime + "\nlast record end at: " + lastRecordTime);
        System.in.read();
    }
}


