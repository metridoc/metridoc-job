package metridoc.bd.utils

import groovy.sql.Sql
import org.slf4j.LoggerFactory

/**
 * Created by tbarker on 1/30/14.
 */
abstract class ValidateRecordCounts {

    def spec = [:]
    def invalidDataGroup = []
    static LOGGER = LoggerFactory.getLogger(ValidateRecordCounts)
    String dataStore = null;

    public static final String CREATE_VALIDATION_TABLE = "create table validate_counts(data_store varchar(24) not null, data_group varchar(24) not null, source_count integer not null default 0, load_count integer not null default 0, constraint unique index idx_data_store_data_group (data_store,data_group))"
    public static final String DROP_VALIDATION_TABLE = "drop table if exists validate_counts"
    public static final String DROP_WORK_TABLE = "drop table if exists work_table"
    public static final String UPDATE_VALIDATION_TABLE = "update validate_counts join work_table using (data_store,data_group) set load_count=group_count"
    public static final String INVALID_DATA = "select data_store, data_group from validate_counts where load_count<>source_count order by data_group"

    /**
     *  executes a query to compare row counts
     */
    void analyze() {
        def invalidDataItem = []

        Sql sql = Sql.newInstance( spec.repository )
        LOGGER.info "analyzing validation table..."
        sql.eachRow ( INVALID_DATA ) {
            if ( it.data_store == dataStore ) invalidDataGroup += it.data_group
        }
    }

    /**
     *  analyzes row count data and log mismatching instances
     */
    void report() {
        analyze()
        LOGGER.info( "creating validation report...\n" )

        String message = ""
        boolean alert = false
        if ( invalidDataGroup.size() > 0 ) {
            invalidDataGroup.each {
                LOGGER.warn( "\t " + dataStore + " contains unmatching row counts between original source and ingested data for ${it}" )
            }
            message += "\t ALERT - " + invalidDataGroup.size()
            alert = true
        } else  message += "\t There were no"

        message += " occurrances of unmatching row counts found.\n\n Validation Complete."

        alert ? LOGGER.warn(message) : LOGGER.info(message)
    }

    /**
     *  @String datastore - name primarily associated with the loaded data
     *
     *  removes the temporary work table and rebuild the validation table
     */
    void setup(String datastore) {

        dataStore = datastore
        LOGGER.info "\ndataStore set to " + datastore
        Sql sql = Sql.newInstance( spec.repository )
        LOGGER.info "\nremoving work table..."
        sql.execute( DROP_WORK_TABLE )
        LOGGER.info "rebuilding validation table..."
        sql.execute( DROP_VALIDATION_TABLE )
        sql.execute( CREATE_VALIDATION_TABLE )
    }

    /**
     *  constructs a sql query to create a temporary work table from specific values passed into a class level hash
     *
     *  @return String - create table query to hold application-specific ingested row counts for validation
     */
    abstract String populateWorkTable()

    /**
     *  constructs a sql query to create a temporary work table to hold values
     *
     *  @return String - create table query to hold source row counts to validate against
     */
    abstract String sourceCount()

    /**
     *  gathers row counts from data sources and loading tables for comparison
     */
    abstract void loadCounts()

    /**
     *  main method to invoke the validation process
     */
    abstract public void validate()

    /**
     *  retrieves the list of invalid data
     *
     *  @return List - data stores containing mismatching record counts between source and metridoc
     */
    public List getInvalidData() {
        return invalidDataGroup
    }
}