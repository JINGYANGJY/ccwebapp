ps aux | grep java | xargs kill -9

rm -rf /home/centos/spring.log
rm -rf /home/centos/csye6225.log

#!/bin/bash
source /etc/profile
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -c file:/home/centos/cloudwatch-config.json -s
nohup java -jar ~/java-0.0.1-SNAPSHOT.jar >~/spring.log 2>&1 &
