#!/bin/sh

#scripts using and arguments passing
#init.sh machineNum
#runMachine.sh machineNum 
#sendMachine.sh machineNum tokensPerSec
#endMachine.sh
#writeToCsv.sh machineNum tokensPerSec

#for run.sh
validatorOrBaseline=validator #validator or baseline
validatorMaxPoll=2000
UTXOMaxPoll=10000
aggregatorMaxPoll=2000
blockSize=3000

#for send.sh
zipfExponent=0
waitTime=150
#tokensPerSec=300000

#`seq 10000 10000 200000`
for tokensPerSec in 300000
do
echo '=== RPS: '$tokensPerSec', '$validatorOrBaseline' ===' 
#using machine 1 to initialize Kafka topics
sshpass -p nsd ssh nsd@140.119.164.32 -p 9010 << MACHINE1
echo '=== Access into machine 1 (port:9010) ==='

echo 'init Kafka'
cd /home/nsd/liang_you_git_repo/Distributed_Banking_Project/scripts/distributed
./init.sh 1

echo '=== Exit machine 1 (port:9010) ==='
exit
MACHINE1

#open consumers 
echo '=== open consumers ==='
gnome-terminal -- ./runMachine1.sh 1
sleep 60s

#sending data
echo '=== sending data ==='
gnome-terminal -- ./sendMachine2.sh 2
sleep $((waitTime))s

#close consumers
echo '=== close consumers ==='
gnome-terminal -- ./endMachine1.sh
gnome-terminal -- ./endMachine2.sh

done



