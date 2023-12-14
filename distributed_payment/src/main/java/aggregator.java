import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import my.avroSchema.Block;
import my.avroSchema.Transaction;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.simple.SimpleLogger;
import java.time.Duration;
import java.util.*;

public class aggregator {

    static KafkaConsumer<String, Block> consumerFromTransactions;
    static Producer<String, Block> producer;
    static ArrayList<List<Transaction> > listOfListOfTransactions = new ArrayList<List<Transaction> >();
    static ArrayList<Integer> listOfCounts = new ArrayList<Integer>();
    static HashMap<String, Long> bankTime = new HashMap<String, Long>();
    static HashMap<Integer, String> bankPartition = new HashMap<>();
    static HashMap<Integer, Long> partitionOffset = new HashMap<>();
    public static void main(String[] args) throws Exception {

        //inputs
        String bootstrapServers = args[0];
        String schemaRegistryUrl = args[1];
        int numOfPartitions = Integer.parseInt(args[2]);
        int maxPoll = Integer.parseInt(args[3]);
        int blockSize = Integer.parseInt(args[4]);
        long blockTimeout = Long.parseLong(args[5]); //aggregator only
        String transactionalId = args[6];
        String log = args[7];

        //setups
        System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, log); //"off", "trace", "debug", "info", "warn", "error".
        InitConsumer(maxPoll, bootstrapServers, schemaRegistryUrl, numOfPartitions);
        InitProducer(bootstrapServers, schemaRegistryUrl, transactionalId);
        producer.initTransactions();

        //poll from "transactions" topic
        while (true) {
            ConsumerRecords<String, Block> records = consumerFromTransactions.poll(Duration.ofMillis(10000));
            for (ConsumerRecord<String, Block> record : records) {
                //aggregate transactions to blocks using list in Avro
                aggToBlock(record.value(), record);
                if (listOfCounts.get(record.value().getTransactions().get(0).getOutbankPartition()) >= blockSize) {
                    producer.beginTransaction(); //Start atomically transactional write.
                    try {
                        //if blocks is full (or time out), send them to "blocks" topic.
                        sendBlock(record.value(), record);
                        producer.commitTransaction();
                    } catch (Exception e) {
                        producer.abortTransaction();
                        System.out.println("Tx aborted.");
                    }
                }
            }
            //checked after every poll, if any concerned bank block timeout, send it to "blocks" topic.
            checkBlockTimeout(numOfPartitions, blockTimeout, "transactions");
        }
    }

    private static void InitConsumer(int maxPoll, String bootstrapServers, String schemaRegistryUrl, int numOfPartitions) {
        //consumer consume from "transactions" topic
        Properties propsConsumer = new Properties();
        propsConsumer.put("bootstrap.servers", bootstrapServers);
        propsConsumer.put("group.id", "aggregator-group");
        propsConsumer.put("auto.offset.reset", "earliest");
        propsConsumer.put("enable.auto.commit", "false");
        propsConsumer.put("isolation.level", "read_committed");
        propsConsumer.put("max.poll.records", maxPoll);
        //avro part
        propsConsumer.setProperty("key.deserializer", StringDeserializer.class.getName());
        propsConsumer.setProperty("value.deserializer", KafkaAvroDeserializer.class.getName());
        propsConsumer.setProperty("schema.registry.url", schemaRegistryUrl);
        propsConsumer.setProperty("specific.avro.reader", "true");

        String input_topic = "transactions";
        consumerFromTransactions =
                new KafkaConsumer<String, Block>(propsConsumer);
        consumerFromTransactions.subscribe(Collections.singleton(input_topic),
                new ConsumerRebalanceListener() {
                    @Override
                    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                        //System.out.println("onPartitionsRevoked");
                    }
                    @Override
                    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                        //initialize block
                        listOfListOfTransactions.clear();
                        listOfCounts.clear();
                        List<Transaction> listOfTransactions;
                        for (int i = 0; i < numOfPartitions; i++) {
                            listOfTransactions = new ArrayList<Transaction>();
                            listOfListOfTransactions.add(listOfTransactions);
                            listOfCounts.add(0);
                        }
                        System.out.println("aggregator is rebalanced. Initialize lists of blocks and counts.");
                    }});
    }

    private static void InitProducer(String bootstrapServers, String schemaRegistryUrl, String transactionalId) {
        Properties propsProducer = new Properties();
        propsProducer.put("bootstrap.servers", bootstrapServers);
        propsProducer.put("transactional.id", transactionalId);
        propsProducer.put("enable.idempotence", "true");
        //propsProducer.put("max.block.ms", "1000");

        //avro part
        propsProducer.setProperty("key.serializer", StringSerializer.class.getName());
        propsProducer.setProperty("value.serializer", KafkaAvroSerializer.class.getName());
        propsProducer.setProperty("schema.registry.url", schemaRegistryUrl);
        producer = new KafkaProducer<>(propsProducer);
    }

    public static void aggToBlock(Block recordValue, ConsumerRecord record) {
        //bank time init if not exist
        if (!bankTime.containsKey(recordValue.getTransactions().get(0).getOutbank())) {
            bankTime.put(recordValue.getTransactions().get(0).getOutbank(), System.currentTimeMillis());
            bankPartition.put(recordValue.getTransactions().get(0).getOutbankPartition(), recordValue.getTransactions().get(0).getOutbank());

        }

        //count +1
        listOfCounts.set(recordValue.getTransactions().get(0).getOutbankPartition(),
                listOfCounts.get(recordValue.getTransactions().get(0).getOutbankPartition()) + 1);
        partitionOffset.put(record.partition(), record.offset());

        //add transaction to current block
        recordValue.getTransactions().get(0).put("timestamp1", record.timestamp());
        listOfListOfTransactions.get(recordValue.getTransactions().get(0).getOutbankPartition()).add(recordValue.getTransactions().get(0));

    }

    public static void sendBlock(Block recordValue, ConsumerRecord record) {
        //if the block is full (or time out), send it to "block" topic.
        Block currentBlock = Block.newBuilder()
                .setTransactions(listOfListOfTransactions.get(recordValue.getTransactions().get(0).getOutbankPartition()))
                .build() ;

        producer.send(new ProducerRecord<String, Block>("blocks", record.partition(), (String) record.key(),currentBlock));

        //reset timeout while send
        bankTime.put(recordValue.getTransactions().get(0).getOutbank(), System.currentTimeMillis());

        //initialize the block
        listOfCounts.set(recordValue.getTransactions().get(0).getOutbankPartition(), 0);
        listOfListOfTransactions.set(recordValue.getTransactions().get(0).getOutbankPartition(), new ArrayList<Transaction>());

        //consumer group manually commit
        consumerFromTransactions.commitSync((Collections.singletonMap(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1, ""))));

    }

    public static void checkBlockTimeout(int numOfPartitions, long blockTimeout, String topic) {
        for (int partition = 0; partition < numOfPartitions; partition++) { //check all blocks
            if (!bankTime.containsKey(bankPartition.get(partition)) || listOfCounts.get(partition) == 0) {
                //skip partition(s) not in charge of this consumer, do nothing
            } else {
                if (System.currentTimeMillis() - bankTime.get(bankPartition.get(partition)) > blockTimeout) { //timeout
                    //start kafka transactional write
                    producer.beginTransaction();

                    try {
                        //send it to "block" topic.
                        Block currentBlock = Block.newBuilder()
                                .setTransactions(listOfListOfTransactions.get(partition))
                                .build();
                        producer.send(new ProducerRecord<String, Block>("blocks", partition,
                                bankPartition.get(partition), currentBlock));

                        //initialize the block
                        listOfCounts.set(partition, 0);
                        listOfListOfTransactions.set(partition, new ArrayList<Transaction>());

                        //reset time
                        bankTime.put(bankPartition.get(partition), System.currentTimeMillis());

                        //consumer group manually commit
                        consumerFromTransactions.commitSync((Collections.singletonMap(
                                new TopicPartition(topic, partition),
                                new OffsetAndMetadata(partitionOffset.get(partition) + 1, ""))));

                        //print info
                        System.out.println("partition " + partition + " send a block since timeout\n" + listOfCounts);

                        //commit transactional write
                        producer.commitTransaction();
                    } catch (Exception e) {
                        producer.abortTransaction();
                        System.out.println("Tx aborted.");
                    }
                }
            }
        }
    }

}


