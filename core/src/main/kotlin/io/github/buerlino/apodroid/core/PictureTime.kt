package io.github.buerlino.apodroid.core

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random

/*
 * The picture time (0.6.0, J1; CLAUDE.md, Background update, Turns): one time, one clock, three
 * rules, decided by the clock, never by what is on the screen.
 * 1. At "New picture at", and with several sources every "Change every" from then, a screen
 *    changes: to its source's newest picture, or to the next source with a picture it hasn't
 *    shown ([dueWallpapers]).
 * 2. A newer picture of that source published in the [pictureWindow] after the change still
 *    replaces it, whenever a check sees it (B15: by the publish time).
 * 3. With several sources and nothing new, the picture stays until one comes out (the screen waits).
 * This file has the times: when a screen changes ([PictureTime]) and when the next run checks
 * ([nextRun]).
 */

/** After a change time, a newer picture of the source that went on still replaces it for this long (rule 2). */
val pictureWindow: Duration = Duration.ofHours(3)

/** The first check after a change time, and a failed run's first retry; then hourly. */
val firstCheck: Duration = Duration.ofMinutes(15)

private val hourly: Duration = Duration.ofHours(1)

/** The per-device offset is at most this, so a time zone's phones don't all ask at once. */
val maxOffset: Duration = Duration.ofMinutes(10)

/** A device's offset, 0 to [maxOffset] in whole seconds; drawn once and stored. */
fun randomOffset(random: Random = Random.Default): Duration = Duration.ofSeconds(random.nextLong(0, maxOffset.seconds + 1))

/**
 * "New picture at" ([pictureAt], an hour 0–23) and "Change every" ([everyHours]: 24, 12, 6 or 3).
 * A column with turns (two or more sources) changes at [pictureAt] + k × [everyHours], a column
 * with one source at [pictureAt] only, every day, on the clock's hours in the time zone: a DST
 * day has 23 or 25 hours between two 06:00s; an hour that doesn't exist (02:00 on a spring-forward
 * night) comes at the gap's end (03:00), one that happens twice counts once, the first time.
 */
data class PictureTime(val pictureAt: Int = 6, val everyHours: Int = 24) {
    init {
        require(pictureAt in 0..23) { "pictureAt $pictureAt" }
        require(everyHours in 1..24 && 24 % everyHours == 0) { "everyHours $everyHours" }
    }

    /** The hours of the day a column changes at, in order. */
    fun hours(turns: Boolean): List<Int> =
        if (!turns) listOf(pictureAt) else (0 until 24 / everyHours).map { (pictureAt + it * everyHours) % 24 }.sorted()

    /** The change times of the local day before [t], its own and the one after, in order. */
    private fun around(t: Instant, zone: ZoneId, turns: Boolean): List<Instant> {
        val day = t.atZone(zone).toLocalDate()
        return (-1L..1L).flatMap { d -> hours(turns).map { day.plusDays(d).atTime(it, 0).atZone(zone).toInstant() } }.distinct().sorted()
    }

    /** The latest change time at or before [now]: the current one. */
    fun last(now: Instant, zone: ZoneId, turns: Boolean): Instant = around(now, zone, turns).last { it <= now }

    /** The first change time after [now]. */
    fun next(now: Instant, zone: ZoneId, turns: Boolean): Instant = around(now, zone, turns).first { it > now }

    /** Whether [t] is one of the change times (a record's stamp from 0.6.0's Set clock or 0.5.0 isn't). */
    fun isChangeTime(t: Instant, zone: ZoneId, turns: Boolean): Boolean = t in around(t, zone, turns)
}

/**
 * When the next run should check (J2 arms the alarms for it), at [now] after a run, by the
 * change times of [time] ([turns]: a column takes turns, so its hours count; they include
 * [PictureTime.pictureAt]). The earliest of:
 * - the next change time;
 * - within the [pictureWindow] after the current change time, [firstCheck] after it, then hourly
 *   (06:00, 06:15, 07:15, 08:15: the :05 posts of every whole-hour time zone), and at its end
 *   (09:00: a half-hour zone's post, Tehran's 08:35, published within it; B15);
 * - while a screen [waiting] (several sources, nothing new: [waiting]), the same checks hourly
 *   until the next change time;
 * - after [failures] failed or stopped runs in a row, [firstCheck] after the first, hourly after
 *   more (the alarms retry, not Android's backoff).
 * The clock's checks come [offset] later (0 to [maxOffset], kept in that range); the retries
 * count from [now]. No model of each source's posting time (user, 2026-10-23).
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
    failures: Int,
    time: PictureTime,
    turns: Boolean,
    offset: Duration,
    waiting: Boolean,
    lastRun: Instant? = null,
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
    while (check < next && (waiting || check < last + pictureWindow)) {
        checks += check
        check += hourly
    }
    val clock = checks.map { it + off }
    if (lastRun != null && clock.any { it > lastRun && it <= now }) return now
    val retry = retryAfter(failures)?.let { now + it }
    return (clock.filter { it > now } + listOfNotNull(retry)).min()
}

/** How long after the last of [failures] failed or stopped runs in a row the alarms retry: null without one. */
private fun retryAfter(failures: Int): Duration? = when {
    failures <= 0 -> null
    failures == 1 -> firstCheck
    else -> hourly
}

/**
 * How long after a run's start its own retry comes, should it never reach its end (the process
 * killed or frozen), after [failures] failed runs in a row before it (a killed one counted at the
 * next start, B19): it would be one more, so [firstCheck] with none before, an hour after one or
 * more. A crash on every run is retried 15 min later, then hourly, not every 15 min all day.
 */
fun runRetry(failures: Int): Duration = retryAfter(maxOf(failures, 0) + 1)!!

/**
 * When to arm the alarms at [now], also outside a run's end (Set, the daily switch on, a page
 * open, a boot, a clock or time zone change, an app update), which must not move a failed run's
 * retry: [nextRun] by the clock from [now], or the retry of the last of [failures] failed runs in
 * a row, which ended at [failedAt], when that comes first; a retry already due comes at [now],
 * as a check passed since [lastRun] does (B16). At a run's end ([failedAt] = [now]) the same as [nextRun] with [failures].
 * While a run goes ([running], started at [lastRun]; also one that started and never ended, in a
 * new process, B19), no later than [runRetry] after its start (at the earliest [now]): the pair
 * its start armed stays, so a run that never reaches its end (the process killed or frozen) is
 * still retried (B16f1). The last failure's retry then doesn't count: the run going is it.
 */
fun nextAlarm(
    now: Instant,
    zone: ZoneId,
    failures: Int,
    failedAt: Instant?,
    time: PictureTime,
    turns: Boolean,
    offset: Duration,
    waiting: Boolean,
    lastRun: Instant? = null,
    running: Boolean = false,
): Instant {
    val clock = nextRun(now, zone, 0, time, turns, offset, waiting, lastRun)
    if (running && lastRun != null) return minOf(clock, maxOf(lastRun + runRetry(failures), now))
    val retry = failedAt?.let { at -> retryAfter(failures)?.let { at + it } } ?: return clock
    return minOf(clock, maxOf(retry, now))
}
