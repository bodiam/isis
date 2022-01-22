package org.apache.isis.viewer.graphql.viewer;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import org.apache.isis.viewer.graphql.model.IsisModuleIncViewerGraphqlModel;
import org.apache.isis.viewer.graphql.viewer.webmodule.GraphQlAutoConfiguration;
import org.apache.isis.viewer.graphql.viewer.webmodule.GraphQlCorsProperties;
import org.apache.isis.viewer.graphql.viewer.webmodule.GraphQlProperties;
import org.apache.isis.viewer.graphql.viewer.webmodule.WebModuleGraphql;

@Configuration
@Import({
        // @Service's
        WebModuleGraphql.class,

        // modules
        IsisModuleIncViewerGraphqlModel.class,

        // autoconfigurations
        GraphQlAutoConfiguration.class
})
@EnableConfigurationProperties({
        GraphQlProperties.class, GraphQlCorsProperties.class
})
@ComponentScan
public class IsisModuleIncViewerGraphqlViewer {
}

