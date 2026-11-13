# __generated__ by Terraform

# __generated__ by Terraform from "sg-06091ff4352085861"
resource "aws_instance" "medassistant_app" {
  ami                                  = var.EC2_AMI
  associate_public_ip_address          = true
  availability_zone                    = "eu-south-2a"
  disable_api_stop                     = false
  disable_api_termination              = false
  ebs_optimized                        = true
  get_password_data                    = false
  hibernation                          = false
  instance_initiated_shutdown_behavior = "stop"
  instance_type                        = "t3.small"
  iam_instance_profile                 = aws_iam_instance_profile.medassitan_ec2_ecr_role.name
  ipv6_address_count                   = 0
  key_name                             = aws_key_pair.medassistant_key.key_name
  monitoring                           = false
  placement_partition_number           = 0
  private_ip                           = "172.31.44.119"
  secondary_private_ips                = []
  source_dest_check                    = true
  subnet_id                            = var.SUBNET_ID
  tags = {
    Name = "medassistant-app"
  }
  tags_all = {
    Name = "medassistant-app"
  }
  tenancy                     = "default"
  user_data                   = file("${path.module}/user_data.sh")
  user_data_replace_on_change = false
  volume_tags                 = null
  vpc_security_group_ids      = [aws_security_group.medassistant_ec2_sg.id]
  capacity_reservation_specification {
    capacity_reservation_preference = "open"
  }
  cpu_options {
    core_count       = 1
    threads_per_core = 2
  }
  credit_specification {
    cpu_credits = "unlimited"
  }
  enclave_options {
    enabled = false
  }
  maintenance_options {
    auto_recovery = "default"
  }
  metadata_options {
    http_endpoint               = "enabled"
    http_put_response_hop_limit = 2
    http_tokens                 = "required"
    instance_metadata_tags      = "disabled"
  }
  private_dns_name_options {
    enable_resource_name_dns_a_record    = true
    enable_resource_name_dns_aaaa_record = false
    hostname_type                        = "ip-name"
  }
  root_block_device {
    delete_on_termination = true
    encrypted             = false
    iops                  = 3000
    tags                  = {}
    throughput            = 125
    volume_size           = 30
    volume_type           = "gp3"
  }
}
