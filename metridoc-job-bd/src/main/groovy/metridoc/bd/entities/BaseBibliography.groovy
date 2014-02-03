package metridoc.bd.entities

/**
 * Created by tbarker on 1/31/14.
 */
abstract class BaseBibliography {
    Long bibliographyId
    String requestNumber
    String patronId
    String patronType
    String author
    String title
    String publisher
    String publicationDate
    String publicationPlace
    String publicationYear
    String edition
    String lccn
    String isbn
    String isbn_2
    Date requestDate
    Date processDate
    String pickupLocation
    Integer borrower
    Integer lender
    String supplierCode
    String callNumber
    Date loadTime
    Integer oclc
    String localItemFound

    static constraints = {
        localItemFound(nullable: true, maxSize: 1)
        requestNumber(maxSize: 12, nullable: true, unique: true)
        patronId(maxSize: 20, nullable: true)
        patronType(maxSize: 1, nullable: true)
        author(maxSize: 300, nullable: true)
        title(maxSize:400, nullable: true)
        publisher(nullable: true)
        publicationDate(nullable: true)
        publicationPlace(nullable: true)
        publicationYear(nullable: true, maxSize: 4)
        edition(nullable: true, maxSize: 24)
        lccn(nullable: true, maxSize: 32)
        isbn(nullable: true, maxSize: 24)
        isbn_2(nullable: true, maxSize: 24)
        requestDate(nullable: true)
        processDate(nullable: true)
        pickupLocation(nullable: true, maxSize: 64)
        borrower(nullable: true)
        lender(nullable: true)
        supplierCode(nullable: true, maxSize: 20)
        callNumber(nullable: true)
        oclc(nullable: true)
    }

    static mapping = {
        id name: 'bibliographyId'
        requestDate index: "idx_bd_bibliography_request_date"
        borrower index: "idx_bd_bibliography_borrower"
        lender index: "idx_bd_bibliography_lender"
        supplierCode index: "idx_bd_bibliography_supplier_code"
        version defaultValue: '0'
        loadTime defaultValue: '0'
    }
}
