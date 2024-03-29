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
blockSize=1000

blockTimeout=10000 #aggregator only
numOfData=1000000 #sourceProducer only
amountPerTransaction=1 #sourceProducer only
#${i}aggregator ${i}validator are transactional.ids
zipfExponent=1

tokensPerSec=50000;
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

outputTxt1="/home/nsd/liang_you_git_repo/Distributed_Banking_Project/scripts/timeStamps/firstTimestamp.txt"
outputTxt2="/home/nsd/liang_you_git_repo/Distributed_Banking_Project/scripts/timeStamps/OriginalData.txt"
outputTxt3="/home/nsd/liang_you_git_repo/Distributed_Banking_Project/scripts/timeStamps/UTXO.txt"
inputTxt1="/home/nsd/liang_you_git_repo/Distributed_Banking_Project/scripts/timeStamps/RPS.txt"
outputcsv="/home/nsd/liang_you_git_repo/Distributed_Banking_Project/scripts/timeStamps/orders.csv"


DEBUG=false
if ${DEBUG}; then
echo -e "\n=== poll from transactions, order, and localBalance topics === " 
gnome-terminal -- java -cp /home/nsd/liang_you_git_repo/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar test/consumeTransactions $bootstrapServers $schemaRegistryUrl $maxPoll
gnome-terminal -- java -cp /home/nsd/liang_you_git_repo/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar test/consumeOrder $bootstrapServers $schemaRegistryUrl $numOfAccounts 0
gnome-terminal -- java -cp /home/nsd/liang_you_git_repo/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar test/consumeOrder $bootstrapServers $schemaRegistryUrl $numOfAccounts 1
gnome-terminal -- java -cp /home/nsd/liang_you_git_repo/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar test/consumeLocalBalance $bootstrapServers $schemaRegistryUrl $maxPoll
fi

echo "consume transactions -- write firstTimestamp, OriginalData & UTXO to txt files" 
#100000 is the roughly number of data, use to decide how long we should wait until polling finish. no need to be the exact number of data.
java -cp /home/nsd/liang_you_git_repo/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar test/writeTimestampsToTxt $bootstrapServers $schemaRegistryUrl $tokensPerSec $executionTime "off" $outputTxt1 $outputTxt2 $outputTxt3

sleep 30s

echo "sort timestamps -- write to csv file" 
java -cp /home/nsd/liang_you_git_repo/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar test/sortTimestamps $outputTxt2 $outputTxt3 $inputTxt1 $outputTxt1 $outputcsv

echo -e "\nEnd. "

