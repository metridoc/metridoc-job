package metridoc.illiad.entities

import grails.persistence.Entity

/**
 * Created with IntelliJ IDEA on 9/7/13
 * @author Tommy Barker
 */
@Entity
class IllTransaction {
    Long userName
    Long transactionNumber
    String requestType
    String loanAuthor
    String loanTitle
    String loanPublisher
    String loanLocation
    String loanDate
    String loanEdition
    String photoJournalTitle
    String photoJournalVolume
    String photoJournalIssue
    String photoJournalMonth
    String photoJournalYear
    String photoJournalInclusivePages
    String photoArticleAuthor
    String photoArticleTitle
    String citedIn
    String transactionStatus
    Date transactionDate
    String issn
    String espNumber
    String lenderCodes
    String lendingLibrary
    String reasonForCancellation
    String callNumber
    String location
    String processType
    String systemId
    String ifmCost
    String inProcessDate
    String billingAmount
    String userId

    static mapping = {
        version(defaultValue: '0')
        transactionDate(index: "idx_ill_transaction_transaction_date")
    }

    static constraints = {
        transactionNumber(unique: true)
        lendingLibrary(nullable: true)
        userName(nullable: true)
        requestType(nullable: true)
        loanAuthor(nullable: true)
        loanTitle(nullable: true)
        loanPublisher(nullable: true)
        loanLocation(nullable: true)
        loanDate(nullable: true)
        loanEdition(nullable: true)
        photoJournalTitle(nullable: true)
        photoJournalVolume(nullable: true)
        photoJournalIssue(nullable: true)
        photoJournalMonth(nullable: true)
        photoJournalYear(nullable: true)
        photoJournalInclusivePages(nullable: true)
        photoArticleAuthor(nullable: true)
        photoArticleTitle(nullable: true)
        citedIn(nullable: true)
        transactionStatus(nullable: true)
        transactionDate(nullable: true)
        issn(nullable: true)
        espNumber(nullable: true)
        lenderCodes(nullable: true)
        reasonForCancellation(nullable: true)
        callNumber(nullable: true)
        location(nullable: true)
        processType(nullable: true)
        systemId(nullable: true)
        ifmCost(nullable: true)
        inProcessDate(nullable: true)
        billingAmount(nullable: true)
        userId(nullable: true)
    }
}
