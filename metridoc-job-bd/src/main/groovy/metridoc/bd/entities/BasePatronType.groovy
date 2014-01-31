package metridoc.bd.entities

/**
 * Created by tbarker on 1/31/14.
 */
abstract class BasePatronType {
    String patronType
    String patronTypeDesc

    static constraints = {
        patronType(maxSize: 1, unique: true)
        patronTypeDesc(maxSize: 50)
    }

    static mapping = {
        version(false)
    }
}
