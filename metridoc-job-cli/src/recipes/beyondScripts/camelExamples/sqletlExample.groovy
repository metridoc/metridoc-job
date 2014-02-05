import metridoc.utils.DataSourceConfigUtil
import groovy.sql.Sql
import metridoc.core.services.CamelService

/*
  make sure you don't use def fooBarDataSource, otherwise the variable will not be bound and usable by sqletl
*/
fooBarDataSource = DataSourceConfigUtil.embeddedDataSource
def sql = new Sql(fooBarDataSource)

def camelService = includeService(CamelService)

sql.execute("create table foo (foobar int)")
sql.execute("create table bar (foobar int)")
sql.execute("insert into foo values (1)")
sql.execute("insert into foo values (2)")
sql.execute("insert into foo values (3)")

assert 0 == sql.firstRow("select count(*) as total from bar").total

camelService.consume("sqletl://FOO?dataSource=fooBarDataSource") {resultSet ->
	camelService.send("sqletl://BAR?dataSource=fooBarDataSource", resultSet)
}

assert 3 == sql.firstRow("select count(*) as total from bar").total

sql.execute("create table baz (baz int)")

camelService.consume("sqletl://select foobar as baz from FOO?dataSource=fooBarDataSource") {resultSet ->
	camelService.send("sqletl://BAZ?dataSource=fooBarDataSource", resultSet)
}

assert 3 == sql.firstRow("select count(*) as total from baz").total

camelService.consume("sqletl://FOO?dataSource=fooBarDataSource") {resultSet ->
	camelService.send("sqletl://insert into BAZ values (:foobar)?dataSource=fooBarDataSource", resultSet)
}

assert 6 == sql.firstRow("select count(*) as total from baz").total