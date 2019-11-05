resource "aws_vpc" "myvpc" {
  cidr_block = var.vpc_cidr_block
  enable_dns_hostnames = true
  enable_dns_support   = true
  enable_classiclink_dns_support = false
  assign_generated_ipv6_cidr_block = false
  instance_tenancy = "default"
  tags = {
    Name = var.vpc_name
  }
}

resource "aws_subnet" "sb1" {
  cidr_block = var.sb1_cidr_block
  vpc_id = aws_vpc.myvpc.id
  availability_zone = var.sb1_availability_zone
  map_public_ip_on_launch = true
  tags = {
    Name = var.sb1_name
  }
}

resource "aws_subnet" "sb2" {
  cidr_block = var.sb2_cidr_block
  vpc_id = aws_vpc.myvpc.id
  availability_zone = var.sb2_availability_zone
  map_public_ip_on_launch = true
  tags = {
    Name = var.sb2_name
  }
}

resource "aws_subnet" "sb3" {
  cidr_block = var.sb3_cidr_block
  vpc_id = aws_vpc.myvpc.id
  availability_zone = var.sb3_availability_zone
  map_public_ip_on_launch = true
  tags = {
    Name = var.sb3_name
  }
}

resource "aws_internet_gateway" "ig" {
  vpc_id = aws_vpc.myvpc.id
  tags = {
    Name = var.ig_name
  }
}

resource "aws_route_table" "rt" {
  vpc_id = aws_vpc.myvpc.id
  route {
    cidr_block = var.public_route_cidr_block
    gateway_id = aws_internet_gateway.ig.id
  }
  tags = {
    Name = var.rt_name
  }
}

resource "aws_route_table_association" "a1" {
  subnet_id      = aws_subnet.sb1.id
  route_table_id = aws_route_table.rt.id
}

resource "aws_route_table_association" "a2" {
  subnet_id      = aws_subnet.sb2.id
  route_table_id = aws_route_table.rt.id
}

resource "aws_route_table_association" "a3" {
  subnet_id      = aws_subnet.sb3.id
  route_table_id = aws_route_table.rt.id
}
