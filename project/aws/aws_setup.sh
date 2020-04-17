#!/bin/sh

# Variables for load balancer, target group, launch configuration, auto scaling group, scaling policies and alarms
loadbalancer="my-load-balancer"
targetgroup="my-target-group"
launchConfigurationName="my-lc-from-instance-changetype"
autoScalingGroupName="my-asg-from-instance"
policyIncreaseName="my-simple-scale-in-policy-increase"
policyDecreaseName="my-simple-scale-in-policy-decrease"
alarmHighName="Step-Scaling-AlarmHigh-AddCapacity"
alarmLowName="Step-Scaling-AlarmLow-RemoveCapacity"
security_group="sg-04ceefaf6f626fba3"
image_id="ami-0966bb7855730320b"
key_name="cloud-compute-lab3"
security_group_name="launch-wizard-1-cloud-compute"


if test "$1" = "terminate";
then
    LoadBalancerArn=$(aws elbv2 describe-load-balancers --output json --query 'LoadBalancers[0].LoadBalancerArn' | 
        grep "arn.*" | 
        sed "s/[,\" ]//g" |
        xargs)

    TargetGroupArn=$(aws elbv2 describe-target-groups --output json --query 'TargetGroups[0].TargetGroupArn' | 
        grep "arn.*" | 
        sed "s/[,\" ]//g" |
        xargs)

    instance_id=$(aws ec2 describe-instances --output json --query 'Reservations[0].Instances[0].InstanceId' | 
        grep "i-*" | 
        sed "s/[,\" ]//g" |
        xargs)

    aws ec2 terminate-instances --instance-ids $instance_id
    aws autoscaling delete-auto-scaling-group --auto-scaling-group-name $autoScalingGroupName --force-delete
    aws autoscaling delete-launch-configuration --launch-configuration-name $launchConfigurationName

    if [ $(echo -n $LoadBalancerArn | wc -m) -ge 1 ];
    then
        aws elbv2 delete-load-balancer --load-balancer-arn $LoadBalancerArn
    fi
    if [ $(echo -n $TargetGroupArn | wc -m) -ge 1 ];
    then

        aws elbv2 delete-target-group --target-group-arn $TargetGroupArn
    fi
    exit 1
fi


# Create load balancer
aws elbv2 create-load-balancer --name $loadbalancer --subnets subnet-00866866 subnet-3b37dc64 --security-groups $security_group

LoadBalancerArn=$(aws elbv2 describe-load-balancers --output json --query 'LoadBalancers[0].LoadBalancerArn' | 
    grep "arn.*" | 
    sed "s/[,\" ]//g" |
    xargs)


# Create target group
aws elbv2 create-target-group --name $targetgroup --protocol HTTP --port 8000 --vpc-id vpc-e1665c9b

TargetGroupArn=$(aws elbv2 describe-target-groups --output json --query 'TargetGroups[0].TargetGroupArn' | 
    grep "arn.*" | 
    sed "s/[,\" ]//g" |
    xargs)


# Create listener
aws elbv2 create-listener --load-balancer-arn $LoadBalancerArn --protocol HTTP --port 80 --default-actions Type=forward,TargetGroupArn=$TargetGroupArn


# Create launch configuration
aws autoscaling create-launch-configuration --launch-configuration-name $launchConfigurationName  \
    --image-id $image_id --key-name $key_name --instance-type t2.micro --instance-monitoring Enabled=true --security-groups $security_group


# Create auto scaling group
aws autoscaling create-auto-scaling-group --auto-scaling-group-name $autoScalingGroupName \
--launch-configuration-name $launchConfigurationName --target-group-arns $TargetGroupArn \
--min-size 1 --max-size 2 --desired-capacity 1 --vpc-zone-identifier "subnet-3b37dc64" --health-check-type ELB --health-check-grace-period 60


# Create scaling policy
aws autoscaling put-scaling-policy --policy-name $policyIncreaseName \
--auto-scaling-group-name $autoScalingGroupName --scaling-adjustment 1 \
--adjustment-type ChangeInCapacity

aws autoscaling put-scaling-policy --policy-name $policyDecreaseName \
--auto-scaling-group-name $autoScalingGroupName --scaling-adjustment -1 \
--adjustment-type ChangeInCapacity

policyIncreaseArn=$(aws autoscaling describe-policies --auto-scaling-group-name my-asg-from-instance --output json --query 'ScalingPolicies[*].PolicyARN' | 
    grep "arn.*increase" | 
    sed "s/[,\" ]//g" |
    xargs)

policyDecreaseArn=$(aws autoscaling describe-policies --auto-scaling-group-name my-asg-from-instance --output json --query 'ScalingPolicies[*].PolicyARN' | 
    grep "arn.*decrease" | 
    sed "s/[,\" ]//g" |
    xargs)


# Create alarms
aws cloudwatch put-metric-alarm --alarm-name $alarmHighName \
--metric-name CPUUtilization --namespace AWS/EC2 --statistic Average \
--period 60 --evaluation-periods 1 --threshold 60 \
--comparison-operator GreaterThanOrEqualToThreshold \
--dimensions "Name=AutoScalingGroupName,Value=$autoScalingGroupName" \
--alarm-actions $policyIncreaseArn

aws cloudwatch put-metric-alarm --alarm-name $alarmLowName \
--metric-name CPUUtilization --namespace AWS/EC2 --statistic Average \
--period 60 --evaluation-periods 1 --threshold 40 \
--comparison-operator LessThanOrEqualToThreshold \
--dimensions "Name=AutoScalingGroupName,Value=$autoScalingGroupName" \
--alarm-actions $policyDecreaseArn

# To enable group metrics
aws autoscaling enable-metrics-collection --auto-scaling-group-name $autoScalingGroupName \
--metrics GroupDesiredCapacity --granularity "1Minute"

# Get DNS name for load balancer
aws elb describe-load-balancers --load-balancer-names $loadbalancer