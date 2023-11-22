package test.delayTest;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.slf4j.simple.SimpleLogger;

import java.util.*;

public class resetTopics {
    public static void main(String[] args) throws Exception {

        //inputs
        String bootstrapServers = args[0];
        int numOfPartitions = Integer.parseInt(args[1]);
        short numOfReplicationFactor = Short.parseShort(args[2]);

        // log setting
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "off"); //"off", "trace", "debug", "info", "warn", "error".

        // create AdminClient
        Properties adminProps = new Properties();
        adminProps.put("bootstrap.servers", bootstrapServers);
        AdminClient adminClient = KafkaAdminClient.create(adminProps);

        // delete topics
        adminClient.deleteTopics(Arrays.asList(
                "test1", "test2"));

        // create topics
        String topic_name1 = "test1";
        NewTopic topic_01 = new NewTopic(topic_name1, numOfPartitions, numOfReplicationFactor);
        String topic_name2 = "test2";
        NewTopic topic_02 = new NewTopic(topic_name2, numOfPartitions, numOfReplicationFactor);

        Thread.sleep(10000); //wait 10 sec in case that the topic deletion is late
        CreateTopicsResult result = adminClient.createTopics(Arrays.asList(topic_01, topic_02));

        // check if topic created successfully
        for (Map.Entry entry : result.values().entrySet()) {
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
    }
}


