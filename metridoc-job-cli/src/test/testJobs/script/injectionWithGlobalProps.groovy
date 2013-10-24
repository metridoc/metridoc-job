import metridoc.core.services.ParseArgsService

//assumes script is being run with some global flag along with bar flag
//mdoc --logLevel info injectionWithGlobalProps.groovy -bar=foo
includeService(ParseArgsService)
def service = includeService(Foo)
assert service.bar : "bar in Foo should not be null"
assert service.foo : "foo in Foo should be true"

class Foo {
    String bar
    boolean foo
}
