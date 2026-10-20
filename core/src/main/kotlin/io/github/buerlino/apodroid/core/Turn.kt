package io.github.buerlino.apodroid.core

import java.time.Duration
import java.time.Instant

/** More than the job's flex (15 min, raised by Android to 5 % of the period), so a run a little early still ends a turn. */
val turnSlack: Duration = Duration.ofMinutes(30)

/**
 * The turn that began at [startedAt] (null: over) and lasts [length] is over at [now]. A
 * [startedAt] after [now] (the clock was set back) counts as over. Periodic jobs may run a
 * little early, hence [turnSlack].
 */
fun isTurnOver(startedAt: Instant?, length: Duration, now: Instant): Boolean =
    startedAt == null || startedAt > now || now >= startedAt + length - turnSlack

/**
 * The next of [ticked] after [after] in order (APOD → NASA → Earth, round to [after] itself)
 * for which [hasPicture]; null when none has one.
 */
fun nextSource(ticked: List<Source>, after: Source, hasPicture: (Source) -> Boolean): Source? {
    val all = Source.entries
    return all.indices.map { all[(after.ordinal + 1 + it) % all.size] }.firstOrNull { it in ticked && hasPicture(it) }
}

/**
 * Whose turn it is with two or more [ticked] sources: [current] (the one on the wallpaper)
 * until its turn, which began at [startedAt] and lasts [length], is over ([isTurnOver]), then
 * the [nextSource] with a picture; null when none has one.
 */
fun nextTurn(
    ticked: List<Source>,
    current: Source,
    startedAt: Instant?,
    length: Duration,
    now: Instant,
    hasPicture: (Source) -> Boolean,
): Source? {
    if (!isTurnOver(startedAt, length, now) && current in ticked && hasPicture(current)) return current
    return nextSource(ticked, current, hasPicture)
}
