package org.apache.isis.viewer.graphql.viewer.webmodule;

import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.graphql.web.WebInput;
import org.springframework.graphql.web.WebInterceptor;
import org.springframework.graphql.web.WebInterceptorChain;
import org.springframework.graphql.web.WebOutput;
import org.springframework.stereotype.Service;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import reactor.core.publisher.Mono;

@Service
public class MyGraphQlSource implements GraphQlSource {

    @Override
    public GraphQL graphQl() {
        return GraphQL.newGraphQL(schema()).build();
    }

    @Override
    public GraphQLSchema schema() {
        return GraphQLSchema.newSchema()
                .build();
    }
}
