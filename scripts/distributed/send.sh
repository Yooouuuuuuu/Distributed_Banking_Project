#!/bin/bash

#args used in java
machine=$1

bootstrapServers="127.0.0.1:9092"
schemaRegistryUrl="http://127.0.0.1:8081"

numOfPartitions=2
numOfAccounts=1000
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


