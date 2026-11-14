package io.github.buerlino.apodroid.core

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random

/*
 * The picture time (0.6.0, J1; turns as a plain rotation since RO1, 2026-11-14; CLAUDE.md,
 * Background update, Turns): one time, one clock, two rules, decided by the clock, never by what
 * is on the screen.
 * 1. At "New picture at", and with several sources every "Change every" from then, a screen
 *    changes to the next of its sources and shows that source's newest picture: the source by the
 *    clock ([PictureTime.turn]), the next with a picture when it has none ([dueWallpapers]).
 * 2. A newer picture of that source published in the [pictureWindow] after the change still
 *    replaces it, whenever a check sees it (B15: by the publish time). With one source a post
 *    stored after a gap in the checks ([afterGap], B21) goes on whenever a run sees it too.
 * 0.6.0's rule 3 (with nothing new the screen waits, checked hourly) went with RO1.
 * This file has the times: when a screen changes ([PictureTime]) and when the next run checks
 * ([nextRun]).
 */

/** After a change time, a newer picture of the source that went on still replaces it for this long (rule 2). */
val pictureWindow: Duration = Duration.ofHours(3)

/** A source's checks this far apart or more are a gap (B21; [afterGap]). */
val checkGap: Duration = Duration.ofHours(24)

/**
 * Whether a new post stored [now] came after a gap (B21): the source's last successful check
 * ([lastCheck], read before this check notes it) is more than [checkGap] old. With one source
 * such a post goes on whenever a run sees it ([Stored.gaps]), so a day offline doesn't lose the
 * day's picture. No check yet (a fresh install, a source's first check) is no gap. Weekends
 * aren't gaps: the change time's checks succeed with nothing new and still note the check.
 */
fun afterGap(lastCheck: Instant?, now: Instant): Boolean = lastCheck != null && Duration.between(lastCheck, now) > checkGap

/** The first check after a change time, and a failed run's first retry; then hourly. */
val firstCheck: Duration = Duration.ofMinutes(15)

private val hourly: Duration = Duration.ofHours(1)

/** The per-device offset is at most this, so a time zone's phones don't all ask at once. */
val maxOffset: Duration = Duration.ofMinutes(10)

/** A device's offset, 0 to [maxOffset] in whole seconds; drawn once and stored. */
fun randomOffset(random: Random = Random.Default): Duration = Duration.ofSeconds(random.nextLong(0, maxOffset.seconds + 1))

/** "New picture at"'s default, 06:00 (an update from 0.5.0 gets it too). */
const val DEFAULT_PICTURE_AT = 6

/** The hours "New picture at" offers: 00:00 to 23:00. */
val pictureHours: IntRange = 0..23

/** [hour] when it's one of the [pictureHours], else [DEFAULT_PICTURE_AT] (a stored or saved value out of range). */
fun pictureAtOrDefault(hour: Int): Int = hour.takeIf { it in pictureHours } ?: DEFAULT_PICTURE_AT

/**
 * "New picture at" ([pictureAt], an hour 0–23) and "Change every" ([everyHours]: 24, 12, 6 or 3).
 * A column with turns (two or more sources) changes at [pictureAt] + k × [everyHours], a column
 * with one source at [pictureAt] only, every day, on the clock's hours in the time zone: a DST
 * day has 23 or 25 hours between two 06:00s; an hour that doesn't exist (02:00 on a spring-forward
 * night) comes at the gap's end (03:00), one that happens twice counts once, the first time.
 */
data class PictureTime(val pictureAt: Int = DEFAULT_PICTURE_AT, val everyHours: Int = 24) {
    init {
        require(pictureAt in pictureHours) { "pictureAt $pictureAt" }
        require(everyHours in 1..24 && 24 % everyHours == 0) { "everyHours $everyHours" }
    }

    /** The hours of the day a column changes at, in order. */
    fun hours(turns: Boolean): List<Int> =
        if (!turns) listOf(pictureAt) else (0 until 24 / everyHours).map { (pictureAt + it * everyHours) % 24 }.sorted()

    /**
     * The change times of the local day before [t], its own and the one after, in order, each
     * with its number ([turn]). A change time whose hour comes before [pictureAt] (00:00 with
     * 06:00 and 6 h) belongs to the day before.
     */
    private fun changes(t: Instant, zone: ZoneId, turns: Boolean): List<Pair<Instant, Long>> {
        val day = t.atZone(zone).toLocalDate()
        val hours = hours(turns)
        return (-1L..1L).flatMap { d ->
            val date = day.plusDays(d)
            hours.map { hour ->
                val ofDay = if (hour < pictureAt) date.minusDays(1) else date
                val number = ofDay.toEpochDay() * hours.size + (hour - pictureAt + 24) % 24 / everyHours
                date.atTime(hour, 0).atZone(zone).toInstant() to number
            }
        }.distinctBy { it.first }.sortedBy { it.first }
    }

    private fun around(t: Instant, zone: ZoneId, turns: Boolean): List<Instant> = changes(t, zone, turns).map { it.first }

    /** The latest change time at or before [now]: the current one. */
    fun last(now: Instant, zone: ZoneId, turns: Boolean): Instant = around(now, zone, turns).last { it <= now }

    /** The first change time after [now]. */
    fun next(now: Instant, zone: ZoneId, turns: Boolean): Instant = around(now, zone, turns).first { it > now }

    /**
     * The number of the current change time with turns, at [now] (RO1, by the clock): the day's
     * change times counted from [pictureAt] (0, 1, …), plus the days since 1970-01-01 × the change
     * times a day. Consecutive change times have consecutive numbers, so a column's sources take
     * turns by it, the number mod the column ([dueWallpapers]): with APOD and NASA every 6 h from
     * 06:00, APOD at 06:00 and 18:00, NASA at 12:00 and 00:00; every 24 h, a day each. A phone
     * off, a Set or a pull never shifts it.
     */
    fun turn(now: Instant, zone: ZoneId): Long = changes(now, zone, true).last { it.first <= now }.second

    /** Whether [t] is one of the change times (a record's stamp from 0.6.0's Set clock or 0.5.0 isn't). */
    fun isChangeTime(t: Instant, zone: ZoneId, turns: Boolean): Boolean = t in around(t, zone, turns)
}

/**
 * When the next run should check by the clock (J2 arms the alarms for it, [nextAlarm] with a
 * failed run's retry), at [now], by the change times of [time] ([turns]: a column takes turns, so
 * its hours count; they include [PictureTime.pictureAt]). The earliest of:
 * - the next change time;
 * - within the [pictureWindow] after the current change time, [firstCheck] after it, then hourly
 *   (06:00, 06:15, 07:15, 08:15: the :05 posts of every whole-hour time zone), and at its end
 *   (09:00: a half-hour zone's post, Tehran's 08:35, published within it; B15);
 * - with [allDay], the same checks hourly until the next change time (0.6.0's waiting screen,
 *   gone with RO1; kept for "As soon as it's out", SO1). No hourly checks after the window
 *   otherwise: a post that comes out later waits for its source's next turn.
 * The clock's checks come [offset] later (0 to [maxOffset], kept in that range). No model of each
 * source's posting time (user, 2026-10-23).
 *
 * A check that has passed since [lastRun] (the job's last run's start) is owed: the run comes at
 * [now] (B16). Every arming replaces the pair before it, so a page open, a Set or a clock change
 * between a check (06:07) and its alarm's delivery (up to 06:17), or a run's end when the run did
 * its update before the check, would otherwise drop it. Without a [lastRun] (none yet) or with
 * one ahead of [now] (the clock set back) nothing is owed.
 */
fun nextRun(
    now: Instant,
    zone: ZoneId,
    time: PictureTime,
    turns: Boolean,
    offset: Duration,
    lastRun: Instant? = null,
    allDay: Boolean = false,
): Instant {
    val off = offset.coerceIn(Duration.ZERO, maxOffset)
    // The checks on the clock without the offset, from the current change time of [now] less the
    // offset: a run at 06:03 with an offset of 7 min still checks at 06:07, and the 05:22 check
    // of the change time before (05:15 + 7 min) is still among them for [lastRun].
    val shifted = now - off
    val last = time.last(shifted, zone, turns)
    val next = time.next(shifted, zone, turns)
    val checks = mutableListOf(last, next, last + pictureWindow)
    var check = last + firstCheck
    while (check < next && (allDay || check < last + pictureWindow)) {
        checks += check
        check += hourly
    }
    val clock = checks.map { it + off }
    if (lastRun != null && clock.any { it > lastRun && it <= now }) return now
    return clock.filter { it > now }.min()
}

/**
 * How long after the start of the last of [failures] failed runs in a row the alarms retry it:
 * [firstCheck] after the first, an hour after more (a crash on every run is retried 15 min later,
 * then hourly, not every 15 min all day). A run counts as failed from its start until it ends
 * well (O4b), so this is also the retry of a run going, should it never end (the process killed
 * or frozen). With none (a run `DailyJob` couldn't schedule, O4a): [firstCheck].
 */
fun retryAfter(failures: Int): Duration = if (failures <= 1) firstCheck else hourly

/**
 * When to arm the alarms at [now] (a run's start and end, Set, the daily switch on, a page open,
 * a boot, a clock or time zone change, an app update): [nextRun] by the clock from [now] (a check
 * passed since [lastRun] at once, B16), or, after [failures] failed runs in a row (a run counts as
 * failed from its start, [lastRun], until it ends well: a run going, one stopped, one killed or
 * one that failed, O4b), [retryAfter] after the last one's start when that comes first, at the
 * earliest [now]. So an arming during a run keeps the pair its start armed (B16f1), one in a new
 * process after the run was killed keeps it too (B19), and one after a failed run keeps its retry.
 */
fun nextAlarm(
    now: Instant,
    zone: ZoneId,
    failures: Int,
    time: PictureTime,
    turns: Boolean,
    offset: Duration,
    lastRun: Instant?,
): Instant {
    val clock = nextRun(now, zone, time, turns, offset, lastRun)
    if (failures <= 0 || lastRun == null) return clock
    return minOf(clock, maxOf(lastRun + retryAfter(failures), now))
}
