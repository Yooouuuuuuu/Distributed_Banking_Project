#!/bin/sh

sshpass -p nsd ssh nsd@140.119.164.32 -p 9010 << MACHINE1
echo 'Access into machine 1 (port:9010)'

#kill Kafka consumers
pkill -f 'distributed-payment-v1-1.0-SNAPSHOT.jar'

echo 'Exit machine 1 (port:9010)'
exit
MACHINE1

