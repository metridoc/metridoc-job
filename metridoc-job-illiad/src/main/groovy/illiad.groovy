import metridoc.core.services.ConfigService
import metridoc.core.tools.ConfigTool
import metridoc.illiad.DateUtil
import metridoc.illiad.IlliadService
import metridoc.illiad.entities.*
import metridoc.service.gorm.GormService
import metridoc.utils.DataSourceConfigUtil

import javax.sql.DataSource

//populate argsMap with cli info
includeService(ConfigService)

def gormService = includeService(GormService)
gormService.enableGormFor(
        IllGroup,
        IllBorrowing,
        IllTracking,
        IllCache,
        IllLenderGroup,
        IllLenderInfo,
        IllLending,
        IllLendingTracking,
        IllLocation,
        IllReferenceNumber,
        IllTransaction,
        IllUserInfo,
        IllFiscalStartMonth
)

if (argsMap.containsKey("preview")) {
    gormService.applicationContext.getBean("dataSource", DataSource).getConnection()
    println "connected successfully to dataSource"
    doPreview()
    return
}

def month = "july"
if (argsMap.containsKey("fiscalMonth")) {
    month = argsMap.fiscalMonth
    DateUtil.setMonth(month)
}

includeTool(IlliadService).execute()
IllFiscalStartMonth.updateMonth(month)

def doPreview() {
    doConnect("dataSource_from_illiad")
}

def doConnect(String name) {
    includeService(ConfigTool)
    println config
    def dataSource = DataSourceConfigUtil.getDataSource(config, name)

    try {
        dataSource.getConnection()
        println "INFO - Connected successfully to $name"
    }
    catch (Throwable throwable) {
        println "ERROR - Could not connect to [$name]"
        throw throwable
    }
}