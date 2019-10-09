#!/bin/bash

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


stackId=$(aws cloudformation create-stack --stack-name ${stack} --template-body file://csye6225-cf-networking.json --parameters ParameterKey=vpcCidrBlock,ParameterValue=$vpcCidrBlock ParameterKey=availabilityZoneA,ParameterValue=$availabilityZoneA ParameterKey=subNetCidrBlockA,ParameterValue=$subNetCidrBlockA ParameterKey=availabilityZoneB,ParameterValue=$availabilityZoneB ParameterKey=subNetCidrBlockB,ParameterValue=$subNetCidrBlockB ParameterKey=availabilityZoneC,ParameterValue=$availabilityZoneC ParameterKey=subNetCidrBlockC,ParameterValue=$subNetCidrBlockC ParameterKey=DestinationCidrBlock,ParameterValue=$DestinationCidrBlock --query [StackId] --output text)

if [ -z $stackId ]; then
    echo ' fail: stack was not created created!'
else
    aws cloudformation wait stack-create-complete --stack-name $stackId
    echo "success: Stack was created created!"
fi
