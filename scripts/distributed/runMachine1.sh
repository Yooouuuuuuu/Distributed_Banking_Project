#!/bin/sh

sshpass -p nsd ssh -X nsd@140.119.164.32 -p 9010 << MACHINE1
echo 'Access into machine 1 (port:9010)'

cd /home/nsd/liang_you_git_repo/Distributed_Banking_Project/scripts/distributed
./run.sh $1 $2 $3 $4 $5 $6 $7 $8

echo 'Exit machine 1 (port:9010)'
exit
MACHINE1

