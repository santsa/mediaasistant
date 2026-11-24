---
name: terraform-import
description: Imports an AWS resource created in the console into the terraform/ project using its flow — import block in import.tf, ./generate.sh to generate config, review, plan, apply after approval, then remove the import block. Only run when the user explicitly invokes it.
disable-model-invocation: true
arguments: [address, id]
argument-hint: <resource_type.name> <aws-id>
---

# Terraform import — mediaassistant

Resource address: `$address` · AWS id: `$id` (full arguments: `$ARGUMENTS`).

Work inside `terraform/`. Follow the project flow documented in `terraform/import.tf`: **add import → `./generate.sh` → `terraform apply` → remove import**. If the address or id is missing, ask; for resources whose import id format is non-obvious (e.g. `aws_iam_role_policy_attachment` is `role/policy-arn`, `aws_security_group_rule` has a composite id), check the provider docs and tell the user the expected format.

## 0. Preconditions

```sh
cd terraform
terraform version
aws sts get-caller-identity --query Account --output text
terraform state list
```

- If `$address` is already in `terraform state list`, stop: it's already managed.
- `terraform.tfvars` must exist (copy from `terraform.tfvars.example` otherwise — ask the user for values, never invent them). Never print its contents.

## 1. Add the import block

Append to `import.tf` (keep the existing commented history and section headers like `# ==== IAM ====`):

```hcl
import {
  to = <address>
  id = "<id>"
}
```

## 2. Generate config

```sh
./generate.sh
```

It runs `terraform plan -generate-config-out` and splits the output into `<prefix>_generated.tf` per resource type (`aws_db_instance`→`rds`, `aws_security_group`→`sg`, `aws_instance`→`ec2`, `aws_key_pair`→`key`; other types use the type name as prefix). If the new type deserves a short prefix, propose adding it to the `map` in `generate.sh`.

- If the generated resource for this type already lives in a hand-written file (e.g. `iam.tf`, `eip.tf`), move the generated block there instead of leaving a duplicate file, and tell the user.

## 3. Clean up the generated code

Review the new block:
- Remove computed/read-only attributes that cause errors or perpetual diffs (`arn`, `id`, `owner_id`, null-valued attributes, etc.).
- Replace hard-coded values that already exist as variables or other resources with references (`var.*`, `aws_security_group.x.id`, `data.*`).
- Never write secrets (passwords, keys) inline — use variables marked `sensitive = true`.

## 4. Plan — and stop for review

```sh
terraform plan
```

Show the user the summary line and the relevant part. The expected result is **`1 to import, 0 to add, 0 to change, 0 to destroy`** (or only harmless in-place changes the user agrees to).

- Any `destroy` or `must be replaced` → **stop**, explain why and fix the config; never apply it.
- Wait for explicit approval before step 5.

## 5. Apply

```sh
terraform apply
```

Run it interactively only after approval (if the session can't answer the prompt, use `terraform apply` on a saved plan: `terraform plan -out=import.tfplan` then `terraform apply import.tfplan`, and delete the plan file afterwards).

## 6. Remove the import block and confirm

- Delete (or comment out, matching the file's existing style) the block added in step 1.
- `terraform plan` again → must report **No changes**.
- If the resource is relevant for other tooling, consider an `output` in `output.tf` and tell the user.

## 7. Report

What was imported, files created/changed, final plan result. Mention that the changes are not committed.

## Don't

- Don't run `terraform destroy`, `terraform state rm`, or `-target` applies.
- Don't apply a plan that changes or replaces existing resources without the user explicitly approving those changes.
- Don't commit `terraform.tfvars`, `*.tfstate` or plan files.
