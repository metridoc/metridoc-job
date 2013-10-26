package metridoc.ezproxy.utils
/**
 * @author Tommy Barker
 */
class TruncateUtils {
    public static final DEFAULT_VARCHAR_LENGTH = 255
    public static final DEFAULT_EZPROXY_ID_LENGTH = 50

    @SuppressWarnings("GrMethodMayBeStatic")
    static void truncateProperties(Map body, String... propertyNames) {
        propertyNames.each { propertyName ->
            def propertyValue = body[propertyName]
            if(propertyName == "ezproxyId") {
                body[propertyName] = truncate(propertyValue, DEFAULT_EZPROXY_ID_LENGTH)
            }
            else if (propertyValue && propertyValue instanceof String) {
                body[propertyName] = truncate(propertyValue, DEFAULT_VARCHAR_LENGTH)
            }
        }
    }

    static String truncate(String value, int length) {
        if (value) {
            if(value.size() > length) {
                return value.substring(0, length)
            }
        }

        return value
    }
}
