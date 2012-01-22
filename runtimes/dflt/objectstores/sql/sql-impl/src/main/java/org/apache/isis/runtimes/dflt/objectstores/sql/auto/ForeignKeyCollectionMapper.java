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

package org.apache.isis.runtimes.dflt.objectstores.sql.auto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import org.apache.isis.core.commons.debug.DebugBuilder;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.adapter.ResolveState;
import org.apache.isis.core.metamodel.adapter.oid.Oid;
import org.apache.isis.core.metamodel.facets.collections.modify.CollectionFacet;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.feature.ObjectAssociation;
import org.apache.isis.runtimes.dflt.objectstores.sql.CollectionMapper;
import org.apache.isis.runtimes.dflt.objectstores.sql.DatabaseConnector;
import org.apache.isis.runtimes.dflt.objectstores.sql.FieldMappingLookup;
import org.apache.isis.runtimes.dflt.objectstores.sql.IdMapping;
import org.apache.isis.runtimes.dflt.objectstores.sql.IdMappingAbstract;
import org.apache.isis.runtimes.dflt.objectstores.sql.ObjectMapping;
import org.apache.isis.runtimes.dflt.objectstores.sql.ObjectMappingLookup;
import org.apache.isis.runtimes.dflt.objectstores.sql.Results;
import org.apache.isis.runtimes.dflt.objectstores.sql.Sql;
import org.apache.isis.runtimes.dflt.objectstores.sql.VersionMapping;
import org.apache.isis.runtimes.dflt.objectstores.sql.mapping.FieldMapping;
import org.apache.isis.runtimes.dflt.objectstores.sql.mapping.ObjectReferenceMapping;
import org.apache.isis.runtimes.dflt.runtime.persistence.PersistorUtil;

/**
 * Stores 1-to-many collections by creating a foreign-key column in the table
 * for the incoming objectAssociation class. This assumes this the class is only
 * ever in 1 collection parent.
 * 
 * @version $Rev$ $Date$
 */
public class ForeignKeyCollectionMapper extends AbstractAutoMapper implements CollectionMapper {
    private static final Logger LOG = Logger.getLogger(ForeignKeyCollectionMapper.class);
    private final ObjectAssociation field;
    private final IdMapping idMapping;
    private final VersionMapping versionMapping;
    private final ObjectReferenceMapping foreignKeyMapping;
    private String foreignKeyName;
    private String columnName;
    private final ObjectMappingLookup objectMapperLookup2;

    private ObjectMapping originalMapping = null;

    public ForeignKeyCollectionMapper(final ObjectAssociation objectAssociation, final String parameterBase, final FieldMappingLookup lookup, final ObjectMappingLookup objectMapperLookup) {
        super(objectAssociation.getSpecification().getFullIdentifier(), parameterBase, lookup, objectMapperLookup);

        this.field = objectAssociation;

        objectMapperLookup2 = objectMapperLookup;

        idMapping = lookup.createIdMapping();
        versionMapping = lookup.createVersionMapping();

        setColumnName(determineColumnName(objectAssociation));
        foreignKeyName = Sql.sqlName("fk_" + getColumnName());

        foreignKeyName = Sql.identifier(foreignKeyName);
        foreignKeyMapping = lookup.createMapping(columnName, specification);
    }

    protected ForeignKeyCollectionMapper(final FieldMappingLookup lookup, final AbstractAutoMapper abstractAutoMapper, final ObjectAssociation field) {
        super(lookup, abstractAutoMapper, field.getSpecification().getFullIdentifier());

        this.field = field;
        objectMapperLookup2 = null;

        idMapping = lookup.createIdMapping();
        versionMapping = lookup.createVersionMapping();

        setColumnName(determineColumnName(field));
        foreignKeyName = Sql.sqlName("fk_" + getColumnName());

        foreignKeyName = Sql.identifier(foreignKeyName);
        foreignKeyMapping = lookup.createMapping(columnName, specification);
    }

    protected String determineColumnName(final ObjectAssociation objectAssociation) {
        return objectAssociation.getSpecification().getShortIdentifier();
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(final String columnName) {
        this.columnName = columnName;
    }

    protected VersionMapping getVersionMapping() {
        return versionMapping;
    }

    protected ObjectReferenceMapping getForeignKeyMapping() {
        return foreignKeyMapping;
    }

    protected String getForeignKeyName() {
        return foreignKeyName;
    }

    @Override
    public void startup(final DatabaseConnector connector, final FieldMappingLookup lookup) {
        if (originalMapping == null) {
            originalMapping = objectMapperLookup.getMapping(specification, null);
        }
        originalMapping.startup(connector, objectMapperLookup2);
        super.startup(connector, lookup);
    }

    @Override
    public boolean needsTables(final DatabaseConnector connection) {
        return !connection.hasColumn(table, foreignKeyName);
    }

    @Override
    public void createTables(final DatabaseConnector connection) {
        if (connection.hasTable(table)) {
            final StringBuffer sql = new StringBuffer();
            sql.append("alter table ");
            sql.append(table);
            sql.append(" add ");
            appendColumnDefinitions(sql);
            connection.update(sql.toString());
        } else {
            final StringBuffer sql = new StringBuffer();
            sql.append("create table ");
            sql.append(table);
            sql.append(" (");
            idMapping.appendCreateColumnDefinitions(sql);
            sql.append(", ");

            appendColumnDefinitions(sql);

            // for (final FieldMapping mapping : fieldMappings) {
            // mapping.appendColumnDefinitions(sql);
            // sql.append(",");
            // }
            // sql.append(versionMapping.appendColumnDefinitions());
            sql.append(")");
            connection.update(sql.toString());
        }
    }

    public IdMappingAbstract getIdMapping() {
        return idMapping;
    }

    protected void appendCollectionUpdateColumnsToNull(final StringBuffer sql) {
        sql.append(foreignKeyName + "=NULL ");
    }

    protected void appendCollectionWhereValues(final DatabaseConnector connector, final ObjectAdapter parent, final StringBuffer sql) {
        foreignKeyMapping.appendUpdateValues(connector, sql, parent);
    }

    protected void appendCollectionUpdateValues(final DatabaseConnector connector, final ObjectAdapter parent, final StringBuffer sql) {
        appendCollectionWhereValues(connector, parent, sql);
    }

    protected void appendColumnDefinitions(final StringBuffer sql) {
        foreignKeyMapping.appendColumnDefinitions(sql);
    }

    @Override
    public void loadInternalCollection(final DatabaseConnector connector, final ObjectAdapter parent, final boolean makeResolved) {

        final ObjectAdapter collection = field.get(parent);
        if (collection.getResolveState().canChangeTo(ResolveState.RESOLVING)) {
            LOG.debug("loading internal collection " + field);
            PersistorUtil.start(collection, ResolveState.RESOLVING);

            final List<ObjectAdapter> list = new ArrayList<ObjectAdapter>();

            loadCollectionIntoList(connector, parent, makeResolved, table, specification, getIdMapping(), fieldMappings, versionMapping, list);

            final CollectionFacet collectionFacet = collection.getSpecification().getFacet(CollectionFacet.class);
            collectionFacet.init(collection, list.toArray(new ObjectAdapter[list.size()]));
            PersistorUtil.end(collection);

            // TODO: Need to finalise this behaviour. At the moment, all
            // collections will get infinitely resolved. I
            // don't think this is desirable. Sub-collections should be left
            // "Partially Resolved".
            if (makeResolved) {
                for (final ObjectAdapter field : list) {
                    // final ObjectMapping mapping =
                    // objectMappingLookup.getMapping(field, connector);
                    if (field.getSpecification().isOfType(parent.getSpecification())) {
                        loadInternalCollection(connector, field, true);
                    }
                }
            }
        }
    }

    protected void loadCollectionIntoList(final DatabaseConnector connector, final ObjectAdapter parent, final boolean makeResolved, final String table, final ObjectSpecification specification, final IdMappingAbstract idMappingAbstract, final List<FieldMapping> fieldMappings,
            final VersionMapping versionMapping, final List<ObjectAdapter> list) {

        final StringBuffer sql = new StringBuffer();
        sql.append("select ");
        idMappingAbstract.appendColumnNames(sql);

        sql.append(", ");
        final String columnList = columnList(fieldMappings);
        if (columnList.length() > 0) {
            sql.append(columnList);
            sql.append(", ");
        }
        sql.append(versionMapping.appendColumnNames());
        sql.append(" from ");
        sql.append(table);
        sql.append(" where ");
        appendCollectionWhereValues(connector, parent, sql);

        final Results rs = connector.select(sql.toString());
        while (rs.next()) {
            final Oid oid = idMappingAbstract.recreateOid(rs, specification);
            final ObjectAdapter element = getAdapter(specification, oid);
            loadFields(element, rs, makeResolved, fieldMappings);
            LOG.debug("  element  " + element.getOid());
            list.add(element);
        }
        rs.close();
    }

    protected void loadFields(final ObjectAdapter object, final Results rs, final boolean makeResolved, final List<FieldMapping> fieldMappings) {
        if (object.getResolveState().canChangeTo(ResolveState.RESOLVING)) {
            PersistorUtil.start(object, ResolveState.RESOLVING);
            for (final FieldMapping mapping : fieldMappings) {
                mapping.initializeField(object, rs);
            }
            object.setOptimisticLock(versionMapping.getLock(rs));
            if (makeResolved) {
                PersistorUtil.end(object);
            }
        }
    }

    /**
     * Override this in the Polymorphic case to return just the elements that
     * are appropriate for the subclass currently being handled.
     * 
     * @param collection
     * @return those elements that ought to be used.
     */
    protected Iterator<ObjectAdapter> getElementsForCollectionAsIterator(final ObjectAdapter collection) {
        final CollectionFacet collectionFacet = collection.getSpecification().getFacet(CollectionFacet.class);
        final Iterable<ObjectAdapter> elements = collectionFacet.iterable(collection);
        return elements.iterator();
    }

    @Override
    public void saveInternalCollection(final DatabaseConnector connector, final ObjectAdapter parent) {
        final ObjectAdapter collection = field.get(parent);
        LOG.debug("Saving internal collection " + collection);

        final Iterator<ObjectAdapter> elements = getElementsForCollectionAsIterator(collection);

        // TODO What is needed to allow a collection update (add/remove) to mark
        // the collection as dirty?
        // checkIfDirty(collection);

        if (elements.hasNext() == false) {
            return;
        }

        clearCollectionParent(connector, parent);

        resetCollectionParent(connector, parent, elements);
    }

    protected void clearCollectionParent(final DatabaseConnector connector, final ObjectAdapter parent) {
        // Delete collection parent
        final StringBuffer sql = new StringBuffer();
        sql.append("update ");
        sql.append(table);
        sql.append(" set ");
        appendCollectionUpdateColumnsToNull(sql);
        sql.append(" where ");
        appendCollectionWhereValues(connector, parent, sql);
        connector.update(sql.toString());
    }

    protected void resetCollectionParent(final DatabaseConnector connector, final ObjectAdapter parent, final Iterator<ObjectAdapter> elements) {
        // Reinstall collection parent
        final StringBuffer update = new StringBuffer();
        update.append("update ");
        update.append(table);
        update.append(" set ");
        appendCollectionUpdateValues(connector, parent, update);
        update.append(" where ");

        idMapping.appendColumnNames(update);

        update.append(" IN (");

        int count = 0;
        for (final Iterator<ObjectAdapter> iterator = elements; iterator.hasNext();) {
            final ObjectAdapter element = iterator.next();

            if (count++ > 0) {
                update.append(",");
            }
            idMapping.appendObjectId(connector, update, element.getOid());
        }
        update.append(")");
        if (count > 0) {
            connector.insert(update.toString());
        }
    }

    protected void checkIfDirty(final ObjectAdapter collection) {
        // Test: is dirty?
        final ObjectSpecification collectionSpecification = collection.getSpecification();
        if (collectionSpecification.isDirty(collection)) {
            LOG.debug(collection.getOid() + " is dirty");
        } else {
            LOG.debug(collection.getOid() + " is clean");
        }

        final CollectionFacet collectionFacetD = collection.getSpecification().getFacet(CollectionFacet.class);
        for (final ObjectAdapter element : collectionFacetD.iterable(collection)) {
            if (collectionSpecification.isDirty(element)) {
                LOG.debug(element.getOid() + " is dirty");
            } else {
                LOG.debug(element.getOid() + " is clean");
            }
        }
    }

    @Override
    public void debugData(final DebugBuilder debug) {
        debug.appendln(field.getName(), "collection");
        debug.indent();
        debug.appendln("Foreign key name", foreignKeyName);
        debug.appendln("Foreign key mapping", foreignKeyMapping);
        debug.appendln("ID mapping", idMapping);
        debug.appendln("Version mapping", versionMapping);
        debug.appendln("Original mapping", originalMapping);
        debug.unindent();
    }

}
