step(foo: "running foo") {
    println "foo has run"
}

step(bar: "running bar", depends: "foo") {
    println "bar has run"
}

runStep("bar")