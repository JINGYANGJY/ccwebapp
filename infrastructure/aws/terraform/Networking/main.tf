provider "aws" {
  profile = var.profile
  region  = var.vpc_region
}

variable "profile" {
  type = "string"
  default = "dev"
}

variable "vpc_region" {
  type = "string"
  default = "us-east-1"
}

variable "cidr_block" {
  type = "string"
  default = "10.0.0.0/16"
}

variable "subnet1_cidr_block" {
  type = "string"
  default = "10.0.1.0/24"
}

variable "subnet2_cidr_block" {
  type = "string"
  default = "10.0.16.0/24"
}

variable "subnet3_cidr_block" {
  type = "string"
  default = "10.0.32.0/24"
}

variable "public_route_cidr_block" {
  type = "string"
  default = "0.0.0.0/0"
}

variable "subnet1_availability_zone" {
  type = "string"
  default = "us-east-1a"
}

variable "subnet2_availability_zone" {
  type = "string"
  default = "us-east-1b"
}

variable "subnet3_availability_zone" {
  type = "string"
  default = "us-east-1c"
}

variable "vpc_name" {
  type = "string"
  default = "myvpc"
}

variable "subnet1_name" {
  type = "string"
  default = "subnet1"
}

variable "subnet2_name" {
  type = "string"
  default = "subnet2"
}

variable "subnet3_name" {
  type = "string"
  default = "subnet3"
}

variable "ig_name" {
  type = "string"
  default = "internet_gateway"
}

variable "rt_name" {
  type = "string"
  default = "route_table"
}

resource "aws_vpc" "myvpc" {
  cidr_block = var.cidr_block
  enable_dns_hostnames = true
  enable_dns_support   = true
  assign_generated_ipv6_cidr_block = false
  instance_tenancy = "default"
  tags = {
    Name = var.vpc_name
  }
}

resource "aws_subnet" "subnet1" {
  cidr_block = var.subnet1_cidr_block
  vpc_id = aws_vpc.myvpc.id
  availability_zone = var.subnet1_availability_zone
  map_public_ip_on_launch = true
  tags = {
    Name = var.subnet1_name
  }
}

resource "aws_subnet" "subnet2" {
  cidr_block = var.subnet2_cidr_block
  vpc_id = aws_vpc.myvpc.id
  availability_zone = var.subnet2_availability_zone
  map_public_ip_on_launch = true
  tags = {
    Name = var.subnet2_name
  }
}

resource "aws_subnet" "subnet3" {
  cidr_block = var.subnet3_cidr_block
  vpc_id = aws_vpc.myvpc.id
  availability_zone = var.subnet3_availability_zone
  map_public_ip_on_launch = true
  tags = {
    Name = var.subnet3_name
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
  subnet_id      = aws_subnet.subnet1.id
  route_table_id = aws_route_table.rt.id
}

resource "aws_route_table_association" "a2" {
  subnet_id      = aws_subnet.subnet2.id
  route_table_id = aws_route_table.rt.id
}

resource "aws_route_table_association" "a3" {
  subnet_id      = aws_subnet.subnet3.id
  route_table_id = aws_route_table.rt.id
}