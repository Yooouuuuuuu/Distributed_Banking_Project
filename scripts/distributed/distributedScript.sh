#!/bin/sh

#scripts using and arguments passing
#init.sh machineNum
#runMachine.sh machineNum 
#sendMachine.sh machineNum tokensPerSec
#endMachine.sh
#writeToCsv.sh machineNum tokensPerSec

#for run.sh
validatorOrBaseline=t #validator or baseline
validatorMaxPoll=2000000
UTXOMaxPoll=10000000
aggregatorMaxPoll=2000000
blockSize=3000
maxFetchBytes=1048576
acks=all

#for send.sh
zipfExponent=0
waitTime=300
tokensPerSec=100000
#tokensPerSec=300000

#init
numOfPartitions=2
numOfAccounts=1000
numOfReplicationFactor=1
delay=0
#delay=100

#for tokensPerSec in `seq 5000 5000 300000`
#for maxFetchBytes in `seq 5000 5000 200000`
#for blockSize in `seq 100 100 3000`
#for delay in `seq 0 5 100`

#for tokensPerSec in `seq 50 50 3000`
for tokensPerSec in 100000
do
echo '=== tokensPerSec: '$tokensPerSec', '$validatorOrBaseline' ===' 
#initialize Kafka topics and add delay
sshpass -p nsd ssh nsd@140.119.164.32 -p 9010 << MACHINE1
echo '=== Access into machine 1 (port:9010) ==='

echo 'init Kafka'
cd /home/nsd/liang_you_git_repo/Distributed_Banking_Project/scripts/distributed
./init.sh 1 $numOfPartitions $numOfAccounts $numOfReplicationFactor

if [ $delay != 0 ]
then
    echo nsd | sudo -S tc qdisc add dev enp52s0 root netem delay '$delay'ms
fi

echo '=== Exit machine 1 (port:9010) ==='
exit
MACHINE1

if [ $delay != 0 ]
then
sshpass -p nsd ssh nsd@140.119.164.32 -p 9011 << MACHINE2
    echo nsd | sudo -S tc qdisc add dev enp52s0 root netem delay '$delay'ms
exit
MACHINE2
fi


#open consumers 
echo '=== open consumers ==='
gnome-terminal -- ./runMachine1.sh 1 $validatorOrBaseline $validatorMaxPoll $UTXOMaxPoll $aggregatorMaxPoll $blockSize $maxFetchBytes $acks
gnome-terminal -- ./runMachine2.sh 2 $validatorOrBaseline $validatorMaxPoll $UTXOMaxPoll $aggregatorMaxPoll $blockSize $maxFetchBytes $acks
sleep 30s

#sending data
echo '=== sending data ==='
gnome-terminal -- ./sendMachine1.sh 1 $((tokensPerSec/2)) $zipfExponent
gnome-terminal -- ./sendMachine2.sh 2 $((tokensPerSec/2)) $zipfExponent
sleep $((waitTime))s

#close consumers
echo '=== close consumers ==='
gnome-terminal -- ./endMachine1.sh $delay
gnome-terminal -- ./endMachine2.sh $delay
sleep 5s

echo "=== calculate TPS ===" 
#read and sort timestamps
sshpass -p nsd ssh nsd@140.119.164.32 -p 9010 << MACHINE2
echo 'Access into machine 2 (port:9011)'

cd /home/nsd/liang_you_git_repo/Distributed_Banking_Project/scripts/distributed
./result.sh $waitTime

echo 'Exit machine 2 (port:9011)'
exit
MACHINE2
done



