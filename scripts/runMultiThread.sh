#!/bin/bash

#args used in java
bootstrapServers="127.0.0.1:9092"
schemaRegistryUrl="http://127.0.0.1:8081"
numOfPartitions=3
numOfAccounts=10
numOfReplicationFactor=1
initBalance=100000000
maxPoll=2000
blockSize=500

blockTimeout=10000 #aggregator only
numOfData=1000000 #sourceProducer only
amountPerTransaction=1 #sourceProducer only
#${i}aggregator ${i}validator ${i}sumUTXO  are transactional.ids
zipfExponent=2

#args used in script
numOfaggregators=1
numOfvalidators=3

#not often change or not used
logger="off" #"off", "trace", "debug", "info", "warn", "error"
successfulMultiplePartition="false"
UTXODoNotAgg="true" #initialize only
randomAmount="false" #1000-100000
UTXOUpdatePeriod=100000000 #validator only
UTXOUpdateBreakTime=1000 #validator only
aggUTXOTime=5000 #sumUTXO only, not used

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
  do gnome-terminal -- java -cp /home/yooouuuuuuu/git-repos/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar validator $bootstrapServers $schemaRegistryUrl $numOfPartitions $numOfAccounts $numOfReplicationFactor $initBalance $maxPoll $blockSize $blockTimeout $aggUTXOTime $numOfData $amountPerTransaction $UTXOUpdatePeriod $UTXOUpdateBreakTime $successfulMultiplePartition $UTXODoNotAgg $randomAmount $logger ${i}validator
done

echo "=== wait for rebalance === "
sleep 5s

#three source can use, sourceProducer, sourceProducerZipf, sourceProducerZipfRps
echo "=== input data === "
gnome-terminal -- java -cp /home/yooouuuuuuu/git-repos/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar sourceProducerZipf $bootstrapServers $schemaRegistryUrl $numOfPartitions $numOfAccounts $numOfReplicationFactor $initBalance $maxPoll $blockSize $blockTimeout $aggUTXOTime $numOfData $amountPerTransaction $UTXOUpdatePeriod $UTXOUpdateBreakTime $successfulMultiplePartition $UTXODoNotAgg $randomAmount $logger $zipfExponent

echo "=== wait for processes end === " 
#sleep $waitTime
read -n 1 -s -r -p "Press any key to continue"

echo -e "\n=== poll from transactions and successful topics === " 
gnome-terminal -- java -cp /home/yooouuuuuuu/git-repos/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar test/consumeTransactions $bootstrapServers $schemaRegistryUrl $maxPoll
gnome-terminal -- java -cp /home/yooouuuuuuu/git-repos/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar test/consumeSuccessful $bootstrapServers $schemaRegistryUrl $numOfPartitions $numOfAccounts $successfulMultiplePartition
gnome-terminal -- java -cp /home/yooouuuuuuu/git-repos/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar test/consumeLocalBalance $bootstrapServers $schemaRegistryUrl $maxPoll

read -n 1 -s -r -p "Press any key to end."
pkill -f 'distributed-payment-v1-1.0-SNAPSHOT.jar'
echo -e "\nEnd. "


