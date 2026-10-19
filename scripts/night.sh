#!/usr/bin/env bash
# scripts/night.sh: APODroid's night shift (.claude/skills/build-apodroid-android/night.md; adapted from pascaline's
# scripts/night.sh). It keeps the laptop awake and loops: a fresh josh (Sonnet, one call, no tools) gets the queue,
# the results so far and the log's tail on stdin and replies with the next command; this script runs it
# (scripts/pipeline.sh only) and asks again. No session lives longer than one decision or one package.
#
#   scripts/night.sh [--until HH:MM] [--max-steps N] [--limit-wait SECONDS]   (defaults: no --until, 30, 1800)
#   scripts/night.sh --next        ask josh once and print his command; run nothing, log nothing
# Each start first checks that the main checkout has no uncommitted change the night would miss (CLAUDE.md, the
# skill with the plan, the agents, the scripts; --dirty-ok skips this), brings dev up to master and runs
# scripts/pipeline.test.sh (the preflight); a failure stops it.
#   scripts/night.sh --dry-run [--until HH:MM] [--max-steps N] [--dry-step MIN]
#                                  the steps on a simulated clock (APODROID_NIGHT_NOW=HH:MM); asks no josh
#
# The end: from --until (the next HH:MM after the start) or at step N - 2, the night closes with two fixed steps:
# the last Opus pass (when a package finished since the last one), then the report. Stops on SHIFT DONE, after the
# report, after 6 usage-limit waits, or when josh twice gives no usable command.
# Output: .coord/log.md (the state) and .coord/night-<date>.log (every command's output).
set -euo pipefail
orig_args=("$@")
max_steps=30; limit_wait=1800; until_hm=""; dry=0; dry_step=30; next_only=0; dirty_ok=0
usage() { echo "usage: scripts/night.sh [--until HH:MM] [--max-steps N] [--limit-wait S] [--dirty-ok] [--next] [--dry-run [--dry-step MIN]]" >&2; exit 2; }
while [[ $# -gt 0 ]]; do case "$1" in
  --max-steps) max_steps="$2"; shift 2;; --limit-wait) limit_wait="$2"; shift 2;; --until) until_hm="$2"; shift 2;;
  --dry-run) dry=1; shift;; --dry-step) dry_step="$2"; shift 2;; --next) next_only=1; shift;; --dirty-ok) dirty_ok=1; shift;;
  *) usage;; esac; done
[[ "$max_steps" =~ ^[0-9]+$ && "$max_steps" -ge 2 ]] || { echo "night.sh: --max-steps needs at least 2 (the closing steps)" >&2; exit 2; }
[[ -z "$until_hm" || "$until_hm" =~ ^([01][0-9]|2[0-3]):[0-5][0-9]$ ]] || { echo "night.sh: --until takes HH:MM" >&2; exit 2; }
self="$(readlink -f "$0")"; here="$(dirname "$self")"
# shellcheck source=scripts/pipeline-lib.sh
source "$here/pipeline-lib.sh"
main="$(dirname "$(git -C "$here" rev-parse --path-format=absolute --git-common-dir)")"; cd "$main"
coord="$main/.coord"; skill=".claude/skills/build-apodroid-android"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
TABLET="${APODROID_TABLET:-HA24BZ2Y}"
if [[ $dry -eq 0 && $next_only -eq 0 && -z "${APODROID_NIGHT_INHIBITED:-}" ]] && command -v systemd-inhibit >/dev/null; then
  APODROID_NIGHT_INHIBITED=1 exec systemd-inhibit --what=sleep:idle:handle-lid-switch --why="APODroid night shift" "$self" "${orig_args[@]}"
fi

# The clock: real, or simulated in a dry run.
if [[ $dry -eq 1 ]]; then fake="$(date -d "today ${APODROID_NIGHT_NOW:-$(date +%H:%M)}" +%s)"; fi
now() { if [[ $dry -eq 1 ]]; then echo "$fake"; else date +%s; fi; }
hm() { date -d "@$(now)" +%H:%M; }
deadline=""
if [[ -n "$until_hm" ]]; then
  deadline="$(date -d "$(date -d "@$(now)" +%F) $until_hm" +%s)"
  (( deadline > $(now) )) || deadline=$((deadline + 86400))  # the next HH:MM after the start
fi
until_text="${deadline:+, until $(date -d "@$deadline" '+%F %H:%M')}"

# The dev worktree, brought up to master (the user's day commits), as pipeline.sh finds it.
integ_dir() { git worktree list --porcelain | awk '/^worktree /{w=substr($0, 10)} /^branch refs\/heads\/dev$/{print w}'; }
integ="$(integ_dir)"
if [[ -z "$integ" && $dry -eq 0 ]]; then
  integ="$main/.claude/worktrees/dev"
  git show-ref --verify --quiet refs/heads/dev || git branch dev master
  git worktree add "$integ" dev >/dev/null 2>&1
fi
: "${integ:=$main}"
night="$integ/$skill/night.md"

if [[ $dry -eq 1 ]]; then
  log() { :; }
  say() { printf '%s %s\n' "$(hm)" "$*"; }
  say "DRY RUN: night.sh (max $max_steps steps$until_text); asks no josh, runs nothing, writes no log"
elif [[ $next_only -eq 1 ]]; then
  log() { :; }
  say() { printf '%s %s\n' "$(date +%H:%M)" "$*" >&2; }
else
  mkdir -p "$coord"
  out="$coord/night-$(date +%F).log"
  log() { [[ ! -s "$coord/log.md" || -z "$(tail -c1 "$coord/log.md")" ]] || echo >> "$coord/log.md"; printf -- '- %s %s\n' "$(date +%H:%M)" "$*" >> "$coord/log.md"; }
  say() { printf '%s %s\n' "$(date +%H:%M)" "$*" | tee -a "$out"; }
  "$here/devlock.sh" acquire night "night.sh-$$" 0 >/dev/null || { echo "night.sh: another night is running ($("$here/devlock.sh" status | grep '^night'))" >&2; exit 1; }
  trap '"$here/devlock.sh" release night "night.sh-$$" >/dev/null 2>&1 || true' EXIT
  printf '\n## %s night, %s\n' "$(date +%F)" "$(date +%H:%M)" >> "$coord/log.md"
  [[ -z "$(git -C "$integ" status --porcelain --untracked-files=no)" ]] || { log "STOPPED night.sh: the dev worktree $integ is not clean"; say "STOPPED: dev worktree not clean"; exit 1; }
  # The night reads master's commits only: an uncommitted plan or CLAUDE.md would be missed.
  dirty="$(git -C "$main" status --porcelain --untracked-files=no -- CLAUDE.md "$skill" .claude/agents scripts | tr '\n' ' ')"
  if [[ -n "$dirty" && $dirty_ok -eq 0 ]]; then
    log "STOPPED night.sh: uncommitted changes the night would miss: $dirty(commit them, or start with --dirty-ok)"
    say "STOPPED: uncommitted changes the night would miss: $dirty(commit them, or --dirty-ok)"; exit 1
  fi
  if ! synced="$(sync_dev "$integ")"; then log "STOPPED night.sh: $synced"; say "STOPPED: $synced"; exit 1; fi
  [[ -z "$synced" ]] || log "night.sh: $synced"
  # The preflight: every queue row resolves against dev's plan, and the scripts work (no agent is started).
  if ! (cd "$integ" && timeout 300 scripts/pipeline.test.sh) > "$coord/preflight.log" 2>&1; then
    log "STOPPED night.sh: the preflight failed (scripts/pipeline.test.sh): $(grep -m1 '^FAIL' "$coord/preflight.log" | cut -c1-200)"
    say "STOPPED: preflight failed, see $coord/preflight.log"; exit 1
  fi
  tstate="$("$ANDROID_HOME/platform-tools/adb" -s "$TABLET" get-state 2>/dev/null || true)"
  log "night.sh started (max $max_steps steps$until_text); dev at $(git -C "$integ" rev-parse --short HEAD); tablet $TABLET: ${tstate:-not attached}"
fi
export CLAUDE_CODE_PRINT_BG_WAIT_CEILING_MS=0

# josh: his agent file's text is the whole system prompt; one call, no tools, run outside the repo so no CLAUDE.md
# (46 KB) is loaded: about 1 k tokens of overhead instead of 25 k (measured 2026-10-19).
josh_prompt="$(awk '/^---$/ { n++; next } n >= 2' "$integ/.claude/agents/josh.md" 2>/dev/null || true)"
[[ -n "$josh_prompt" || $dry -eq 1 ]] || { echo "night.sh: no $integ/.claude/agents/josh.md" >&2; exit 2; }
josh_dir="$(mktemp -d)"
josh_question='Which command runs next? Reply with exactly one line: the command, or SHIFT DONE.'
josh_input() {
  echo "# night.md: the queue"; h2 "$night" "The queue"
  echo "# night.md: the commands josh replies with"; h2 "$night" "Commands josh replies with"
  echo "# Results so far (every night header and RESULT line of .coord/log.md)"
  [[ -f "$coord/log.md" ]] && grep -E '^## |RESULT ' "$coord/log.md" | tail -n 80
  echo; echo "# The last Opus pass reply"; last_pass_reply "$coord/log.md"
  echo; echo "# The log's last 20 lines"; [[ -f "$coord/log.md" ]] && tail -n 20 "$coord/log.md"
  return 0
}
ask_josh() {  # prints josh's one-line reply; waits and asks again on a usage limit
  local rc reply
  while :; do
    josh_input > "$josh_dir/in.md"; cp "$josh_dir/in.md" "$coord/josh-last-input.md" 2>/dev/null || true
    set +e
    (cd "$josh_dir" && timeout 600 claude -p --tools "" --model "${APODROID_TEST_MODEL:-sonnet}" --effort medium --system-prompt "$josh_prompt" \
      --strict-mcp-config --output-format text --no-session-persistence "$josh_question") < "$josh_dir/in.md" > "$josh_dir/out.md" 2> "$josh_dir/err.md"
    rc=$?; set -e
    if is_limit "$rc" "$josh_dir/err.md" "$josh_dir/out.md"; then limit_wait_or_stop "josh"; continue; fi
    reply="$(grep -vE '^\s*$' "$josh_dir/out.md" | tail -n 1 | sed 's/^[` ]*//; s/[` ]*$//')"
    echo "${reply:-(josh gave nothing, exit $rc)}"; return 0
  done
}
limits=0
limit_wait_or_stop() {
  limits=$((limits + 1))
  (( limits <= 6 )) || { log "STOPPED night.sh: usage limit 6 times"; say "STOPPED: usage limit 6 times"; exit 1; }
  log "night.sh: usage limit ($1), waiting $limit_wait s"; say "usage limit ($1), waiting $limit_wait s"; sleep "$limit_wait"
}

if [[ $next_only -eq 1 ]]; then
  echo "--- josh's input ($(josh_input | wc -l) lines) ---"; josh_input; echo "--- josh ---"; ask_josh; exit 0
fi

declare -A norsl=()
run_cmd() {  # <cmd>: one pipeline line; returns its exit code (75: a usage limit, after the wait)
  local cmd="$1" cmdout rc
  [[ $dry -eq 0 ]] || return 0
  cmdout="$(mktemp)"
  set +e; (cd "$integ" && eval "timeout 14400 $cmd") > "$cmdout" 2>&1; rc=$?; set -e
  cat "$cmdout" >> "$out"
  say "rc=$rc: $(grep '^RESULT ' "$cmdout" | tail -n 1 || tail -n 1 "$cmdout")"
  if [[ $rc -ne 75 ]] && ! grep -q '^RESULT ' "$cmdout"; then
    # no RESULT line: the pipeline died; the same command twice is skipped (night.md, rule 8)
    norsl["$cmd"]=$(( ${norsl["$cmd"]:-0} + 1 ))
    if (( ${norsl["$cmd"]} >= 2 )); then log "RESULT -: SKIPPED ($cmd: no RESULT line twice; do not repeat it)"
    else log "RESULT -: FAILED ($cmd: no RESULT line, rc=$rc; run it once more)"; fi
  fi
  rm -f "$cmdout"
  if [[ $rc -eq 75 ]]; then limit_wait_or_stop "pipeline"; fi
  return $rc
}
steps=0; bad=0; closing=0
tick() { steps=$((steps + 1)); [[ $dry -eq 0 ]] || fake=$((fake + dry_step * 60)); }

while :; do
  n=$((steps + 1))
  if (( closing == 0 )); then
    why=""
    if [[ -n "$deadline" ]] && (( $(now) >= deadline )); then why="--until $until_hm reached"
    elif (( steps >= max_steps - 2 )); then why="step cap $max_steps minus the two closing steps"; fi
    if [[ -n "$why" ]]; then closing=1; log "night.sh: closing ($why): the last Opus pass, then the report"; say "closing ($why)"; fi
  fi
  if (( closing == 1 )); then  # closing step 1: the last pass, when a package finished since the last one
    if [[ $dry -eq 1 ]] || [[ -n "$(since_last_pass "$coord/log.md")" ]]; then
      cmd="scripts/pipeline.sh pass"; say "night.sh #$n (closing 1 of 2): $cmd"; log "night.sh #$n (closing 1 of 2): $cmd"
      rc=0; run_cmd "$cmd" || rc=$?
      (( rc == 75 )) && continue
    else log "night.sh #$n (closing 1 of 2): no package since the last pass, no pass"; fi
    closing=2; tick; continue
  fi
  if (( closing == 2 )); then  # closing step 2: the report
    cmd="scripts/pipeline.sh report"; say "night.sh #$n (closing 2 of 2): $cmd"; log "night.sh #$n (closing 2 of 2): $cmd"
    rc=0; run_cmd "$cmd" || rc=$?
    (( rc == 75 )) && continue
    tick; log "SHIFT DONE (night.sh, closed after $steps steps)"; say "done"; exit 0
  fi

  if [[ $dry -eq 1 ]]; then cmd='scripts/pipeline.sh <next step>   (josh)'; else cmd="$(ask_josh)"; fi
  say "josh #$n: $cmd"; log "josh #$n: $cmd"
  if [[ $dry -eq 1 ]]; then tick; continue; fi
  case "$cmd" in
    "SHIFT DONE") log "SHIFT DONE (night.sh)"; say "done"; exit 0;;
    scripts/pipeline.sh\ *)
      if unsafe "$cmd"; then
        bad=$((bad + 1)); log "night.sh: refused josh's line (shell metacharacters, a backtick or a dollar sign)"
        (( bad >= 2 )) && { log "STOPPED night.sh: josh's line refused twice"; say "STOPPED: josh's line refused twice"; exit 1; }
      else bad=0; run_cmd "$cmd" || true; fi;;
    *)
      bad=$((bad + 1)); log "night.sh: josh gave no pipeline command"
      (( bad >= 2 )) && { log "STOPPED night.sh: josh gave no pipeline command twice"; say "STOPPED: josh gave no command twice"; exit 1; };;
  esac
  tick
done
