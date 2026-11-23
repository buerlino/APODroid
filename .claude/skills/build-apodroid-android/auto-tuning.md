# Auto, tuned from the user's picks (plan, 2026-11-23)

Status: **a plan, nothing built.** Release placement open (0.10.0 is a feature freeze; the user decides).
The data is in `/home/nordmu/Documents/source99/APODroid/private/auto-2026-11-23/` (gitignored, never committed).

## Why

Two misses on the FP6:
- 21 Nov, "Voyager's Neptune", went upright; the user wanted it sideways. The word table scores it +28.8
  against −2.0.
- 23 Nov, "The Emergence of Dark Nebula Lynds 1251", a 2×3 panel comparison, went on as Fill, cutting the
  panels; the user wanted it whole.

## The data (what is measured, 2026-11-23)

- `posts.json`: every APOD from 3 Apr 2025 to 23 Nov 2026 (600 posts, 42 video), parsed as `:core` does.
  The pictures at `?h=640` are in `pics/`, `../pictures/` and `../pictures-oct-nov/`.
- **The picks** (`picks/picks/*.json`, `picks-both.json`): 300 posts, each shown twice in a random order
  (repeats at least 120 apart; the picker https://claude.ai/code/artifact/fe5e5e9d-9154-4623-8eb1-51d9fa08cf07).
  Wide pictures had four choices (Fill, Fill sideways, Whole, Whole sideways); portrait and square ones
  had two (Fill, Whole). The question: "which would you want as your wallpaper, the way you hold your
  phone?"
  - The sample: every seam hit and every keyword hit (title, alt, the explanation's layout words), then
    random posts up to 300 (`pick-sample.json`). **It is enriched for "whole"**, so the share of
    "whole" in it (19%) is above the archive's.
- **Reliability:**
  - The same pick both times: 264 of 300 (88%); κ 0.75 for wide pictures, 0.85 for the rest.
  - Whole or Fill: 89% the same. Sideways or upright: 98% the same (143 of 146).
  - 22 of the 36 mismatches involve "Not sure".
  - Against S1's labels of 17 Nov: 102 of 108 agree.
  - **The labels are the posts picked the same, sure, both times:** 250 posts, 48 of them whole.
- **Whole takes the turn:** of 22 wide pictures picked whole, 19 were picked sideways. Every portrait
  whole is upright.
- **What already predicts "whole"** (on the 250):

  | Signal | Fires | Whole | Precision | Catches of the 48 |
  |---|---|---|---|---|
  | A seam and a layout word in the title or alt | 6 | 6 | 100% | 12% |
  | A seam | 25 | 18 | 72% | 38% |
  | The picture's shape (Fill shows little) | | | AUC 0.54 | |
  | A "whole" word table, leave-one-out | top 10 | 5 | AUC 0.78 | |
  | Words + seam + shape, leave-one-out | top 10 / top 20 | 8 / 15 | AUC 0.82 | |

- **The turn on the new picks** (139 consistent wide pictures): today's table turns 2 the user wants
  upright (9 May 2025 IXPE, 26 Apr 2026 Mystic Mountain) and keeps 35 upright that the user would turn.
  Neptune: Fill sideways both times.

## The goal

Auto stays one mode with no new setting or text. Its order stays **orientation, then position, then
size**, and size gets a third outcome:

1. **Orientation:** ⛶'s pill, else Auto's turn (APOD, landscape, the word table says no up). Retrained,
   below.
2. **Position:** unchanged (the subject; ⛶'s drag).
3. **Size:** Fill, or **whole on the orientation from step 1** (new: "Auto's whole"), or the limited layout
   with Keep pictures sharp.

The success measure is fixed before any tuning:

- **Wrongly turned** (the user wants it upright, Auto turns it): at most 1% of the upright labels.
- **Wrongly whole** (the user wants Fill, Auto shows it whole): at most 2% of the Fill labels.
- **Then as many right turns and right wholes as those allow.**

Why so strict on whole: a wrong whole is a smaller picture with blurred bands every time it's on, while
a missed whole is today's Fill.

## The analysis (desktop, Python, `private/`; nothing in the repo)

**A1. One label set.** `auto-labels-2026-11-23.json`, per post:
- `turn`: yes, no or none. The new picks where sure and the same both times; else S1's label.
- `whole`: yes, no or none. New picks only.
- A conflict between S1 and the new picks: the new picks win (made twice). Listed, not hidden.
- 21 Nov (Neptune: turn yes, whole no) and 23 Nov (whole yes) are in the picks already.
- 22 Nov (Jupiter, Juno) is not labelled; asked of the user.

**A2. The turn.**
- Retrain the word table on A1's `turn` labels: about 380 posts against S1's 282.
- The threshold is chosen **inside each leave-one-out fold** (nested), at the strictest value that keeps
  that fold's training set within the 1% budget, so the reported numbers aren't tuned on the test post.
- Report wrongly turned and right turns against today's table, and **the flips on all 558 picture posts**
  (which posts change orientation, either way, listed with titles for the user).
- Image features (edge coherence, anisotropy) only if nested leave-one-out shows more right turns at the
  same wrong rate. S1's check found none.

**A3. Auto's whole.**
- Candidates, each by nested leave-one-out on A1's `whole` labels:
  - (a) seam and layout word;
  - (b) a "whole" word table (Bernoulli naive Bayes as `UpWords`, its own table and threshold);
  - (c) (b) or (a);
  - (d) a logistic model on the word score, seams and Fill's shown share.
- Each at the threshold meeting the 2% budget.
- **The sample's enrichment** is corrected for when estimating how often each fires in the archive:
  report the rate on all 558 picture posts and on the random part of the sample alone.
- With 48 positives, any precision is uncertain: report the Wilson 95% interval and decide on its lower
  bound.
- **If no candidate's lower bound clears 90% precision:** ship (a) alone, or nothing, and say so.

**A4. Held-out check, prospective.**
- The rule chosen in A2 and A3 is frozen (the table files and thresholds hashed into the plan).
- Then the APODs from 24 Nov to about 24 Dec, unseen by the analysis, get the same twice-shown picker.
- Auto's frozen answer is compared with the user's.
- **Pass:** within the budgets above; else back to A2 and A3 with all the data, and a new hold-out.
- A1–A3's figures are in-sample for the design choices; A4 is the honest number.

**A5. An independent re-run.** A fresh agent recomputes A2–A4 from the frozen files and the scripts
(`scripts/`), without this plan's numbers, and reports differences.

## The build (only after the user's go, A4's pass and the release decision)

**B1. `:core`.**
- `up-words.tsv` replaced by A2's table (a new date in its header).
- `UpWords.kt` unchanged but for the threshold if A2 moves it.
- A new `WholeWords.kt` with `whole-words.tsv` and `wantsWhole(title, alt, explanation)`, if A3 picks a
  word table.
- `Panels.kt` with `seams(grey, w, h)` (the line and cut test, on the 512 px grey copy; pure arithmetic,
  only what Android 10 has), if A3 uses seams.
- `wholeOf(noted, size, source, …)` beside `sidewaysOf`: ⛶'s noted whole, else Auto's whole while the
  stored Size is Auto and the source APOD, else false.
- Tests:
  - the expected scores;
  - with `private/` present, the labelled posts score as the builder says (as `readsEveryRealPost`);
  - made-up panel pictures: a 2×3 grid with 2 px gutters, a before/after pair, a horizon, a ring plane;
  - the truth table of `wholeOf`: noted whole {true, false, none} × Size {Centre, Auto, Live} × source
    {APOD, NASA, Earth, my picture} × signal {yes, no}.

**B2. The app.**
- **`Part.whole` gets three values,** as `Part.sideways` did (SW3):
  - the pref `<source>.whole` missing means none, so Auto decides;
  - a part from before keeps its stored true or false (no migration; per post, so gone in a day).
- **Where the seams are noted:** `noteSubject` notes them in the same decode (`<source>.subjectWhole`);
  a post noted before is noted again once, as SH2c did.
- **Every still set** (the job's, the page's, Set's, ⛶'s ✓; B27's keep) uses `wholeOf`. The orientation
  comes first (Auto's turn), then whole on it, so a wide panel picture goes on whole and sideways.
  Keep pictures sharp limits it by `limitedFit`, as today's whole picture. The darkening is unchanged
  (the whole picture's measure).
- **Never:** under Centre or Live, Live's copy, NASA's and Earth's pictures, "my picture".
- **⛶:** "Whole picture" opens on the value used. It isn't highlighted (it's the stored setup's), and a
  touched toggle notes true or false. ✓'s no-change check compares the values used.
- **The log:** ", whole, auto whole" when Auto chose it.
- **No new text or setting.** The ⓘ of Size: "Auto: …" stays as approved unless the user wants it to
  mention whole (a text change needs the user's approval).
- **CLAUDE.md:**
  - Wallpaper: Auto's whole.
  - Choosing the part: whole's three values.
  - Settings: Size.
  - Next features.
- **The maps:** `page.md` (⛶ under Auto's whole) and `background.md` (the note, the log line).

**B3. Device checks** (SH4's way, the Android 14 emulator, `scripts/sh4_*.py`):
- A made-up 2×3 panel picture, 2160×2700, stored as APOD's post with a layout text: Auto whole,
  upright; Centre Fill.
- The same at 3000×2000: whole, sideways.
- ⛶ off, then on, overriding.
- Keep pictures sharp on: the limit.
- Home, Lock, Both; the job's set and Set's.
- Pixels checked on `files/debug-set.png`; a plain post takes 0.9's paths byte for byte.

**B4. The update from 0.10:** a stored part's whole stays explicit; posts get their seams noted once; the
turn table's flips listed in the changelog's words ("Auto turns some pictures differently").

## Risks

- **Overfitting one person's taste on 250 posts:** nested leave-one-out and A4's hold-out. Other users
  have ⛶.
- **The enriched sample** inflates apparent precision: A3 reports on the archive and the random part.
- **A wrong whole costs more than a missed one:** the 2% budget and the lower bound.
- **The text changes after a post** (NASA edits alt texts): already handled; the alt is stored with the
  post and a changed alt is stored.
- **The feature freeze:** B waits for the release decision.

## Where it stands (2026-11-23, paused to save usage)

- **A1 done:** `auto-labels-2026-11-23.json` (`scripts/a1.py`).
  - 314 turn labels (228 sideways OK, 86 upright), not about 380 as A2 says.
  - 250 whole labels (48 whole).
  - Six turn conflicts with S1, the picks winning: 2025-08-23, 2025-11-22, 2025-12-03, 2026-08-11 and
    2026-11-05 now sideways; 2026-04-26 now upright.
- **The hold-out pool:** 400 older APODs (28 Feb 2024 to 2 Apr 2025) in `json-holdout/p7–p10.json`,
  unparsed, no pictures fetched yet, unseen.
- **The independent review (A5's kind, of this plan)** was started and stopped unfinished; run it again
  first.
- **Next:** the review, then A2 and A3, then freeze, then the hold-out picker (300 of the 400, twice),
  then the comparison, then B in 0.10.0.

## Decided (user, 2026-11-23)

1. **In 0.10.0**, because 0.10.0's real week tests it.
2. **22 Nov (Jupiter from Juno): turn** (A1's label).
3. The budgets: explained to the user; open until the reviewer's findings are in.
4. **A4 agreed in principle.** A month of new posts doesn't fit 0.10.0's week, so the proposal is a
   hold-out from the archive instead (unlabelled older APODs, the rule frozen first, the same picker),
   with the next posts as a live check during the real week.
