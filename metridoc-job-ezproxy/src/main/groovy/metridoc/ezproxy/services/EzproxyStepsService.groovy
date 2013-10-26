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


