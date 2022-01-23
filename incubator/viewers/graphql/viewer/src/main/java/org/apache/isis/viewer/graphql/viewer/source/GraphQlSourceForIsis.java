package org.apache.isis.viewer.graphql.viewer.source;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.stereotype.Service;

import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.services.registry.ServiceRegistry;
import org.apache.isis.core.config.IsisConfiguration;
import org.apache.isis.core.config.environment.IsisSystemEnvironment;
import org.apache.isis.core.config.metamodel.specloader.IntrospectionMode;
import org.apache.isis.core.metamodel.facets.actions.semantics.ActionSemanticsFacet;
import org.apache.isis.core.metamodel.spec.feature.MixedIn;
import org.apache.isis.core.metamodel.spec.feature.ObjectAction;
import org.apache.isis.core.metamodel.specloader.SpecificationLoader;

import lombok.RequiredArgsConstructor;
import lombok.val;

import graphql.GraphQL;
import graphql.Scalars;
import graphql.execution.instrumentation.tracing.TracingInstrumentation;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import static graphql.schema.FieldCoordinates.coordinates;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

@Service()
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class GraphQlSourceForIsis implements GraphQlSource {

    private final ServiceRegistry serviceRegistry;
    private final SpecificationLoader specificationLoader;
    private final IsisConfiguration isisConfiguration;
    private final IsisSystemEnvironment isisSystemEnvironment;
    private final ExecutionStrategyResolvingWithinInteraction executionStrategy;

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
                // .instrumentation(new TracingInstrumentation())
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

        val queryBuilder = newObject().name("Query");
        val codeRegistryBuilder = GraphQLCodeRegistry.newCodeRegistry();

        specificationLoader.forEach(objectSpecification -> {

            val logicalTypeName = objectSpecification.getLogicalTypeName();
            val correspondingClass = objectSpecification.getCorrespondingClass();
            switch (objectSpecification.getBeanSort()) {
                case VIEW_MODEL: // @DomainObject(nature=VIEW_MODEL)
                    // TODO
                    break;
                case ENTITY:    // @DomainObject(nature=ENTITY)
                    // TODO
                    break;
                case MANAGED_BEAN_CONTRIBUTING: //@DomainService

                    serviceRegistry.lookupBeanById(logicalTypeName).ifPresent(service -> {

                        String logicalTypeNameSanitized = logicalTypeName.replace('.', '_');

                        // as a first pass, expose a single "property" of the service, which is just a count of how
                        // many safe actions there are
                        val serviceAsGraphQlType = newObject().name(logicalTypeNameSanitized)
                                .field(newFieldDefinition()
                                        .name("numSafeActions")
                                        .type(Scalars.GraphQLInt)
                                        .build()).build();
                        codeRegistryBuilder.dataFetcher(
                                FieldCoordinates.coordinates(serviceAsGraphQlType, "numSafeActions"),
                                (DataFetcher<Object>) environment -> objectSpecification.streamRuntimeActions(MixedIn.INCLUDED)
                                        .map(ObjectAction.class::cast)
                                        .filter((ObjectAction x) -> x.containsFacet(ActionSemanticsFacet.class))
                                        .map(x -> x.getFacet(ActionSemanticsFacet.class))
                                        .map(x -> x.value() == SemanticsOf.SAFE)
                                        .count());

                        // make the service accessible from the top-level Query
                        queryBuilder.field(newFieldDefinition().name(logicalTypeNameSanitized).type(serviceAsGraphQlType).build());
                        codeRegistryBuilder.dataFetcher(FieldCoordinates.coordinates("Query", newFieldDefinition().name(logicalTypeNameSanitized).type(serviceAsGraphQlType).build().getName()), (DataFetcher<Object>) environment -> service);
                    });
                    break;

                case MANAGED_BEAN_NOT_CONTRIBUTING: // a @Service or @Component ... ignore
                case MIXIN:
                case VALUE:
                case COLLECTION:
                case ABSTRACT:
                case VETOED:
                case UNKNOWN:
                    break;
            }
        }, false);

        // type Query {
        //     numServices: Int
        // }
        val query_numServices = newFieldDefinition()
                .name("numServices")
                .type(Scalars.GraphQLInt)
                .build();

        GraphQLObjectType query = queryBuilder
                .field(query_numServices)
                .build();


        val codeRegistry = codeRegistryBuilder
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
