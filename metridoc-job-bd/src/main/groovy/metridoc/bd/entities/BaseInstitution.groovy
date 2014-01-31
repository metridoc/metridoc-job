package metridoc.bd.entities

/**
 * Created by tbarker on 1/31/14.
 */
abstract class BaseInstitution {
    String catalogCode
    String institution
    Integer libraryId

    static constraints = {
        libraryId unique: true
        institution unique: true, maxSize: 64
        catalogCode maxSize: 1
    }

    static mapping = {
        version defaultValue: '0'
        catalogCode defaultValue: ''
    }
}
