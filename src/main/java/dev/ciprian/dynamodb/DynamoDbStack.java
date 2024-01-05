package dev.ciprian.dynamodb;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;
import software.constructs.Construct;

public class DynamoDbStack extends Stack {

    private final Table table;

    public DynamoDbStack(Construct scope, String id, StackProps props) {
        super(scope, id, props);

        this.table = Table.Builder.create(this, "UserPosts")
                .tableName("UserPosts")
                .partitionKey(Attribute.builder().name("UserId").type(AttributeType.STRING).build())
                .sortKey(Attribute.builder().name("PostTitle").type(AttributeType.STRING).build())
                .billingMode(BillingMode.PROVISIONED)
                .readCapacity(5)
                .writeCapacity(5)
                .build();
    }

    public Table getTable() {
        return table;
    }

}
