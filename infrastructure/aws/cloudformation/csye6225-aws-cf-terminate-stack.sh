#!/bin/bash

echo "Enter your stack name [ENTER]: "
read stack
aws cloudformation delete-stack --stack-name ${stack}
i=1
sp="/-\|"
while true
do
  printf "\b${sp:i++%${#sp}:1}"
done &
trap "kill $!" EXIT
aws cloudformation wait stack-delete-complete --stack-name ${stack}
echo '\n'
kill $! && trap " " EXIT
echo "Stack ${stack} was deleted successfully!"