#!/bin/bash
source /etc/profile
nohup java -jar ~/java-0.0.1-SNAPSHOT.jar >~/spring.log 2>&1 &
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
    -a fetch-config \
    -m ec2 \
    -c file:cloudwatch-config.json \
    -s
