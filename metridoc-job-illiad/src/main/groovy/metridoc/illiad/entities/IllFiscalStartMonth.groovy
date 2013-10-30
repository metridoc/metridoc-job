package metridoc.illiad.entities

import grails.persistence.Entity
import metridoc.illiad.DateUtil

/**
 * Created with IntelliJ IDEA on 9/12/13
 * @author Tommy Barker
 */
@Entity
class IllFiscalStartMonth {
    int month

    static void updateMonth(String month) {
        assert DateUtil.isValidMonth(month) : "$month is not a valid month"

        withNewTransaction {
            def startMonth = new IllFiscalStartMonth(
                    month: Calendar."${month.toUpperCase()}" as Integer
            )

            startMonth.save()
        }
    }
}
