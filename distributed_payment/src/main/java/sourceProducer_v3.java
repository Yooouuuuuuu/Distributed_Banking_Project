import io.confluent.kafka.serializers.KafkaAvroSerializer;
import my.avroSchema.Block;
import my.avroSchema.Transaction;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.simple.SimpleLogger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class sourceProducer_v3 {
    static String outAccount;
    static String inAccount;

    public static void main(String[] args) {

        /*
        args[0]: # of partitions
        args[1]: # of transactions
        args[2]: "max.poll.records"
        args[3]: bootstrap.servers
        */

        //inputs
        String bootstrapServers = "127.0.0.1:9092";
        String schemaRegistryUrl = "http://127.0.0.1:8081";
        int numOfPartitions = 3;
        int numOfAccounts = 10;
        long initBalance = 1000000L;
        long numOfData = 100;
        long amount = 100000L; //per transaction

        //setups
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "info"); //"off", "trace", "debug", "info", "warn", "error".
        //create banks (100, 101, 102 etc) & accounts
        ArrayList<String> bank = new ArrayList<String>();
        HashMap<String, Long> bankBalance = new HashMap<String, Long>();
        int banks = 0;
        String account;
        while(banks < numOfPartitions){
            bank.add("10" + banks);
            for (int accountNum = 1; accountNum <= numOfAccounts; accountNum++) {
                if (accountNum < 10) {
                    account =  "10" + banks + "000" + accountNum;
                }else if (accountNum < 100){
                    account =  "10" + banks + "00" + accountNum;
                }else if (accountNum < 1000){
                    account =  "10" + banks + "0" + accountNum;
                }else {
                    account = "10" + banks + accountNum;
                }
                bankBalance.put(account, initBalance);
            }
            banks += 1;
        }
        //create producer
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", bootstrapServers);
        properties.setProperty("acks", "all");
        properties.setProperty("retries", "10");
        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer", KafkaAvroSerializer.class.getName());
        properties.setProperty("schema.registry.url", schemaRegistryUrl);
        KafkaProducer<String, Block> producer = new KafkaProducer<String, Block>(properties);

        //sending random data
        for (long i = 1L; i <= numOfData; i++) {
            //random inBank and outBank
            int outBankNum = ThreadLocalRandom.current().nextInt(0, numOfPartitions);
            int inBankNum = ThreadLocalRandom.current().nextInt(0, numOfPartitions);
            int outAccountNum = ThreadLocalRandom.current().nextInt(1, numOfAccounts+1);
            int inAccountNum = ThreadLocalRandom.current().nextInt(1, numOfAccounts+1);

            if (outAccountNum < 10) {
                outAccount =  bank.get(outBankNum) + "000" + outAccountNum;
            }else if (outAccountNum < 100){
                outAccount =  bank.get(outBankNum) + "00" + outAccountNum;
            }else if (outAccountNum < 1000){
                outAccount =  bank.get(outBankNum) + "0" + outAccountNum;
            }else {
                outAccount = bank.get(outBankNum) + outAccountNum;
            }
            if (inAccountNum < 10) {
                inAccount =  bank.get(inBankNum) + "000" + inAccountNum;
            }else if (inAccountNum < 100){
                inAccount =  bank.get(inBankNum) + "00" + inAccountNum;
            }else if (inAccountNum < 1000){
                inAccount =  bank.get(inBankNum) + "0" + inAccountNum;
            }else {
                inAccount = bank.get(inBankNum) + inAccountNum;
            }

            //build block
            Transaction detail = new Transaction(i,
                    bank.get(outBankNum), outAccount,
                    bank.get(inBankNum), inAccount,
                    outBankNum, inBankNum, amount, 0);
            List<Transaction> listOfDetail = new ArrayList<Transaction>();
            listOfDetail.add(detail);

            Block output = Block.newBuilder()
                    .setTransactions(listOfDetail)
                    .build();
            System.out.println(output);

            //send
            producer.send(new ProducerRecord<String, Block>("transactions", outBankNum, bank.get(outBankNum), output));
            bankBalance.compute(outAccount, (key, value) -> value - detail.getAmount());
            bankBalance.compute(inAccount, (key, value) -> value + detail.getAmount());
        }

        //flush and close producer
        producer.flush();
        producer.close();
        System.out.println(bankBalance);
    }
}