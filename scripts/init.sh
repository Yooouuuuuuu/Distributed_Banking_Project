#!/bin/bash

#args used in java

machine=1

if [ $machine -eq 1 ] 
then
    bootstrapServers="192.168.50.213:9092"
    schemaRegistryUrl="http://192.168.50.213:8081"
else
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
zipfExponent=1

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

echo "initialize kafka topics"
java -cp /home/nsd/liang_you_git_repo/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar initialize $bootstrapServers $schemaRegistryUrl $numOfPartitions $numOfAccounts $numOfReplicationFactor $initBalance $maxPoll $blockSize $blockTimeout $aggUTXOTime $numOfData $amountPerTransaction $UTXOUpdatePeriod $UTXOUpdateBreakTime $successfulMultiplePartition $UTXODoNotAgg $randomAmount $logger

echo -e "\nEnd. "


