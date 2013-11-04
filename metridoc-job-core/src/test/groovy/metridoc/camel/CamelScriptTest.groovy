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

import metridoc.core.services.CamelService
import org.apache.camel.Exchange
import org.junit.Test

/**
 * Created with IntelliJ IDEA.
 * User: tbarker
 * Date: 12/29/12
 * Time: 12:57 PM
 * To change this template use File | Settings | File Templates.
 */
class CamelScriptTest {

    def ownerProperty = true

    @Test
    void "test some basic routing, default route called is direct:start"() {

        def calledMe = false
        def binding = new Binding()
        def camel = binding.includeService(CamelService)
        camel.addRoutes {
            from("direct:start").process {
                calledMe = true
            }
        }
        camel.send("direct:start")

        assert calledMe
    }

    @Test
    void "property from owner should exist in registry"() {
        def binding = new Binding()
        binding.variables.foo = "bar"
        def camel = binding.includeService(CamelService)
        camel.addRoutes {
            from("direct:start").process {Exchange exchange ->
                assert "bar" == exchange.context.registry.lookupByName("foo")
            }
        }
    }
}
