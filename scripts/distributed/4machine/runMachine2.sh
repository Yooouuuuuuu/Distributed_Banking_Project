#!/bin/sh

sshpass -p nsd ssh -X nsd@140.119.164.32 -p 9011 << MACHINE2
echo 'Access into machine 2 (port:9011)'

cd /home/nsd/liang_you_git_repo/Distributed_Banking_Project/scripts/distributed
./run.sh $1 $2 $3 $4 $5 $6 $7 $8 $9 $10

echo 'Exit machine 2 (port:9011)'
exit
MACHINE2

