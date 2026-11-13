# Import blocks for resources created from the AWS console.
# Flow: add the import here -> ./generate.sh -> terraform apply -> remove the import.

# ==== IAM ====
# import {
#   to = aws_iam_role.medassitan_ec2_ecr_role
#   id = "medassitan-ec2-ecr-role"
# }

# import {
#   to = aws_iam_role_policy_attachment.medassitan_ec2_ecr_readonly
#   id = "medassitan-ec2-ecr-role/arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
# }

# import {
#   to = aws_iam_instance_profile.medassitan_ec2_ecr_role
#   id = "medassitan-ec2-ecr-role"
# }
