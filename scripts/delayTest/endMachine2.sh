#!/bin/sh

sshpass -p nsd ssh nsd@140.119.164.32 -p 9011 << MACHINE2
echo 'Access into machine 2 (port:9011)'

#kill Kafka consumers
pkill -f 'distributed-payment-v1-1.0-SNAPSHOT.jar'

echo 'Exit machine 2 (port:9011)'
exit
MACHINE2

