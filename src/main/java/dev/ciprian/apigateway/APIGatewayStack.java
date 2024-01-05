package dev.ciprian.apigateway;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.apigatewayv2.HttpMethod;
import software.amazon.awscdk.services.lambda.Function;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

import static dev.ciprian.config.Constants.*;

public class APIGatewayStack extends Stack {

    private final Resource usersResource;
    private final Resource loginResource;
    private final Resource postsResource;

    public APIGatewayStack(Construct scope, String id, StackProps props) {
        super(scope, id, props);

        var endpointConfiguration = EndpointConfiguration.builder()
                .types(List.of(EndpointType.REGIONAL))
                .build();

        var stageOptions = StageOptions.builder()
                .stageName("dev")
                .description("Development stage for the User Posts app")
                .loggingLevel(MethodLoggingLevel.INFO)
                .dataTraceEnabled(true)
                .build();

        var api = RestApi.Builder.create(this, id)
                .restApiName("user-posts-api")
                .description("API for the User Posts app")
                .cloudWatchRole(true)
                .cloudWatchRoleRemovalPolicy(RemovalPolicy.DESTROY)
                .endpointConfiguration(endpointConfiguration)
                .deployOptions(stageOptions)
                .apiKeySourceType(ApiKeySourceType.HEADER)
                .build();

        var apiKey = ApiKey.Builder.create(this, "APIKey")
                .apiKeyName("api-key")
                .enabled(true)
                .build();

        var usagePlanPerApiStage = UsagePlanPerApiStage.builder()
                .api(api)
                .stage(api.getDeploymentStage())
                .build();

        var usagePlan = UsagePlan.Builder.create(this, "UsagePlan")
                .name("usage-plan")
                .apiStages(List.of(usagePlanPerApiStage))
                .build();

        usagePlan.addApiKey(apiKey);

        this.usersResource = api.getRoot().resourceForPath(USERS_PATH);
        this.loginResource = api.getRoot().resourceForPath(LOGIN_PATH);
        this.postsResource = api.getRoot().resourceForPath(POSTS_PATH);
    }

    public void addRegisterUserRoute(Function function) {
        var requestTemplates = Map.of(APPLICATION_JSON, """
                {
                  "method": "$context.httpMethod",
                  "headers": {
                    "x-function-name": "registerUser"
                  },
                  "body": $input.json('$')
                }
                """);

        var lambdaIntegrationOptions = getLambdaIntegrationOptions(requestTemplates);
        var lambdaIntegration = new LambdaIntegration(function, lambdaIntegrationOptions);
        usersResource.addMethod(HttpMethod.POST.name(), lambdaIntegration, getMethodOptions());
    }

    public void addLoginUserRoute(Function function) {
        var requestTemplates = Map.of(APPLICATION_JSON, """
                {
                  "method": "$context.httpMethod",
                  "headers": {
                    "x-function-name": "loginUser"
                  },
                  "body": $input.json('$')
                }
                """);

        var lambdaIntegrationOptions = getLambdaIntegrationOptions(requestTemplates);
        var lambdaIntegration = new LambdaIntegration(function, lambdaIntegrationOptions);
        loginResource.addMethod(HttpMethod.POST.name(), lambdaIntegration, getMethodOptions());
    }

    public void addGetPostsRoute(Function function, CognitoUserPoolsAuthorizer authorizer) {
        var requestTemplates = Map.of(APPLICATION_JSON, """
                {
                  "method": "$context.httpMethod",
                  "headers": {
                    "x-function-name": "getPosts",
                    "x-user-id": "$context.authorizer.claims.sub"
                  },
                  "body": $input.json('$')
                }
                """);

        var lambdaIntegrationOptions = getLambdaIntegrationOptions(requestTemplates);
        var lambdaIntegration = new LambdaIntegration(function, lambdaIntegrationOptions);
        postsResource.addMethod(HttpMethod.GET.name(), lambdaIntegration, getMethodOptions(authorizer));
    }

    public void addSavePostRoute(Function function, CognitoUserPoolsAuthorizer authorizer) {
        var requestTemplates = Map.of(APPLICATION_JSON, """
                {
                  "method": "$context.httpMethod",
                  "headers": {
                    "x-function-name": "savePost",
                    "x-user-id": "$context.authorizer.claims.sub"
                  },
                  "body": $input.json('$')
                }
                """);

        var lambdaIntegrationOptions = getLambdaIntegrationOptions(requestTemplates);
        var lambdaIntegration = new LambdaIntegration(function, lambdaIntegrationOptions);
        postsResource.addMethod(HttpMethod.POST.name(), lambdaIntegration, getMethodOptions(authorizer));
    }

    public void addDeletePostRoute(Function function, CognitoUserPoolsAuthorizer authorizer) {
        var requestTemplates = Map.of(APPLICATION_JSON, """
                {
                  "method": "$context.httpMethod",
                  "headers": {
                    "x-function-name": "deletePost",
                    "x-user-id": "$context.authorizer.claims.sub"
                  },
                  "body": $input.json('$')
                }
                """);

        var lambdaIntegrationOptions = getLambdaIntegrationOptions(requestTemplates);
        var lambdaIntegration = new LambdaIntegration(function, lambdaIntegrationOptions);
        postsResource.addMethod(HttpMethod.DELETE.name(), lambdaIntegration, getMethodOptions(authorizer));
    }

    @NotNull
    private static LambdaIntegrationOptions getLambdaIntegrationOptions(Map<String, String> requestTemplates) {
        var integrationResponse200 = IntegrationResponse.builder()
                .statusCode(STATUS_CODE_200)
                .responseTemplates(Map.of(APPLICATION_JSON, """
                        #set($body = $input.path('$'))
                        #set($context.responseOverride.status =  $body.statusCode)
                        $input.json('$')
                        """))
                .build();

        var responseTemplates = Map.of(APPLICATION_JSON, """
                #set($errorMessage = $input.path('$.errorMessage'))
                #set($statusCode = $errorMessage.substring(0, 3))
                #set($context.responseOverride.status = $statusCode)
                {
                    "errorMessage": "$errorMessage.substring(4)",
                    "statusCode": $statusCode
                }
                 """);

        var integrationResponse400 = IntegrationResponse.builder()
                .statusCode(STATUS_CODE_400)
                .selectionPattern("400.*")
                .responseTemplates(responseTemplates)
                .build();

        var integrationResponse403 = IntegrationResponse.builder()
                .statusCode(STATUS_CODE_403)
                .selectionPattern("403.*")
                .responseTemplates(responseTemplates)
                .build();

        var integrationResponse500 = IntegrationResponse.builder()
                .statusCode(STATUS_CODE_500)
                .selectionPattern("500.*")
                .responseTemplates(responseTemplates)
                .build();

        return LambdaIntegrationOptions.builder()
                .proxy(false)
                .allowTestInvoke(true)
                .requestTemplates(requestTemplates)
                .passthroughBehavior(PassthroughBehavior.WHEN_NO_TEMPLATES)
                .integrationResponses(List.of(integrationResponse200, integrationResponse400, integrationResponse403, integrationResponse500))
                .build();
    }

    @NotNull
    private static MethodOptions getMethodOptions() {
        return MethodOptions.builder()
                .authorizationType(AuthorizationType.NONE)
                .apiKeyRequired(true)
                .methodResponses(getEmptyMethodResponses())
                .build();
    }

    @NotNull
    private static MethodOptions getMethodOptions(CognitoUserPoolsAuthorizer authorizer) {
        return MethodOptions.builder()
                .authorizationType(AuthorizationType.COGNITO)
                .authorizer(authorizer)
                .apiKeyRequired(true)
                .methodResponses(getEmptyMethodResponses())
                .build();
    }

    @NotNull
    private static List<MethodResponse> getEmptyMethodResponses() {
        var methodResponse200 = MethodResponse.builder()
                .statusCode(STATUS_CODE_200)
                .responseModels(Map.of(APPLICATION_JSON, Model.EMPTY_MODEL))
                .build();

        var methodResponse400 = MethodResponse.builder()
                .statusCode(STATUS_CODE_400)
                .responseModels(Map.of(APPLICATION_JSON, Model.EMPTY_MODEL))
                .build();

        var methodResponse403 = MethodResponse.builder()
                .statusCode(STATUS_CODE_403)
                .responseModels(Map.of(APPLICATION_JSON, Model.EMPTY_MODEL))
                .build();

        var methodResponse500 = MethodResponse.builder()
                .statusCode(STATUS_CODE_500)
                .responseModels(Map.of(APPLICATION_JSON, Model.EMPTY_MODEL))
                .build();

        return List.of(methodResponse200, methodResponse400, methodResponse403, methodResponse500);
    }

}
