package fi.kidozz.app.features.dashboard

import java.time.LocalDate

/**
 * Returns only *new* dates that are not already present in existing absences.
 * This prevents re-submitting dates that already have absence records.
 * 
 * @param selected The dates the user has selected in the calendar
 * @param existing The dates that already have absence records
 * @return Only the dates that are new (not in existing)
 */
fun computeNewAbsenceDates(
    selected: Set<LocalDate>,
    existing: Set<LocalDate>
): Set<LocalDate> = selected - existing
