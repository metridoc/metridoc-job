import metridoc.core.services.HibernateService
import metridoc.core.services.ConfigService

/*
    This test assumes that mdoc is run with -localMysql and -mergeMetridocConfig=false command
    see HibernateDataSourceSpec
 */
includeService(ConfigService)
def tool = includeService(HibernateService)
assert tool.localMysql: "localMysql should be true"
assert config.dataSource.url == "jdbc:mysql://localhost:3306/test"



