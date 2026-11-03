// The document as Funput predicts it: the last reading the client answered with,
// plus every edit issued since that it has not confirmed yet.
//
// Non-preedit used to remember one repair against one reading: enough for a client
// that answers every keystroke on time, one short for one answering a keystroke late
// (Firefox's content cache), whose reading never equals "the last repair applied" —
// nothing was ever confirmed there, no finished word re-opened — while two identical
// repairs in a row (`␣␣`, `⌫⌫`) made a stale reading *look* confirmed.
//
// A short journal fixes both. A reading is compared with every prefix of it:
//
//   p(0) = base,  p(i+1) = p(i) less e_i.deleted characters, then e_i.text
//
// and the oldest k with p(k) == reading (match.h says why the oldest) says the
// client has applied k of the n edits. k == n is the old record's in sync; 1 ≤ k < n
// is a client that follows but lags; k == 0 is a reading no different from a silent
// client's and is no evidence at all. What the client will read once it has caught
// up is `predicted()`: Backspace handling re-opens the word found *there*, not in
// the reading — the shadow copy Windows keeps as `CommittedTail` (retone.rs).
//
// A reading that matches no prefix restarts the journal from itself, keeping the
// pending edits as *provisional* (match.h). A restarted journal is not *trusted*,
// and until it is it only tracks: no word is re-opened on it, no Backspace is
// taken over, no verdict is reached (trust.h; the README spells out what is
// guaranteed to whom). A client that dropped a delete holds more text than
// predicted, never less, so a repair measured against predicted() can delete the
// wrong text but never text that is not there, and dropped.h catches the drop at
// the next write. A reading the shell only cached never reaches here (observe.h).
//
// One ambiguity is built in rather than hidden: a delete-only edit the client
// dropped, followed by an identical one it applied, reads as the first applied and
// the second pending, exactly as lag does. Such an acknowledgement is flagged
// `ambiguous()` until a later reading settles it, and no word is re-opened on it.

#ifndef FUNPUT_COMPOSE_NONPREEDIT_DOCUMENT_PREDICTED_H
#define FUNPUT_COMPOSE_NONPREEDIT_DOCUMENT_PREDICTED_H

#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

#include "compose/composer/nonpreedit/document/match.h"
#include "compose/composer/nonpreedit/document/trust.h"

namespace funput {

class PredictedDocument {
public:
    using Edit = PredictedEdit;
    using Reset = Belief::Reset;

    struct Ack {
        size_t acked = 0;   // of `pending`, confirmed by this reading when `matched`
        size_t pending = 0;
        bool matched = false;
        bool trusted = false; // the base is believed exact after this reading
        Verdict verdict = Verdict::Unknown;
    };

    // Real typing leaves two or three writes unanswered, a silent burst more. Past
    // this the journal is lost, base and all, and the next answered reading restarts
    // it with no verdict: folding the oldest edit into the base convicted a client.
    static constexpr size_t kCapacity = 16;

    void reset(Reset why) {
        belief_.reset(why, edits_.size());
        known_ = ambiguous_ = false;
        provisional_ = 0;
        base_.clear();
        edits_.clear();
    }

    void predict(uint32_t deleted, const std::string &text, bool afterAdopt) {
        if (edits_.size() == kCapacity) reset(Reset::Overflow);
        edits_.push_back({deleted, text, afterAdopt});
    }

    // Judge `reading` against the prefixes and keep what it leaves unconfirmed.
    Ack observe(const std::string &reading) {
        Ack ack;
        ack.pending = edits_.size();
        // No base to compare with, or a reading known to be stale: the next one is it.
        const bool stale = belief_.consumeStale();
        if (!known_ || stale) return restart(reading, ack);
        const PrefixMatch match = bestMatch(base_, edits_, provisional_, reading);
        if (!match.found) {
            // Only a trusted base can be read against. Judged with the provisional
            // edits taken as applied: the on-time explanation of the last restart.
            const size_t p = provisional_;
            if (belief_.trusted) {
                ack.verdict = droppedDelete(prefixesOf(base_, edits_, p), edits_, p, reading);
            }
            return restart(reading, ack);
        }
        ack.matched = true;
        ack.acked = match.k;
        ack.pending = edits_.size() - match.skip;
        const size_t next = match.next();
        const auto end = edits_.begin() + static_cast<std::ptrdiff_t>(next);
        const auto confirmed = end - static_cast<std::ptrdiff_t>(match.k);
        const bool wrote = std::any_of(confirmed, end, writes);
        ambiguous_ = match.k >= 1 && next < edits_.size() &&
                     sameDeleteOnly(edits_[next - 1], edits_[next]);
        edits_.erase(edits_.begin(), end);
        // An edit confirmed after the base puts every later one after it too.
        if (next > 0) provisional_ = 0;
        belief_.matched(match.skip == 0, match.contested, wrote, edits_.size(), ack.pending);
        base_ = reading;
        ack.trusted = belief_.trusted;
        return ack;
    }

    // The document once every pending edit has landed.
    std::string predicted() const { return prefixesOf(base_, edits_, 0).back(); }
    size_t pending() const { return edits_.size(); }
    // Whether any pending edit wrote text — the only kind a reading can judge.
    bool pendingWrites() const { return std::any_of(edits_.begin(), edits_.end(), writes); }

    bool trusted() const { return belief_.trusted; }
    bool ambiguous() const { return ambiguous_; } // the acknowledgement the header names

private:
    static bool writes(const Edit &edit) { return !edit.text.empty(); }

    // The document moved somewhere the journal does not describe. Start over from it.
    Ack restart(const std::string &reading, Ack ack) {
        known_ = true;
        ambiguous_ = false;
        base_ = reading;
        provisional_ = edits_.size();
        belief_.restarted(edits_.size());
        ack.trusted = belief_.trusted;
        return ack;
    }

    bool known_ = false; // whether `base_` is a reading the client answered with
    std::string base_;
    std::vector<Edit> edits_;
    size_t provisional_ = 0; // how many of `edits_` predate the last restart
    Belief belief_;
    bool ambiguous_ = false;
};

} // namespace funput

#endif // FUNPUT_COMPOSE_NONPREEDIT_DOCUMENT_PREDICTED_H
