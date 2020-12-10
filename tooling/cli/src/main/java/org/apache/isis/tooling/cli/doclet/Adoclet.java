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
package org.apache.isis.tooling.cli.doclet;

import java.io.File;
import java.util.stream.Stream;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import org.apache.isis.commons.collections.Can;
import org.apache.isis.tooling.javamodel.CompilationUnits;
import org.apache.isis.tooling.javamodel.Javadocs;
import org.apache.isis.tooling.javamodel.TypeDeclarations;
import org.apache.isis.tooling.model4adoc.AsciiDocFactory;
import org.apache.isis.tooling.model4adoc.AsciiDocWriter;

import lombok.NonNull;
import lombok.Value;
import lombok.val;
import lombok.extern.log4j.Log4j2;

@Value
@Log4j2
public class Adoclet {

    private final ClassOrInterfaceDeclaration td;

    public static Stream<Adoclet> parse(final @NonNull File sourceFile) {

        if("package-info.java".equals(sourceFile.getName())) {
            // ignore package files
            return Stream.empty();
        }
        
        try {
            
            // remove 'tag::' and 'end::' lines
            // remove '// <.>' foot note references
            val source = AdocIncludeTagFilter.read(sourceFile);

            val cu = StaticJavaParser.parse(source);
            
            return Stream.of(cu)
            .flatMap(CompilationUnits::streamPublicTypeDeclarations)
            .filter(Adoclets::hasIndexDirective)
            .map(Adoclet::new);

        } catch (Exception e) {
            log.error("failed to parse java source file {}", sourceFile, e);
            return Stream.empty();
        }

    }
    
    public String getName() {
        return td.getNameAsString();
    }

    public String getAsciiDocXref(
            final @NonNull AdocletContext docletContext) {
        val toAdocConverter = ToAsciiDoc.of(docletContext);
        return toAdocConverter.xref(this);
    }
    
    public String toAsciiDoc(
            final @NonNull AdocletContext docletContext) {
        
        val doc = AsciiDocFactory.doc();
        
        val introBlock = AsciiDocFactory.block(doc);
        val javaSourceBlock = AsciiDocFactory.block(doc);
        val methodDescriptionBlock = AsciiDocFactory.block(doc);
        
        val mds = TypeDeclarations.streamPublicMethodDeclarations(td)
                .filter(Javadocs::presentAndNotHidden)
                .collect(Can.toCan());
        
        val cds = TypeDeclarations.streamPublicConstructorDeclarations(td)
                .filter(Javadocs::presentAndNotHidden)
                .collect(Can.toCan());
        
        
        val toAdocConverter = ToAsciiDoc.of(docletContext);

        // -- intro
        
        td.getJavadoc().ifPresent(javadoc->{
            introBlock.setSource(toAdocConverter.javadoc(javadoc, 0));    
        });
        
        // -- java content
        
        if(docletContext.isIncludeJavaSource()) {
        
            val java = new StringBuilder();
            
            java.append(String.format("%s %s {\n", 
                    getDeclarationKeyword(), 
                    td.getName().asString()));
            
            
            cds.forEach(cd->{
                
                java.append(String.format("\n  %s // <.>\n", 
                        Adoclets.toNormalizedConstructorDeclaration(cd)));
                
            });
            
            mds.forEach(md->{
    
                java.append(String.format("\n  %s // <.>\n", 
                        Adoclets.toNormalizedMethodDeclaration(md)));
                
            });
    
            java.append("}\n");
            
            javaSourceBlock.setSource(
                    AsciiDocFactory.SourceFactory.java(java.toString(), td.getName().asString()));
        }
            
        // -- constructor and method descriptions
        
        val methodDescriptions = new StringBuilder();
        
        cds.forEach(cd->{
            
            cd.getJavadoc()
            .ifPresent(javadoc->{
                methodDescriptions.append(String.format(docletContext.getMemberDescriptionFormat(),
                        toAdocConverter.constructorDeclaration(cd),
                        toAdocConverter.javadoc(javadoc, 1)));
            });
            
        });
        
        mds.forEach(md->{
            
            md.getJavadoc()
            .ifPresent(javadoc->{
                methodDescriptions.append(String.format(docletContext.getMemberDescriptionFormat(),
                        toAdocConverter.methodDeclaration(md),
                        toAdocConverter.javadoc(javadoc, 1)));
            });
            
        });
        
        methodDescriptionBlock.setSource(methodDescriptions.toString());
        
        try {
            val title = String.format("%s : _%s_\n\n", 
                    td.getName().asString(),
                    getDeclarationKeyword());
            
            doc.setTitle(title);
            return AsciiDocWriter.toString(doc);
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
        
    }

    // -- HELPER

    private String getDeclarationKeyword() {
        return td.isInterface()
                ? "interface"
                : "class";
    }



}
