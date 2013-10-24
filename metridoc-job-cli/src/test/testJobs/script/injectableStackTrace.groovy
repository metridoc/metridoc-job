import metridoc.core.services.ParseArgsService


//need this for injecting stacktrace when it is a job argument
includeService(ParseArgsService)
def foo = includeService(Foo)
assert foo.stacktrace : "stacktrace should be injectable and set to true"
println "stacktrace is injectable"

class Foo {
    boolean stacktrace
}
