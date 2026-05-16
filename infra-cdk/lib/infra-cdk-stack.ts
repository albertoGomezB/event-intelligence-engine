import * as cdk from 'aws-cdk-lib/core';
import * as logs from 'aws-cdk-lib/aws-logs';
import { Construct } from 'constructs';

export class InfraCdkStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // CloudWatch Log Group for Event Intelligence Engine
    new logs.LogGroup(this, 'EventIntelligenceEngineLogGroup', {
      logGroupName: '/event-intelligence-engine/application',
      retention: logs.RetentionDays.TWO_WEEKS,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
    });
  }
}
