import foo.bar.Foo
import grails.persistence.Entity
import metridoc.tool.gorm.GormTool
import metridoc.core.services.ParseArgsService


new Foo()

println "including parse service"
includeService(ParseArgsService)
println "including gorm service"
includeService(GormTool).enableFor(Bar)

println "listing bar contents"
Bar.list()

println "foo ran"
return "foo ran"

@Entity
class Bar {
    String foo
}