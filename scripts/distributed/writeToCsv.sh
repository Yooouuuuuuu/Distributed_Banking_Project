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

outputTxt1="/home/nsd/liang_you_git_repo/Distributed_Banking_Project/scripts/timeStamps/tokensPerSec$2/firstTimestamp.txt"
outputTxt2="/home/nsd/liang_you_git_repo/Distributed_Banking_Project/scripts/timeStamps/tokensPerSec$2/OriginalData.txt"
outputTxt3="/home/nsd/liang_you_git_repo/Distributed_Banking_Project/scripts/timeStamps/tokensPerSec$2/UTXO.txt"
outputTxt4="/home/nsd/liang_you_git_repo/Distributed_Banking_Project/scripts/timeStamps/tokensPerSec$2/untested.txt"
outputcsv="/home/nsd/liang_you_git_repo/Distributed_Banking_Project/scripts/timeStamps/tokensPerSec$2/orders.csv"
logger="off" #"off", "trace", "debug", "info", "warn", "error"

#need to change for testing
tokensPerSec=$2;
executionTime=10000;

echo "consume transactions -- write firstTimestamp, OriginalData & UTXO to txt files" 
#100000 is the roughly number of data, use to decide how long we should wait until polling finish. no need to be the exact number of data.
java -cp /home/nsd/liang_you_git_repo/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar test/writeTimestampsToTxt $bootstrapServers $schemaRegistryUrl $tokensPerSec $executionTime "off" $outputTxt1 $outputTxt2 $outputTxt3 $outputTxt4

echo "record and sort order timestamps" 
java -cp /home/nsd/liang_you_git_repo/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar test/sortTimestamps $outputTxt2 $outputTxt3 $outputTxt4 $outputTxt1 $outputcsv

echo -e "\nEnd. "

