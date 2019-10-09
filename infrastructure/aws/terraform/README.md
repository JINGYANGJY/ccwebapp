# CSYE 6225 - Fall 2019

## Team Information

 Name | NEU ID | Email Address 
 -----|--------|--------------
 Shujie Fan   | 001838430 | fan.shuj@northeastern.edu 
 Yiqiang Wang | 001403835 | wang.yiqi@northeastern.edu 
 Jing Yang    | 001886075 | yang.jing4@northeastern.edu 

## Technology Stack
* Terraform
* AWS

## Build and Deploy Instructions
* Open the Github link: https://github.com/JINGYANGJY/ccwebapp
* Open the terminal, go inside ccwebapp/infrastructure/aws/terraform folder
* Initialize the working directory for terraform
```console 
terraform init
```
* Provision with this command (provide all the variable values)
```console
terraform apply -var="cidr_block=10.0.0.0/16" -var="..."
```
or create a xxx.tfvars file and put all the variable values inside, and then run with
```console
terraform apply -var-file="xxx.tfvars"
```
* From your terminal, destroy the resources
```console
terraform destroy
```
