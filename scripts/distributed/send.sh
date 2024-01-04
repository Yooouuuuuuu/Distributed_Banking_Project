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

numOfPartitions=$4
numOfAccounts=$5
initBalance=100000000
amountPerTransaction=1
executionTime=1000000

#need to change for testing
tokensPerSec=$2
zipfExponent=$3

#three source can use, sourceProducer, sourceProducerZipf, sourceProducerZipfRps
echo "machine $machine is inputing data and waiting for processes end"
gnome-terminal -- java -cp /home/nsd/liang_you_git_repo/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar sourceProducer $bootstrapServers $schemaRegistryUrl $numOfPartitions $numOfAccounts $initBalance $amountPerTransaction $zipfExponent $tokensPerSec $executionTime $machine

echo -e "\nEnd. "


