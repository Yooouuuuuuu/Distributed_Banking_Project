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
        String outputTxt = args[9];
        String machine = args[10];

        //setups
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "info"); //"off", "trace", "debug", "info", "warn", "error".

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
        ZipfDistribution zipfDistribution = new ZipfDistribution(accountList.length - 1, zipfExponent);
        RateLimiter limiter = RateLimiter.create(tokensPerSec); //rps
        long start = System.currentTimeMillis();
        long lastFlushTime = System.currentTimeMillis();
        boolean keepSending = true;
        long serialNumber = Long.parseLong(machine);

        //sending random data
        while (keepSending) {
            limiter.acquire(); //acquire a token for permission, if no token left, block it.

            //generate and send data
            int out = zipfDistribution.sample();
            String outAccount = accountList[out];
            String outBank = outAccount.substring(0, 3);

            int in = zipfDistribution.sample();
            String inAccount = accountList[in];
            String inBank = inAccount.substring(0, 3);

            /*
            //reselect if in and out are same account
            while (inAccount.equals(outAccount)) {
                in = zipfDistribution.sample();
                inAccount = accountList[in];
                inBank = inAccount.substring(0, 3);
            }

             */

            /*
            if (randomAmount) {
                amountPerTransaction = ThreadLocalRandom.current().nextInt(1000, 10000);
            } //else use the value args give
             */

            Transaction detail = new Transaction(serialNumber,
                    outBank, outAccount,
                    inBank, inAccount,
                    bankList.indexOf(outBank),
                    bankList.indexOf(inBank),
                    amountPerTransaction,
                    0, null, null);
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

        //check rps
        long end = System.currentTimeMillis();
        long spendTime = end - start;
        float RPS;
        System.out.println("execution time: " + spendTime + " (" + executionTime + ") ms");

        if (machine == String.valueOf(1)) {
            RPS = (float) (1000 * ((serialNumber - 1)/2+1)) / spendTime;
            System.out.println("numbers of payments sent: " + ((serialNumber - 1)/2+1) + ". RPS = " + RPS);
        } else {
            RPS = (float) (1000 * ((serialNumber - 1)/2)) / spendTime;
            System.out.println("numbers of payments sent: " + ((serialNumber - 1)/2) + ". RPS = " + RPS);
        }

        //print result
        //System.out.println("bank balance: " + bankBalance);
        System.out.println("rejected count: " + rejectedCount);
        //This bankBalance is for reference only,
        //if any transaction has been rejected, here shows the linearization result,
        //however our system is not even serialization.

        //write important data as txt
        PrintWriter writer = new PrintWriter(outputTxt);

        writer.println("numbers of payments");
        if (machine == String.valueOf(1)) {
            writer.println((serialNumber - 1) / 2+1);
        }else {
            writer.println((serialNumber - 1) / 2);
        }
        writer.println("source producer execution time");
        writer.println(spendTime);
        writer.println("RPS");
        writer.println(RPS);

        writer.flush();
        writer.close();

        //System.in.read();

    }
}
