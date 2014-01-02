import metridoc.funds.services.FundsService

configure()

includeService(FundsService)

def stepToRun = argsMap.params ? argsMap.params[0] : "runFunds"

runStep(stepToRun)