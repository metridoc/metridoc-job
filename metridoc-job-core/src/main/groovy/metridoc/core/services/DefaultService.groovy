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

import metridoc.core.MetridocScript

/**
 * @author Tommy Barker
 */
class DefaultService implements Service {

    Binding binding = new Binding()

    def getVariable(String variableName) {
        getVariable(variableName, null)
    }

    public <T> T getVariable(String variableName, Class<T> expectedType) {
        //command line goes first
        def value = null
        if (getBinding()) {
            if (getBinding().hasVariable("argsMap")) {
                value = getVariableHelper(getBinding().argsMap, variableName, expectedType)
            }
            if (value != null) return value

            value = getVariableHelper(getBinding().variables, variableName, expectedType)
            if (value != null) return value

            if (getBinding().hasVariable("config")) {
                value = getVariableHelper(getBinding().config, variableName, expectedType)
            }
        }

        return value
    }

    @SuppressWarnings(["GroovyAssignabilityCheck", "GroovyUnusedCatchParameter"])
    private static <T> T getVariableHelper(config, String variableName, Class<T> expectedType) {
        Map usedConfig = convertConfig(config)
        def value = usedConfig[variableName]

        if (value instanceof Map && value.isEmpty()) {
            return null
        }

        if (expectedType == null) {
            return value
        }
        else {
            if (value != null) {
                try {
                    return value.asType(expectedType)
                }
                catch (Throwable throwable) {
                    //do nothing, not compatible
                }
            }
        }

        return null
    }

    private static Map convertConfig(config) {
        if (config instanceof ConfigObject) {
            return config.flatten()
        }
        if (config instanceof Map) {
            return config
        }
        if (config instanceof Binding) {
            return config.variables
        }

        return [:]
    }

    /**
     * @deprecated
     * @param targetInfo
     * @param closure
     */
    void target(LinkedHashMap targetInfo, Closure closure) {
        step(targetInfo, closure)
    }

    void step(LinkedHashMap stepInfo, Closure closure) {
        getBinding().step(stepInfo, closure)
    }

    void step(LinkedHashMap stepInfo) {
        def stepName = MetridocScript.getStepName(stepInfo)
        if (this.metaClass.respondsTo(this, stepName)) {
            getBinding().step(stepInfo, this.&"$stepName")
        }
        else {
            getBinding().step(stepInfo)
        }
    }

    public <T> T includeService(Class<T> serviceClass) {
        return getBinding().includeService(serviceClass)
    }

    /**
     * @deprecated
     * @param tool
     * @return
     */
    public <T> T includeTool(Class<T> tool) {
        includeService(tool)
    }

    public <T> T includeService(LinkedHashMap args, Class<T> serviceClass) {
        return getBinding().includeService(args, serviceClass)
    }

    /**
     * @deprecated
     * @param args
     * @param tool
     * @return
     */
    public <T> T includeTool(LinkedHashMap args, Class<T> tool) {
        includeService(args, tool)
    }

    /**
     * @deprecated
     * @param targets
     */
    void includeTargets(Class<Script> targets) {
        includeSteps(targets)
    }

    void includeSteps(Class<Script> steps) {
        getBinding().includeSteps(steps)
    }

    void depends(String... targetNames) {
        getBinding().depends(targetNames)
    }

    void profile(String description, Closure work) {
        getBinding().profile(description, work)
    }

    void setDefaultStep(String step) {
        binding.defaultStep = step
    }

    void runStep(String step) {
        binding.runStep(step)
    }

    void runSteps(String... steps) {
        binding.runSteps(steps)
    }

    void runDefaultStep() {
        binding.runDefaultStep()
    }
}
