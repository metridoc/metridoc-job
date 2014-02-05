package bar

import metridoc.core.Step

class Bar {
	boolean barFlag = false

	String baz = "not set"

	@Step(description = "runs bar")
	void runBar() {
		if(barFlag) {
			println "barFlag was set"
		} 
		else {
			println "barFlag was not set"
		}

		println "value of baz is [$baz]"
	}
}