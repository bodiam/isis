package org.apache.isis.viewer.graphql.viewer;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import org.apache.isis.viewer.graphql.model.IsisModuleIncViewerGraphqlModel;

@Configuration
@Import({
        // modules
        IsisModuleIncViewerGraphqlModel.class
})
public class IsisModuleIncViewerGraphqlViewer {
}

