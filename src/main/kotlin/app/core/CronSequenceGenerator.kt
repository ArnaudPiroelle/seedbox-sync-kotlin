package app.core

import java.util.*


/**
 * Date sequence generator for a
 * [Crontab pattern](https://www.manpagez.com/man/5/crontab/),
 * allowing clients to specify a pattern that the sequence matches.
 *
 *
 * The pattern is a list of six single space-separated fields: representing
 * second, minute, hour, day, month, weekday. Month and weekday names can be
 * given as the first three letters of the English names.
 *
 *
 * Example patterns:
 *
 *  * "0 0 * * * *" = the top of every hour of every day.
 *  * "*&#47;10 * * * * *" = every ten seconds.
 *  * "0 0 8-10 * * *" = 8, 9 and 10 o'clock of every day.
 *  * "0 0 6,19 * * *" = 6:00 AM and 7:00 PM every day.
 *  * "0 0/30 8-10 * * *" = 8:00, 8:30, 9:00, 9:30, 10:00 and 10:30 every day.
 *  * "0 0 9-17 * * MON-FRI" = on the hour nine-to-five weekdays
 *  * "0 0 0 25 12 ?" = every Christmas Day at midnight
 *
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Ruslan Sibgatullin
 * @since 3.0
 * @see CronTrigger
 */
class CronSequenceGenerator {
    /**
     * Return the cron pattern that this sequence generator has been built for.
     */
    val expression: String

    private val timeZone: TimeZone?
    private val months = BitSet(12)
    private val daysOfMonth = BitSet(31)
    private val daysOfWeek = BitSet(7)
    private val hours = BitSet(24)
    private val minutes = BitSet(60)
    private val seconds = BitSet(60)
    /**
     * Construct a [CronSequenceGenerator] from the pattern provided,
     * using the specified [TimeZone].
     * @param expression a space-separated list of time fields
     * @param timeZone the TimeZone to use for generated trigger times
     * @throws IllegalArgumentException if the pattern cannot be parsed
     */
    /**
     * Construct a [CronSequenceGenerator] from the pattern provided,
     * using the default [TimeZone].
     * @param expression a space-separated list of time fields
     * @throws IllegalArgumentException if the pattern cannot be parsed
     * @see java.util.TimeZone.getDefault
     */
    @JvmOverloads
    constructor(expression: String, timeZone: TimeZone? = TimeZone.getDefault()) {
        this.expression = expression
        this.timeZone = timeZone
        parse(expression)
    }

    private constructor(expression: String, fields: Array<String>) {
        this.expression = expression
        timeZone = null
        doParse(fields)
    }

    /**
     * Get the next [Date] in the sequence matching the Cron pattern and
     * after the value provided. The return value will have a whole number of
     * seconds, and will be after the input value.
     * @param date a seed value
     * @return the next value matching the pattern
     */
    fun next(date: Date?): Date {
        /*
		The plan:
		1 Start with whole second (rounding up if necessary)
		2 If seconds match move on, otherwise find the next match:
		2.1 If next match is in the next minute then roll forwards
		3 If minute matches move on, otherwise find the next match
		3.1 If next match is in the next hour then roll forwards
		3.2 Reset the seconds and go to 2
		4 If hour matches move on, otherwise find the next match
		4.1 If next match is in the next day then roll forwards,
		4.2 Reset the minutes and seconds and go to 2
		*/
        val calendar: Calendar = GregorianCalendar()
        calendar.timeZone = timeZone
        calendar.time = date

        // First, just reset the milliseconds and try to calculate from there...
        calendar[Calendar.MILLISECOND] = 0
        val originalTimestamp = calendar.timeInMillis
        doNext(calendar, calendar[Calendar.YEAR])
        if (calendar.timeInMillis == originalTimestamp) {
            // We arrived at the original timestamp - round up to the next whole second and try again...
            calendar.add(Calendar.SECOND, 1)
            doNext(calendar, calendar[Calendar.YEAR])
        }
        return calendar.time
    }

    private fun doNext(calendar: Calendar, dot: Int) {
        val resets: MutableList<Int> = ArrayList()
        val second = calendar[Calendar.SECOND]
        val emptyList = emptyList<Int>()
        val updateSecond = findNext(seconds, second, calendar, Calendar.SECOND, Calendar.MINUTE, emptyList)
        if (second == updateSecond) {
            resets.add(Calendar.SECOND)
        }
        val minute = calendar[Calendar.MINUTE]
        val updateMinute = findNext(minutes, minute, calendar, Calendar.MINUTE, Calendar.HOUR_OF_DAY, resets)
        if (minute == updateMinute) {
            resets.add(Calendar.MINUTE)
        } else {
            doNext(calendar, dot)
        }
        val hour = calendar[Calendar.HOUR_OF_DAY]
        val updateHour = findNext(hours, hour, calendar, Calendar.HOUR_OF_DAY, Calendar.DAY_OF_WEEK, resets)
        if (hour == updateHour) {
            resets.add(Calendar.HOUR_OF_DAY)
        } else {
            doNext(calendar, dot)
        }
        val dayOfWeek = calendar[Calendar.DAY_OF_WEEK]
        val dayOfMonth = calendar[Calendar.DAY_OF_MONTH]
        val updateDayOfMonth =
            findNextDay(calendar, daysOfMonth, dayOfMonth, daysOfWeek, dayOfWeek, resets)
        if (dayOfMonth == updateDayOfMonth) {
            resets.add(Calendar.DAY_OF_MONTH)
        } else {
            doNext(calendar, dot)
        }
        val month = calendar[Calendar.MONTH]
        val updateMonth = findNext(months, month, calendar, Calendar.MONTH, Calendar.YEAR, resets)
        if (month != updateMonth) {
            require(calendar[Calendar.YEAR] - dot <= 4) {
                "Invalid cron expression \"" + expression +
                        "\" led to runaway search for next trigger"
            }
            doNext(calendar, dot)
        }
    }

    private fun findNextDay(
        calendar: Calendar,
        daysOfMonth: BitSet,
        dayOfMonth: Int,
        daysOfWeek: BitSet,
        dayOfWeek: Int,
        resets: List<Int>
    ): Int {
        var dayOfMonth = dayOfMonth
        var dayOfWeek = dayOfWeek
        var count = 0
        val max = 366
        // the DAY_OF_WEEK values in java.util.Calendar start with 1 (Sunday),
        // but in the cron pattern, they start with 0, so we subtract 1 here
        while ((!daysOfMonth[dayOfMonth] || !daysOfWeek[dayOfWeek - 1]) && count++ < max) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            dayOfMonth = calendar[Calendar.DAY_OF_MONTH]
            dayOfWeek = calendar[Calendar.DAY_OF_WEEK]
            reset(calendar, resets)
        }
        require(count < max) { "Overflow in day for expression \"" + expression + "\"" }
        return dayOfMonth
    }

    /**
     * Search the bits provided for the next set bit after the value provided,
     * and reset the calendar.
     * @param bits a [BitSet] representing the allowed values of the field
     * @param value the current value of the field
     * @param calendar the calendar to increment as we move through the bits
     * @param field the field to increment in the calendar (@see
     * [Calendar] for the static constants defining valid fields)
     * @param lowerOrders the Calendar field ids that should be reset (i.e. the
     * ones of lower significance than the field of interest)
     * @return the value of the calendar field that is next in the sequence
     */
    private fun findNext(
        bits: BitSet,
        value: Int,
        calendar: Calendar,
        field: Int,
        nextField: Int,
        lowerOrders: List<Int>
    ): Int {
        var nextValue = bits.nextSetBit(value)
        // roll over if needed
        if (nextValue == -1) {
            calendar.add(nextField, 1)
            reset(calendar, listOf(field))
            nextValue = bits.nextSetBit(0)
        }
        if (nextValue != value) {
            calendar[field] = nextValue
            reset(calendar, lowerOrders)
        }
        return nextValue
    }

    /**
     * Reset the calendar setting all the fields provided to zero.
     */
    private fun reset(calendar: Calendar, fields: List<Int>) {
        for (field in fields) {
            calendar[field] = if (field == Calendar.DAY_OF_MONTH) 1 else 0
        }
    }
    // Parsing logic invoked by the constructor
    /**
     * Parse the given pattern expression.
     */
    @Throws(IllegalArgumentException::class)
    private fun parse(expression: String) {
        val fields: Array<String> = StringUtils.tokenizeToStringArray(expression, " ")
        require(areValidCronFields(fields)) {
            String.format(
                "Cron expression must consist of 6 fields (found %d in \"%s\")", fields.size, expression
            )
        }
        doParse(fields)
    }

    private fun doParse(fields: Array<String>) {
        setNumberHits(seconds, fields[0], 0, 60)
        setNumberHits(minutes, fields[1], 0, 60)
        setNumberHits(hours, fields[2], 0, 24)
        setDaysOfMonth(daysOfMonth, fields[3])
        setMonths(months, fields[4])
        setDays(daysOfWeek, replaceOrdinals(fields[5], "SUN,MON,TUE,WED,THU,FRI,SAT"), 8)
        if (daysOfWeek[7]) {
            // Sunday can be represented as 0 or 7
            daysOfWeek.set(0)
            daysOfWeek.clear(7)
        }
    }

    /**
     * Replace the values in the comma-separated list (case insensitive)
     * with their index in the list.
     * @return a new String with the values from the list replaced
     */
    private fun replaceOrdinals(value: String, commaSeparatedList: String): String {
        var value = value
        val list: Array<String> = StringUtils.commaDelimitedListToStringArray(commaSeparatedList)
        for (i in list.indices) {
            val item = list[i].toUpperCase()
            value = StringUtils.replace(value.toUpperCase(), item, "" + i)
        }
        return value
    }

    private fun setDaysOfMonth(bits: BitSet, field: String) {
        val max = 31
        // Days of month start with 1 (in Cron and Calendar) so add one
        setDays(bits, field, max + 1)
        // ... and remove it from the front
        bits.clear(0)
    }

    private fun setDays(bits: BitSet, field: String, max: Int) {
        var field = field
        if (field.contains("?")) {
            field = "*"
        }
        setNumberHits(bits, field, 0, max)
    }

    private fun setMonths(bits: BitSet, value: String) {
        var value = value
        val max = 12
        value = replaceOrdinals(value, "FOO,JAN,FEB,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC")
        val months = BitSet(13)
        // Months start with 1 in Cron and 0 in Calendar, so push the values first into a longer bit set
        setNumberHits(months, value, 1, max + 1)
        // ... and then rotate it to the front of the months
        for (i in 1..max) {
            if (months[i]) {
                bits.set(i - 1)
            }
        }
    }

    private fun setNumberHits(bits: BitSet, value: String, min: Int, max: Int) {
        val fields: Array<String> = StringUtils.delimitedListToStringArray(value, ",")
        for (field in fields) {
            if (!field.contains("/")) {
                // Not an incrementer so it must be a range (possibly empty)
                val range = getRange(field, min, max)
                bits[range[0]] = range[1] + 1
            } else {
                val split: Array<String> = StringUtils.delimitedListToStringArray(field, "/")
                require(split.size <= 2) {
                    "Incrementer has more than two fields: '" +
                            field + "' in expression \"" + expression + "\""
                }
                val range = getRange(split[0], min, max)
                if (!split[0].contains("-")) {
                    range[1] = max - 1
                }
                val delta = split[1].toInt()
                require(delta > 0) {
                    "Incrementer delta must be 1 or higher: '" +
                            field + "' in expression \"" + expression + "\""
                }
                var i = range[0]
                while (i <= range[1]) {
                    bits.set(i)
                    i += delta
                }
            }
        }
    }

    private fun getRange(field: String, min: Int, max: Int): IntArray {
        val result = IntArray(2)
        if (field.contains("*")) {
            result[0] = min
            result[1] = max - 1
            return result
        }
        if (!field.contains("-")) {
            result[1] = field.toInt()
            result[0] = result[1]
        } else {
            val split: Array<String> = StringUtils.delimitedListToStringArray(field, "-")
            require(split.size <= 2) {
                "Range has more than two fields: '" +
                        field + "' in expression \"" + expression + "\""
            }
            result[0] = split[0].toInt()
            result[1] = split[1].toInt()
        }
        require(!(result[0] >= max || result[1] >= max)) {
            "Range exceeds maximum (" + max + "): '" +
                    field + "' in expression \"" + expression + "\""
        }
        require(!(result[0] < min || result[1] < min)) {
            "Range less than minimum (" + min + "): '" +
                    field + "' in expression \"" + expression + "\""
        }
        require(result[0] <= result[1]) {
            "Invalid inverted range: '" + field +
                    "' in expression \"" + expression + "\""
        }
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is CronSequenceGenerator) {
            return false
        }
        val otherCron = other
        return months == otherCron.months && daysOfMonth == otherCron.daysOfMonth && daysOfWeek == otherCron.daysOfWeek && hours == otherCron.hours && minutes == otherCron.minutes && seconds == otherCron.seconds
    }

    override fun hashCode(): Int {
        return 17 * months.hashCode() + 29 * daysOfMonth.hashCode() + 37 * daysOfWeek.hashCode() + 41 * hours.hashCode() + 53 * minutes.hashCode() + 61 * seconds.hashCode()
    }

    override fun toString(): String {
        return javaClass.simpleName + ": " + expression
    }

    companion object {
        /**
         * Determine whether the specified expression represents a valid cron pattern.
         * @param expression the expression to evaluate
         * @return `true` if the given expression is a valid cron expression
         * @since 4.3
         */
        fun isValidExpression(expression: String?): Boolean {
            if (expression == null) {
                return false
            }
            val fields: Array<String> = StringUtils.tokenizeToStringArray(expression, " ")
            return if (!areValidCronFields(fields)) {
                false
            } else try {
                CronSequenceGenerator(expression, fields)
                true
            } catch (ex: IllegalArgumentException) {
                false
            }
        }

        private fun areValidCronFields(fields: Array<String>?): Boolean {
            return fields != null && fields.size == 6
        }
    }
}