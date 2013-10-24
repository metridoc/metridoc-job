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



package metridoc.core

import camelscript.CamelGLite
import camelscript.ResponseException
import metridoc.core.tools.CamelTool
import metridoc.core.tools.RunnableTool
import org.apache.camel.Exchange

import java.util.concurrent.Future

/**
 * Created with IntelliJ IDEA on 6/6/13
 * @author Tommy Barker
 *
 * A helpful class that wraps around the scripting and routing libraries.  Generally extended by Grails services
 */
abstract class MetridocJob extends RunnableTool {

    /**
     * specific to grails.  Makes sure that all extensions are of prototype scope
     */
    static scope = "prototype"
    def grailsApplication

    CamelTool getCamelTool() {
        def result = getVariable("camelTool", CamelTool)
        if (result == null) {
            result = includeTool(CamelTool)
        }

        return result
    }

    @Override
    void setBinding(Binding binding) {
        super.setBinding(binding)
        includeTool(CamelTool)
    }

    @Override
    def execute() {
        def result = super.execute()
        camelTool.close()
        return result
    }

    /**
     * if using the grails job-runner plugin, this will be called
     * without any inject of command line arguments.  The framework
     * will use its own injection
     *
     * @param argsMap
     * @return
     */
    def execute(Map argsMap) {
        binding.argsMap = argsMap
        if (grailsApplication?.config) {
            binding.config = grailsApplication.config
        }
        def manager = binding.getManager()
        manager.handlePropertyInjection(this)
        execute()
    }

    CamelGLite bind(object) {
        camelTool.bind(object)
    }

    CamelGLite bind(String name, object) {
        camelTool.bind(name, object)
    }

    CamelGLite consume(String endpoint, Closure closure) {
        camelTool.consume(endpoint, closure)
    }

    void consumeForever(String endpoint, Closure closure) {
        camelTool.consumeForever(endpoint, closure)
    }

    void consumeForever(String endpoint, long wait, Closure closure) {
        camelTool.consumeForever(endpoint, wait, closure)
    }

    CamelGLite consumeNoWait(String endpoint, Closure closure) {
        camelTool.consumeNoWait(endpoint, closure)
    }

    void consumeWait(String endpoint, long wait, Closure closure) {
        camelTool.consumeWait(endpoint, wait, closure)
    }

    void consumeTillDone(String endpoint, long wait = 5000L, Closure closure) {
        camelTool.consumeTillDone(endpoint, wait, closure)
    }

    public <T> T convertTo(Class<T> convertion, valueToConvert) {
        camelTool.convertTo(convertion, valueToConvert)
    }

    Exchange send(String endpoint, body) throws ResponseException {
        camelTool.send(endpoint, body)
    }

    Exchange send(String endpoint, body, Map headers) throws ResponseException {
        camelTool.send(endpoint, body, headers)
    }

    Future<Exchange> asyncSend(String endpoint, body) {
        camelTool.asyncSend(endpoint, body)
    }

    Future<Exchange> asyncSend(String endpoint, body, Map headers) {
        camelTool.asyncSend(endpoint, body, headers)
    }
}
