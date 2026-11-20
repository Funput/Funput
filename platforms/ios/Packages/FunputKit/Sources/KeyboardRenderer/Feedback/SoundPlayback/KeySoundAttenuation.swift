#if canImport(UIKit)
import AVFoundation
import Foundation
import os

/// Writes a quieter copy of a bundled key sound, for players that take no volume.
///
/// Copies live in the caches directory under the build number, so a build that ships
/// new sounds never plays an old copy; the first load of a new build clears the
/// copies of earlier ones. iOS may purge them at any time, and the next load writes
/// them again. Full gain needs no copy.
enum KeySoundAttenuation {
    private static let log = Logger(subsystem: "app.funput.keyboard", category: "KeySound")

    static func url(for source: URL, gain: Float) -> URL? {
        guard gain < 1 else { return source }
        guard let directory = cacheDirectory() else { return nil }
        let name = "\(source.deletingPathExtension().lastPathComponent)-\(Int((gain * 100).rounded())).wav"
        let target = directory.appendingPathComponent(name)
        if FileManager.default.fileExists(atPath: target.path) { return target }
        do {
            try writeCopy(of: source, gain: gain, to: target)
            return target
        } catch {
            log.error("Key sound copy failed: \(error.localizedDescription, privacy: .public)")
            return nil
        }
    }

    private static func cacheDirectory() -> URL? {
        let files = FileManager.default
        guard let caches = files.urls(for: .cachesDirectory, in: .userDomainMask).first else { return nil }
        let root = caches.appendingPathComponent("KeySounds", isDirectory: true)
        let build = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "0"
        let directory = root.appendingPathComponent(build, isDirectory: true)
        if files.fileExists(atPath: directory.path) { return directory }
        try? files.removeItem(at: root)
        do {
            try files.createDirectory(at: directory, withIntermediateDirectories: true)
            return directory
        } catch {
            log.error("Key sound cache unavailable: \(error.localizedDescription, privacy: .public)")
            return nil
        }
    }

    /// Writes beside the target first, so a copy cut short never takes its name.
    private static func writeCopy(of source: URL, gain: Float, to target: URL) throws {
        let input = try AVAudioFile(forReading: source)
        let buffer = try scaledSamples(of: input, gain: gain)
        // AVAudioFile picks the container from the extension, so `.wav` must stay last.
        let partial = target.deletingPathExtension().appendingPathExtension("partial.wav")
        try? FileManager.default.removeItem(at: partial)
        try write(buffer, settings: input.fileFormat.settings, to: partial)
        try FileManager.default.moveItem(at: partial, to: target)
    }

    private static func scaledSamples(of input: AVAudioFile, gain: Float) throws -> AVAudioPCMBuffer {
        let format = input.processingFormat
        guard let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: AVAudioFrameCount(input.length)) else {
            throw CocoaError(.fileReadCorruptFile)
        }
        try input.read(into: buffer)
        guard let channels = buffer.floatChannelData else { throw CocoaError(.fileReadCorruptFile) }
        // Interleaved samples share one pointer; deinterleaved ones get one per channel.
        let pointers = format.isInterleaved ? 1 : Int(format.channelCount)
        let samples = Int(buffer.frameLength) * Int(format.channelCount) / pointers
        for channel in 0..<pointers {
            for index in 0..<samples { channels[channel][index] *= gain }
        }
        return buffer
    }

    /// The file is finished when `output` goes out of scope, before the caller moves it.
    private static func write(_ buffer: AVAudioPCMBuffer, settings: [String: Any], to url: URL) throws {
        let output = try AVAudioFile(
            forWriting: url,
            settings: settings,
            commonFormat: buffer.format.commonFormat,
            interleaved: buffer.format.isInterleaved
        )
        try output.write(from: buffer)
    }
}
#endif
