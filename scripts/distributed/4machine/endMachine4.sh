#!/bin/sh

delay=$1

sshpass -p nsd ssh nsdlab@140.119.164.32 -p 9013 << MACHINE1
echo 'Access into machine 1 (port:9010)'

if [ delay != 0 ]
then
    echo nsd | sudo -S tc qdisc del dev enx5c925ed76abd root netem delay '$delay'ms
fi

#kill Kafka consumers
pkill -f 'distributed-payment-v1-1.0-SNAPSHOT.jar'

echo 'Exit machine 4 (port:9013)'
exit
MACHINE1

