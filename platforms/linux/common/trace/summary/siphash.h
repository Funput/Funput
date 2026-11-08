// SipHash-2-4: the keyed hash behind a summary's `hash` field.
//
// A summary may let a reader match two strings within one trace, and nothing more.
// A plain hash with a salt mixed in does not deliver that: FNV-1a keeps a 32-bit
// state, the salt only moves it, one known string (or two unknown short ones) gives
// the state back, and from there every hash in the file falls to a dictionary.
// SipHash is a PRF: without its 128-bit key no hash says anything about its input,
// and no number of (input, hash) pairs gives the key back. The key is drawn from the
// OS once per process and lives only in the engine's memory.
//
// Aumasson & Bernstein, "SipHash: a fast short-input PRF" (2012). The implementation
// is checked against the paper's test vectors in tests/trace/summary/siphash_tests.cpp.

#ifndef FUNPUT_TRACE_SUMMARY_SIPHASH_H
#define FUNPUT_TRACE_SUMMARY_SIPHASH_H

#include <cstddef>
#include <cstdint>
#include <random>
#include <string_view>

namespace funput {

struct SipKey {
    uint64_t k0 = 0;
    uint64_t k1 = 0;
};

inline SipKey randomSipKey() {
    std::random_device device;
    const auto word = [&device] { return (static_cast<uint64_t>(device()) << 32) | device(); };
    return {word(), word()};
}

namespace siphash {

inline uint64_t rotl(uint64_t x, int bits) {
    return (x << bits) | (x >> (64 - bits));
}

// Bytes as a little-endian word; `count` < 8 leaves the high bytes zero.
inline uint64_t word(const unsigned char *bytes, size_t count) {
    uint64_t out = 0;
    for (size_t i = 0; i < count; ++i) out |= static_cast<uint64_t>(bytes[i]) << (8 * i);
    return out;
}

struct State {
    uint64_t v0, v1, v2, v3;

    void round() {
        v0 += v1;
        v1 = rotl(v1, 13);
        v1 ^= v0;
        v0 = rotl(v0, 32);
        v2 += v3;
        v3 = rotl(v3, 16);
        v3 ^= v2;
        v0 += v3;
        v3 = rotl(v3, 21);
        v3 ^= v0;
        v2 += v1;
        v1 = rotl(v1, 17);
        v1 ^= v2;
        v2 = rotl(v2, 32);
    }

    // One message word, with the c = 2 compression rounds.
    void absorb(uint64_t m) {
        v3 ^= m;
        round();
        round();
        v0 ^= m;
    }
};

} // namespace siphash

// 64-bit SipHash-2-4 of `data` under `key`.
inline uint64_t siphash24(std::string_view data, const SipKey &key) {
    siphash::State s{key.k0 ^ 0x736f6d6570736575ULL, key.k1 ^ 0x646f72616e646f6dULL,
                     key.k0 ^ 0x6c7967656e657261ULL, key.k1 ^ 0x7465646279746573ULL};
    const auto *bytes = reinterpret_cast<const unsigned char *>(data.data());
    size_t i = 0;
    for (; i + 8 <= data.size(); i += 8) s.absorb(siphash::word(bytes + i, 8));
    // The last word: the remaining bytes, and the length in the top byte.
    s.absorb(siphash::word(bytes + i, data.size() - i) |
             (static_cast<uint64_t>(data.size() & 0xff) << 56));
    s.v2 ^= 0xff;
    for (int d = 0; d < 4; ++d) s.round(); // d = 4 finalization rounds
    return s.v0 ^ s.v1 ^ s.v2 ^ s.v3;
}

} // namespace funput

#endif // FUNPUT_TRACE_SUMMARY_SIPHASH_H
