extension PersonalSuggestionService {
    /// Starts another host app's typing on this service.
    ///
    /// One service serves every host app in the process, so that there is one worker
    /// and one open store instead of one per app. What the bar was answering goes, and
    /// anything still in flight is refused by the generation it carries.
    ///
    /// The worker stays when the last host and this one both have Full Access: its
    /// store is on disk and is the same for every app. Without it the learned words
    /// live in the worker's memory alone and belong to the app they were typed in, so
    /// the worker goes with them and the next request makes a new one.
    public func beginSession(hasFullAccess: Bool) {
        clear()
        guard !(workerConfiguration.hasFullAccess && hasFullAccess) else { return }
        worker?.flush()
        worker = nil
    }
}
