#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

TMP=all_generated.tf
rm -f "$TMP"
terraform plan -generate-config-out="$TMP"

# AWS resource type -> file prefix
awk '
BEGIN {
  map["aws_db_instance"]="rds"
  map["aws_security_group"]="sg"
  map["aws_instance"]="ec2"
  map["aws_key_pair"]="key"
}
/^# __generated__ by Terraform from/ { header=$0; next }
/^resource "/ {
  type=$2; gsub(/"/,"",type)
  prefix=(type in map) ? map[type] : type
  out=prefix "_generated.tf"
  if (!(out in seen)) { seen[out]=1; if (system("test -f " out) != 0) print "# __generated__ by Terraform" > out }
  print "\n" header >> out
}
out != "" { print >> out }
/^}/ { out="" }
' "$TMP"

rm -f "$TMP"
ls -1 *_generated.tf
