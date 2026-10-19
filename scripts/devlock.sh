#!/usr/bin/env bash
# Exclusive locks for what the night shift shares (copied from pascaline's tools/devlock.sh).
#
#   scripts/devlock.sh acquire <resource> <owner> [wait_seconds]   # exit 0 = you hold it, 1 = busy
#   scripts/devlock.sh release <resource> <owner>
#   scripts/devlock.sh status
#   scripts/devlock.sh break <resource>     # for a lock whose owner is gone
#
# Resources: host-cpu (Gradle), integ (merges into dev), tablet (the Lenovo, rosie), night (one night.sh).
# Locks are directories (mkdir is atomic) in the main checkout's .coord/locks, shared by every worktree.
set -euo pipefail

common="$(git -C "$(dirname "${BASH_SOURCE[0]}")" rev-parse --path-format=absolute --git-common-dir)"
LOCKS="$(dirname "$common")/.coord/locks"
mkdir -p "$LOCKS"
cmd="${1:?usage: devlock.sh acquire|release|status|break ...}"

case "$cmd" in
  acquire)
    res="${2:?resource}" owner="${3:?owner}" wait="${4:-0}" start=$SECONDS
    while ! mkdir "$LOCKS/$res" 2>/dev/null; do
      if (( SECONDS - start >= wait )); then
        echo "busy: $res held by $(cat "$LOCKS/$res/owner" 2>/dev/null || echo '?')" >&2
        exit 1
      fi
      sleep 5
    done
    printf '%s\n' "$owner" > "$LOCKS/$res/owner"
    date -Iseconds > "$LOCKS/$res/since"
    echo "acquired $res for $owner"
    ;;
  release)
    res="${2:?resource}" owner="${3:?owner}"
    holder="$(cat "$LOCKS/$res/owner" 2>/dev/null || true)"
    [[ "$holder" == "$owner" ]] || { echo "not releasing $res: held by '${holder:-nobody}', not $owner" >&2; exit 1; }
    rm -rf "${LOCKS:?}/$res"
    echo "released $res"
    ;;
  status)
    shopt -s nullglob
    any=0
    for d in "$LOCKS"/*/; do
      any=1
      r="$(basename "$d")"
      echo "$r: $(cat "$d/owner" 2>/dev/null) since $(cat "$d/since" 2>/dev/null)"
    done
    (( any )) || echo "no locks held"
    ;;
  break)
    res="${2:?resource}"
    rm -rf "${LOCKS:?}/$res"
    echo "broke lock $res"
    ;;
  *) echo "unknown command $cmd" >&2; exit 2 ;;
esac
