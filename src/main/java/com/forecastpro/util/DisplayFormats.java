package com.forecastpro.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DisplayFormats {

    /** Display format for dates shown in UI tables, labels, and API display fields. */
    public static final DateTimeFormatter UI_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private DisplayFormats() {
    }

    public static String formatDate(LocalDate d) {
        return d == null ? "" : d.format(UI_DATE);
    }

    /** First day of calendar month, formatted like other report dates (dd/MM/yyyy). */
    public static String formatMonthBucket(int year, int month) {
        return formatDate(LocalDate.of(year, Math.max(1, Math.min(12, month)), 1));
    }

    public static String formatInstantDate(Instant i, ZoneId zone) {
        if (i == null) {
            return "";
        }
        return UI_DATE.format(i.atZone(zone).toLocalDate());
    }

    public static String formatInstantDateTime(Instant i, ZoneId zone) {
        if (i == null) {
            return "";
        }
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(zone).format(i);
    }
}
