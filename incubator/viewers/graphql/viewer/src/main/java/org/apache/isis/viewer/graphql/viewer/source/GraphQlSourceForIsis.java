package org.apache.isis.viewer.graphql.viewer.source;

import graphql.GraphQL;
import graphql.Scalars;
import graphql.schema.*;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.services.registry.ServiceRegistry;
import org.apache.isis.core.config.IsisConfiguration;
import org.apache.isis.core.config.environment.IsisSystemEnvironment;
import org.apache.isis.core.config.metamodel.specloader.IntrospectionMode;
import org.apache.isis.core.metamodel.facets.actions.semantics.ActionSemanticsFacet;
import org.apache.isis.core.metamodel.spec.feature.MixedIn;
import org.apache.isis.core.metamodel.spec.feature.ObjectAction;
import org.apache.isis.core.metamodel.spec.feature.ObjectFeature;
import org.apache.isis.core.metamodel.specloader.SpecificationLoader;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

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
//                .instrumentation(new TracingInstrumentation())
                .queryExecutionStrategy(executionStrategy)
                .build();
    }

    @Override
    public GraphQLSchema schema() {

        val fullyIntrospected = specificationLoader.isMetamodelFullyIntrospected();
        if (!fullyIntrospected) {
            throw new IllegalStateException("Metamodel is not fully introspected");
        }

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

                                List<ObjectAction> objectActionList = objectSpecification.streamRuntimeActions(MixedIn.INCLUDED)
                                        .map(ObjectAction.class::cast)
                                        .filter((ObjectAction x) -> x.containsFacet(ActionSemanticsFacet.class))
                                        .filter(x -> x.getFacet(ActionSemanticsFacet.class).value() == SemanticsOf.SAFE)
                                        .collect(Collectors.toList());

                                // for now filters when no safe actions
                                if (!objectActionList.isEmpty()) {

                                    String logicalTypeNameSanitized = logicalTypeName.replace('.', '_');

                                    val serviceAsGraphQlType = newObject().name(logicalTypeNameSanitized);

                                    objectActionList
                                            .forEach(objectAction -> {
                                                String fieldName = objectAction.getId();


                                                GraphQLFieldDefinition.Builder builder = newFieldDefinition()
                                                        .name(fieldName)
                                                        .type(TypeMapper.outputTypeFor(objectAction));
                                                if (objectAction.getParameters().isNotEmpty()) {
                                                    builder.arguments(objectAction.getParameters().stream()
                                                            .map(objectActionParameter -> GraphQLArgument.newArgument()
                                                                    .name(objectActionParameter.getId())
                                                                    .type(TypeMapper.inputTypeFor(objectActionParameter))
                                                                    .build())
                                                            .collect(Collectors.toList()));
                                                }
                                                serviceAsGraphQlType
                                                        .field(builder
                                                                .build());

                                            });

                                    GraphQLObjectType graphQLObjectType = serviceAsGraphQlType.build();

                                    objectActionList
                                            .forEach(objectAction -> {

                                                String fieldName = objectAction.getId();
                                                codeRegistryBuilder.dataFetcher(
                                                        FieldCoordinates.coordinates(graphQLObjectType, fieldName),
                                                        new DataFetcher<Object>() {

                                                            @Override
                                                            public Object get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {

                                                                // objectAction.execute()

                                                                return "TODO - " + fieldName;
                                                            }

                                                        });

                                            });
                                    // corresponding data fetch

                                    // make the service accessible from the top-level Query
                                    // query {
                                    //   demo_SomeService: demo_SomeService;
                                    // }
                                    queryBuilder.field(newFieldDefinition().name(logicalTypeNameSanitized).type(serviceAsGraphQlType).build());
                                    codeRegistryBuilder.dataFetcher(FieldCoordinates.coordinates("Query", newFieldDefinition().name(logicalTypeNameSanitized).type(serviceAsGraphQlType).build().getName()), (DataFetcher<Object>) environment -> service);
                                }
                            }

                    );
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
                .codeRegistry(codeRegistry)
                .build();
    }

}
