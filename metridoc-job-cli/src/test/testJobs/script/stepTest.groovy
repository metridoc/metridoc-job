
/**
 * @author Tommy Barker
 */
step(foo: "run foo", depends: "bar") {
    println "foo ran"
}

step(bar: "run bar") {
    println "bar ran"
}

runStep("foo")