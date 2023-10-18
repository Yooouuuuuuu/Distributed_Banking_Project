#!/bin/sh

#scripts using and arguments passing
#init.sh machineNum
#runMachine.sh machineNum 
#sendMachine.sh machineNum tokensPerSec
#endMachine.sh
#writeToCsv.sh machineNum tokensPerSec

for tokensPerSec in 10000 20000
do
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
gnome-terminal -- ./runMachine2.sh 2
sleep 30s

#sending data
echo '=== sending data ==='
gnome-terminal -- ./sendMachine1.sh 1 $tokensPerSec
gnome-terminal -- ./sendMachine2.sh 2 $tokensPerSec
sleep 300s

#close consumers
echo '=== close consumers ==='
gnome-terminal -- ./endMachine1.sh
gnome-terminal -- ./endMachine2.sh

#read and sort timestamps
sshpass -p nsd ssh nsd@140.119.164.32 -p 9011 << MACHINE2
echo '=== Access into machine 2 (port:9011) ==='

echo '=== read and sort timestamps ==='
cd /home/nsd/liang_you_git_repo/Distributed_Banking_Project/scripts/distributed
./writeToCsv.sh 2 $tokensPerSec

echo '=== Exit machine 2 (port:9011) ==='
exit
MACHINE2
done



