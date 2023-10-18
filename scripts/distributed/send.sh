#!/bin/bash

#args used in java

machine=$1

if [ $machine -eq 2 ] 
then
    bootstrapServers="192.168.50.213:9092"
    schemaRegistryUrl="http://192.168.50.213:8081"
else
    bootstrapServers="192.168.50.224:9092"
    schemaRegistryUrl="http://192.168.50.224:8081"
fi

mkdir -p /home/nsd/liang_you_git_repo/Distributed_Banking_Project/scripts/timeStamps/tokensPerSec$2

numOfPartitions=2
numOfAccounts=1000
initBalance=100000000
amountPerTransaction=1
outputFile="/home/nsd/liang_you_git_repo/Distributed_Banking_Project/scripts/timeStamps/tokensPerSec$2/RPS.txt"

#need to change for testing
zipfExponent=1
tokensPerSec=$2
executionTime=10000

#three source can use, sourceProducer, sourceProducerZipf, sourceProducerZipfRps
echo "machine $machine is inputing data and waiting for processes end"
java -cp /home/nsd/liang_you_git_repo/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar sourceProducer $bootstrapServers $schemaRegistryUrl $numOfPartitions $numOfAccounts $initBalance $amountPerTransaction $zipfExponent $tokensPerSec $executionTime $outputFile $machine

echo -e "\nEnd. "


