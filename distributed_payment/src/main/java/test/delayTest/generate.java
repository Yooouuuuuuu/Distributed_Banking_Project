package test.delayTest;

import com.google.common.util.concurrent.RateLimiter;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import my.avroSchema.Block;
import my.avroSchema.Transaction;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.simple.SimpleLogger;

import java.io.IOException;
import java.util.*;

public class generate {
    static long rejectedCount = 0;

    public static void main(String[] args) throws IOException {

        //inputs
        String bootstrapServers = args[0];
        String schemaRegistryUrl = args[1];
        int numOfPartitions = Integer.parseInt(args[2]);
        int numOfAccounts = Integer.parseInt(args[3]);

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
                bankBalance.put(account, 1000000L);
                allAccounts.add(account);
            }
            banks += 1;
        }
        Collections.shuffle(allAccounts);

        //setups
        RateLimiter limiter = RateLimiter.create(1); //rps
        long serialNumber = 0;

        String outAccount = null;
        String outBank = null;
        String inAccount = null;
        String inBank = null;

        //sending random data
        while (true) {
            limiter.acquire(); //acquire a token for permission, if no token left, block it.

            // evenly distributed
            Random rand = new Random();

            //generate and send data
            outAccount = allAccounts.get(rand.nextInt(allAccounts.size()));
            outBank = outAccount.substring(0, 3);

            inAccount = allAccounts.get(rand.nextInt(allAccounts.size()));;
            inBank = inAccount.substring(0, 3);

            //reselect if in and out are same account
            while (inBank.equals(outBank)) {
                inAccount = allAccounts.get(rand.nextInt(allAccounts.size()));;
                inBank = inAccount.substring(0, 3);
            }

            Transaction detail = new Transaction(serialNumber,
                    outBank, outAccount,
                    inBank, inAccount,
                    bankList.indexOf(outBank),
                    bankList.indexOf(inBank),
                    1L,
                    0, System.currentTimeMillis(), 0L);
            List<Transaction> listOfDetail = new ArrayList<Transaction>();
            listOfDetail.add(detail);
            Block output = Block.newBuilder()
                    .setTransactions(listOfDetail)
                    .build();
            producer.send(new ProducerRecord<String, Block>("test1",
                    bankList.indexOf(outBank),
                    outBank,
                    output));

            serialNumber += 1;
            producer.flush();

            System.out.println(output);
        }
    }
}
