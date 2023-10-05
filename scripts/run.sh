#!/bin/bash

#args used in java

machine=1

if [ $machine -eq 1 ] 
then
    echo machine 1
    bootstrapServers="192.168.50.213:9092"
    schemaRegistryUrl="http://192.168.50.213:8081"
else
    echo machine 2
    bootstrapServers="192.168.50.224:9092"
    schemaRegistryUrl="http://192.168.50.224:8081"
fi

numOfPartitions=2
numOfAccounts=100
numOfReplicationFactor=1
initBalance=100000000
maxPoll=2000
blockSize=500

blockTimeout=10000 #aggregator only
numOfData=1000000 #sourceProducer only
amountPerTransaction=1 #sourceProducer only
#${i}aggregator ${i}validator are transactional.ids
zipfExponent=0

tokensPerSec=10000;
executionTime=10000;

#args used in script
numOfaggregators=1
numOfvalidators=1

#not often change or not used
logger="off" #"off", "trace", "debug", "info", "warn", "error"
successfulMultiplePartition="true"
UTXODoNotAgg="true" #initialize only
randomAmount="false" #1000-100000
aggUTXOTime=5000 #sumUTXO only
UTXOUpdatePeriod=100000000 #validator only
UTXOUpdateBreakTime=1000 #validator only
UTXODirectAdd="true"

echo "=== Open $numOfaggregators aggregators and $numOfvalidators validator === "
gnome-terminal -- java -cp /home/nsd/liang_you_git_repo/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar aggregator $bootstrapServers $schemaRegistryUrl $numOfPartitions $numOfAccounts $numOfReplicationFactor $initBalance $maxPoll $blockSize $blockTimeout $aggUTXOTime $numOfData $amountPerTransaction $UTXOUpdatePeriod $UTXOUpdateBreakTime $successfulMultiplePartition $UTXODoNotAgg $randomAmount $logger ${machine}aggregator

#three validators can use, validator, validatorMultiThread, validatorMultiThreadNoConcurrentHashMap
gnome-terminal -- java -cp /home/nsd/liang_you_git_repo/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar validator $bootstrapServers $schemaRegistryUrl $numOfPartitions $numOfAccounts $numOfReplicationFactor $initBalance $maxPoll $blockSize $blockTimeout $aggUTXOTime $numOfData $amountPerTransaction $UTXOUpdatePeriod $UTXOUpdateBreakTime $successfulMultiplePartition $UTXODoNotAgg $randomAmount $logger ${machine}validator $UTXODirectAdd


#wait for kafka consumer to rebalance
sleep 5s

#three source can use, sourceProducer, sourceProducerZipf, sourceProducerZipfRps
echo "=== input data and wait for processes end === "
gnome-terminal -- java -cp /home/nsd/liang_you_git_repo/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar sourceProducer $bootstrapServers $schemaRegistryUrl $numOfPartitions $numOfAccounts $numOfReplicationFactor $initBalance $maxPoll $blockSize $blockTimeout $aggUTXOTime $numOfData $amountPerTransaction $UTXOUpdatePeriod $UTXOUpdateBreakTime $successfulMultiplePartition $UTXODoNotAgg $randomAmount $logger $zipfExponent $tokensPerSec $executionTime "/home/nsd/liang_you_git_repo/Distributed_Banking_Project/scripts/timeStamps/RPS.txt"

#wait until finish
sleep 30s
#read -n 1 -s -r -p "Press any key to continue"
#pkill -f 'distributed-payment-v1-1.0-SNAPSHOT.jar'
#echo -e "\nEnd. "


