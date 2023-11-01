#!/bin/bash

#args used in java

bootstrapServers="140.119.164.32:9092"
schemaRegistryUrl="http://140.119.164.32:8081"


numOfPartitions=2
numOfAccounts=1000
initBalance=100000000
amountPerTransaction=1

#need to change for testing
zipfExponent=1
tokensPerSec=1000000
executionTime=10000

outputFile=0
machine=0

#three source can use, sourceProducer, sourceProducerZipf, sourceProducerZipfRps
echo "machine $machine is inputing data and waiting for processes end"
java -cp /home/yooouuuuuuu/project/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar sourceProducer $bootstrapServers $schemaRegistryUrl $numOfPartitions $numOfAccounts $initBalance $amountPerTransaction $zipfExponent $tokensPerSec $executionTime $outputFile $machine

echo -e "\nEnd. "


