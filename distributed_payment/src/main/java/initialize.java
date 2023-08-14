import io.confluent.kafka.serializers.KafkaAvroSerializer;
import my.avroSchema.Account;
import my.avroSchema.AccountInfo;
import my.avroSchema.Block;
import my.avroSchema.Transaction;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.simple.SimpleLogger;
import java.util.*;

public class initialize {
    public static void main(String[] args) throws Exception {

        //inputs
        String bootstrapServers = args[0];
        String schemaRegistryUrl = args[1];
        int numOfPartitions = Integer.parseInt(args[2]);
        int numOfAccounts = Integer.parseInt(args[3]);
        short numOfReplicationFactor = Short.parseShort(args[4]);
        long initBalance = Long.parseLong(args[5]);
        boolean successfulMultiplePartition = Boolean.parseBoolean(args[14]);
        boolean UTXODoNotAgg = Boolean.parseBoolean(args[15]);
        String log = args[17];

        // log setting
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, log); //"off", "trace", "debug", "info", "warn", "error".

        // create AdminClient
        Properties adminProps = new Properties();
        adminProps.put("bootstrap.servers", bootstrapServers);
        AdminClient adminClient = KafkaAdminClient.create(adminProps);

        // delete topics
        adminClient.deleteTopics(Arrays.asList("transactions", "blocks", "successful", "rejected", "localBalance", "UTXO", "aggUTXO", "aggUTXOOffset", "accountInfo", "UTXOOffset"));

        // create topics
        String topic_name1 = "blocks";
        NewTopic topic_01 = new NewTopic(topic_name1, numOfPartitions, numOfReplicationFactor);

        String topic_name2 = "localBalance";
        Map<String, String> configs = new HashMap<>();
        configs.put("cleanup.policy","compact");
        configs.put("min.cleanable.dirty.ratio","0.5"); //default: 0.5
        configs.put("max.compaction.lag.ms", "60000"); //default: 9223372036854775807
        configs.put("delete.retention.ms","100");
        configs.put("segment.ms","100");
        NewTopic topic_02 = new NewTopic(topic_name2, numOfPartitions, numOfReplicationFactor);
        topic_02.configs(configs); //spacial configs for specific topic

        String topic_name3 = "successful";
        NewTopic topic_03;
        String topic_name4 = "rejected";
        NewTopic topic_04;


        if (successfulMultiplePartition) {
            topic_03 = new NewTopic(topic_name3, numOfPartitions, numOfReplicationFactor);
            topic_04 = new NewTopic(topic_name4, numOfPartitions, numOfReplicationFactor);
        } else {
            topic_03 = new NewTopic(topic_name3, 1, numOfReplicationFactor); //for serialization
            topic_04 = new NewTopic(topic_name4, 1, numOfReplicationFactor);
        }

        String topic_name5 = "transactions";
        NewTopic topic_05 = new NewTopic(topic_name5, numOfPartitions, numOfReplicationFactor);

        //sometimes changed
        String topic_name6 = "UTXO";
        NewTopic topic_06 = new NewTopic(topic_name6, numOfPartitions, numOfReplicationFactor);
        String topic_name7 = "UTXOOffset";
        NewTopic topic_07 = new NewTopic(topic_name7, numOfPartitions, numOfReplicationFactor);

        if (UTXODoNotAgg) {
            Thread.sleep(10000); //wait 10 sec in case that the topic deletion is late
            CreateTopicsResult result = adminClient.createTopics(Arrays.asList(topic_01, topic_02, topic_03, topic_04, topic_05, topic_06, topic_07));

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

        }else {
            topic_06 = new NewTopic(topic_name6, 1, numOfReplicationFactor); //optional on partition
            String aggUTXOOffset = "aggUTXOOffset";
            topic_07 = new NewTopic(aggUTXOOffset, numOfPartitions, numOfReplicationFactor);
            String topic_name8 = "aggUTXO";
            NewTopic topic_08 = new NewTopic(topic_name8, numOfPartitions, numOfReplicationFactor);
            String topic_name9 = "accountInfo";
            NewTopic topic_09 = new NewTopic(topic_name9, numOfPartitions, numOfReplicationFactor);

            Thread.sleep(10000); //wait 10 sec in case that the topic deletion is late
            CreateTopicsResult result = adminClient.createTopics(Arrays.asList(topic_01, topic_02, topic_03, topic_04, topic_05, topic_06, topic_07, topic_08, topic_09));

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

        // init data
        // initialize kafka producer
        Properties propsProducer = new Properties();
        propsProducer.put("bootstrap.servers", bootstrapServers);
        propsProducer.put("enable.idempotence", "true");
        propsProducer.put("max.block.ms", "1000");
        // avro part
        propsProducer.setProperty("key.serializer", StringSerializer.class.getName());
        propsProducer.setProperty("value.serializer", KafkaAvroSerializer.class.getName());
        propsProducer.setProperty("schema.registry.url", schemaRegistryUrl);

        KafkaProducer producer = new KafkaProducer<>(propsProducer);

        // create and send init balances
        for (int partitionNum = 0; partitionNum < numOfPartitions; partitionNum++) {
            List<Transaction> listOfDetail = new ArrayList<Transaction>();
            List<Account> listOfAccount = new ArrayList<>();
            for (int accountNum = 1; accountNum <= numOfAccounts; accountNum++) {
                if (accountNum < 10) {
                    // Accounts init balance
                    Transaction detail = new Transaction(0L,
                            "10" + partitionNum, "10" + partitionNum + "000" + accountNum,
                            "10" + partitionNum, "10" + partitionNum + "000" + accountNum,
                            partitionNum, partitionNum, initBalance, 2);
                    listOfDetail.add(detail);

                    // save account info to
                    Account account = new Account("10" + partitionNum + "000" + accountNum);
                    listOfAccount.add(account);

                }else if (accountNum < 100){
                    Transaction detail = new Transaction(0L,
                            "10" + partitionNum, "10" + partitionNum + "00" + accountNum,
                            "10" + partitionNum, "10" + partitionNum + "00" + accountNum,
                            partitionNum, partitionNum, initBalance, 2);
                    listOfDetail.add(detail);

                    Account account = new Account("10" + partitionNum + "00" + accountNum);
                    listOfAccount.add(account);
                }else if (accountNum < 1000){
                    Transaction detail = new Transaction(0L,
                            "10" + partitionNum, "10" + partitionNum + "0" + accountNum,
                            "10" + partitionNum, "10" + partitionNum + "0" + accountNum,
                            partitionNum, partitionNum, initBalance, 2);
                    listOfDetail.add(detail);

                    Account account = new Account("10" + partitionNum + "0" + accountNum);
                    listOfAccount.add(account);
                }else{
                    Transaction detail = new Transaction(0L,
                            "10" + partitionNum, "10" + partitionNum + accountNum,
                            "10" + partitionNum, "10" + partitionNum + accountNum,
                            partitionNum, partitionNum, initBalance, 2);
                    listOfDetail.add(detail);

                    Account account = new Account("10" + partitionNum + accountNum);
                    listOfAccount.add(account);
                }
            }

            //send init block & account info
            Block initialize = Block.newBuilder()
                    .setTransactions(listOfDetail)
                    .build();
            producer.send(new ProducerRecord<String, Block>("blocks",
                    partitionNum,
                    "10"+partitionNum,
                    initialize));

            AccountInfo info = AccountInfo.newBuilder()
                    .setBank("10" + partitionNum)
                    .setBankPartition(partitionNum)
                    .setAccounts(listOfAccount)
                    .build();
            producer.send(new ProducerRecord<String, AccountInfo>("accountInfo",
                    partitionNum,
                    "10"+partitionNum,
                    info));

        }
        producer.flush();
        producer.close();
        System.out.println("Bank balance has been initialized.");
    }
}


