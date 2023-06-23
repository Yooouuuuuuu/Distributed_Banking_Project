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
amountPerTransaction=100 #sourceProducer only

UTXOUpdatePeriod=25000 #validator only
UTXOUpdateBreakTime=1000 #validator only
successfulMultiplePartition="true"
UTXODoNotAgg="false"
randomAmount="false" #1000-100000

numOfaggregators=1
numOfvalidators=3
#waitTime=1000 #secs wait for validations

for i in $( eval echo {1..$numOfvalidators} )
do echo "${i}validator"
done


