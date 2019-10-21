#!/bin/bash


IPV4REGEX="^((25[0-5]|2[0-4][0-9]|[01]?[0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9]?)(\/([0-9]|[1-2][0-9]|3[0-2]))$"

echo "Enter your stack name [ENTER]: "
read stack

echo "Enter vpc-cidr-block and press [ENTER]: "
read vpcCidrBlock

echo "Enter first availability-zone and press [ENTER]: "
read availabilityZoneA

echo "Enter first sub-net-cidr-block and press [ENTER]: "
read subNetCidrBlockA

echo "Enter second availability-zone and press [ENTER]: "
read availabilityZoneB

echo "Enter second sub-net-cidr-block and press [ENTER]: "
read subNetCidrBlockB

echo "Enter third availability-zone and press [ENTER]: "
read availabilityZoneC

echo "Enter third sub-net-cidr-block and press [ENTER]: "
read subNetCidrBlockC

echo "Enter DestinationCidrBlock and press [ENTER]: "
read DestinationCidrBlock

if [[ "$vpcCidrBlock" =~ $IPV4REGEX ]] && [[ "$subNetCidrBlockA" =~ $IPV4REGEX ]] && [[ "$subNetCidrBlockB" =~ $IPV4REGEX ]] && [[ "$subNetCidrBlockC" =~ $IPV4REGEX ]]

then 

  echo "Start"
StackStatusReason=$(aws cloudformation create-stack --stack-name ${stack} --template-body file://csye6225-cf-networking.json --parameters ParameterKey=vpcCidrBlock,ParameterValue=$vpcCidrBlock ParameterKey=availabilityZoneA,ParameterValue=$availabilityZoneA ParameterKey=subNetCidrBlockA,ParameterValue=$subNetCidrBlockA ParameterKey=availabilityZoneB,ParameterValue=$availabilityZoneB ParameterKey=subNetCidrBlockB,ParameterValue=$subNetCidrBlockB ParameterKey=availabilityZoneC,ParameterValue=$availabilityZoneC ParameterKey=subNetCidrBlockC,ParameterValue=$subNetCidrBlockC ParameterKey=DestinationCidrBlock,ParameterValue=$DestinationCidrBlock --output json)

else

  echo "Invalid Input"

  exit 0

fi
