package com.musicdrop.tv.chromium

object TvAdBypassScript {
    val SCRIPT = """
        (function() {
            if (window._musicDropInjected) return;
            window._musicDropInjected = true;
            console.log('[MusicDrop TV] Initializing YouTube TV Ad & Sign-in Bypass Engine...');

            // 1. Consent cookies
            try {
                document.cookie = "SOCS=CAESEwgDEgk0ODEzNzk5NDIaAmVuIAEaBgiA_LyaBg; path=/; domain=.youtube.com; max-age=31536000; SameSite=None; Secure";
                document.cookie = "CONSENT=YES+cb.20210720-07-p0.en+FX+417; path=/; domain=.youtube.com; max-age=31536000; SameSite=None; Secure";
            } catch(e) {}

            // 2. Automatically dismiss Google Sign-In & Guest Dialogs
            function dismissSignInDialogs() {
                try {
                    // Click buttons like Skip, Use signed out, Guest, Dismiss, Not now, Cancel
                    var clickables = document.querySelectorAll('button, [role="button"], a, div');
                    for (var i = 0; i < clickables.length; i++) {
                        var el = clickables[i];
                        var txt = (el.innerText || el.textContent || '').trim().toLowerCase();
                        if (txt === 'skip' || 
                            txt === 'use youtube signed out' || 
                            txt === 'use signed out' || 
                            txt === 'continue signed out' || 
                            txt === 'continue as guest' || 
                            txt === 'guest' || 
                            txt === 'not now' || 
                            txt === 'dismiss' || 
                            txt === 'cancel' || 
                            txt === 'remind me later' ||
                            txt === 'no thanks' ||
                            txt === 'stay signed out') {
                            console.log('[MusicDrop TV] Auto-clicked sign-in dismissal button:', txt);
                            el.click();
                        }
                    }

                    // Hide modals and overlays that demand login or block TV viewing
                    var dialogs = document.querySelectorAll(
                        'yt-confirm-dialog-renderer, ytd-popup-container, .yt-dialog, [role="dialog"], .upsell-dialog, ytd-consent-bump-v2-lightbox, #dialog, .yt-spec-modal-overlay, .dialog-container, ytd-mealbar-promo-renderer'
                    );
                    for (var j = 0; j < dialogs.length; j++) {
                        var dText = (dialogs[j].innerText || '').toLowerCase();
                        if (dText.includes('sign in') || dText.includes('guest') || dText.includes('account') || dText.includes('agree')) {
                            var cancelBtn = dialogs[j].querySelector('[aria-label="Dismiss"], [aria-label="Cancel"], [aria-label="Close"], button');
                            if (cancelBtn) cancelBtn.click();
                            dialogs[j].style.display = 'none';
                        }
                    }
                } catch(e) {}
            }
            setInterval(dismissSignInDialogs, 400);

            // 3. Auto-skip ad elements immediately as soon as they appear in DOM
            function skipAds() {
                try {
                    var skipButtons = document.querySelectorAll(
                        '.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, .videoAdUiSkipButton, [id^="skip-button"], div.ad-skip-button, button.skip'
                    );
                    for (var i = 0; i < skipButtons.length; i++) {
                        skipButtons[i].click();
                        console.log('[MusicDrop TV] Skipped ad button clicked');
                    }

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

                    var overlays = document.querySelectorAll(
                        '.ytp-ad-overlay-container, .ytp-ad-message-container, ytd-action-companion-ad-renderer, #player-ads'
                    );
                    for (var k = 0; k < overlays.length; k++) {
                        overlays[k].style.display = 'none';
                    }
                } catch(e) {}
            }
            setInterval(skipAds, 400);

            // 4. Robust Video Extraction & Bridge Notification
            window.MusicDropGetVideoInfo = function() {
                var id = '';
                var title = '';

                try {
                    // Check HTML5 player API
                    var p = document.getElementById('movie_player') || document.querySelector('.html5-video-player');
                    if (p && typeof p.getVideoData === 'function') {
                        var d = p.getVideoData();
                        if (d && d.video_id) {
                            id = d.video_id;
                            title = d.title || '';
                        }
                    }

                    // Check player URL
                    if (!id && p && typeof p.getVideoUrl === 'function') {
                        var u = p.getVideoUrl() || '';
                        var m = u.match(/[?&]v=([a-zA-Z0-9_-]{11})/);
                        if (m) id = m[1];
                    }

                    // Check location href & hash
                    if (!id) {
                        var loc = window.location.href + ' ' + window.location.hash;
                        var m2 = loc.match(/(?:v=|watch\/|embed\/|vi\/|video\/)([a-zA-Z0-9_-]{11})/);
                        if (m2) id = m2[1];
                    }

                    // Title fallback
                    if (!title) {
                        var tElem = document.querySelector('.ytp-title-link, .video-title, h1, .title, .ytp-title-text');
                        if (tElem) title = tElem.innerText || tElem.textContent || '';
                    }
                    if (!title) title = document.title || '';
                } catch(e) {}

                return { id: id, title: title };
            };

            var lastNotifiedId = '';
            function syncVideoToBridge() {
                var info = window.MusicDropGetVideoInfo();
                if (info.id && info.id !== lastNotifiedId) {
                    lastNotifiedId = info.id;
                    console.log('[MusicDrop TV] Active video detected:', info.id, info.title);
                    if (window.MusicDropBridge) {
                        window.MusicDropBridge.onVideoStarted(info.id, info.title);
                    }
                }
            }
            setInterval(syncVideoToBridge, 1000);

            // Listen on HTML5 video play event to sync immediately
            document.addEventListener('play', function(e) {
                if (e.target && e.target.tagName === 'VIDEO') {
                    setTimeout(syncVideoToBridge, 300);
                }
            }, true);

            // 5. Remote D-pad focus outline and CSS cleanup
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
                yt-dialog, ytd-popup-container, .yt-dialog, .upsell-dialog, ytd-consent-bump-v2-lightbox, .sign-in-promo {
                    display: none !important;
                }
            `;
            document.head.appendChild(style);
        })();
    """.trimIndent()
}

