import * as cdk from 'aws-cdk-lib';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as subscriptions from 'aws-cdk-lib/aws-sns-subscriptions';
import * as cloudwatch_actions from 'aws-cdk-lib/aws-cloudwatch-actions';
import { Construct } from 'constructs';

export class InfraCdkStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const appLogGroup = new logs.LogGroup(this, 'EventIntelligenceEngineLogGroup', {
      logGroupName: '/event-intelligence-engine/application',
      retention: logs.RetentionDays.ONE_MONTH, // prod: THREE_MONTHS or more depending on policy
      removalPolicy: cdk.RemovalPolicy.RETAIN, // prod : RETAIN
    });

    // Alarm Topic SNS
    const alarmsTopic = new sns.Topic(this, 'EventIntelligenceAlarmsTopic', {
      topicName: 'event-intelligence-alarms',
    });

    // Read optional alarm email from CDK context (use `cdk deploy -c alarmEmail=you@domain`)
    const alarmEmail = this.node.tryGetContext('alarmEmail');
    if (alarmEmail) {
      alarmsTopic.addSubscription(new subscriptions.EmailSubscription(alarmEmail));
    }

    // CloudWatch Metrics Filters
    const temporaryFailureFilter = new logs.MetricFilter(this, 'EventTemporaryFailureMetricFilter', {
      logGroup: appLogGroup,
      metricNamespace: 'EventIntelligenceEngine',
      metricName: 'EventsTemporaryFailure',
      filterPattern: logs.FilterPattern.literal('event_temporary_failure'),
      metricValue: '1',
      defaultValue: 0,
    });

    const reviewFilter = new logs.MetricFilter(this, 'EventReviewRequiredMetricFilter', {
      logGroup: appLogGroup,
      metricNamespace: 'EventIntelligenceEngine',
      metricName: 'EventsReviewRequired',
      filterPattern: logs.FilterPattern.literal('event_review_required'),
      metricValue: '1',
      defaultValue: 0,
    });

    const permanentFailureFilter = new logs.MetricFilter(this, 'EventPermanentFailureMetricFilter', {
      logGroup: appLogGroup,
      metricNamespace: 'EventIntelligenceEngine',
      metricName: 'EventsPermanentFailure',
      filterPattern: logs.FilterPattern.literal('event_permanent_failure'),
      metricValue: '1',
      defaultValue: 0,
    });

    const completedFilter = new logs.MetricFilter(this, 'EventCompletedMetricFilter', {
      logGroup: appLogGroup,
      metricNamespace: 'EventIntelligenceEngine',
      metricName: 'EventsCompleted',
      filterPattern: logs.FilterPattern.literal('event_completed'),
      metricValue: '1',
      defaultValue: 0,
    });

    const permanentFailureAlarm = new cloudwatch.Alarm(this, 'EventPermanentFailureAlarm', {
      metric: permanentFailureFilter.metric({
        statistic: 'sum',
        period: cdk.Duration.minutes(5),
      }),
      threshold: 1,
      evaluationPeriods: 1,
      datapointsToAlarm: 1,
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD,
      alarmDescription: 'Permanent failures detected in event processing',
      treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
    });

    const reviewRequiredHighAlarm = new cloudwatch.Alarm(this, 'EventReviewRequiredHighAlarm', {
      metric: reviewFilter.metric({
        statistic: 'sum',
        period: cdk.Duration.minutes(15),
      }),
      threshold: 20,
      evaluationPeriods: 1,
      datapointsToAlarm: 1,
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD,
      alarmDescription: 'High number of events requiring human review',
      treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
    });

    const reviewRateMetric = new cloudwatch.MathExpression({
      expression: '100 * review / MAX([completed, 1])',
      usingMetrics: {
        review: reviewFilter.metric({ statistic: 'sum', period: cdk.Duration.minutes(15) }),
        completed: completedFilter.metric({ statistic: 'sum', period: cdk.Duration.minutes(15) }),
      },
      label: 'ReviewRequiredRatePercent',
    });

    // Alarms
    const reviewRateAlarm = new cloudwatch.Alarm(this, 'EventReviewRateHighAlarm', {
      metric: reviewRateMetric,
      threshold: 15, // example: >15%
      evaluationPeriods: 1,
      datapointsToAlarm: 1,
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
      alarmDescription: 'High review-required rate compared to completed events',
      treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
    });

    // Connecting alarms to SNS topic
    permanentFailureAlarm.addAlarmAction(new cloudwatch_actions.SnsAction(alarmsTopic));
    reviewRequiredHighAlarm.addAlarmAction(new cloudwatch_actions.SnsAction(alarmsTopic));
    reviewRateAlarm.addAlarmAction(new cloudwatch_actions.SnsAction(alarmsTopic));

    const dashboard = new cloudwatch.Dashboard(this, 'EventIntelligenceEngineDashboard', {
      dashboardName: 'EventIntelligenceEngine',
    });

    // Adding widgets to the dashboard
    dashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'Event Processing Status',
        left: [
          completedFilter.metric({ statistic: 'sum', period: cdk.Duration.minutes(5), label: 'Completed' }),
          temporaryFailureFilter.metric({ statistic: 'sum', period: cdk.Duration.minutes(5), label: 'Temporary Failures' }),
          reviewFilter.metric({ statistic: 'sum', period: cdk.Duration.minutes(5), label: 'Review Required' }),
          permanentFailureFilter.metric({ statistic: 'sum', period: cdk.Duration.minutes(5), label: 'Permanent Failures' }),
        ],
        width: 12,
        height: 6,
      }),
      new cloudwatch.GraphWidget({
        title: 'Review Rate % (15 min)',
        left: [reviewRateMetric],
        width: 12,
        height: 6,
      }),
    );
  }
}