// The document as Funput predicts it: the last reading the client confirmed, plus
// every edit issued since that it has not confirmed yet.
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
// and the largest k with p(k) == reading says the client has applied k of the n
// edits. k == n is what the old single record meant by in sync; 1 ≤ k < n is a
// client that follows but lags; k == 0 is a reading no different from a silent
// client's and is no evidence at all. The stale reading that used to coincide with
// the one record now matches prefix n−1, never n.
//
// What the client will read once it has caught up is `predicted()`. Backspace
// handling re-opens the word found *there*, not in the reading — the same shadow
// copy Windows keeps as `CommittedTail` (crates/funput-desktop/src/retone.rs).
// For a client that applies every edit in order, predicted() is its document
// whatever its lag; a client that dropped a delete is longer, never shorter, so
// a repair measured against predicted() can delete the wrong text but never text
// that is not there — and the dropped-delete check below is what then catches it.
//
// One ambiguity is built in rather than hidden: a delete-only edit the client
// dropped, followed by an identical one it applied, reads as the first applied and
// the second pending. Readings alone cannot tell lag from a dropped delete there,
// and lag is the case this journal exists for. The journal rights itself at the
// next reading that matches no prefix.

#ifndef FUNPUT_COMPOSE_NONPREEDIT_DOCUMENT_PREDICTED_H
#define FUNPUT_COMPOSE_NONPREEDIT_DOCUMENT_PREDICTED_H

#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

#include "compose/composer/nonpreedit/verdict.h"
#include "ffi/utf8.h"

namespace funput {

class PredictedDocument {
public:
    // One "delete N characters, then write this", Funput's own or the app's.
    // `afterAdopt` marks the repair that follows a re-opened word, the one a client
    // like Chrome's address bar drops — see Verdict::RefuseRetone.
    struct Edit {
        uint32_t deleted = 0;
        std::string text;
        bool afterAdopt = false;
    };

    // What one reading said: `acked` of `pending` edits confirmed when `matched`,
    // else the reading matched no prefix and the journal was restarted from it.
    struct Ack {
        size_t acked = 0;
        size_t pending = 0;
        bool matched = false;
        Verdict verdict = Verdict::Unknown;
    };

    // Real typing never has more than two or three writes unanswered. Past this the
    // oldest is folded into the base as applied: the only alternative is to stop
    // predicting, and an edit that old is never going to be judged anyway.
    static constexpr size_t kCapacity = 4;

    void reset() {
        base_.clear();
        edits_.clear();
    }

    void predict(uint32_t deleted, const std::string &text, bool afterAdopt) {
        if (edits_.size() == kCapacity) {
            base_ = apply(base_, edits_.front());
            edits_.erase(edits_.begin());
        }
        edits_.push_back({deleted, text, afterAdopt});
    }

    // Judge `reading` against the prefixes and keep what it leaves unconfirmed.
    Ack observe(const std::string &reading) {
        Ack ack;
        ack.pending = edits_.size();
        std::vector<std::string> prefixes{base_};
        for (const Edit &edit : edits_) prefixes.push_back(apply(prefixes.back(), edit));
        for (size_t k = prefixes.size(); k-- > 0;) {
            if (prefixes[k] != reading) continue;
            ack.matched = true;
            ack.acked = k;
            edits_.erase(edits_.begin(), edits_.begin() + static_cast<std::ptrdiff_t>(k));
            base_ = reading;
            return ack;
        }
        // The three-string comparison, per unconfirmed edit: a delete with nothing
        // written cannot convict, since "dropped" is then the unchanged document,
        // which is also what a client that has not answered yet reads.
        for (size_t j = 0; j < edits_.size(); ++j) {
            if (edits_[j].text.empty()) continue;
            const std::string dropped = prefixes[j] + edits_[j].text;
            if (reading != dropped || dropped == prefixes[j + 1]) continue;
            ack.verdict = edits_[j].afterAdopt ? Verdict::RefuseRetone : Verdict::RefuseMode;
            break;
        }
        // Verdict or not, the document moved somewhere these edits do not describe —
        // a click, a selection, a client that dropped something. Start over from it.
        base_ = reading;
        edits_.clear();
        return ack;
    }

    // The document once every pending edit has landed.
    std::string predicted() const {
        std::string out = base_;
        for (const Edit &edit : edits_) out = apply(out, edit);
        return out;
    }

    size_t pending() const { return edits_.size(); }

    // Whether any pending edit wrote text — the only kind a reading can judge.
    bool pendingWrites() const {
        for (const Edit &edit : edits_) {
            if (!edit.text.empty()) return true;
        }
        return false;
    }

private:
    static std::string apply(const std::string &document, const Edit &edit) {
        return dropLast(document, edit.deleted) + edit.text;
    }

    std::string base_;
    std::vector<Edit> edits_;
};

} // namespace funput

#endif // FUNPUT_COMPOSE_NONPREEDIT_DOCUMENT_PREDICTED_H
