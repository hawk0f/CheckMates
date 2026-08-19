package dev.hawk0f.checkmates.platform

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970

private val formatter = NSDateFormatter().apply {
    dateStyle = NSDateFormatterMediumStyle
    timeStyle = NSDateFormatterShortStyle
}

actual fun epochMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun formatDate(millis: Long): String =
    formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(millis / 1000.0))
