/*
 * Copyright 2013 Trustees of the University of Pennsylvania Licensed under the
 * 	Educational Community License, Version 2.0 (the "License"); you may
 * 	not use this file except in compliance with the License. You may
 * 	obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * 	Unless required by applicable law or agreed to in writing,
 * 	software distributed under the License is distributed on an "AS IS"
 * 	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * 	or implied. See the License for the specific language governing
 * 	permissions and limitations under the License.
 */



package metridoc.camel

import org.junit.Before
import org.junit.Test

/**
 * Created with IntelliJ IDEA.
 * User: tbarker
 * Date: 12/28/12
 * Time: 2:27 PM
 * To change this template use File | Settings | File Templates.
 */
class CamelScriptRegistryTest {

    def registry = new CamelScriptRegistry()

    @Before
    void setupRegistry() {
        def c = new SampleClosure(new SampleOwner())
        c.delegate = new SampleDelegate()
        registry.closure = c
    }

    @Test
    void "propertiesMap is built on owner properties"() {
        assert "bar" == registry.propertiesMap.foo
    }

    @Test
    void "propertieMap is built on delegate properties"() {
        assert "foobar" == registry.propertiesMap.bar
    }

    @Test
    void "by default owner properties override delegate properties"() {
        assert "bar" == registry.propertiesMap.foo
    }

    @Test
    void "if set to delegate first, the delegate properties override owner properties"() {
        registry.closure.resolveStrategy = Closure.DELEGATE_FIRST
        assert "foo" == registry.propertiesMap.foo
    }

    @Test
    void "null is returned if the value is not the right type"() {
        assert "bar" == registry.lookup("foo", String)
        assert null == registry.lookup("foo", Integer)
    }

    @Test
    void "owner only has no delegate properties"() {
        registry.closure.resolveStrategy = Closure.OWNER_ONLY
        assert null == registry.lookup("delegateOnlyProperty")
    }

    @Test
    void "check that property map returns correct values based on required type"() {
        assert 4 == registry.lookupByType(String).size()
        assert 0 == registry.lookupByType(Integer).size()
    }

    @Test
    void "closure must not be null"() {
        registry.closure = null
        try {
            registry.propertiesMap
        } catch (AssertionError error) {
        }
    }

    @Test
    void "test extracting data from a script binding"() {
        def scriptDelegate = new SampleScriptDelegate()
        scriptDelegate.run()
        registry.closure.delegate = scriptDelegate
        assert "fromScript" == registry.lookup("scriptProp")
    }

    @Test
    void "properties from delegate override are loaded AFTER the delegate"() {
        registry.delegateOverride = [delegateOnlyProperty: "fooOverride"]
        def value = registry.lookup("delegateOnlyProperty")
        assert "fooOverride" == value
    }
}

class SampleOwner {
    def foo = "bar"
    def ownerOnlyProperty = "owner"
}

class SampleDelegate {
    def foo = "foo"
    def bar = "foobar"
    def delegateOnlyProperty = "delegate"
}

class SampleScriptDelegate extends Script {

    @Override
    Object run() {
        scriptProp = "fromScript"
    }
}

class SampleClosure extends Closure {

    SampleClosure(Object owner) {
        super(owner)
    }
}