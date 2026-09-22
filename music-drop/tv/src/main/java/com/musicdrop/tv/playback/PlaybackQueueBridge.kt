package com.musicdrop.tv.playback

/**
 * Lets the system-level MediaSession player (lock screen / notification / Bluetooth
 * "next" & "previous" controls, handled inside [FileDropMediaService]'s ForwardingPlayer)
 * defer to the app's own queue logic in [com.musicdrop.tv.ui.viewmodel.MainViewModel]
 * instead of only ever asking the raw ExoPlayer instance.
 *
 * Why this exists: ExoPlayer's own playlist usually only holds ONE resolved media item
 * at a time for streamed (YouTube) sources, because the next track's stream URL isn't
 * known/resolved yet — so `player.hasNextMediaItem()` is false even though the app has
 * a perfectly good "up next" queue (recommended / same-artist / user-picked) ready to
 * go. Without this bridge, the lock screen's Next button would silently do nothing for
 * anything except a fully front-loaded local/offline queue.
 *
 * MainViewModel registers real implementations of these at startup; FileDropMediaService
 * (which runs in the same process) just calls them. Same process is important — this
 * would NOT work if the service ran in a separate `:process`.
 */
object PlaybackQueueBridge {
    var hasNext: () -> Boolean = { false }
    var hasPrevious: () -> Boolean = { false }
    var onNext: () -> Unit = {}
    var onPrevious: () -> Unit = {}
}
