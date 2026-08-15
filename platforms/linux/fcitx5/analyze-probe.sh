#!/usr/bin/env bash
# Summarize a probe log into the table that decides whether non-preedit is viable.
# See probe.h for what each question means.
#
# Usage: analyze.sh [path]        # default ~/.config/Funput/probe.jsonl
set -euo pipefail

LOG="${1:-${XDG_CONFIG_HOME:-$HOME/.config}/Funput/probe.jsonl}"
[ -f "$LOG" ] || { echo "No probe log at $LOG (did you run with FUNPUT_PROBE=1?)" >&2; exit 1; }
command -v jq >/dev/null || { echo "needs jq" >&2; exit 1; }

echo "== Q1/Q4: what each app claims, and whether we can even name it =="
jq -r 'select(.ev=="focus")
       | [(.app // "" | if .=="" then "<UNKNOWN>" else . end), .frontend,
          (if (.caps|index("SurroundingText")) then "surrounding" else "-" end),
          (if (.caps|index("Preedit")) then "preedit" else "-" end),
          (if (.caps|index("ClientUnfocusCommit")) then "unfocus-commit" else "-" end),
          (if (.caps|index("Terminal")) then "TERMINAL" else "-" end),
          (if (.caps|index("Password")) or (.caps|index("Sensitive")) then "SENSITIVE" else "-" end)]
       | @tsv' "$LOG" | sort -u | column -t

echo
echo "== Q2: does surrounding text match what we committed? (per app) =="
jq -r 'select(.ev=="surrounding" and has("matchesTail"))
       | [(.app // "<UNKNOWN>"), (.matchesTail|tostring)] | @tsv' "$LOG" |
    sort | uniq -c | sort -rn | awk 'BEGIN{print "count\tapp\tmatches"}{print $1"\t"$2"\t"$3}' | column -t

echo
echo "== Q3: how late does it arrive? (ms, per app) =="
jq -r 'select(.ev=="surrounding" and has("dtMs")) | [(.app // "<UNKNOWN>"), .dtMs] | @tsv' "$LOG" |
    awk -F'\t' '{n[$1]++; sum[$1]+=$2; if ($2>max[$1]) max[$1]=$2}
        END{print "app\tsamples\tavg_ms\tmax_ms";
            for (a in n) printf "%s\t%d\t%.1f\t%d\n", a, n[a], sum[a]/n[a], max[a]}' | column -t

echo
echo "== Q5: was a selection live when we committed? (the autofill hazard) =="
jq -r 'select(.ev=="commit") | [(.app // "<UNKNOWN>"), (.before.selection // false | tostring)] | @tsv' "$LOG" |
    sort | uniq -c | awk 'BEGIN{print "count\tapp\tselection_live"}{print $1"\t"$2"\t"$3}' | column -t

echo
echo "== Q6: the write self-test (type ;;;p) =="
if jq -e 'select(.ev=="selftest")' "$LOG" >/dev/null 2>&1; then
    jq -r 'select(.ev=="selftest")
           | [.step, (.drift // .sym // "" | tostring), (.reason // "")] | @tsv' "$LOG" | column -t
    echo
    echo "  chord-seen                        = a Ctrl+Alt chord DID reach us (sym in col 2)"
    echo "  start/wrote/deleted/ordered/done  = the write path works end to end"
    echo "  write-failed                      = commitString never landed"
    echo "  delete-failed drift=+5            = deleteSurroundingText did nothing"
    echo "  delete-failed drift=+2 or +4      = it counted BYTES, not characters"
    echo "  delete-failed drift<0             = it ate the user's own text"
    echo "  order-failed                      = commit/delete/commit lost its order"
else
    echo "  (never run — press Ctrl+Alt+P with the caret in a scratch field)"
fi

echo
echo "== commits with no surrounding update at all (client never answered) =="
jq -r '[inputs] | . as $all
       | ($all | map(select(.ev=="commit")) | length) as $commits
       | ($all | map(select(.ev=="surrounding" and has("dtMs"))) | length) as $answered
       | "commits: \($commits)  answered: \($answered)  silent: \($commits - $answered)"' -n "$LOG"
