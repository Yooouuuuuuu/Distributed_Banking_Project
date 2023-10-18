#!/bin/sh

sshpass -p nsd ssh nsd@140.119.164.32 -p 9010 << MACHINE1
echo '=== Access into machine 1 (port:9010) ==='

cd /home/nsd/liang_you_git_repo/Distributed_Banking_Project/scripts/distributed
./send.sh $1 $2

echo '=== Exit machine 1 (port:9010) ==='
exit
MACHINE1

