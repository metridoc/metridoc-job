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



package metridoc.core.services

import org.junit.Test

class RunnableServiceTest {

    @Test
    void "dealing with bug where the runnable tool crashes since it is trying to deal with property injection incorrectly"() {
        def runnableTool = new RunnableService() {

            @Override
            def configure() {
                //do nothing
            }
        }

        runnableTool.execute()
    }

    @Test
    void "test a basic job withthe runnable tool"() {
        boolean fooRan = false
        def runnableTool = new RunnableService() {
            @Override
            def configure() {
                step(foo: "run foo") {
                    fooRan = true
                }

                setDefaultStep("foo")
            }
        }

        runnableTool.execute()

        assert fooRan
    }

    @Test
    void "setting the default target on the commanline will override the default target"() {
        boolean barRan = false
        boolean fooRan = false
        def runnableTool = new RunnableService() {

            @Override
            def configure() {
                step(foo: "run foo") {
                    fooRan = true
                }

                step(bar: "run bar") {
                    barRan = true
                }

                setDefaultStep("foo")
            }
        }

        def binding = runnableTool.binding
        binding.setVariable("args", ["-target=bar"] as String[])
        runnableTool.execute()
        assert barRan
        assert !fooRan
    }

    @Test
    void "a runnable tool can only run once"() {
        def runnableTool = new RunnableService() {

            @Override
            def configure() {
                //do nothing
            }
        }

        runnableTool.execute()
        try {
            runnableTool.execute()
            assert false: "exception should have occurred"
        }
        catch (ServiceException ignored) {
        }
    }

    @Test
    void "test from binding"() {
        def binding = new Binding()
        binding.includeService(ParseArgsService)
        binding.includeService(RunnableServiceMock).execute()
    }

    class RunnableServiceMock extends RunnableService {

        @Override
        def configure() {
            return null  //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}
