package metridoc.sql

import groovy.util.logging.Slf4j

import java.sql.SQLException

/**
 * Created by tbarker on 12/6/13.
 */
@Slf4j
class InsertableRecord {
    InsertMetaData insertMetaData
    Map<String, Object> originalRecord

    @Lazy(soft = true)
    LinkedHashMap<String, Object> transformedRecord = {
        LinkedHashMap response = [:]
        def record = originalRecord.clone() as Map
        insertMetaData.sortedParams.each { String paramName ->
            if (!record.containsKey(paramName)) {
                if (record.containsKey(paramName.toUpperCase())) {
                    response[paramName] = record.remove(paramName.toUpperCase())
                }
                else if (record.containsKey(paramName.toLowerCase())) {
                    response[paramName] = record.remove(paramName.toLowerCase())
                }
                else {
                    if (!insertMetaData.columnsWithDefaults.contains(paramName)) {
                        response[paramName] = null
                    }
                }
            }
            else {
                response[paramName] = record.remove(paramName)
            }
        }

        if (!record.isEmpty()) {
            throw new SQLException("the contents of [$record] has data not mappable to params [$insertMetaData.sortedParams]")
        }

        return response
    }()
}
