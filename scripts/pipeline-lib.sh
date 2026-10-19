#!/usr/bin/env bash
# Helpers of scripts/pipeline.sh and scripts/night.sh, sourced by them and by scripts/pipeline.test.sh.

# queue_row <night.md> <ID>: the row of the queue table (section "## The queue") whose ID column is <ID>,
# as model|effort|tablet|after|spec; nothing when there is no such row.
queue_row() {
  awk -F'|' -v id="$2" '
    /^## / { q = ($0 ~ /^## The queue/); next }
    q && /^\|/ {
      for (i = 2; i < NF; i++) gsub(/^ +| +$/, "", $i)
      if ($3 == id) { split($5, m, / *, */); printf "%s|%s|%s|%s|%s\n", m[1], m[2], $6, $7, $8; exit }
    }' "$1"
}

# section <file> <key>: the "### [ ] <key>. ..." section (also "### [x]"), up to the next ### or ## heading.
section() {
  awk -v k="$2" '
    /^## / && p { exit }
    /^### / { if (p) exit; h = $0; sub(/^### \[[ xX]\] /, "", h); if (index(h, k ". ") == 1) p = 1 }
    p' "$1"
}

# h2 <file> <title>: the "## <title>..." section, without its heading line, up to the next ## heading.
h2() {
  awk -v t="## $2" '/^## / { if (p) exit; if (index($0, t) == 1) { p = 1; next } } p' "$1"
}

# spec_text <plan> <features> <spec>: the sections a queue row's Spec names, e.g. "plan M1" or
# "plan F1–F5 + features 4". Fails, naming the part, when a section is missing.
spec_text() {
  local plan="$1" features="$2" part file key out
  local IFS=$'\n'
  for part in $(sed 's/ *+ */\n/g' <<<"$3"); do
    case "$part" in
      plan\ *) file="$plan"; key="${part#plan }";;
      features\ *) file="$features"; key="${part#features }";;
      *) echo "spec_text: unknown part '$part' (plan <key> or features <key>)" >&2; return 1;;
    esac
    out="$(section "$file" "$key")"
    [[ -n "$out" ]] || { echo "spec_text: no section '### [ ] $key.' in $file" >&2; return 1; }
    printf '%s\n\n' "$out"
  done
}

# plan_preamble <plan>: the plan's text before "## Packages" (the request, the decisions, the layout).
plan_preamble() {
  awk '/^## Packages/ { exit } { print }' "$1"
}

# since_last_pass <log>: the finished packages' RESULT lines (text after "RESULT ": MERGED or NOT MERGED, not a
# LIMIT) since the last finished Opus pass.
since_last_pass() {
  [[ -f "$1" ]] || return 0
  awk '/RESULT pass [0-9]+: done/ { n = 0; delete r; next }
       /RESULT #[0-9]+ [^:]+: (MERGED|NOT MERGED)/ { sub(/^.*RESULT /, ""); r[++n] = $0 }
       END { for (i = 1; i <= n; i++) print r[i] }' "$1"
}

# next_pass <log>: the number of the next Opus pass (finished passes plus one; a LIMIT is retried with its number).
next_pass() {
  local done=0
  [[ -f "$1" ]] && done="$(grep -cE 'RESULT pass [0-9]+: done' "$1" || true)"
  echo $(( ${done:-0} + 1 ))
}

# last_pass_reply <log>: the last Opus pass reply as logged (its indented lines).
last_pass_reply() {
  [[ -f "$1" ]] || return 0
  awk '/^- [0-9:]+ Opus pass [0-9]+ reply:/ { n = 0; delete r; on = 1; next }
       on && /^    / { r[++n] = $0; next }
       on { on = 0 }
       END { for (i = 1; i <= n; i++) print r[i] }' "$1"
}

# audited_head_check <worktree> <branch> <audited sha>: succeeds when refs/heads/<branch> is the audited commit;
# otherwise prints why and fails. (pascaline, A17: a worker that detached HEAD and committed there left the branch
# behind the commit the audit read, and the merge took the wrong one.)
audited_head_check() {
  local wt="$1" br="$2" sha="$3" tip
  tip="$(git -C "$wt" rev-parse -q --verify "refs/heads/$br^{commit}" 2>/dev/null || true)"
  [[ -n "$sha" ]] || { echo "no audited commit recorded"; return 1; }
  [[ -n "$tip" ]] || { echo "branch $br does not exist"; return 1; }
  [[ "$tip" == "$sha" ]] || { echo "branch $br is at ${tip:0:9}, the audit read ${sha:0:9}"; return 1; }
  return 0
}

# from_opus_pass <reply file>: the reply from its first OPUS PASS line on (text before it breaks the form);
# a reply without that line is printed whole.
from_opus_pass() {
  if grep -q '^OPUS PASS' "$1"; then sed -n '/^OPUS PASS/,$p' "$1"; else cat "$1"; fi
}

# unsafe <cmd>: shell metacharacters outside plain double quotes, or a backtick or $ anywhere (night.sh evals
# josh's line, so it refuses these).
unsafe() {
  local bare; bare="$(sed -E 's/"[^"$`]*"//g' <<<"$1")"
  [[ "$bare" =~ [\;\&\|\`\$\<\>] || "$1" == *[\`\$]* ]]
}

# is_limit <rc> <files...>: a failed claude call that hit the usage limit.
is_limit() {
  local rc="$1"; shift
  [[ "$rc" -ne 0 ]] && grep -qiE 'usage limit|rate limit|limit reached|429|overloaded' "$@" 2>/dev/null
}
