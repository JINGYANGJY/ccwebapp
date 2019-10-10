echo "Enter VPC name:"
read VPC_NAME

#Get a vpc-Id using the name provided
vpcId=`aws ec2 describe-vpcs --filter "Name=tag:Name,Values=$VPC_NAME" --query 'Vpcs[*].{id:VpcId}' --output text`
#Get a Internet Gateway Id using the name provided
gatewayId=`aws ec2 describe-internet-gateways --filter "Name=tag:Name,Values=$VPC_NAME-ig" --query 'InternetGateways[*].{id:InternetGatewayId}' --output text`
#Get a route table Id using the name provided
routeTableId=`aws ec2 describe-route-tables --filter "Name=tag:Name,Values=$VPC_NAME-rt" --query 'RouteTables[*].{id:RouteTableId}' --output text`
#Get a Subnet Id using the name provided
publicSubnetId=`aws ec2 describe-subnets --filter "Name=tag:Name,Values=$VPC_NAME-subnet1" --query 'Subnets[*].{id:SubnetId}' --output text`
publicSubnetId1=`aws ec2 describe-subnets --filter "Name=tag:Name,Values=$VPC_NAME-subnet2" --query 'Subnets[*].{id:SubnetId}' --output text`
publicSubnetId2=`aws ec2 describe-subnets --filter "Name=tag:Name,Values=$VPC_NAME-subnet3" --query 'Subnets[*].{id:SubnetId}' --output text`

#Delete all subnets from the vpc
aws ec2 delete-subnet --subnet-id $publicSubnetId
aws ec2 delete-subnet --subnet-id $publicSubnetId1
aws ec2 delete-subnet --subnet-id $publicSubnetId2
echo "Delete all subnets from the vpc "$vpcId

#Delete the route
aws ec2 delete-route --route-table-id $routeTableId --destination-cidr-block 0.0.0.0/0
#aws ec2 delete-route --route-table-id $routeTableId --destination-cidr-block 10.0.0.0/16
echo "Deleting the route from route table"

#Delete the route table
aws ec2 delete-route-table --route-table-id $routeTableId
echo "Deleting the route table with id: "$routeTableId

#Detach Internet gateway and vpc
aws ec2 detach-internet-gateway --internet-gateway-id $gatewayId --vpc-id $vpcId
echo "Detaching the Internet gateway from vpc"

#Delete the Internet gateway
aws ec2 delete-internet-gateway --internet-gateway-id $gatewayId
echo "Deleting the Internet gateway: "$gatewayId

#Delete the vpc
aws ec2 delete-vpc --vpc-id $vpcId
echo "Deleting vpc "$vpcId

echo "Deletion Completed"