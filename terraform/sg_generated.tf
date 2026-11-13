resource "aws_security_group" "medassitant_rds_sg" {
  description = "Created by RDS management console"
  egress = [{
    cidr_blocks      = [var.ANYWHERE_CIDR]
    description      = ""
    from_port        = 0
    ipv6_cidr_blocks = []
    prefix_list_ids  = []
    protocol         = "-1"
    security_groups  = []
    self             = false
    to_port          = 0
  }]
  ingress = [{
    cidr_blocks      = [var.MY_IP_CIDR]
    description      = ""
    from_port        = 5432
    ipv6_cidr_blocks = []
    prefix_list_ids  = []
    protocol         = "tcp"
    security_groups  = []
    self             = false
    to_port          = 5432
    }, {
    cidr_blocks      = []
    description      = "PostgreSQL desde la EC2 medassistant-app"
    from_port        = 5432
    ipv6_cidr_blocks = []
    prefix_list_ids  = []
    protocol         = "tcp"
    security_groups  = [aws_security_group.medassistant_ec2_sg.id]
    self             = false
    to_port          = 5432
  }]
  name                   = var.RDS_SG_NAME
  revoke_rules_on_delete = null
  tags                   = {}
  tags_all               = {}
  vpc_id                 = data.aws_vpc.default.id
}

resource "aws_security_group" "medassistant_ec2_sg" {
  description = "Security group for MedAssitant EC2"
  egress = [{
    cidr_blocks      = [var.ANYWHERE_CIDR]
    description      = ""
    from_port        = 0
    ipv6_cidr_blocks = []
    prefix_list_ids  = []
    protocol         = "-1"
    security_groups  = []
    self             = false
    to_port          = 0
  }]
  ingress = [{
    cidr_blocks      = [var.ANYWHERE_CIDR]
    description      = "MedAssistant HTTP"
    from_port        = 8080
    ipv6_cidr_blocks = []
    prefix_list_ids  = []
    protocol         = "tcp"
    security_groups  = []
    self             = false
    to_port          = 8080
    }, {
    cidr_blocks      = [var.ANYWHERE_CIDR]
    description      = "keyCloak HTTP"
    from_port        = 8090
    ipv6_cidr_blocks = []
    prefix_list_ids  = []
    protocol         = "tcp"
    security_groups  = []
    self             = false
    to_port          = 8090
    }, {
    cidr_blocks      = [var.MY_IP_CIDR]
    description      = "MedAssistant SSH from my IP"
    from_port        = 22
    ipv6_cidr_blocks = []
    prefix_list_ids  = []
    protocol         = "tcp"
    security_groups  = []
    self             = false
    to_port          = 22
    }, {
    cidr_blocks      = []
    description      = "SSH desde EC2 Instance Connect"
    from_port        = 22
    ipv6_cidr_blocks = []
    prefix_list_ids  = [data.aws_ec2_managed_prefix_list.ec2_instance_connect.id]
    protocol         = "tcp"
    security_groups  = []
    self             = false
    to_port          = 22
  }]
  name                   = var.EC2_SG_NAME
  revoke_rules_on_delete = null
  tags                   = {}
  tags_all               = {}
  vpc_id                 = data.aws_vpc.default.id
}
