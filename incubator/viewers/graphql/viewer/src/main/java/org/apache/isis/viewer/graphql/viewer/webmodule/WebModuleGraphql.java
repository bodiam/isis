/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.isis.viewer.graphql.viewer.webmodule;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.graphql.web.WebGraphQlHandler;
import org.springframework.graphql.web.WebInput;
import org.springframework.graphql.web.WebOutput;
import org.springframework.graphql.web.webmvc.GraphQlHttpHandler;
import org.springframework.stereotype.Service;

import org.apache.isis.applib.annotation.PriorityPrecedence;
import org.apache.isis.applib.services.inject.ServiceInjector;
import org.apache.isis.commons.collections.Can;
import org.apache.isis.core.config.IsisConfiguration;
import org.apache.isis.core.webapp.modules.WebModuleAbstract;
import org.apache.isis.core.webapp.modules.WebModuleContext;

import lombok.Getter;
import lombok.val;

import reactor.core.publisher.Mono;

/**
 * WebModule that provides the GraphQL Viewer.
 *
 * @since 2.0 {@index}
 *
 * @implNote CDI feels responsible to resolve injection points for any Servlet or Filter
 * we register programmatically on the ServletContext.
 * As long as injection points are considered to be resolved by Spring, we can workaround this fact:
 * By replacing annotations {@code @Inject} with {@code @Autowire} for any Servlet or Filter,
 * that get contributed by a WebModule, these will be ignored by CDI.
 *
 */
@Service
@Named("isis.viewer.graphql.WebModuleGraphql")
@javax.annotation.Priority(PriorityPrecedence.MIDPOINT - 80)
@Qualifier("Graphql")
public final class WebModuleGraphql extends WebModuleAbstract {

    private final IsisConfiguration isisConfiguration;

    @Inject
    public WebModuleGraphql(
            final IsisConfiguration isisConfiguration,
            final ServiceInjector serviceInjector) {
        super(serviceInjector);
        this.isisConfiguration = isisConfiguration;
    }

    @Getter
    private final String name = "GraphQL";

    @Override
    public void prepare(WebModuleContext ctx) {
        super.prepare(ctx);
//        new GraphQlHttpHandler(new WebGraphQlHandler() {
//            @Override
//            public Mono<WebOutput> handleRequest(WebInput input) {
//                return null;
//            }
//        });


    }

    @Override
    public Can<ServletContextListener> init(ServletContext ctx) throws ServletException {
        return Can.empty(); // registers no listeners
    }


}
