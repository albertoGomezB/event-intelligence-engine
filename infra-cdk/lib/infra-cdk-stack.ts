import * as cdk from 'aws-cdk-lib';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import { Construct } from 'constructs';

export class InfraCdkStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const appLogGroup = new logs.LogGroup(this, 'EventIntelligenceEngineLogGroup', {
      logGroupName: '/event-intelligence-engine/application',
      retention: logs.RetentionDays.TWO_WEEKS,
      removalPolicy: cdk.RemovalPolicy.DESTROY, // prod: RETAIN
    });

    // 1) temporary failure (retries)
    const temporaryFailureFilter = new logs.MetricFilter(this, 'EventTemporaryFailureMetricFilter', {
      logGroup: appLogGroup,
      metricNamespace: 'EventIntelligenceEngine',
      metricName: 'EventsTemporaryFailure',
      filterPattern: logs.FilterPattern.literal('event_temporary_failure'),
      metricValue: '1',
      defaultValue: 0,
    });

    // 2) review required
    const reviewFilter = new logs.MetricFilter(this, 'EventReviewRequiredMetricFilter', {
      logGroup: appLogGroup,
      metricNamespace: 'EventIntelligenceEngine',
      metricName: 'EventsReviewRequired',
      filterPattern: logs.FilterPattern.literal('event_review_required'),
      metricValue: '1',
      defaultValue: 0,
    });

    // 3) permanent failure
    const permanentFailureFilter = new logs.MetricFilter(this, 'EventPermanentFailureMetricFilter', {
      logGroup: appLogGroup,
      metricNamespace: 'EventIntelligenceEngine',
      metricName: 'EventsPermanentFailure',
      filterPattern: logs.FilterPattern.literal('event_permanent_failure'),
      metricValue: '1',
      defaultValue: 0,
    });

    // Alarm: permanent failure detected every 5 minutes
    new cloudwatch.Alarm(this, 'EventPermanentFailureAlarm', {
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

    // Alarm: too many permanent failures detected every 5 minutes (adjust threshold)
    new cloudwatch.Alarm(this, 'EventReviewRequiredHighAlarm', {
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

    // Dashboard: Event Intelligence Engine
    new cloudwatch.Dashboard(this, 'EventIntelligenceEngineDashboard', {
      dashboardName: 'EventIntelligenceEngine',
    }).addWidgets(
      new cloudwatch.GraphWidget({
        title: 'Event Processing Status',
        left: [
          temporaryFailureFilter.metric({
            statistic: 'sum',
            period: cdk.Duration.minutes(5),
            label: 'Temporary Failures (retries)',
          }),
          reviewFilter.metric({
            statistic: 'sum',
            period: cdk.Duration.minutes(5),
            label: 'Review Required',
          }),
          permanentFailureFilter.metric({
            statistic: 'sum',
            period: cdk.Duration.minutes(5),
            label: 'Permanent Failures',
          }),
        ],
        width: 12,
        height: 6,
      }),
      new cloudwatch.SingleValueWidget({
        title: 'Temporary Failures (Last 5 min)',
        metrics: [
          temporaryFailureFilter.metric({
            statistic: 'sum',
            period: cdk.Duration.minutes(5),
          }),
        ],
        width: 4,
        height: 3,
      }),
      new cloudwatch.SingleValueWidget({
        title: 'Review Required (Last 15 min)',
        metrics: [
          reviewFilter.metric({
            statistic: 'sum',
            period: cdk.Duration.minutes(15),
          }),
        ],
        width: 4,
        height: 3,
      }),
      new cloudwatch.SingleValueWidget({
        title: 'Permanent Failures (Last 5 min)',
        metrics: [
          permanentFailureFilter.metric({
            statistic: 'sum',
            period: cdk.Duration.minutes(5),
          }),
        ],
        width: 4,
        height: 3,
      }),
    );
  }
}