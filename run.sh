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

p=5
n=50000000
m=1000
args3="true"
args4="true"
args5="true"
args6="false"
args7="false"
waitTime=1000 #secs wait for validations

echo "=== Initialize kafka topics === "
java -cp /home/user/liang_you/project/kafka_payment/target/kafkaPayment-1.0-SNAPSHOT.jar initialize $p $n $m $args3 $args4 $args5 $args6

if [ $args7 = "false" ]
then
  echo "=== open validators & balancers === "
  for i in $( eval echo {1..$p} )
    do gnome-terminal -- java -cp /home/user/liang_you/project/kafka_payment/target/kafkaPayment-1.0-SNAPSHOT.jar validator $p $n $m $args3 $args4 $args5 $args6 
  done

  for i in $( eval echo {1..$p} )
    do gnome-terminal -- java -cp /home/user/liang_you/project/kafka_payment/target/kafkaPayment-1.0-SNAPSHOT.jar balancer $p $n $m $args3 $args4 $args5 $args6
  done
else
  echo "=== open validator === "
  gnome-terminal -- java -cp /home/user/liang_you/project/kafka_payment/target/kafkaPayment-1.0-SNAPSHOT.jar validateToBalance 1 $n $m $args3 $args4 $args5 $args6
fi

# wait for rebalance
sleep 30 

echo "=== input data === "
java -cp /home/user/liang_you/project/kafka_payment/target/kafkaPayment-1.0-SNAPSHOT.jar sourceProducer $p $n $m $args3 $args4 $args5 $args6

# wait for validations and kill them all
sleep $waitTime
pkill -f 'kafkaPayment-1.0-SNAPSHOT.jar'

echo "=== consume, sort and write to csv === "
java -cp /home/user/liang_you/project/kafka_payment/target/kafkaPayment-1.0-SNAPSHOT.jar consumeTimestamps $p $n $m $args3 $args4 $args5 $args6 $args7
java -cp /home/user/liang_you/project/kafka_payment/target/kafkaPayment-1.0-SNAPSHOT.jar sortTimestamps $p $n $m $args3 $args4 $args5 $args6 $args7


