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

import org.apache.commons.lang.StringUtils

/**
 * @author Tommy Barker
 */
abstract class RunnableService extends DefaultService {
    private hasRun = false

    /**
     * @deprecated
     * @param step
     */
    void setDefaultTarget(String step) {
        setDefaultStep(step)
    }

    def execute() {

        synchronized (this) {
            if (hasRun) {
                throw new ServiceException("${this.getClass().simpleName} has already run, and can only run once")
            }
            hasRun = true
        }

        def parseTool = includeService(ParseArgsService)
        //in case args was set after this was initialized
        parseTool.setBinding(binding)

        def thisToolName = StringUtils.uncapitalize(this.getClass().simpleName)
        if (!binding.hasVariable(thisToolName)) {
            binding.setVariable(thisToolName, this)
        }
        //redo injection in case properties were set after including the tool
        def manager = binding.manager
        manager.handlePropertyInjection(this)
        configure()
        String step = getVariable("target", String)
        step = getVariable("step", String) ?: step
        if (step) {
            setDefaultStep(step)
        }
        String defaultTarget = manager.defaultStep
        if (manager.stepMap.containsKey(defaultTarget)) {
            manager.runDefaultStep()
        }
    }

    abstract configure()
}
