// MusicDrop Service Worker
const CACHE_NAME = 'musicdrop-v1.1';
const ASSETS_TO_CACHE = [
  './',
  './index.html',
  './app.css',
  './app.js',
  './manifest.json',
  './mascot.png',
  './icons/icon-192.png',
  './icons/icon-512.png',
  './icons/apple-touch-icon.png',
  './icons/favicon.png'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(ASSETS_TO_CACHE);
    }).then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.map((key) => {
          if (key !== CACHE_NAME) {
            return caches.delete(key);
          }
        })
      );
    }).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);

  // Allow live API and YouTube requests to go straight to network
  if (url.origin.includes('youtube.com') || 
      url.origin.includes('googlevideo.com') ||
      url.origin.includes('googleapis.com') ||
      url.origin.includes('invidious') ||
      url.origin.includes('piped')) {
    return;
  }

  event.respondWith(
    caches.match(event.request).then((cachedResponse) => {
      return cachedResponse || fetch(event.request).then((networkResponse) => {
        return networkResponse;
      });
    }).catch(() => {
      if (event.request.mode === 'navigate') {
        return caches.match('./index.html');
      }
    })
  );
});
