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
#n=50000000
#m=1000
#args3="true"
#args4="true"
#args5="true"
#args6="false"
#args7="false"
#waitTime=1000 #secs wait for validations

echo "=== Initialize kafka topics === "
java -cp /home/yooouuuuuuu/project/test/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar initialize_v3

echo "=== Open === "
for i in $( eval echo {1..$p} )
  do gnome-terminal -- java -cp /home/yooouuuuuuu/project/test/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar aggregator_v2
done

for i in $( eval echo {1..$p} )
  do gnome-terminal -- java -cp /home/yooouuuuuuu/project/test/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar validator_v3
done

gnome-terminal -- java -cp /home/yooouuuuuuu/project/test/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar sumUTXO_v3

# wait for rebalance
sleep 20

echo "=== input data === "
gnome-terminal -- java -cp /home/yooouuuuuuu/project/test/distributed_payment/target/distributed-payment-v1-1.0-SNAPSHOT.jar sourceProducer_v3

# wait for validations and kill them all
#sleep $waitTime
#pkill -f 'distributed-payment-v1-1.0-SNAPSHOT.jar'


