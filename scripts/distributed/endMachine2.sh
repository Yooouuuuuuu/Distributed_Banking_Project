#!/bin/sh

delay=$1

sshpass -p nsd ssh nsd@140.119.164.32 -p 9011 << MACHINE2
echo 'Access into machine 2 (port:9011)'

if [ delay != 0 ]
then
    echo nsd | sudo -S tc qdisc del dev enp52s0 root netem delay '$delay'ms
fi

#kill Kafka consumers
pkill -f 'distributed-payment-v1-1.0-SNAPSHOT.jar'

echo 'Exit machine 2 (port:9011)'
exit
MACHINE2

