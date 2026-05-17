#!/usr/bin/env node
import * as cdk from 'aws-cdk-lib/core';
import { ObservabilityStack } from '../lib/observability-stack';
import { DatabaseStack } from '../lib/database-stack';
import { MessagingStack } from '../lib/messaging-stack';

const app = new cdk.App();

// Get environment from context or default to 'dev'
const environment = (app.node.tryGetContext('env') || 'dev') as 'dev' | 'staging' | 'prod';

const stackProps: cdk.StackProps = {
  /* Customize env here if needed
  env: { account: process.env.CDK_DEFAULT_ACCOUNT, region: process.env.CDK_DEFAULT_REGION },
  */
};

// Create stacks in dependency order
const obsStack = new ObservabilityStack(app, `EventIntelligence-Observability-${environment}`, {
  ...stackProps,
  environment,
});

const dbStack = new DatabaseStack(app, `EventIntelligence-Database-${environment}`, {
  ...stackProps,
  environment,
});

const msgStack = new MessagingStack(app, `EventIntelligence-Messaging-${environment}`, {
  ...stackProps,
  environment,
});

// Optional: add dependencies if needed
// msgStack.addDependency(obsStack);

