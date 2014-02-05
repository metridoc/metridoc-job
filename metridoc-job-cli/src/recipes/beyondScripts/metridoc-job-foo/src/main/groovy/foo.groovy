@Grab(group='com.github.groovy-wslite', module='groovy-wslite', version='0.8.0')
import wslite.rest.*

configure()

println "hello from foo"

includeService(bar.Bar)

runStep("runBar")