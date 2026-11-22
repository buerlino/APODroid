#!/usr/bin/env bash
# Checks scripts/pipeline.sh, night.sh and pipeline-lib.sh without starting an agent: every call is a --dry-run, a
# helper on made-up input, or fails before a run. Needs the dev worktree (pipeline.sh makes it from master).
#   scripts/pipeline.test.sh
set -uo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/pipeline-lib.sh
source "$here/pipeline-lib.sh"
fails=0; count=0
fail() { echo "FAIL: $*" >&2; fails=$((fails + 1)); }
tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT

# Every queue row resolves: the dry run exits 0, names the branch and holds the row's sections.
"$here/pipeline.sh" O4 --dry-run > "$tmp/o4" 2>&1 || fail "O4 --dry-run: $(tail -n 1 "$tmp/o4")"
integ="$(git worktree list --porcelain | awk '/^worktree /{w=substr($0, 10)} /^branch refs\/heads\/dev$/{print w}')"
[[ -n "$integ" ]] || { echo "no dev worktree" >&2; exit 1; }
night="$integ/.claude/skills/build-apodroid-android/night.md"
while IFS= read -r qid; do
  count=$((count + 1))
  out="$("$here/pipeline.sh" "$qid" --dry-run 2>&1)"; rc=$?
  [[ $rc -eq 0 ]] || { fail "$qid --dry-run: exit $rc: $(tail -n 1 <<<"$out")"; continue; }
  grep -q "branch wp/${qid,,}," <<<"$out" || fail "$qid: dry run names no branch wp/${qid,,}"
  grep -qE "^## Your task: package $qid " <<<"$out" || fail "$qid: no task heading"
  grep -qE "^### \[[ x]\] " <<<"$out" || fail "$qid: the bundle holds no plan section"
  grep -qE '^## For (winston|rosie)' <<<"$out" && grep -q '^## Definition of done' <<<"$out" || fail "$qid: the bundle lacks night.md's sections"
done < <(awk -F'|' '/^## / { q = ($0 ~ /^## The queue/); next } q && /^\| *[0-9]+ *\|/ { gsub(/ /, "", $3); print $3 }' "$night")
[[ $count -gt 0 ]] || fail "no queue rows found in $night"

# B26 takes plan-0.7.0.md's section, O1 its track file's (a file beside the plan); O4's row says opus, high, no
# tablet. A Spec naming a file that isn't there fails.
out="$("$here/pipeline.sh" B26 --dry-run 2>&1)"
grep -qE '^### \[[ x]\] B26\.' <<<"$out" || fail "B26: the bundle lacks plan-0.7.0.md's B26"
out="$("$here/pipeline.sh" O1 --dry-run 2>&1)"
grep -qE '^### \[[ x]\] O1\. Declutter' <<<"$out" && ! grep -qE '^### \[[ x]\] O2\.' <<<"$out" || fail "O1: the bundle lacks on-time-0.7.0.md's O1 alone"
grep -q '^## For every package' <<<"$out" || fail "O1: the bundle lacks plan-0.7.0.md's preamble"
grep -q 'winston (opus, high), branch wp/o4, tablet no' "$tmp/o4" || fail "O4: not opus, high, tablet no: $(head -n 1 "$tmp/o4")"
plan="$(ls "$integ"/.claude/skills/build-apodroid-android/plan-*.md | sort -V | tail -n1)"
spec_text "$plan" /dev/null "nope-0.7.0.md X1" > /dev/null 2>&1 && fail "spec_text accepts a missing file"
spec_text "$plan" /dev/null "../night.md X1" > /dev/null 2>&1 && fail "spec_text accepts a path outside the skill"
# A device package has rosie as its worker, with her rules in the bundle.
if [[ -n "$(queue_row "$night" O15)" ]]; then
  out="$("$here/pipeline.sh" O15 --dry-run 2>&1)"
  grep -q 'package O15: rosie (opus, high), branch wp/o15, tablet no' <<<"$out" || fail "O15: not rosie, opus, high: $(head -n 1 <<<"$out")"
  grep -q '^## For rosie: the tablet checks (night.md)' <<<"$out" || fail "O15: the bundle lacks rosie's rules"
fi
# A fix package takes its package's row and the flags; a made-up ID without a task fails with exit 2.
out="$("$here/pipeline.sh" O1f1 --model sonnet --tablet no --dry-run "Fix the thing in Store.kt" 2>&1)"
grep -q 'winston (sonnet, medium), branch wp/o1f1, tablet no' <<<"$out" || fail "O1f1: wrong defaults: $(head -n 1 <<<"$out")"
grep -q '^## The package it fixes: O1' <<<"$out" || fail "O1f1: no section of O1 for context"
out="$("$here/pipeline.sh" Z9 --dry-run 2>&1)"; rc=$?
[[ $rc -eq 2 ]] && grep -q 'Z9' <<<"$out" || fail "made-up ID: exit $rc: $out"
out="$("$here/pipeline.sh" O1 --model gpt --dry-run 2>&1)"; rc=$?
[[ $rc -eq 2 ]] || fail "a bad model is accepted: exit $rc"

# The log helpers on a made-up log.
cat > "$tmp/log.md" <<'EOF'
## 2026-10-19 night, 22:00
- 22:30 RESULT #1 M1: MERGED abc1234 (tablet SKIP)
- 23:30 RESULT #2 M2: LIMIT
- 23:40 RESULT #3 M2: NOT MERGED (the check failed after two fix rounds; branch wp/m2 kept)
- 23:50 RESULT pass 1: done (exit 0)
- 23:51 Opus pass 1 reply:
    OPUS PASS 1
    FIX M2f1: model opus, effort high, tablet yes, branch wp/m2, prompt: fix it
    NOTES: none
- 23:52 josh #5: scripts/pipeline.sh M2f1
- 23:59 RESULT #4 M2f1: MERGED def5678 (tablet PASS)
EOF
[[ "$(since_last_pass "$tmp/log.md")" == "#4 M2f1: MERGED def5678 (tablet PASS)" ]] || fail "since_last_pass: $(since_last_pass "$tmp/log.md")"
[[ "$(next_pass "$tmp/log.md")" == 2 ]] || fail "next_pass: $(next_pass "$tmp/log.md")"
last_pass_reply "$tmp/log.md" | grep -q 'FIX M2f1: model opus' || fail "last_pass_reply misses the FIX line"
last_pass_reply "$tmp/log.md" | grep -q 'josh #5' && fail "last_pass_reply runs past the reply"
head -n 3 "$tmp/log.md" > "$tmp/log2.md"
[[ "$(since_last_pass "$tmp/log2.md" | wc -l)" -eq 1 ]] || fail "since_last_pass counts a LIMIT as finished"

# The regression counts the merges since the last one that reached the tablet; its dry run starts nothing.
cat > "$tmp/log3.md" <<'EOF2'
- 22:30 RESULT #1 M1: MERGED abc1234 (tablet SKIP)
- 23:00 RESULT regression 1: NOT RUN (the tablet HA24BZ2Y is not attached; on the morning list)
- 23:30 RESULT #2 M2: NOT MERGED (the check failed after two fix rounds; branch wp/m2 kept)
- 23:40 RESULT #3 M3: MERGED 1234abc (tablet PASS)
EOF2
[[ "$(merged_since_regression "$tmp/log3.md" | paste -sd';')" == "#1 M1: MERGED abc1234 (tablet SKIP);#3 M3: MERGED 1234abc (tablet PASS)" ]] || fail "merged_since_regression: $(merged_since_regression "$tmp/log3.md")"
[[ "$(next_regression "$tmp/log3.md")" == 1 ]] || fail "next_regression counts a NOT RUN: $(next_regression "$tmp/log3.md")"
echo '- 23:50 RESULT regression 1: FIX (exit 0; x)' >> "$tmp/log3.md"
[[ -z "$(merged_since_regression "$tmp/log3.md")" && "$(next_regression "$tmp/log3.md")" == 2 ]] || fail "a FIX regression doesn't end the count"
out="$("$here/pipeline.sh" regression --dry-run 2>&1)"; rc=$?
[[ $rc -eq 0 ]] && grep -qE '^RESULT regression [0-9]+: (DRY RUN|NOTHING)' <<<"$out" || fail "regression --dry-run: exit $rc: $out"

# josh's lines: plain commands pass, metacharacters don't.
unsafe 'scripts/pipeline.sh M1' && fail "unsafe refuses a plain command"
unsafe 'scripts/pipeline.sh M2f1 --model opus "Fix Store.kt line 473; the clock (one reading) & the hold"' && fail "unsafe refuses ; & ( ) inside double quotes"
unsafe 'scripts/pipeline.sh M1; rm -rf x' || fail "unsafe passes a ; outside quotes"
unsafe 'scripts/pipeline.sh M1 "$(id)"' || fail "unsafe passes a dollar sign"
unsafe 'scripts/pipeline.sh M1 "`id`"' || fail "unsafe passes a backtick"

# Only the audited commit is merged: a branch left behind a detached HEAD is refused.
g="$tmp/heads"; mkdir -p "$g"; git -C "$g" init -q -b main
gcm() { git -C "$1" -c user.name=t -c user.email=t@t commit -q --allow-empty -m "$2"; }
gcm "$g" base; git -C "$g" branch wp/x; git -C "$g" worktree add -q "$tmp/heads-wt" wp/x
gcm "$tmp/heads-wt" on-branch; tip="$(git -C "$tmp/heads-wt" rev-parse HEAD)"
audited_head_check "$tmp/heads-wt" wp/x "$tip" >/dev/null || fail "audited_head_check refuses the audited branch"
git -C "$tmp/heads-wt" checkout -q --detach; gcm "$tmp/heads-wt" detached
audited_head_check "$tmp/heads-wt" wp/x "$(git -C "$tmp/heads-wt" rev-parse HEAD)" >/dev/null && fail "audited_head_check passes a detached HEAD"

# A pass reply is logged from its OPUS PASS line on.
printf 'Here it is.\n\nOPUS PASS 9\nVERDICT: M1 ok\n' > "$tmp/reply.md"
[[ "$(from_opus_pass "$tmp/reply.md" | head -n 1)" == "OPUS PASS 9" ]] || fail "from_opus_pass keeps the line before OPUS PASS"

# night.sh closes with the last pass and the report, at --until (across midnight) and at the step cap minus 2.
out="$(APODROID_NIGHT_NOW=23:00 "$here/night.sh" --dry-run --until 01:00 2>&1)"; rc=$?
[[ $rc -eq 0 ]] || fail "night.sh --dry-run --until: exit $rc: $out"
grep -q '^01:00 closing (--until 01:00 reached)' <<<"$out" || fail "night.sh --until 01:00 from 23:00 doesn't close at 01:00: $out"
grep -q '^01:00 night.sh #5 (closing 1 of 3): scripts/pipeline.sh pass$' <<<"$out" || fail "night.sh: no last pass as closing step 1: $out"
grep -q '^01:30 night.sh #6 (closing 2 of 3): scripts/pipeline.sh regression$' <<<"$out" || fail "night.sh: no regression as closing step 2: $out"
grep -q '^02:00 night.sh #7 (closing 3 of 3): scripts/pipeline.sh report$' <<<"$out" || fail "night.sh: no report as closing step 3: $out"
[[ "$(grep -c 'josh #' <<<"$out")" -eq 4 ]] || fail "night.sh asks josh after the deadline: $out"
out="$(APODROID_NIGHT_NOW=22:00 "$here/night.sh" --dry-run --max-steps 5 2>&1)"
grep -q 'josh #2: ' <<<"$out" && ! grep -q 'josh #3: ' <<<"$out" && grep -q 'night.sh #3 (closing 1 of 3)' <<<"$out" && grep -q 'night.sh #4 (closing 2 of 3)' <<<"$out" && grep -q 'night.sh #5 (closing 3 of 3)' <<<"$out" || fail "night.sh --max-steps 5: no closing at steps 3 to 5: $out"
grep -q '#6' <<<"$out" && fail "night.sh --max-steps 5 runs a sixth step"

# The usage limit (2026-11-22: the CLI's line on stdout, exit 1, wasn't recognised, so josh's reply was
# taken for a command and the night stopped): is_limit and the wait until the reset it names.
lim="$(mktemp -d)"; echo "You've hit your 5-hour limit · resets 4:40am (Europe/Zurich)" > "$lim/out.md"; : > "$lim/err.md"
echo "The pass found nothing; a usage limit is the night's to handle." > "$lim/ok.md"
is_limit 1 "$lim/err.md" "$lim/out.md" || fail "is_limit misses the CLI's limit line with exit 1"
is_limit 0 "$lim/err.md" "$lim/out.md" || fail "is_limit misses the CLI's limit line with exit 0"
is_limit 0 "$lim/ok.md" && fail "is_limit takes a good reply that mentions a usage limit"
now="$(date -d '2026-11-22 02:26' +%s)"
[[ "$(limit_wait_seconds "$now" "$lim/out.md")" -eq $(( (2 * 3600 + 14 * 60) + 300 )) ]] || fail "limit_wait_seconds 02:26 to 04:40 + 5 min: $(limit_wait_seconds "$now" "$lim/out.md")"
now="$(date -d '2026-11-22 23:30' +%s)"
[[ "$(limit_wait_seconds "$now" "$lim/out.md")" -eq $(( (5 * 3600 + 10 * 60) + 300 )) ]] || fail "limit_wait_seconds over midnight: $(limit_wait_seconds "$now" "$lim/out.md")"
now="$(date -d '2026-11-22 12:00' +%s)"
[[ -z "$(limit_wait_seconds "$now" "$lim/out.md")" ]] || fail "limit_wait_seconds waits more than 6 h"
[[ -z "$(limit_wait_seconds "$now" "$lim/ok.md")" ]] || fail "limit_wait_seconds without a limit line"
echo "You've hit your 5-hour limit · resets 5pm (Europe/Zurich)" > "$lim/out.md"
[[ "$(limit_wait_seconds "$now" "$lim/out.md")" -eq $(( 5 * 3600 + 300 )) ]] || fail "limit_wait_seconds of a whole hour: $(limit_wait_seconds "$now" "$lim/out.md")"
rm -rf "$lim"

# The agent files: the four exist, none forces the background, josh's text is a whole prompt.
for a in josh winston susanna rosie; do
  f="$integ/.claude/agents/$a.md"; [[ -f "$f" ]] || { fail "no $f"; continue; }
  grep -q '^background: *true' "$f" && fail "$a.md sets background: true"
done
[[ "$(awk '/^---$/ { n++; next } n >= 2' "$integ/.claude/agents/josh.md" | wc -w)" -gt 50 ]] || fail "josh.md has no prompt body"

[[ $fails -eq 0 ]] || { echo "pipeline.test.sh: $fails failure(s), $count queue rows" >&2; exit 1; }
echo "pipeline.test.sh: ok ($count queue rows)"
