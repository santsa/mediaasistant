// rds
output "rds_endpoint" {
  value = aws_db_instance.postgres.endpoint
}

output "rds_address" {
  value = aws_db_instance.postgres.address
}

output "rds_port" {
  value = aws_db_instance.postgres.port
}

output "rds_db_name" {
  value = aws_db_instance.postgres.db_name
}

output "rds_jdbc_url" {
  value = "jdbc:postgresql://${aws_db_instance.postgres.endpoint}/${aws_db_instance.postgres.db_name}"
}

// ec2
output "ec2_id" {
  value = aws_instance.medassistant_app.id
}

output "ec2_public_ip" {
  value = aws_eip.medassistant_app_eip.public_ip
}

output "ec2_public_dns" {
  value = aws_eip.medassistant_app_eip.public_dns
}

output "ec2_ssh" {
  value = "ssh -i medassistant-key.pem ec2-user@${aws_eip.medassistant_app_eip.public_ip}"
}
