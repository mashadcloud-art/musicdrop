// MusicDrop Web PWA Engine
// Live Search, Media Downloader, and YouTube Player Engine

const DEFAULT_PLAYLIST = [
  { id: 'dQw4w9WgXcQ', title: 'Never Gonna Give You Up', artist: 'Rick Astley', thumb: 'https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg', duration: '3:32' },
  { id: '4NRXx6U8ABQ', title: 'Blinding Lights', artist: 'The Weeknd', thumb: 'https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg', duration: '4:22' },
  { id: 'JGwWNGJdvx8', title: 'Shape of You', artist: 'Ed Sheeran', thumb: 'https://i.ytimg.com/vi/JGwWNGJdvx8/hqdefault.jpg', duration: '4:23' },
  { id: 'kJQP7kiw5Fk', title: 'Despacito', artist: 'Luis Fonsi ft. Daddy Yankee', thumb: 'https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg', duration: '4:41' },
  { id: 'OPf0YbXqDm0', title: 'Uptown Funk', artist: 'Mark Ronson ft. Bruno Mars', thumb: 'https://i.ytimg.com/vi/OPf0YbXqDm0/hqdefault.jpg', duration: '4:30' },
  { id: 'fJ9rUzIMcZQ', title: 'Bohemian Rhapsody', artist: 'Queen', thumb: 'https://i.ytimg.com/vi/fJ9rUzIMcZQ/hqdefault.jpg', duration: '5:59' },
  { id: 'hT_nvWreIhg', title: 'Counting Stars', artist: 'OneRepublic', thumb: 'https://i.ytimg.com/vi/hT_nvWreIhg/hqdefault.jpg', duration: '4:43' },
  { id: '09R8_2nJtjg', title: 'Sugar', artist: 'Maroon 5', thumb: 'https://i.ytimg.com/vi/09R8_2nJtjg/hqdefault.jpg', duration: '5:01' },
  { id: 'YQHsXMglC9A', title: 'Hello', artist: 'Adele', thumb: 'https://i.ytimg.com/vi/YQHsXMglC9A/hqdefault.jpg', duration: '6:06' },
  { id: 'CevxZvSJLk8', title: 'Roar', artist: 'Katy Perry', thumb: 'https://i.ytimg.com/vi/CevxZvSJLk8/hqdefault.jpg', duration: '4:30' },
  { id: '2Vv-BfVoq4g', title: 'Perfect', artist: 'Ed Sheeran', thumb: 'https://i.ytimg.com/vi/2Vv-BfVoq4g/hqdefault.jpg', duration: '4:39' },
  { id: 'kffacxfA7G4', title: 'Baby Shark Dance', artist: 'Pinkfong', thumb: 'https://i.ytimg.com/vi/kffacxfA7G4/hqdefault.jpg', duration: '2:16' }
];

const CATEGORIES = {
  'trending': DEFAULT_PLAYLIST,
  'pop': [
    { id: 'JGwWNGJdvx8', title: 'Shape of You', artist: 'Ed Sheeran', thumb: 'https://i.ytimg.com/vi/JGwWNGJdvx8/hqdefault.jpg', duration: '4:23' },
    { id: '4NRXx6U8ABQ', title: 'Blinding Lights', artist: 'The Weeknd', thumb: 'https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg', duration: '4:22' },
    { id: 'YQHsXMglC9A', title: 'Hello', artist: 'Adele', thumb: 'https://i.ytimg.com/vi/YQHsXMglC9A/hqdefault.jpg', duration: '6:06' },
    { id: 'CevxZvSJLk8', title: 'Roar', artist: 'Katy Perry', thumb: 'https://i.ytimg.com/vi/CevxZvSJLk8/hqdefault.jpg', duration: '4:30' },
    { id: '1nFxMuPYeKA', title: 'Shake It Off', artist: 'Taylor Swift', thumb: 'https://i.ytimg.com/vi/1nFxMuPYeKA/hqdefault.jpg', duration: '4:02' }
  ],
  'hiphop': [
    { id: 'tvTRZJ-4EyI', title: 'HUMBLE.', artist: 'Kendrick Lamar', thumb: 'https://i.ytimg.com/vi/tvTRZJ-4EyI/hqdefault.jpg', duration: '3:04' },
    { id: 'uxpDa-c-4Mc', title: 'God\'s Plan', artist: 'Drake', thumb: 'https://i.ytimg.com/vi/uxpDa-c-4Mc/hqdefault.jpg', duration: '5:56' },
    { id: 'L_LUpnjgPso', title: 'Starboy', artist: 'The Weeknd ft. Daft Punk', thumb: 'https://i.ytimg.com/vi/L_LUpnjgPso/hqdefault.jpg', duration: '3:50' },
    { id: 'JDb3ZZD4bA0', title: 'Marvins Room', artist: 'Drake', thumb: 'https://i.ytimg.com/vi/JDb3ZZD4bA0/hqdefault.jpg', duration: '5:47' }
  ],
  'electronic': [
    { id: 'IcrbM1l_BoI', title: 'Wake Me Up', artist: 'Avicii', thumb: 'https://i.ytimg.com/vi/IcrbM1l_BoI/hqdefault.jpg', duration: '4:32' },
    { id: 'ALZHF5UqnU4', title: 'Alone', artist: 'Marshmello', thumb: 'https://i.ytimg.com/vi/ALZHF5UqnU4/hqdefault.jpg', duration: '3:19' },
    { id: '60ItHLz5WEA', title: 'Faded', artist: 'Alan Walker', thumb: 'https://i.ytimg.com/vi/60ItHLz5WEA/hqdefault.jpg', duration: '3:32' },
    { id: 'kOkQ4T5WO9E', title: 'This Is What You Came For', artist: 'Calvin Harris ft. Rihanna', thumb: 'https://i.ytimg.com/vi/kOkQ4T5WO9E/hqdefault.jpg', duration: '3:59' }
  ],
  'chill': [
    { id: 'jfKfPfyJRdk', title: 'lofi hip hop radio - beats to relax/study to', artist: 'Lofi Girl', thumb: 'https://i.ytimg.com/vi/jfKfPfyJRdk/hqdefault.jpg', duration: 'LIVE' },
    { id: '5qap5aO4i9A', title: 'lofi hip hop radio - beats to sleep/chill to', artist: 'Lofi Girl', thumb: 'https://i.ytimg.com/vi/5qap5aO4i9A/hqdefault.jpg', duration: 'LIVE' },
    { id: '2Vv-BfVoq4g', title: 'Perfect', artist: 'Ed Sheeran', thumb: 'https://i.ytimg.com/vi/2Vv-BfVoq4g/hqdefault.jpg', duration: '4:39' },
    { id: '09R8_2nJtjg', title: 'Sugar', artist: 'Maroon 5', thumb: 'https://i.ytimg.com/vi/09R8_2nJtjg/hqdefault.jpg', duration: '5:01' }
  ]
};

class MusicDropEngine {
  constructor() {
    this.currentTab = 'home';
    this.queue = [...DEFAULT_PLAYLIST];
    this.currentIndex = 0;
    this.isPlaying = false;
    this.isVideoMode = false;
    this.ytPlayer = null;
    this.ytReady = false;
    this.progressTimer = null;
    this.selectedDownloadTrack = null;

    this.downloads = JSON.parse(localStorage.getItem('musicdrop_downloads') || '[]');
    this.favorites = JSON.parse(localStorage.getItem('musicdrop_favs') || '[]');

    this.initDOM();
    this.initEvents();
    this.initYouTube();
    this.renderHomeTracks('trending');
    this.renderLibraryTracks();
    this.initPWA();
    this.startVisualizer();
  }

  initDOM() {
    // Dock elements
    this.dockThumb = document.getElementById('dock-thumb');
    this.dockTitle = document.getElementById('dock-title');
    this.dockArtist = document.getElementById('dock-artist');
    this.dockPlayBtn = document.getElementById('dock-play-btn');
    this.dockPrevBtn = document.getElementById('dock-prev-btn');
    this.dockNextBtn = document.getElementById('dock-next-btn');
    this.dockProgress = document.getElementById('dock-progress');
    this.dockFill = document.getElementById('dock-fill');
    this.dockCurrentTime = document.getElementById('dock-time-current');
    this.dockTotalTime = document.getElementById('dock-time-total');
    this.dockFavBtn = document.getElementById('dock-fav-btn');
    this.dockDownloadBtn = document.getElementById('dock-download-btn');
    this.dockVideoBtn = document.getElementById('dock-video-btn');

    // Fullscreen Overlay
    this.fullOverlay = document.getElementById('fullscreen-player');
    this.fullCollapseBtn = document.getElementById('btn-collapse');
    this.fullCover = document.getElementById('full-cover');
    this.fullTitle = document.getElementById('full-title');
    this.fullArtist = document.getElementById('full-artist');
    this.fullDownloadBtn = document.getElementById('full-download-btn');
    this.discView = document.getElementById('disc-view');
    this.videoView = document.getElementById('video-view');

    // Grids
    this.homeGrid = document.getElementById('home-tracks-grid');
    this.searchGrid = document.getElementById('search-tracks-grid');
    this.libraryGrid = document.getElementById('library-tracks-grid');
    this.libraryEmpty = document.getElementById('library-empty');

    // Search Inputs
    this.headerSearchInput = document.getElementById('search-input');
    this.headerSearchClear = document.getElementById('search-clear');
    this.suggestionsBox = document.getElementById('suggestions-box');
    this.largeSearchInput = document.getElementById('search-input-lg');
    this.btnSearchGo = document.getElementById('btn-search-go');
    this.searchLoading = document.getElementById('search-loading');
    this.searchCount = document.getElementById('search-count');

    // Modals
    this.downloadModal = document.getElementById('download-modal');
    this.dlThumb = document.getElementById('dl-thumb');
    this.dlTitle = document.getElementById('dl-title');
    this.dlArtist = document.getElementById('dl-artist');
    this.dlAudioLink = document.getElementById('dl-audio-link');
    this.dlVideoLink = document.getElementById('dl-video-link');
    this.dlOfflineBtn = document.getElementById('dl-offline-btn');
    this.btnCloseDl = document.getElementById('btn-close-dl');

    this.iosModal = document.getElementById('ios-modal');
    this.btnInstallHeader = document.getElementById('btn-install-header');
    this.btnDismissSheet = document.getElementById('btn-dismiss-sheet');

    // Initialize track in dock
    this.updateTrackUI(this.queue[0]);
  }

  initEvents() {
    // Tab switching
    document.querySelectorAll('.nav-item').forEach(item => {
      item.addEventListener('click', () => {
        const tab = item.dataset.tab;
        this.switchTab(tab);
      });
    });

    document.getElementById('nav-brand').addEventListener('click', () => {
      this.switchTab('home');
    });

    document.getElementById('btn-hero-search').addEventListener('click', () => {
      this.switchTab('search');
      setTimeout(() => this.largeSearchInput.focus(), 150);
    });

    document.getElementById('btn-hero-play').addEventListener('click', () => {
      this.loadTrack(this.queue[0], true);
    });

    // Clear library
    document.getElementById('btn-clear-library').addEventListener('click', () => {
      if (confirm('Clear all downloaded and saved songs from this device?')) {
        this.downloads = [];
        localStorage.setItem('musicdrop_downloads', JSON.stringify([]));
        this.renderLibraryTracks();
      }
    });

    // Fullscreen dock open
    document.getElementById('dock-info-wrap').addEventListener('click', () => {
      this.openFullscreenPlayer();
    });
    this.fullCollapseBtn.addEventListener('click', () => {
      this.closeFullscreenPlayer();
    });

    // Playback buttons
    this.dockPlayBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      this.togglePlay();
    });
    this.dockNextBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      this.playNext();
    });
    this.dockPrevBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      this.playPrev();
    });
    this.dockFavBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      this.toggleFavorite(this.queue[this.currentIndex]);
    });
    this.dockDownloadBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      this.openDownloadModal(this.queue[this.currentIndex]);
    });
    this.fullDownloadBtn.addEventListener('click', () => {
      this.openDownloadModal(this.queue[this.currentIndex]);
    });
    this.dockVideoBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      this.toggleVideoMode();
    });

    // Seek bar
    this.dockProgress.addEventListener('click', (e) => {
      const rect = this.dockProgress.getBoundingClientRect();
      const clickPos = (e.clientX - rect.left) / rect.width;
      if (this.ytPlayer && this.ytPlayer.getDuration) {
        const duration = this.ytPlayer.getDuration() || 0;
        const target = clickPos * duration;
        this.ytPlayer.seekTo(target, true);
        this.dockFill.style.width = (clickPos * 100) + '%';
      }
    });

    // Home Category chips
    document.querySelectorAll('.category-chip').forEach(chip => {
      chip.addEventListener('click', () => {
        document.querySelectorAll('.category-chip').forEach(c => c.classList.remove('active'));
        chip.classList.add('active');
        const cat = chip.dataset.category;
        document.getElementById('category-title').textContent = chip.textContent.trim();
        this.renderHomeTracks(cat);
      });
    });

    // Header Search Input with Suggestions
    let searchDebounce;
    this.headerSearchInput.addEventListener('input', (e) => {
      const q = e.target.value.trim();
      this.headerSearchClear.classList.toggle('active', q.length > 0);
      clearTimeout(searchDebounce);

      if (q.length > 1) {
        this.fetchSuggestions(q);
      } else {
        this.suggestionsBox.classList.remove('open');
      }

      searchDebounce = setTimeout(() => {
        if (q.length > 1) {
          this.switchTab('search');
          this.largeSearchInput.value = q;
          this.executeSearch(q);
        }
      }, 400);
    });

    this.headerSearchClear.addEventListener('click', () => {
      this.headerSearchInput.value = '';
      this.headerSearchClear.classList.remove('active');
      this.suggestionsBox.classList.remove('open');
    });

    // Large Search Page Input
    this.btnSearchGo.addEventListener('click', () => {
      const q = this.largeSearchInput.value.trim();
      if (q) this.executeSearch(q);
    });

    this.largeSearchInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        const q = this.largeSearchInput.value.trim();
        if (q) this.executeSearch(q);
      }
    });

    // Popular Search Tags
    document.querySelectorAll('.trend-tag').forEach(tag => {
      tag.addEventListener('click', () => {
        const q = tag.textContent.trim();
        this.largeSearchInput.value = q;
        this.executeSearch(q);
      });
    });

    // Close suggestions when clicking outside
    document.addEventListener('click', (e) => {
      if (!this.suggestionsBox.contains(e.target) && e.target !== this.headerSearchInput) {
        this.suggestionsBox.classList.remove('open');
      }
    });

    // Download Modal Events
    this.btnCloseDl.addEventListener('click', () => {
      this.downloadModal.classList.remove('open');
    });
    this.downloadModal.addEventListener('click', (e) => {
      if (e.target === this.downloadModal) {
        this.downloadModal.classList.remove('open');
      }
    });
    this.dlOfflineBtn.addEventListener('click', () => {
      if (this.selectedDownloadTrack) {
        this.saveTrackOffline(this.selectedDownloadTrack);
      }
    });

    // Install Modal
    this.btnInstallHeader.addEventListener('click', () => this.showInstallPrompt());
    this.btnDismissSheet.addEventListener('click', () => this.iosModal.classList.remove('open'));
  }

  switchTab(tabId) {
    this.currentTab = tabId;
    document.querySelectorAll('.tab-page').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));

    const activePage = document.getElementById('tab-' + tabId);
    const activeNav = document.getElementById('nav-' + tabId);
    if (activePage) activePage.classList.add('active');
    if (activeNav) activeNav.classList.add('active');

    if (tabId === 'library') {
      this.renderLibraryTracks();
    }
  }

  /* YouTube Live Suggestions */
  async fetchSuggestions(query) {
    try {
      const script = document.createElement('script');
      const callbackName = 'ytSuggestCallback_' + Math.floor(Math.random() * 1000000);
      window[callbackName] = (data) => {
        delete window[callbackName];
        document.body.removeChild(script);
        if (data && data[1] && data[1].length > 0) {
          this.renderSuggestions(data[1].slice(0, 6));
        } else {
          this.suggestionsBox.classList.remove('open');
        }
      };
      script.src = `https://suggestqueries.google.com/complete/search?client=youtube&ds=yt&q=${encodeURIComponent(query)}&jsonp=${callbackName}`;
      document.body.appendChild(script);
    } catch (e) {
      console.log('Suggest query error:', e);
    }
  }

  renderSuggestions(suggestions) {
    this.suggestionsBox.innerHTML = '';
    suggestions.forEach(item => {
      const text = typeof item === 'string' ? item : item[0];
      const div = document.createElement('div');
      div.className = 'suggestion-item';
      div.innerHTML = `
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
        <span>${text}</span>
      `;
      div.addEventListener('click', () => {
        this.headerSearchInput.value = text;
        this.largeSearchInput.value = text;
        this.suggestionsBox.classList.remove('open');
        this.switchTab('search');
        this.executeSearch(text);
      });
      this.suggestionsBox.appendChild(div);
    });
    this.suggestionsBox.classList.add('open');
  }

  /* Execute Real Live Search */
  async executeSearch(query) {
    this.searchLoading.style.display = 'flex';
    this.searchGrid.innerHTML = '';
    this.searchCount.textContent = `Searching for "${query}"...`;

    try {
      const apiUrl = `https://shnwazdev-ytmusicapi.vercel.app/api/search?query=${encodeURIComponent(query)}`;
      const resp = await fetch(apiUrl);
      if (!resp.ok) throw new Error('Search API HTTP ' + resp.status);
      const json = await resp.json();

      let songs = [];
      if (json && Array.isArray(json.data)) {
        songs = json.data
          .filter(item => item.videoId)
          .map(item => {
            const thumbUrl = (item.thumbnails && item.thumbnails.length > 0)
              ? item.thumbnails[item.thumbnails.length - 1].url
              : `https://i.ytimg.com/vi/${item.videoId}/hqdefault.jpg`;

            const artistName = (item.artists && Array.isArray(item.artists))
              ? item.artists.map(a => a.name).join(', ')
              : (item.artist || 'YouTube Artist');

            return {
              id: item.videoId,
              title: item.title || 'Untitled',
              artist: artistName,
              thumb: thumbUrl,
              duration: item.duration || '3:30'
            };
          });
      }

      this.searchLoading.style.display = 'none';

      if (songs.length > 0) {
        this.searchCount.textContent = `Found ${songs.length} real YouTube tracks for "${query}"`;
        this.queue = songs;
        this.renderTracksToGrid(this.searchGrid, songs);
      } else {
        this.fallbackSearch(query);
      }
    } catch (err) {
      console.log('Search error:', err);
      this.fallbackSearch(query);
    }
  }

  fallbackSearch(query) {
    this.searchLoading.style.display = 'none';
    const filtered = DEFAULT_PLAYLIST.filter(t =>
      t.title.toLowerCase().includes(query.toLowerCase()) ||
      t.artist.toLowerCase().includes(query.toLowerCase())
    );

    if (filtered.length > 0) {
      this.searchCount.textContent = `Showing ${filtered.length} matched tracks`;
      this.renderTracksToGrid(this.searchGrid, filtered);
    } else {
      this.searchCount.textContent = `No exact matches found for "${query}". Try another artist or song name!`;
    }
  }

  /* Render Track Cards */
  renderHomeTracks(cat) {
    const list = CATEGORIES[cat] || DEFAULT_PLAYLIST;
    this.queue = list;
    this.renderTracksToGrid(this.homeGrid, list);
  }

  renderLibraryTracks() {
    if (!this.downloads || this.downloads.length === 0) {
      this.libraryEmpty.style.display = 'flex';
      this.libraryGrid.style.display = 'none';
    } else {
      this.libraryEmpty.style.display = 'none';
      this.libraryGrid.style.display = 'grid';
      this.renderTracksToGrid(this.libraryGrid, this.downloads);
    }
  }

  renderTracksToGrid(container, tracks) {
    container.innerHTML = '';
    tracks.forEach((track, index) => {
      const card = document.createElement('div');
      card.className = 'track-card';
      card.dataset.id = track.id;
      if (this.queue[this.currentIndex] && this.queue[this.currentIndex].id === track.id) {
        card.classList.add('playing');
      }

      card.innerHTML = `
        <div class="thumb-wrap">
          <img class="track-thumb" src="${track.thumb}" alt="${track.title}" loading="lazy" />
          <div class="card-play-overlay">
            <div class="play-circle-btn">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
            </div>
          </div>
          <span class="track-duration">${track.duration || '3:30'}</span>
          <button class="card-dl-btn" title="Download song">
            <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/></svg>
          </button>
        </div>
        <div class="track-title" title="${track.title}">${track.title}</div>
        <div class="track-artist" title="${track.artist}">${track.artist}</div>
      `;

      // Play click
      card.addEventListener('click', (e) => {
        if (e.target.closest('.card-dl-btn')) {
          e.stopPropagation();
          this.openDownloadModal(track);
          return;
        }
        this.currentIndex = index;
        this.loadTrack(track, true);
      });

      container.appendChild(card);
    });
  }

  /* DOWNLOAD CENTER LOGIC */
  openDownloadModal(track) {
    this.selectedDownloadTrack = track;
    this.dlThumb.src = track.thumb;
    this.dlTitle.textContent = track.title;
    this.dlArtist.textContent = track.artist;

    // Direct audio & video download URLs
    // Using high-speed converter endpoints
    const videoUrl = `https://www.youtube.com/watch?v=${track.id}`;
    this.dlAudioLink.href = `https://loader.to/api/button/?url=${encodeURIComponent(videoUrl)}&f=mp3`;
    this.dlVideoLink.href = `https://loader.to/api/button/?url=${encodeURIComponent(videoUrl)}&f=1080`;

    this.downloadModal.classList.add('open');
  }

  saveTrackOffline(track) {
    if (!this.downloads.some(d => d.id === track.id)) {
      this.downloads.push(track);
      localStorage.setItem('musicdrop_downloads', JSON.stringify(this.downloads));
      alert(`"${track.title}" has been saved to your Offline In-App Library!`);
    } else {
      alert(`"${track.title}" is already in your Offline Library!`);
    }
    this.downloadModal.classList.remove('open');
  }

  /* YOUTUBE IFRAME ENGINE */
  initYouTube() {
    const tag = document.createElement('script');
    tag.src = 'https://www.youtube.com/iframe_api';
    const firstScript = document.getElementsByTagName('script')[0];
    firstScript.parentNode.insertBefore(tag, firstScript);

    window.onYouTubeIframeAPIReady = () => {
      this.ytPlayer = new YT.Player('yt-player-frame', {
        height: '100%',
        width: '100%',
        videoId: this.queue[0].id,
        playerVars: {
          autoplay: 0,
          controls: 1,
          playsinline: 1,
          rel: 0,
          modestbranding: 1,
          enablejsapi: 1,
          origin: window.location.origin
        },
        events: {
          onReady: () => {
            this.ytReady = true;
          },
          onStateChange: (event) => {
            if (event.data === YT.PlayerState.PLAYING) {
              this.onPlayStarted();
            } else if (event.data === YT.PlayerState.PAUSED) {
              this.onPlayPaused();
            } else if (event.data === YT.PlayerState.ENDED) {
              this.playNext();
            }
          }
        }
      });
    };
  }

  loadTrack(track, autoPlay = true) {
    this.updateTrackUI(track);

    if (this.ytPlayer && this.ytReady && this.ytPlayer.loadVideoById) {
      if (autoPlay) {
        this.ytPlayer.loadVideoById(track.id);
        this.onPlayStarted();
      } else {
        this.ytPlayer.cueVideoById(track.id);
      }
    }

    this.updateMediaSession(track);
  }

  togglePlay() {
    if (!this.ytReady || !this.ytPlayer) return;
    const state = this.ytPlayer.getPlayerState ? this.ytPlayer.getPlayerState() : -1;
    if (state === YT.PlayerState.PLAYING) {
      this.ytPlayer.pauseVideo();
    } else {
      this.ytPlayer.playVideo();
    }
  }

  onPlayStarted() {
    this.isPlaying = true;
    this.dockPlayBtn.innerHTML = `<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/></svg>`;
    this.discView.classList.add('playing');
    this.startProgressTracker();
  }

  onPlayPaused() {
    this.isPlaying = false;
    this.dockPlayBtn.innerHTML = `<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>`;
    this.discView.classList.remove('playing');
    this.stopProgressTracker();
  }

  playNext() {
    this.currentIndex = (this.currentIndex + 1) % this.queue.length;
    this.loadTrack(this.queue[this.currentIndex], true);
  }

  playPrev() {
    this.currentIndex = (this.currentIndex - 1 + this.queue.length) % this.queue.length;
    this.loadTrack(this.queue[this.currentIndex], true);
  }

  toggleVideoMode() {
    this.isVideoMode = !this.isVideoMode;
    this.dockVideoBtn.classList.toggle('active', this.isVideoMode);
    
    if (this.isVideoMode) {
      this.discView.classList.add('hidden');
      this.videoView.classList.add('active');
      this.openFullscreenPlayer();
    } else {
      this.discView.classList.remove('hidden');
      this.videoView.classList.remove('active');
    }
  }

  updateTrackUI(track) {
    this.dockThumb.src = track.thumb;
    this.dockTitle.textContent = track.title;
    this.dockArtist.textContent = track.artist;
    this.dockTotalTime.textContent = track.duration || '0:00';

    this.fullCover.src = track.thumb;
    this.fullTitle.textContent = track.title;
    this.fullArtist.textContent = track.artist;

    const isFav = this.favorites.some(f => f.id === track.id);
    this.dockFavBtn.classList.toggle('active', isFav);

    document.querySelectorAll('.track-card').forEach(card => {
      card.classList.toggle('playing', card.dataset.id === track.id);
    });
  }

  startProgressTracker() {
    this.stopProgressTracker();
    this.progressTimer = setInterval(() => {
      if (this.ytPlayer && this.ytPlayer.getCurrentTime && this.ytPlayer.getDuration) {
        const curr = this.ytPlayer.getCurrentTime() || 0;
        const dur = this.ytPlayer.getDuration() || 0;
        if (dur > 0) {
          const pct = (curr / dur) * 100;
          this.dockFill.style.width = pct + '%';
          this.dockCurrentTime.textContent = this.formatTime(curr);
          this.dockTotalTime.textContent = this.formatTime(dur);
        }
      }
    }, 500);
  }

  stopProgressTracker() {
    if (this.progressTimer) {
      clearInterval(this.progressTimer);
      this.progressTimer = null;
    }
  }

  formatTime(seconds) {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  }

  updateMediaSession(track) {
    if ('mediaSession' in navigator) {
      navigator.mediaSession.metadata = new MediaMetadata({
        title: track.title,
        artist: track.artist,
        album: 'MusicDrop',
        artwork: [
          { src: track.thumb, sizes: '512x512', type: 'image/jpeg' },
          { src: 'icons/icon-512.png', sizes: '512x512', type: 'image/png' }
        ]
      });

      navigator.mediaSession.setActionHandler('play', () => this.togglePlay());
      navigator.mediaSession.setActionHandler('pause', () => this.togglePlay());
      navigator.mediaSession.setActionHandler('nexttrack', () => this.playNext());
      navigator.mediaSession.setActionHandler('previoustrack', () => this.playPrev());
      navigator.mediaSession.setActionHandler('seekto', (details) => {
        if (this.ytPlayer && details.seekTime) {
          this.ytPlayer.seekTo(details.seekTime, true);
        }
      });
    }
  }

  toggleFavorite(track) {
    const idx = this.favorites.findIndex(f => f.id === track.id);
    if (idx >= 0) {
      this.favorites.splice(idx, 1);
      this.dockFavBtn.classList.remove('active');
    } else {
      this.favorites.push(track);
      this.dockFavBtn.classList.add('active');
    }
    localStorage.setItem('musicdrop_favs', JSON.stringify(this.favorites));
  }

  openFullscreenPlayer() {
    this.fullOverlay.classList.add('open');
  }

  closeFullscreenPlayer() {
    this.fullOverlay.classList.remove('open');
  }

  startVisualizer() {
    const vBars = document.querySelectorAll('.v-bar');
    setInterval(() => {
      if (this.isPlaying) {
        vBars.forEach(b => {
          const rand = Math.floor(Math.random() * 24) + 4;
          b.style.height = rand + 'px';
        });
      } else {
        vBars.forEach(b => b.style.height = '4px');
      }
    }, 120);
  }

  initPWA() {
    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.register('./sw.js').catch(err => {
        console.log('SW registration note:', err);
      });
    }

    const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream;
    const isStandalone = window.navigator.standalone || window.matchMedia('(display-mode: standalone)').matches;

    if (isIOS && !isStandalone) {
      if (!sessionStorage.getItem('musicdrop_ios_prompt_shown')) {
        setTimeout(() => {
          this.iosModal.classList.add('open');
          sessionStorage.setItem('musicdrop_ios_prompt_shown', 'true');
        }, 2000);
      }
    }

    window.addEventListener('beforeinstallprompt', (e) => {
      e.preventDefault();
      this.deferredPrompt = e;
      this.btnInstallHeader.style.display = 'inline-flex';
    });
  }

  showInstallPrompt() {
    const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream;
    if (isIOS) {
      this.iosModal.classList.add('open');
    } else if (this.deferredPrompt) {
      this.deferredPrompt.prompt();
      this.deferredPrompt.userChoice.then(() => {
        this.deferredPrompt = null;
      });
    } else {
      this.iosModal.classList.add('open');
    }
  }
}

// Start Engine
document.addEventListener('DOMContentLoaded', () => {
  window.musicDrop = new MusicDropEngine();
});
