#!/bin/sh

sshpass -p nsd ssh -X nsd@140.119.164.32 -p 9013 << MACHINE1
echo 'Access into machine 1 (port:9010)'

cd /home/nsdlab/liang_you_git_repo/Distributed_Banking_Project/scripts/distributed
./send.sh $1 $2 $3

echo 'Exit machine 4 (port:9013)'
exit
MACHINE1

