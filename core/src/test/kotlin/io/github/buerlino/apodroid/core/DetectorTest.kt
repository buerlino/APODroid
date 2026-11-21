package io.github.buerlino.apodroid.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class DetectorTest {
    private val zurich = ZoneId.of("Europe/Zurich")

    private fun at(text: String): Instant = LocalDateTime.parse(text).atZone(zurich).toInstant()

    /** The detector's "now" in the exit-record tests: after every record and arming they use. */
    private val NOW = at("2026-11-16T12:00")

    private val offset = Duration.ofMinutes(4)

    /** The alarms found gone at [now] after a run at [lastRun], nothing else explaining it. */
    private fun lost(lastRun: String, now: String) =
        alarmsLost(gone = true, booted = false, updated = false, runPending = false, lastRun = at(lastRun), now = at(now))

    @Test
    fun wsTabletDayIsAMiss() {
        // W, Wed 28 Oct 2026: the last run 13:34, ZUI's cleaner force-stopped the app at 13:57, the
        // lock screen took turns (its new stamp 18:00), the page opened at 19:40 and set it.
        val lost = lost("2026-10-28T13:34", "2026-10-28T19:40")
        assertEquals(Lost(at("2026-10-28T19:40"), at("2026-10-28T13:34")), lost)
        assertEquals(at("2026-10-28T18:00"), missedChange(listOf(at("2026-10-28T18:00")), offset, lost))
    }

    @Test
    fun aHeldAlarmIsNone() {
        // A bucket (RARE: 57 min in V11a) holds the alarm past T + 30 min, but it's still there.
        val lost = alarmsLost(gone = false, booted = false, updated = false, runPending = false, lastRun = at("2026-10-28T09:00"), now = at("2026-10-28T06:50"))
        assertNull(lost)
        assertNull(missedChange(listOf(at("2026-10-28T06:00")), offset, lost))
    }

    @Test
    fun aPageBeforeTheChangeIsOverdueIsNone() {
        // 06:00 + 7 min offset + 30 min = 06:37: a page at 06:20 isn't late enough.
        val seven = Duration.ofMinutes(7)
        val early = lost("2026-10-27T09:00", "2026-10-28T06:20")
        assertNull(missedChange(listOf(at("2026-10-28T06:00")), seven, early))
        // At 06:37 it is.
        val late = lost("2026-10-27T09:00", "2026-10-28T06:37")
        assertEquals(at("2026-10-28T06:00"), missedChange(listOf(at("2026-10-28T06:00")), seven, late))
    }

    @Test
    fun aBootExplainsTheAlarmsGone() {
        // The phone off overnight: booted after T, the alarms gone with the boot, not a stop.
        val lost = alarmsLost(gone = true, booted = true, updated = false, runPending = false, lastRun = at("2026-10-27T21:00"), now = at("2026-10-28T08:00"))
        assertNull(lost)
        assertNull(missedChange(listOf(at("2026-10-28T06:00")), offset, lost))
    }

    @Test
    fun anUpdateExplainsTheAlarmsGone() {
        assertNull(alarmsLost(gone = true, booted = false, updated = true, runPending = false, lastRun = at("2026-10-27T21:00"), now = at("2026-10-28T08:00")))
    }

    @Test
    fun aPendingRunExplainsTheAlarmsGone() {
        // The pair fired and was used up; the run (job 2) waits for the network or its quota.
        assertNull(alarmsLost(gone = true, booted = false, updated = false, runPending = true, lastRun = at("2026-10-27T21:00"), now = at("2026-10-28T08:00")))
    }

    @Test
    fun noRunYetIsNone() {
        // Nothing noted yet: nothing is owed.
        assertNull(alarmsLost(gone = true, booted = false, updated = false, runPending = false, lastRun = null, now = at("2026-10-28T08:00")))
    }

    @Test
    fun aRunThatStartedAndFailedIsNone() {
        // The 06:00 run started at 06:04 and failed (no network, a 503): the problem box's.
        val lost = lost("2026-10-28T06:04", "2026-10-28T08:00")
        assertNull(missedChange(listOf(at("2026-10-28T06:00")), offset, lost))
    }

    @Test
    fun theDailySwitchOnAfterTheChangeIsNone() {
        // The switch on notes the last run's start: the change before it isn't owed.
        val lost = lost("2026-10-28T07:00", "2026-10-28T08:00")
        assertNull(missedChange(listOf(at("2026-10-28T06:00")), offset, lost))
    }

    @Test
    fun nothingToSetIsNone() {
        assertNull(missedChange(emptyList(), offset, lost("2026-10-27T21:00", "2026-10-28T08:00")))
    }

    @Test
    fun aChangeAfterTheAlarmsWereFoundGoneIsNone() {
        // A later run's set by the next change time: the alarms were armed again by then.
        val lost = lost("2026-10-27T21:00", "2026-10-28T08:00")
        assertNull(missedChange(listOf(at("2026-10-28T12:00")), offset, lost))
    }

    @Test
    fun theLatestMissedChangeIsNamed() {
        // Home with one source (06:00), lock with turns (18:00): both missed, the later named.
        val lost = lost("2026-10-27T21:00", "2026-10-28T19:40")
        val stamps = listOf(at("2026-10-28T06:00"), at("2026-10-28T18:00"))
        assertEquals(at("2026-10-28T18:00"), missedChange(stamps, offset, lost))
        // Without a loss noted, none.
        assertNull(missedChange(stamps, offset, null))
    }


    @Test fun missedTodayHasNoDate() =
        assertEquals("06:00 change", missedWhen(at("2026-11-16T06:00:00"), at("2026-11-16T20:00:00"), zurich, "06:00"))

    @Test fun missedYesterdayNamesTheDay() =
        assertEquals("06:00 change on 16 Nov", missedWhen(at("2026-11-16T06:00:00"), at("2026-11-17T09:00:00"), zurich, "06:00"))

    @Test fun acrossMidnightInTheZone() {
        // 23:30 UTC on the 16th is 00:30 on the 17th in Zurich: today's when now is the 17th there
        val missed = Instant.parse("2026-11-16T23:30:00Z")
        assertEquals("12:30 AM change", missedWhen(missed, at("2026-11-17T08:00:00"), zurich, "12:30 AM"))
        assertEquals("12:30 AM change on 17 Nov", missedWhen(missed, at("2026-11-18T08:00:00"), zurich, "12:30 AM"))
    }

    @Test
    fun theExitRecordsAreReadOnlyOn11To14() {
        // TP1f1's fix round 1: the FP6 (Android 16) read and logged every record at each new process.
        for (sdk in 30..34) assertEquals(true, readsExitRecords(sdk))
        for (sdk in listOf(29, 35, 36, 37)) assertEquals(false, readsExitRecords(sdk))
    }

    @Test
    fun aForceStopAfterTheArmingCountsOn11To14() {
        // TP1: the emulator (Android 14) after `am force-stop`: the PendingIntents still there, an
        // exit record the user asked for after the last arming.
        val armed = at("2026-11-16T06:04")
        val stop = listOf(UserExit(at("2026-11-16T09:30"), "stop io.github.buerlino.apodroid due to from pid 1234"))
        for (sdk in 30..34) assertEquals(true, stoppedSinceArmed(sdk, stop, armed, NOW))
        // 29 has no records, 15+ cancel the PendingIntents themselves: as before.
        assertEquals(false, stoppedSinceArmed(29, stop, armed, NOW))
        assertEquals(false, stoppedSinceArmed(35, stop, armed, NOW))
        assertEquals(false, stoppedSinceArmed(36, stop, armed, NOW))
    }

    @Test
    fun aStopBeforeTheArmingIsNone() {
        // The force stop's record is older than the arming after it (a page open armed again).
        val stop = listOf(UserExit(at("2026-11-16T09:30"), "stop io.github.buerlino.apodroid due to from pid 1234"))
        assertEquals(false, stoppedSinceArmed(34, stop, at("2026-11-16T09:31"), NOW))
        assertEquals(false, stoppedSinceArmed(34, stop, at("2026-11-16T09:30"), NOW))
        assertEquals(false, stoppedSinceArmed(34, emptyList(), at("2026-11-16T06:00"), NOW))
    }

    @Test
    fun aRecordFromAfterNowIsNone() {
        // Open question 5 (0.9.1; T5 on the emulator): a force stop at a faked 28 Nov, the clock set back
        // to 17 Nov and armed again there: the old record is newer than the arming but later than now.
        val stop = listOf(UserExit(at("2026-11-28T09:30"), "stop io.github.buerlino.apodroid due to from pid 1234"))
        val armed = at("2026-11-17T10:00")
        assertEquals(false, stoppedSinceArmed(34, stop, armed, at("2026-11-17T10:05")))
        // The clock back where it was: the record counts again.
        assertEquals(true, stoppedSinceArmed(34, stop, armed, at("2026-11-28T09:30")))
    }

    @Test
    fun noArmingNotedIsNone() {
        assertEquals(false, stoppedSinceArmed(34, listOf(UserExit(at("2026-11-16T09:30"), null)), null, NOW))
    }

    @Test
    fun aRecentsSwipeIsNone() {
        // A swipe from Recents kills the process as the user's ("remove task") but leaves the alarms.
        val armed = at("2026-11-16T06:04")
        assertEquals(false, stoppedSinceArmed(34, listOf(UserExit(at("2026-11-16T09:30"), "remove task")), armed, NOW))
        assertEquals(true, stoppedSinceArmed(34, listOf(UserExit(at("2026-11-16T09:30"), "remove task"), UserExit(at("2026-11-16T10:00"), "stop io.github.buerlino.apodroid due to from pid 1234")), armed, NOW))
    }

    @Test
    fun aBackgroundKillIsNone() {
        // TP1 fix round 1: `am kill`, task killers and cleaners call killBackgroundProcesses, recorded as
        // REASON_USER_REQUESTED on 30-34, but the alarms stay. Only a force stop's "stop …" counts.
        val armed = at("2026-11-16T06:04")
        for (d in listOf("kill background", "kill all background", null, "")) {
            assertEquals(false, stoppedSinceArmed(34, listOf(UserExit(at("2026-11-16T09:30"), d)), armed, NOW))
        }
    }

    @Test
    fun aForceStopBySubreasonCounts() {
        // TP1f1: T4 saw no line after 11 force stops on the Android 14 emulator; a record whose summary
        // names the subreason counts whatever its description says.
        val armed = at("2026-11-16T06:04")
        val summary = "ApplicationExitInfo(timestamp=2026-12-01 07:01:22.500 pid=19443 realUid=10142 packageUid=10142 " +
            "definingUid=10142 user=0 process=io.github.buerlino.apodroid reason=10 (USER REQUESTED) subreason=21 (FORCE STOP) " +
            "status=0 importance=400 pss=0 rss=0 description=stop io.github.buerlino.apodroid due to from pid 19658 state=empty)"
        assertEquals(true, isForceStopSummary(summary))
        for (d in listOf(null, "", "force-stop")) {
            assertEquals(true, stoppedSinceArmed(34, listOf(UserExit(at("2026-11-16T09:30"), d, forceStop = true)), armed, NOW))
        }
        // Still only after the arming, and only on 30-34.
        assertEquals(false, stoppedSinceArmed(34, listOf(UserExit(at("2026-11-16T06:00"), null, forceStop = true)), armed, NOW))
        assertEquals(false, stoppedSinceArmed(35, listOf(UserExit(at("2026-11-16T09:30"), null, forceStop = true)), armed, NOW))
    }

    @Test
    fun otherSubreasonsArentAForceStop() {
        assertEquals(false, isForceStopSummary(null))
        assertEquals(false, isForceStopSummary("reason=10 (USER REQUESTED) subreason=22 (REMOVE TASK) description=remove task"))
        assertEquals(false, isForceStopSummary("reason=10 (USER REQUESTED) subreason=0 (UNKNOWN) description=kill background"))
        assertEquals(false, isForceStopSummary("reason=10 subreason=210"))
    }
}
