/**
 * mdoc https://raw.github.com/metridoc/metridoc-job/master/metridoc-job-cli/src/recipes/streaming/streamResultSet.groovy
 */
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import groovy.stream.Stream
import metridoc.core.services.CamelService
import metridoc.utils.DataSourceConfigUtil

import java.sql.ResultSet

dataSource = DataSourceConfigUtil.embeddedDataSource

def sql = new Sql(dataSource)

sql.execute("create table FOO (bar int)")
assert sql.executeUpdate("insert into FOO values(-1)")
assert sql.executeUpdate("insert into FOO values(1)")
assert sql.executeUpdate("insert into FOO values(-2)")
assert sql.executeUpdate("insert into FOO values(2)")
assert sql.executeUpdate("insert into FOO values(3)")
assert sql.executeUpdate("insert into FOO values(4)")
sql.execute("create table FOOBAR (bam int)")

def camelService = includeService(CamelService)

camelService.consume("sqletl:FOO?dataSource=dataSource") {ResultSet resultSet ->
    def stream = Stream.fromResultSet(resultSet).filter {GroovyRowResult rowResult ->
        rowResult.bar > 0
    }.map {GroovyRowResult rowResult ->
        def result = [:]
        result.bam = rowResult.bar * 2

        return result
    }

    camelService.send("sqletl:FOOBAR?dataSource=dataSource", stream)
}

assert 4 == sql.firstRow("select count(*) as total from FOOBAR").total
assert 1 == sql.firstRow("select count(*) as total from FOOBAR where bam = 2").total
assert 1 == sql.firstRow("select count(*) as total from FOOBAR where bam = 4").total
assert 1 == sql.firstRow("select count(*) as total from FOOBAR where bam = 6").total
assert 1 == sql.firstRow("select count(*) as total from FOOBAR where bam = 8").total