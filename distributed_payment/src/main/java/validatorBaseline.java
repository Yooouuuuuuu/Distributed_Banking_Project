import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.subject.RecordNameStrategy;
import my.avroSchema.Block;
import my.avroSchema.LocalBalance;
import my.avroSchema.Transaction;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.simple.SimpleLogger;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class validatorBaseline {
    static KafkaConsumer<String, Block> consumerFromBlocks;
    static KafkaConsumer<String, LocalBalance> consumerFromLocalBalance;
    static KafkaProducer producer;
    static HashMap<String, Long> bankBalance = new HashMap<String, Long>();
    static HashMap<Integer, String> partitionBank = new HashMap<Integer, String>();
    static long rejectedCount = 0;
    static long UTXOCount = 0;
    static long pollCount = 0;

    public static void main(String[] args) throws Exception {

        //inputs
        String bootstrapServers = args[0];
        String schemaRegistryUrl = args[1];
        int maxPoll = Integer.parseInt(args[6]);
        boolean successfulMultiplePartition = Boolean.parseBoolean(args[14]);
        String log = args[17];
        String transactionalId = args[18];

        //setups
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, log);//"off", "trace", "debug", "info", "warn", "error"
        InitConsumer(maxPoll, bootstrapServers, schemaRegistryUrl);
        InitProducer(bootstrapServers, schemaRegistryUrl, transactionalId);
        //Logger logger = LoggerFactory.getLogger(validatorDirectPollUTXO.class);
        producer.initTransactions();

        //variables for testing
        long time1 = System.currentTimeMillis();
        long time2;
        long interval;

        //poll from "blocks" topic
        while (true) {
            ConsumerRecords<String, Block> records = consumerFromBlocks.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, Block> record : records) {
                //Start atomically transactional write. One block per transactional write.
                producer.beginTransaction();
                try {
                    //As outbank, withdraw money and create UTXO for inbank. Category 0 means it is a raw transaction.
                    if (record.value().getTransactions().get(0).getCategory() == 0) {
                        ProcessBlocks(record.value(), successfulMultiplePartition);
                    }else if (record.value().getTransactions().get(0).getCategory() == 1) {
                        ProcessUTXOBlocks(record.value());
                        UTXOCount += 1;
                        /*
                        if (record.value().getTransactions().get(0).getSerialNumber() % 1000 == 0) {
                            System.out.println("Transaction of serialNum " +
                                    record.value().getTransactions().get(0).getSerialNumber() +
                                    " is finished. (UTXO side)");
                        }
                         */
                    }else if (record.value().getTransactions().get(0).getCategory() == 2) {
                        //Category 2 means it is an initialize record for accounts' balance. Only do once when system start.
                        InitBank(record.value(), record);
                    }
                    consumerFromBlocks.commitSync();
                    producer.commitTransaction();

                } catch (Exception e)  {
                    producer.abortTransaction();
                    bankBalance = new HashMap<String, Long>();
                    partitionBank = new HashMap<>();
                    System.out.println("Tx aborted. Reset hashmaps. Exception: " + e.getMessage());
                }
            }

            //variables for testing
            pollCount += 1;
            time2 = System.currentTimeMillis();
            interval = time1 - time2;
            time1 = System.currentTimeMillis();
            System.out.println("----------------------------------------------------\n " +
                    "poll interval: " + interval);
            System.out.println("poll count: " + pollCount + " poll size: " + records.count());
            System.out.println("numbers of UTXO consumed: " + UTXOCount);
            //after some test, I found that with ProcessUTXOBlocks, the increase to about 20000ms when
            // max.poll.records is 2000. Thus, the main reason of this version being so slow is likely the
            // multiple writes in ProcessUTXOBlocks.
        }
    }

    private static void InitConsumer(int maxPoll, String bootstrapServers, String schemaRegistryUrl) {
        //consumer consume from "blocks" topic
        Properties propsConsumerTx = new Properties();
        propsConsumerTx.put("bootstrap.servers", bootstrapServers);
        propsConsumerTx.put("group.id", "validator-group");
        propsConsumerTx.put("auto.offset.reset", "earliest");
        propsConsumerTx.put("enable.auto.commit", "false");
        propsConsumerTx.put("isolation.level", "read_committed");
        propsConsumerTx.put("max.poll.records", maxPoll);
        //avro part
        propsConsumerTx.setProperty("key.deserializer", StringDeserializer.class.getName());
        propsConsumerTx.setProperty("value.deserializer", KafkaAvroDeserializer.class.getName());
        propsConsumerTx.setProperty("schema.registry.url", schemaRegistryUrl);
        propsConsumerTx.setProperty("specific.avro.reader", "true");

        String input_topic = "blocks";
        consumerFromBlocks =
                new KafkaConsumer<String, Block>(propsConsumerTx);
        consumerFromBlocks.subscribe(Collections.singletonList(input_topic),
                new ConsumerRebalanceListener() {
                    @Override
                    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                        //System.out.println("onPartitionsRevoked");
                    }
                    @Override
                    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                        //reset hashmaps and flag
                        bankBalance = new HashMap<String, Long>();
                        partitionBank = new HashMap<>();
                        System.out.println("validator is rebalanced. Reset hashmaps.");
                    }});

        // the three consumers below using the same property
        Properties propsConsumerAssign = new Properties();
        propsConsumerAssign.put("bootstrap.servers", bootstrapServers);
        propsConsumerAssign.put("isolation.level", "read_committed");
        propsConsumerAssign.put("enable.auto.commit", "false");
        propsConsumerAssign.setProperty("key.deserializer", StringDeserializer.class.getName());
        propsConsumerAssign.setProperty("value.deserializer", KafkaAvroDeserializer.class.getName());
        propsConsumerAssign.setProperty("schema.registry.url", schemaRegistryUrl);
        propsConsumerAssign.setProperty("specific.avro.reader", "true");
        //consumer consume from "localBalance" topic
        consumerFromLocalBalance =
                new KafkaConsumer<String, LocalBalance>(propsConsumerAssign);
    }

    private static void InitProducer(String bootstrapServers, String schemaRegistryUrl, String transactionalId) {
        Properties propsProducer = new Properties();
        propsProducer.put("bootstrap.servers", bootstrapServers);
        propsProducer.put("transactional.id", transactionalId);
        propsProducer.put("transaction.timeout.ms", 300000);
        propsProducer.put("enable.idempotence", "true");
        propsProducer.put("max.block.ms", "1000");
        // avro part
        propsProducer.setProperty("key.serializer", StringSerializer.class.getName());
        propsProducer.setProperty("value.serializer", KafkaAvroSerializer.class.getName());
        propsProducer.setProperty("schema.registry.url", schemaRegistryUrl);
        propsProducer.put("value.subject.name.strategy", RecordNameStrategy.class.getName());
        producer = new KafkaProducer<>(propsProducer);
    }

    private static void InitBank(Block recordValue, ConsumerRecord<String, Block> record) throws ExecutionException, InterruptedException {
        for (int i = 0; i < recordValue.getTransactions().size(); i++) {
            partitionBank.put(record.partition(), record.key());
            bankBalance.put(recordValue.getTransactions().get(i).getOutAccount(),
                    recordValue.getTransactions().get(i).getAmount());

            LocalBalance initBalance =
                    new LocalBalance(bankBalance.get(recordValue.getTransactions().get(i).getOutAccount()));
            producer.send(new ProducerRecord<String, LocalBalance>("localBalance",
                    recordValue.getTransactions().get(0).getOutbankPartition(),
                    recordValue.getTransactions().get(i).getOutAccount(),
                    initBalance));
        }
        producer.send(new ProducerRecord<String, Block>("successful", recordValue));

        System.out.println("Initialized. Now in charge of: " + partitionBank + " (partition:bank)");
    }

    private static void ProcessBlocks(Block recordValue, boolean successfulMultiplePartition)
            throws ExecutionException, IOException, InterruptedException {

        //initialize block
        Block currentBlock = recordValue;
        Block rejectedBlock;
        List<Transaction> listOfRejected = new ArrayList<Transaction>();
        boolean rejected = false;
        List<Integer> listOfRejectedIndex = new ArrayList<>();

        //validate transactions
        for (int i = 0; i < recordValue.getTransactions().size(); i++) {
            //check if bankBalance exist
            if (!bankBalance.containsKey(recordValue.getTransactions().get(i).getOutAccount())) {
                PollFromLocalBalance(recordValue.getTransactions().get(i).getOutbankPartition(),
                        recordValue.getTransactions().get(i).getOutbank());
            }

            //If the out account have enough money, pay for it.
            if (bankBalance.get(recordValue.getTransactions().get(i).getOutAccount())
                    >= recordValue.getTransactions().get(i).getAmount()) {
                long withdraw = currentBlock.getTransactions().get(i).getAmount();
                bankBalance.compute(recordValue.getTransactions().get(i).getOutAccount(), (key, value)
                        -> value - withdraw);

                // update "localBalance" topic
                LocalBalance newBalance =
                        new LocalBalance(bankBalance.get(recordValue.getTransactions().get(i).getOutAccount()));
                producer.send(new ProducerRecord<String, LocalBalance>("localBalance",
                        recordValue.getTransactions().get(i).getOutbankPartition(),
                        recordValue.getTransactions().get(i).getOutAccount(),
                        newBalance));


                //send UTXO after every transaction
                Transaction UTXODetail = recordValue.getTransactions().get(i);
                UTXODetail.put("category", 1);
                List<Transaction> listOfUTXODetail = new ArrayList<Transaction>();
                listOfUTXODetail.add(UTXODetail);
                Block UTXOBlock = Block.newBuilder()
                        .setTransactions(listOfUTXODetail)
                        .build();
                producer.send(new ProducerRecord<String, Block>("blocks",
                        UTXOBlock.getTransactions().get(0).getInbankPartition(),
                        UTXOBlock.getTransactions().get(0).getInbank(),
                        UTXOBlock));

            } else {
                // If balance is still not enough, reject the TX.
                rejected = true;
                Transaction rejectedTx = recordValue.getTransactions().get(i);
                listOfRejected.add(rejectedTx);
                listOfRejectedIndex.add(i);
                rejectedCount += 1;
                System.out.printf("Transaction No.%d cancelled.%n " + rejectedCount + " rejected.\n"
                        , recordValue.getTransactions().get(i).getSerialNumber());
            }
        }

        // send rejected
        if (rejected) {
            for (int i = 1; i <= listOfRejectedIndex.size(); i++) {
                currentBlock.getTransactions().remove((int)listOfRejectedIndex.get(listOfRejectedIndex.size() - i));
                //The casting to int is important thus list's ".remove" function is same for index or object.
            }
                rejectedBlock = Block.newBuilder().setTransactions(listOfRejected).build();
                producer.send(new ProducerRecord<String, Block>("rejected", rejectedBlock));
        }

        // send successful
        if (!successfulMultiplePartition) {
            producer.send(new ProducerRecord<String, Block>("successful", currentBlock));
        } else {
            producer.send(new ProducerRecord<String, Block>("successful",
                    currentBlock.getTransactions().get(0).getOutbankPartition(),
                    currentBlock.getTransactions().get(0).getOutbank(),currentBlock));
        }

    }

    private static void ProcessUTXOBlocks(Block recordValue)
            throws ExecutionException, IOException, InterruptedException {

        //validate transactions
        //check if bankBalance exist
        if (!bankBalance.containsKey(recordValue.getTransactions().get(0).getInAccount())) {
            PollFromLocalBalance(recordValue.getTransactions().get(0).getInbankPartition(),
                    recordValue.getTransactions().get(0).getInbank());
        }

        //add money to inbank
        bankBalance.compute(recordValue.getTransactions().get(0).getInAccount(), (key, value)
                -> value + recordValue.getTransactions().get(0).getAmount());

        // update "localBalance" topic
        LocalBalance newBalance =
                new LocalBalance(bankBalance.get(recordValue.getTransactions().get(0).getInAccount()));
        producer.send(new ProducerRecord<String, LocalBalance>("localBalance",
                recordValue.getTransactions().get(0).getInbankPartition(),
                recordValue.getTransactions().get(0).getInAccount(),
                newBalance));
    }

    private static void PollFromLocalBalance(int outbankPartition, String bankID) {

        //consumer assign to specific topicPartition
        TopicPartition topicPartition =
                new TopicPartition("localBalance", outbankPartition);
        consumerFromLocalBalance.assign(Collections.singletonList(topicPartition));

        //find the latest offset, so we know when to break the while loop
        consumerFromLocalBalance.seekToEnd(Collections.singleton(topicPartition));
        long latestOffset = consumerFromLocalBalance.position(topicPartition);

        //poll data of specific topic partition from beginning to end
        consumerFromLocalBalance.seek(topicPartition, 0);
        outerLoop:
        while (true) {
            ConsumerRecords<String, LocalBalance> balanceRecords = consumerFromLocalBalance.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, LocalBalance> balanceRecord : balanceRecords) {
                bankBalance.compute(balanceRecord.key(), (key, value)
                        -> balanceRecord.value().getBalance());
                //break when poll to end, while -2 for some reason, maybe the marker
                if (balanceRecord.offset() == latestOffset - 2) {
                    break outerLoop;
                }
            }
        }
        //set other hashmaps (has been reset)
        partitionBank.put(outbankPartition, bankID);
        System.out.println("Poll from localBalance. Now in charge of: " + partitionBank + " (partition:bank)");
    }
}
