package org.apache.isis.viewer.graphql.viewer.source;

import javax.inject.Inject;

import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.stereotype.Service;

import org.apache.isis.viewer.graphql.viewer.source.dummydomain.LeaseRepository;

import lombok.RequiredArgsConstructor;
import lombok.val;

import graphql.GraphQL;
import graphql.Scalars;
import graphql.execution.AsyncExecutionStrategy;
import graphql.execution.instrumentation.tracing.TracingInstrumentation;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeReference;

import static graphql.schema.FieldCoordinates.coordinates;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class MyGraphSource implements GraphQlSource {

    private final LeaseRepository leaseRepository;
    private final ExecutionStrategyResolvingWithinInteraction executionStrategy;

    @Override
    public GraphQL graphQl() {
        // val asyncExecutionStrategy = new AsyncExecutionStrategy();
        return GraphQL.newGraphQL(schema())
                .instrumentation(new TracingInstrumentation())
                .queryExecutionStrategy(executionStrategy)
                .build();
    }

    @Override
    public GraphQLSchema schema() {

        // type LeaseRepository {
        //     numLeases: Int
        // }
        val leaseRepository_numLeases = GraphQLFieldDefinition.newFieldDefinition()
                .name("numLeases")
                .type(Scalars.GraphQLInt)
                .build();
        val leaseRepositoryType = GraphQLObjectType.newObject()
                .name("LeaseRepository")
                .field(leaseRepository_numLeases)
                .build();

        // type Query {
        //     leaseRepo: LeaseRepository
        // }
        val query_leaseRepo = GraphQLFieldDefinition.newFieldDefinition()
                .name("leaseRepo")
                .type(GraphQLTypeReference.typeRef(leaseRepositoryType.getName()))
                .build();
        GraphQLObjectType query = GraphQLObjectType.newObject()
                .name("Query")
                .field(query_leaseRepo)
                .build();

        val codeRegistry = GraphQLCodeRegistry.newCodeRegistry()
                .dataFetcher(coordinates(query.getName(), query_leaseRepo.getName()),
                        (DataFetcher<Object>) environment -> leaseRepository)
                .dataFetcher(coordinates(leaseRepositoryType.getName(), leaseRepository_numLeases.getName()),
                        (DataFetcher<Object>) environment -> leaseRepository.numLeases)
                .build();


        return GraphQLSchema.newSchema()
                .query(query)
                .additionalType(leaseRepositoryType)
                .codeRegistry(codeRegistry)
                .build();
    }

}
