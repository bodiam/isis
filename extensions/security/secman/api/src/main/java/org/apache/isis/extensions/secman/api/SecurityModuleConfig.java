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
package org.apache.isis.extensions.secman.api;

import java.util.Set;
import java.util.stream.Stream;

import org.apache.isis.commons.internal.base._NullSafe;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;

@Builder
public class SecurityModuleConfig {

    // -- SCHEMA
    
    /** Logical DB schema to use for the SecMan entities. 
     * @implNote Yet just wishful thinking, not implemented. 
     */
    @Getter @Builder.Default @NonNull
    final String databaseSchema = "";
    
    // -- ROLES

    @Getter @Builder.Default @NonNull
    final String regularUserRoleName = "isis-module-security-regular-user";

    @Getter @Builder.Default @NonNull
    final String fixtureRoleName = "isis-module-security-fixtures";

    @Getter @Builder.Default @NonNull
    final String adminRoleName = "isis-module-security-admin";

    // -- ADMIN

    @Getter @Builder.Default @NonNull
    final String adminUserName = "isis-module-security-admin";

    @Getter @Builder.Default @NonNull
    final String adminPassword = "pass";

    /**
     * cannot be removed via user interface
     */
    @Getter @Builder.Default @NonNull
    final String[] adminStickyPackagePermissions = new String[]{
            "org.apache.isis.extensions.secman.api",
            "org.apache.isis.extensions.secman.model",
            "org.apache.isis.extensions.secman.jdo.dom", // either jdo or jpa is going to be orphaned
            "org.apache.isis.extensions.secman.jpa.dom", // either jdo or jpa is going to be orphaned
    };
    
    @Getter @Singular 
    final Set<String> adminAdditionalPackagePermissions;
    
    /**
     * Delegated users, on first successful logon, are auto-created but disabled (by default).
     * <p> 
     * This option allows to override this behavior, such that authenticated 
     * users are also auto-enabled.
     * <p>
     * default: false 
     *
     */
    @Getter @Builder.Default 
    final boolean autoEnableIfDelegatedAndAuthenticated = false;

    // -- UTILITIES
    
    public Stream<String> streamAdminPackagePermissions() {
        return Stream.concat(
                _NullSafe.stream(adminStickyPackagePermissions),
                _NullSafe.stream(adminAdditionalPackagePermissions));
    }
    
    public boolean isStickyAdminPackage(String featureFqn) {
        return _NullSafe.stream(adminStickyPackagePermissions)
        .anyMatch(stickyPackage->stickyPackage.equals(featureFqn));
    }


}
