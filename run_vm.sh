#!/bin/bash
############################################
# args[0]: # of partitions
# args[1]: # of transactions
# args[2]: "max.poll.records"
# args[3]: batch processing
# args[4]: poll from localBalance while repartition
# args[5]: credit topic exist
# args[6]: direct write to successful
# args[7]: one partition only, skip balancer.
############################################

p=3
n=10000000
m=1000
args3="true"
args4="true"
args5="true"
args6="false"
args7="true"
args8='34.125.104.54'
waitTime=100
#secs wait for validations

echo "=== Initialize kafka topics === "8
java -cp /home/g110753209/consumers/kafka_payment/target/kafkaPayment-1.0-SNAPSHOT.jar initialize $p $n $m $args3 $args4 $args5 $args6 $args7 $args8

read -p "Press enter to continue"

echo "=== input data === "
java -cp /home/g110753209/consumers/kafka_payment/target/kafkaPayment-1.0-SNAPSHOT.jar sourceProducer $p $n $m $args3 $args4 $args5 $args6 $args7 $args8
echo "=== finish inputing data, waiting for validations === "

sleep $waitTime
echo "=== check validators and balancers, then kill all === "
ps aux | grep 'kafkaPayment-1.0-SNAPSHOT.jar'
#kill -f 'kafkaPayment-1.0-SNAPSHOT.jar'

echo "=== consume timestamps === "
java -cp /home/g110753209/consumers/kafka_payment/target/kafkaPayment-1.0-SNAPSHOT.jar consumeTimestamps $p $n $m $args3 $args4 $args5 $args6 $args7 $args8
#java -cp /home/user/liang_you/project/kafka_payment/target/kafkaPayment-1.0-SNAPSHOT.jar sortTimestamps $p $n $m $args3 $args4 $args5 $args6 $args7 $args8

echo "=== bigTX head === "
head /home/g110753209/TXtimestamps/bigTX.txt
echo "=== balance tail === "
tail /home/g110753209/TXtimestamps/balance.txt
