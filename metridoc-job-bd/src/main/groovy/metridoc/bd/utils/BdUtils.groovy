package metridoc.bd.utils

import groovy.util.logging.Slf4j
import org.apache.commons.lang.StringUtils

import java.sql.ResultSet

/**
 * Created by tbarker on 1/9/14.
 */
@Slf4j
class BdUtils {

    /**
     * helpful for migrating small lookup tables where data could change or might not be in the target table yet
     *
     * @param from
     * @param to
     * @param withId
     *
     * @see {@link metridoc.bd.entities.BdInstitution}, {@link metridoc.bd.entities.BdPatronType}
     */
    static void migrateData(ResultSet from, Class to, String withId) {
        to.withTransaction {
            while(from.next()) {
                def row = from.toRowResult()
                log.debug("migrating row {} to {}", row, to)
                String lowerCaseId = withId.toLowerCase()
                String idField = StringUtils.capitalize(getFieldNameForUnderscoredSeparatedName(withId))
                def toInstance = to."findBy${idField}"(row.containsKey(lowerCaseId) ? row."${lowerCaseId}" : row."${lowerCaseId.toUpperCase()}")
                if(toInstance == null) {
                    toInstance = to.newInstance()
                }
                row.each {String key, value ->
                    def convertedKeyName = getFieldNameForUnderscoredSeparatedName(key)
                    toInstance."$convertedKeyName" = value
                }
                if (toInstance.isDirty()) {
                    toInstance.save(failOnError: true)
                }
            }
        }
    }

    public static boolean isBlank(String str) {
        return str == null || str.trim().length() == 0;
    }

    public static String getFieldNameForUnderscoredSeparatedName(String name) {
        // Handle null and empty strings.
        if (isBlank(name)) return name;

        if (name.indexOf('_') == -1) {
            return name.toLowerCase()
        }

        StringBuilder buf = new StringBuilder();
        String[] tokens = name.split("_");
        for (String token : tokens) {
            if (token == null || token.length() == 0) continue;
            buf.append(token.substring(0, 1).toUpperCase())
                    .append(token.substring(1).toLowerCase());
        }
        return StringUtils.uncapitalize(buf.toString());
    }
}
