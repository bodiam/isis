package org.apache.isis.viewer.graphql.viewer.source;

import graphql.AssertException;
import graphql.Scalars;
import graphql.schema.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class ScratchPad {

    @Test
    public void simple_case() {

        // given
        GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject().name("query");
        queryBuilder.field(GraphQLFieldDefinition.newFieldDefinition().name("f1").type(Scalars.GraphQLString).build());
        GraphQLObjectType query = queryBuilder.build();

        final Map<String, GraphQLObjectType.Builder> entityBuilders = new HashMap<>();

        GraphQLObjectType.Builder eb1 = GraphQLObjectType.newObject().name("e1");
        eb1.field(GraphQLFieldDefinition.newFieldDefinition().name("e1f1").type(Scalars.GraphQLString).build());
        entityBuilders.put("e1", eb1);

        GraphQLObjectType.Builder eb2 = GraphQLObjectType.newObject().name("e2");
        eb2.field(GraphQLFieldDefinition.newFieldDefinition().name("e2f1").type(Scalars.GraphQLString).build());
        entityBuilders.put("e2", eb2);

        // when
        Set<GraphQLType> entityTypes = new HashSet<>();
        entityBuilders.forEach((k, b) -> entityTypes.add(b.build()));

        // then

        GraphQLSchema schema = GraphQLSchema.newSchema()
                .query(query)
                .additionalTypes(entityTypes)
//                .codeRegistry(codeRegistry)
                .build();


        // then
        Assertions.assertTrue(schema.containsType("e1"));
        GraphQLFieldDefinition fde1 = (GraphQLFieldDefinition) schema.getType("e1").getChildren().get(0);
        Assertions.assertEquals("e1f1", fde1.getName());
        Assertions.assertEquals(Scalars.GraphQLString, fde1.getType());

        Assertions.assertTrue(schema.containsType("e2"));
        GraphQLFieldDefinition fde2 = (GraphQLFieldDefinition) schema.getType("e2").getChildren().get(0);
        Assertions.assertEquals("e2f1", fde2.getName());
        Assertions.assertEquals(Scalars.GraphQLString, fde2.getType());

        Assertions.assertTrue(schema.containsType("query"));
        GraphQLFieldDefinition fdq = (GraphQLFieldDefinition) schema.getType("query").getChildren().get(0);
        Assertions.assertEquals("f1", fdq.getName());
        Assertions.assertEquals(Scalars.GraphQLString, fdq.getType());

    }


    @Test
    public void nested_builders_fail() {

        // given
        GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject().name("query");
        queryBuilder.field(GraphQLFieldDefinition.newFieldDefinition().name("f1").type(Scalars.GraphQLString).build());
        GraphQLObjectType query = queryBuilder.build();

        final Map<String, GraphQLObjectType.Builder> entityBuilders = new HashMap<>();

        GraphQLObjectType.Builder eb1 = GraphQLObjectType.newObject().name("e1");
        eb1.field(GraphQLFieldDefinition.newFieldDefinition().name("e1f1").type(Scalars.GraphQLString).build());
        entityBuilders.put("e1", eb1);

        GraphQLObjectType.Builder builderUsingOtherBuilderInFieldDefinition = GraphQLObjectType.newObject().name("e2");
        builderUsingOtherBuilderInFieldDefinition.field(GraphQLFieldDefinition.newFieldDefinition().name("e2f1").type(eb1).build());
        entityBuilders.put("e2", builderUsingOtherBuilderInFieldDefinition);

        // when
        Set<GraphQLType> entityTypes = new HashSet<>();
        entityBuilders.forEach((k, b) -> entityTypes.add(b.build()));

        // then
        AssertException assertException = Assertions.assertThrowsExactly(AssertException.class, () -> {
            GraphQLSchema schema = GraphQLSchema.newSchema()
                    .query(query)
                    .additionalTypes(entityTypes)
//                .codeRegistry(codeRegistry)
                    .build();
        });
        Assertions.assertEquals("All types within a GraphQL schema must have unique names. No two provided types may have the same name.\n" +
                "No provided type may have a name which conflicts with any built in types (including Scalar and Introspection types).\n" +
                "You have redefined the type 'e1' from being a 'GraphQLObjectType' to a 'GraphQLObjectType'", assertException.getMessage());

    }

}