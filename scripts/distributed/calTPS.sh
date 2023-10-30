#!/bin/sh

sshpass -p nsd ssh nsd@140.119.164.32 -p 9011 << MACHINE2
echo 'Access into machine 2 (port:9011)'

cd /home/nsd/liang_you_git_repo/Distributed_Banking_Project/scripts/distributed
./TPS.sh

echo 'Exit machine 2 (port:9011)'
exit
MACHINE2

