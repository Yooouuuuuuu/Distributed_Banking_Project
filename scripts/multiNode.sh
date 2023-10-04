#!/bin/bash

#args used in java
bootstrapServers="127.0.0.1:9092"
schemaRegistryUrl="http://127.0.0.1:8081"
numOfPartitions=2
numOfAccounts=5
numOfReplicationFactor=1
initBalance=100000000
maxPoll=2000
blockSize=500

blockTimeout=10000 #aggregator only
numOfData=1000000 #sourceProducer only
amountPerTransaction=1 #sourceProducer only
#${i}aggregator ${i}validator are transactional.ids
zipfExponent=2

tokensPerSec=10000;
executionTime=10000;

#args used in script
numOfaggregators=1
numOfvalidators=2

#not often change or not used
logger="off" #"off", "trace", "debug", "info", "warn", "error"
successfulMultiplePartition="true"
UTXODoNotAgg="true" #initialize only
randomAmount="false" #1000-100000
aggUTXOTime=5000 #sumUTXO only
UTXOUpdatePeriod=100000000 #validator only
UTXOUpdateBreakTime=1000 #validator only
UTXODirectAdd="true"

#auto test, secs wait for validations to process
#waitTime=1000

echo "=== Initialize kafka topics === "
java -cp /home/yooouuuuuuu/git-repos/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar initialize $bootstrapServers $schemaRegistryUrl $numOfPartitions $numOfAccounts $numOfReplicationFactor $initBalance $maxPoll $blockSize $blockTimeout $aggUTXOTime $numOfData $amountPerTransaction $UTXOUpdatePeriod $UTXOUpdateBreakTime $successfulMultiplePartition $UTXODoNotAgg $randomAmount $logger

echo "=== Open $numOfaggregators aggregators and $numOfvalidators validator === "
for i in $( eval echo {1..$numOfaggregators} )
  do gnome-terminal -- java -cp /home/yooouuuuuuu/git-repos/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar aggregator $bootstrapServers $schemaRegistryUrl $numOfPartitions $numOfAccounts $numOfReplicationFactor $initBalance $maxPoll $blockSize $blockTimeout $aggUTXOTime $numOfData $amountPerTransaction $UTXOUpdatePeriod $UTXOUpdateBreakTime $successfulMultiplePartition $UTXODoNotAgg $randomAmount $logger ${i}aggregator
done

#three validators can use, validator, validatorMultiThread, validatorMultiThreadNoConcurrentHashMap
for i in $( eval echo {1..$numOfvalidators} )
  do gnome-terminal -- java -cp /home/yooouuuuuuu/git-repos/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar validator $bootstrapServers $schemaRegistryUrl $numOfPartitions $numOfAccounts $numOfReplicationFactor $initBalance $maxPoll $blockSize $blockTimeout $aggUTXOTime $numOfData $amountPerTransaction $UTXOUpdatePeriod $UTXOUpdateBreakTime $successfulMultiplePartition $UTXODoNotAgg $randomAmount $logger ${i}validator $UTXODirectAdd
done

#wait for kafka consumer to rebalance
sleep 5s

#three source can use, sourceProducer, sourceProducerZipf, sourceProducerZipfRps
echo "=== input data and wait for processes end === "
gnome-terminal -- java -cp /home/yooouuuuuuu/git-repos/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar sourceProducer $bootstrapServers $schemaRegistryUrl $numOfPartitions $numOfAccounts $numOfReplicationFactor $initBalance $maxPoll $blockSize $blockTimeout $aggUTXOTime $numOfData $amountPerTransaction $UTXOUpdatePeriod $UTXOUpdateBreakTime $successfulMultiplePartition $UTXODoNotAgg $randomAmount $logger $zipfExponent $tokensPerSec $executionTime "/home/yooouuuuuuu/git-repos/Distributed_Banking_Project/scripts/timeStamps/RPS.txt"

#wait until finish
sleep 30s
#read -n 1 -s -r -p "Press any key to continue"
pkill -f 'distributed-payment-v1-1.0-SNAPSHOT.jar'


DEBUG=false
if ${DEBUG}; then
echo -e "\n=== poll from transactions, order, and localBalance topics === " 
gnome-terminal -- java -cp /home/yooouuuuuuu/git-repos/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar test/consumeTransactions $bootstrapServers $schemaRegistryUrl $maxPoll
gnome-terminal -- java -cp /home/yooouuuuuuu/git-repos/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar test/consumeOrder $bootstrapServers $schemaRegistryUrl $numOfAccounts 0
gnome-terminal -- java -cp /home/yooouuuuuuu/git-repos/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar test/consumeOrder $bootstrapServers $schemaRegistryUrl $numOfAccounts 1
gnome-terminal -- java -cp /home/yooouuuuuuu/git-repos/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar test/consumeLocalBalance $bootstrapServers $schemaRegistryUrl $maxPoll
fi

java -cp /home/yooouuuuuuu/git-repos/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar test/consumeTransactions $bootstrapServers $schemaRegistryUrl "/home/yooouuuuuuu/git-repos/Distributed_Banking_Project/scripts/timeStamps/firstTimestamp.txt"

echo "=== record and sort order timestamps === " 
#100000 is the roughly number of data, use to decide how long we should wait until polling finish. no need to be the exact number of data.
java -cp /home/yooouuuuuuu/git-repos/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar test/writeTimestampsToTxt "127.0.0.1:9092" "http://127.0.0.1:8081" 100000 "off" "/home/yooouuuuuuu/git-repos/Distributed_Banking_Project/scripts/timeStamps/OriginalData.txt" "/home/yooouuuuuuu/git-repos/Distributed_Banking_Project/scripts/timeStamps/UTXO.txt"

sleep 30s

java -cp /home/yooouuuuuuu/git-repos/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar test/sortTimestamps "/home/yooouuuuuuu/git-repos/Distributed_Banking_Project/scripts/timeStamps/OriginalData.txt" "/home/yooouuuuuuu/git-repos/Distributed_Banking_Project/scripts/timeStamps/UTXO.txt" "/home/yooouuuuuuu/git-repos/Distributed_Banking_Project/scripts/timeStamps/RPS.txt" "/home/yooouuuuuuu/git-repos/Distributed_Banking_Project/scripts/timeStamps/firstTimestamp.txt" "/home/yooouuuuuuu/git-repos/Distributed_Banking_Project/scripts/timeStamps/orders.csv"

echo -e "\nEnd. "


