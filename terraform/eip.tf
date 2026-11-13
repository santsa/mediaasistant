# Elastic IP to ec2 instance
resource "aws_eip" "medassistant_app_eip" {
  vpc = true

  tags = {
    Name = "medassistant-app-eip"
  }
}

# Association
resource "aws_eip_association" "medassistant_app_eip_assoc" {
  instance_id   = aws_instance.medassistant_app.id
  allocation_id = aws_eip.medassistant_app_eip.id
}
