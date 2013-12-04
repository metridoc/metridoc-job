package metridoc.bd.services

import metridoc.bd.entities.BdBibliography
import metridoc.core.Step
import metridoc.service.gorm.GormService

/**
 * Created by tbarker on 12/4/13.
 */
class BdIngestionService {

    GormService gormService

    @Step(description = "create tables for Borrow Direct and EzBorrow")
    void createTables() {
        gormService.enableFor(BdBibliography)
    }

    @Step(description = "runs entire Borrow Direct and Ez Borrow workflow", depends = ["createTables"])
    void runWorkflow() {}
}
