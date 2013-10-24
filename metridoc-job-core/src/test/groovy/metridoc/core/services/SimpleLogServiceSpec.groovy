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

import spock.lang.Specification

import static SimpleLogService.DEFAULT_LOG_LEVEL
import static SimpleLogService.METRIDOC_LOGGER

/**
 * Created with IntelliJ IDEA on 7/25/13
 * @author Tommy Barker
 */
class SimpleLogServiceSpec extends Specification {

    void "logging is added after includeTool is run"() {
        when:
        def binding = new Binding()
        binding.includeService(SimpleLogService)

        then:
        System.getProperty(METRIDOC_LOGGER)
        System.getProperty(DEFAULT_LOG_LEVEL)
    }
}
