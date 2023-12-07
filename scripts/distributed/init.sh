#!/bin/bash

#args used in java

machine=$1

bootstrapServers="127.0.0.1:9092"
schemaRegistryUrl="http://127.0.0.1:8081"

numOfPartitions=2
numOfAccounts=1000
numOfReplicationFactor=1
initBalance=100000000
orderMultiplePartition="true"
UTXODoNotAgg="true"
logger="off" #"off", "trace", "debug", "info", "warn", "error"

echo "machine $machine initialize kafka topics"
java -cp /home/nsd/liang_you_git_repo/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar initialize $bootstrapServers $schemaRegistryUrl $numOfPartitions $numOfAccounts $numOfReplicationFactor $initBalance $orderMultiplePartition $UTXODoNotAgg $logger

echo -e "\nEnd. "


