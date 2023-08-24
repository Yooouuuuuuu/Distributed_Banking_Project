import io.confluent.kafka.serializers.KafkaAvroSerializer;
import my.avroSchema.Block;
import my.avroSchema.Transaction;
import org.apache.commons.math3.distribution.ZipfDistribution;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.simple.SimpleLogger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class sourceProducerZipf {
    static long rejectedCount = 0;

    public static void main(String[] args) throws IOException {

        //inputs
        String bootstrapServers = args[0];
        String schemaRegistryUrl = args[1];
        int numOfPartitions = Integer.parseInt(args[2]);
        int numOfAccounts = Integer.parseInt(args[3]);
        long initBalance = Long.parseLong(args[5]);
        long numOfData = Long.parseLong(args[10]); //sourceProducer only
        long amountPerTransaction = Long.parseLong(args[11]); //sourceProducer only
        boolean randomAmount = Boolean.parseBoolean(args[16]);
        float zipfExponent = Float.parseFloat(args[18]);

        //setups
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "off"); //"off", "trace", "debug", "info", "warn", "error".

        //create producer
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", bootstrapServers);
        properties.setProperty("acks", "all");
        properties.setProperty("retries", "10");
        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer", KafkaAvroSerializer.class.getName());
        properties.setProperty("schema.registry.url", schemaRegistryUrl);
        KafkaProducer<String, Block> producer = new KafkaProducer<String, Block>(properties);

        //create banks (100, 101, 102 etc) & accounts
        ArrayList<String> bankList = new ArrayList<String>();
        HashMap<String, Long> bankBalance = new HashMap<String, Long>();
        List<String> allAccounts = new ArrayList<String>();
        int banks = 0;
        String account;
        while(banks < numOfPartitions){
            bankList.add("10" + banks); //100, 101, 102, etc...
            for (int accountNum = 1; accountNum <= numOfAccounts; accountNum++) {
                if (accountNum < 10) {
                    account =  "10" + banks + "000" + accountNum; //1000001
                }else if (accountNum < 100){
                    account =  "10" + banks + "00" + accountNum; //1000010
                }else if (accountNum < 1000){
                    account =  "10" + banks + "0" + accountNum; //1000100
                }else {
                    account = "10" + banks + accountNum; //1001000
                }
                bankBalance.put(account, initBalance);
                allAccounts.add(account);
            }
            banks += 1;
        }
        Collections.shuffle(allAccounts);
        System.out.println(allAccounts);
        String[] accountList = allAccounts.toArray(new String[0]);

        //zipf distribution setting
        ZipfDistribution zipfDistribution = new ZipfDistribution(accountList.length - 1, zipfExponent);

        //sending random data
        for (long i = 1L; i <= numOfData; i++) {

            int out= zipfDistribution.sample();
            String outAccount = accountList[out];
            String outBank = outAccount.substring(0, 3);

            int in = zipfDistribution.sample();
            String inAccount = accountList[in];
            String inBank = inAccount.substring(0, 3);

            if (randomAmount) {
                amountPerTransaction = ThreadLocalRandom.current().nextInt(1000, 10000);
            } //else use the value args give

            //build block
            Transaction detail = new Transaction(i,
                    outBank, outAccount,
                    inBank, inAccount,
                    bankList.indexOf(outBank), bankList.indexOf(inBank), amountPerTransaction, 0);
            List<Transaction> listOfDetail = new ArrayList<Transaction>();
            listOfDetail.add(detail);

            Block output = Block.newBuilder()
                    .setTransactions(listOfDetail)
                    .build();

            //send
            producer.send(new ProducerRecord<String, Block>("transactions",
                    bankList.indexOf(outBank),
                    outBank,
                    output));
            if (bankBalance.get(outAccount) - amountPerTransaction >= 0) {
                bankBalance.compute(outAccount, (key, value) -> value - detail.getAmount());
                bankBalance.compute(inAccount, (key, value) -> value + detail.getAmount());
            } else {
                rejectedCount += 1;
            }
        }

        //flush and close producer
        producer.flush();
        producer.close();

        //print result
        System.out.println("bank balance: " + bankBalance);
        System.out.println("rejected count: " + rejectedCount);
        //This bankBalance is for reference only,
        //if any transaction has been rejected, here shows the linearization result,
        //however our system is not even serialization.

        System.in.read();

    }
}
