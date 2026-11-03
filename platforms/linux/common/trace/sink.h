// The trace's file, and nothing about what goes into it.
//
// Private by construction. The `funput` cache directory is made 0700 and the file
// is created 0600 by the `open(2)` call itself, not by a chmod after it, so there is
// no moment at which another user could read a line; a file an older build left
// with the umask's mode is tightened on open. Only the directory the trace makes
// is private: `~/.cache` belongs to the user and keeps whatever mode it has.
//
// Writes go through the descriptor. A stream cannot name a creation mode, and a
// line handed to `write(2)` is in the page cache when the call returns, which is
// what "flushed" has to mean for a session that ends in a crash.
//
// Append across restarts, bounded. A file already past half the bound when it is
// opened is first rotated to `<path>.1` (one generation, same mode): a fresh
// process's ring holds nothing of the previous session, so its first rewrite would
// otherwise wipe exactly the session that crashed.
//
// Nothing here throws. Every operation says whether it worked, and the trace
// switches itself off on the first that did not.

#ifndef FUNPUT_TRACE_SINK_H
#define FUNPUT_TRACE_SINK_H

#include <cstddef>
#include <deque>
#include <string>

namespace funput {

class TraceSink {
public:
    TraceSink(std::string path, size_t maxBytes);
    ~TraceSink();
    TraceSink(const TraceSink &) = delete;
    TraceSink &operator=(const TraceSink &) = delete;

    const std::string &path() const { return path_; }
    bool isOpen() const { return fd_ >= 0; }
    // Make the directory, rotate if due, open for append. False on failure.
    bool open();
    void close();

    // Whether one more line of `lineBytes` (plus its newline) stays within the bound.
    bool fits(size_t lineBytes) const { return bytes_ + lineBytes + 1 <= maxBytes_; }
    bool append(const std::string &line);
    // The file becomes exactly these lines.
    bool rewrite(const std::deque<std::string> &lines);

private:
    std::string path_;
    size_t maxBytes_;
    int fd_ = -1;
    size_t bytes_ = 0;
};

} // namespace funput

#endif // FUNPUT_TRACE_SINK_H
