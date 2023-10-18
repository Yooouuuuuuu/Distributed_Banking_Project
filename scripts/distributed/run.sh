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

numOfPartitions=2
numOfAccounts=1000
blockTimeout=1000
orderMultiplePartition="true"
UTXODirectAdd="true"
logger="error" #"off", "trace", "debug", "info", "warn", "error"

#need to change for testing
validatorMaxPoll=2000
aggregatorMaxPoll=2000
blockSize=1000



echo "machine $machine Open a aggregator and a validator"

#validator or validatorBaselineAgg
gnome-terminal -- java -cp /home/nsd/liang_you_git_repo/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar validatorBaselineAgg $bootstrapServers $schemaRegistryUrl $validatorMaxPoll $orderMultiplePartition $UTXODirectAdd ${machine}validator $logger & 

#aggregator or aggregatorForBaseline
gnome-terminal -- java -cp /home/nsd/liang_you_git_repo/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar aggregatorForBaseline $bootstrapServers $schemaRegistryUrl $numOfPartitions $aggregatorMaxPoll $blockSize $blockTimeout ${machine}aggregator $logger &

echo -e "\nEnd. "


