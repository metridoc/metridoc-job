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



package metridoc.utils

import java.util.regex.Matcher

/**
 * Basic log parsing utility
 */
class LogParser {

    LinkedHashMap<String, String> regexMap = [:]

    Map<String, Matcher> parseGetMatcher(String line) {
        return LogParser.parse(line, regexMap) {it}
    }

    Map<String, String> parse(String line) {
        return LogParser.parse(line, regexMap) {Matcher m ->
            m.group(1)
        }
    }

    static Map parse(String line, Map regexMap, Closure closure) {
        def result = [:]
        def currentLine = line
        regexMap.each {
            def m = currentLine =~ it.value
            if (m.lookingAt()) {
                result[it.key] = closure.call(m)
                def start = m.group()
                currentLine = currentLine.substring(start.size())
            } else {
                result[it.key] = null
            }
        }

        return result
    }
}
