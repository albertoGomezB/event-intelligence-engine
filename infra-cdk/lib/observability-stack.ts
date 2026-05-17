import * as cdk from 'aws-cdk-lib';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as subscriptions from 'aws-cdk-lib/aws-sns-subscriptions';
import * as cloudwatch_actions from 'aws-cdk-lib/aws-cloudwatch-actions';
import { Construct } from 'constructs';
import { AppConfig, EnvironmentConfig } from './shared/config';

export interface ObservabilityStackProps extends cdk.StackProps {
  environment: 'dev' | 'staging' | 'prod';
}

export class ObservabilityStack extends cdk.Stack {
  public readonly alarmsTopic: sns.Topic;
  public readonly appLogGroup: logs.LogGroup;

  constructor(scope: Construct, id: string, props: ObservabilityStackProps) {
    super(scope, id, props);

    const envConfig = EnvironmentConfig(props.environment);

    // Log Group
    this.appLogGroup = new logs.LogGroup(this, 'AppLogGroup', {
      logGroupName: AppConfig.logGroupName,
      retention: this.getRetentionDays(envConfig.logRetention),
      removalPolicy:
        envConfig.removalPolicy === 'DESTROY'
          ? cdk.RemovalPolicy.DESTROY
          : cdk.RemovalPolicy.RETAIN,
    });

    cdk.Tags.of(this.appLogGroup).add('Environment', props.environment);

    // SNS Topic for Alarms
    this.alarmsTopic = new sns.Topic(this, 'AlarmsTopic', {
      topicName: `${AppConfig.alarmTopicName}-${props.environment}`,
    });

    cdk.Tags.of(this.alarmsTopic).add('Environment', props.environment);

    // Optional email subscription
    const alarmEmail = this.node.tryGetContext('alarmEmail');
    if (alarmEmail) {
      this.alarmsTopic.addSubscription(
        new subscriptions.EmailSubscription(alarmEmail)
      );
    }

    // Metric Filters
    const temporaryFailureFilter = new logs.MetricFilter(
      this,
      'TempFailureFilter',
      {
        logGroup: this.appLogGroup,
        metricNamespace: AppConfig.metricNamespace,
        metricName: 'EventsTemporaryFailure',
        filterPattern: logs.FilterPattern.literal('event_temporary_failure'),
        metricValue: '1',
        defaultValue: 0,
      }
    );

    const reviewFilter = new logs.MetricFilter(this, 'ReviewFilter', {
      logGroup: this.appLogGroup,
      metricNamespace: AppConfig.metricNamespace,
      metricName: 'EventsReviewRequired',
      filterPattern: logs.FilterPattern.literal('event_review_required'),
      metricValue: '1',
      defaultValue: 0,
    });

    const permanentFailureFilter = new logs.MetricFilter(
      this,
      'PermFailureFilter',
      {
        logGroup: this.appLogGroup,
        metricNamespace: AppConfig.metricNamespace,
        metricName: 'EventsPermanentFailure',
        filterPattern: logs.FilterPattern.literal('event_permanent_failure'),
        metricValue: '1',
        defaultValue: 0,
      }
    );

    // Alarms
    const permanentFailureAlarm = new cloudwatch.Alarm(
      this,
      'PermFailureAlarm',
      {
        metric: permanentFailureFilter.metric({
          statistic: 'sum',
          period: cdk.Duration.minutes(5),
        }),
        threshold: 1,
        evaluationPeriods: 1,
        datapointsToAlarm: 1,
        comparisonOperator:
          cloudwatch.ComparisonOperator
            .GREATER_THAN_OR_EQUAL_TO_THRESHOLD,
        alarmDescription:
          'Permanent failures detected in event processing',
        treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
      }
    );

    const reviewRequiredAlarm = new cloudwatch.Alarm(
      this,
      'ReviewRequiredAlarm',
      {
        metric: reviewFilter.metric({
          statistic: 'sum',
          period: cdk.Duration.minutes(15),
        }),
        threshold: 20,
        evaluationPeriods: 1,
        datapointsToAlarm: 1,
        comparisonOperator:
          cloudwatch.ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD,
        alarmDescription:
          'High number of events requiring human review',
        treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
      }
    );

    // Connect alarms to SNS
    permanentFailureAlarm.addAlarmAction(
      new cloudwatch_actions.SnsAction(this.alarmsTopic)
    );
    reviewRequiredAlarm.addAlarmAction(
      new cloudwatch_actions.SnsAction(this.alarmsTopic)
    );

    // Dashboard
    const dashboard = new cloudwatch.Dashboard(
      this,
      'EventIntelligenceDashboard',
      {
        dashboardName: `EventIntelligenceEngine-${props.environment}`,
      }
    );

    dashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'Event Processing Status',
        left: [
          temporaryFailureFilter.metric({
            statistic: 'sum',
            period: cdk.Duration.minutes(5),
            label: 'Temporary Failures',
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
      })
    );

    // Outputs
    new cdk.CfnOutput(this, 'AlarmsTopicArn', {
      value: this.alarmsTopic.topicArn,
      exportName: `${id}-AlarmsTopicArn`,
    });

    new cdk.CfnOutput(this, 'LogGroupName', {
      value: this.appLogGroup.logGroupName,
      exportName: `${id}-LogGroupName`,
    });
  }

  private getRetentionDays(days: number): logs.RetentionDays {
    const map: { [key: number]: logs.RetentionDays } = {
      7: logs.RetentionDays.ONE_WEEK,
      14: logs.RetentionDays.TWO_WEEKS,
      30: logs.RetentionDays.ONE_MONTH,
      90: logs.RetentionDays.THREE_MONTHS,
    };
    return map[days] || logs.RetentionDays.ONE_MONTH;
  }
}

