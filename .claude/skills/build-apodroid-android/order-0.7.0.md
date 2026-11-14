# The list's order (0.7.0)

(User, 2026-11-14: "The list of the sources, APOD, NASA, Earth, must be sortable using an icon to
drag up and down; once they are in the arrangement you want, upon setting it, the wallpaper that
is on top will be set at 06:00." Planned the same day by reading dev, no code; the user answered
the open questions the same day, and susanna's review (against dev 3ef5c74: RO1–RO3, SO1–SO3, BR1
merged) is folded in. The decisions are in [Decided](#decided); what the user didn't say is in
[Night defaults](#night-defaults), each with its reason, for the user to overrule. The packages
write their decisions into `CLAUDE.md` when they land; this file is the how.)

## Why

0.6.0 and RO1 turn a screen's sources in a fixed order, APOD → NASA → Earth (`Source.entries`;
RO1: the change time's number mod the column's sources). A user with NASA and APOD can't have NASA
first, and a Set never shifts the rotation (RO1, 2026-11-14), so nothing the user does says which
source comes at the next "New picture at". The user wants both: the order, and the top one at the
next 06:00 after setting it.

**Back from 2026-10-23:** "the ↑ ↓ order" and "Turns start at" with "When I tap Set" were dropped
then (`CLAUDE.md`, Next features, 0.6.0: the picture time, Dropped), in the simplification to
three rules. Now the user wants an order (2026-11-14), set by drag, not by ↑ ↓ buttons; and the
top source at the next "New picture at" after a Set of a new order brings back a Set that starts
the turns, in one fixed form (no setting), only for the order.

## Decided

(User, 2026-11-14.)
- **"Pictures from" can be sorted by drag**: each row gets a drag handle, a text glyph ("≡", no
  icon library: the app uses text glyphs only); dragging it moves the row up or down. Three rows
  only, so Compose's own pointer input, no library.
- **The order is the turns' order:** it replaces the fixed APOD → NASA → Earth. One order for both
  screens: each screen takes its ticked sources in the list's order.
- **Part of Setup's draft** (until Set), highlighted in `tertiary` while it differs from the stored
  one, as the other draft settings; Set saves it.
- **"Keep what's on"** (asked: NASA, APOD, Earth sorted, Set at 20:00): Set saves the order and
  the wallpaper stays as it is; at the next "New picture at" (06:00 by default) each screen gets
  its top ticked source, then the others in the list's order.
- **Between that Set and the next "New picture at" the turns go on:** with "Change every" 3, 6 or
  12 h the change times in between (00:00 after a Set at 20:00 with 6 h) still change, the
  rotation phased so the top lands at the next "New picture at". Not the literal "stays as it is
  until 06:00", which would freeze the screen for up to a day after a Set in the morning; with
  24 h the two are the same.
- **The list restarts from its top only when the order changed**, or at the first Set with the
  list shown while no anchor is stored (`orderFrom` missing: an install from 0.6.0 or a build
  before OR2). A Set that changes anything else (ticks, "Change every", "New picture at", "If
  APOD is a video", "my picture", "As soon as it's out") and turning on "Change automatically"
  leave the anchor as it is.
- **"APOD stays"** (answered on susanna's review): until the anchor's "New picture at" (now < A),
  a Set without a new order and the daily switch turned on keep each screen's owner and set its
  picture again, as a Set of a new order with other changes does; they don't jump to the clock's
  turn, which right after an anchor is mostly the last in the list. From A on they are RO1's Set.
  So "keep what's on" holds from the order's Set until A, whatever else is set in between.
- **"Top one goes on now"** (answered on susanna's review): with "Change automatically" off, a Set
  of a new order puts each screen's top ticked source on at once. With the switch off nothing
  turns, so this Set is the only thing that puts a picture on.
- **This changes RO1's rule.** RO1 (2026-11-14, Open 1 (b)): the turn's source is the change
  time's number mod the column's sources, and "a phone off, a Set or a pull never shifts it". Now
  a Set of a new order does shift it, once, to put the top at the next "New picture at" (the
  anchor), and until then Sets keep what's on. A phone off, a pull, and every Set and switch-on
  from the anchor on still never shift it.
- **Screen readers:** drag can't be used, so each row gets the actions "Move up" and "Move down"
  (Android's own words, as in its language list).
- Texts (the glyph, the actions, the ⓘ below) are placeholders until the user approves them from
  the tablet's screenshots.

## Night defaults

(User, 2026-11-14, from pass 36: Night default 1 kept, a Set of the order alone puts the top on at once when the owner can't hold.)

(Chosen 2026-11-14 while planning, without the user; each stands unless the user overrules it.)

1. **How a Set or the switch on keeps what's on before A** (the daily switch on):
   - **Only the order changed** (the draft `Setup` equals the stored one in everything but
     `order`, `parts` included, and holds no newly picked "my picture", `saveSetup`'s `pick`
     false): each screen's record is written with its owner and picture kept and the stamp set
     to the current change time; nothing is set. The toast is the one Set already has, "Setup
     saved. Nothing to set yet." (no new text). Why the stamp: a change time that passed without
     a run (an order Set at 06:03 before the 06:07 run with 24 h) would otherwise be taken by the
     next run with the new phase, putting the clock's −1 (Earth) on; the user said keep. Rule 2
     and "As soon as it's out" still put the owner's newer picture on.
   - **Anything else** (other settings changed too, a Set that changed nothing, the first Set
     with no anchor whose order is unchanged, the daily switch turned on): each screen keeps its
     owner while that is still ticked there and has something to show, its newest picture goes
     on again at once (the record's picture forgotten, as 0.6.0's Set), the stamp the current
     change time. So a changed "my picture", whole picture, ⛶ part or "If APOD is a video" shows
     at once, and an updater's first Set puts the wallpaper back as 0.6.0's did. Why: deferring
     those to 06:00 would make Set look broken; the source on the screen is what "keep" means.
   - **Keep can't hold** (no record: a fresh install, a screen whose column was empty; the owner
     no longer ticked there; the owner with nothing to show, e.g. APOD's video with "Skip it"):
     the screen gets its **top** ticked source at once (the next in the list's order with a
     picture when the top has none). Why: the user just said which comes first; the clock's turn
     before A is mostly the last one in the list.
2. **With the daily switch off, every Set before A puts the top on at once**, not only one of a
   new order (that one is the user's). Why: the same reason, and RO1's clock's turn before A
   would be the last in the list. From A on, RO1's Set.
3. **The prefs:** `order`, a comma list of all three sources (`NASA,APOD,EARTH`), default
   `APOD,NASA,EARTH`, read tolerantly (unknown names dropped, duplicates dropped, missing sources
   appended in the default order); `orderFrom`, the anchor, the local date (ISO, `2026-11-15`) of
   the next "New picture at" after the Set that moved it (by the saved setup's hour: the same day
   for a Set before it, else the next day). **Both are written only while the list shows** ("More
   NASA pictures" on in the draft), in `saveSetup`'s one commit; `orderFrom` only when the order
   changed or none is stored. In APOD mode both stay as stored (`CLAUDE.md`: a hidden setting
   goes back to the stored one), so the first Set in APOD mode writes no `orderFrom`. **No
   migration:** an install from 0.6.0 (or a 0.7.0 build before OR2) has neither, which is RO1's
   rotation exactly (the default order, the number mod the sources), until its first Set with the
   list shown.
4. **The handle:** "≡" at the row's start, before the tick box (the dropdown keeps wrapping under
   the name at the end), a 48 dp touch target in `onSurfaceVariant`; shown on all three rows,
   ticked or not, also the last ticked row (its tick box is greyed out, its handle isn't), and
   also when no column takes turns (then the order changes nothing until a column has two
   sources: no row moves sideways when a second tick makes turns). Hidden from screen readers;
   the actions are on the row's tick box node.
5. **The highlight:** each row whose place differs from the stored order shows its "≡" in
   `tertiary`; the row's screen-reader state gets ", not set yet" when its place or its screens
   differ (the one existing text); "Setup"'s title as for any draft change. Not the heading
   "Pictures from" (the rows say which moved).
6. **What else follows the order:** Setup's closed summary lists the sources in the list's order
   ("NASA, APOD · both screens", "Home: NASA, APOD · Lock: Earth"); "Change every"'s ⓘ (RO2's
   placeholder) names it: "Every 6 h from 06:00, each screen switches to its next source in the
   list; a new order starts with the top one at 06:00." (placeholder). **Unchanged, APOD, NASA,
   Earth:** the title's dropdown and the swipe (the sources the page offers, not the turns), the
   problem box's first source, "Save every picture"'s order. No new status line (no "Next: NASA
   at 06:00": the texts stay lean). Why: the order is the turns'; moving the pager with it would
   change the page's swipe on every drag for no gain in what's set.
7. **APOD mode** (the switch off): the order is kept, not shown, not written (Night default 3); it
   changes nothing there (APOD alone).
8. **Two packages, no OR3:** the faked night and the Sets before and after A are OR2's tablet
   checks, not a rosie package of its own; T2 runs them again on the combined tree.

## The rules (what changes in 0.7.0's)

**Rule 1 with the order** (turns, a column with two or more): at each change time c a screen
changes to its turn's source and shows its newest picture. With the column's ticked sources in
the list's order, S₀ … Sₙ₋₁, the turn's source is

  **S[(N(c) − N(A)) mod n]**

- N(c): the change time's number (`PictureTime.turn`, RO1: the local day's change times counted
  from "New picture at", plus the day's number since 1970-01-01 × the change times a day).
- A: the anchor, the "New picture at" of the date `orderFrom`; N(A) = that date's epoch day ×
  the change times a day (24 / "Change every"), always computed from the stored setup, never
  stored itself. It's the day's first change time, so its index in the day is 0. Without
  `orderFrom`, N(A) = 0: RO1's rotation.
- A source with nothing to show passes the turn to the next **in the list's order** with a
  picture (`nextSource`); none: the screen keeps its picture (as RO1).

So after a Set of NASA, APOD, Earth at 20:00 on both screens:

| "Change every" | 00:00 | 06:00 (A) | 12:00 | 18:00 | 00:00 | 06:00 |
|---|---|---|---|---|---|---|
| 24 h | — | NASA | — | — | — | APOD |
| 6 h, three sources | Earth | NASA | APOD | Earth | NASA | APOD |
| 6 h, NASA and APOD | APOD | NASA | APOD | NASA | APOD | NASA |

(With 6 h and two sources the top is at every 06:00 and 18:00; with three it moves by one a day:
"then the others in the list's order".)

**Set and the switch on, by one rule:** with the anchor after this Set (the new one, or the
stored one), **before A** (now < A) they keep what's on (Night default 1; with the daily switch
off a Set puts the top on, Decided and Night default 2); **from A on, or with no anchor**, they
are RO1's Set (each screen to the current change time's turn by the formula, the next with a
picture when that one has none, set at once; a screen with no record on the clock's turn, RO1's
`start`). A Set of a new order always lands before its own A.

**Which Set moves the anchor:** one with the list shown whose order differs from the stored one,
or with the list shown while no `orderFrom` is stored. It writes `order` and `orderFrom` (the
local date of `time.next(now, zone, turns = false)`, the next "New picture at" after now, by the
saved setup's hour). Every other Set, and the daily switch on (`startAfresh`), leave `orderFrom`;
a Set with the list shown writes `order` (unchanged then).

**Without a new order, the rotation's place** is always the formula with the **kept date and the
new setup**, N(A) recomputed:
- **"Change every" changed:** N(A) = the same date × the new change times a day. On each "New
  picture at" k days after the anchor's, the turn is S[(k × 24 / "Change every") mod n], as if
  the new setting had run since the anchor; between them the day's change times go on in the
  list's order. Why this over continuing from the source on now (a new anchor picked so the next
  change gets the one after it): it's what RO1 does with its fixed day 1970-01-01 (the anchor is
  only another day 0); the position is a function of the clock, the settings and one date, as
  2026-10-23 and RO1 wanted ("by the clock, not by what's on the screen"); continuing from what's
  on would hang on the screen's state, the hidden state 2026-11-11 removed; and `orderFrom` stays
  as the user said. Before A such a Set keeps what's on anyway.
- **"New picture at" changed:** the date stays and N(A) doesn't depend on the hour, so the anchor
  is that date's new "New picture at"; the days' positions stay the same, each change time moves
  with the hour. A change of hour can move A before or after now (06:00 → 05:00 at 05:30 on A's
  day): "before A" is decided by the saved setup's A.
- **A tick or an untick** (n changes): the same N(c) − N(A) mod the new n over the column's
  ticked sources in the list's order. The top comes at every "New picture at" whose distance
  from the anchor is a multiple of n (with 24 h); a newly ticked source comes in at its place in
  the list. From A on, RO1's Set puts the current turn on at once, so a tick can change what's on
  at Set (RO1 does so today); before A the owner stays.
- **The daily switch on** (`startAfresh`): before A keeps what's on, from A on RO1's start
  afresh; `orderFrom` as it is.

**Unchanged:**
- **One source per column:** the order changes nothing (its change time is "New picture at"
  only, rule 2 decides).
- **Rule 2** (the 3-hour window by the publish time) and **"As soon as it's out"** (SO1, SO2):
  they act on the owner, whichever the order made it, also on a kept owner before A.
- **Equal columns** change together (the lock screen takes home's record): one order, so equal
  columns stay equal.
- **Columns that differ:** each screen its own ticked sources in the list's order, the same N(c)
  and anchor, so each has its own top at A.
- **A phone off over A:** missed change times make one change, by the current one's number: off
  from 05:00 to 08:00 with 24 h, the top goes on at 08:00; off until 13:00 with 6 h, 12:00's
  source (the second) goes on. The top doesn't wait for the phone (by the clock, as RO1).
- **The stand-in** fills APOD's turn wherever APOD is in the order.
- **A pull, never back, the window's checks, the alarms** (`nextRun`, `nextAlarm`: the change
  times don't move; only which source each gets).
- DST and time zones: N counts local days on the clock's hours (RO1), and `orderFrom` is a local
  date, so a DST night or a new zone keeps the phase.

## What changes (by reading dev)

- **`:core`, `Turn.kt`:** `nextSource` cycles over the list's order, not `Source.entries` (the
  owner may be unticked, so over the whole order from its place); a parser for the pref
  (`orderOf(String?)`, tolerant as Night default 3).
- **`:core`, `Screens.kt`:** `Rules.turnOf` takes the column in the list's order and the anchor
  (S[(N − N(A)) mod n]); `pick` and `start` with the order (`start` stays the clock's turn);
  `dueWallpapers` and `restartAndForget` take the order and the anchor, defaults the old ones
  (`Source.entries`, none), so a call without them is RO1's. `restartAndForget` decides by now
  against A: from A on (or no anchor) RO1's path with the order; before A, by `orderOnly` and
  `daily`: the records kept with the current stamp (only the order), the owner kept and forgotten
  (the rest), the top (the switch off, or keep can't hold: also a screen with no record or in
  `drop`, as a fresh record on its top). A suggestion, the worker's to shape: a
  `Rotation(order, from)` passed where `PictureTime` goes.
- **`:core`, `PictureTime.kt`:** the anchor's date at Set (`time.next(now, zone, false)`'s local
  date), A's instant for a date, and N(A) (the date's epoch day × `hours(true).size`), next to
  `turn`.
- **`Store`:** `Setup.order` (default `Source.entries`), the prefs `order` and `orderFrom`;
  `saveSetup` writes them only while the draft shows the list, `orderFrom` only for a new order
  or none stored, and tells `restartAndForget` `orderOnly` (the draft `Setup` equals the stored
  one but for `order`, `parts` included, and `pick` false) and the daily switch; `startAfresh`
  leaves `orderFrom` and passes the anchor; `setDue` passes the order and the anchor.
- **The page:** the drag list, its actions, the draft order (saved state too), `setupChanged`
  with the order while the list shows, the highlight, Setup's summary in the order, "Change
  every"'s ⓘ text.
- **`CLAUDE.md`:** The app, Settings (the list: the handle, the order, the draft, Set keeping
  what's on until A); Turns and the picture time (rule 1 with the order and the anchor, Set and
  the switch on before and from A, RO1's "a Set never shifts" kept from A on); Next features,
  0.6.0's Dropped line ("the ↑ ↓ order" back as the drag order, 2026-11-14) and a 0.7.0 entry for
  this track with the user's answers.

## Tests and checks

- `ScreensTest`:
  - **The user's example:** NASA, APOD, Earth on both screens, 24 h, APOD on; a Set of the order
    alone at 20:00 sets nothing and keeps APOD; the next 06:00 NASA, then APOD, then Earth.
  - **"APOD stays":** the same; at 21:00 a Set changing only "If APOD is a video" keeps APOD and
    sets it again (not Earth, the clock's −1); NASA at 06:00. The same with the daily switch off
    and on at 21:00 (`startAfresh`) instead of the Set. And from A on (the next day at 07:00) a
    Set without a new order is RO1's (the formula's turn at once).
  - **An order Set before a missed run:** 24 h, offset 7 min, APOD on with yesterday's stamp; a
    Set of the order alone at 06:03, then the 06:07 run: nothing new from the order goes on
    (Earth doesn't), the stamp 06:00 today; APOD's own newer picture by rule 2 may.
  - **The first Set with no anchor**, order unchanged (an update from 0.6.0, Set tapped to put
    the wallpaper back): `orderFrom` written, the owner kept and set again (not an order-only
    keep); a first Set in APOD mode writes no `orderFrom` (with `Store`'s, OR2).
  - The table above (6 h with three and with two sources), and the 00:00 before the anchor.
  - A phone off over the anchor (24 h: the top at 08:00; 6 h: the second at 13:00).
  - The top with nothing to show at the anchor passes to the next **in the list's order**
    (Earth, NASA, APOD with nothing of Earth's: NASA, where APOD → NASA → Earth would give APOD).
  - Before A, other changes: the owner kept and set again; the owner unticked → the top; the
    owner with nothing to show → the top with a picture; no record → the top; the daily switch
    off → the top; equal columns one set; columns that differ each their top at A.
  - **Without a new order, from A on:** "Change every" 24 h → 6 h with three sources, the anchor
    date kept: the turns by the formula with the new times a day (k days after the anchor at
    06:00: S[4k mod 3]); "New picture at" 06:00 → 08:00: the same day positions at 08:00; a tick
    from two sources to three: the formula with n = 3.
  - "As soon as it's out" with an order, and with a kept owner before A: the owner's newer
    picture goes on, another's waits.
  - A video day: the stand-in in APOD's place in the order.
  - A DST day as the anchor (Zurich, 28 Mar 2027 with "New picture at" 02:00: 03:00 that day).
  - Every existing test passes unchanged with the defaults (no order, no anchor = RO1).
- `TurnTest`: `nextSource` over an order; `orderOf` (empty, unknown names, duplicates, missing).
- `PictureTimeTest`: the anchor's date (a Set just before, at and after "New picture at": the
  same day before it, the next day from it; the DST gap and overlap hours), A's instant, and
  N(A) for each "Change every".
- `RulesSimulationTest` (SIM): `tapSet` draws an order now and then (a random order, sometimes the
  order alone, sometimes with other changes, sometimes in APOD mode) and mirrors `saveSetup`; the
  daily switch on never moves the anchor. Invariant 3 ("the newest of the turn's source") stands;
  new: **11**, after each rule-1 change a column with turns has as owner S[(N(c) − N(A)) mod n]
  in the list's order with the anchor stored then, or the next with a picture; **12**, at a Set of
  the order alone the owner of every screen is unchanged, and no other source's picture goes on
  at Set's own update (rule 2, "As soon as it's out" or a retry may still set the owner's newer
  picture); **13**, `orderFrom` changes only at a Set with the list shown whose order differs from
  the stored one or with none stored. Both with and without "As soon as it's out".
- On the tablet (rosie, OR2): the drag, the highlight, the actions' labels where `uiautomator
  dump` shows them, the order-only Set, "APOD stays", the faked night. TalkBack's "Move up" /
  "Move down" by the user on the FP6 (rosie's dump doesn't show custom actions; `SKILL.md`:
  TalkBack only by the user).

## Packages

Each reads this file's text above first (`plan-0.7.0.md`, For every package); the queue rows are
`night.md`'s.

### [ ] OR1. `:core`: the list's order in the turns, the anchor, Set keeping what's on until it (Opus, high)

After RO3 and SO3 (RO1 and SO1 changed the same functions: `dueWallpapers`, `Rules`,
`restartAndForget`). The rules above, the Decided answers and Night defaults 1–3 (What changes,
`:core`): the order in `nextSource`, `turnOf`, `pick`, `start`; the anchor's phase with N(A) from
the current setup; `restartAndForget` before and from A (only the order, keep, the top, RO1's);
the anchor's date, A's instant and N(A); the pref's parser. Defaults keep every call RO1's.
- **Files:** `Turn.kt`, `Screens.kt`, `PictureTime.kt`, `TurnTest.kt`, `ScreensTest.kt`,
  `PictureTimeTest.kt`, `RulesSimulationTest.kt`; `Store.kt` only where a `:core` signature
  changed (compiles; no anchor and the default order until OR2, so dev stays RO1's).
- **Tests:** Tests and checks above, the `:core` ones: the user's example, "APOD stays" (the Set
  and the switch off and on at 21:00), the order Set at 06:03 before the 06:07 run, the first Set
  with no anchor, from A on with "Change every", "New picture at" and a tick changed; SIM's 240
  seeds with invariants 11–13.
- **Done when:** the tests pass; the user's example, "APOD stays", the 06:03 case, the
  top-with-nothing-to-show case and the "Change every" case fail on the old code; every existing
  test passes unchanged; the report lists any SIM invariant rewritten and why.

### [ ] OR2. The drag list, its actions, the order in the draft and Set, `CLAUDE.md` (Opus, high)

After OR1. `Setup.order`, the prefs `order` and `orderFrom` (Night default 3: only while the draft
shows the list), `saveSetup` (one commit; `orderFrom` only for a new order or none stored;
`orderOnly` from the whole `Setup` but `order`, `parts` included, and `pick`; the daily switch),
`startAfresh` (leaves `orderFrom`, passes it), `setDue`; the page: Night defaults 4–7.
- **The drag:** `pointerInput` with `detectDragGestures` on the handle (vertical; no long press),
  the dragged row drawn lifted (`zIndex`, `graphicsLayer`'s `translationY`), swapped with its
  neighbour in the draft once it passes half that row's height (heights from `onSizeChanged`);
  the handle consumes the drag, so the settings pane doesn't scroll while a row moves, and does
  scroll from anywhere else. No animation library.
- **Screen readers:** `customActions` "Move up" (not on the first row) and "Move down" (not on the
  last) on the row's tick box node, calling the same move as the drag; the handle
  `clearAndSetSemantics`; ", not set yet" as Night default 5. On the last ticked row the tick box
  is disabled (greyed out); whether TalkBack offers custom actions on a disabled node is
  unverified: the user's FP6 check (if it doesn't, the actions go on a node that stays enabled).
- **The draft:** the order in `onSaveInstanceState` (a rotation and the system killing the app
  keep it), in `setupChanged` while the list shows, kept when "More NASA pictures" is turned off
  and on in the draft (as the list), not saved while it's off.
- **Files:** `Store.kt`, `MainActivity.kt`; `CLAUDE.md` (What changes); the skill's Where things
  are (`SKILL.md`), and its "End a turn" (the order and `orderFrom` in the prefs).
- **Tablet** (rosie, the debug build):
  - The drag with `input swipe` from a handle (slow, 1000 ms): the rows' order in `uiautomator
    dump`, the moved rows' "≡" and "Setup" in `tertiary` (screenshots), the pane not scrolled by
    the drag; a rotation keeps the draft order; a font scale of 2.0 (the handle stays at the
    start, the dropdown wraps).
  - Set of the order alone, the switch on: nothing set (`dumpsys wallpaper`'s id unchanged, no
    `Set: … wallpaper set` in the log), the prefs `order` and `orderFrom` (the local date of the
    next "New picture at" after the Set: today's for a Set before it, else tomorrow's), the
    records' stamps the current change time, Setup's summary in the order (screenshot).
  - **"APOD stays":** NASA, APOD, Earth, 24 h, APOD on; the order Set (clock faked to 20:00), then
    at 21:00 a Set changing only "If APOD is a video": APOD set again (the log's `Set: …`), not
    Earth, `orderFrom` unchanged; the daily switch off and on at 21:00: the same; the clock
    stepped to the anchor's alarm: NASA, one set with both flags.
  - **From A on, a Set without a new order doesn't restart:** after A, a Set that only changes
    "Change every" and one that only ticks a source: `orderFrom` unchanged after each, each puts
    the formula's turn on (not the top unless the formula says so).
  - **APOD mode:** with the switch off in the draft and no `orderFrom` (prefs edited after `am
    kill`), a Set writes neither `order` nor `orderFrom`.
  - **A faked night:** APOD, NASA and Earth on both screens, 6 h, the order NASA, APOD, Earth set
    before 00:00; the clock stepped as RO3 did (`auto_time 0`, `cmd alarm set-time` half a minute
    before each alarm, the alarm left to fire): 00:00 → Earth, 06:00 → NASA, 12:00 → APOD, each
    one set with both flags, nothing in between; the clock back and `auto_time 1` at the end.
  - The daily switch off, a Set of a new order: the top at once.
  - "Change every"'s ⓘ (screenshot); the smoke list. Not checked here: TalkBack's actions, also
    on the greyed-out last ticked row (the user, FP6).
- **Done when:** built; the tablet passes; the report lists the placeholder texts ("≡", "Move up",
  "Move down", the ⓘ) for the user.

## Seams

- **RO1–RO3** (`rotation-0.7.0.md`): this replaces RO1's "a Set never shifts the rotation" for a
  Set of a new order (and the first Set with the list shown and no anchor), and RO1's Set before
  the anchor (kept what's on); from the anchor on, every Set, the daily switch on and a screen
  with no record stay RO1's. RO1's FP6 test (`theFp6sDayOf11NovPutsApodOnAt1800`) holds (no
  `orderFrom`, the default order).
- **SO1–SO3** (`soon-0.7.0.md`): no change to the switch; it acts on whichever owner the order
  made or Set kept.
- **Track L** (`live-0.7.0.md`): L6 adds Size to `Setup` and Set opens Android's picker; "only the
  order" compares the whole `Setup` but the order, so a changed Size is "another change" (the
  picker as L6 says), and a Set of the order alone opens nothing. "With turns each picture
  crosses within its own turn" holds.
- **Track BR** and **O14** (the detector): no change (the change times don't move; a Set of the
  order alone sets nothing, so the detector sees no set).
- **T1** (SIM on the combined rules) runs after OR2; **T2** runs OR2's checks again; **R1**'s
  changelog gets a line for the order.
