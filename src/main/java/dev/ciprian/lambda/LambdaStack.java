package dev.ciprian.lambda;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.Architecture;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;

import java.util.List;

public class LambdaStack extends Stack {

    private final Function function;

    public LambdaStack(Construct scope, String id, StackProps props) {
        super(scope, id, props);

        var description = "Lambda with Spring Cloud Function";
        var handler = "org.springframework.cloud.function.adapter.aws.FunctionInvoker::handleRequest";
        var codePath = "../user-posts-lambda/build/libs/user-posts-lambda-1.0-SNAPSHOT-aws.jar";
        var functionName = "user-posts";

        this.function = Function.Builder.create(this, "UserPostsLambda")
                .description(description)
                .code(Code.fromAsset(codePath))
                .runtime(Runtime.JAVA_21)
                .handler(handler)
                .architecture(Architecture.ARM_64)
                .functionName(functionName)
                .role(buildUserPostsLambdaRole())
                .timeout(Duration.seconds(10))
                .memorySize(512)
                .build();
    }

    @NotNull
    private Role buildUserPostsLambdaRole() {
        var roleName = "user-posts";
        var description = "User Posts lambda execution role, managed by CDK";
        var principal = new ServicePrincipal("lambda.amazonaws.com");

        var basicExecutionArn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole";
        var executionPolicy = ManagedPolicy.fromManagedPolicyArn(this, "UserPostsLambdaBasicExecutionPolicy", basicExecutionArn);

        return Role.Builder.create(this, "UserPostsExecutionRole")
                .roleName(roleName)
                .description(description)
                .assumedBy(principal)
                .managedPolicies(List.of(executionPolicy))
                .build();
    }

    public Function getFunction() {
        return function;
    }

}
