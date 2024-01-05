# User Posts Lambda CDK

## About this repo

Setup infrastructure meant for the [user-posts-lambda](https://github.com/ciprian88m/user-posts-lambda) repo.

Manages stacks for API Gateway, Cognito, DynamoDB and the Lambda function.

Notes:
- all routes are secured with an API key that is generated in the API Gateway
- posts routes are also secured with a Cognito-based authorizer

A Postman collection is included for easier testing.

## Commands

Since it's a CDK project, the standard commands apply:

* `mvn package`     compile and run tests
* `cdk ls`          list all stacks in the app
* `cdk synth`       emits the synthesized CloudFormation template
* `cdk deploy`      deploy this stack to your default AWS account/region
* `cdk diff`        compare deployed stack with current state
* `cdk docs`        open CDK documentation

You should run `cdk bootstrap` if this is your first CDK project.
