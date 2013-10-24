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

import java.util.regex.Matcher

/**
 * @author Tommy Barker
 */
class ParseArgsService {
    static final String ARGS = "args"
    static final KEY_VALUE = /^[\-]+([^=]+)=(.*)$/
    static final KEY_NO_VALUE = /^[\-]+([^=]+)$/
    static final ONLY_PARAM = /^([^\-]+)$/

    Binding binding

    @SuppressWarnings("GrMethodMayBeStatic")
    void init() {
        if (binding.hasVariable(ARGS) &&
                (binding."$ARGS" instanceof String[] ||
                        binding."$ARGS" instanceof List)) {

            def args = binding."$ARGS"
            if (args instanceof List) {
                args = args as String[]
            }
            binding.argsMap = parseCli(args)
        }
    }

    static Map parseCli(String[] args) {
        def argsMap = [:]
        args.each {
            Matcher m = it =~ KEY_VALUE
            if (m.matches()) {
                def key = m.group(1)
                def value = m.group(2)
                argsMap."${key}" = value
            }
            m = it =~ KEY_NO_VALUE
            if (m.matches()) {
                def key = m.group(1)
                argsMap."${key}" = true
            }
            m = it =~ ONLY_PARAM
            if (m.matches()) {
                def key = m.group(1)
                argsMap.params = argsMap.params ?: []
                argsMap.params << key
            }
        }

        return argsMap
    }
}
