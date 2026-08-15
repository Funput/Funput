// The write self-test: does deleteSurroundingText work on this client, does it
// count characters, and does it stay ordered against the commits around it?
//
// Driven by the surrounding-text updates the client sends back, because every write
// is asynchronous — each step fires the next one only once the previous landed.

#include "probe/support/internal.h"

namespace funput::probe::detail {

namespace {

enum class Step {
    Idle,
    AwaitWrite,   // committed the probe text, waiting to see it land
    AwaitDelete,  // deleted it back, waiting to compare against the baseline
    AwaitBurst,   // commit+delete+commit with no waiting, to test ordering
    AwaitCleanup, // removing the burst's leftover
};

// Three characters, five bytes. "ế" is one character but three of them, which is the
// whole point: `deleteSurroundingText` counts characters per the spec, and an
// ASCII-only probe would pass on a client that counts bytes — then corrupt
// Vietnamese in the field.
constexpr char kProbeText[] = "aếb";
constexpr int kProbeChars = 3;

// A client may answer one write with several updates. Rather than deciding on the
// first, each step tolerates a few and gives up after this many.
constexpr int kMaxUpdatesPerStep = 5;

struct SelfTest {
    Step step = Step::Idle;
    std::string baseline;
    int updates = 0;
};

SelfTest &state() {
    static SelfTest value;
    return value;
}

void enter(Step step) {
    state().step = step;
    state().updates = 0;
}

void report(const char *step, nlohmann::json extra) {
    extra["ev"] = "selftest";
    extra["step"] = step;
    write(std::move(extra));
}

// Bytes gained (positive) or lost (negative) against the baseline. Leftovers mean
// the delete under-counted, a negative means it ate the user's own text.
long long drift(const std::string &now, const std::string &baseline) {
    return static_cast<long long>(now.size()) - static_cast<long long>(baseline.size());
}

} // namespace

void cancelSelfTest() {
    if (state().step != Step::Idle) {
        report("cancelled", {});
        enter(Step::Idle);
    }
}

void startSelfTest(fcitx::InputContext *ic) {
    if (ic == nullptr) return;
    if (!ic->surroundingText().isValid()) {
        // Without a readable document there is nothing to compare against, and a
        // blind delete is exactly what this is meant to avoid.
        report("refused", {{"reason", "no surrounding text"}});
        enter(Step::Idle);
        return;
    }
    state().baseline = textBeforeCursor(ic);
    enter(Step::AwaitWrite);
    report("start", {{"baselineLen", state().baseline.size()}});
    ic->commitString(kProbeText);
}

void advanceSelfTest(fcitx::InputContext *ic) {
    SelfTest &test = state();
    if (test.step == Step::Idle) return;
    const std::string now = textBeforeCursor(ic);
    const std::string &base = test.baseline;
    const bool exhausted = ++test.updates >= kMaxUpdatesPerStep;

    // The step has not landed yet. Keep waiting until the client has answered often
    // enough that the result clearly is not coming, then record where it stopped and
    // stand down — leaving any debris on screen, which is itself part of the answer.
    // `drift` names the failure: for the delete step, +5 means nothing was removed,
    // +2 or +4 means it counted bytes instead of characters, and a negative means it
    // ate the user's own text.
    const auto giveUp = [&](const char *what) {
        if (!exhausted) return;
        report(what, {{"drift", drift(now, base)}});
        enter(Step::Idle);
    };

    switch (test.step) {
    case Step::Idle:
        return;

    case Step::AwaitWrite:
        if (now != base + kProbeText) return giveUp("write-failed");
        report("wrote", {{"drift", drift(now, base)}});
        ic->deleteSurroundingText(-kProbeChars, kProbeChars);
        enter(Step::AwaitDelete);
        return;

    case Step::AwaitDelete:
        if (now != base) return giveUp("delete-failed");
        report("deleted", {});
        // The exact shape a retro-edit uses: commit, delete, commit — no waiting.
        ic->commitString("x");
        ic->deleteSurroundingText(-1, 1);
        ic->commitString("y");
        enter(Step::AwaitBurst);
        return;

    case Step::AwaitBurst:
        if (now != base + "y") return giveUp("order-failed");
        report("ordered", {});
        ic->deleteSurroundingText(-1, 1);
        enter(Step::AwaitCleanup);
        return;

    case Step::AwaitCleanup:
        if (now != base) return giveUp("cleanup-failed");
        report("done", {{"clean", true}});
        enter(Step::Idle);
        return;
    }
}

} // namespace funput::probe::detail
