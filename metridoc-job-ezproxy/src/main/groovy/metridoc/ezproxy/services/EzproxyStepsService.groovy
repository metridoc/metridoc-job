/*
  *Copyright 2013 Trustees of the University of Pennsylvania. Licensed under the
  *	Educational Community License, Version 2.0 (the "License"); you may
  *	not use this file except in compliance with the License. You may
  *	obtain a copy of the License at
  *
  *http://www.osedu.org/licenses/ECL-2.0
  *
  *	Unless required by applicable law or agreed to in writing,
  *	software distributed under the License is distributed on an "AS IS"
  *	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
  *	or implied. See the License for the specific language governing
  *	permissions and limitations under the License.
  */

package metridoc.ezproxy.services

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import metridoc.core.InjectArgBase
import metridoc.core.Step

/**
 * Created with IntelliJ IDEA on 6/13/13
 * @author Tommy Barker
 */
@SuppressWarnings("GrMethodMayBeStatic")
@Slf4j
@ToString(includePackage = false, includeNames = true)
@InjectArgBase("ezproxy")
class EzproxyStepsService {
    EzproxyIngestService ezproxyIngestService

    @Step(description = "previews the data")
    void preview() {
        ezproxyIngestService.preview = true
        processEzproxyFile()
    }

    @Step(description = "processes an ezproxy file")
    void processEzproxyFile() {
        ezproxyIngestService.ingestData()
    }
}


