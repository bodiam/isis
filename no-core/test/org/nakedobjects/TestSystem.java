package org.nakedobjects;

import org.nakedobjects.container.configuration.TestConfiguration;
import org.nakedobjects.object.NakedObject;
import org.nakedobjects.object.NakedObjectLoader;
import org.nakedobjects.object.NakedObjectSpecification;
import org.nakedobjects.object.defaults.DummyObjectFactory;
import org.nakedobjects.object.defaults.IdentityAdapterMapImpl;
import org.nakedobjects.object.defaults.MockNakedObjectSpecificationLoader;
import org.nakedobjects.object.defaults.MockObjectManager;
import org.nakedobjects.object.defaults.MockReflectionFactory;
import org.nakedobjects.object.defaults.ObjectLoaderImpl;
import org.nakedobjects.object.defaults.PojoAdapterHashImpl;
import org.nakedobjects.object.persistence.NakedObjectManager;
import org.nakedobjects.object.persistence.defaults.LocalObjectManager;
import org.nakedobjects.object.reflect.DummyReflectorFactory;


public class TestSystem {
    private MockNakedObjectSpecificationLoader specificationLoader;
    private NakedObjectLoader objectLoader;
    private NakedObjectsClient nakedObjects;
    private NakedObjectManager objectManager;

    public TestSystem() {
        specificationLoader = new MockNakedObjectSpecificationLoader();

        ObjectLoaderImpl objectLoader = new ObjectLoaderImpl();
        objectLoader.setPojoAdapterMap(new PojoAdapterHashImpl());
        objectLoader.setIdentityAdapterMap(new IdentityAdapterMapImpl());
        objectLoader.setObjectFactory(new DummyObjectFactory());
        
        this.objectLoader = objectLoader;
        
        objectManager = new MockObjectManager();
    }
    
    public void init() {
        nakedObjects = new NakedObjectsClient();
        
        nakedObjects.setConfiguration(new TestConfiguration());
        nakedObjects.setSpecificationLoader(specificationLoader);
        nakedObjects.setObjectLoader(objectLoader);
        nakedObjects.setObjectManager(objectManager);        
        nakedObjects.setReflectorFactory(new DummyReflectorFactory());
        nakedObjects.setReflectionFactory(new MockReflectionFactory());

        nakedObjects.init();
    }

    
    public void setObjectLoader(NakedObjectLoader objectLoader) {
        this.objectLoader = objectLoader;
    }
    
    public void setSpecificationLoader(MockNakedObjectSpecificationLoader specificationLoader) {
        this.specificationLoader = specificationLoader;
    }
    
    public void addSpecification(NakedObjectSpecification specification) {
        specificationLoader.addSpecification(specification);
    }

    public NakedObject createAdapterForTransient(Object associate) {
        return objectLoader.createAdapterForTransient(associate);
    }

    public void shutdown() {
       NakedObjects.shutdown();
    }

    public void setObjectManager(LocalObjectManager objectManager) {
        this.objectManager = objectManager;
    }
}

/*
 * Naked Objects - a framework that exposes behaviourally complete business objects directly to the user. Copyright (C) 2000 -
 * 2005 Naked Objects Group Ltd
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * The authors can be contacted via www.nakedobjects.org (the registered address of Naked Objects Group is Kingsway House, 123
 * Goldworth Road, Woking GU21 1NR, UK).
 */