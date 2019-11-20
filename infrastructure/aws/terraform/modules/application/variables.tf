variable "vpc_id" {

}

variable "sb1_id" {

}

variable "sb2_id" {

}

variable "sb3_id" {

}

variable "region" {

}

variable "domain" { 
  description = "the domain name"
}

variable "ami_id" {
  description = "the id of ami you want to launch instances"
}

variable "public_key_path" {
  description = "the path of your public access key file"
}

variable "profile" {
}

variable "awscodedeployrole"{
}

variable "AWS_ACCESS_KEY_ID"{
}

variable "AWS_SECRET_ACCESS_KEY"{
}

variable "AWS_REGION"{
}

variable "circleciName"{
}

variable "sb1_availability_zone" {}

variable "sb2_availability_zone" {}

variable "sb3_availability_zone" {}

variable "certificate_arn" {}

variable "zone_id" {}
