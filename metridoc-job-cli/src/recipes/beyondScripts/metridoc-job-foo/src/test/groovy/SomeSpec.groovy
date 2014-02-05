import spock.lang.Specification

class SomeSpec extends Specification{

    void "test something"() {
        when:
        def response = 2 + 3

        then:
        5 == response
    }
}
