package org.apache.isis.viewer.graphql.viewer.source;

import java.util.concurrent.CountDownLatch;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.stereotype.Service;

import org.apache.isis.applib.services.registry.ServiceRegistry;
import org.apache.isis.core.config.IsisConfiguration;
import org.apache.isis.core.config.environment.IsisSystemEnvironment;
import org.apache.isis.core.config.metamodel.specloader.IntrospectionMode;
import org.apache.isis.core.metamodel.specloader.SpecificationLoaderDefault;

import lombok.RequiredArgsConstructor;
import lombok.val;

import graphql.GraphQL;
import graphql.Scalars;
import graphql.execution.instrumentation.tracing.TracingInstrumentation;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import static graphql.schema.FieldCoordinates.coordinates;

@Service()
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class GraphQlSourceForIsis implements GraphQlSource {

    private final ServiceRegistry serviceRegistry;
    private final SpecificationLoaderDefault specificationLoader;
    private final IsisConfiguration isisConfiguration;
    private final IsisSystemEnvironment isisSystemEnvironment;
    private final ExecutionStrategyResolvingWithinInteraction executionStrategy;

    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    @PostConstruct
    public void init() {
        boolean fullyIntrospect = IntrospectionMode.isFullIntrospect(isisConfiguration, isisSystemEnvironment);
        if (!fullyIntrospect) {
            throw new IllegalStateException("GraphQL requires full introspection mode");
        }
    }

    @Override
    public GraphQL graphQl() {
        return GraphQL.newGraphQL(schema())
                .instrumentation(new TracingInstrumentation())
                .queryExecutionStrategy(executionStrategy)
                .build();
    }

    @Override
    public GraphQLSchema schema() {

        val fullyIntrospected = specificationLoader.isMetamodelFullyIntrospected();
        if(!fullyIntrospected) {
            throw new IllegalStateException("Metamodel is not fully introspected");
        }

//        // type LeaseRepository {
//        //     numLeases: Int
//        // }
//        val leaseRepository_numLeases = GraphQLFieldDefinition.newFieldDefinition()
//                .name("numLeases")
//                .type(Scalars.GraphQLInt)
//                .build();
//        val leaseRepositoryType = GraphQLObjectType.newObject()
//                .name("LeaseRepository")
//                .field(leaseRepository_numLeases)
//                .build();

//        // type Query {
//        //     leaseRepo: LeaseRepository
//        // }
//        val query_leaseRepo = GraphQLFieldDefinition.newFieldDefinition()
//                .name("leaseRepo")
//                .type(GraphQLTypeReference.typeRef(leaseRepositoryType.getName()))
//                .build();
//        GraphQLObjectType query = GraphQLObjectType.newObject()
//                .name("Query")
//                .field(query_leaseRepo)
//                .build();
//
//        val codeRegistry = GraphQLCodeRegistry.newCodeRegistry()
//                .dataFetcher(coordinates(query.getName(), query_leaseRepo.getName()),
//                        (DataFetcher<Object>) environment -> leaseRepository)
//                .dataFetcher(coordinates(leaseRepositoryType.getName(), leaseRepository_numLeases.getName()),
//                        (DataFetcher<Object>) environment -> leaseRepository.numLeases)
//                .build();

        // type Query {
        //     numServices: Int
        // }
        val query_numServices = GraphQLFieldDefinition.newFieldDefinition()
                .name("numServices")
                .type(Scalars.GraphQLInt)
                .build();
        GraphQLObjectType query = GraphQLObjectType.newObject()
                .name("Query")
                .field(query_numServices)
                .build();


        val codeRegistry = GraphQLCodeRegistry.newCodeRegistry()
        .dataFetcher(coordinates(query.getName(), query_numServices.getName()),
                (DataFetcher<Object>) environment -> this.serviceRegistry.streamRegisteredBeans().count())
        .build();

        return GraphQLSchema.newSchema()
                .query(query)
                // .additionalType(leaseRepositoryType)
                .codeRegistry(codeRegistry)
                .build();
    }

}
