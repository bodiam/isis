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
package org.apache.isis.security.keycloak;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.MappedJwtClaimSetConverter;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.Assert;

import org.apache.isis.core.config.IsisConfiguration;
import org.apache.isis.core.runtimeservices.IsisModuleCoreRuntimeServices;
import org.apache.isis.core.security.authentication.login.LoginSuccessHandlerUNUSED;
import org.apache.isis.core.webapp.IsisModuleCoreWebapp;
import org.apache.isis.security.keycloak.handler.LogoutHandlerForKeycloak;
import org.apache.isis.security.keycloak.services.KeycloakOauth2UserService;
import org.apache.isis.security.spring.IsisModuleSecuritySpring;

import lombok.RequiredArgsConstructor;
import lombok.val;

/**
 * Configuration Bean to support Isis Security using Keycloak.
 *
 * @since 2.0 {@index}
 */
@Configuration
@Import({
        // modules
        IsisModuleCoreRuntimeServices.class,
        IsisModuleCoreWebapp.class,

        // services
        LogoutHandlerForKeycloak.class,

        // builds on top of Spring
        IsisModuleSecuritySpring.class,

})
@EnableWebSecurity
public class IsisModuleSecurityKeycloak {


    @Bean
    public WebSecurityConfigurerAdapter webSecurityConfigurer(
            final IsisConfiguration isisConfiguration,
            final KeycloakOauth2UserService keycloakOidcUserService,
            final List<LoginSuccessHandlerUNUSED> loginSuccessHandlersUNUSED,
            final List<LogoutHandler> logoutHandlers
            ) {
        //val realm = isisConfiguration.getSecurity().getKeycloak().getRealm();
        return new KeycloakWebSecurityConfigurerAdapter(keycloakOidcUserService, logoutHandlers, isisConfiguration
        );
    }

//    @RequiredArgsConstructor
//    public static class AuthSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
//
//        final List<LoginSuccessHandler> loginSuccessHandlers;
//
//        @Override
//        public void onAuthenticationSuccess(
//                final HttpServletRequest request,
//                final HttpServletResponse response,
//                final Authentication authentication) throws ServletException, IOException {
//            super.onAuthenticationSuccess(request, response, authentication);
//            loginSuccessHandlers.forEach(LoginSuccessHandler::onSuccess);
//        }
//    }

    @Bean
    KeycloakOauth2UserService keycloakOidcUserService(final OAuth2ClientProperties oauth2ClientProperties) {

        val jwtDecoder = createNimbusJwtDecoder(
                oauth2ClientProperties.getProvider().get("keycloak").getJwkSetUri(),
                JwsAlgorithms.RS256);

        val authoritiesMapper = new SimpleAuthorityMapper();
        authoritiesMapper.setConvertToUpperCase(true);

        return new KeycloakOauth2UserService(jwtDecoder, authoritiesMapper);
    }

    @RequiredArgsConstructor
    public static class KeycloakWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

        private final KeycloakOauth2UserService keycloakOidcUserService;
        private final List<LogoutHandler> logoutHandlers;
        private final IsisConfiguration isisConfiguration;

        @Override
        public void configure(final HttpSecurity http) throws Exception {

            val successUrl = isisConfiguration.getSecurity().getKeycloak().getLoginSuccessUrl();
            val realm = isisConfiguration.getSecurity().getKeycloak().getRealm();
            val loginPage = OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI
                    + "/" + realm;

            val httpSecurityLogoutConfigurer =
                http
                    .sessionManagement()
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .and()

                    .authorizeRequests()
                        .anyRequest().authenticated()
                    .and()

                    // responsibility to propagate logout to Keycloak is performed by
                    // LogoutHandlerForKeycloak (called by Isis' LogoutMenu, not by Spring)
                    // this is to ensure that Isis can invalidate the http session eagerly and not preserve it in
                    // the SecurityContextPersistenceFilter (which uses http session to do its work)
                    .logout()
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"));

            logoutHandlers.forEach(httpSecurityLogoutConfigurer::addLogoutHandler);

            httpSecurityLogoutConfigurer
                    .and()

                    // This is the point where OAuth2 login of Spring 5 gets enabled
                    .oauth2Login()
                        .defaultSuccessUrl(successUrl, true)
//                            .successHandler(new AuthSuccessHandler(loginSuccessHandlers))
                        .successHandler(new SavedRequestAwareAuthenticationSuccessHandler())
                        .userInfoEndpoint()
                            .oidcUserService(keycloakOidcUserService)
                    .and()

                    .loginPage(loginPage);
        }
    }

    // -- HELPER

    private static NimbusJwtDecoder createNimbusJwtDecoder(final String jwkSetUrl, final String jwsAlgorithms) {
        Assert.hasText(jwkSetUrl, "jwkSetUrl cannot be empty");

        final OAuth2TokenValidator<Jwt> jwtValidator = JwtValidators.createDefault();
        final Converter<Map<String, Object>, Map<String, Object>> claimSetConverter =
                MappedJwtClaimSetConverter
                    .withDefaults(Collections.emptyMap());

        final NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(jwkSetUrl)
                .jwsAlgorithm(SignatureAlgorithm.from(jwsAlgorithms))
                .build();
        decoder.setClaimSetConverter(claimSetConverter);
        decoder.setJwtValidator(jwtValidator);
        return decoder;
    }


}

