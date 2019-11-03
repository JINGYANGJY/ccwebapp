#!/bin/bash
cd /home/ubuntu/webapp/target
pwd
nohup java -jar ~/java-0.0.1-SNAPSHOT.jar >~/spring.log 2>&1 &