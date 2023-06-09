#!/bin/bash
############################################
#args[0]: bootstrap.Servers
#args[1]: schema.RegistryUrl
#args[2]: # of partitions
#args[3]: # of accounts
#args[4]: # of replica
#args[5]: init balance of each bank
#args[6]: "max.poll.records"
#args[7]: block size

#args[8]: timeout of aggregating transactions to a block for aggregator

#args[9]: timeout of aggregating transactions as UTXO for sumUTXO
        
#args[10]: # of data (transactions)
#args[11]: amount per transaction

#args[11]: timeout of validator update accounts' UTXO
#args[12]: maximum time for validator to update UTXO
#args[13]: randomly update UTXO or not
############################################

bootstrapServers="127.0.0.1:9092"
schemaRegistryUrl="http://127.0.0.1:8081"
numOfPartitions=3
numOfAccounts=10
numOfReplicationFactor=1
initBalance=1000000
maxPoll=1000
blockSize=500

blockTimeout=10000 #aggregator only

aggUTXOTime=10000 #sumUTXO only

numOfData=100000 #sourceProducer only
amountPerTransaction=500 #sourceProducer only

UTXOUpdatePeriod=25000 #validator only
UTXOUpdateBreakTime=1000 #validator only
successfulMultiplePartition="false"
UTXODoNotAgg="false"
randomAmount="false" #1-1000

numOfaggregators=3
numOfvalidators=3
waitTime=1000 #secs wait for validations

echo "=== Initialize kafka topics === "
java -cp /home/yooouuuuuuu/git-repos/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar initialize $bootstrapServers $schemaRegistryUrl $numOfPartitions $numOfAccounts $numOfReplicationFactor $initBalance $maxPoll $blockSize $blockTimeout $aggUTXOTime $numOfData $amountPerTransaction $UTXOUpdatePeriod $UTXOUpdateBreakTime $successfulMultiplePartition $UTXODoNotAgg $randomAmount

echo "=== Open aggregators, validator and a sumUTXO === "
for i in $( eval echo {1..$numOfaggregators} )
  do gnome-terminal -- java -cp /home/yooouuuuuuu/git-repos/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar aggregator $bootstrapServers $schemaRegistryUrl $numOfPartitions $numOfAccounts $numOfReplicationFactor $initBalance $maxPoll $blockSize $blockTimeout $aggUTXOTime $numOfData $amountPerTransaction $UTXOUpdatePeriod $UTXOUpdateBreakTime $successfulMultiplePartition $UTXODoNotAgg $randomAmount
done

for i in $( eval echo {1..$numOfvalidators} )
  do gnome-terminal -- java -cp /home/yooouuuuuuu/git-repos/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar validator $bootstrapServers $schemaRegistryUrl $numOfPartitions $numOfAccounts $numOfReplicationFactor $initBalance $maxPoll $blockSize $blockTimeout $aggUTXOTime $numOfData $amountPerTransaction $UTXOUpdatePeriod $UTXOUpdateBreakTime $successfulMultiplePartition $UTXODoNotAgg $randomAmount
done

gnome-terminal -- java -cp /home/yooouuuuuuu/git-repos/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar sumUTXO $bootstrapServers $schemaRegistryUrl $numOfPartitions $numOfAccounts $numOfReplicationFactor $initBalance $maxPoll $blockSize $blockTimeout $aggUTXOTime $numOfData $amountPerTransaction $UTXOUpdatePeriod $UTXOUpdateBreakTime $successfulMultiplePartition $UTXODoNotAgg $randomAmount

echo "=== wait for rebalance === "
sleep 20

echo "=== input data === "
gnome-terminal -- java -cp /home/yooouuuuuuu/git-repos/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar sourceProducer $bootstrapServers $schemaRegistryUrl $numOfPartitions $numOfAccounts $numOfReplicationFactor $initBalance $maxPoll $blockSize $blockTimeout $aggUTXOTime $numOfData $amountPerTransaction $UTXOUpdatePeriod $UTXOUpdateBreakTime $successfulMultiplePartition $UTXODoNotAgg $randomAmount

echo "=== wait for processes end and kill === " 
#sleep $waitTime
read -n 1 -s -r -p "Press any key to continue"
pkill -f 'distributed-payment-v1-1.0-SNAPSHOT.jar'

echo -e "\n=== poll from transactions and successful topics === " 
gnome-terminal -- java -cp /home/yooouuuuuuu/git-repos/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar test/consumeTransactions $bootstrapServers $schemaRegistryUrl $maxPoll
gnome-terminal -- java -cp /home/yooouuuuuuu/git-repos/Distributed_Banking_Project/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar test/consumeSuccessful $bootstrapServers $schemaRegistryUrl $numOfPartitions $numOfAccounts $successfulMultiplePartition

read -n 1 -s -r -p "Press any key to end."
pkill -f 'distributed-payment-v1-1.0-SNAPSHOT.jar'
echo -e "\nEnd. "


