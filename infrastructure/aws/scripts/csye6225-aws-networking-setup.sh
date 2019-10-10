#!/bin/bash
# create-aws-vpc

echo "Enter vpc-cidr-block and press [ENTER]: "
read vpcCidrBlock

echo "Creating VPC..."

vpcId=$(aws ec2 create-vpc --cidr-block "$vpcCidrBlock" --query [Vpc.VpcId] --output text)
echo $vpcId
aws_response=$(aws ec2 describe-vpcs --filters Name=vpc-id,Values=${vpcId}  --output json)
echo $aws_response
echo "Enter vpc-name and press [ENTER]: "
read vpcName
aws ec2 create-tags --resources $vpcId --tags Key=Name,Value="$vpcName"

# create subnet for vpc
# Show availability zones
aws ec2 describe-availability-zones


echo "Create first subnet"
echo "Enter availability-zone and press [ENTER]: "
read availabilityZoneA
echo "Enter sub-net-cidr-block and press [ENTER]: "
read subNetCidrBlockA
SubnetA=$(aws ec2 create-subnet --availability-zone $availabilityZoneA --vpc-id $vpcId --cidr-block $subNetCidrBlockA --query [Subnet.SubnetId] --output text)
aws ec2 create-tags --resources $SubnetA --tags Key=Name,Value="$vpcName-subnet1"

echo "Create second subnet"
echo "Enter availability-zone and press [ENTER]: "
read availabilityZoneB
echo "Enter sub-net-cidr-block and press [ENTER]: "
read subNetCidrBlockB
SubnetB=$(aws ec2 create-subnet --availability-zone $availabilityZoneB --vpc-id $vpcId --cidr-block $subNetCidrBlockB --query [Subnet.SubnetId] --output text)
aws ec2 create-tags --resources $SubnetB --tags Key=Name,Value="$vpcName-subnet2"

echo "Create third subnet"
echo "Enter availability-zone and press [ENTER]: "
read availabilityZoneC
echo "Enter sub-net-cidr-block and press [ENTER]: "
read subNetCidrBlockC
SubnetC=$(aws ec2 create-subnet --availability-zone $availabilityZoneC --vpc-id $vpcId --cidr-block $subNetCidrBlockC --query [Subnet.SubnetId] --output text)
aws ec2 create-tags --resources $SubnetC --tags Key=Name,Value="$vpcName-subnet3"


echo "Creating internet-gateway ..."
internetGatewayId=$(aws ec2 create-internet-gateway --query [InternetGateway.InternetGatewayId] --output text)
aws ec2 create-tags --resources $internetGatewayId --tags Key=Name,Value="$vpcName-ig"

aws ec2 attach-internet-gateway --vpc-id $vpcId --internet-gateway-id $internetGatewayId

echo "Creating route-table ..."
routeTableId=$(aws ec2 create-route-table --vpc-id $vpcId --query [RouteTable.RouteTableId] --output text)
aws ec2 create-tags --resources $routeTableId --tags Key=Name,Value="$vpcName-rt"

aws ec2 create-route --route-table-id $routeTableId --destination-cidr-block 0.0.0.0/0 --gateway-id $internetGatewayId

echo "Associate route-table with subnet ..."

aws ec2 associate-route-table --route-table-id $routeTableId --subnet-id $SubnetA

aws ec2 associate-route-table --route-table-id $routeTableId --subnet-id $SubnetB

aws ec2 associate-route-table --route-table-id $routeTableId --subnet-id $SubnetC


echo "\n"
echo "All set"
exit 0