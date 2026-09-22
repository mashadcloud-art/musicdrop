package com.musicdrop.tv.chromium

object TvAdBypassScript {
    val SCRIPT = """
        (function() {
            if (window._musicDropInjected) return;
            window._musicDropInjected = true;
            console.log('[MusicDrop TV] Initializing YouTube TV Chromium Ad-Bypass Engine...');

            // 1. Auto-skip ad elements immediately as soon as they appear in DOM
            function skipAds() {
                try {
                    // Click official skip ad buttons
                    var skipButtons = document.querySelectorAll(
                        '.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, .videoAdUiSkipButton, [id^="skip-button"]'
                    );
                    for (var i = 0; i < skipButtons.length; i++) {
                        skipButtons[i].click();
                        console.log('[MusicDrop TV] Skipped ad button clicked');
                    }

                    // Force finish any video playing in an ad container
                    var adContainer = document.querySelector('.ad-showing, .ad-interrupting');
                    if (adContainer) {
                        var videos = document.querySelectorAll('video');
                        for (var j = 0; j < videos.length; j++) {
                            var v = videos[j];
                            if (v && !isNaN(v.duration) && v.duration > 0) {
                                v.currentTime = v.duration - 0.1;
                                v.playbackRate = 16.0;
                                console.log('[MusicDrop TV] Fast-forwarded ad segment');
                            }
                        }
                    }

                    // Remove ad banners and overlays
                    var overlays = document.querySelectorAll(
                        '.ytp-ad-overlay-container, .ytp-ad-message-container, ytd-action-companion-ad-renderer, #player-ads'
                    );
                    for (var k = 0; k < overlays.length; k++) {
                        overlays[k].style.display = 'none';
                    }
                } catch(e) {}
            }

            setInterval(skipAds, 400);

            // 2. Track currently playing video ID and notify Native Bridge for downloads
            function trackVideo() {
                try {
                    var url = window.location.href;
                    var match = url.match(/[?&]v=([a-zA-Z0-9_-]{11})/);
                    var videoId = match ? match[1] : '';
                    if (!videoId) {
                        // Check if in /watch/ or /tv#/watch
                        var pathMatch = url.match(/watch\/([a-zA-Z0-9_-]{11})/);
                        if (pathMatch) videoId = pathMatch[1];
                    }

                    var titleElem = document.querySelector('.ytp-title-link, h1, .title');
                    var title = titleElem ? titleElem.innerText : '';

                    if (videoId && window.MusicDropBridge) {
                        window.MusicDropBridge.onVideoStarted(videoId, title);
                    }
                } catch(e) {}
            }

            setInterval(trackVideo, 1500);

            // 3. Ensure remote D-pad focus outline is crisp and bright
            var style = document.createElement('style');
            style.innerHTML = `
                :focus {
                    outline: 3px solid #FFFFFF !important;
                    outline-offset: 2px !important;
                    box-shadow: 0 0 15px rgba(255, 255, 255, 0.8) !important;
                }
                .ad-showing, .ad-interrupting {
                    display: none !important;
                }
            `;
            document.head.appendChild(style);
        })();
    """.trimIndent()
}
