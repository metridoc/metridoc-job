//mdoc https://raw.github.com/metridoc/metridoc-job/master/metridoc-job-cli/src/recipes/basics/StepsDeps.groovy

step(foo: "running foo") {
    println "foo has run"
}

step(bar: "running bar", depends: "foo") {
    println "bar has run"
}

step(foobar: "running foobar", depends: ["foo", "bar"]) {
        println "foobar has run"
}

runStep("foobar")