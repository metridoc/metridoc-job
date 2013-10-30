package metridoc.illiad

class DateUtil {
    public static FY_START_MONTH = Calendar.JULY;
    public static final long ONE_DAY = 1000 * 60 * 60 * 24

    public static Date getDate(int year, int month, int day, int hourOfDay, int minute, int second) {
        def calendar = Calendar.getInstance()
        calendar.set(year, month, day, hourOfDay, minute, second)
        return calendar.getTime()//new Date(calendar.getTimeInMillis())
    }

    public static Date getDateStartOfDay(year, month, day) {
        return getDate(year, month, day, 0, 0, 0)
    }

    public static Date getDateEndOfDay(int year, int month, int day) {
        return getDate(year, month, day, 23, 59, 59)
    }

    public static int getFiscalYear(int year, int month) {
        return month < FY_START_MONTH || FY_START_MONTH == Calendar.JANUARY ? year : year + 1
    }

    public static int getLastDayOfMonth(int year, int month) {
        def calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        //Reset current day of month in case today is 31st
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    public static int getCurrentFiscalYear() {
        def currentDate = Calendar.getInstance();
        return getFiscalYear(currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH))
    }

    public static Date getFiscalYearStartDate(int fiscalYear) {
        return getDateStartOfDay(fiscalYear - 1, FY_START_MONTH, 1)
    }

    public static int getFiscalYearEndMonth() {
        return FY_START_MONTH - 1;
    }

    public static Date getFiscalYearEndDate(int fiscalYear) {
        def endMonth = getFiscalYearEndMonth();
        return getDateEndOfDay(fiscalYear, endMonth, getLastDayOfMonth(fiscalYear, endMonth))
    }

    public static Double differenceByDays(Date left, Date right) {
        if (datesNotNull(left, right)) {
            long leftLong = left.time
            long rightLong = right.time

            return (leftLong - rightLong) / ONE_DAY
        } else {
            return null
        }
    }

    static boolean isValidMonth(String month) {
        try {
            Calendar."${month.toUpperCase()}"
            return true
        }
        catch (Throwable ignored) {
            return false
        }
    }

    static void setMonth(String month) {
        assert isValidMonth(month) : "$month is not a valid month"
        FY_START_MONTH = Calendar."${month.toUpperCase()}"
    }

    private static boolean datesNotNull(Date dateOne, Date dateTwo) {
        dateOne != null && dateTwo != null
    }
}

