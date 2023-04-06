import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Boolean.parseBoolean;

public class sourceProducer {
    public static void main(String[] args) throws ExecutionException, InterruptedException {

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

        int numOfPartitions = Integer.parseInt(args[0]); //3
        long numOfData = Integer.parseInt(args[1]); //10000
        boolean toSuccessfulTopic = parseBoolean(args[6]);
        boolean bigTXOnly = true;
        String bootstrapServers = args[8];


        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "off"); //"off", "trace", "debug", "info", "warn", "error".

        //inputs

        HashMap<String, Long> bankBalance = new HashMap<String, Long>();
        ArrayList<String> bank = new ArrayList<String>();

        //create bank 100, 101, 102
        int count = 0;
        while(count < numOfPartitions){
            bank.add("10" + count);
            bankBalance.put("10" + count, 100000000L);
            count += 1;
        }

        //create producer properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "TxSerializer");

        //create the producer
        KafkaProducer<String, Transaction> producer = new KafkaProducer<>(properties);

        long amount;
        for (long i = 0; i < numOfData; i++) { //3.5% of the transactions are big transactions

            //whether we use aggregator
            if (bigTXOnly) {
                amount = 1L;
            } else {
                if (Math.random() <= 0.035) {
                    amount = 1L;
                } else {
                    amount = 1L;
                }
            }

            //random inBank, outBank
            int inBankNum = ThreadLocalRandom.current().nextInt(0, numOfPartitions);
            int outBankNum = ThreadLocalRandom.current().nextInt(0, numOfPartitions);
            Transaction output = new Transaction(bank.get(inBankNum), bank.get(outBankNum), amount, i, inBankNum, outBankNum, 0);

            //String topic;
            if (toSuccessfulTopic) {
                producer.send(new ProducerRecord<String, Transaction>("successfulTX", output.getInBankPartition(), output.getInBank(), output));
                producer.send(new ProducerRecord<String, Transaction>("successfulTX", output.getOutBankPartition(), output.getOutBank(), output));
            }else {
                if (amount == 1L) {
                    producer.send(new ProducerRecord<String, Transaction>("bigTX", output.getInBankPartition(), output.getInBank(), output));
                } else if (amount == 1) {
                    producer.send(new ProducerRecord<String, Transaction>("smallTX", output.getInBankPartition(), output.getInBank(), output));
                }
                //System.out.println(output.getInBank()+", "+output.getOutBank()+", "+output.getInBankPartition()+", "+output.getOutBankPartition()+", "+output.getCategory()+", "+output.getAmount()+", "+output.getSerialNumber()+", ");
            }
                bankBalance.compute(output.getInBank(), (key, value) -> value - output.getAmount());
                bankBalance.compute(output.getOutBank(), (key, value) -> value + output.getAmount());

        }

        //flush and close producer
        producer.flush();
        producer.close();

        System.out.println(bankBalance);
    }
}



