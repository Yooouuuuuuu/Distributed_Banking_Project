#!/bin/bash

#args used in java

machine=1
if [ $machine -eq 2 ] 
then
    bootstrapServers="192.168.50.213:9092"
    schemaRegistryUrl="http://192.168.50.213:8081"
else
    bootstrapServers="192.168.50.224:9092"
    schemaRegistryUrl="http://192.168.50.224:8081"
fi

mkdir -p /home/nsd/liang_you_git_repo/Distributed_Banking_Project/scripts/result
touch /home/nsd/liang_you_git_repo/Distributed_Banking_Project/scripts/result/result.txt

#100000 is the roughly number of data, use to decide how long we should wait until polling finish. no need to be the exact number of data.
java -cp /home/nsd/liang_you_git_repo/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar tpsRpsAndLatency $bootstrapServers $schemaRegistryUrl $1

echo -e "\nEnd. "

