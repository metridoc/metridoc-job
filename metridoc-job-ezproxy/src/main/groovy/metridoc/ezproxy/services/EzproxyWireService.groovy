package metridoc.ezproxy.services

import metridoc.core.services.CamelService
import metridoc.core.services.ConfigService
import metridoc.core.services.DefaultService
import metridoc.core.services.ParseArgsService
import metridoc.service.gorm.GormService

/**
 * Created with IntelliJ IDEA on 9/24/13
 * @author Tommy Barker
 */
class EzproxyWireService extends DefaultService {

    boolean preview

    EzproxyStepsService wireupServices(Class ezproxyIngestClass) {
        includeService(ParseArgsService)
        includeService(ConfigService)

        wireupNonConfigServices(ezproxyIngestClass)
    }

    protected EzproxyStepsService wireupNonConfigServices(Class ezproxyIngestClass) {
        if (!preview) {
            try {
                includeService(GormService).enableFor(ezproxyIngestClass)
            }
            catch (IllegalStateException ignored) {
                //already enabled
            }
        }

        def camelService = includeService(CamelService)
        def ezproxyFileFilter = includeService(EzproxyFileFilterService, entityClass: ezproxyIngestClass)
        camelService.bind("ezproxyFileFilter", ezproxyFileFilter)

        includeService(EzproxyIngestService, entityClass: ezproxyIngestClass)
        includeService(EzproxyStepsService)
    }


}
