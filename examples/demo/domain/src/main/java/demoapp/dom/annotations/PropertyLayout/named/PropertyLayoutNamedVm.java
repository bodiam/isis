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
package demoapp.dom.annotations.PropertyLayout.named;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.isis.applib.annotation.DomainObject;
import org.apache.isis.applib.annotation.Editing;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.Nature;
import org.apache.isis.applib.annotation.Optionality;
import org.apache.isis.applib.annotation.Property;
import org.apache.isis.applib.annotation.PropertyLayout;
import org.apache.isis.applib.annotation.Where;

import lombok.Getter;
import lombok.Setter;

import demoapp.dom._infra.asciidocdesc.HasAsciiDocDescription;

//tag::class[]
@XmlRootElement(name = "root")
@XmlType
@XmlAccessorType(XmlAccessType.FIELD)
@DomainObject(
        nature=Nature.VIEW_MODEL,
        objectType = "demo.PropertyLayoutNamedVm",
        editing = Editing.ENABLED
)
public class PropertyLayoutNamedVm implements HasAsciiDocDescription {

    public String title() {
        return "PropertyLayout#named";
    }

//tag::annotation[]
    @Property(optionality = Optionality.OPTIONAL)
    @PropertyLayout(
        named = "Named using @PropertyLayout"                // <.>
        , describedAs =
            "@PropertyLayout(named= \"...\")"
        , hidden = Where.ALL_TABLES
    )
    @MemberOrder(name = "properties", sequence = "1")
    @XmlElement(required = false)
    @Getter @Setter
    private String propertyUsingAnnotation;
//end::annotation[]

//tag::layout-file[]
    @Property(optionality = Optionality.OPTIONAL)
    @PropertyLayout(                                        // <.>
        describedAs =
            "<cpt:property id=\"...\">" +
                "<cpt:named>...</cpt:named>" +
            "</cpt:property>"
        , hidden = Where.ALL_TABLES
    )
    @MemberOrder(name = "properties", sequence = "2")
    @XmlElement(required = false)
    @Getter @Setter
    private String propertyUsingLayout;
//end::layout-file[]

//tag::meta-annotated[]
    @Property(optionality = Optionality.OPTIONAL)
    @NamedMetaAnnotation                            // <.>
    @PropertyLayout(
        describedAs = "@NamedMetaAnnotation"
        , hidden = Where.ALL_TABLES
    )
    @MemberOrder(name = "meta-annotated", sequence = "1")
    @XmlElement(required = false)
    @Getter @Setter
    private String propertyUsingMetaAnnotation;
//end::meta-annotated[]

//tag::meta-annotated-overridden[]
    @Property(optionality = Optionality.OPTIONAL)
    @NamedMetaAnnotation                                // <.>
    @PropertyLayout(
        named = "@PropertyLayout name " +
                "overrides meta-annotation"         // <.>
        , describedAs =
            "@NamedMetaAnnotation @PropertyLayout(...)"
        , hidden = Where.ALL_TABLES
    )
    @MemberOrder(name = "meta-annotated", sequence = "2")
    @XmlElement(required = false)
    @Getter @Setter
    private String propertyUsingMetaAnnotationButOverridden;
//end::meta-annotated-overridden[]

    //tag::markup[]
    @Property(optionality = Optionality.OPTIONAL)
    @PropertyLayout(
        named = "Named <b>uses</b> <i>markup</i>",          // <.>
        namedEscaped = false                                // <.>
        , describedAs =
            "@PropertyLayout(named= \"...\", namedEscaped=false)"
        , hidden = Where.ALL_TABLES
    )
    @MemberOrder(name = "markup", sequence = "1")
    @XmlElement(required = false)
    @Getter @Setter
    private String propertyUsingMarkup;
//end::markup[]

    //tag::markup-escaped[]
    @Property(optionality = Optionality.OPTIONAL)
    @PropertyLayout(
        named = "Named <b>but</b> <i>escaped</i>",          // <.>
        namedEscaped = true                                 // <.>
        , describedAs =
            "@PropertyLayout(" +
            "named = \"...\", namedEscaped=true)"
        , hidden = Where.ALL_TABLES
    )
    @MemberOrder(name = "markup", sequence = "2")
    @XmlElement(required = false)
    @Getter @Setter
    private String propertyUsingEscapedMarkup;
//end::markup-escaped[]


}
//end::class[]
