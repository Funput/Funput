// The document as Funput predicts it: the last reading the client answered with,
// plus every edit issued since that it has not confirmed yet.
//
// Non-preedit used to remember one repair against one reading. That is exactly
// enough for a client that answers every keystroke on time, and exactly one short
// for a client that answers one keystroke late — Firefox's content cache, say. Its
// reading never equals "the last repair applied", so the document never counted as
// confirmed and a finished word could never be re-opened there. Worse, two
// identical repairs in a row (`␣␣`, `⌫⌫`) made a stale reading *look* confirmed.
//
// A short journal fixes both. A reading is compared with every prefix of it:
//
//   p(0) = base,  p(i+1) = p(i) less e_i.deleted characters, then e_i.text
//
// and the oldest k with p(k) == reading (dropped.h says why the oldest) says the
// client has applied k of the n edits. k == n is the old record's in sync; 1 ≤ k < n is a
// client that follows but lags; k == 0 is a reading no different from a silent
// client's and is no evidence at all. The stale reading that used to coincide with
// the one record now matches prefix n−1, never n.
//
// What the client will read once it has caught up is `predicted()`. Backspace
// handling re-opens the word found *there*, not in the reading — the shadow copy
// Windows keeps as `CommittedTail` (crates/funput-desktop/src/retone.rs). What that
// guarantees: for a client that applies every edit in order, predicted() is its
// document whatever its lag, as long as nothing moved the caret meanwhile — a caret
// key, Enter or a shortcut resets the journal (NonPreeditState::forget); a click
// the client has not reported yet is the one residual. A client that dropped a
// delete holds more text than predicted, never less, so a repair measured against
// predicted() can delete the wrong text but never text that is not there, and
// dropped.h catches the drop at the next write. A reading the shell only cached
// never reaches here (document/observe.h): silence moves neither base nor verdict.
//
// A reading that matches no prefix restarts the journal from itself, and the pending
// edits stay on as *provisional*: a lagging client reads a document the caret has
// left for a few more keys, so they may still be in flight (the journal then catches
// up with it), or the reading is current and they were applied before it moved (the
// next reading matches with them skipped). A client that moved the caret and then
// dropped a delete is judged on the second reading as before.
//
// One ambiguity is built in rather than hidden: a delete-only edit the client
// dropped, followed by an identical one it applied, reads as the first applied and
// the second pending. Readings alone cannot tell that from lag, and lag is the case
// this journal exists for. Such an acknowledgement is flagged `ambiguous()` until a
// later reading settles it, and no word is re-opened on it.

#ifndef FUNPUT_COMPOSE_NONPREEDIT_DOCUMENT_PREDICTED_H
#define FUNPUT_COMPOSE_NONPREEDIT_DOCUMENT_PREDICTED_H

#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

#include "compose/composer/nonpreedit/document/dropped.h"

namespace funput {

class PredictedDocument {
public:
    using Edit = PredictedEdit;

    struct Ack { // `acked` of `pending` confirmed when `matched`, else restarted
        size_t acked = 0;
        size_t pending = 0;
        bool matched = false;
        Verdict verdict = Verdict::Unknown;
    };

    // Real typing never has more than two or three writes unanswered; a client that
    // falls silent for a burst leaves more. Past this the journal is lost, base and
    // all, and the next answered reading restarts it with no verdict. Folding the
    // oldest edit into the base as applied convicted a client of an edit it never saw.
    static constexpr size_t kCapacity = 16;

    void reset() {
        known_ = ambiguous_ = false;
        provisional_ = 0;
        base_.clear(), edits_.clear();
    }

    void predict(uint32_t deleted, const std::string &text, bool afterAdopt) {
        if (edits_.size() == kCapacity) reset();
        edits_.push_back({deleted, text, afterAdopt});
    }

    // Judge `reading` against the prefixes and keep what it leaves unconfirmed.
    Ack observe(const std::string &reading) {
        Ack ack;
        ack.pending = edits_.size();
        // No base to compare with: lost, or never started. The next reading is it.
        if (!known_) return restart(reading, ack);
        const PrefixMatch match = bestMatch(base_, edits_, provisional_, reading);
        if (match.found) {
            ack.matched = true;
            ack.acked = match.k;
            ack.pending = edits_.size() - match.skip;
            const size_t next = match.skip + match.k;
            ambiguous_ = match.k >= 1 && next < edits_.size() &&
                         sameDeleteOnly(edits_[next - 1], edits_[next]);
            edits_.erase(edits_.begin(), edits_.begin() + static_cast<std::ptrdiff_t>(next));
            provisional_ = next >= provisional_ ? 0 : provisional_ - next;
            base_ = reading;
            return ack;
        }
        // Judged as if every provisional edit were applied: the on-time explanation.
        const size_t p = provisional_;
        ack.verdict = droppedDelete(prefixesOf(base_, edits_, p), edits_, p, reading);
        return restart(reading, ack);
    }

    // The document once every pending edit has landed.
    std::string predicted() const { return prefixesOf(base_, edits_, 0).back(); }

    size_t pending() const { return edits_.size(); }

    // Whether any pending edit wrote text — the only kind a reading can judge.
    bool pendingWrites() const {
        return std::any_of(edits_.begin(), edits_.end(),
                           [](const Edit &edit) { return !edit.text.empty(); });
    }

    bool ambiguous() const { return ambiguous_; } // the acknowledgement the header names

    // Whether some state past the base reads as `reading`: an empty answer is then
    // what the journal itself predicts somewhere, not a client that shows nothing.
    bool explains(const std::string &reading) const {
        const std::vector<std::string> prefixes = prefixesOf(base_, edits_, 0);
        return std::find(prefixes.begin() + 1, prefixes.end(), reading) != prefixes.end();
    }

private:
    // The document moved somewhere the journal does not describe. Start over from it.
    Ack restart(const std::string &reading, Ack ack) {
        known_ = true, ambiguous_ = false;
        base_ = reading;
        provisional_ = edits_.size();
        return ack;
    }

    bool known_ = false; // whether `base_` is a reading the client answered with
    std::string base_;
    std::vector<Edit> edits_;
    size_t provisional_ = 0; // how many of `edits_` predate the last restart
    bool ambiguous_ = false;
};

} // namespace funput

#endif // FUNPUT_COMPOSE_NONPREEDIT_DOCUMENT_PREDICTED_H
