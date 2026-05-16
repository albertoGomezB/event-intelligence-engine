# Event Intelligence Engine - CDK Infrastructure

AWS CDK infrastructure for the Event Intelligence Engine. Manages CloudWatch Log Group for application logs.

## Prerequisites

- AWS CLI configured with credentials
- Node.js 18+
- AWS CDK CLI: `npm install -g aws-cdk`

## Setup & Deploy

```bash
# Install dependencies
npm install

# Synthesize CloudFormation template
npx cdk synth

# Deploy to AWS (first time: full bootstrap)
npx cdk bootstrap  # Only needed first time
npx cdk deploy

# View deployed resources
npx cdk diff      # Show changes before deploying
```

## Resources

- **CloudWatch Log Group**: `/event-intelligence-engine/application`
  - Retention: 2 weeks
  - Namespace for all application logs

## Configuring Java Application

Update your Spring Boot `application.properties` to send logs to CloudWatch (future step: AWS CloudWatch Appender).

## Cleanup

```bash
npx cdk destroy  # Remove all resources
```

