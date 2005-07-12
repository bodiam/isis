package org.nakedobjects.object.persistence.defaults;

import org.nakedobjects.NakedObjects;
import org.nakedobjects.object.NakedClass;
import org.nakedobjects.object.NakedCollection;
import org.nakedobjects.object.NakedObject;
import org.nakedobjects.object.NakedObjectSpecification;
import org.nakedobjects.object.persistence.CreateObjectCommand;
import org.nakedobjects.object.persistence.DestroyObjectCommand;
import org.nakedobjects.object.persistence.InstancesCriteria;
import org.nakedobjects.object.persistence.NakedObjectStore;
import org.nakedobjects.object.persistence.ObjectNotFoundException;
import org.nakedobjects.object.persistence.ObjectStoreException;
import org.nakedobjects.object.persistence.Oid;
import org.nakedobjects.object.persistence.PersistenceCommand;
import org.nakedobjects.object.persistence.SaveObjectCommand;
import org.nakedobjects.object.persistence.UnsupportedFindException;
import org.nakedobjects.object.reflect.NakedObjectField;
import org.nakedobjects.utility.Debug;
import org.nakedobjects.utility.DebugString;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Category;

/**
 * This object store keep all objects in memory and simply provides a 
 * index of instances for each particular type.  This store does not exhibit the same
 * behaviour as real object stores that persist the objects' data.  For a more releastic store 
 * use the Memory Object Store
 * 
 * @see org.nakedobjects.object.persistence.defaults.MemoryObjectStore
 */
public class TransientObjectStore implements NakedObjectStore {
    private final static Category LOG = Category.getInstance(TransientObjectStore.class);
    protected final Hashtable objects;
    protected final Hashtable instances;

    public TransientObjectStore() {
        LOG.info("Creating object store");
        instances = new Hashtable();
        objects = new Hashtable();
    }

    public void abortTransaction() {
        LOG.debug("transaction aborted");
    }

    public CreateObjectCommand createCreateObjectCommand(final NakedObject object) {
        return new CreateObjectCommand() {
            public void execute() throws ObjectStoreException {
                LOG.debug("  create object " + object);
                NakedObjectSpecification specification = object.getSpecification();
                LOG.debug("   saving object " + object + " as instance of " + specification.getFullName());
                TransientObjectStoreInstances ins = instancesFor(specification);
                ins.add(object);
                objects.put(object.getOid(), object);
            }

            public NakedObject onObject() {
                return object;
            }

            public String toString() {
                return "CreateObjectCommand [object=" + object + "]";
            }
        };
    }

    public DestroyObjectCommand createDestroyObjectCommand(final NakedObject object) {
        return new DestroyObjectCommand() {
            public void execute() throws ObjectStoreException {
                LOG.info("  delete object '" + object + "'");
                objects.remove(object.getOid());
                
                NakedObjectSpecification specification = object.getSpecification();
                LOG.debug("   destroy object " + object + " as instance of " + specification.getFullName());
                TransientObjectStoreInstances ins = instancesFor(specification);
                ins.remove(object.getOid());
                
                NakedObjects.getObjectLoader().unloaded(object);
            }

            public NakedObject onObject() {
                return object;
            }

            public String toString() {
                return "DestroyObjectCommand [object=" + object + "]";
            }
        };
    }

    public SaveObjectCommand createSaveObjectCommand(final NakedObject object) {
        return new SaveObjectCommand() {
            public void execute() throws ObjectStoreException {
                NakedObjectSpecification specification = object.getSpecification();
                LOG.debug("   saving object " + object + " as instance of " + specification.getFullName());
                TransientObjectStoreInstances ins = instancesFor(specification);
                ins.save(object);
            }

            public NakedObject onObject() {
                return object;
            }

            public String toString() {
                return "SaveObjectCommand [object=" + object + "]";
            }
        };
    }

    private String debugCollectionGraph(NakedCollection collection, String name, int level, Vector recursiveElements) {
        StringBuffer s = new StringBuffer();

        if (recursiveElements.contains(collection)) {
            s.append("*\n");
        } else {
            recursiveElements.addElement(collection);

            Enumeration e = ((NakedCollection) collection).elements();

            while (e.hasMoreElements()) {
                indent(s, level);

                NakedObject element;
                try {
                    element = ((NakedObject) e.nextElement());
                } catch (ClassCastException ex) {
                    LOG.error(ex);
                    return s.toString();
                }

                s.append(element);
                s.append(debugGraph(element, name, level + 1, recursiveElements));
            }
        }

        return s.toString();
    }

    private String debugGraph(NakedObject object, String name, int level, Vector recursiveElements) {
        if (level > 3) {
            return "...\n"; // only go 3 levels?
        }

        if (recursiveElements == null) {
            recursiveElements = new Vector(25, 10);
        }

        if (object instanceof NakedCollection) {
            return "\n" + debugCollectionGraph((NakedCollection) object, name, level, recursiveElements);
        } else {
            return "\n" + debugObjectGraph(object, name, level, recursiveElements);
        }
    }

    private String debugObjectGraph(NakedObject object, String name, int level, Vector recursiveElements) {
        StringBuffer s = new StringBuffer();

        recursiveElements.addElement(object);

        // work through all its fields
        NakedObjectField[] fields;

        fields = object.getSpecification().getFields();

        for (int i = 0; i < fields.length; i++) {
            NakedObjectField field = fields[i];
            Object obj = object.getField(field);

            name = field.getName();
            indent(s, level);

            if (field.isCollection()) {
                s.append(name + ": \n" + debugCollectionGraph((NakedCollection) obj, "nnn", level + 1, recursiveElements));
            } else {
                if (obj instanceof NakedObject) {
                    if (recursiveElements.contains(obj)) {
                        s.append(name + ": " + obj + "*\n");
                    } else {
                        s.append(name + ": " + obj);
                        s.append(debugGraph((NakedObject) obj, name, level + 1, recursiveElements));
                    }
                } else {
                    s.append(name + ": " + obj);
                    s.append("\n");
                }
            }
        }

        return s.toString();
    }

    public void endTransaction() {
        LOG.debug("end transaction");
    }

    protected void finalize() throws Throwable {
        super.finalize();
        LOG.info("finalizing object store");
    }

    public String getDebugData() {
        DebugString debug = new DebugString();
        debug.appendTitle("Business Objects");
        Enumeration e = instances.keys();
        while (e.hasMoreElements()) {
            NakedObjectSpecification spec = (NakedObjectSpecification) e.nextElement();
            debug.appendln(0, spec.getFullName());
            TransientObjectStoreInstances instances = instancesFor(spec);
            Vector v = new Vector();
            instances.instances(v);
            Enumeration f = v.elements();
            if (!f.hasMoreElements()) {
                debug.appendln(8, "no instances");
            }
            while (f.hasMoreElements()) {
                debug.appendln(8, objects.get(f.nextElement()).toString());
            }
        }
        debug.appendln();

        debug.appendTitle("Object graphs");
        Vector dump = new Vector();
        e = instances.keys();
        while (e.hasMoreElements()) {
            NakedObjectSpecification spec = (NakedObjectSpecification) e.nextElement();
            TransientObjectStoreInstances instances = instancesFor(spec);
            Vector v = new Vector();
            instances.instances(v);
            Enumeration f = v.elements();
            while (f.hasMoreElements()) {
                debug.append(spec.getFullName());
                debug.append(": ");
                Oid oid = (Oid) f.nextElement();
                NakedObject object = (NakedObject) objects.get(oid);
                debug.append(object);
                debug.appendln(debugGraph(object, "name???", 0, dump));
            }
        }
        return debug.toString();
    }

    public String getDebugTitle() {
        return name();
    }

    public NakedObject[] getInstances(InstancesCriteria criteria) throws ObjectStoreException, UnsupportedFindException {
        Vector allInstances = new Vector();
        getInstances(criteria, allInstances);
        NakedObject[] matchedInstances = new NakedObject[allInstances.size()];
        int matches = 0;
        for (int i = 0; i < allInstances.size(); i++) {
            Oid oid = (Oid) allInstances.elementAt(i);
            NakedObject object = (NakedObject) objects.get(oid);
            if (criteria.matches(object)) {
                matchedInstances[matches++] = object;
            }
        }
        
        NakedObject[] ins = new  NakedObject[matches];
        System.arraycopy(matchedInstances, 0, ins, 0, matches);
        return ins;
    }

    private void getInstances(InstancesCriteria criteria, Vector instances) {
        NakedObjectSpecification spec = criteria.getSpecification();
        instancesFor(spec).instances(instances);
        if (criteria.includeSubclasses()) {
            NakedObjectSpecification[] subclasses = spec.subclasses();
            for (int i = 0; i < subclasses.length; i++) {
                getInstances(subclasses[i], instances, true);
            }
        }
    }

    public NakedObject[] getInstances(NakedObjectSpecification spec, boolean includeSubclasses) {
        LOG.debug("get instances" + (includeSubclasses ? " (included subclasses)" : ""));
        Vector instances = new Vector();
        getInstances(spec, instances, includeSubclasses);
        NakedObject[] ins = new NakedObject[instances.size()];
        for (int i = 0; i < ins.length; i++) {
            Oid oid = (Oid) instances.elementAt(i);
            ins[i] = (NakedObject) objects.get(oid);
        }
        return ins;
    }

    private void getInstances(NakedObjectSpecification spec, Vector instances, boolean includeSubclasses) {
        instancesFor(spec).instances(instances);
        if (includeSubclasses) {
            NakedObjectSpecification[] subclasses = spec.subclasses();
            for (int i = 0; i < subclasses.length; i++) {
                getInstances(subclasses[i], instances, true);
            }
        }
    }

    public NakedClass getNakedClass(String name) throws ObjectNotFoundException, ObjectStoreException {
        throw new ObjectNotFoundException();
    }

    public NakedObject getObject(Oid oid, NakedObjectSpecification hint) throws ObjectNotFoundException, ObjectStoreException {
        LOG.debug("getObject " + oid);
        NakedObject nakedObject = (NakedObject) objects.get(oid);
        if(nakedObject == null) {
            throw new ObjectNotFoundException(oid);            
        }
        return nakedObject;
    }

    public boolean hasInstances(NakedObjectSpecification spec, boolean includeSubclasses) {
        if (instancesFor(spec).hasInstances()) {
            return true;
        }
        if (includeSubclasses) {
            NakedObjectSpecification[] subclasses = spec.subclasses();
            for (int i = 0; i < subclasses.length; i++) {
                if (hasInstances(subclasses[i], includeSubclasses)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void indent(StringBuffer s, int level) {
        for (int indent = 0; indent < level; indent++) {
            s.append(Debug.indentString(4) + "|");
        }

        s.append(Debug.indentString(4) + "+--");
    }

    public void init() throws ObjectStoreException {
        LOG.info("init");
    }

    private TransientObjectStoreInstances instancesFor(NakedObjectSpecification spec) {
		TransientObjectStoreInstances ins = (TransientObjectStoreInstances) instances.get(spec);
		if (ins == null) {
			ins = new TransientObjectStoreInstances();
			instances.put(spec, ins);
		}
        return ins;
    }

    public String name() {
        return "Transient Object Store";
    }

    public int numberOfInstances(NakedObjectSpecification spec, boolean includeSubclasses) {
        int numberOfInstances = instancesFor(spec).numberOfInstances();
        if (includeSubclasses) {
            NakedObjectSpecification[] subclasses = spec.subclasses();
            for (int i = 0; i < subclasses.length; i++) {
                numberOfInstances += numberOfInstances(subclasses[i], true);
            }
        }
        return numberOfInstances;
    }

    public void resolveEagerly(NakedObject object, NakedObjectField field) throws ObjectStoreException {}

    public void reset() {
    }
    
    public void runTransaction(PersistenceCommand[] commands) throws ObjectStoreException {
        LOG.info("start execution of transaction ");
        for (int i = 0; i < commands.length; i++) {
            commands[i].execute();
        }
        LOG.info("end execution");
    }

    public void resolveImmediately(NakedObject object) throws ObjectStoreException {
        LOG.debug("resolve " + object);
    }

    public void shutdown() throws ObjectStoreException {
        LOG.info("shutdown " + this);
        objects.clear();
        for (Enumeration e = instances.elements(); e.hasMoreElements();) {
            TransientObjectStoreInstances inst = (TransientObjectStoreInstances) e.nextElement();
            inst.shutdown();
        }
        instances.clear();
    }

    public void startTransaction() {
        LOG.debug("start transaction");
    }

}

/*
 * Naked Objects - a framework that exposes behaviourally complete business
 * objects directly to the user. Copyright (C) 2000 - 2005 Naked Objects Group
 * Ltd
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * The authors can be contacted via www.nakedobjects.org (the registered address
 * of Naked Objects Group is Kingsway House, 123 Goldworth Road, Woking GU21
 * 1NR, UK).
 */

