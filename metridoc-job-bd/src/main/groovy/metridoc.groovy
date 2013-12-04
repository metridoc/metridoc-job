import metridoc.bd.services.BdIngestionService
import metridoc.service.gorm.GormService
import metridoc.core.services.ParseArgsService

configure()

includeService(ParseArgsService)
includeService(GormService)
includeService(BdIngestionService)

if(argsMap.params) {
    runStep(argsMap.params[0])
}
else {
    runStep("runWorkflow")
}



