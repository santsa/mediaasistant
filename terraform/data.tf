// EC2 Instance Connect: IP ranges of the service in the region
data "aws_ec2_managed_prefix_list" "ec2_instance_connect" {
  name = "com.amazonaws.${var.AWS_REGION}.ec2-instance-connect"
}

# Account's default VPC, where the security groups and the RDS are created
data "aws_vpc" "default" {
  default = true
}
