#!/bin/sh

#init Kafka
sshpass -p nsd ssh nsd@140.119.164.32 -p 9010 << MACHINE1
echo '=== Access into machine 1 (port:9010) ==='

echo 'init Kafka'
cd /home/nsd/liang_you_git_repo/Distributed_Banking_Project/scripts/distributed
./init.sh

echo '=== Exit machine 1 (port:9010) ==='
exit
MACHINE1

#open consumers
echo '=== open consumers ==='
gnome-terminal -- ./runMachine1.sh
gnome-terminal -- ./runMachine2.sh
sleep 30s

#sending data
echo '=== sending data ==='
gnome-terminal -- ./sendMachine1.sh
gnome-terminal -- ./sendMachine2.sh
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
./writeToCsv.sh

echo '=== Exit machine 2 (port:9011) ==='
exit
MACHINE2

