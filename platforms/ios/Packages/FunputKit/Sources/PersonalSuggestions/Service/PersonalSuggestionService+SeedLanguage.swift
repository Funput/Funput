import KeyboardInput

extension PersonalSuggestionService {
    /// Follows the composer's language and tone style. The host subscribes it to
    /// `KeyboardInputCoordinator.onSeedLanguageChange`, which hands over the current
    /// value and every change after it; that is the only delivery.
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
