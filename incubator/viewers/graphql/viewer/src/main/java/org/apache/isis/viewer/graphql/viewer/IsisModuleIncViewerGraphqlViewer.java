package org.apache.isis.viewer.graphql.viewer;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import org.apache.isis.viewer.graphql.model.IsisModuleIncViewerGraphqlModel;
import org.apache.isis.viewer.graphql.viewer.webmodule.WebModuleGraphql;

@Configuration
@Import({
        // @Service's
        WebModuleGraphql.class,

        // modules
        IsisModuleIncViewerGraphqlModel.class
})
public class IsisModuleIncViewerGraphqlViewer {
}

