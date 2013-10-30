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