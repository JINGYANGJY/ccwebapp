
data "aws_caller_identity" "current" { }

# circleci-ec2-ami
resource "aws_iam_policy" "circleci-ec2-ami" {
  name        = "circleci-ec2-ami"
  description = "allows CircleCI to upload artifacts from latest successful build to dedicated S3 bucket used by code deploy"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [{
      "Effect": "Allow",
      "Action" : [
        "ec2:AttachVolume",
        "ec2:AuthorizeSecurityGroupIngress",
        "ec2:CopyImage",
        "ec2:CreateImage",
        "ec2:CreateKeypair",
        "ec2:CreateSecurityGroup",
        "ec2:CreateSnapshot",
        "ec2:CreateTags",
        "ec2:CreateVolume",
        "ec2:DeleteKeyPair",
        "ec2:DeleteSecurityGroup",
        "ec2:DeleteSnapshot",
        "ec2:DeleteVolume",
        "ec2:DeregisterImage",
        "ec2:DescribeImageAttribute",
        "ec2:DescribeImages",
        "ec2:DescribeInstances",
        "ec2:DescribeInstanceStatus",
        "ec2:DescribeRegions",
        "ec2:DescribeSecurityGroups",
        "ec2:DescribeSnapshots",
        "ec2:DescribeSubnets",
        "ec2:DescribeTags",
        "ec2:DescribeVolumes",
        "ec2:DetachVolume",
        "ec2:GetPasswordData",
        "ec2:ModifyImageAttribute",
        "ec2:ModifyInstanceAttribute",
        "ec2:ModifySnapshotAttribute",
        "ec2:RegisterImage",
        "ec2:RunInstances",
        "ec2:StopInstances",
        "ec2:TerminateInstances"
      ],
      "Resource" : "*"
  }]
}
EOF
}


resource "aws_iam_user_policy_attachment" "circleci-policy-3" {
  user       = var.circleciName
  policy_arn = "${aws_iam_policy.circleci-ec2-ami.arn}"
}

resource "aws_iam_user_policy_attachment" "attachpolicy-lambda1" {
  user       = var.circleciName
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSCodeDeployRoleForLambda"
}

resource "aws_iam_user_policy_attachment" "attachpolicy-lambda2" {
  user       = var.circleciName
  policy_arn = "arn:aws:iam::aws:policy/AWSLambdaFullAccess"
}
