import * as cdk from 'aws-cdk-lib';
import * as dynamodb from 'aws-cdk-lib/aws-dynamodb';
import { Construct } from 'constructs';
import { AppConfig, EnvironmentConfig } from './shared/config';

export interface DatabaseStackProps extends cdk.StackProps {
  environment: 'dev' | 'staging' | 'prod';
}

export class DatabaseStack extends cdk.Stack {
  public readonly eventsTable: dynamodb.Table;

  constructor(scope: Construct, id: string, props: DatabaseStackProps) {
    super(scope, id, props);

    const envConfig = EnvironmentConfig(props.environment);

    // Simple F2P DynamoDB: PAY_PER_REQUEST (no capacidad mínima)
    this.eventsTable = new dynamodb.Table(this, 'EventsTable', {
      tableName: AppConfig.eventsTableName,
      partitionKey: {
        name: AppConfig.eventsTablePartitionKey,
        type: dynamodb.AttributeType.STRING,
      },
      sortKey: {
        name: AppConfig.eventsTableSortKey,
        type: dynamodb.AttributeType.STRING,
      },
      billingMode: dynamodb.BillingMode.PAY_PER_REQUEST,
      removalPolicy:
        envConfig.removalPolicy === 'DESTROY'
          ? cdk.RemovalPolicy.DESTROY
          : cdk.RemovalPolicy.RETAIN,
      encryption: dynamodb.TableEncryption.AWS_MANAGED,
      pointInTimeRecovery: props.environment === 'prod',
    });

    // Add tags
    cdk.Tags.of(this.eventsTable).add('Environment', props.environment);
    Object.entries(AppConfig.tags).forEach(([key, value]) => {
      cdk.Tags.of(this.eventsTable).add(key, value);
    });

    // GSI for correlation ID queries
    this.eventsTable.addGlobalSecondaryIndex({
      indexName: 'CorrelationIdIndex',
      partitionKey: {
        name: 'correlationId',
        type: dynamodb.AttributeType.STRING,
      },
    });

    // Output
    new cdk.CfnOutput(this, 'EventsTableName', {
      value: this.eventsTable.tableName,
      exportName: `${id}-EventsTableName`,
    });

    new cdk.CfnOutput(this, 'EventsTableArn', {
      value: this.eventsTable.tableArn,
      exportName: `${id}-EventsTableArn`,
    });
  }
}

