# Role assumed by the EC2 instance to pull images from ECR
resource "aws_iam_role" "medassitan_ec2_ecr_role" {
  name        = "medassitan-ec2-ecr-role"
  description = "Allows EC2 instances to call AWS services on your behalf."

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Action    = ["sts:AssumeRole"]
      Principal = { Service = ["ec2.amazonaws.com"] }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "medassitan_ec2_ecr_readonly" {
  role       = aws_iam_role.medassitan_ec2_ecr_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

# Instance profile: what gets attached to the EC2 instance (wraps the role)
resource "aws_iam_instance_profile" "medassitan_ec2_ecr_role" {
  name = "medassitan-ec2-ecr-role"
  role = aws_iam_role.medassitan_ec2_ecr_role.name
}
