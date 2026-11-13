# __generated__ by Terraform (public_key obtained with aws ec2 describe-key-pairs)
resource "aws_key_pair" "medassistant_key" {
  key_name   = "medassistant-key"
  public_key = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDdPcbeUHI5qDvH40Qsr74GTaP3zXL2uuxB6GIle90JeNI6h066l3QjMXh4OuFHhbv+KrHHnWwo65/dlP/Xt250YmyoYa6t56/2wudrLMEzwflKlF7yvjP9EHomcpTaHWsiTGxsvhvksMe1v4h9Zh5DVwuaWZJu7LhzYF3DJBD8TIs2GEU/J8qENS0wP07Dd1agn5qjjxxVDiaJWEJKzhfzCww+jLIs8kgkhZcTIC0mN+8HsHHNAmbSSzrBa7zz0Y/b/cPgzFBAAMomeOFFBOJDUvDj5sX8cNDYDNL6ekSgD9BgSLwcEXLTlu2cuo/NoqKK25vomUqR8befz6ouIjVJ medassistant-key"
  tags       = {}

  # the import does not bring public_key; without this Terraform would force replacing the key
  lifecycle {
    ignore_changes = [public_key]
  }
}
