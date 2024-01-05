package dev.ciprian.cognito;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.cognito.*;
import software.constructs.Construct;

public class CognitoStack extends Stack {

    private final UserPool userPool;
    private final UserPoolClient userPoolClient;

    public CognitoStack(Construct scope, String id, StackProps props) {
        super(scope, id, props);

        var signInAliases = SignInAliases.builder()
                .username(true)
                .preferredUsername(true)
                .build();

        var standardAttributes = StandardAttributes.builder()
                .email(StandardAttribute.builder().required(true).build())
                .build();

        this.userPool = UserPool.Builder.create(this, "UserPool")
                .userPoolName("user-posts")
                .signInAliases(signInAliases)
                .signInCaseSensitive(false)
                .selfSignUpEnabled(false)
                .mfa(Mfa.OFF)
                .accountRecovery(AccountRecovery.NONE)
                .standardAttributes(standardAttributes)
                .build();

        var authFlows = AuthFlow.builder()
                .userSrp(false)
                .adminUserPassword(true)
                .custom(false)
                .build();

        this.userPoolClient = UserPoolClient.Builder.create(this, "AppClient")
                .userPool(userPool)
                .userPoolClientName("app")
                .authFlows(authFlows)
                .preventUserExistenceErrors(true)
                .build();
    }

    public UserPool getUserPool() {
        return userPool;
    }

    public UserPoolClient getUserPoolClient() {
        return userPoolClient;
    }

}
