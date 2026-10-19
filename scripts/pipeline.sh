#!/usr/bin/env bash
# scripts/pipeline.sh: one work package end to end, with fresh headless agents and small contexts
# (.claude/skills/build-apodroid-android/night.md; adapted from pascaline's scripts/pipeline.sh).
#
#   scripts/pipeline.sh <ID> [options] ["<task text>"]
#       A queue package: its row in night.md's queue gives model, effort, tablet and spec.
#       A fix package (<ID>f1, <ID>f2, ...): the task text is the fix; the defaults come from <ID>'s row.
#       --model M         winston's model: opus or sonnet
#       --effort E        low, medium or high
#       --tablet T        yes or no: rosie runs the tablet checks before the merge
#       --branch B        the branch (default wp/<id>); an existing one is resumed from its last commit
#       --audit-model M   susanna's model: auto (default: opus when winston runs opus or the diff touches
#                         core/src/main/, Store.kt or DailyJob.kt, else sonnet; a floor), sonnet or opus
#       --no-worker       review-only: the branch exists; dev is merged into it first (a conflict is a fix round)
#       --no-merge        stop after the checks; print the merge command
#       --dry-run         print the bundle and the plan; start nothing, make no worktree
#   scripts/pipeline.sh pass       an Opus pass by susanna on dev over the packages since the last pass
#   scripts/pipeline.sh report     the night's report, written by susanna to .coord/reports/
#
# Lifecycle: bundle → winston in his own worktree from dev → the Gradle check (:core tests, lint, the R8
# release build) → susanna audits the diff → rosie checks the tablet (when the row says so and the tablet is
# attached) → merge of the audited commit into dev → worktree and branch removed. The check, the audit and the
# tablet share at most two fix rounds by winston, each fed only the failing part.
# Output: one RESULT line on stdout (also in .coord/log.md), on every exit. Exit 0 merged, 1 not merged,
# 2 not started or failed early, 75 usage limit (night.sh waits and retries).
set -euo pipefail
here="$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")"
# shellcheck source=scripts/pipeline-lib.sh
source "$here/pipeline-lib.sh"

main="$(dirname "$(git -C "$here" rev-parse --path-format=absolute --git-common-dir)")"
cd "$main"
coord="$main/.coord"
mkdir -p "$coord/runs" "$coord/passes" "$coord/reports"
skill=".claude/skills/build-apodroid-android"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
TABLET="${APODROID_TABLET:-HA24BZ2Y}"
adb="$ANDROID_HOME/platform-tools/adb"
gradle_check=(:core:test :app:lintRelease :app:assembleRelease)
export CLAUDE_CODE_PRINT_BG_WAIT_CEILING_MS=0
claude_common=(--permission-mode auto --output-format text --no-session-persistence --strict-mcp-config
  --settings "$here/agent-settings.json")
lock() { "$here/devlock.sh" "$@" >/dev/null; }

log() { [[ ! -s "$coord/log.md" || -z "$(tail -c1 "$coord/log.md")" ]] || echo >> "$coord/log.md"; printf -- '- %s %s\n' "$(date +%H:%M)" "$*" >> "$coord/log.md"; }
runlog() { printf '%s %s\n' "$(date +%H:%M:%S)" "$*" >> "$run/run.log"; }
result() { [[ "${dry:-0}" -eq 1 ]] || log "RESULT $*"; echo "RESULT $*"; }
runno=""; id=""
die() {
  echo "pipeline: $*" >&2
  if [[ -n "$runno" ]]; then result "#$runno $id: NOT MERGED ($*)"; else result "-: NOT STARTED ($*)"; fi
  exit 2
}

# The worktree where dev is checked out; the main checkout stays on master, untouched.
integ_dir() { git worktree list --porcelain | awk '/^worktree /{w=substr($0, 10)} /^branch refs\/heads\/dev$/{print w}'; }
ensure_integ() {
  integ="$(integ_dir)"
  if [[ -z "$integ" ]]; then
    integ="$main/.claude/worktrees/dev"
    git show-ref --verify --quiet refs/heads/dev || git branch dev master
    git worktree add "$integ" dev >/dev/null 2>&1 || die "cannot add the dev worktree at $integ"
  fi
  [[ -z "$(git -C "$integ" status --porcelain --untracked-files=no)" ]] || die "the dev worktree $integ is not clean"
}
# The gitignored private/ (real responses for SourcesTest, the tablet scripts) is linked into a worktree.
link_shared() { [[ -e "$1/private" || ! -d "$main/private" ]] || ln -s "$main/private" "$1/private"; }

# ---------- pass ----------
if [[ "${1:-}" == "pass" ]]; then
  dry=0; [[ "${2:-}" == "--dry-run" ]] && dry=1
  ensure_integ
  n="$(next_pass "$coord/log.md")"
  since="$(since_last_pass "$coord/log.md" | paste -sd';' | sed 's/;/; /g')"
  [[ -n "$since" ]] || { result "pass $n: NOTHING (no package finished since the last pass)"; exit 0; }
  prompt="Opus pass $n of the night shift: follow \"Opus pass\" in $skill/night.md for these pipeline results: $since. You run in the dev worktree $integ; the run folders are in $coord/runs/. Take scripts/devlock.sh acquire host-cpu pass-$n 1800 around the Gradle check and scripts/devlock.sh acquire integ pass-$n 600 around a doc commit, and release each right after. Reply in exactly the form the section gives."
  if [[ $dry -eq 1 ]]; then echo "DRY RUN: Opus pass $n on: $since"; echo "RESULT pass $n: DRY RUN"; exit 0; fi
  run="$coord/passes/pass-$n"; mkdir -p "$run"
  log "Opus pass $n: susanna (opus) on: $since"
  set +e
  (cd "$integ" && timeout 7200 claude -p --disallowedTools Agent --add-dir "$coord" \
    --agent susanna --model opus --effort medium "${claude_common[@]}" "$prompt") > "$run/reply.md" 2> "$run/stderr.log"
  rc=$?; set -e
  if is_limit "$rc" "$run/stderr.log" "$run/reply.md"; then result "pass $n: LIMIT"; exit 75; fi
  { echo "- $(date +%H:%M) Opus pass $n reply:"; from_opus_pass "$run/reply.md" | sed 's/^/    /'; } >> "$coord/log.md"
  from_opus_pass "$run/reply.md"; result "pass $n: done (exit $rc)"; exit 0
fi

# ---------- report ----------
if [[ "${1:-}" == "report" ]]; then
  ensure_integ
  night_date="$(grep -oE '^## [0-9-]+ night' "$coord/log.md" | tail -n1 | cut -d' ' -f2)"
  out="$coord/reports/${night_date:-$(date +%F)}-night.md"; run="$coord/reports/.${night_date:-$(date +%F)}"; mkdir -p "$run"
  log "Shift report: susanna (sonnet) writes $out"
  prompt="Write the shift report of $skill/night.md (\"The shift report\") to $out, from the night's part of .coord/log.md on stdin. You run in the dev worktree $integ; the run folders are in $coord/runs/ and the pass replies in $coord/passes/. Never commit, never push. Reply with the file's path only."
  set +e
  awk '/^## [0-9-]+ night/ { buf = "" } { buf = buf $0 "\n" } END { printf "%s", buf }' "$coord/log.md" |
    (cd "$integ" && timeout 1800 claude -p --disallowedTools Agent --add-dir "$coord" \
      --agent susanna --model sonnet --effort medium "${claude_common[@]}" "$prompt") > "$run/reply.md" 2> "$run/stderr.log"
  rc=$?; set -e
  if is_limit "$rc" "$run/stderr.log" "$run/reply.md"; then result "report: LIMIT"; exit 75; fi
  [[ -s "$out" ]] && result "report: $out" || result "report: FAILED (exit $rc; see $run/stderr.log)"
  exit 0
fi

# ---------- a package ----------
[[ $# -ge 1 ]] || die "usage: pipeline.sh <ID> [options] [\"<task text>\"] | pass | report"
id="$1"; shift
[[ "$id" =~ ^[A-Z][A-Za-z0-9]*$ ]] || die "bad package ID '$id'"
model=""; effort=""; tablet=""; branch=""; audit_model="auto"; nowork=0; merge=1; dry=0; task=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --model) model="$2"; shift 2;; --effort) effort="$2"; shift 2;; --tablet) tablet="$2"; shift 2;;
    --branch) branch="$2"; shift 2;; --audit-model) audit_model="$2"; shift 2;;
    --no-worker) nowork=1; shift;; --no-merge) merge=0; shift;; --dry-run) dry=1; shift;;
    --*) die "unknown option $1";;
    *) [[ -z "$task" ]] || die "more than one task text"; task="$1"; shift;;
  esac
done

ensure_integ
night="$integ/$skill/night.md"
plan="$(ls "$integ/$skill"/plan-*.md 2>/dev/null | sort -V | tail -n1)"
features="$integ/$skill/features.md"
[[ -f "$night" && -f "$plan" ]] || die "no night.md or plan-*.md in $integ/$skill"
base="$(sed -E 's/f[0-9]+$//' <<<"$id")"
row="$(queue_row "$night" "$base")"
[[ -n "$row" || -n "$task" ]] || die "package $id is not in the queue of night.md and has no task text"
r_model=""; r_effort=""; r_tablet=""; r_after=""; r_spec=""
[[ -z "$row" ]] || IFS='|' read -r r_model r_effort r_tablet r_after r_spec <<<"$row"
model="${model:-${r_model:-sonnet}}"; effort="${effort:-${r_effort:-medium}}"; tablet="${tablet:-${r_tablet:-no}}"
[[ "$model" =~ ^(opus|sonnet|haiku)$ ]] || die "model '$model' (opus, sonnet)"
[[ "$effort" =~ ^(low|medium|high)$ ]] || die "effort '$effort' (low, medium, high)"
[[ "$tablet" =~ ^(yes|no)$ ]] || die "tablet '$tablet' (yes, no)"
[[ "$audit_model" =~ ^(auto|sonnet|opus|haiku)$ ]] || die "audit model '$audit_model'"
branch="${branch:-wp/${id,,}}"

git worktree prune
dverdict=""
# The run's folder and number.
counter="$coord/counter"; [[ -f "$counter" ]] || echo 0 > "$counter"
n=$(( $(cat "$counter") + 1 ))
run="$coord/runs/$(printf '%03d' "$n")-${id,,}"
[[ $dry -eq 0 ]] || run="$(mktemp -d)"
mkdir -p "$run"

# The task: the queue row's sections of the plan (and features.md), or the fix text with its package's sections.
spec="$run/task.md"
{
  if [[ -n "$task" ]]; then
    echo "## Your task: fix package $id"; echo; echo "$task"; echo
    if [[ -n "$r_spec" ]]; then echo "## The package it fixes: $base (for context)"; echo; spec_text "$plan" "$features" "$r_spec"; fi
  else
    echo "## Your task: package $id (from $(basename "$plan"))"; echo; spec_text "$plan" "$features" "$r_spec"
  fi
} > "$spec" || die "package $id: its spec '$r_spec' does not resolve (see the message above)"

# The bundle for winston and the criteria for susanna.
bundle="$run/spec.md"
{
  echo "# Pipeline run $n: $id for winston ($model, $effort) on branch $branch"; echo
  cat "$spec"
  echo "## The plan's decisions and layout (the top of $(basename "$plan"), before its package list)"; echo
  plan_preamble "$plan"; echo
  echo "## For winston (night.md)"; h2 "$night" "For winston"
  echo "## Your report (night.md)"; h2 "$night" "Your report"
  echo "## Definition of done (night.md)"; h2 "$night" "Definition of done"
} > "$bundle"
{
  echo "# Acceptance criteria: run $n, $id"; echo; cat "$spec"
  echo "## Definition of done (night.md)"; h2 "$night" "Definition of done"
  echo "## For susanna: the audit (night.md)"; h2 "$night" "For susanna: the audit"
} > "$run/criteria.md"

wt="$main/.claude/worktrees/wp-$(printf '%03d' "$n")-${id,,}"
if [[ $dry -eq 1 ]]; then
  echo "DRY RUN: pipeline run $n, package $id: winston ($model, $effort), branch $branch, tablet $tablet, audit $audit_model"
  echo "dev worktree: $integ; worker worktree: $wt (from dev, or the existing branch)"
  echo "spec: ${r_spec:-the task text}; after: ${r_after:-—}"
  echo "check: ./gradlew --console=plain ${gradle_check[*]}  (ANDROID_HOME=$ANDROID_HOME, under the host-cpu lock)"
  echo "tablet: $([[ $tablet == yes ]] && echo "rosie (sonnet) on $TABLET after the audit, when attached" || echo "no checks")"
  echo "merge: git -C $integ merge --no-ff <the audited sha>  (under the integ lock)$([[ $merge -eq 0 ]] && echo '  (--no-merge: printed only)')"
  echo "--- bundle ($(wc -l < "$bundle") lines) ---"; cat "$bundle"
  rm -rf "$run"; echo "RESULT #$n $id: DRY RUN (nothing started)"; exit 0
fi

if [[ $nowork -eq 1 ]]; then git show-ref --verify --quiet "refs/heads/$branch" || die "--no-worker: branch $branch does not exist"; fi
echo "$n" > "$counter"; runno="$n"
log "pipeline #$n $id (winston, $model, $effort; tablet $tablet) on $branch: started"
runlog "start $id $model $effort $branch tablet=$tablet"

# The worktree: fresh from dev, or the existing branch resumed from its last commit.
if git show-ref --verify --quiet "refs/heads/$branch"; then
  existing="$(git worktree list --porcelain | awk -v b="refs/heads/$branch" '/^worktree /{w=substr($0, 10)} $0=="branch "b{print w}')"
  if [[ -n "$existing" ]]; then wt="$existing"; else git worktree add "$wt" "$branch" >/dev/null 2>&1; fi
  runlog "resume $branch in $wt"
else
  git worktree add "$wt" -b "$branch" dev >/dev/null 2>&1
fi
link_shared "$wt"

# Run one headless agent in the worktree. The extra options go first: --tools, --add-dir and --disallowedTools take
# lists and would swallow a prompt that follows them. agent_env holds VAR=value pairs for the agent (rosie's adb).
agent_env=()
run_agent() {  # <name> <log> <agent> <model> <effort> <stdin file> <timeout s> <prompt> [claude options...]
  local name="$1" out="$2" ag="$3" mdl="$4" eff="$5" input="$6" secs="$7" prompt="$8"; shift 8
  local rc
  set +e
  (cd "$wt" && env "${agent_env[@]}" timeout "$secs" claude -p "$@" --agent "$ag" --model "$mdl" --effort "$eff" \
    "${claude_common[@]}" "$prompt") < "$input" > "$out" 2> "$out.err"
  rc=$?; set -e
  if is_limit "$rc" "$out" "$out.err"; then log "pipeline #$n $id: LIMIT during $name (claude exit $rc)"; result "#$n $id: LIMIT"; exit 75; fi
  runlog "$name: claude exit $rc; reply: $(tail -n 3 "$out" | tr '\n' ' ' | cut -c1-200)"
  return 0
}

# The Gradle check: a clean worktree, then the :core tests, lint and the R8 release build.
excerpt() {
  grep -nE 'FAILED|^e: |error:|What went wrong|AssertionError|ComparisonFailure|expected:|Exception|Lint found' -A6 "$1" | head -n 150 || true
  echo "--- tail ---"; tail -n 40 "$1"
  local lint="$wt/app/build/reports/lint-results-release.txt"
  if grep -q 'Lint found' "$1" && [[ -f "$lint" ]]; then echo "--- lint report ---"; grep -A6 'Error:' "$lint" | head -n 80 || true; fi
}
run_check() {  # <round>; on a failure writes fail-<round>.txt and returns 1
  local round="$1" out="$run/check-$1.log" dirty rc=0
  : > "$run/fail-$round.txt"
  dirty="$(git -C "$wt" status --porcelain)"
  if [[ -n "$dirty" ]]; then
    { echo "## The worktree has uncommitted changes: commit what belongs to the package, delete the rest"; echo "$dirty"; } >> "$run/fail-$round.txt"
    runlog "check: worktree not clean"; return 1
  fi
  lock acquire host-cpu "pipeline-$n" 1800 || die "host-cpu lock busy"
  (cd "$wt" && timeout 2400 ./gradlew --console=plain "${gradle_check[@]}") > "$out" 2>&1 || rc=1
  lock release host-cpu "pipeline-$n" || true
  if [[ $rc -ne 0 ]]; then
    { echo "## The Gradle check failed: ./gradlew ${gradle_check[*]} (full log: $out)"; excerpt "$out"; } >> "$run/fail-$round.txt"
    runlog "check: FAILED"; return 1
  fi
  runlog "check: ok ($(grep -m1 -oE 'BUILD SUCCESSFUL in [0-9a-z ]+' "$out" || echo ok))"
  return 0
}

# susanna's model: auto is a floor of opus for an opus worker or a diff in the logic that decides the wallpaper.
changed() { git -C "$wt" diff --name-only "dev...HEAD"; }
resolve_audit_model() {
  local why=""
  if [[ "$model" == "opus" ]]; then why="winston ran opus"
  elif changed | grep -qE '^(core/src/main/|app/src/main/kotlin/.*/(Store|DailyJob)\.kt$)'; then why="the diff touches core, Store.kt or DailyJob.kt"; fi
  if [[ "$audit_model" == "opus" ]]; then echo opus
  elif [[ -n "$why" ]]; then
    [[ "$audit_model" == "auto" ]] || { log "pipeline #$n $id: audit model raised to opus: $why (the line said $audit_model)"; runlog "audit model raised: $why"; }
    echo opus
  elif [[ "$audit_model" == "auto" ]]; then echo sonnet
  else echo "$audit_model"; fi
}

# rosie on the tablet: sets dverdict to PASS, FIX, STOP, NOT RUN or SKIP.
tablet_check() {  # <round>
  local round="$1" state apk prompt
  dverdict="SKIP"; [[ "$tablet" == yes ]] || return 0
  dverdict="NOT RUN"
  state="$("$adb" -s "$TABLET" get-state 2>/dev/null || true)"
  if [[ "$state" != device ]]; then log "pipeline #$n $id: tablet NOT RUN ($TABLET ${state:-not attached}); its checks go on the morning list"; return 0; fi
  lock acquire host-cpu "pipeline-$n" 1800 || die "host-cpu lock busy"
  if ! (cd "$wt" && timeout 1800 ./gradlew --console=plain :app:assembleDebug) > "$run/debug-$round.log" 2>&1; then
    lock release host-cpu "pipeline-$n" || true
    log "pipeline #$n $id: tablet NOT RUN (the debug build failed: $run/debug-$round.log)"; return 0
  fi
  lock release host-cpu "pipeline-$n" || true
  apk="$run/app-debug-$round.apk"; cp "$wt/app/build/outputs/apk/debug/app-debug.apk" "$apk"
  mkdir -p "$run/tablet-$round"
  { cat "$spec"; echo "## The worker's report"; git -C "$wt" log -1 --format=%B; echo; echo "## The files the package changed"; git -C "$wt" diff --stat "dev...HEAD"; } > "$run/tablet-$round.in"
  prompt="DEVICE pipeline run $n, package $id, round $round. On stdin: the package's text, winston's report and the files changed. Install $apk on the tablet $TABLET and run the package's tablet checks as \"For rosie: the tablet checks\" in $skill/night.md says. Put screenshots, dumps and the backup in $run/tablet-$round/. Reply with PASS, FIX, STOP or NOT RUN alone on the first line, then the lines that section asks for."
  lock acquire tablet "pipeline-$n" 1800 || { log "pipeline #$n $id: tablet NOT RUN (tablet lock busy)"; return 0; }
  agent_env=("ANDROID_SERIAL=$TABLET" "PATH=$ANDROID_HOME/platform-tools:$PATH")
  run_agent "tablet-$n-$round" "$run/tablet-$round.log" rosie sonnet medium "$run/tablet-$round.in" 3600 "$prompt" \
    --disallowedTools Agent,Edit,NotebookEdit --add-dir "$run"
  agent_env=()
  lock release tablet "pipeline-$n" || true
  dverdict="$(grep -m1 -oE '^(PASS|FIX|STOP|NOT RUN)' "$run/tablet-$round.log" || true)"
  dverdict="${dverdict:-NOT RUN}"
  log "pipeline #$n $id: tablet $dverdict (round $round): $(grep -vE '^(PASS|FIX|STOP|NOT RUN)?$' "$run/tablet-$round.log" | head -n 1 | cut -c1-140)"
}

# Round 0: winston (or, review-only, dev merged into the branch).
rounds=0
if [[ $nowork -eq 0 ]]; then
  prompt="You are winston in pipeline run $n for package $id. The bundle is on stdin (also at $bundle). You run in your own worktree $wt on branch $branch from dev: never switch branches and never commit anywhere else. Follow the bundle's \"For winston\" rules. Run ./gradlew :core:test (and :app:compileDebugKotlin when app/ changed) while you work; the pipeline runs the full check afterwards. Commit small, each commit with the Co-Authored-By trailer of your model, leave the worktree clean, and end with the report commit. Never start a background loop that waits for a process; run each command in the foreground with a timeout. Reply in at most five lines: last commit, done or not done, open questions."
  run_agent "pl-$n-winston" "$run/worker-0.log" winston "$model" "$effort" "$bundle" 10800 "$prompt" --disallowedTools Agent
  log "pipeline #$n $id: winston done, $(git -C "$wt" log -1 --format=%h) — $(tail -n 1 "$run/worker-0.log" | cut -c1-140)"
else
  log "pipeline #$n $id: review-only; merging dev into $branch"
  if ! git -C "$wt" merge --no-edit dev > "$run/premerge.log" 2>&1; then
    conflicts="$(git -C "$wt" diff --name-only --diff-filter=U)"
    [[ -n "$conflicts" ]] || { git -C "$wt" merge --abort >/dev/null 2>&1 || true; die "merging dev into $branch failed: $(tail -n 1 "$run/premerge.log")"; }
    { echo "## Merging dev into $branch conflicts in:"; echo "$conflicts"; } > "$run/fail-1.txt"
    rounds=1
    run_agent "fix-$n-1" "$run/fix-1.log" winston "$model" "$effort" "$run/fail-1.txt" 3600 \
      "You are winston on branch $branch in your own worktree; a merge of dev is in progress with conflicts (files on stdin) for package $id. Resolve each conflict keeping both sides' intent, git add the files and finish the merge with git commit (with your model's trailer). Run ./gradlew :core:test. Reply in at most three lines." --disallowedTools Agent
    if git -C "$wt" rev-parse -q --verify MERGE_HEAD >/dev/null || [[ -n "$(git -C "$wt" diff --name-only --diff-filter=U)" ]]; then
      git -C "$wt" merge --abort >/dev/null 2>&1 || true
      die "conflicts merging dev into $branch not resolved (worktree $wt kept)"
    fi
    log "pipeline #$n $id: conflicts with dev resolved by winston"
  fi
fi
if [[ -z "$(git -C "$wt" log --oneline "dev..HEAD")" ]]; then
  log "pipeline #$n $id: no commits on $branch"; result "#$n $id: NOT MERGED (no commits; worktree $wt kept)"; exit 1
fi

# The check, the audit and the tablet share at most two fix rounds.
audited=""
while :; do
  if ! run_check "$rounds"; then
    log "pipeline #$n $id: check failed (round $rounds): $(sed -n 2p "$run/fail-$rounds.txt" | cut -c1-140)"
    reason="the check failed"; feedback="$run/fail-$rounds.txt"
  else
    log "pipeline #$n $id: check passed (round $rounds)"
    audited="$(git -C "$wt" rev-parse HEAD)"; runlog "audit $rounds reads $audited"
    git -C "$wt" diff -U5 "dev...$audited" > "$run/audit-$rounds.diff"
    { cat "$run/criteria.md"; echo; echo "## winston's report (git log -1)"; git -C "$wt" log -1 --format=%B "$audited"
      echo "## The diff (git diff -U5 dev...$audited, the HEAD of $branch, $(wc -l < "$run/audit-$rounds.diff") lines)"; cat "$run/audit-$rounds.diff"; } > "$run/audit-$rounds.in"
    am="$(resolve_audit_model)"
    run_agent "audit-$n-$rounds" "$run/audit-$rounds.log" susanna "$am" medium "$run/audit-$rounds.in" 1800 \
      "AUDIT pipeline run $n, package $id, branch $branch. On stdin: the acceptance criteria (with \"For susanna: the audit\"), winston's report and the diff against dev; the Gradle check passed (:core tests, lint, the R8 release build). Reply with the single word APPROVED on the first line, or FIX on the first line followed by at most five findings, each with file:line, the concrete failure and the fix. Nothing else." \
      --tools Read,Grep,Glob
    verdict="$(grep -m1 -oE '^(APPROVED|FIX)' "$run/audit-$rounds.log" || true)"
    if [[ "$verdict" == "APPROVED" ]]; then
      log "pipeline #$n $id: audit APPROVED ($am, round $rounds)"
      tablet_check "$rounds"
      case "$dverdict" in
        PASS|SKIP|"NOT RUN") break;;
        STOP)
          log "pipeline #$n $id: tablet STOP; branch $branch kept at ${audited:0:9}"
          result "#$n $id: NOT MERGED (tablet STOP, needs the user: see $run/tablet-$rounds.log; branch $branch, worktree $wt kept)"; exit 1;;
      esac
      reason="the tablet check found a bug"; feedback="$run/tablet-$rounds.log"
    else
      log "pipeline #$n $id: audit ${verdict:-unclear} ($am, round $rounds): $(grep -vE '^(FIX|APPROVED)?$' "$run/audit-$rounds.log" | head -n 1 | cut -c1-140)"
      reason="the audit asked for fixes"; feedback="$run/audit-$rounds.log"
    fi
  fi
  if [[ $rounds -ge 2 ]]; then
    log "pipeline #$n $id: not merged after two fix rounds; branch $branch kept at $(git -C "$wt" log -1 --format=%h)"
    result "#$n $id: NOT MERGED ($reason after two fix rounds; branch $branch, worktree $wt kept; see $run)"; exit 1
  fi
  rounds=$((rounds + 1))
  { echo "# Fix round $rounds of 2 for pipeline run $n ($id): $reason"; echo; cat "$feedback"; echo; echo "# The task bundle"; cat "$bundle"; } > "$run/fix-$rounds.in"
  run_agent "fix-$n-$rounds" "$run/fix-$rounds.log" winston "$model" "$effort" "$run/fix-$rounds.in" 7200 \
    "You are winston on branch $branch in your own worktree, fix round $rounds of 2 for package $id: $reason. On stdin: the failing output or the findings, then the task bundle. Fix the cause (not the test), add the regression test where one is missing, run the failing part yourself, commit with your model's trailer (new commits, amend nothing), leave the worktree clean, and update the report: end with a new report commit that says what this round changed. Never start a background loop that waits for a process. Reply in at most five lines: last commit, what you fixed, anything you could not fix." \
    --disallowedTools Agent
  log "pipeline #$n $id: fix round $rounds done, $(git -C "$wt" log -1 --format=%h)"
done

# Merge the audited commit into dev, only when the branch still points at it.
if ! why="$(audited_head_check "$wt" "$branch" "$audited")"; then
  log "pipeline #$n $id: the branch is not the audited HEAD: $why"
  result "#$n $id: NOT MERGED (the branch is not the audited HEAD; $why; worktree $wt kept)"; exit 1
fi
if [[ $merge -eq 0 ]]; then result "#$n $id: APPROVED, not merged (--no-merge): git -C $integ merge --no-ff $audited"; exit 0; fi
lock acquire integ "pipeline-$n" 600 || die "integ lock busy; the approved branch $branch is kept at ${audited:0:9}"
if ! why="$(audited_head_check "$wt" "$branch" "$audited")"; then
  lock release integ "pipeline-$n" || true
  result "#$n $id: NOT MERGED (the branch is not the audited HEAD; $why; worktree $wt kept)"; exit 1
fi
if git -C "$integ" merge --no-ff -m "Merge $branch: $id (pipeline run $n)" "$audited" > "$run/merge.log" 2>&1; then
  mc="$(git -C "$integ" rev-parse --short HEAD)"
  git worktree remove --force "$wt" >/dev/null 2>&1 || true
  git -C "$integ" branch -d "$branch" >/dev/null 2>&1 || true
  lock release integ "pipeline-$n" || true
  result "#$n $id: MERGED $mc (tablet $dverdict)"; exit 0
fi
git -C "$integ" merge --abort >/dev/null 2>&1 || true
lock release integ "pipeline-$n" || true
result "#$n $id: NOT MERGED (merge conflict with dev; branch $branch, worktree $wt kept)"; exit 1
