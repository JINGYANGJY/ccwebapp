#!/bin/bash
# create-aws-vpc

echo "Enter vpc-cidr-block and press [ENTER]: "
read vpcCidrBlock

echo "Creating VPC..."

vpcId=$(aws ec2 create-vpc --cidr-block "$vpcCidrBlock" --query [Vpc.VpcId] --output text)
echo $vpcId
aws_response=$(aws ec2 describe-vpcs --filters Name=vpc-id,Values=${vpcId}  --output json)
echo $aws_response


# create subnet for vpc
# Show availability zones
aws ec2 describe-availability-zones

create_subnet () {
  echo "Enter availability-zone and press [ENTER]: "
  read availabilityZone
  echo "Enter sub-net-cidr-block and press [ENTER]: "
  read subNetCidrBlock
  aws ec2 create-subnet \
    --cidr-block "$subNetCidrBlock" \
    --availability-zone "$availabilityZone" \
    --vpc-id "$vpcId" \
    --output json
}


echo "Create first subnet"
create_subnet

SubnetA=$(aws ec2 describe-subnets --filters Name=vpc-id,Values=${vpcId} --query [Subnets[0].SubnetId] --output text)

echo "Create second subnet"
create_subnet

SubnetB=$(aws ec2 describe-subnets --filters Name=vpc-id,Values=${vpcId}  --query [Subnets[1].SubnetId] --output text)

echo "Create third subnet"
create_subnet

SubnetC=$(aws ec2 describe-subnets --filters Name=vpc-id,Values=${vpcId}  --query [Subnets[2].SubnetId] --output text)


echo "Creating internet-gateway ..."
aws ec2 create-internet-gateway
internetGatewayId=$(aws ec2 create-internet-gateway --query [InternetGateway.InternetGatewayId] --output text)


aws ec2 attach-internet-gateway --vpc-id $vpcId --internet-gateway-id $internetGatewayId

echo "Creating route-table ..."
routeTableId=$(aws ec2 create-route-table --vpc-id $vpcId --query [RouteTable.RouteTableId] --output text)

aws ec2 create-route --route-table-id $routeTableId --destination-cidr-block 0.0.0.0/0 --gateway-id $internetGatewayId

echo "Associate route-table with subnet ..."

aws ec2 associate-route-table --route-table-id $routeTableId --subnet-id $SubnetA

aws ec2 associate-route-table --route-table-id $routeTableId --subnet-id $SubnetB

aws ec2 associate-route-table --route-table-id $routeTableId --subnet-id $SubnetC


echo "\n"
echo "All set"
exit 0