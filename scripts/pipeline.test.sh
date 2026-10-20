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
"$here/pipeline.sh" M1 --dry-run > "$tmp/m1" 2>&1 || fail "M1 --dry-run: $(tail -n 1 "$tmp/m1")"
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

# F2 takes the plan's F section and features.md's item 4; M1's row says opus, high, no tablet.
out="$("$here/pipeline.sh" F2 --dry-run 2>&1)"
grep -qE '^### \[[ x]\] F2\.' <<<"$out" && grep -qE '^### \[[ x]\] 4\. Wi-Fi only' <<<"$out" || fail "F2: the bundle lacks the plan's F2 or features.md's item 4"
grep -q 'winston (opus, high), branch wp/m1, tablet no' "$tmp/m1" || fail "M1: not opus, high, tablet no: $(head -n 1 "$tmp/m1")"
# A device package has rosie as its worker, with her rules in the bundle.
if [[ -n "$(queue_row "$night" M0)" ]]; then
  out="$("$here/pipeline.sh" M0 --dry-run 2>&1)"
  grep -q 'package M0: rosie (opus, medium), branch wp/m0, tablet no' <<<"$out" || fail "M0: not rosie, opus, medium: $(head -n 1 <<<"$out")"
  grep -q '^## For rosie: the tablet checks (night.md)' <<<"$out" || fail "M0: the bundle lacks rosie's rules"
fi
# A fix package takes its package's row and the flags; a made-up ID without a task fails with exit 2.
out="$("$here/pipeline.sh" M2f1 --model sonnet --tablet no --dry-run "Fix the thing in Store.kt" 2>&1)"
grep -q 'winston (sonnet, high), branch wp/m2f1, tablet no' <<<"$out" || fail "M2f1: wrong defaults: $(head -n 1 <<<"$out")"
grep -q '^## The package it fixes: M2' <<<"$out" || fail "M2f1: no section of M2 for context"
out="$("$here/pipeline.sh" Z9 --dry-run 2>&1)"; rc=$?
[[ $rc -eq 2 ]] && grep -q 'Z9' <<<"$out" || fail "made-up ID: exit $rc: $out"
out="$("$here/pipeline.sh" M1 --model gpt --dry-run 2>&1)"; rc=$?
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

# The agent files: the four exist, none forces the background, josh's text is a whole prompt.
for a in josh winston susanna rosie; do
  f="$integ/.claude/agents/$a.md"; [[ -f "$f" ]] || { fail "no $f"; continue; }
  grep -q '^background: *true' "$f" && fail "$a.md sets background: true"
done
[[ "$(awk '/^---$/ { n++; next } n >= 2' "$integ/.claude/agents/josh.md" | wc -w)" -gt 50 ]] || fail "josh.md has no prompt body"

[[ $fails -eq 0 ]] || { echo "pipeline.test.sh: $fails failure(s), $count queue rows" >&2; exit 1; }
echo "pipeline.test.sh: ok ($count queue rows)"
