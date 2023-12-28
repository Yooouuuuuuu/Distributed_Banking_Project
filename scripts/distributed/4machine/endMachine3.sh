#!/bin/sh

delay=$1

sshpass -p nsd ssh nsdlab@140.119.164.32 -p 9012 << MACHINE1
echo 'Access into machine 1 (port:9010)'

if [ delay != 0 ]
then
    echo nsd | sudo -S tc qdisc del dev enx5c925ed762c6 root netem delay '$delay'ms
fi

#kill Kafka consumers
pkill -f 'distributed-payment-v1-1.0-SNAPSHOT.jar'

echo 'Exit machine 3 (port:9012)'
exit
MACHINE1

