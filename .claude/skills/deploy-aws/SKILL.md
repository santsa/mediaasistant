---
name: deploy-aws
description: Builds the production Docker image, pushes it to Amazon ECR and redeploys it on the EC2 instance with docker-compose-aws.yml, then checks the app started. Only run when the user explicitly invokes it.
disable-model-invocation: true
arguments: [tag]
argument-hint: [image-tag (default: latest)]
---

# Deploy to AWS — mediaassistant

Image tag: `$tag` (default `latest` if empty).

This skill has outward-facing effects (pushes an image, restarts production). Follow the steps in order and **stop on the first failure** — never continue to the next step after an error.

## 0. Preconditions — check, don't assume

Run and show the results:

```sh
git status --short && git rev-parse --abbrev-ref HEAD && git log -1 --oneline
aws sts get-caller-identity --query Account --output text
aws configure get region
(cd terraform && terraform output -raw ec2_public_ip && terraform output -raw ec2_ssh)
```

- Uncommitted changes or a branch other than the usual deploy branch → tell the user and ask whether to continue.
- Resolve `ACCOUNT`, `REGION`, `EC2_IP` and the SSH command from the output above. If `AWS_REGION` / `AWS_ACCOUNT_ID` env vars are set, they win. If anything can't be resolved, ask the user — never guess.
- Registry: `$ACCOUNT.dkr.ecr.$REGION.amazonaws.com`, repository `medassistant`.

**Show the user a summary (account, region, image URI with tag, EC2 host, commit) and get an explicit "yes" before step 2.**

## 1. Build and verify locally

```sh
docker build -t medassistant:aws .
```

Optionally (recommended when the change touched `aws`-profile beans, `schema-aws.sql` or configuration) offer a local check with `docker compose -f docker-compose.aws-test.yml up -d` (pgvector on :5434) before pushing.

## 2. Push to ECR

```sh
aws ecr get-login-password --region "$REGION" \
  | docker login --username AWS --password-stdin "$ACCOUNT.dkr.ecr.$REGION.amazonaws.com"
docker tag medassistant:aws "$ACCOUNT.dkr.ecr.$REGION.amazonaws.com/medassistant:$TAG"
docker push "$ACCOUNT.dkr.ecr.$REGION.amazonaws.com/medassistant:$TAG"
```

## 3. Redeploy on EC2

Use the SSH command from `terraform output -raw ec2_ssh`. On the instance (the compose file, `keycloak_realm/` and `.env` are already there — see README "Run on EC2"):

```sh
aws ecr get-login-password --region "$REGION" \
  | docker login --username AWS --password-stdin "$ACCOUNT.dkr.ecr.$REGION.amazonaws.com"
docker compose -f docker-compose-aws.yml pull medassistant
docker compose -f docker-compose-aws.yml up -d medassistant
```

- If `$TAG` is not `latest`, the instance's `.env` `MEDASSISTANT_IMAGE` must point to that tag. Show the user the current value and ask before editing it.
- If `docker-compose-aws.yml` or `keycloak_realm/` changed locally since the last deploy, tell the user and ask whether to `scp` them to the instance first.
- Never print or copy the contents of `.env` / `.env_aws` (they hold RDS and API secrets).

## 4. Verify

```sh
docker compose -f docker-compose-aws.yml ps
docker logs --tail 80 medassistant-app
```

Look for `Started MediaassistantApplication`; report errors verbatim if it crashes or restarts. Then from the local machine check the API answers on `http://$EC2_IP:8080` (a `401` without a token means the app is up and security works). Offer `/smoke-test` against the EC2 host for a full check.

## 5. Report

Image URI and digest pushed, commit deployed, container status, and anything skipped. If something failed, say exactly at which step and what was left in which state (e.g. "image pushed but container not restarted").

## Don't

- Don't run `terraform apply`, modify security groups, or touch RDS from this skill.
- Don't `docker compose down` or remove volumes on the instance (Keycloak DB lives in `keycloak-db-data`).
- Don't push an image built from uncommitted code without the user's explicit OK.
