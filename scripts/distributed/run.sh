#!/bin/bash

#args used in java
machine=$1

then
    bootstrapServers="192.168.50.213:9092"
    schemaRegistryUrl="http://192.168.50.213:8081"
else
    bootstrapServers="192.168.50.224:9092"
    schemaRegistryUrl="http://192.168.50.224:8081"
fi

validatorOrBaseline=$2

numOfPartitions=2
numOfAccounts=1000
blockTimeout=1000
orderMultiplePartition="true"
UTXODirectAdd="true"
logger="error" #"off", "trace", "debug", "info", "warn", "error"

#need to change for testing
validatorMaxPoll=$3
UTXOMaxPoll=$4
aggregatorMaxPoll=$5
blockSize=$6

if [ "validator" = $2 ]
then
echo "machine $machine Open a aggregator and a validator"
gnome-terminal -- java -cp /home/nsd/liang_you_git_repo/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar validator $bootstrapServers $schemaRegistryUrl $validatorMaxPoll $UTXOMaxPoll $orderMultiplePartition $UTXODirectAdd ${machine}validator $logger & 
gnome-terminal -- java -cp /home/nsd/liang_you_git_repo/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar aggregator $bootstrapServers $schemaRegistryUrl $numOfPartitions $aggregatorMaxPoll $blockSize $blockTimeout ${machine}aggregator $logger &
else
echo "machine $machine Open a aggregatorBaseline and a validatorBaseline"
gnome-terminal -- java -cp /home/nsd/liang_you_git_repo/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar validatorBaseline $bootstrapServers $schemaRegistryUrl $validatorMaxPoll $orderMultiplePartition $UTXODirectAdd ${machine}validator $logger & 
gnome-terminal -- java -cp /home/nsd/liang_you_git_repo/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar aggregatorBaseline $bootstrapServers $schemaRegistryUrl $numOfPartitions $aggregatorMaxPoll $blockSize $blockTimeout ${machine}aggregator $logger &
fi

echo -e "\nEnd. "

