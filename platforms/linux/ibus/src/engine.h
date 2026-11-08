// IBus input-method engine for Funput. Drives `funput-engine` (via the C ABI)
// using IBus's preedit/commit model — the same shape as the Fcitx5 addon
// (platforms/linux/fcitx5/src/funput_engine.cpp) and the macOS IMKit shell, NOT
// the Windows backspace-injection path. The composing word is shown as preedit and
// committed on a word boundary, navigation key, or VI/EN toggle. The preedit is
// published with IBUS_ENGINE_PREEDIT_COMMIT, so IBus itself flushes the word on
// focus loss, reset and engine switch — see updatePreedit() in engine/client.cpp
// and the focusOut()/disable()/reset() handlers, which must not commit again.
//
// Unlike the Fcitx5 addon (an in-process .so), an IBus engine is a separate
// process launched by ibus-daemon over D-Bus; see main.cpp for the bus glue.

#ifndef FUNPUT_IBUS_ENGINE_H
#define FUNPUT_IBUS_ENGINE_H

#include <ibus.h>

// Whether libibus can name the focused client: `focus-in-id` and the construct-only
// `has-focus-id` property arrived in 1.5.27 (src/ibusengine.h at that tag has the
// vfuncs; 1.5.26 has not). Below that, an engine is built the default way and the
// daemon calls `focus-in` with no client, and the shell behaves as it did before
// PR 5: unnamed, `Transport::Direct`, the gnome-shell guard inert. Overridable from
// the compiler command line (`-DFUNPUT_IBUS_FOCUS_ID=0`) so that fallback path can be
// compiled against a newer libibus and does not rot unseen.
#ifndef FUNPUT_IBUS_FOCUS_ID
#define FUNPUT_IBUS_FOCUS_ID IBUS_CHECK_VERSION(1, 5, 27)
#endif

G_BEGIN_DECLS

// Named IBusFunputEngine (not FunputEngine) to avoid clashing with the FFI's
// opaque `typedef struct FunputEngine FunputEngine;` from funput.h.
#define FUNPUT_TYPE_ENGINE (ibus_funput_engine_get_type())
#define FUNPUT_ENGINE(obj) \
    (G_TYPE_CHECK_INSTANCE_CAST((obj), FUNPUT_TYPE_ENGINE, IBusFunputEngine))

typedef struct _IBusFunputEngine IBusFunputEngine;
typedef struct _IBusFunputEngineClass IBusFunputEngineClass;

GType ibus_funput_engine_get_type(void);

G_END_DECLS

#endif // FUNPUT_IBUS_ENGINE_H
