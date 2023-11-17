import com.google.common.util.concurrent.RateLimiter;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import my.avroSchema.Block;
import my.avroSchema.Transaction;
import org.apache.commons.math3.distribution.ZipfDistribution;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.simple.SimpleLogger;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class sourceProducer {
    static long rejectedCount = 0;

    public static void main(String[] args) throws IOException {

        //inputs
        String bootstrapServers = args[0];
        String schemaRegistryUrl = args[1];
        int numOfPartitions = Integer.parseInt(args[2]);
        int numOfAccounts = Integer.parseInt(args[3]);
        long initBalance = Long.parseLong(args[4]);
        long amountPerTransaction = Long.parseLong(args[5]); //sourceProducer only
        float zipfExponent = Float.parseFloat(args[6]);
        float tokensPerSec = Float.parseFloat(args[7]);
        long executionTime = Long.parseLong(args[8]);
        String machine = args[9];

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
        //System.out.println(allAccounts);
        String[] accountList = allAccounts.toArray(new String[0]);

        //setups
        RateLimiter limiter = RateLimiter.create(tokensPerSec); //rps
        long start = System.currentTimeMillis();
        long lastFlushTime = System.currentTimeMillis();
        boolean keepSending = true;
        long serialNumber = Long.parseLong(machine);

        int out ;
        String outAccount = null;
        String outBank = null;

        int in;
        String inAccount = null;
        String inBank = null;

        //sending random data
        while (keepSending) {
            limiter.acquire(); //acquire a token for permission, if no token left, block it.

            if (zipfExponent != 0) {
                ZipfDistribution zipfDistribution = new ZipfDistribution(accountList.length - 1, zipfExponent);

                //generate and send data
                out = zipfDistribution.sample();
                outAccount = accountList[out];
                outBank = outAccount.substring(0, 3);

                in = zipfDistribution.sample();
                inAccount = accountList[in];
                inBank = inAccount.substring(0, 3);

                //reselect if in and out are same account
                while (inBank.equals(outBank)) {
                    in = zipfDistribution.sample();
                    inAccount = accountList[in];
                    inBank = inAccount.substring(0, 3);
                }

            } else {
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

            }

            Transaction detail = new Transaction(serialNumber,
                    outBank, outAccount,
                    inBank, inAccount,
                    bankList.indexOf(outBank),
                    bankList.indexOf(inBank),
                    amountPerTransaction,
                    0, 0L, 0L);
            List<Transaction> listOfDetail = new ArrayList<Transaction>();
            listOfDetail.add(detail);
            Block output = Block.newBuilder()
                    .setTransactions(listOfDetail)
                    .build();
            producer.send(new ProducerRecord<String, Block>("transactions",
                    bankList.indexOf(outBank),
                    outBank,
                    output));

            serialNumber += 2;

            //calculate local result for verification
            if (bankBalance.get(outAccount) - amountPerTransaction >= 0) {
                bankBalance.compute(outAccount, (key, value) -> value - detail.getAmount());
                bankBalance.compute(inAccount, (key, value) -> value + detail.getAmount());
            } else {
                rejectedCount += 1;
            }

            //periodically flush data to Kafka
            if (System.currentTimeMillis() - lastFlushTime >= 1000) {
                producer.flush();
                lastFlushTime = System.currentTimeMillis();
            }

            //end loop
            if (System.currentTimeMillis() - start >= executionTime) {
                keepSending = false;
            }
        }

        //flush and close producer
        producer.flush();
        producer.close();

    }
}
