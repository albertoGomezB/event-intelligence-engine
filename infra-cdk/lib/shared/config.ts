/**
 * Shared configuration constants for Event Intelligence Engine infrastructure
 */

export const AppConfig = {
  // Application identity
  appName: 'event-intelligence-engine',
  appNameFormatted: 'EventIntelligenceEngine',

  // CloudWatch
  logGroupName: '/event-intelligence-engine/application',
  metricNamespace: 'EventIntelligenceEngine',

  // SNS
  alarmTopicName: 'event-intelligence-alarms',

  // SQS
  eventQueueName: 'event-intelligence-events',
  eventQueueDeadLetterName: 'event-intelligence-events-dlq',

  // DynamoDB
  eventsTableName: 'event-intelligence-events',
  eventsTablePartitionKey: 'eventId',
  eventsTableSortKey: 'receivedAt',

  // Tags
  tags: {
    Application: 'Event Intelligence Engine',
    Environment: 'shared', // will be overridden in stacks
    ManagedBy: 'CDK',
  },
};

export const EnvironmentConfig = (env: 'dev' | 'staging' | 'prod') => {
  const baseConfig = {
    dev: {
      logRetention: 7, // days
      removalPolicy: 'DESTROY',
      dynamoDBBilling: 'PAY_PER_REQUEST',
      sqsVisibilityTimeout: 60,
      sqsMessageRetention: 86400, // 1 day
    },
    staging: {
      logRetention: 30,
      removalPolicy: 'RETAIN',
      dynamoDBBilling: 'PROVISIONED',
      dynamoDBReadCapacity: 5,
      dynamoDBWriteCapacity: 5,
      sqsVisibilityTimeout: 300,
      sqsMessageRetention: 345600, // 4 days
    },
    prod: {
      logRetention: 90,
      removalPolicy: 'RETAIN',
      dynamoDBBilling: 'PROVISIONED',
      dynamoDBReadCapacity: 25,
      dynamoDBWriteCapacity: 25,
      sqsVisibilityTimeout: 600,
      sqsMessageRetention: 1209600, // 14 days
    },
  };

  return baseConfig[env];
};

