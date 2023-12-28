#!/bin/sh

sshpass -p nsd ssh -X nsdlab@140.119.164.32 -p 9012 << MACHINE1
echo 'Access into machine 1 (port:9010)'

cd /home/nsdlab/liang_you_git_repo/Distributed_Banking_Project/scripts/distributed/4machine
./run2.sh $1 $2 $3 $4 $5 $6 $7 $8

echo 'Exit machine 3 (port:9012)'
exit
MACHINE1

