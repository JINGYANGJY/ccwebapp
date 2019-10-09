#!/bin/bash
# create-aws-vpc

echo "Enter vpc-cidr-block and press [ENTER]: "
read vpcCidrBlock

echo "Creating VPC..."
# create vpc with cidr block /16
aws_response=$(aws ec2 create-vpc \
 --cidr-block "$vpcCidrBlock" \
 --output json)
echo $aws_response

echo "Enter the vpc-id shown above to store it and press [ENTER]: "
read vpcID

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
    --vpc-id "$vpcID" \
    --output json
}


echo "Create first subnet"
create_subnet

echo "Enter the subnet-id shown above to store it and press [ENTER]: "
read SubnetA

echo "Create second subnet"
create_subnet

echo "Enter the subnet-id shown above to store it and press [ENTER]: "
read SubnetB

echo "Create third subnet"
create_subnet

echo "Enter the subnet-id shown above to store it and press [ENTER]: "
read SubnetC


echo "Creating internet-gateway ..."
aws ec2 create-internet-gateway
echo "Enter the gateway-id shown above to store it and press [ENTER]: "
read internetGatewayId

aws ec2 attach-internet-gateway --vpc-id $vpcID --internet-gateway-id $internetGatewayId

echo "Creating route-table ..."
aws ec2 create-route-table --vpc-id $vpcID
echo "Enter the RouteTableId shown above to store it and press [ENTER]: "
read routeTableId

aws ec2 create-route --route-table-id $routeTableId --destination-cidr-block 0.0.0.0/0 --gateway-id $internetGatewayId

echo "Associate route-table with subnet ..."

aws ec2 associate-route-table --route-table-id $routeTableId --subnet-id $SubnetA

aws ec2 associate-route-table --route-table-id $routeTableId --subnet-id $SubnetB

aws ec2 associate-route-table --route-table-id $routeTableId --subnet-id $SubnetC


echo "\n"
echo "All set"
exit 0