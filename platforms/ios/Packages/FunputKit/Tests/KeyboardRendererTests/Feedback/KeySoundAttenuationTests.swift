#if canImport(UIKit)
import AVFoundation
import FunputShared
import Testing
@testable import KeyboardRenderer

/// The quieter copies that system sounds play, since a system sound takes no volume.
struct KeySoundAttenuationTests {
    @Test("Full gain plays the bundled file itself")
    func fullGainIsTheSource() throws {
        let source = try #require(KeySoundPlayerPool.resourceURL(style: .wood, kind: .key))
        #expect(KeySoundAttenuation.url(for: source, gain: 1) == source)
    }

    @Test("A copy keeps the sound's length and scales its peak", arguments: [KeySoundVolume.soft, .medium])
    func copyIsScaled(volume: KeySoundVolume) throws {
        let source = try #require(KeySoundPlayerPool.resourceURL(style: .mechanical, kind: .delete))
        let copy = try #require(KeySoundAttenuation.url(for: source, gain: volume.gain))
        defer { try? FileManager.default.removeItem(at: copy) }

        let original = try Self.read(source)
        let quieter = try Self.read(copy)
        #expect(quieter.frames == original.frames)
        #expect(original.peak > 0)
        // 16-bit samples round, so allow one step of quantisation either way.
        #expect(abs(quieter.peak - original.peak * volume.gain) < 0.001)
    }

    @Test("A second request reuses the copy instead of writing it again")
    func copyIsCached() throws {
        let source = try #require(KeySoundPlayerPool.resourceURL(style: .bubble, kind: .modifier))
        let first = try #require(KeySoundAttenuation.url(for: source, gain: KeySoundVolume.soft.gain))
        defer { try? FileManager.default.removeItem(at: first) }
        let written = try FileManager.default.attributesOfItem(atPath: first.path)[.modificationDate] as? Date
        let second = try #require(KeySoundAttenuation.url(for: source, gain: KeySoundVolume.soft.gain))
        let reused = try FileManager.default.attributesOfItem(atPath: second.path)[.modificationDate] as? Date
        #expect(first == second)
        #expect(written == reused)
    }

    private static func read(_ url: URL) throws -> (frames: AVAudioFramePosition, peak: Float) {
        let file = try AVAudioFile(forReading: url)
        let buffer = try #require(AVAudioPCMBuffer(pcmFormat: file.processingFormat, frameCapacity: AVAudioFrameCount(file.length)))
        try file.read(into: buffer)
        let channels = try #require(buffer.floatChannelData)
        var peak: Float = 0
        for channel in 0..<Int(file.processingFormat.channelCount) {
            for index in 0..<Int(buffer.frameLength) { peak = max(peak, abs(channels[channel][index])) }
        }
        return (file.length, peak)
    }
}
#endif
