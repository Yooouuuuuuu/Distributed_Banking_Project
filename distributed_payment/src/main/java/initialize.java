import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.*;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.KafkaFuture;

public class initialize {
    public static void main(String[] args) throws Exception {

        /*
        args[0]: # of partitions
        args[1]: # of transactions
        args[2]: "max.poll.records"
        args[3]: batch processing
        args[4]: poll from localBalance while repartition
        args[5]: credit topic exist
        args[6]: direct write to successful
        args[7]: one partition only, skip balancer.
        args[8]: bootstrap.servers
        */

        //inputs
        int numOfPartitions = Integer.parseInt(args[0]);
        short numOfReplicationFactor = 1;
        String bootstrapServers = args[8];

        //props
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "off"); //"off", "trace", "debug", "info", "warn", "error".
        Properties adminProps = new Properties();
        adminProps.put("bootstrap.servers", bootstrapServers);
        AdminClient adminClient = KafkaAdminClient.create(adminProps);

        // delete topics
        adminClient.deleteTopics(Arrays.asList("smallTX", "bigTX", "localBalance", "successfulTX", "rejectedTX", "balance", "credit", "validateOffset", "aggregatedCredit"));
        //System.in.read();

        // create topics
        String topic_name1 = "bigTX";
        NewTopic topic_01 = new NewTopic(topic_name1, numOfPartitions, numOfReplicationFactor);

        String topic_name2 = "localBalance";
        Map<String, String> configs = new HashMap<>();
        configs.put("cleanup.policy","compact");
        configs.put("delete.retention.ms","100");
        configs.put("segment.ms","100");
        configs.put("min.cleanable.dirty.ratio","0.01");
        NewTopic topic_02 = new NewTopic(topic_name2, numOfPartitions, numOfReplicationFactor);
        topic_02.configs(configs); //spacial configs for specific topic

        String topic_name3 = "successfulTX";
        NewTopic topic_03 = new NewTopic(topic_name3, numOfPartitions, numOfReplicationFactor);
        String topic_name4 = "rejectedTX";
        NewTopic topic_04 = new NewTopic(topic_name4, numOfPartitions, numOfReplicationFactor);
        String topic_name5 = "balance";
        NewTopic topic_05 = new NewTopic(topic_name5, numOfPartitions, numOfReplicationFactor);
        String topic_name6 = "smallTX";
        NewTopic topic_06 = new NewTopic(topic_name6, numOfPartitions, numOfReplicationFactor);
        String topic_name7 = "credit";
        NewTopic topic_07 = new NewTopic(topic_name7, numOfPartitions, numOfReplicationFactor);
        String topic_name8 = "validateOffset";
        NewTopic topic_08 = new NewTopic(topic_name8, numOfPartitions, numOfReplicationFactor);
        String topic_name9 = "aggregatedCredit";
        NewTopic topic_09 = new NewTopic(topic_name9, numOfPartitions, numOfReplicationFactor);
        Thread.sleep(10000); //wait 10 sec in case that the topic deletion is late
        CreateTopicsResult result = adminClient.createTopics(Arrays.asList(topic_01, topic_02, topic_03, topic_04, topic_05, topic_06, topic_07, topic_08, topic_09));

        // check if topic created successfully
        for(Map.Entry entry : result.values().entrySet()) {
            String topic_name = (String) entry.getKey();
            boolean success = true;
            String error_msg = "";
            try {
                ((KafkaFuture<Void>) entry.getValue()).get();
            } catch (Exception e) {
                success = false;
                error_msg = e.getMessage();
            }
            if (success)
                System.out.println("Topic: " + topic_name + " creation completed!");
            else
                System.out.println("Topic: " + topic_name + " creation fail, due to [" + error_msg + "]");
        }
        adminClient.close();

        // initialize
        Properties producerProps = new Properties();
        producerProps.put("bootstrap.servers", bootstrapServers);
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer", "TxSerializer");
        Producer<String, Transaction> producer = new KafkaProducer<>(producerProps);

        int partitionNum = 0;
        Transaction transaction = new Transaction("822", "000", 100000000L, -1, 0, -1, 3);

        while (numOfPartitions > 0) { //numOfPartitions < 10
            transaction = new Transaction("10"+partitionNum, "000", 10000000000L, -1, partitionNum, -1, 3);
            producer.send(new ProducerRecord<String, Transaction>("bigTX", transaction.getInBankPartition(), transaction.getInBank(), transaction)).get();

            partitionNum +=1;
            numOfPartitions -=1;
        }

        System.out.println("Bank balance has been initialized.");
    }
}


