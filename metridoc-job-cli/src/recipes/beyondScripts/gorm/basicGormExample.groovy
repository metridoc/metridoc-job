import grails.persistence.Entity
import grails.validation.ValidationException
import metridoc.service.gorm.GormService
import metridoc.utils.DataSourceConfigUtil

/**
 * https://raw.githubusercontent.com/metridoc/metridoc-job/master/metridoc-job-cli/src/recipes/beyondScripts/gorm/basicGormExample.groovy*/
dataSource = DataSourceConfigUtil.embeddedDataSource

//will used the bound dataSource above.  If not set, it will use the default dataSource defined in
// ~/.metridoc/MetridocConfig.groovy
def service = includeService(GormService)
service.enableFor(Foo, FooWithConstraints)

Foo.withTransaction {
    new Foo(bar: "baz1").save()
    new Foo(bar: "baz2").save()
    new Foo(bar: "baz3").save()
    new Foo(bar: "baz4").save()
    log.info "4 foos have been inserted"
}

assert 4 == Foo.count()

FooWithConstraints.withTransaction {
    def invalidFoo = new FooWithConstraints(
            bar: "12345678901",
            foo: 16,
    )

    assert !invalidFoo.save()
    assert !invalidFoo.validate()
    assert "nullable" == invalidFoo.errors.getFieldError("baz").code
    assert "maxSize.exceeded" == invalidFoo.errors.getFieldError("bar").code
    assert "max.exceeded" == invalidFoo.errors.getFieldError("foo").code

    try {
        invalidFoo.save(failOnError: true)
        assert false: "an error should have occurred"
    } catch (ValidationException e) {
        //do nothing
    }

    assert new FooWithConstraints(
            bar: "1234567890",
            foo: 15,
            baz: "boom"
    ).save()
}

assert 1 == FooWithConstraints.count()
log.info "1 fooWithConstraints was inserted"

@Entity
class Foo {
    String bar
}

@Entity
class FooWithConstraints {
    String bar
    int foo
    String baz

    static constraints = {
        bar maxSize: 10
        foo max:15
    }
}
