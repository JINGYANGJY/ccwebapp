provider "aws" {
  profile = var.profile
  region = var.region
}


//circleci module
module "policies" {
    source = "./modules/policies"
    circleciName = var.circleciName
    region = var.region
    profile = var.profile
}

//network module
module "network" {
    source = "./modules/network"
    profile = var.profile
    region =var.region
    vpc_cidr_block= var.vpc_cidr_block
    sb1_cidr_block= var.sb1_cidr_block
    sb2_cidr_block= var.sb2_cidr_block
    sb3_cidr_block= var.sb3_cidr_block
    public_route_cidr_block= var.public_route_cidr_block
    sb1_availability_zone= var.sb1_availability_zone
    sb2_availability_zone= var.sb2_availability_zone
    sb3_availability_zone= var.sb3_availability_zone
    vpc_name= var.vpc_name
    sb1_name= var.sb1_name
    sb2_name= var.sb2_name
    sb3_name= var.sb3_name
    ig_name= var.ig_name
    rt_name= var.rt_name

}

//application module
module "application" {
    source = "./modules/application"
    domain = var.domain
    ami_id = var.ami_id
    public_key_path = var.public_key_path
    profile = var.profile
    region = var.region
    vpc_id = module.network.vpc_id
    sb1_id = module.network.sb1_id
    sb2_id = module.network.sb2_id
    sb3_id = module.network.sb3_id
    sb1_availability_zone= var.sb1_availability_zone
    sb2_availability_zone= var.sb2_availability_zone
    sb3_availability_zone= var.sb3_availability_zone
    awscodedeployrole=var.awscodedeployrole
    AWS_ACCESS_KEY_ID=var.AWS_ACCESS_KEY_ID
    certificate_arn=var.certificate_arn
    AWS_REGION=var.AWS_REGION
    AWS_SECRET_ACCESS_KEY=var.AWS_SECRET_ACCESS_KEY
    circleciName = var.circleciName
    zone_id = var.zone_id
}
