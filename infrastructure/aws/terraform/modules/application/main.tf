data "aws_caller_identity" "current" { }

# s3 bucket encrypt key
resource "aws_kms_key" "mykey" {
  description             = "This key is used to encrypt bucket objects"
}

# s3 bucket for image
resource "aws_s3_bucket" "bucket" {
  bucket = "webapp.${var.domain}"
  force_destroy = true
  acl    = "private"

  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        kms_master_key_id = "${aws_kms_key.mykey.arn}"
        sse_algorithm     = "aws:kms"
      }
    }
  }

  lifecycle_rule {
    id      = "log"
    enabled = true

    prefix = "log/"

    tags = {
      "rule"      = "log"
      "autoclean" = "true"
    }

    transition {
      days          = 30
      storage_class = "STANDARD_IA" # or "ONEZONE_IA"
    }

    transition {
      days          = 60
      storage_class = "GLACIER"
    }

    expiration {
      days = 90
    }
  }

  lifecycle_rule {
    id      = "tmp"
    prefix  = "tmp/"
    enabled = true

    expiration {
      date = "2019-12-15"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "bucketAccess" {
  bucket = "${aws_s3_bucket.bucket.id}"

  block_public_acls   = true
  block_public_policy = true
  ignore_public_acls  = true
  restrict_public_buckets =true
}

# s3 bucket for code deploy
resource "aws_s3_bucket" "codedeploy" {
  bucket = "codedeploy.${var.domain}"
  force_destroy = true
  acl    = "private"

  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        kms_master_key_id = "${aws_kms_key.mykey.arn}"
        sse_algorithm     = "aws:kms"
      }
    }
  }

  lifecycle_rule {
    id      = "cleanup"
    enabled = true

    prefix = "cleanup/"

    tags = {
      "rule"      = "cleanup"
      "autoclean" = "true"
    }

    expiration {
      days = 60
    }

    noncurrent_version_expiration {
      days = 1
    }
  }
}

resource "aws_s3_bucket_public_access_block" "codedeployAccess" {
  bucket = "${aws_s3_bucket.codedeploy.id}"

  block_public_acls   = true
  block_public_policy = true
  ignore_public_acls  = true
  restrict_public_buckets =true
}


resource "aws_db_instance" "DB_Instance" {
  identifier           = "csye6225-fall2019"
  allocated_storage    = 20
  engine               = "mysql"
  engine_version       = "5.7"
  instance_class       = "db.t2.medium"
  multi_az             = false
  name                 = "csye6225"
  username             = "root"
  password             = "root12345"
  parameter_group_name = "default.mysql5.7" 
  publicly_accessible  = true
  db_subnet_group_name = "${aws_db_subnet_group.sbsubnets.name}"
  vpc_security_group_ids=["${aws_security_group.DBSecurity.id}"]
  skip_final_snapshot  = true
}

resource "aws_db_subnet_group" "sbsubnets" {
  name       = "dbsubnet"
  subnet_ids = ["${var.sb1_id}", "${var.sb2_id}", "${var.sb3_id}"]

  tags = {
    Name = "My DB subnet group"
  }
}

resource "aws_security_group" "DBSecurity" {
  name        = "DBSecurity"
  vpc_id      = var.vpc_id
}

resource "aws_security_group_rule" "DBSecurityRule" {
  type = "ingress"
  from_port   = 3306
  to_port     = 3306
  protocol    = "tcp"
  source_security_group_id = "${aws_security_group.applicationSP.id}"
  security_group_id = "${aws_security_group.DBSecurity.id}"
}

# DynamoDB Table
resource "aws_dynamodb_table" "csye6225" {
  name           = "csye6225"
  billing_mode   = "PROVISIONED"
  read_capacity  = 20
  write_capacity = 20
  hash_key       = "id"

  attribute {
    name = "id"
    type = "S"
  }

  ttl {
    attribute_name = "ttl"
    enabled        = true
  }


  tags = {
    Name       = "csye6225"
    Enironment = "${var.profile}"
  }
}

# key pair
resource "aws_key_pair" "deployer" {
  key_name   = "pb_key"
  public_key = "${file(var.public_key_path)}"
}

# Iam role
resource "aws_iam_role" "CodeDeployEC2ServiceRole" {
  name               = "CodeDeployEC2ServiceRole"
  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}

resource "aws_iam_role" "CodeDeployServiceRole" {
  name        = "CodeDeployServiceRole"
  description = "Allows EC2 instances to call AWS services such as auto calling on your behalf."

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "codedeploy.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}

# Iam instance profile for code deploy ec2
resource "aws_iam_instance_profile" "codedeployec2" {
  name = "CodeDeployEC2ServiceRoleProfile"
  role = "${aws_iam_role.CodeDeployEC2ServiceRole.name}"
}

# CodeDeploy Applcation
resource "aws_codedeploy_app" "codedeployapp" {
  compute_platform = "Server"
  name = "csye6225-webapp"
}

# CodeDeploy Deployment Group
resource "aws_codedeploy_deployment_group" "codedeploygroup" {
  app_name              = "${aws_codedeploy_app.codedeployapp.name}"
  deployment_group_name = "csye6225-webapp-deployment"
  service_role_arn      = "${aws_iam_role.CodeDeployServiceRole.arn}"

  ec2_tag_set {
    ec2_tag_filter {
      key   = "Name"
      type  = "KEY_AND_VALUE"
      value = "csye6225-ec2"
    }
  }

  auto_rollback_configuration {
    enabled = true
    events  = ["DEPLOYMENT_FAILURE"]
  }
}


# CodeDeploy-EC2-S3 policy
resource "aws_iam_policy" "CodeDeploy-EC2-S3" {
  name        = "CodeDeploy-EC2-S3"
  description = "allows EC2 instances to read data from S3 buckets"

  policy = <<EOF
{
  "Version": "2012-10-17",
    "Statement": [
        {
            "Action": [
                "s3:Get*",
                "s3:List*"
            ],
            "Effect": "Allow",
            "Resource": [
                 "arn:aws:s3:::${aws_s3_bucket.codedeploy.bucket}",
                 "arn:aws:s3:::${aws_s3_bucket.codedeploy.bucket}/*"
            ]
        }
    ]
}
EOF
}

resource "aws_iam_policy_attachment" "attachpolicy-ec2role" {
  name       = "attachpolicy-ec2role"
  roles      = ["${aws_iam_role.CodeDeployEC2ServiceRole.name}"]
  policy_arn = "${aws_iam_policy.CodeDeploy-EC2-S3.arn}"
}

resource "aws_iam_policy_attachment" "attachCloudWatchPolicy-ec2role" {
  name       = "attachCloudWatchPolicy-ec2role"
  roles      = ["${aws_iam_role.CodeDeployEC2ServiceRole.name}"]
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}

resource "aws_iam_policy_attachment" "attachpolicy-codedeployRole" {
  name       = "attachpolicy-codedeployRole"
  roles      = ["${aws_iam_role.CodeDeployServiceRole.name}"]
  policy_arn = var.awscodedeployrole
}


# CircleCI-Upload-To-S3
resource "aws_iam_policy" "CircleCI-Upload-To-S3" {
  name        = "CircleCI-Upload-To-S3"
  description = "allows CircleCI to upload artifacts from latest successful build to dedicated S3 bucket used by code deploy"

  policy = <<EOF
{
  "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:PutObject"
            ],
            "Resource": [
                 "arn:aws:s3:::${aws_s3_bucket.codedeploy.bucket}",
                 "arn:aws:s3:::${aws_s3_bucket.codedeploy.bucket}/*"
            ]
        }
    ]
}
EOF
}


resource "aws_iam_user_policy_attachment" "circleci-policy-1" {
  user       = var.circleciName
  policy_arn = "${aws_iam_policy.CircleCI-Upload-To-S3.arn}"
}

# CircleCI-Code-Deploy
resource "aws_iam_policy" "CircleCI-Code-Deploy" {
  name        = "CircleCI-Code-Deploy"
  description = "allows CircleCI to call CodeDeploy APIs to initiate application deployment on EC2 instances"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "codedeploy:RegisterApplicationRevision",
        "codedeploy:GetApplicationRevision"
      ],
      "Resource": [
        "arn:aws:codedeploy:${var.region}:${data.aws_caller_identity.current.account_id}:application:csye6225-webapp"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "codedeploy:CreateDeployment",
        "codedeploy:GetDeployment"
      ],
      "Resource": [
        "*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "codedeploy:GetDeploymentConfig"
      ],
      "Resource": [
        "arn:aws:codedeploy:${var.region}:${data.aws_caller_identity.current.account_id}:deploymentconfig:CodeDeployDefault.OneAtATime",
        "arn:aws:codedeploy:${var.region}:${data.aws_caller_identity.current.account_id}:deploymentconfig:CodeDeployDefault.HalfAtATime",
        "arn:aws:codedeploy:${var.region}:${data.aws_caller_identity.current.account_id}:deploymentconfig:CodeDeployDefault.AllAtOnce"
      ]
    }
  ]
}
EOF
}

resource "aws_iam_user_policy_attachment" "circleci-policy-2" {
  user       = var.circleciName
  policy_arn = "${aws_iam_policy.CircleCI-Code-Deploy.arn}"
}


# security group for EC2 instances
resource "aws_security_group" "loadBalancer" {
  vpc_id      = var.vpc_id

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "application-loadBalancer"
  }
}

resource "aws_security_group_rule" "applicationSecurityRule" {
  type = "ingress"
  from_port   = 8080
  to_port     = 8080
  protocol    = "tcp"
  source_security_group_id = "${aws_security_group.loadBalancer.id}"
  security_group_id = "${aws_security_group.applicationSP.id}"
}

resource "aws_lb" "applicationLoadBanlancer" {
  name               = "applicationLoadBanlancer"
  ip_address_type    = "ipv4"
  load_balancer_type = "application"
  security_groups    = ["${aws_security_group.loadBalancer.id}"]
  subnets            = ["${var.sb1_id}", "${var.sb2_id}", "${var.sb3_id}"]

  enable_deletion_protection = false

  tags = {
    Name = "csye6225-loadbanlancer"
    value = "loadbanlancer"
  }
}

resource "aws_lb_listener" "ALBListenerService" {
  load_balancer_arn = "${aws_lb.applicationLoadBanlancer.arn}"
  port              = "443"
  protocol          = "HTTPS"
  certificate_arn   = var.certificate_arn
  default_action {
    type             = "forward"
    target_group_arn = "${aws_lb_target_group.ALBtargetGroup.arn}"
  }
}

# security group for EC2 instances
resource "aws_security_group" "applicationSP" {
  name        = "applicationSP"
  vpc_id      = var.vpc_id
  egress {
    from_port = 0
    to_port = 0
    protocol = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "applicationSP"
  }
}


resource "aws_launch_configuration" "asg-lanch-config" {
  name = "asg-lanch-config"
  image_id      = "${var.ami_id}"
  instance_type = "t2.micro"
  associate_public_ip_address= true

  # Our Security group to allow HTTP and SSH access
  security_groups = ["${aws_security_group.applicationSP.id}"]

  #subnet_id = var.sb1_id
  key_name = "${aws_key_pair.deployer.id}"

  # Iam
  iam_instance_profile = "${aws_iam_instance_profile.codedeployec2.name}"

   root_block_device {
      delete_on_termination = true
      volume_type = "gp2"
      volume_size = 20
  }

  depends_on = [aws_db_instance.DB_Instance]

  user_data = <<-EOF
  #! /bin/bash
        echo export AWS_REGION=${var.AWS_REGION}>>/etc/profile
        echo export AWS_ACCESS_KEY_ID=${var.AWS_ACCESS_KEY_ID}>>/etc/profile
        echo export AWS_SECRET_ACCESS_KEY=${var.AWS_SECRET_ACCESS_KEY}>>/etc/profile
        echo export S3_IMAGE_BUCKET_NAME=${aws_s3_bucket.bucket.bucket}>>/etc/profile
        echo export DATABASE_HOSTNAME=${aws_db_instance.DB_Instance.endpoint}>>/etc/profile
        echo export DATABASE_USERNAME="root">>/etc/profile
        echo export DATABASE_PASSWORD="root12345">>/etc/profile
        echo export SERVER_PORT=8080>>/etc/profile
        echo export DOMAIN_NAME=${var.domain}>>/etc/profile
        echo export TOPIC_ARN=${aws_sns_topic.recipe_topic.arn}>>/etc/profile
  EOF
}


resource "aws_autoscaling_group" "autoscalinggroup" {
  vpc_zone_identifier       = ["${var.sb1_id}", "${var.sb2_id}", "${var.sb3_id}"]
  name                      = "autoscalinggroup"
  max_size                  = 10
  min_size                  = 3
  desired_capacity          = 3
  default_cooldown          = 60
  wait_for_capacity_timeout = 0
  launch_configuration      = "${aws_launch_configuration.asg-lanch-config.name}"
  target_group_arns         = ["${aws_lb_target_group.ALBtargetGroup.arn}"]

  tag {
    key                 = "Name"
    value               = "csye6225-ec2"
    propagate_at_launch = true
  }
}

resource "aws_lb_target_group" "ALBtargetGroup" {
  name        = "ALBtargetGroup"
  port        = 80
  protocol    = "HTTP"
  target_type = "instance"
  vpc_id      = var.vpc_id
  health_check {
    path = "/health"
    port = 80
    healthy_threshold = 3
    unhealthy_threshold = 5
    timeout = 5
    interval = 30
    protocol = "HTTPS"
  }
}


resource "aws_autoscaling_policy" "up" {
  name                   = "up"
  scaling_adjustment     = 1
  adjustment_type        = "ChangeInCapacity"
  cooldown               = 60
  autoscaling_group_name = "${aws_autoscaling_group.autoscalinggroup.name}"
}

resource "aws_autoscaling_policy" "down" {
  name                   = "down"
  scaling_adjustment     = -1
  adjustment_type        = "ChangeInCapacity"
  cooldown               = 60
  autoscaling_group_name = "${aws_autoscaling_group.autoscalinggroup.name}"
}


resource "aws_cloudwatch_metric_alarm" "CPUAlarmHigh" {
  alarm_name          = "CPUAlarmHigh"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = "300"
  statistic           = "Average"
  threshold           = "5"

  dimensions = {
    AutoScalingGroupName = "${aws_autoscaling_group.autoscalinggroup.name}"
  }

  alarm_description = "Scale-up if CPU > 90% for 10 minutes"
  alarm_actions     = ["${aws_autoscaling_policy.up.arn}"]
}

resource "aws_cloudwatch_metric_alarm" "CPUAlarmLow" {
  alarm_name          = "CPUAlarmLow"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = "300"
  statistic           = "Average"
  threshold           = "3"

  dimensions = {
    AutoScalingGroupName = "${aws_autoscaling_group.autoscalinggroup.name}"
  }

  alarm_description = "Scale-down if CPU < 70% for 10 minutes"
  alarm_actions     = ["${aws_autoscaling_policy.down.arn}"]
}

resource "aws_sns_topic" "recipe_topic" {
  name = "recipe_topic"
}

resource "aws_iam_role" "lambda_role" {
  name = "lambda_role"


  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Effect": "Allow"
    }
  ]
}
EOF
}

resource "aws_iam_policy_attachment" "attachpolicy-lambda" {
  name       = "attachpolicy-lambda"
  roles      = ["${aws_iam_role.lambda_role.name}"]
  policy_arn = "arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess"
}


resource "aws_iam_policy_attachment" "attachpolicy-lambda1" {
  name       = "attachpolicy-lambda"
  roles      = ["${aws_iam_role.lambda_role.name}"]
  policy_arn = "arn:aws:iam::aws:policy/AWSLambdaFullAccess"
}

resource "aws_iam_policy_attachment" "attachpolicy-lambda2" {
  name       = "attachpolicy-lambda"
  roles      = ["${aws_iam_role.lambda_role.name}"]
  policy_arn = "arn:aws:iam::aws:policy/AmazonSESFullAccess"
}

resource "aws_iam_policy_attachment" "attachpolicy-lambda3" {
  name       = "attachpolicy-lambda"
  roles      = ["${aws_iam_role.lambda_role.name}"]
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

data "archive_file" "dummy" {
  type = "zip"
  output_path = "lambda_function.zip"
  source {
    content = "hello"
    filename = "dummy.txt"
  }
}

resource "aws_lambda_function" "lambda_function" {
  filename      = "${data.archive_file.dummy.output_path}"
  function_name = "lambda_function"
  role          = "${aws_iam_role.lambda_role.arn}"
  timeout       = 120
  memory_size   = 600
  handler       = "LogEvent::handleRequest"
  runtime = "java8"

  environment {
    variables = {
      from = "demo@${var.domain}"
    }
  }
}

resource "aws_sns_topic_subscription" "subscription" {
  topic_arn = "${aws_sns_topic.recipe_topic.arn}"
  protocol  = "lambda"
  endpoint  = "${aws_lambda_function.lambda_function.arn}"
}

resource "aws_lambda_permission" "lambda_permission" {
  statement_id  = "AllowMyDemoAPIInvoke"
  action        = "lambda:InvokeFunction"
  function_name = "${aws_lambda_function.lambda_function.function_name}"
  principal     = "sns.amazonaws.com"
  source_arn = "${aws_lambda_function.lambda_function.arn}"
}