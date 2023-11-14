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
blockSize=1000

#for send.sh
waitTime=300
zipfExponent=0


#`seq 10000 10000 200000`
for tokensPerSec in 150000
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
gnome-terminal -- ./runMachine1.sh 1 $validatorOrBaseline $validatorMaxPoll $UTXOMaxPoll $aggregatorMaxPoll $blockSize
gnome-terminal -- ./runMachine2.sh 2 $validatorOrBaseline $validatorMaxPoll $UTXOMaxPoll $aggregatorMaxPoll $blockSize
sleep 30s

#sending data
echo '=== sending data ==='
gnome-terminal -- ./sendMachine1.sh 1 $((tokensPerSec/2)) $zipfExponent
gnome-terminal -- ./sendMachine2.sh 2 $((tokensPerSec/2)) $zipfExponent
sleep $((waitTime))s

#close consumers
echo '=== close consumers ==='
gnome-terminal -- ./endMachine1.sh
gnome-terminal -- ./endMachine2.sh
sleep 5s

echo "=== calculate TPS ===" 
#read and sort timestamps
sshpass -p nsd ssh nsd@140.119.164.32 -p 9011 << MACHINE2
echo 'Access into machine 2 (port:9011)'

cd /home/nsd/liang_you_git_repo/Distributed_Banking_Project/scripts/distributed
./result.sh $waitTime

echo 'Exit machine 2 (port:9011)'
exit
MACHINE2
done



