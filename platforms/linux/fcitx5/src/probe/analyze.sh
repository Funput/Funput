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
echo "== commits with no surrounding update at all (client never answered) =="
jq -r '[inputs] | . as $all
       | ($all | map(select(.ev=="commit")) | length) as $commits
       | ($all | map(select(.ev=="surrounding" and has("dtMs"))) | length) as $answered
       | "commits: \($commits)  answered: \($answered)  silent: \($commits - $answered)"' -n "$LOG"
