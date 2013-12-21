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

import groovy.util.logging.Slf4j
import metridoc.core.services.ParseArgsService
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.time.StopWatch
import org.slf4j.LoggerFactory

import java.lang.reflect.Field
import java.lang.reflect.Method

@Slf4j
class StepManager {
    static final String DEFAULT_TARGET = "default"
    String defaultStep = DEFAULT_TARGET
    Map<String, Closure> stepMap = [:]
    Map<String, List> dependsMap = [:]
    Set<String> stepsRan = []
    private boolean _interrupted = false
    private Binding _binding
    List injectedServices = []

    void setDefaultTarget(String defaultTarget) {
        this.defaultStep = defaultTarget
    }

    /**
     * @deprecated
     * @param targetMap
     */
    void setTargetMap(Map<String, Closure> targetMap) {
        this.stepMap = targetMap
    }

    /**
     * @deprecated
     * @param targetsRan
     */
    void setTargetsRan(Set<String> targetsRan) {
        this.stepsRan = targetsRan
    }

    /**
     * @deprecated
     * @return
     */
    String getDefaultTarget() {
        return defaultStep
    }

    /**
     * @deprecated
     * @return
     */
    Map<String, Closure> getTargetMap() {
        return stepMap
    }

    /**
     * @deprecated
     * @return
     */
    Set<String> getTargetsRan() {
        return stepsRan
    }

    Binding getBinding() {
        if (_binding) return _binding

        _binding = new Binding()
        _binding.targetManager = this
        _binding.stepManager = this
        return _binding
    }

    void setBinding(Binding _binding) {
        this._binding = _binding
    }

    /**
     * If job is not run from the command line, use this to fire off an interuption.  This is not as
     * effective as killing a commandline job though.  Basically either the job will have to be aware of
     * the interuption or wait until it is checked in a progress closure
     * @return
     */
    void interrupt() {
        interrupted = true
        getBinding().interrupted = true
    }

    boolean getInterrupted() {
        def bindingInterrupted = binding.hasVariable("interrupted") ? binding.interrupted : false
        return _interrupted || bindingInterrupted
    }

    void setInterrupted(boolean interrupted) {
        this._interrupted = interrupted
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    def step(Map data, Closure closure) {
        closure.delegate = this //required for imported step
        def dependsList = data.remove("depends")
        assert data.size() == 1: "The target map has more variables than it should"
        def key = (data.keySet() as List<String>)[0]
        String description = data[key]
        def closureToRun

        if (dependsList) {
            closureToRun = {
                profile(description) {
                    if (dependsList instanceof String) {
                        depends(dependsList)
                    }
                    else {
                        depends(dependsList as String[])
                    }
                    closure.call()
                }
            }
        }
        else {
            closureToRun = {
                profile(description, closure)
            }
        }
        stepMap.put(key, closureToRun)
    }

    void addDepends(String targetName, List depends) {
        dependsMap[targetName] = depends
    }

    void addDepends(String targetName, String depends) {
        dependsMap[targetName] = [depends]
    }

    void addDepends(String targetName, String[] depends) {
        dependsMap[targetName] = depends as List
    }

    def target(Map data, Closure closure) {
        step(data, closure)
    }

    /**
     * fires off a target by name if it has not been run yet.  If it has run then it is skipped
     *
     * @param stepNames
     * @return
     */
    @SuppressWarnings("UnnecessaryQualifiedReference")
    def depends(String... stepNames) {
        stepNames.each { stepName ->
            Closure step = stepMap.get(stepName)
            assert step != null: "step $stepName does not exist"

            def stepHasNotBeenCalled = !stepsRan.contains(stepName)
            if (stepHasNotBeenCalled) {
                if (dependsMap.containsKey(stepName)) {
                    depends(dependsMap[stepName] as String[])
                }
                step.delegate = this
                step.resolveStrategy = Closure.DELEGATE_FIRST
                step.call()
                stepsRan.add(stepName)
            }
        }
    }

    /**
     * @deprecated
     * @param scriptClass
     * @return
     */
    def includeTargets(Class<? extends Script> scriptClass) {
        return includeSteps(scriptClass, binding)
    }

    /**
     * loads scripts that contains steps to allow for code reuse
     *
     * @param scriptClass
     * @return returns the binding from the script in case global variables need to accessed
     */
    def includeSteps(Class<? extends Script> scriptClass) {
        return includeSteps(scriptClass, binding)
    }

    /**
     * @deprecated
     * @param scriptClass
     * @param binding
     * @return
     */
    def includeTargets(Class<? extends Script> scriptClass, Binding binding) {
        includeSteps(scriptClass, binding)

    }

    /**
     * the same as {@link #includeSteps(Class)}, but a binding can be passed so more global variables can
     * be loaded
     *
     * @param scriptClass
     * @param binding
     * @return the passed binding
     */
    def includeSteps(Class<? extends Script> scriptClass, Binding binding) {
        binding.setVariable("target") { Map description, Closure closure ->
            step(description, closure)
        }

        binding.setVariable("step") { Map description, Closure closure ->
            step(description, closure)
        }
        Script script = scriptClass.newInstance()
        script.binding = binding
        script.run()

        return binding
    }

    /**
     * includes a raw target map.  Especially useful if you want to include info from another job
     *
     * @param targetMap
     */
    def includeTargets(Map<String, Closure> targetMap) {
        this.stepMap.putAll(targetMap)
    }

    /**
     * imports binding variables from another binding
     *
     * @param binding
     */
    def importBindingVariables(Binding binding) {
        this.binding.variables.putAll(binding.variables)
    }

    /**
     * profiles a chunk of code stating when it starts and finishes
     * @param description description of the chunk of code
     * @param closure the code to run
     */
    def profile(String description, Closure closure) {
        if (interrupted) {
            throw new JobInterruptionException(this.getClass().name)
        }
        def log = LoggerFactory.getLogger(StepManager)
        StopWatch stopWatch = new StopWatch()
        stopWatch.start()
        log.info "profiling [$description] start"
        closure.call()
        stopWatch.stop()
        log.info "profiling [$description] finished $stopWatch"
        if (interrupted) {
            throw new JobInterruptionException(this.getClass().name)
        }
    }

    def <T> T includeService(Class<T> serviceClass) {
        includeService([:] as LinkedHashMap, serviceClass)
    }

    /**
     * @deprecated
     * @param tool
     * @return
     */
    public <T> T includeTool(Class<T> tool) {
        includeService([:] as LinkedHashMap, tool)
    }

    def <T> T includeService(LinkedHashMap args, Class<T> serviceClass) {
        def serviceName = serviceClass.simpleName
        def serviceNameUsed = StringUtils.uncapitalize(serviceName)
        if (binding.hasVariable(serviceNameUsed)) {
            def log = LoggerFactory.getLogger(StepManager)
            log.debug "service $serviceNameUsed already exists"
        }
        else {
            def service = createService(args, serviceClass)
            if (!binding.hasVariable(serviceNameUsed)) {
                binding."$serviceNameUsed" = service
            }
            def servicesToCheck = new ArrayList(injectedServices)
            //now we can avoid concurrent modification issues
            servicesToCheck.each {
                def property = it.properties.find { it.key == serviceNameUsed }
                try {
                    if (property && it."$serviceNameUsed" == null) {
                        if (it.metaClass.respondsTo(it, "set${serviceName}", [service.getClass()] as Object[])) {
                            it."$serviceNameUsed" = service
                        }
                    }
                }
                catch (Throwable throwable) {
                    log.error "error occurred trying to inject $service into $it, skipping injection", throwable
                }
            }
            injectedServices << service
            addServiceSteps(service)
        }

        return binding."${serviceNameUsed}"
    }

    void addServiceSteps(service) {
        service.getClass().getMethods().each { Method method ->
            def stepAnnotation = method.getAnnotation(Step)
            if (stepAnnotation) {
                def data = [:]
                String name = stepAnnotation.name() ?: method.name
                data[name] = stepAnnotation.description()
                if (stepAnnotation.depends()) {
                    data.depends = stepAnnotation.depends() as List
                }
                def closure = service.&"$method.name"
                step(data, closure)
            }
        }
    }

    def <T> T createService(LinkedHashMap args, Class<T> serviceClass) {
        def instance = serviceClass.newInstance(args)
        if (instance.metaClass.respondsTo(instance, "setBinding")) {
            instance.binding = binding
        }
        handlePropertyInjection(instance)
        if (instance.metaClass.respondsTo(instance, "init")) {
            instance.init()
        }

        return instance
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

    protected void handlePropertyInjection(instance) {
        InjectArgBase injectArgBase = instance.getClass().getAnnotation(InjectArgBase)
        instance.metaClass.properties.each { MetaBeanProperty property ->
            InjectArg injectArg
            def key = property.name

            try {
                def field = getField(instance, key)
                if (field) {
                    injectArg = field.getAnnotation(InjectArg)
                }
                else {
                    return
                }
            }
            catch (NoSuchFieldException ignored) {
                //ignore... handles issues when searching for field "class" for instance
                return
            }

            boolean ignoreInjection = injectArg ? injectArg.ignore() : false

            if (ignoreInjection) return
            if (injectWithCli(instance, key, injectArg)) return
            if (injectWithConfig(instance, key, injectArg, injectArgBase)) return

            injectWithBinding(instance, key, injectArg)
        }
    }

    protected void injectWithBinding(def instance, String fieldName, InjectArg injectArg) {
        boolean injectByName = injectArg ? injectArg.injectByName() : true
        if (injectByName) {
            if (binding.hasVariable(fieldName)) {
                setValueOnInstance(instance, fieldName, binding."$fieldName")
            }
        }
    }

    protected boolean injectWithConfig(def instance, String fieldName, InjectArg injectArg,
                                       InjectArgBase injectArgBase) {
        def configObject = binding.variables.config
        def usedName = injectArg ? injectArg.injectByName() ? fieldName : null : fieldName
        def key = injectArg ? injectArg.config() ?: usedName : usedName
        def prefix
        if (injectArgBase) {
            prefix = injectArgBase.value()
        }
        if (!key.contains(".") && prefix) {
            key = "$prefix.$key"
        }

        if (configObject instanceof ConfigObject) {
            def flattened = configObject.flatten()
            def containsKey = flattened.containsKey(key as String)
            if (containsKey) {
                return setValueOnInstance(instance, fieldName, flattened[key])
            }
        }

        return false
    }

    protected boolean injectWithCli(instance, String fieldName, InjectArg injectArg) {
        def argsMap = binding.variables.argsMap
        def usedName = injectArg ? injectArg.injectByName() ? fieldName : null : fieldName
        def key = injectArg ? injectArg.cli() ?: usedName : usedName
        if (argsMap instanceof Map) {
            if (argsMap.containsKey(key)) {
                return setValueOnInstance(instance, fieldName, argsMap[key])
            }
        }

        return false
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    protected boolean setValueOnInstance(instance, String fieldName, value) {
        try {
            def field = getField(instance, fieldName)
            def type = field.type

            def isBoolean = type instanceof Boolean || type.name == "boolean"
            if (isBoolean) {
                instance."$fieldName" = Boolean.valueOf(value as String)
            }
            else {
                instance."$fieldName" = value.asType(type)
            }
            return true
        }
        catch (Throwable ignored) {
            //ignore, probably a casting issue
        }

        return false
    }

    def runDefaultStep() {
        includeService(ParseArgsService)
        if (binding.hasVariable("argsMap")) {
            Map argsMap = binding.argsMap
            defaultStep = argsMap.step ?: argsMap.target ?: defaultStep
        }
        depends(defaultStep)
    }
    /**
     * @deprecated
     * @return
     */
    def runDefaultTarget() {
        runDefaultStep()
    }

    protected static Field getField(instance, String fieldName) {
        def clazz = instance.getClass()
        def field = null

        while (clazz && field == null) {
            try {
                field = clazz.getDeclaredField(fieldName)
            }
            catch (NoSuchFieldException ignored) {
                clazz = clazz.superclass
            }
        }

        return field
    }
}
