variable "AWS_REGION" {
  type = string
}

variable "AWS_ACCESS_KEY_ID" {
  type = string
}

variable "AWS_SECRET_ACCESS_KEY" {
  type = string
}

variable "RDS_DB_IDENTIFIER" {
  type        = string
}

variable "RDS_SG_ID" {
  type = string
}

variable "DB_PASSWORD" {
  type      = string
  sensitive = true
}

variable "EC2_AMI" {
  type = string
  sensitive = true
}

variable "SUBNET_ID" {
   type = string
   sensitive = true
}

variable "RDS_CA_CERT_IDENTIFIER" {
  type    = string
  default = "rds-ca-rsa2048-g1"
}

variable "RDS_DB_NAME" {
  type    = string
  default = "medassistant_prod"
}

variable "RDS_DB_SUBNET_GROUP_NAME" {
  type = string
}

variable "RDS_KMS_KEY_ID" {
  type      = string
  sensitive = true
}

variable "RDS_SG_NAME" {
  type    = string
  default = "medassitant-rds-sg"
}

variable "EC2_SG_NAME" {
  type    = string
  default = "medassistant-ec2-sg"
}

# Public (internet) access to the app, Keycloak and SG egress
variable "ANYWHERE_CIDR" {
  type    = string
  default = "0.0.0.0/0"
}

# Your public IP for SSH to the EC2 instance and direct PostgreSQL access (format x.x.x.x/32)
variable "MY_IP_CIDR" {
  type = string
}
