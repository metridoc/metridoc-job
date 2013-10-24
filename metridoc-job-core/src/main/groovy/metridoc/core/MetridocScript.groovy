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

import groovy.sql.Sql
import metridoc.core.services.ConfigService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.sql.DataSource

/**
 * Class to use if you are doing groovy scripting and want to add Metridoc functionality
 */
class MetridocScript {
    public static final String STEP_NOT_FOUND_IN_BINDING_OR_SCRIPT = "Could not find a corresponding method or closure for step"

    static StepManager getManager(Script self) {
        initializeTargetManagerIfNotThere(self.binding)
        self.stepManager
    }

    static StepManager getManager(Binding binding) {
        initializeTargetManagerIfNotThere(binding)
        binding.stepManager
    }

    private static initializeTargetManagerIfNotThere(Script script) {
        initializeTargetManagerIfNotThere(script.binding)
    }

    private static initializeTargetManagerIfNotThere(Binding binding) {
        if (!binding.hasVariable("stepManager")) {
            StepManager stepManager = new StepManager(binding: binding)
            binding.stepManager = stepManager
        }

        if (!binding.hasVariable("targetManager")) {
            binding.targetManager = binding.stepManager
        }
    }

    static ConfigObject configure(Script self) {
        //Need to make sure that we use the correct classloader, fixes issues with remote runs
        Thread.currentThread().setContextClassLoader(self.getClass().getClassLoader())
        includeService(self, ConfigService)
        return self.binding.config
    }

    static void step(Script self, LinkedHashMap description, Closure unitOfWork) {
        initializeTargetManagerIfNotThere(self)
        getManager(self).step(description, unitOfWork)
    }

    /**
     * @deprecated
     * @param self
     * @param description
     * @param unitOfWork
     */
    static void target(Script self, LinkedHashMap description, Closure unitOfWork) {
        step(self, description, unitOfWork)
    }

    static void step(Binding self, LinkedHashMap description, Closure unitOfWork) {
        initializeTargetManagerIfNotThere(self)
        getManager(self).step(description, unitOfWork)
    }

    /**
     * @deprecated
     * @param self
     * @param description
     * @param unitOfWork
     */
    static void target(Binding self, LinkedHashMap description, Closure unitOfWork) {
        step(self, description, unitOfWork)
    }

    static void includeSteps(Script self, Class<? extends Script> steps) {
        getManager(self).includeSteps(steps)
    }

    static void includeTargets(Script self, Class<? extends Script> steps) {
        includeSteps(self, steps)
    }

    /**
     * @deprecated
     * @param self
     * @param targets
     */
    static void includeTargets(Binding self, Class<Script> targets) {
        includeSteps(self, targets)
    }

    static void includeSteps(Binding self, Class<Script> steps) {
        getManager(self).includeSteps(steps)
    }

    static <T> T includeService(Script self, Class<T> tool) {
        getManager(self).includeService(tool)
    }

    /**
     * @deprecated
     * @param self
     * @param service
     * @return
     */
    static <T> T includeTool(Script self, Class<T> service) {
        getManager(self).includeService(service)
    }

    static <T> T includeService(Binding self, Class<T> service) {
        getManager(self).includeService(service)
    }

    /**
     * @deprecated
     * @param self
     * @param tool
     * @return
     */
    static <T> T includeTool(Binding self, Class<T> tool) {
        getManager(self).includeService(tool)
    }

    static <T> T includeService(Script self, LinkedHashMap args, Class<T> tool) {
        getManager(self).includeService(args, tool)
    }

    /**
     * @deprecated
     * @param self
     * @param args
     * @param tool
     * @return
     */
    static <T> T includeTool(Script self, LinkedHashMap args, Class<T> tool) {
        getManager(self).includeService(args, tool)
    }

    static <T> T includeService(Binding self, LinkedHashMap args, Class<T> tool) {
        getManager(self).includeService(args, tool)
    }

    /**
     * @deprecated
     * @param self
     * @param args
     * @param tool
     * @return
     */
    static <T> T includeTool(Binding self, LinkedHashMap args, Class<T> tool) {
        getManager(self).includeService(args, tool)
    }

    static void runDefaultStep(Script self) {
        getManager(self).runDefaultStep()
    }

    /**
     * @deprecated
     * @param self
     */
    static void runDefaultTarget(Script self) {
        runDefaultStep(self)
    }

    static void runDefaultStep(Binding self) {
        getManager(self).runDefaultStep()
    }

    /**
     * @deprecated
     * @param self
     */
    static void runDefaultTarget(Binding self) {
        runDefaultStep(self)
    }

    static void runSteps(Script self, String... steps) {
        getManager(self).depends(steps)
    }

    /**
     * @deprecated
     * @param self
     * @param targets
     */
    static void runTargets(Script self, String... targets) {
        runSteps(self, targets)
    }

    static void runSteps(Binding self, String... steps) {
        getManager(self).depends(steps)
    }

    /**
     * @deprecated
     * @param self
     * @param targets
     */
    static void runTargets(Binding self, String... targets) {
        runSteps(self, targets)
    }

    static void depends(Script self, String... targetDependencies) {
        getManager(self).depends(targetDependencies)
    }

    static void depends(Binding self, String... targetDependencies) {
        getManager(self).depends(targetDependencies)
    }

    static void profile(Script self, String description, Closure work) {
        getManager(self).profile(description, work)
    }

    static void profile(Binding self, String description, Closure work) {
        getManager(self).profile(description, work)
    }

    static void step(Script self, LinkedHashMap description) {
        String stepName = getStepName(description)

        def inScriptOrBinding = self.metaClass.respondsTo(self, stepName) ||
                inBinding(self, stepName)

        assert inScriptOrBinding: STEP_NOT_FOUND_IN_BINDING_OR_SCRIPT

        step(self, description, self.&"$stepName" as Closure)
    }

    static void step(Binding self, LinkedHashMap description) {
        String stepName = getStepName(description)

        assert inBinding(self, stepName): STEP_NOT_FOUND_IN_BINDING_OR_SCRIPT

        step(self, description, self."$stepName" as Closure)
    }

    protected static String getStepName(LinkedHashMap description) {
        String stepName = description.find { it.key != "depends" }.key
        assert stepName: "step name must NOT be null or empty"
        stepName
    }

    protected static boolean inBinding(Binding binding, String stepName) {
        binding.hasVariable(stepName) && binding."$stepName" instanceof Closure
    }

    protected static boolean inBinding(Script script, String stepName) {
        inBinding(script.binding, stepName)
    }

    static void setDefaultStep(Binding self, String step) {
        getManager(self).defaultStep = step
    }

    static void setDefaultStep(Script self, String step) {
        setDefaultStep(self.binding, step)
    }

    static void runStep(Binding self, String step) {
        getManager(self).depends(step)
    }

    static void runStep(Script self, String step) {
        runSteps(self.binding, step)
    }

    static Logger getLog(Script self) {
        getLog(self.binding)
    }

    static Logger getLog(Binding self) {
        if(self.hasVariable("log")) {
            return self.variables["log"] as Logger
        }

        LoggerFactory.getLogger("metridoc.script")
    }

    static DataSource getDataSource(Script self) {
        getDataSource(self.binding)
    }

    static DataSource getDataSource(Binding self) {
        if(self.hasVariable("dataSource")) {
            return self.getVariable("dataSource") as DataSource
        }

        return null
    }

    static Sql getSql(Script self) {
        getSql(self)
    }

    static Sql getSql(Binding self) {
        if(self.hasVariable("sql")) {
            return self.getVariable("sql") as Sql
        }

        def dataSource = getDataSource(self)
        if(dataSource) {
            return new Sql(dataSource)
        }

        return null
    }
}
