import KeyboardInput

extension PersonalSuggestionService {
    /// Follows the composer's language and tone style; the host calls it with
    /// `KeyboardInputCoordinator.seedLanguage` at activation and from
    /// `onSeedLanguageChange` after that.
    ///
    /// Kept in the worker configuration, so a worker created later starts with it, and
    /// sent to a live worker on its own, so a change does not rerun the configure path
    /// (store checks and pending dictionary edits) on every language switch.
    public func setSeedLanguage(_ language: SuggestionSeedLanguage) {
        guard language != workerConfiguration.seedLanguage else { return }
        workerConfiguration = .init(
            enabled: workerConfiguration.enabled,
            hasFullAccess: workerConfiguration.hasFullAccess,
            resetToken: workerConfiguration.resetToken,
            seedLanguage: language
        )
        worker?.setSeedLanguage(language)
    }
}
