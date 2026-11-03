#include "trace/sink.h"

#include <fcntl.h>
#include <sys/stat.h>
#include <unistd.h>

#include <cerrno>
#include <cstdio>
#include <filesystem>
#include <system_error>

namespace funput {

namespace {

constexpr mode_t kPrivateFile = S_IRUSR | S_IWUSR; // 0600
constexpr mode_t kPrivateDir = S_IRWXU;            // 0700

// The leaf (`funput`) is ours and is made private; anything above it is the user's
// and is only created if missing, with the umask like any other directory of theirs.
bool makeDirectory(const std::filesystem::path &dir) {
    if (dir.empty()) return true;
    std::error_code ec;
    std::filesystem::create_directories(dir.parent_path(), ec);
    if (ec) return false;
    return ::mkdir(dir.c_str(), kPrivateDir) == 0 || errno == EEXIST;
}

bool writeAll(int fd, const char *data, size_t size) {
    while (size > 0) {
        const ssize_t written = ::write(fd, data, size);
        if (written < 0) {
            if (errno == EINTR) continue;
            return false;
        }
        data += written;
        size -= static_cast<size_t>(written);
    }
    return true;
}

} // namespace

TraceSink::TraceSink(std::string path, size_t maxBytes)
    : path_(std::move(path)), maxBytes_(maxBytes) {}

TraceSink::~TraceSink() {
    close();
}

bool TraceSink::open() {
    close();
    if (path_.empty()) return false;
    const std::filesystem::path file(path_);
    if (!makeDirectory(file.parent_path())) return false;
    // A rotation that fails only loses the previous session's protection, not the
    // trace: carry on appending.
    std::error_code ec;
    const auto existing = std::filesystem::file_size(file, ec);
    if (!ec && existing > maxBytes_ / 2) {
        const std::string previous = path_ + ".1";
        if (std::rename(path_.c_str(), previous.c_str()) == 0) {
            ::chmod(previous.c_str(), kPrivateFile);
        }
    }
    fd_ = ::open(path_.c_str(), O_WRONLY | O_CREAT | O_APPEND | O_CLOEXEC, kPrivateFile);
    if (fd_ < 0) return false;
    struct stat status {};
    if (::fstat(fd_, &status) != 0) {
        close();
        return false;
    }
    if ((status.st_mode & 0777) != kPrivateFile) ::fchmod(fd_, kPrivateFile);
    bytes_ = static_cast<size_t>(status.st_size);
    return true;
}

void TraceSink::close() {
    if (fd_ >= 0) ::close(fd_);
    fd_ = -1;
    bytes_ = 0;
}

bool TraceSink::append(const std::string &line) {
    if (fd_ < 0) return false;
    // One write per line, newline included: a reader never sees a line without it.
    std::string out;
    out.reserve(line.size() + 1);
    out += line;
    out += '\n';
    if (!writeAll(fd_, out.data(), out.size())) return false;
    bytes_ += out.size();
    return true;
}

bool TraceSink::rewrite(const std::deque<std::string> &lines) {
    // O_APPEND: after the truncation every write lands at the new end, offset 0.
    if (fd_ < 0 || ::ftruncate(fd_, 0) != 0) return false;
    bytes_ = 0;
    for (const std::string &line : lines) {
        if (!append(line)) return false;
    }
    return true;
}

} // namespace funput
