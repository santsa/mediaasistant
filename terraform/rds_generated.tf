# __generated__ by Terraform

# __generated__ by Terraform from "medassistant-prod"
resource "aws_db_instance" "postgres" {
  allocated_storage                     = 20
  allow_major_version_upgrade           = null
  apply_immediately                     = null
  auto_minor_version_upgrade            = false
  availability_zone                     = "eu-south-2a"
  backup_retention_period               = 0
  backup_window                         = "05:08-05:38"
  ca_cert_identifier                    = var.RDS_CA_CERT_IDENTIFIER
  copy_tags_to_snapshot                 = true
  custom_iam_instance_profile           = null
  customer_owned_ip_enabled             = false
  db_name                               = var.RDS_DB_NAME
  db_subnet_group_name                  = var.RDS_DB_SUBNET_GROUP_NAME
  delete_automated_backups              = true
  deletion_protection                   = false
  domain                                = null
  domain_iam_role_name                  = null
  enabled_cloudwatch_logs_exports       = []
  engine                                = "postgres"
  engine_version                        = "17.8"
  final_snapshot_identifier             = null
  iam_database_authentication_enabled   = false
  identifier                            = var.RDS_DB_IDENTIFIER
  instance_class                        = "db.t4g.micro"
  kms_key_id                            = var.RDS_KMS_KEY_ID
  license_model                         = "postgresql-license"
  maintenance_window                    = "tue:03:06-tue:03:36"
  manage_master_user_password           = null
  max_allocated_storage                 = 0
  monitoring_interval                   = 0
  multi_az                              = false
  network_type                          = "IPV4"
  option_group_name                     = "default:postgres-17"
  parameter_group_name                  = "default.postgres17"
  password                              = var.DB_PASSWORD
  performance_insights_enabled          = false
  performance_insights_retention_period = 0
  port                                  = 5432
  publicly_accessible                   = true
  replicate_source_db                   = null
  skip_final_snapshot                   = true
  storage_encrypted                     = true
  storage_type                          = "gp3"
  tags                                  = {}
  tags_all                              = {}
  username                              = "postgres"
  vpc_security_group_ids                = [aws_security_group.medassitant_rds_sg.id]

  # the password is only used when creating the DB; it does not change the current DB password
  lifecycle {
    ignore_changes = [password]
  }
}
