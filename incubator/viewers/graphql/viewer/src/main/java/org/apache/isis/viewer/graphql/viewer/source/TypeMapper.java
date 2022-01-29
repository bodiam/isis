package org.apache.isis.viewer.graphql.viewer.source;

import graphql.Scalars;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLOutputType;
import org.apache.isis.core.metamodel.facetapi.FeatureType;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.feature.ObjectAction;
import org.apache.isis.core.metamodel.spec.feature.ObjectActionParameter;

public class TypeMapper {

    public static GraphQLInputType inputTypeFor(final ObjectActionParameter objectActionParameter){
        ObjectSpecification elementType = objectActionParameter.getElementType();
        switch (elementType.getBeanSort()) {
            case VALUE:
                // TODO
            case COLLECTION:
                // TODO
            case ENTITY:
                // TODO
            case VIEW_MODEL:
                // TODO
            default:
                // for now
                return Scalars.GraphQLString;
        }

    }

    public static GraphQLOutputType outputTypeFor(final ObjectAction objectAction){
        ObjectSpecification returnType = objectAction.getReturnType();
        switch (returnType.getBeanSort()){
            case VALUE:
                // TODO
            case COLLECTION:
                // TODO
            case ENTITY:
                // TODO
            case VIEW_MODEL:
                // TODO
            default:
                // for now
                return Scalars.GraphQLString;
        }
    }

}
