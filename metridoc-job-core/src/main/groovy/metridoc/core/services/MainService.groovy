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

import groovy.util.logging.Slf4j

/**
 * @author Tommy Barker
 */
@Slf4j
class MainService extends RunnableService {

    Map<String, Class<? extends RunnableService>> runnableServices = [:]
    String defaultService

    /**
     * @deprecated
     * @param runnableTools
     */
    void setRunnableTools(Map<String, Class<? extends RunnableService>> runnableTools) {
        this.runnableServices = runnableTools
    }

    Map<String, Class<? extends RunnableService>> getRunnableTools() {
        return runnableServices
    }

    String getDefaultTool() {
        return defaultService
    }

    /**
     * @deprecated
     * @param defaultTool
     */
    void setDefaultTool(String defaultTool) {
        this.defaultService = defaultTool
    }

    public static void main(String[] args) {
        Binding binding = new Binding()
        binding.args = args
        def mainTool = new MainService(binding: binding)
        mainTool.execute()
    }

    @SuppressWarnings("GroovyVariableNotAssigned")
    @Override
    def configure() {
        assert runnableServices: "runnableServices cannot be null or empty"
        List params = getVariable("params") as List
        assert params || defaultService: "params cannot be null or empty, or a defaultTool must be specified"
        String toolToRun = params ? params[0] : defaultService
        log.info "running $toolToRun"
        assert runnableServices.containsKey(toolToRun): "[$toolToRun] does not exist"
        def tool = includeService(runnableServices[toolToRun])
        tool.execute()
    }
}
