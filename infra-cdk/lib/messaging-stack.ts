import * as cdk from 'aws-cdk-lib';
import * as sqs from 'aws-cdk-lib/aws-sqs';
import { Construct } from 'constructs';
import { AppConfig, EnvironmentConfig } from './shared/config';

export interface MessagingStackProps extends cdk.StackProps {
  environment: 'dev' | 'staging' | 'prod';
}

export class MessagingStack extends cdk.Stack {
  public readonly eventQueue: sqs.Queue;
  public readonly eventDeadLetterQueue: sqs.Queue;

  constructor(scope: Construct, id: string, props: MessagingStackProps) {
    super(scope, id, props);

    const envConfig = EnvironmentConfig(props.environment);

    // Dead Letter Queue - simple, F2P
    this.eventDeadLetterQueue = new sqs.Queue(
      this,
      'EventDeadLetterQueue',
      {
        queueName: `${AppConfig.eventQueueDeadLetterName}-${props.environment}`,
        removalPolicy:
          envConfig.removalPolicy === 'DESTROY'
            ? cdk.RemovalPolicy.DESTROY
            : cdk.RemovalPolicy.RETAIN,
        retentionPeriod: cdk.Duration.seconds(
          envConfig.sqsMessageRetention
        ),
      }
    );

    cdk.Tags.of(this.eventDeadLetterQueue).add(
      'Environment',
      props.environment
    );
    cdk.Tags.of(this.eventDeadLetterQueue).add('QueueType', 'DLQ');

    // Main Event Queue
    this.eventQueue = new sqs.Queue(this, 'EventQueue', {
      queueName: `${AppConfig.eventQueueName}-${props.environment}`,
      removalPolicy:
        envConfig.removalPolicy === 'DESTROY'
          ? cdk.RemovalPolicy.DESTROY
          : cdk.RemovalPolicy.RETAIN,
      visibilityTimeout: cdk.Duration.seconds(
        envConfig.sqsVisibilityTimeout
      ),
      retentionPeriod: cdk.Duration.seconds(
        envConfig.sqsMessageRetention
      ),
      deadLetterQueue: {
        queue: this.eventDeadLetterQueue,
        maxReceiveCount: 3,
      },
    });

    cdk.Tags.of(this.eventQueue).add('Environment', props.environment);
    cdk.Tags.of(this.eventQueue).add('QueueType', 'Primary');

    // Outputs
    new cdk.CfnOutput(this, 'EventQueueUrl', {
      value: this.eventQueue.queueUrl,
      exportName: `${id}-EventQueueUrl`,
    });

    new cdk.CfnOutput(this, 'EventQueueArn', {
      value: this.eventQueue.queueArn,
      exportName: `${id}-EventQueueArn`,
    });

    new cdk.CfnOutput(this, 'EventDLQUrl', {
      value: this.eventDeadLetterQueue.queueUrl,
      exportName: `${id}-EventDLQUrl`,
    });
  }
}

