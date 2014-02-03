package metridoc.bd.entities

/**
 * Created by tbarker on 1/31/14.
 */
abstract class BaseExceptionCode {
    String exceptionCode
    String exceptionCodeDesc

    static constraints = {
        exceptionCode(maxSize: 3, unique: true)
        exceptionCodeDesc(maxSize: 64, unique: true)
    }

    static mapping = {
        id name: "exceptionCode", generator: "assigned"
        version defaultValue: '0'
    }
}
