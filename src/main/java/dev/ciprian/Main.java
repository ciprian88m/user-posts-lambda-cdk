package dev.ciprian;

import dev.ciprian.apigateway.APIGatewayStack;
import dev.ciprian.cognito.CognitoStack;
import dev.ciprian.dynamodb.DynamoDbStack;
import dev.ciprian.lambda.LambdaStack;
import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.CognitoUserPoolsAuthorizer;

import java.util.List;

import static dev.ciprian.config.Constants.*;

public class Main {

    public static void main(final String[] args) {
        var app = new App();

        var lambdaStack = new LambdaStack(app, "UserPostsLambdaStack", getStackProps());

        var cognitoStack = new CognitoStack(app, "UserPostsCognitoStack", getStackProps());

        lambdaStack.getFunction().addEnvironment(ENV_REGION, lambdaStack.getRegion());
        lambdaStack.getFunction().addEnvironment(ENV_USER_POOL_ID, cognitoStack.getUserPool().getUserPoolId());
        lambdaStack.getFunction().addEnvironment(ENV_CLIENT_ID, cognitoStack.getUserPoolClient().getUserPoolClientId());

        var functionPermissions = new String[]{"cognito-idp:AdminCreateUser", "cognito-idp:AdminSetUserPassword", "cognito-idp:AdminInitiateAuth"};
        cognitoStack.getUserPool().grant(lambdaStack.getFunction(), functionPermissions);

        var apiGatewayStack = new APIGatewayStack(app, "UserPostsAPIStack", getStackProps());

        var authorizer = CognitoUserPoolsAuthorizer.Builder.create(apiGatewayStack, "UserPostsCognitoAuthorizer")
                .cognitoUserPools(List.of(cognitoStack.getUserPool()))
                .build();

        apiGatewayStack.addRegisterUserRoute(lambdaStack.getFunction());
        apiGatewayStack.addLoginUserRoute(lambdaStack.getFunction());
        apiGatewayStack.addGetPostsRoute(lambdaStack.getFunction(), authorizer);
        apiGatewayStack.addSavePostRoute(lambdaStack.getFunction(), authorizer);
        apiGatewayStack.addDeletePostRoute(lambdaStack.getFunction(), authorizer);

        var dynamoStack = new DynamoDbStack(app, "UserPostsDynamoStack", getStackProps());
        dynamoStack.getTable().grantReadWriteData(lambdaStack.getFunction());

        lambdaStack.getFunction().addEnvironment(ENV_TABLE_NAME, dynamoStack.getTable().getTableName());

        app.synth();
    }

    @NotNull
    private static StackProps getStackProps() {
        return StackProps.builder()
                .env(getEnvironment())
                .description("Spring Cloud Function-based app for managing users and posts")
                .build();
    }

    @NotNull
    private static Environment getEnvironment() {
        return Environment.builder()
                .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                .region(System.getenv("CDK_DEFAULT_REGION"))
                .build();
    }

}
