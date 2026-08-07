// --- Global Error Diagnostic Handler ---
window.onerror = function(msg, url, lineNo, columnNo, error) {
  console.error("Global Error:", msg, "Line:", lineNo, "Col:", columnNo, error);
  try {
    const errDiv = document.createElement('div');
    errDiv.style.position = 'fixed';
    errDiv.style.top = '10px';
    errDiv.style.left = '50%';
    errDiv.style.transform = 'translateX(-50%)';
    errDiv.style.background = '#ff2d55';
    errDiv.style.color = '#ffffff';
    errDiv.style.padding = '12px 20px';
    errDiv.style.borderRadius = '10px';
    errDiv.style.zIndex = '999999';
    errDiv.style.fontSize = '12.5px';
    errDiv.style.fontWeight = 'bold';
    errDiv.style.boxShadow = '0 10px 30px rgba(0,0,0,0.8)';
    errDiv.textContent = `JS Error: ${msg} (Línea ${lineNo})`;
    document.body.appendChild(errDiv);
    setTimeout(() => errDiv.remove(), 10000);
  } catch(e) {}
  return false;
};

// --- WebAudio Direct PCM Engine ---
let globalAudioCtx = null;
let currentSourceNode = null;

async function playStreamViaWebAudio(streamUrl) {
  try {
    if (!globalAudioCtx) {
      globalAudioCtx = new (window.AudioContext || window.webkitAudioContext)();
    }
    if (globalAudioCtx.state === 'suspended') {
      await globalAudioCtx.resume();
    }

    logPlayback(`WebAudio: Fetching audio stream...`, "info");
    const response = await fetch(streamUrl);
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const arrayBuffer = await response.arrayBuffer();

    logPlayback(`WebAudio: Decoding PCM audio buffer (${arrayBuffer.byteLength} bytes)...`, "info");
    const audioBuffer = await globalAudioCtx.decodeAudioData(arrayBuffer);

    if (currentSourceNode) {
      try { currentSourceNode.stop(); } catch(e) {}
    }

    const source = globalAudioCtx.createBufferSource();
    source.buffer = audioBuffer;
    source.connect(globalAudioCtx.destination);
    source.start(0);
    currentSourceNode = source;
    logPlayback(`WebAudio: PCM audio streaming out loud to speakers!`, "success");
  } catch(e) {
    logPlayback(`WebAudio play Exception: ${e.message}`, "warn");
  }
}

// --- Auto-Resolve Playable Video ID Helper ---
async function resolvePlayableVideoId(title, artist, album) {
  try {
    const cleanTitle = (title || '').replace(/[\(\)\[\]]/g, '').trim();
    const cleanArtist = (artist || '').trim();
    const query = `${cleanTitle} ${cleanArtist} audio`.trim();
    logPlayback(`Auto-resolving playable video ID via search for: "${query}"...`, "info");
    
    const searchData = await callInnerTubeAPI('search', { query }, WEB_CONTEXT);
    let foundId = null;
    
    function findVideoIdDeep(obj) {
      if (!obj || typeof obj !== 'object' || foundId) return;
      if (obj.videoId && typeof obj.videoId === 'string' && /^[a-zA-Z0-9_-]{11}$/.test(obj.videoId)) {
        foundId = obj.videoId;
        return;
      }
      for (const k in obj) {
        if (Object.prototype.hasOwnProperty.call(obj, k)) {
          findVideoIdDeep(obj[k]);
        }
      }
    }

    findVideoIdDeep(searchData);

    if (foundId) {
      logPlayback(`Resolved playable videoId "${foundId}" for "${query}"`, "success");
      return foundId;
    }
  } catch(e) {
    logPlayback(`resolvePlayableVideoId exception: ${e.message}`, "error");
  }
  return null;
}

// --- Windows WASAPI Audio Session Unlocker ---
function unlockWindowsAudioSession() {
  try {
    if (!globalAudioCtx) {
      const AudioCtxClass = window.AudioContext || window.webkitAudioContext;
      if (AudioCtxClass) {
        globalAudioCtx = new AudioCtxClass();
      }
    }
    if (globalAudioCtx && globalAudioCtx.state === 'suspended') {
      globalAudioCtx.resume();
    }
  } catch (e) {
    console.warn("WASAPI unlock exception:", e);
  }
  const audioEl = document.getElementById('audio-player');
  if (audioEl) audioEl.muted = false;
}
document.addEventListener('click', unlockWindowsAudioSession);
document.addEventListener('keydown', unlockWindowsAudioSession);

// --- Playback Diagnostic Logging System ---
const playbackLogs = [];
function logPlayback(msg, type = 'info') {
  const time = new Date().toLocaleTimeString();
  playbackLogs.push({ time, msg, type });
  if (playbackLogs.length > 100) playbackLogs.shift();

  console.log(`[PlaybackLog ${time}]`, msg);

  const container = document.getElementById('debug-log-content');
  if (container) {
    const el = document.createElement('div');
    let color = '#d0d0d0';
    if (type === 'error') color = '#ff6b6b';
    if (type === 'success') color = '#51cf66';
    if (type === 'warn') color = '#fcc419';
    el.style.color = color;
    el.style.wordBreak = 'break-word';
    el.textContent = `[${time}] ${msg}`;
    container.appendChild(el);
    container.scrollTop = container.scrollHeight;
  }
}
window.logPlayback = logPlayback;

function initDiagnosticLogsUI() {
  const btnToggle = document.getElementById('btn-toggle-logs');
  const btnClose = document.getElementById('btn-close-logs');
  const btnClear = document.getElementById('btn-clear-logs');
  const overlay = document.getElementById('debug-log-overlay');
  const container = document.getElementById('debug-log-content');

  if (btnToggle && overlay) {
    btnToggle.addEventListener('click', () => {
      overlay.style.display = (overlay.style.display === 'none' || !overlay.style.display) ? 'flex' : 'none';
    });
  }
  if (btnClose && overlay) {
    btnClose.addEventListener('click', () => {
      overlay.style.display = 'none';
    });
  }
  if (btnClear && container) {
    btnClear.addEventListener('click', () => {
      container.innerHTML = '<div style="color: #888;">Logs limpiados.</div>';
    });
  }

  // Keyboard shortcut Ctrl+Shift+D to toggle diagnostic panel
  document.addEventListener('keydown', (e) => {
    if ((e.ctrlKey && e.shiftKey && e.key === 'D')) {
      if (overlay) overlay.style.display = (overlay.style.display === 'none' || !overlay.style.display) ? 'flex' : 'none';
    }
  });
}


// ==========================================
// --- RAYMUSIC LIBRARY & PLAYLIST STORAGE ---
// ==========================================
// ==========================================
// --- RAYMUSIC COMPLETE LIBRARY STORAGE ---
// ==========================================
const LibraryStorage = {
  // LIKED SONGS
  getLikedSongs: function() {
    try { return JSON.parse(localStorage.getItem('rm_liked_songs') || '[]'); } catch(e) { return []; }
  },
  isLiked: function(trackId) {
    if (!trackId) return false;
    const songs = this.getLikedSongs();
    return songs.some(s => String(s.id).replace('Video','') === String(trackId).replace('Video',''));
  },
  toggleLike: function(track) {
    if (!track || !track.id) return false;
    let songs = this.getLikedSongs();
    const cleanId = String(track.id).replace('Video','');
    const index = songs.findIndex(s => String(s.id).replace('Video','') === cleanId);
    let isNowLiked = false;
    
    if (index >= 0) {
      songs.splice(index, 1);
      isNowLiked = false;
      logPlayback(`Quitado de favoritos: "${track.title}"`, "info");
    } else {
      const item = {
        id: cleanId,
        title: track.title || "Canción",
        artist: track.artist || "Artista",
        artistId: track.artistId || "",
        artwork: track.artwork || "",
        durationSec: track.durationSec || 0,
        likedAt: Date.now()
      };
      songs.unshift(item);
      isNowLiked = true;
      logPlayback(`Guardado en Canciones Favoritas: "${track.title}"`, "success");
    }
    localStorage.setItem('rm_liked_songs', JSON.stringify(songs));
    updatePlayerHeartUI();
    return isNowLiked;
  },

  // PLAYLISTS
  getPlaylists: function() {
    try { return JSON.parse(localStorage.getItem('rm_playlists') || '[]'); } catch(e) { return []; }
  },
  getPlaylistById: function(playlistId) {
    return this.getPlaylists().find(p => p.id === playlistId) || null;
  },
  createPlaylist: function(title, description = '', artworkUrl = '') {
    const playlists = this.getPlaylists();
    const newPlaylist = {
      id: 'pl_' + Date.now() + '_' + Math.random().toString(36).substr(2, 4),
      title: title || 'Nueva Playlist',
      description: description,
      artworkUrl: artworkUrl || '',
      createdAt: Date.now(),
      tracks: []
    };
    playlists.unshift(newPlaylist);
    localStorage.setItem('rm_playlists', JSON.stringify(playlists));
    logPlayback(`Playlist creada: "${newPlaylist.title}"`, "success");
    updateSidebarCustomPlaylists();
    return newPlaylist;
  },
  deletePlaylist: function(playlistId) {
    let playlists = this.getPlaylists().filter(p => p.id !== playlistId);
    localStorage.setItem('rm_playlists', JSON.stringify(playlists));
    logPlayback(`Playlist eliminada: "${playlistId}"`, "info");
    updateSidebarCustomPlaylists();
  },
  addTrackToPlaylist: function(playlistId, track) {
    const playlists = this.getPlaylists();
    const pl = playlists.find(p => p.id === playlistId);
    if (!pl) return false;
    
    const cleanId = String(track.id).replace('Video','');
    if (pl.tracks.some(t => String(t.id).replace('Video','') === cleanId)) {
      logPlayback(`La canción ya está en la playlist "${pl.title}"`, "warn");
      return false;
    }
    
    const item = {
      id: cleanId,
      title: track.title || "Canción",
      artist: track.artist || "Artista",
      artwork: track.artwork || "",
      durationSec: track.durationSec || 0,
      addedAt: Date.now()
    };
    pl.tracks.push(item);
    if (!pl.artworkUrl && item.artwork) pl.artworkUrl = item.artwork;
    
    localStorage.setItem('rm_playlists', JSON.stringify(playlists));
    logPlayback(`Añadida "${track.title}" a "${pl.title}"`, "success");
    return true;
  },
  removeTrackFromPlaylist: function(playlistId, trackId) {
    const playlists = this.getPlaylists();
    const pl = playlists.find(p => p.id === playlistId);
    if (!pl) return;
    const cleanId = String(trackId).replace('Video','');
    pl.tracks = pl.tracks.filter(t => String(t.id).replace('Video','') !== cleanId);
    localStorage.setItem('rm_playlists', JSON.stringify(playlists));
    logPlayback(`Quitada canción de playlist "${pl.title}"`, "info");
  },

  // SAVED ALBUMS
  getSavedAlbums: function() {
    try { return JSON.parse(localStorage.getItem('rm_saved_albums') || '[]'); } catch(e) { return []; }
  },
  isAlbumSaved: function(albumId) {
    if (!albumId) return false;
    return this.getSavedAlbums().some(a => a.id === albumId);
  },
  toggleSaveAlbum: function(album) {
    if (!album || !album.title) return false;
    let albums = this.getSavedAlbums();
    const albumId = album.id || album.title;
    const index = albums.findIndex(a => a.id === albumId || a.title === album.title);
    let isSaved = false;

    if (index >= 0) {
      albums.splice(index, 1);
      isSaved = false;
      logPlayback(`Álbum quitado de biblioteca: "${album.title}"`, "info");
    } else {
      albums.unshift({
        id: albumId,
        title: album.title,
        artist: album.artist || "Artista",
        artwork: album.artwork || album.artworkUrl || "",
        tracksCount: album.tracks ? album.tracks.length : 0,
        savedAt: Date.now()
      });
      isSaved = true;
      logPlayback(`Álbum guardado en biblioteca: "${album.title}"`, "success");
    }
    localStorage.setItem('rm_saved_albums', JSON.stringify(albums));
    return isSaved;
  },

  // FOLLOWED ARTISTS
  getFollowedArtists: function() {
    try { return JSON.parse(localStorage.getItem('rm_followed_artists') || '[]'); } catch(e) { return []; }
  },
  isArtistFollowed: function(artistId) {
    if (!artistId) return false;
    return this.getFollowedArtists().some(a => a.id === artistId);
  },
  toggleFollowArtist: function(artist) {
    if (!artist || (!artist.id && !artist.name)) return false;
    let artists = this.getFollowedArtists();
    const artistId = artist.id || artist.name;
    const index = artists.findIndex(a => a.id === artistId || a.name === artist.name);
    let isFollowed = false;

    if (index >= 0) {
      artists.splice(index, 1);
      isFollowed = false;
      logPlayback(`Dejaste de seguir a: "${artist.name}"`, "info");
    } else {
      artists.unshift({
        id: artistId,
        name: artist.name || artist.title || "Artista",
        artwork: artist.artwork || artist.artworkUrl || "",
        followedAt: Date.now()
      });
      isFollowed = true;
      logPlayback(`Siguiendo a artista: "${artist.name}"`, "success");
    }
    localStorage.setItem('rm_followed_artists', JSON.stringify(artists));
    return isFollowed;
  },

  // PINNED ITEMS
  getPinnedItems: function() {
    try { return JSON.parse(localStorage.getItem('rm_pinned') || '[]'); } catch(e) { return []; }
  },
  isPinned: function(id) {
    return this.getPinnedItems().some(p => p.id === id);
  },
  togglePin: function(item) {
    let pins = this.getPinnedItems();
    const index = pins.findIndex(p => p.id === item.id || p.title === item.title);
    let isPinned = false;
    if (index >= 0) {
      pins.splice(index, 1);
      isPinned = false;
    } else {
      pins.unshift({
        id: item.id || item.title,
        type: item.type || 'playlist',
        title: item.title,
        artist: item.artist || '',
        artwork: item.artwork || item.artworkUrl || '',
        pinnedAt: Date.now()
      });
      isPinned = true;
    }
    localStorage.setItem('rm_pinned', JSON.stringify(pins));
    return isPinned;
  },

  // RECENTLY PLAYED
  getRecentlyPlayed: function() {
    try { return JSON.parse(localStorage.getItem('rm_recent') || '[]'); } catch(e) { return []; }
  },
  addRecentlyPlayed: function(track) {
    if (!track || !track.title) return;
    let recent = this.getRecentlyPlayed();
    const cleanId = String(track.id || '').replace('Video','');
    recent = recent.filter(r => String(r.id || '').replace('Video','') !== cleanId);
    recent.unshift({
      id: cleanId,
      title: track.title,
      artist: track.artist || '',
      artwork: track.artwork || '',
      playedAt: Date.now()
    });
    if (recent.length > 50) recent.pop();
    localStorage.setItem('rm_recent', JSON.stringify(recent));
  },
  clearRecentlyPlayed: function() {
    localStorage.setItem('rm_recent', '[]');
  }
};

function updatePlayerHeartUI() {
  const btnFav = document.getElementById('player-favorite');
  if (!btnFav) return;
  const currentTrack = currentQueue[activeIndex];
  if (currentTrack && currentTrack.id && LibraryStorage.isLiked(currentTrack.id)) {
    btnFav.classList.add('liked');
    btnFav.title = "Quitar de favoritos";
  } else {
    btnFav.classList.remove('liked');
    btnFav.title = "Marcar como favorito";
  }
}

function updateSidebarCustomPlaylists() {
  const container = document.getElementById('sidebar-custom-playlists');
  if (!container) return;
  const playlists = LibraryStorage.getPlaylists();
  
  if (playlists.length === 0) {
    container.innerHTML = '';
    return;
  }

  let html = '';
  playlists.forEach(pl => {
    html += `
      <a href="#" class="nav-item custom-pl-item" data-pl-id="${pl.id}" style="padding-left: 28px; font-size: 13px;">
        <svg class="nav-icon" viewBox="0 0 24 24" width="16" height="16">
          <path fill="currentColor" d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z" />
        </svg>
        <span style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${escapeHtmlAttr(pl.title)}</span>
      </a>
    `;
  });
  container.innerHTML = html;

  container.querySelectorAll('.custom-pl-item').forEach(el => {
    el.addEventListener('click', (e) => {
      e.preventDefault();
      const plId = el.getAttribute('data-pl-id');
      renderCustomPlaylistView(plId);
    });
  });
}

function showCreatePlaylistModal() {
  const existing = document.getElementById('modal-create-playlist');
  if (existing) existing.remove();

  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay';
  overlay.id = 'modal-create-playlist';
  overlay.innerHTML = `
    <div class="modal-card">
      <div class="modal-header">
        <h3 class="modal-title">Crear Nueva Playlist</h3>
        <button class="modal-close-btn" id="modal-close">✕</button>
      </div>
      <input type="text" id="pl-name-input" class="modal-input" placeholder="Nombre de la playlist" autofocus />
      <input type="text" id="pl-desc-input" class="modal-input" placeholder="Descripción (opcional)" />
      <div class="modal-actions">
        <button class="btn-secondary" id="btn-cancel-pl">Cancelar</button>
        <button class="btn-primary" id="btn-confirm-pl">Crear</button>
      </div>
    </div>
  `;
  document.body.appendChild(overlay);

  const close = () => overlay.remove();
  document.getElementById('modal-close').onclick = close;
  document.getElementById('btn-cancel-pl').onclick = close;
  
  const create = () => {
    const name = document.getElementById('pl-name-input').value.trim();
    const desc = document.getElementById('pl-desc-input').value.trim();
    if (!name) return;
    const pl = LibraryStorage.createPlaylist(name, desc);
    close();
    renderCustomPlaylistView(pl.id);
  };

  document.getElementById('btn-confirm-pl').onclick = create;
  document.getElementById('pl-name-input').onkeydown = (e) => {
    if (e.key === 'Enter') create();
  };
}

function showAddToPlaylistModal(track) {
  if (!track || !track.id) return;
  const playlists = LibraryStorage.getPlaylists();

  const existing = document.getElementById('modal-add-to-playlist');
  if (existing) existing.remove();

  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay';
  overlay.id = 'modal-add-to-playlist';

  let listHtml = '';
  if (playlists.length === 0) {
    listHtml = '<p style="color: #aaa; font-size: 13px;">No tienes playlists creadas. ¡Crea una primero!</p>';
  } else {
    playlists.forEach(pl => {
      listHtml += `
        <div class="playlist-select-item" data-pl-id="${pl.id}">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor" style="color: var(--accent-color);">
            <path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/>
          </svg>
          <div>
            <div style="font-weight: 600; font-size: 14px;">${escapeHtmlAttr(pl.title)}</div>
            <div style="font-size: 12px; color: var(--text-secondary);">${pl.tracks.length} canciones</div>
          </div>
        </div>
      `;
    });
  }

  overlay.innerHTML = `
    <div class="modal-card">
      <div class="modal-header">
        <h3 class="modal-title">Añadir a Playlist</h3>
        <button class="modal-close-btn" id="modal-add-close">✕</button>
      </div>
      <div style="font-size: 13px; color: var(--text-secondary); margin-bottom: 4px;">
        Canción: <strong style="color: #fff;">${escapeHtmlAttr(track.title)}</strong>
      </div>
      <div style="display: flex; flex-direction: column; gap: 8px; max-height: 260px; overflow-y: auto;">
        ${listHtml}
      </div>
      <div class="modal-actions" style="margin-top: 12px;">
        <button class="btn-secondary" id="btn-new-pl-from-add">+ Nueva Playlist</button>
      </div>
    </div>
  `;
  document.body.appendChild(overlay);

  const close = () => overlay.remove();
  document.getElementById('modal-add-close').onclick = close;

  overlay.querySelectorAll('.playlist-select-item').forEach(item => {
    item.addEventListener('click', () => {
      const plId = item.getAttribute('data-pl-id');
      LibraryStorage.addTrackToPlaylist(plId, track);
      close();
    });
  });

  document.getElementById('btn-new-pl-from-add').onclick = () => {
    close();
    showCreatePlaylistModal();
  };
}

function showTrackOptionsMenu(track, event) {
  if (event) event.stopPropagation();
  if (!track || (!track.id && !track.title)) return;

  const existing = document.getElementById('modal-track-options');
  if (existing) existing.remove();

  const isFav = LibraryStorage.isLiked ? LibraryStorage.isLiked(track.id) : false;

  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay';
  overlay.id = 'modal-track-options';
  overlay.style.zIndex = '99999';

  overlay.innerHTML = `
    <div class="modal-card" style="max-width: 380px; padding: 22px 24px; border-radius: 24px; background: rgba(24, 24, 28, 0.96); backdrop-filter: blur(24px); border: 1px solid rgba(255,255,255,0.15); box-shadow: 0 20px 60px rgba(0,0,0,0.8); animation: fadeIn 0.2s ease-out;">
      
      <!-- Track Info Header -->
      <div style="display: flex; align-items: center; gap: 14px; padding-bottom: 16px; margin-bottom: 12px; border-bottom: 1px solid rgba(255,255,255,0.08);">
        <img src="${upgradeThumbQuality(track.artwork)}" style="width: 52px; height: 52px; border-radius: 12px; object-fit: cover; box-shadow: 0 6px 16px rgba(0,0,0,0.4); flex-shrink: 0;">
        <div style="display: flex; flex-direction: column; overflow: hidden; white-space: nowrap;">
          <span style="font-size: 15px; font-weight: 800; color: white; text-overflow: ellipsis; overflow: hidden; line-height: 1.2;">${escapeHtmlAttr(track.title || 'Canción')}</span>
          <span style="font-size: 12.5px; color: rgba(255,255,255,0.65); text-overflow: ellipsis; overflow: hidden; margin-top: 3px;">${escapeHtmlAttr(track.artist || 'Artista')}</span>
        </div>
        <button id="modal-track-opts-close" style="background: rgba(255,255,255,0.1); border: none; color: white; width: 28px; height: 28px; border-radius: 50%; cursor: pointer; font-size: 14px; display: flex; align-items: center; justify-content: center; margin-left: auto; flex-shrink: 0;" title="Cerrar">✕</button>
      </div>

      <!-- Options List -->
      <div style="display: flex; flex-direction: column; gap: 4px;">
        
        <div class="track-opt-item" id="opt-start-radio" style="display: flex; align-items: center; gap: 14px; padding: 10px 14px; border-radius: 12px; cursor: pointer; transition: background 0.15s ease;">
          <svg viewBox="0 0 24 24" width="20" height="20"><path fill="currentColor" d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>
          <span style="font-size: 14px; font-weight: 600; color: white;">Iniciar radio</span>
        </div>

        <div class="track-opt-item" id="opt-add-playlist" style="display: flex; align-items: center; gap: 14px; padding: 10px 14px; border-radius: 12px; cursor: pointer; transition: background 0.15s ease;">
          <svg viewBox="0 0 24 24" width="20" height="20"><path fill="currentColor" d="M14 10H3v2h11v-2zm0-4H3v2h11V6zm4 8v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zM3 16h7v-2H3v2z"/></svg>
          <span style="font-size: 14px; font-weight: 600; color: white;">Añadir a playlist</span>
        </div>

        <div class="track-opt-item" id="opt-play-next" style="display: flex; align-items: center; gap: 14px; padding: 10px 14px; border-radius: 12px; cursor: pointer; transition: background 0.15s ease;">
          <svg viewBox="0 0 24 24" width="20" height="20"><path fill="currentColor" d="M6 18l8.5-6L6 6v12zM16 6v12h2V6z"/></svg>
          <span style="font-size: 14px; font-weight: 600; color: white;">Reproducir a continuación</span>
        </div>

        <div class="track-opt-item" id="opt-add-queue" style="display: flex; align-items: center; gap: 14px; padding: 10px 14px; border-radius: 12px; cursor: pointer; transition: background 0.15s ease;">
          <svg viewBox="0 0 24 24" width="20" height="20"><path fill="currentColor" d="M4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm16-4H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H8V4h12v12z"/></svg>
          <span style="font-size: 14px; font-weight: 600; color: white;">Añadir a la cola</span>
        </div>

        <div class="track-opt-item" id="opt-toggle-fav" style="display: flex; align-items: center; gap: 14px; padding: 10px 14px; border-radius: 12px; cursor: pointer; transition: background 0.15s ease;">
          <svg viewBox="0 0 24 24" width="20" height="20"><path fill="${isFav ? '#ff2d55' : 'currentColor'}" d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
          <span style="font-size: 14px; font-weight: 600; color: ${isFav ? '#ff2d55' : 'white'};">${isFav ? 'Quitar de favoritos' : 'Añadir a favoritos'}</span>
        </div>

        ${track.artistId ? `
        <div class="track-opt-item" id="opt-go-artist" style="display: flex; align-items: center; gap: 14px; padding: 10px 14px; border-radius: 12px; cursor: pointer; transition: background 0.15s ease;">
          <svg viewBox="0 0 24 24" width="20" height="20"><path fill="currentColor" d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
          <span style="font-size: 14px; font-weight: 600; color: white;">Ir al artista</span>
        </div>
        ` : ''}

        <div class="track-opt-item" id="opt-share" style="display: flex; align-items: center; gap: 14px; padding: 10px 14px; border-radius: 12px; cursor: pointer; transition: background 0.15s ease;">
          <svg viewBox="0 0 24 24" width="20" height="20"><path fill="currentColor" d="M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92 1.61 0 2.92-1.31 2.92-2.92s-1.31-2.92-2.92-2.92z"/></svg>
          <span style="font-size: 14px; font-weight: 600; color: white;">Compartir canción</span>
        </div>

      </div>
    </div>
  `;

  document.body.appendChild(overlay);

  const close = () => overlay.remove();
  document.getElementById('modal-track-opts-close').onclick = close;
  overlay.addEventListener('click', (e) => {
    if (e.target === overlay) close();
  });

  // Attach hover styles to option items
  overlay.querySelectorAll('.track-opt-item').forEach(item => {
    item.addEventListener('mouseenter', () => item.style.background = 'rgba(255,255,255,0.1)');
    item.addEventListener('mouseleave', () => item.style.background = 'transparent');
  });

  // Actions
  document.getElementById('opt-start-radio').onclick = () => {
    close();
    playTrackDetails(track.id, track.title, track.artist, track.artwork, track.artistId, track.durationSec);
  };

  document.getElementById('opt-add-playlist').onclick = () => {
    close();
    showAddToPlaylistModal(track);
  };

  document.getElementById('opt-play-next').onclick = () => {
    close();
    currentQueue.splice(activeIndex + 1, 0, track);
    renderQueue();
    renderExpandedQueue();
  };

  document.getElementById('opt-add-queue').onclick = () => {
    close();
    currentQueue.push(track);
    renderQueue();
    renderExpandedQueue();
  };

  document.getElementById('opt-toggle-fav').onclick = () => {
    close();
    if (LibraryStorage.toggleLike) {
      LibraryStorage.toggleLike(track);
    }
  };

  const btnGoArtist = document.getElementById('opt-go-artist');
  if (btnGoArtist && track.artistId) {
    btnGoArtist.onclick = () => {
      close();
      loadArtistPage(track.artistId, track.artist);
    };
  }

  document.getElementById('opt-share').onclick = () => {
    close();
    const url = `https://music.youtube.com/watch?v=${track.id}`;
    if (navigator.clipboard) {
      navigator.clipboard.writeText(url).then(() => {
        logPlayback(`Enlace copiado al portapapeles: ${url}`, "success");
      });
    }
  };
}

// --- RENDERERS FOR LIBRARY & PLAYLIST VIEWS ---


function renderSavedAlbumsView() {
  setHeaderVisible(true);
  document.getElementById('page-title').textContent = "Álbumes Guardados";
  const albums = LibraryStorage.getSavedAlbums();

  if (albums.length === 0) {
    contentArea.innerHTML = `
      <div style="padding: 60px 20px; text-align: center;">
        <div style="font-size: 48px; margin-bottom: 16px;">💿</div>
        <h2 style="font-size: 22px; font-weight: 700; margin-bottom: 8px;">Sin Álbumes Guardados</h2>
        <p style="color: var(--text-secondary); max-width: 400px; margin: 0 auto;">Presiona "+ Agregar" en cualquier página de álbum para guardarlo en tu biblioteca.</p>
      </div>
    `;
    return;
  }

  let cardsHtml = '';
  albums.forEach(album => {
    cardsHtml += `
      <div class="playlist-card" data-album-id="${album.id}">
        <button class="playlist-delete-btn" data-del-album="${album.id}" title="Quitar de biblioteca">✕</button>
        <img src="${album.artwork || 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300'}" class="playlist-card-art" />
        <div class="playlist-card-title">${escapeHtmlAttr(album.title)}</div>
        <div class="playlist-card-sub">${escapeHtmlAttr(album.artist)}</div>
      </div>
    `;
  });

  contentArea.innerHTML = `
    <div style="padding: 24px;">
      <h2 style="font-size: 22px; font-weight: 700; margin-bottom: 20px;">Álbumes Guardados (${albums.length})</h2>
      <div class="library-grid">
        ${cardsHtml}
      </div>
    </div>
  `;

  contentArea.querySelectorAll('.playlist-card[data-album-id]').forEach(card => {
    const albId = card.getAttribute('data-album-id');
    card.onclick = (e) => {
      if (e.target.closest('.playlist-delete-btn')) return;
      loadPlaylistContents(albId, card.querySelector('.playlist-card-title').textContent);
    };

    const delBtn = card.querySelector('.playlist-delete-btn');
    if (delBtn) {
      delBtn.onclick = (e) => {
        e.stopPropagation();
        const alb = albums.find(a => a.id === albId);
        if (alb) {
          LibraryStorage.toggleSaveAlbum(alb);
          renderSavedAlbumsView();
        }
      };
    }
  });
}

function renderFollowedArtistsView() {
  setHeaderVisible(true);
  document.getElementById('page-title').textContent = "Artistas Seguidos";
  const artists = LibraryStorage.getFollowedArtists();

  if (artists.length === 0) {
    contentArea.innerHTML = `
      <div style="padding: 60px 20px; text-align: center;">
        <div style="font-size: 48px; margin-bottom: 16px;">🎤</div>
        <h2 style="font-size: 22px; font-weight: 700; margin-bottom: 8px;">Sin Artistas Seguidos</h2>
        <p style="color: var(--text-secondary); max-width: 400px; margin: 0 auto;">Presiona "Seguir" o "⭐" en la página de cualquier artista para añadirlo a tu biblioteca.</p>
      </div>
    `;
    return;
  }

  let cardsHtml = '';
  artists.forEach(artist => {
    cardsHtml += `
      <div class="playlist-card" data-artist-id="${artist.id}" style="text-align: center; align-items: center;">
        <button class="playlist-delete-btn" data-del-artist="${artist.id}" title="Dejar de seguir">✕</button>
        <img src="${artist.artwork || 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300'}" class="playlist-card-art" style="border-radius: 50%; width: 140px; height: 140px;" />
        <div class="playlist-card-title">${escapeHtmlAttr(artist.name)}</div>
        <div class="playlist-card-sub">Artista</div>
      </div>
    `;
  });

  contentArea.innerHTML = `
    <div style="padding: 24px;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
        <h2 style="font-size: 22px; font-weight: 700;">Artistas Seguidos (${artists.length})</h2>
      </div>
      <div class="library-grid">
        ${cardsHtml}
      </div>
    </div>
  `;

  contentArea.querySelectorAll('.playlist-card[data-artist-id]').forEach(card => {
    const artId = card.getAttribute('data-artist-id');
    const artName = card.querySelector('.playlist-card-title').textContent;
    card.onclick = (e) => {
      if (e.target.closest('.playlist-delete-btn')) return;
      loadArtistPage(artId, artName);
    };

    const delBtn = card.querySelector('.playlist-delete-btn');
    if (delBtn) {
      delBtn.onclick = (e) => {
        e.stopPropagation();
        const art = artists.find(a => a.id === artId);
        if (art) {
          LibraryStorage.toggleFollowArtist(art);
          renderFollowedArtistsView();
        }
      };
    }
  });
}

function renderPinnedItemsView() {
  setHeaderVisible(true);
  document.getElementById('page-title').textContent = "Pins";
  const pins = LibraryStorage.getPinnedItems();

  if (pins.length === 0) {
    contentArea.innerHTML = `
      <div style="padding: 60px 20px; text-align: center;">
        <div style="font-size: 48px; margin-bottom: 16px;">📌</div>
        <h2 style="font-size: 22px; font-weight: 700; margin-bottom: 8px;">Sin Elementos Fijados</h2>
        <p style="color: var(--text-secondary); max-width: 400px; margin: 0 auto;">Fija tus álbumes, listas o artistas preferidos desde el menú de 3 puntos (•••).</p>
      </div>
    `;
    return;
  }

  let cardsHtml = '';
  pins.forEach(pin => {
    cardsHtml += `
      <div class="playlist-card" data-pin-id="${pin.id}">
        <button class="playlist-delete-btn" data-del-pin="${pin.id}" title="Desfijar">✕</button>
        <img src="${pin.artwork || 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300'}" class="playlist-card-art" />
        <div class="playlist-card-title">${escapeHtmlAttr(pin.title)}</div>
        <div class="playlist-card-sub">${escapeHtmlAttr(pin.artist || pin.type || '')}</div>
      </div>
    `;
  });

  contentArea.innerHTML = `
    <div style="padding: 24px;">
      <h2 style="font-size: 22px; font-weight: 700; margin-bottom: 20px;">Elementos Fijados (${pins.length})</h2>
      <div class="library-grid">
        ${cardsHtml}
      </div>
    </div>
  `;

  contentArea.querySelectorAll('.playlist-card[data-pin-id]').forEach(card => {
    const pinId = card.getAttribute('data-pin-id');
    card.onclick = (e) => {
      if (e.target.closest('.playlist-delete-btn')) return;
      loadPlaylistContents(pinId, card.querySelector('.playlist-card-title').textContent);
    };

    const delBtn = card.querySelector('.playlist-delete-btn');
    if (delBtn) {
      delBtn.onclick = (e) => {
        e.stopPropagation();
        const pin = pins.find(p => p.id === pinId);
        if (pin) {
          LibraryStorage.togglePin(pin);
          renderPinnedItemsView();
        }
      };
    }
  });
}


function renderLikedSongsView() {
  setHeaderVisible(true);
  document.getElementById('page-title').textContent = "Canciones Favoritas";
  const liked = LibraryStorage.getLikedSongs();

  if (liked.length === 0) {
    contentArea.innerHTML = `
      <div style="padding: 60px 20px; text-align: center;">
        <div style="font-size: 48px; margin-bottom: 16px;">❤️</div>
        <h2 style="font-size: 22px; font-weight: 700; margin-bottom: 8px;">Tus Canciones Favoritas</h2>
        <p style="color: var(--text-secondary); max-width: 400px; margin: 0 auto 20px auto;">Aún no has guardado canciones. Presiona el corazón en cualquier canción para guardarla aquí.</p>
      </div>
    `;
    return;
  }

  let rowsHtml = '';
  liked.forEach((track, i) => {
    rowsHtml += `
      <div class="song-row" data-index="${i}">
        <span class="song-index">${i + 1}</span>
        <img class="song-thumbnail" src="${track.artwork || ''}" alt="art" />
        <div class="song-info">
          <span class="song-title">${escapeHtmlAttr(track.title)}</span>
          <span class="song-artist">${escapeHtmlAttr(track.artist)}</span>
        </div>
        <div style="display: flex; gap: 8px; align-items: center; margin-left: auto;">
          <button class="btn-row-add-pl" title="Añadir a playlist" style="background:none; border:none; color:var(--text-secondary); cursor:pointer; padding:6px;">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
          </button>
          <button class="btn-row-unlike" title="Quitar de favoritos" style="background:none; border:none; color:#ff2d55; cursor:pointer; padding:6px;">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
          </button>
          <span class="song-duration" style="font-size: 12px; color: var(--text-secondary); min-width: 40px; text-align: right;">${track.durationSec ? formatTime(track.durationSec) : '--:--'}</span>
        </div>
      </div>
    `;
  });

  contentArea.innerHTML = `
    <div style="padding: 24px;">
      <div style="display: flex; align-items: center; gap: 24px; margin-bottom: 30px;">
        <div style="width: 140px; height: 140px; border-radius: 16px; background: linear-gradient(135deg, #ff2d55, #ff3b30); display: flex; align-items: center; justify-content: center; box-shadow: 0 10px 30px rgba(255,45,85,0.4);">
          <svg viewBox="0 0 24 24" width="64" height="64" fill="#fff"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
        </div>
        <div>
          <span style="font-size: 12px; text-transform: uppercase; font-weight: 700; letter-spacing: 1px; color: var(--accent-color);">Playlist Autogenerada</span>
          <h1 style="font-size: 32px; font-weight: 800; margin: 4px 0 8px 0;">Canciones Favoritas</h1>
          <p style="color: var(--text-secondary); font-size: 14px;">${liked.length} canciones guardadas</p>
          <div style="margin-top: 16px; display: flex; gap: 12px;">
            <button class="btn-primary" id="btn-play-all-liked" style="display: flex; align-items: center; gap: 8px; padding: 12px 24px; font-size: 14px;">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M8 5v14l11-7z"/></svg> Reproducir Todo
            </button>
          </div>
        </div>
      </div>

      <div class="songs-list-container">
        ${rowsHtml}
      </div>
    </div>
  `;

  contentArea.querySelectorAll('.song-row').forEach((row, idx) => {
    row.addEventListener('click', (e) => {
      if (e.target.closest('.btn-row-unlike') || e.target.closest('.btn-row-add-pl')) return;
      currentQueue = liked;
      activeIndex = idx;
      playTrack();
    });

    const btnUnlike = row.querySelector('.btn-row-unlike');
    if (btnUnlike) {
      btnUnlike.onclick = (e) => {
        e.stopPropagation();
        LibraryStorage.toggleLike(liked[idx]);
        renderLikedSongsView();
      };
    }

    const btnAddPl = row.querySelector('.btn-row-add-pl');
    if (btnAddPl) {
      btnAddPl.onclick = (e) => {
        e.stopPropagation();
        showAddToPlaylistModal(liked[idx]);
      };
    }
  });

  const btnPlayAll = document.getElementById('btn-play-all-liked');
  if (btnPlayAll) {
    btnPlayAll.onclick = () => {
      currentQueue = liked;
      activeIndex = 0;
      playTrack();
    };
  }
}

function renderAllPlaylistsView() {
  setHeaderVisible(true);
  document.getElementById('page-title').textContent = "Playlists";
  const userPlaylists = LibraryStorage.getPlaylists();
  const likedSongs = LibraryStorage.getLikedSongs();

  let cardsHtml = `
    <div class="playlist-card" id="card-liked-songs" style="background: linear-gradient(135deg, rgba(255,45,85,0.2), rgba(255,59,48,0.1)); border-color: rgba(255,45,85,0.3);">
      <div class="playlist-card-art" style="background: linear-gradient(135deg, #ff2d55, #ff3b30);">
        <svg viewBox="0 0 24 24" width="40" height="40" fill="#fff"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
      </div>
      <div class="playlist-card-title">Canciones Favoritas</div>
      <div class="playlist-card-sub">${likedSongs.length} canciones</div>
    </div>
  `;

  userPlaylists.forEach(pl => {
    const art = pl.artworkUrl ? `<img src="${pl.artworkUrl}" class="playlist-card-art" />` : `
      <div class="playlist-card-art">
        <svg viewBox="0 0 24 24" width="36" height="36" fill="rgba(255,255,255,0.4)"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>
      </div>
    `;

    cardsHtml += `
      <div class="playlist-card" data-pl-id="${pl.id}">
        <button class="playlist-delete-btn" data-del-id="${pl.id}" title="Eliminar playlist">✕</button>
        ${art}
        <div class="playlist-card-title">${escapeHtmlAttr(pl.title)}</div>
        <div class="playlist-card-sub">${pl.tracks.length} canciones</div>
      </div>
    `;
  });

  contentArea.innerHTML = `
    <div style="padding: 24px;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
        <h2 style="font-size: 22px; font-weight: 700;">Todas tus Playlists</h2>
        <button class="btn-primary" id="btn-create-pl-header">+ Nueva Playlist</button>
      </div>
      <div class="library-grid">
        ${cardsHtml}
      </div>
    </div>
  `;

  document.getElementById('btn-create-pl-header').onclick = showCreatePlaylistModal;

  document.getElementById('card-liked-songs').onclick = renderLikedSongsView;

  contentArea.querySelectorAll('.playlist-card[data-pl-id]').forEach(card => {
    card.addEventListener('click', (e) => {
      if (e.target.closest('.playlist-delete-btn')) return;
      const plId = card.getAttribute('data-pl-id');
      renderCustomPlaylistView(plId);
    });

    const delBtn = card.querySelector('.playlist-delete-btn');
    if (delBtn) {
      delBtn.onclick = (e) => {
        e.stopPropagation();
        const delId = delBtn.getAttribute('data-del-id');
        if (confirm("¿Seguro que deseas eliminar esta playlist?")) {
          LibraryStorage.deletePlaylist(delId);
          renderAllPlaylistsView();
        }
      };
    }
  });
}

function renderCustomPlaylistView(playlistId) {
  setHeaderVisible(true);
  const pl = LibraryStorage.getPlaylistById(playlistId);
  if (!pl) {
    contentArea.innerHTML = '<p class="error-msg" style="padding: 40px;">Playlist no encontrada.</p>';
    return;
  }

  document.getElementById('page-title').textContent = pl.title;

  let rowsHtml = '';
  if (pl.tracks.length === 0) {
    rowsHtml = `
      <div style="padding: 40px; text-align: center; color: var(--text-secondary);">
        Esta playlist está vacía. Añade canciones usando el botón (+) en cualquier canción.
      </div>
    `;
  } else {
    pl.tracks.forEach((track, i) => {
      rowsHtml += `
        <div class="song-row" data-index="${i}">
          <span class="song-index">${i + 1}</span>
          <img class="song-thumbnail" src="${track.artwork || ''}" alt="art" />
          <div class="song-info">
            <span class="song-title">${escapeHtmlAttr(track.title)}</span>
            <span class="song-artist">${escapeHtmlAttr(track.artist)}</span>
          </div>
          <div style="display: flex; gap: 8px; align-items: center; margin-left: auto;">
            <button class="btn-row-del-track" title="Quitar de playlist" style="background:none; border:none; color:var(--text-secondary); cursor:pointer; padding:6px;">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M19 13H5v-2h14v2z"/></svg>
            </button>
            <span class="song-duration" style="font-size: 12px; color: var(--text-secondary); min-width: 40px; text-align: right;">${track.durationSec ? formatTime(track.durationSec) : '--:--'}</span>
          </div>
        </div>
      `;
    });
  }

  contentArea.innerHTML = `
    <div style="padding: 24px;">
      <div style="display: flex; align-items: center; gap: 24px; margin-bottom: 30px;">
        <div style="width: 140px; height: 140px; border-radius: 16px; background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.1); display: flex; align-items: center; justify-content: center; box-shadow: 0 10px 30px rgba(0,0,0,0.5);">
          ${pl.artworkUrl ? `<img src="${pl.artworkUrl}" style="width:100%;height:100%;object-fit:cover;border-radius:16px;" />` : `<svg viewBox="0 0 24 24" width="48" height="48" fill="rgba(255,255,255,0.3)"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>`}
        </div>
        <div>
          <span style="font-size: 12px; text-transform: uppercase; font-weight: 700; letter-spacing: 1px; color: var(--accent-color);">Playlist Personal</span>
          <h1 style="font-size: 32px; font-weight: 800; margin: 4px 0 8px 0;">${escapeHtmlAttr(pl.title)}</h1>
          <p style="color: var(--text-secondary); font-size: 14px;">${pl.description || ''} • ${pl.tracks.length} canciones</p>
          <div style="margin-top: 16px; display: flex; gap: 12px;">
            ${pl.tracks.length > 0 ? `
              <button class="btn-primary" id="btn-play-custom-pl" style="display: flex; align-items: center; gap: 8px; padding: 12px 24px; font-size: 14px;">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M8 5v14l11-7z"/></svg> Reproducir
              </button>
            ` : ''}
            <button class="btn-secondary" id="btn-del-custom-pl">Eliminar Playlist</button>
          </div>
        </div>
      </div>

      <div class="songs-list-container">
        ${rowsHtml}
      </div>
    </div>
  `;

  if (pl.tracks.length > 0) {
    document.getElementById('btn-play-custom-pl').onclick = () => {
      currentQueue = pl.tracks;
      activeIndex = 0;
      playTrack();
    };

    contentArea.querySelectorAll('.song-row').forEach((row, idx) => {
      row.addEventListener('click', (e) => {
        if (e.target.closest('.btn-row-del-track')) return;
        currentQueue = pl.tracks;
        activeIndex = idx;
        playTrack();
      });

      const delBtn = row.querySelector('.btn-row-del-track');
      if (delBtn) {
        delBtn.onclick = (e) => {
          e.stopPropagation();
          LibraryStorage.removeTrackFromPlaylist(pl.id, pl.tracks[idx].id);
          renderCustomPlaylistView(pl.id);
        };
      }
    });
  }

  document.getElementById('btn-del-custom-pl').onclick = () => {
    if (confirm(`¿Seguro que deseas eliminar la playlist "${pl.title}"?`)) {
      LibraryStorage.deletePlaylist(pl.id);
      renderAllPlaylistsView();
    }
  };
}

function renderRecentlyPlayedView() {
  setHeaderVisible(true);
  document.getElementById('page-title').textContent = "Reciente";
  const recent = LibraryStorage.getRecentlyPlayed();

  if (recent.length === 0) {
    contentArea.innerHTML = `
      <div style="padding: 60px 20px; text-align: center;">
        <h2 style="font-size: 20px; font-weight: 700; margin-bottom: 8px;">Sin Historial Reciente</h2>
        <p style="color: var(--text-secondary);">Las canciones que reproduzcas aparecerán automáticamente aquí.</p>
      </div>
    `;
    return;
  }

  let rowsHtml = '';
  recent.forEach((track, i) => {
    const timeStr = track.playedAt ? new Date(track.playedAt).toLocaleString() : '';
    rowsHtml += `
      <div class="song-row" data-index="${i}">
        <span class="song-index">${i + 1}</span>
        <img class="song-thumbnail" src="${track.artwork || ''}" alt="art" />
        <div class="song-info">
          <span class="song-title">${escapeHtmlAttr(track.title)}</span>
          <span class="song-artist">${escapeHtmlAttr(track.artist)}</span>
        </div>
        <span style="font-size: 11px; color: var(--text-muted); margin-left: auto;">${timeStr}</span>
      </div>
    `;
  });

  contentArea.innerHTML = `
    <div style="padding: 24px;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
        <h2 style="font-size: 22px; font-weight: 700;">Reproducciones Recientes</h2>
        <button class="btn-secondary" id="btn-clear-recent">Limpiar Historial</button>
      </div>
      <div class="songs-list-container">
        ${rowsHtml}
      </div>
    </div>
  `;

  contentArea.querySelectorAll('.song-row').forEach((row, idx) => {
    row.addEventListener('click', () => {
      currentQueue = recent;
      activeIndex = idx;
      playTrack();
    });
  });

  document.getElementById('btn-clear-recent').onclick = () => {
    LibraryStorage.clearRecentlyPlayed();
    renderRecentlyPlayedView();
  };
}



// ==========================================
// --- GLOBAL CONTEXT MENU & ACTIONS ENGINE ---
// ==========================================
let activeContextMenuTarget = null;

function showContextMenu(e, item, itemType = 'song') {
  e.preventDefault();
  e.stopPropagation();
  activeContextMenuTarget = item;

  const menu = document.getElementById('app-context-menu');
  const itemsContainer = document.getElementById('context-menu-items');
  if (!menu || !itemsContainer) return;

  const x = Math.min(e.clientX || e.pageX, window.innerWidth - 220);
  const y = Math.min(e.clientY || e.pageY, window.innerHeight - 300);

  menu.style.left = `${x}px`;
  menu.style.top = `${y}px`;

  const isLiked = item.id ? LibraryStorage.isLiked(item.id) : false;

  let optionsHtml = '';
  if (itemType === 'song') {
    optionsHtml = `
      <div class="ctx-item" id="ctx-play-next">Reproducir a continuación</div>
      <div class="ctx-item" id="ctx-add-queue">Añadir a la cola</div>
      <div class="ctx-item" id="ctx-add-playlist">Añadir a playlist</div>
      <div class="ctx-item" id="ctx-toggle-like">${isLiked ? 'Quitar de favoritos' : 'Guardar en favoritos'}</div>
      ${item.artistId ? `<div class="ctx-item" id="ctx-go-artist">Ir al artista</div>` : ''}
      <div class="ctx-item" id="ctx-share">Copiar enlace</div>
    `;
  } else if (itemType === 'album' || itemType === 'playlist') {
    optionsHtml = `
      <div class="ctx-item" id="ctx-play-all">Reproducir todo</div>
      <div class="ctx-item" id="ctx-add-queue">Añadir a la cola</div>
      <div class="ctx-item" id="ctx-save-lib">Guardar en biblioteca</div>
      <div class="ctx-item" id="ctx-share">Copiar enlace</div>
    `;
  } else if (itemType === 'artist') {
    optionsHtml = `
      <div class="ctx-item" id="ctx-play-all">Reproducir temas populares</div>
      <div class="ctx-item" id="ctx-follow">Seguir artista</div>
      <div class="ctx-item" id="ctx-share">Copiar enlace</div>
    `;
  }

  itemsContainer.innerHTML = optionsHtml;
  menu.classList.remove('hidden');

  // Event handlers for context options
  const btnPlayNext = document.getElementById('ctx-play-next');
  if (btnPlayNext) {
    btnPlayNext.onclick = () => {
      hideContextMenu();
      currentQueue.splice(activeIndex + 1, 0, item);
      renderQueue();
      logPlayback(`Added "${item.title}" to play next`, "info");
    };
  }

  const btnAddQueue = document.getElementById('ctx-add-queue');
  if (btnAddQueue) {
    btnAddQueue.onclick = () => {
      hideContextMenu();
      currentQueue.push(item);
      renderQueue();
      logPlayback(`Added "${item.title}" to queue`, "info");
    };
  }

  const btnAddPlaylist = document.getElementById('ctx-add-playlist');
  if (btnAddPlaylist) {
    btnAddPlaylist.onclick = () => {
      hideContextMenu();
      showAddToPlaylistModal(item);
    };
  }

  const btnToggleLike = document.getElementById('ctx-toggle-like');
  if (btnToggleLike) {
    btnToggleLike.onclick = () => {
      hideContextMenu();
      LibraryStorage.toggleLike(item);
    };
  }

  const btnGoArtist = document.getElementById('ctx-go-artist');
  if (btnGoArtist) {
    btnGoArtist.onclick = () => {
      hideContextMenu();
      loadArtistPage(item.artistId, item.artist);
    };
  }

  const btnPlayAll = document.getElementById('ctx-play-all');
  if (btnPlayAll) {
    btnPlayAll.onclick = () => {
      hideContextMenu();
      if (item.tracks && item.tracks.length > 0) {
        currentQueue = item.tracks;
        activeIndex = 0;
        playTrack();
      }
    };
  }

  const btnSaveLib = document.getElementById('ctx-save-lib');
  if (btnSaveLib) {
    btnSaveLib.onclick = () => {
      hideContextMenu();
      LibraryStorage.togglePin(item);
    };
  }

  const btnShare = document.getElementById('ctx-share');
  if (btnShare) {
    btnShare.onclick = () => {
      hideContextMenu();
      const url = `https://music.youtube.com/watch?v=${item.id}`;
      navigator.clipboard.writeText(url);
      logPlayback(`Copied link to clipboard: ${url}`, "success");
    };
  }
}

function hideContextMenu() {
  const menu = document.getElementById('app-context-menu');
  if (menu) menu.classList.add('hidden');
}

document.addEventListener('click', (e) => {
  if (!e.target.closest('#app-context-menu')) hideContextMenu();
});

// ==========================================
// --- LRCLIB REAL LYRICS FETCH ENGINE ---
// ==========================================
let currentParsedLyrics = [];

function parseLrcContent(lrcText) {
  if (!lrcText) return [];
  const lines = lrcText.split('\n');
  const parsed = [];
  const timeRegex = /\[(\d{2}):(\d{2})[\.:](\d{2,3})\]/;

  lines.forEach(line => {
    const match = line.match(timeRegex);
    if (match) {
      const min = parseInt(match[1], 10);
      const sec = parseInt(match[2], 10);
      const ms = parseInt(match[3], 10);
      const timeSec = min * 60 + sec + (ms > 99 ? ms / 1000 : ms / 100);
      const text = line.replace(timeRegex, '').trim();
      if (text) parsed.push({ time: timeSec, text });
    } else if (line.trim()) {
      parsed.push({ time: -1, text: line.trim() });
    }
  });

  return parsed;
}

function syncLyricsPosition(currentTimeSec) {
  if (!currentParsedLyrics || currentParsedLyrics.length === 0) return;

  let activeIdx = -1;
  for (let i = 0; i < currentParsedLyrics.length; i++) {
    if (currentParsedLyrics[i].time <= currentTimeSec && currentParsedLyrics[i].time !== -1) {
      activeIdx = i;
    } else if (currentParsedLyrics[i].time > currentTimeSec) {
      break;
    }
  }

  if (activeIdx >= 0) {
    document.querySelectorAll('.lyric-line').forEach(el => {
      const idx = parseInt(el.getAttribute('data-line-idx'), 10);
      if (idx === activeIdx) {
        el.style.color = '#ffffff';
        el.style.fontSize = '20px';
        el.style.fontWeight = '900';
        el.style.textShadow = '0 0 15px rgba(255,45,85,0.8)';
        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      } else {
        el.style.color = 'rgba(255,255,255,0.4)';
        el.style.fontSize = '16px';
        el.style.fontWeight = '700';
        el.style.textShadow = 'none';
      }
    });
  }
}


/* index.js - Playback Engine, Navigation, and Live YouTube Music API client */

// --- Global App State ---
let currentQueue = [];
let activeIndex = -1;
let currentPlaybackContext = null; // Stores collection context info (album, playlist, single/EP, featured on)
let isShuffle = false;
let isRepeat = false;
let searchTimeout = null;

// Playback tracking (driven by audio element events)
let playbackInterval = null; // kept for compat, no longer used for ticking
let currentPlaybackTime = 0;
let currentPlaybackDuration = 180;
let isPlaying = false;

// --- Global Navigation History Stack ---
const historyStack = [];
let historyIndex = -1;
let isNavigatingHistory = false;

function pushNavigation(action) {
  if (isNavigatingHistory) return;
  
  if (historyIndex < historyStack.length - 1) {
    historyStack.splice(historyIndex + 1);
  }
  
  const lastAction = historyStack[historyStack.length - 1];
  if (lastAction && lastAction.name === action.name && JSON.stringify(lastAction.params) === JSON.stringify(action.params)) {
    return;
  }
  
  historyStack.push(action);
  historyIndex = historyStack.length - 1;
  updateNavArrowUI();
}

function updateNavArrowUI() {
  const btnPrev = document.getElementById('nav-prev');
  const btnNext = document.getElementById('nav-next');
  if (btnPrev) btnPrev.disabled = (historyIndex <= 0);
  if (btnNext) btnNext.disabled = (historyIndex >= historyStack.length - 1);
}

function goBack() {
  if (historyIndex > 0) {
    historyIndex--;
    executeNavigation(historyStack[historyIndex]);
  }
}

function goForward() {
  if (historyIndex < historyStack.length - 1) {
    historyIndex++;
    executeNavigation(historyStack[historyIndex]);
  }
}

function executeNavigation(action) {
  isNavigatingHistory = true;
  try {
    switch (action.name) {
      case 'tab':
        loadTab(action.params.tabName, false);
        break;
      case 'search':
        if (searchInput) searchInput.value = action.params.query;
        performSearch(action.params.query, false);
        break;
      case 'artist':
        loadArtistPage(action.params.artistId, action.params.artistName, false);
        break;
      case 'playlist':
      case 'album':
        loadPlaylistContents(action.params.playlistId, action.params.playlistTitle, false);
        break;
    }
  } finally {
    isNavigatingHistory = false;
    updateNavArrowUI();
  }
}

// --- Recently Played Storage ---
let recentlyPlayed = [];
try {
  recentlyPlayed = JSON.parse(localStorage.getItem('raymusic_recently_played') || '[]');
} catch(e) {}

function addToRecentlyPlayed(track) {
  if (!track || !track.id) return;
  recentlyPlayed = recentlyPlayed.filter(t => t.id !== track.id);
  recentlyPlayed.unshift({
    id: track.id,
    title: track.title,
    artist: track.artist,
    artistId: track.artistId,
    artwork: track.artwork,
    type: 'song'
  });
  if (recentlyPlayed.length > 30) recentlyPlayed = recentlyPlayed.slice(0, 30);
  try {
    localStorage.setItem('raymusic_recently_played', JSON.stringify(recentlyPlayed));
  } catch(e) {}
}

// --- DOM Elements ---
const audio = document.getElementById('audio-player');
const playBtn = document.getElementById('player-play');
const playIcon = document.getElementById('play-icon');
const pauseIcon = document.getElementById('pause-icon');
const prevBtn = document.getElementById('player-prev');
const nextBtn = document.getElementById('player-next');
const shuffleBtn = document.getElementById('player-shuffle');
const repeatBtn = document.getElementById('player-repeat');
const favoriteBtn = document.getElementById('player-favorite');

// Timings & Progress slider
const timeElapsedLabel = document.getElementById('time-elapsed');
const timeRemainingLabel = document.getElementById('time-remaining');
const timelineSlider = document.getElementById('timeline-slider');
const timelineProgress = document.getElementById('timeline-progress');
const timelineHandle = document.getElementById('timeline-handle');

// Song info overlays
const songTitleMini = document.getElementById('song-title-mini');
const songArtistMini = document.getElementById('song-artist-mini');
const songArtworkMini = document.getElementById('song-artwork-mini');
const nowPlayingTitle = document.getElementById('now-playing-title');
const nowPlayingArtist = document.getElementById('now-playing-artist');
const nowPlayingLargeArtwork = document.getElementById('now-playing-large-artwork');

// Sidebar toggle & Panels
const btnToggleSidebar = document.getElementById('btn-toggle-sidebar');
const rightPanel = document.getElementById('right-panel');
const queueListContainer = document.getElementById('queue-list') || document.getElementById('queue-list-container');
const queueItemCountLabel = document.getElementById('queue-item-count');

// Search & Suggestions & Refresh
const searchInput = document.getElementById('search-input');
const searchSuggestions = document.getElementById('search-suggestions');
const btnRefresh = document.getElementById('btn-refresh');

// Volume slider
const volumeIcon = document.getElementById('volume-icon');
const volumeSlider = document.getElementById('volume-slider');
const volumeProgress = document.getElementById('volume-progress');
const volumeHandle = document.getElementById('volume-handle');

// Content Areas
const contentArea = document.getElementById('content-area');

// --- Standard Fallback Songs if API Offline ---
const FALLBACK_TRACKS = [
  {
    id: "J09Tz8v3oCc",
    title: "These Days",
    artist: "Stray Fossa",
    artistId: "UCe6vS0sBfS0A3c8V2P-g-2w",
    album: "Laridae - EP",
    artwork: "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=360&h=360&fit=crop&q=80",
    streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
  },
  {
    id: "tokyoawayVideo",
    title: "tokyo away",
    artist: "Night Tapes",
    artistId: "UCe6vS0sBfS0A3c8V2P-g-3w",
    album: "portals//polarities",
    artwork: "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=360&h=360&fit=crop&q=80",
    streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
  },
  {
    id: "AlgernonVideo",
    title: "Algernon",
    artist: "A Beacon School",
    artistId: "UCe6vS0sBfS0A3c8V2P-g-4w",
    album: "Cola",
    artwork: "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=360&h=360&fit=crop&q=80",
    streamUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"
  }
];

// --- Initialization ---
document.addEventListener('DOMContentLoaded', () => {
  if (typeof initNativeBinds === 'function') initNativeBinds();
  if (typeof initPlayerEvents === 'function') initPlayerEvents();
  if (typeof initSidebarNav === 'function') initSidebarNav();
  if (typeof initSearchEvents === 'function') initSearchEvents();
  if (typeof initAudioEvents === 'function') initAudioEvents();
  if (typeof initDiagnosticLogsUI === 'function') initDiagnosticLogsUI();
  
  // Load Home Feed initially
  loadTab('Home');
});

function initSearchEvents() {
  const input = document.getElementById('search-input');
  const suggestionsBox = document.getElementById('search-suggestions');
  if (!input) return;

  let debounceTimer = null;
  input.addEventListener('input', (e) => {
    const val = e.target.value.trim();
    clearTimeout(debounceTimer);
    if (!val) {
      if (suggestionsBox) suggestionsBox.classList.add('hidden');
      return;
    }
    debounceTimer = setTimeout(async () => {
      try {
        const data = await callInnerTubeAPI('music/get_search_suggestions', { input: val }, WEB_CONTEXT, 2000);
        const contents = data.contents?.[0]?.searchSuggestionsSectionRenderer?.contents || [];
        if (contents.length > 0 && suggestionsBox) {
          suggestionsBox.innerHTML = '';
          contents.forEach(item => {
            const run = item.searchSuggestionRenderer?.suggestion?.runs?.[0]?.text || item.searchSuggestionRenderer?.suggestion?.runs?.map(r => r.text).join('') || '';
            if (run) {
              const div = document.createElement('div');
              div.className = 'suggestion-item';
              div.style.padding = '8px 14px';
              div.style.cursor = 'pointer';
              div.style.fontSize = '13px';
              div.style.color = '#fff';
              div.textContent = run;
              div.addEventListener('click', () => {
                input.value = run;
                suggestionsBox.classList.add('hidden');
                performSearch(run);
              });
              suggestionsBox.appendChild(div);
            }
          });
          suggestionsBox.classList.remove('hidden');
        } else if (suggestionsBox) {
          suggestionsBox.classList.add('hidden');
        }
      } catch(err) {
        if (suggestionsBox) suggestionsBox.classList.add('hidden');
      }
    }, 250);
  });

  input.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      const val = input.value.trim();
      if (suggestionsBox) suggestionsBox.classList.add('hidden');
      if (val) performSearch(val);
    }
  });

  document.addEventListener('click', (e) => {
    if (suggestionsBox && !e.target.closest('.search-box')) {
      suggestionsBox.classList.add('hidden');
    }
  });
}
window.performSearch = performSearch;

function initSidebarNav() {
  const navItems = document.querySelectorAll('.nav-item');
  navItems.forEach(item => {
    item.addEventListener('click', (e) => {
      e.preventDefault();
      navItems.forEach(el => el.classList.remove('active'));
      item.classList.add('active');
      const textSpan = item.querySelector('span');
      if (textSpan) {
        const tabText = textSpan.textContent.trim();
        loadTab(tabText);
      }
    });
  });

  // Top header logo click
  const brandHeader = document.querySelector('.brand-header');
  if (brandHeader) {
    brandHeader.style.cursor = 'pointer';
    brandHeader.addEventListener('click', () => {
      loadTab('Home');
    });
  }

  // Top header back/forward navigation buttons
  const btnNavPrev = document.getElementById('nav-prev');
  if (btnNavPrev) {
    btnNavPrev.addEventListener('click', () => {
      if (typeof goBack === 'function') goBack();
    });
  }
  const btnNavNext = document.getElementById('nav-next');
  if (btnNavNext) {
    btnNavNext.addEventListener('click', () => {
      if (typeof goForward === 'function') goForward();
    });
  }

  // Refresh button
  const btnRefresh = document.getElementById('btn-refresh');
  if (btnRefresh) {
    btnRefresh.addEventListener('click', () => {
      const activeTab = document.querySelector('.nav-item.active span')?.textContent || 'Home';
      loadTab(activeTab);
    });
  }
}

// --- C++ Native Bridge Integration ---
function initNativeBinds() {
  const postToNative = (msg) => {
    if (window.chrome && window.chrome.webview) {
      window.chrome.webview.postMessage(msg);
    } else {
      console.log("[Native Dev Out]:", msg);
    }
  };

  const btnClose = document.getElementById('btn-close');
  if (btnClose) btnClose.addEventListener('click', () => postToNative({ action: 'close' }));

  const btnMinimize = document.getElementById('btn-minimize');
  if (btnMinimize) btnMinimize.addEventListener('click', () => postToNative({ action: 'minimize' }));

  const btnMaximize = document.getElementById('btn-maximize');
  if (btnMaximize) btnMaximize.addEventListener('click', () => postToNative({ action: 'maximize' }));

  const titlebar = document.getElementById('custom-titlebar');
  if (titlebar) {
    titlebar.addEventListener('mousedown', (e) => {
      if (!e.target.closest('.window-controls-win') && !e.target.closest('.window-right-actions') && !e.target.closest('.window-left-brand')) {
        postToNative({ action: 'drag' });
      }
    });

    titlebar.addEventListener('dblclick', (e) => {
      if (!e.target.closest('.window-controls-win') && !e.target.closest('.window-right-actions')) {
        postToNative({ action: 'maximize' });
      }
    });
  }
}

function initPlayerEvents() {
  if (playBtn) {
    playBtn.addEventListener('click', () => {
      if (isPlaying) {
        if (ytPlayer && ytPlayer.pauseVideo) ytPlayer.pauseVideo();
        if (audio) audio.pause();
      } else {
        if (ytPlayer && ytPlayer.playVideo) ytPlayer.playVideo();
        if (audio) audio.play().catch(() => {});
      }
    });
  }
}



// --- YTM InnerTube API Integration ---
const YTM_API_URL = "https://music.youtube.com/youtubei/v1";
const API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX3";

const WEB_CONTEXT = {
  context: {
    client: {
      clientName: "WEB_REMIX",
      clientVersion: "1.20240101.01.00",
      gl: "US",
      hl: "en"
    },
    user: {}
  }
};

const ANDROID_VR_CONTEXT = {
  context: {
    client: {
      clientName: "ANDROID_VR",
      clientVersion: "1.43.32",
      gl: "US",
      hl: "en"
    },
    user: {}
  }
};

async function callInnerTubeAPI(endpoint, bodyData, clientContext = WEB_CONTEXT, timeoutMs = 12000) {
  const url = `${YTM_API_URL}/${endpoint}?key=${API_KEY}`;
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(url, {
      method: 'POST',
      signal: controller.signal,
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        ...clientContext,
        ...bodyData
      })
    });
    clearTimeout(timeoutId);
    if (!response.ok) throw new Error(`API Error: ${response.statusText}`);
    return await response.json();
  } catch (e) {
    clearTimeout(timeoutId);
    console.warn(`InnerTube fetch failed on endpoint [${endpoint}]:`, e.message || e);
    throw e;
  }
}

function onClickArtist(artistId, artistName) {
  if (artistId && artistId.startsWith("UC")) {
    loadArtistPage(artistId, artistName);
  } else if (artistName) {
    performSearch(artistName);
  }
}
window.onClickArtist = onClickArtist;


function escapeHtmlAttr(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/'/g, '&#39;')
    .replace(/"/g, '&quot;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}
window.escapeHtmlAttr = escapeHtmlAttr;

function safeLoadArtistPage(e, artistId, artistName) {
  if (e && e.stopPropagation) e.stopPropagation();
  loadArtistPage(artistId, artistName);
}
window.safeLoadArtistPage = safeLoadArtistPage;

function extractArtistInfo(runs, fallbackTitle = "Artista") {
  if (!runs || !Array.isArray(runs) || runs.length === 0) {
    return { artistText: fallbackTitle, artistId: null };
  }

  let foundArtistRun = null;
  for (const run of runs) {
    const browseId = run.navigationEndpoint?.browseEndpoint?.browseId;
    if (browseId && (browseId.startsWith("UC") || browseId.startsWith("FEmusic") || run.navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType === "MUSIC_PAGE_TYPE_ARTIST")) {
      foundArtistRun = run;
      break;
    }
  }

  if (!foundArtistRun) {
    const generic = ["song", "canción", "cancion", "single", "álbum", "album", "ep", "video", "vídeo", "música", "musica", "•", "-", "|"];
    for (const run of runs) {
      const txt = (run.text || "").trim();
      const lower = txt.toLowerCase();
      if (txt && !generic.includes(lower) && !/^\d+:\d+(:\d+)?$/.test(txt)) {
        foundArtistRun = run;
        break;
      }
    }
  }

  if (foundArtistRun) {
    const artistText = foundArtistRun.text.trim();
    const artistId = foundArtistRun.navigationEndpoint?.browseEndpoint?.browseId || null;
    return { artistText, artistId };
  }

  const firstTextRun = runs.find(r => r.text && r.text.trim() !== "•" && r.text.trim() !== "-");
  return {
    artistText: firstTextRun ? firstTextRun.text.trim() : fallbackTitle,
    artistId: firstTextRun?.navigationEndpoint?.browseEndpoint?.browseId || null
  };
}
window.extractArtistInfo = extractArtistInfo;

function upgradeThumbQuality(url) {
  if (!url) return "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800&h=800";
  let u = String(url);
  if (u.includes("ytimg.com")) {
    const base = u.split('?')[0];
    u = base.replace("/default.jpg", "/hqdefault.jpg")
            .replace("/mqdefault.jpg", "/hqdefault.jpg")
            .replace("/sddefault.jpg", "/hqdefault.jpg");
  }
  if (u.includes("googleusercontent.com") || u.includes("ggpht.com")) {
    u = u.replace(/=[ws]\d+[^&]*/g, "=w1200-h1200-p-l90-rj");
    if (!u.includes("=w") && !u.includes("=s")) {
      u += "=w1200-h1200-p-l90-rj";
    }
  }
  return u;
}

function extractThumbnail(item) {
  if (!item) return "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&h=500";
  
  function pickBest(thumbs) {
    if (!thumbs || thumbs.length === 0) return null;
    return thumbs[thumbs.length - 1].url;
  }
  
  let url = pickBest(item.thumbnail?.thumbnails)
         || pickBest(item.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails)
         || pickBest(item.thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails)
         || pickBest(item.musicThumbnailRenderer?.thumbnail?.thumbnails)
         || pickBest(item.musicDetailHeaderRenderer?.thumbnail?.croppedSquareThumbnailRenderer?.thumbnail?.thumbnails)
         || pickBest(item.musicDetailHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails)
         || pickBest(item.croppedSquareThumbnailRenderer?.thumbnail?.thumbnails)
         || pickBest(item.header?.musicDetailHeaderRenderer?.thumbnail?.croppedSquareThumbnailRenderer?.thumbnail?.thumbnails)
         || pickBest(item.header?.musicDetailHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails)
         || pickBest(item.thumbnails)
         || pickBest(item.avatar?.thumbnails)
         || pickBest(item.image?.thumbnails);
         
  if (url) return upgradeThumbQuality(url);

  const vId = item.videoId || item.navigationEndpoint?.watchEndpoint?.videoId || item.id;
  if (vId && typeof vId === 'string' && !vId.startsWith("UC") && !vId.startsWith("MPREb") && !vId.startsWith("VL") && !vId.startsWith("PL")) {
    return `https://i.ytimg.com/vi/${vId}/hqdefault.jpg`;
  }
  return "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&h=500";
}

function parseDurationToSeconds(durationStr) {
  if (!durationStr || typeof durationStr !== 'string') return 0;
  const match = durationStr.match(/\d+:\d+(:\d+)?/);
  if (!match) return 0;
  const parts = match[0].split(':').map(Number);
  if (parts.length === 2) {
    return parts[0] * 60 + parts[1];
  } else if (parts.length === 3) {
    return parts[0] * 3600 + parts[1] * 60 + parts[2];
  }
  return 0;
}

// --- Native Audio Engine via YouTube IFrame API ---
let ytPlayer = null;
let isYtReady = false;
let currentVolume = 0.8;

window.onYouTubeIframeAPIReady = function() {
  ytPlayer = new YT.Player('yt-player', {
    height: '150',
    width: '200',
    playerVars: {
      'autoplay': 1,
      'controls': 0,
      'disablekb': 1,
      'fs': 0,
      'rel': 0,
      'playsinline': 1,
      'origin': window.location.origin || 'http://localhost'
    },
    events: {
      'onReady': () => {
        isYtReady = true;
        if (ytPlayer) {
          if (ytPlayer.unMute) ytPlayer.unMute();
          if (ytPlayer.setVolume) ytPlayer.setVolume((currentVolume || 0.8) * 100);
        }
      },
      'onStateChange': (event) => {
        if (event.data === YT.PlayerState.PLAYING) {
          isPlaying = true;
          if (playIcon) playIcon.classList.add('hidden');
          if (pauseIcon) pauseIcon.classList.remove('hidden');
          const dur = ytPlayer.getDuration();
          if (dur && dur > 0) {
            currentPlaybackDuration = Math.floor(dur);
            const track = currentQueue[activeIndex];
            if (track) track.durationSec = currentPlaybackDuration;
            updateTimelineUI();
          }
          startRealTimePlayback();
          if (window.updateExpandedPlayerView) window.updateExpandedPlayerView();
        } else if (event.data === YT.PlayerState.PAUSED) {
          isPlaying = false;
          playIcon.classList.remove('hidden');
          pauseIcon.classList.add('hidden');
          if (window.updateExpandedPlayerView) window.updateExpandedPlayerView();
        } else if (event.data === YT.PlayerState.ENDED) {
          nextTrack();
        }
      },
      'onError': (err) => {
        console.warn('YouTube IFrame Player error:', err);
      }
    }
  });
};

function playNativeVlc(videoId, startTime = 0) {
  if (isYtReady && ytPlayer && ytPlayer.loadVideoById) {
    ytPlayer.loadVideoById({
      videoId: videoId,
      startSeconds: startTime || 0
    });
  }
}

function stopNativeVlc() {
  if (isYtReady && ytPlayer && ytPlayer.stopVideo) {
    try { ytPlayer.stopVideo(); } catch(e) {}
  }
}

// Upgrade thumbnail resolution to 1024x1024 or maximum uncompressed HD quality

function escapeHtmlAttr(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/'/g, '&#39;')
    .replace(/"/g, '&quot;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}
window.escapeHtmlAttr = escapeHtmlAttr;

function safeLoadArtistPage(e, artistId, artistName) {
  if (e && e.stopPropagation) e.stopPropagation();
  loadArtistPage(artistId, artistName);
}
window.safeLoadArtistPage = safeLoadArtistPage;

function extractArtistInfo(runs, fallbackTitle = "Artista") {
  if (!runs || !Array.isArray(runs) || runs.length === 0) {
    return { artistText: fallbackTitle, artistId: null };
  }

  let foundArtistRun = null;
  for (const run of runs) {
    const browseId = run.navigationEndpoint?.browseEndpoint?.browseId;
    if (browseId && (browseId.startsWith("UC") || browseId.startsWith("FEmusic") || run.navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType === "MUSIC_PAGE_TYPE_ARTIST")) {
      foundArtistRun = run;
      break;
    }
  }

  if (!foundArtistRun) {
    const generic = ["song", "canción", "cancion", "single", "álbum", "album", "ep", "video", "vídeo", "música", "musica", "•", "-", "|"];
    for (const run of runs) {
      const txt = (run.text || "").trim();
      const lower = txt.toLowerCase();
      if (txt && !generic.includes(lower) && !/^\d+:\d+(:\d+)?$/.test(txt)) {
        foundArtistRun = run;
        break;
      }
    }
  }

  if (foundArtistRun) {
    const artistText = foundArtistRun.text.trim();
    const artistId = foundArtistRun.navigationEndpoint?.browseEndpoint?.browseId || null;
    return { artistText, artistId };
  }

  const firstTextRun = runs.find(r => r.text && r.text.trim() !== "•" && r.text.trim() !== "-");
  return {
    artistText: firstTextRun ? firstTextRun.text.trim() : fallbackTitle,
    artistId: firstTextRun?.navigationEndpoint?.browseEndpoint?.browseId || null
  };
}
window.extractArtistInfo = extractArtistInfo;

function upgradeThumbQuality(url) {
  if (!url) return url;
  if (url.includes("lh3.googleusercontent.com") || url.includes("yt3.ggpht.com") || url.includes("yt3.googleusercontent.com")) {
    return url.replace(/=w\d+-h\d+.*$/, "=w1024-h1024-l90-rj")
              .replace(/=s\d+.*$/, "=s1024-c");
  }
  if (url.includes("mzstatic.com")) {
    return url.replace(/\/\d+x\d+bb\.(webp|jpg)$/, "/1000x1000bb.jpg");
  }
  if (url.includes("ytimg.com/vi/")) {
    return url.replace("hqdefault.jpg", "maxresdefault.jpg")
              .replace("mqdefault.jpg", "maxresdefault.jpg")
              .replace("sddefault.jpg", "maxresdefault.jpg");
  }
  if (url.includes("=w") || url.includes("=s")) {
    return url.replace(/=w\d+(-h\d+)?/, "=w1024-h1024")
              .replace(/=s\d+/, "=s1024");
  }
  return url;
}

// Extract unthrottled streaming URL via multi-client fallback pool
async function fetchStreamUrl(videoId) {
  const clients = [
    {
      clientName: "IOS",
      clientVersion: "21.03.1",
      deviceMake: "Apple",
      deviceModel: "iPhone16,2",
      osName: "iOS",
      osVersion: "18.2.22C152",
      gl: "US",
      hl: "en"
    },
    {
      clientName: "ANDROID",
      clientVersion: "19.05.36",
      osName: "Android",
      osVersion: "14",
      deviceMake: "Google",
      deviceModel: "Pixel 8",
      androidSdkVersion: "34",
      gl: "US",
      hl: "en"
    },
    {
      clientName: "ANDROID_VR",
      clientVersion: "1.61.48",
      osName: "Android",
      osVersion: "12",
      deviceMake: "Oculus",
      deviceModel: "Quest 3",
      androidSdkVersion: "32",
      gl: "US",
      hl: "en"
    },
    {
      clientName: "WEB_REMIX",
      clientVersion: "1.20241028.01.00",
      gl: "US",
      hl: "en"
    }
  ];

  for (const client of clients) {
    try {
      const payload = {
        videoId: videoId,
        contentCheckOk: true,
        racyCheckOk: true,
        context: {
          client: {
            clientName: client.clientName,
            clientVersion: client.clientVersion,
            gl: client.gl,
            hl: client.hl
          },
          user: {}
        }
      };

      if (client.osName) {
        payload.context.client.osName = client.osName;
        payload.context.client.osVersion = client.osVersion;
        payload.context.client.deviceMake = client.deviceMake;
        payload.context.client.deviceModel = client.deviceModel;
        payload.context.client.androidSdkVersion = client.androidSdkVersion;
      }

      const response = await fetch(`${YTM_API_URL}/player?key=${API_KEY}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
      });
      if (!response.ok) continue;

      const data = await response.json();
      const formats = [
        ...(data.streamingData?.adaptiveFormats || []),
        ...(data.streamingData?.formats || [])
      ];

      if (formats.length > 0) {
        const audioStreams = formats.filter(f => f.mimeType && f.mimeType.startsWith('audio/'));
        if (audioStreams.length > 0) {
          // Prioritize audio/mp4 (AAC) which HTML5 <audio> handles natively without WebM container bugs
          audioStreams.sort((a, b) => {
            const aMp4 = a.mimeType.includes('mp4') ? 1 : 0;
            const bMp4 = b.mimeType.includes('mp4') ? 1 : 0;
            if (aMp4 !== bMp4) return bMp4 - aMp4;
            return (b.bitrate || 0) - (a.bitrate || 0);
          });
          
          let durationSec = 0;
          if (data.videoDetails && data.videoDetails.lengthSeconds) {
            durationSec = parseInt(data.videoDetails.lengthSeconds, 10);
          }
          
          for (const stream of audioStreams) {
            if (stream.url) {
              if (!durationSec && stream.approxDurationMs) {
                durationSec = Math.round(parseInt(stream.approxDurationMs, 10) / 1000);
              }
              console.log(`Successfully resolved stream for ${videoId} using client ${client.clientName} (${stream.mimeType}), duration: ${durationSec}s`);
              return { url: stream.url, durationSec: durationSec || 0 };
            }
          }
        }
      }
    } catch (e) {
      console.warn(`Failed to fetch stream using client ${client.clientName}:`, e);
    }
  }
  return null;
}

// Fetch Watch Next items to populate recommendations queue
async function fetchWatchNext(videoId) {
  try {
    const cleanId = String(videoId).replace('Video', '').trim();
    if (!cleanId || cleanId.length < 5) return [];

    logPlayback(`Fetching related songs for videoId "${cleanId}"...`, "info");
    const data = await callInnerTubeAPI('next', { videoId: cleanId, playlistId: 'RDAMVM' + cleanId }, WEB_CONTEXT);
    const list = [];

    // Try multiple known response paths
    let contents = null;

    // Path 1: Standard YTM watch next
    contents = data?.contents?.singleColumnMusicWatchNextResultsRenderer?.tabbedRenderer?.watchNextTabbedResultsRenderer?.tabs?.[0]?.tabRenderer?.content?.musicQueueRenderer?.content?.playlistPanelRenderer?.contents;

    // Path 2: Alternative structure
    if (!contents) {
      contents = data?.contents?.twoColumnWatchNextResults?.autoplay?.autoplay?.sets?.[0]?.autoplayVideo;
    }

    // Path 3: Direct playlist panel
    if (!contents) {
      const tabs = data?.contents?.singleColumnMusicWatchNextResultsRenderer?.tabbedRenderer?.watchNextTabbedResultsRenderer?.tabs;
      if (tabs) {
        for (const tab of tabs) {
          const possibleContents = tab?.tabRenderer?.content?.musicQueueRenderer?.content?.playlistPanelRenderer?.contents;
          if (possibleContents && possibleContents.length > 0) {
            contents = possibleContents;
            break;
          }
        }
      }
    }

    // Path 4: Recursive deep scan for playlistPanelVideoRenderer items
    if (!contents || contents.length === 0) {
      logPlayback(`Standard paths failed, doing deep scan of response...`, "info");
      const foundVideos = [];
      function deepScanForVideos(obj, depth = 0) {
        if (!obj || depth > 12) return;
        if (typeof obj !== 'object') return;
        
        if (obj.playlistPanelVideoRenderer) {
          foundVideos.push({ playlistPanelVideoRenderer: obj.playlistPanelVideoRenderer });
          return;
        }
        if (obj.playlistPanelVideoWrapperRenderer) {
          foundVideos.push({ playlistPanelVideoWrapperRenderer: obj.playlistPanelVideoWrapperRenderer });
          return;
        }
        
        if (Array.isArray(obj)) {
          for (const item of obj) deepScanForVideos(item, depth + 1);
        } else {
          for (const key of Object.keys(obj)) {
            deepScanForVideos(obj[key], depth + 1);
          }
        }
      }
      deepScanForVideos(data);
      if (foundVideos.length > 0) {
        contents = foundVideos;
        logPlayback(`Deep scan found ${foundVideos.length} related tracks`, "success");
      }
    }
    
    if (contents && contents.length > 0) {
      logPlayback(`Found ${contents.length} items in watch next response`, "info");
      contents.forEach(itemContainer => {
        let video = itemContainer.playlistPanelVideoRenderer;
        if (!video && itemContainer.playlistPanelVideoWrapperRenderer) {
          video = itemContainer.playlistPanelVideoWrapperRenderer.primaryItem?.playlistPanelVideoRenderer;
        }
        if (!video) return;
        
        const id = video.videoId;
        if (!id) return;
        
        const title = video.title?.runs?.[0]?.text || "Canción";
        const artist = video.longBylineText?.runs?.[0]?.text || video.shortBylineText?.runs?.[0]?.text || "Artista";
        const artistId = video.longBylineText?.runs?.[0]?.navigationEndpoint?.browseEndpoint?.browseId;
        const album = video.longBylineText?.runs?.[2]?.text || "YouTube Music";
        let thumb = video.thumbnail?.thumbnails?.[0]?.url || "";
        
        if (thumb && thumb.includes("ytimg.com")) {
          thumb = thumb.replace("w120-h120", "w360-h360").replace("w60-h60", "w360-h360");
        }

        let durSec = 0;
        const durText = video.lengthText?.runs?.[0]?.text;
        if (durText) {
          const parts = durText.split(':').map(Number);
          if (parts.length === 2) durSec = parts[0] * 60 + parts[1];
          else if (parts.length === 3) durSec = parts[0] * 3600 + parts[1] * 60 + parts[2];
        }

        list.push({
          id: id,
          title: title,
          artist: artist,
          artistId: artistId,
          album: album,
          artwork: thumb,
          durationSec: durSec,
          streamUrl: ""
        });
      });
    } else {
      logPlayback(`No related tracks found via watch next API for "${cleanId}"`, "warn");
    }

    return list;
  } catch (err) {
    logPlayback(`fetchWatchNext exception: ${err.message}`, "error");
    console.warn("Could not fetch watch next recommendations:", err);
    return [];
  }
}

// Fallback: Fetch related songs via search when fetchWatchNext returns nothing
async function fetchRelatedBySearch(title, artist) {
  try {
    const query = `${title} ${artist || ''} similar songs`.trim();
    logPlayback(`Fallback: Searching related songs for "${query}"...`, "info");
    const searchData = await callInnerTubeAPI('search', { query, params: 'EgWKAQIIAWoKEAMQBBAJEAoQBQ%3D%3D' }, WEB_CONTEXT);

    const list = [];
    const results = searchData?.contents?.tabbedSearchResultsRenderer?.tabs?.[0]?.tabRenderer?.content?.sectionListRenderer?.contents;
    if (results) {
      for (const section of results) {
        const shelf = section.musicShelfRenderer || section.musicCardShelfRenderer;
        const items = shelf?.contents || [];
        for (const item of items) {
          const r = item.musicResponsiveListItemRenderer;
          if (!r) continue;

          const vId = r.playlistItemData?.videoId 
            || r.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId;
          if (!vId || !/^[a-zA-Z0-9_-]{11}$/.test(vId)) continue;

          const titleRuns = r.flexColumns?.[0]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs;
          const trackTitle = titleRuns?.[0]?.text || "Canción";
          
          const artistRuns = r.flexColumns?.[1]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs;
          let trackArtist = "Artista";
          let trackArtistId = "";
          if (artistRuns && artistRuns.length > 0) {
            trackArtist = artistRuns[0].text;
            trackArtistId = artistRuns[0]?.navigationEndpoint?.browseEndpoint?.browseId || "";
          }

          let thumbUrl = "";
          const thumbs = r.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails;
          if (thumbs && thumbs.length > 0) {
            thumbUrl = thumbs[thumbs.length - 1].url;
            if (thumbUrl.includes("ytimg.com")) {
              thumbUrl = thumbUrl.replace("w120-h120", "w360-h360").replace("w60-h60", "w360-h360");
            }
          }

          list.push({
            id: vId,
            title: trackTitle,
            artist: trackArtist,
            artistId: trackArtistId,
            album: "YouTube Music",
            artwork: thumbUrl,
            durationSec: 0,
            streamUrl: ""
          });

          if (list.length >= 20) break;
        }
        if (list.length >= 20) break;
      }
    }

    logPlayback(`Fallback search found ${list.length} related tracks`, list.length > 0 ? "success" : "warn");
    return list;
  } catch (err) {
    logPlayback(`fetchRelatedBySearch exception: ${err.message}`, "error");
    return [];
  }
}

function setHeaderSearchPillVisible(visible) {
  const searchPill = document.querySelector('.search-pill-box');
  const pageTitle = document.getElementById('page-title');
  if (searchPill) {
    searchPill.style.display = visible ? 'flex' : 'none';
  }
  if (pageTitle) {
    pageTitle.style.display = visible ? 'none' : 'block';
  }
}

function setHeaderVisible(visible) {
  const contentHeader = document.querySelector('.content-header');
  if (contentHeader) {
    if (visible) {
      contentHeader.classList.remove('hidden');
    } else {
      contentHeader.classList.add('hidden');
    }
  }
}

// --- Global Error Diagnostic Handler ---
window.onerror = function(msg, url, lineNo, columnNo, error) {
  console.error("Global Error:", msg, "Line:", lineNo, "Col:", columnNo, error);
  try {
    const errDiv = document.createElement('div');
    errDiv.style.position = 'fixed';
    errDiv.style.top = '10px';
    errDiv.style.left = '50%';
    errDiv.style.transform = 'translateX(-50%)';
    errDiv.style.background = '#ff2d55';
    errDiv.style.color = '#ffffff';
    errDiv.style.padding = '12px 20px';
    errDiv.style.borderRadius = '10px';
    errDiv.style.zIndex = '999999';
    errDiv.style.fontSize = '12.5px';
    errDiv.style.fontWeight = 'bold';
    errDiv.style.boxShadow = '0 10px 30px rgba(0,0,0,0.8)';
    errDiv.textContent = `JS Error: ${msg} (Línea ${lineNo})`;
    document.body.appendChild(errDiv);
    setTimeout(() => errDiv.remove(), 10000);
  } catch(e) {}
  return false;
};

// --- Navigation Tab Loader ---
function loadTab(tabName, shouldPushHistory = true) {
  setHeaderVisible(true);

  const cleanTab = (tabName || '').trim().toLowerCase();
  const isSearchTab = cleanTab.includes('buscar') || cleanTab.includes('search');
  setHeaderSearchPillVisible(isSearchTab);

  if (shouldPushHistory) {
    pushNavigation({ name: 'tab', params: { tabName } });
  }

  if (isSearchTab) {
    renderExploreCategoriesView();
    return;
  }

  contentArea.innerHTML = `<div class="loading-spinner-container"><div class="spinner"></div><p>Cargando ${tabName}...</p></div>`;
  document.getElementById('page-title').textContent = tabName;

  if (cleanTab === 'home' || cleanTab === 'inicio') {
    loadHomeFeed();
  } else if (cleanTab === 'novedades' || cleanTab === 'explore') {
    loadExploreFeed();
  } else if (cleanTab === 'radio') {
    loadRadioFeed();
  } else if (cleanTab === 'conciertos') {
    loadConcertsFeed();
  } else if (cleanTab === 'canciones' || cleanTab === 'favoritos' || cleanTab === 'me gusta' || cleanTab === 'liked') {
    renderLikedSongsView();
  } else if (cleanTab.includes('playlist')) {
    renderAllPlaylistsView();
  } else if (cleanTab.includes('álbum') || cleanTab.includes('album')) {
    renderSavedAlbumsView();
  } else if (cleanTab.includes('artista')) {
    renderFollowedArtistsView();
  } else if (cleanTab.includes('reciente') || cleanTab.includes('historial')) {
    renderRecentlyPlayedView();
  } else if (cleanTab.includes('pin')) {
    renderPinnedItemsView();
  } else if (cleanTab.includes('ajuste') || cleanTab.includes('configuraci') || cleanTab.includes('setting')) {
    renderSettingsView();
  } else if (cleanTab.includes('video')) {
    renderLikedSongsView();
  } else {
    renderLikedSongsView();
  }
}

function renderSettingsView() {
  setHeaderVisible(true);
  document.getElementById('page-title').textContent = "Ajustes";
  
  const savedQuality = localStorage.getItem('rm_audio_quality') || 'high';
  const savedNorm = localStorage.getItem('rm_volume_norm') !== 'false';
  const savedMotion = localStorage.getItem('rm_motion_video') !== 'false';
  const savedLyricsProvider = selectedLyricsProvider || 'auto';
  const savedAccentColor = localStorage.getItem('rm_accent_color') || '#ff2d55';

  contentArea.innerHTML = `
    <div style="max-width: 820px; padding: 28px 32px; animation: fadeIn 0.25s ease-out; color: white;">
      
      <!-- Audio & Streaming Section -->
      <section style="margin-bottom: 32px; background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.06); border-radius: 20px; padding: 24px;">
        <h2 style="font-size: 18px; font-weight: 800; margin-bottom: 18px; display: flex; align-items: center; gap: 10px; color: var(--accent-color);">
          <svg viewBox="0 0 24 24" width="20" height="20"><path fill="currentColor" d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>
          Calidad de Audio y Reproducción
        </h2>
        
        <!-- Audio Quality -->
        <div style="display: flex; align-items: center; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid rgba(255,255,255,0.06);">
          <div>
            <div style="font-size: 14px; font-weight: 700;">Calidad de sonido de transmisión</div>
            <div style="font-size: 12px; color: rgba(255,255,255,0.6); margin-top: 2px;">Selecciona la calidad de audio PCM / AAC preferida</div>
          </div>
          <select id="set-audio-quality" style="background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.2); color: white; border-radius: 10px; padding: 6px 12px; font-size: 13px; font-weight: 700; outline: none; cursor: pointer;">
            <option value="high" ${savedQuality === 'high' ? 'selected' : ''}>Alta (320 kbps / PCM WebAudio)</option>
            <option value="normal" ${savedQuality === 'normal' ? 'selected' : ''}>Normal (160 kbps)</option>
            <option value="low" ${savedQuality === 'low' ? 'selected' : ''}>Ahorro de datos (96 kbps)</option>
          </select>
        </div>

        <!-- Volume Normalization -->
        <div style="display: flex; align-items: center; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid rgba(255,255,255,0.06);">
          <div>
            <div style="font-size: 14px; font-weight: 700;">Normalización de volumen</div>
            <div style="font-size: 12px; color: rgba(255,255,255,0.6); margin-top: 2px;">Mantiene el mismo nivel de volumen en todas las canciones</div>
          </div>
          <input type="checkbox" id="set-volume-norm" ${savedNorm ? 'checked' : ''} style="width: 20px; height: 20px; accent-color: var(--accent-color); cursor: pointer;" />
        </div>
      </section>

      <!-- Visual & Design Section -->
      <section style="margin-bottom: 32px; background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.06); border-radius: 20px; padding: 24px;">
        <h2 style="font-size: 18px; font-weight: 800; margin-bottom: 18px; display: flex; align-items: center; gap: 10px; color: var(--accent-color);">
          <svg viewBox="0 0 24 24" width="20" height="20"><path fill="currentColor" d="M12 3c-4.97 0-9 4.03-9 9 0 2.12.74 4.07 1.97 5.61L4.35 19.4c-.39.39-.39 1.02 0 1.41.39.39 1.02.39 1.41 0l1.9-1.9C9.22 19.53 10.57 20 12 20c4.97 0 9-4.03 9-9s-4.03-9-9-9z"/></svg>
          Apariencia y Visuales
        </h2>

        <!-- Artist Motion Video Backgrounds -->
        <div style="display: flex; align-items: center; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid rgba(255,255,255,0.06);">
          <div>
            <div style="font-size: 14px; font-weight: 700;">Videos Animados de Fondo en Artistas</div>
            <div style="font-size: 12px; color: rgba(255,255,255,0.6); margin-top: 2px;">Reproduce canvas de Apple Music HLS en la portada de artistas</div>
          </div>
          <input type="checkbox" id="set-motion-video" ${savedMotion ? 'checked' : ''} style="width: 20px; height: 20px; accent-color: var(--accent-color); cursor: pointer;" />
        </div>

        <!-- Accent Color Picker -->
        <div style="display: flex; align-items: center; justify-content: space-between; padding: 12px 0;">
          <div>
            <div style="font-size: 14px; font-weight: 700;">Color de Acento de la Aplicación</div>
            <div style="font-size: 12px; color: rgba(255,255,255,0.6); margin-top: 2px;">Elige el tema de color para botones e indicadores</div>
          </div>
          <div style="display: flex; gap: 10px; align-items: center;">
            <button class="accent-pick-btn" data-color="#ff2d55" style="width: 26px; height: 26px; border-radius: 50%; background: #ff2d55; border: 2px solid ${savedAccentColor === '#ff2d55' ? '#fff' : 'transparent'}; cursor: pointer;"></button>
            <button class="accent-pick-btn" data-color="#8a2be2" style="width: 26px; height: 26px; border-radius: 50%; background: #8a2be2; border: 2px solid ${savedAccentColor === '#8a2be2' ? '#fff' : 'transparent'}; cursor: pointer;"></button>
            <button class="accent-pick-btn" data-color="#007aff" style="width: 26px; height: 26px; border-radius: 50%; background: #007aff; border: 2px solid ${savedAccentColor === '#007aff' ? '#fff' : 'transparent'}; cursor: pointer;"></button>
            <button class="accent-pick-btn" data-color="#00e676" style="width: 26px; height: 26px; border-radius: 50%; background: #00e676; border: 2px solid ${savedAccentColor === '#00e676' ? '#fff' : 'transparent'}; cursor: pointer;"></button>
          </div>
        </div>
      </section>

      <!-- Lyrics Provider Section -->
      <section style="margin-bottom: 32px; background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.06); border-radius: 20px; padding: 24px;">
        <h2 style="font-size: 18px; font-weight: 800; margin-bottom: 18px; display: flex; align-items: center; gap: 10px; color: var(--accent-color);">
          <svg viewBox="0 0 24 24" width="20" height="20"><path fill="currentColor" d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"/></svg>
          Proveedor de Letras Preferido
        </h2>
        
        <div style="display: flex; align-items: center; justify-content: space-between; padding: 12px 0;">
          <div>
            <div style="font-size: 14px; font-weight: 700;">Proveedor Predeterminado</div>
            <div style="font-size: 12px; color: rgba(255,255,255,0.6); margin-top: 2px;">Origen para buscar y sincronizar letras de canciones</div>
          </div>
          <select id="set-lyrics-provider" style="background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.2); color: white; border-radius: 10px; padding: 6px 12px; font-size: 13px; font-weight: 700; outline: none; cursor: pointer;">
            <option value="auto" ${savedLyricsProvider === 'auto' ? 'selected' : ''}>Auto (YouTube + LRCLIB)</option>
            <option value="lrclib" ${savedLyricsProvider === 'lrclib' ? 'selected' : ''}>LRCLIB</option>
            <option value="kugou" ${savedLyricsProvider === 'kugou' ? 'selected' : ''}>KuGou</option>
            <option value="betterlyrics" ${savedLyricsProvider === 'betterlyrics' ? 'selected' : ''}>BetterLyrics</option>
            <option value="lyricsplus" ${savedLyricsProvider === 'lyricsplus' ? 'selected' : ''}>LyricsPlus (Apple/Spotify)</option>
            <option value="simpmusic" ${savedLyricsProvider === 'simpmusic' ? 'selected' : ''}>SimpMusic</option>
          </select>
        </div>
      </section>

      <!-- System & Diagnostics Section -->
      <section style="background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.06); border-radius: 20px; padding: 24px;">
        <h2 style="font-size: 18px; font-weight: 800; margin-bottom: 18px; display: flex; align-items: center; gap: 10px; color: var(--accent-color);">
          <svg viewBox="0 0 24 24" width="20" height="20"><path fill="currentColor" d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2z"/></svg>
          Sistema y Caché
        </h2>

        <div style="display: flex; align-items: center; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid rgba(255,255,255,0.06);">
          <div>
            <div style="font-size: 14px; font-weight: 700;">Caché Local de la Aplicación</div>
            <div style="font-size: 12px; color: rgba(255,255,255,0.6); margin-top: 2px;">Limpiar imágenes guardadas en caché y datos temporales</div>
          </div>
          <button id="btn-clear-cache" style="background: rgba(255,45,85,0.18); border: 1px solid rgba(255,45,85,0.4); color: #ff2d55; padding: 6px 16px; border-radius: 10px; font-size: 13px; font-weight: 700; cursor: pointer;">Limpiar Caché</button>
        </div>

        <div style="display: flex; align-items: center; justify-content: space-between; padding: 12px 0;">
          <div>
            <div style="font-size: 14px; font-weight: 700;">Versión de RayMusic</div>
            <div style="font-size: 12px; color: rgba(255,255,255,0.6); margin-top: 2px;">RayMusic Native Desktop v2.4.0 (Windows x64 WebView2)</div>
          </div>
          <span style="font-size: 12px; font-weight: 800; color: var(--accent-color); background: rgba(255,255,255,0.08); padding: 4px 10px; border-radius: 8px;">v2.4.0</span>
        </div>
      </section>

    </div>
  `;

  // Attach Settings Listeners
  const qualitySel = document.getElementById('set-audio-quality');
  if (qualitySel) {
    qualitySel.addEventListener('change', (e) => {
      localStorage.setItem('rm_audio_quality', e.target.value);
    });
  }

  const normChk = document.getElementById('set-volume-norm');
  if (normChk) {
    normChk.addEventListener('change', (e) => {
      localStorage.setItem('rm_volume_norm', e.target.checked ? 'true' : 'false');
    });
  }

  const motionChk = document.getElementById('set-motion-video');
  if (motionChk) {
    motionChk.addEventListener('change', (e) => {
      localStorage.setItem('rm_motion_video', e.target.checked ? 'true' : 'false');
    });
  }

  const lyricsSel = document.getElementById('set-lyrics-provider');
  if (lyricsSel) {
    lyricsSel.addEventListener('change', (e) => {
      selectedLyricsProvider = e.target.value;
    });
  }

  document.querySelectorAll('.accent-pick-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const color = btn.dataset.color;
      document.documentElement.style.setProperty('--accent-color', color);
      localStorage.setItem('rm_accent_color', color);
      renderSettingsView();
    });
  });

  const clearBtn = document.getElementById('btn-clear-cache');
  if (clearBtn) {
    clearBtn.addEventListener('click', () => {
      clearBtn.textContent = '¡Caché Limpiada!';
      setTimeout(() => clearBtn.textContent = 'Limpiar Caché', 2000);
    });
  }
}

const INICIO_DEFAULT_DATA = {};

function renderHomeShelfSection(title, items) {
  const section = document.createElement('section');
  section.className = "content-section";
  section.innerHTML = `<h2 class="section-title-sub" style="margin-bottom: 14px; border-bottom: 1px solid var(--border-color); padding-bottom: 6px;">${title}</h2>`;

  const container = document.createElement('div');
  container.style.display = "flex";
  container.style.flexDirection = "column";
  container.style.gap = "4px";

  items.forEach((item, idx) => {
    const row = document.createElement('div');
    row.style.display = "flex";
    row.style.alignItems = "center";
    row.style.padding = "8px 16px";
    row.style.borderRadius = "10px";
    row.style.cursor = "pointer";
    row.style.backgroundColor = "rgba(255,255,255,0.02)";
    row.style.transition = "background-color 0.15s";

    row.addEventListener('mouseenter', () => row.style.backgroundColor = "rgba(255,255,255,0.06)");
    row.addEventListener('mouseleave', () => row.style.backgroundColor = "rgba(255,255,255,0.02)");

    row.innerHTML = `
      <span style="width: 28px; font-size: 12px; color: var(--text-muted); text-align: center;">${idx + 1}</span>
      <img src="${item.artwork}" style="width: 38px; height: 38px; border-radius: ${item.type === 'artist' ? '50%' : '6px'}; object-fit: cover; margin-right: 16px;">
      <div style="display: flex; flex-direction: column; flex-grow: 1; overflow: hidden; white-space: nowrap;">
        <span style="font-size: 13.5px; font-weight: 600; color: var(--text-primary); text-overflow: ellipsis; overflow: hidden;">${item.title}</span>
        <span class="artist-link" style="font-size: 11.5px; color: var(--text-secondary); text-overflow: ellipsis; overflow: hidden; align-self: flex-start;" onclick="event.stopPropagation(); loadArtistPage('${item.artistId}', '${item.artist}')">${item.artist}</span>
      </div>
    `;

    row.addEventListener('click', () => {
      if (item.type === 'song') {
        playTrackDetails(item.id, item.title, item.artist, item.artwork, item.artistId, item.durationSec || 0);
      } else if (item.type === 'artist') {
        loadArtistPage(item.id, item.title);
      } else {
        loadPlaylistContents(item.id, item.title);
      }
    });

    container.appendChild(row);
  });

  section.appendChild(container);
  contentArea.appendChild(section);
}

function parseCarouselShelf(shelf) {
  const items = [];
  if (shelf.contents || shelf.items) {
    const rawList = shelf.contents || shelf.items || [];
    rawList.forEach(itemContainer => {
      const item = itemContainer.musicTwoRowItemRenderer || itemContainer.musicResponsiveListItemRenderer || itemContainer.musicMultiRowListItemRenderer;
      if (!item) return;
      
      const songId = item.navigationEndpoint?.watchEndpoint?.videoId || item.onTap?.watchEndpoint?.videoId;
      const playlistId = item.navigationEndpoint?.browseEndpoint?.browseId 
                      || item.title?.runs?.[0]?.navigationEndpoint?.browseEndpoint?.browseId;
      
      let titleText = "Música";
      if (item.title?.runs?.[0]?.text) {
        titleText = item.title.runs.map(r => r.text).join("");
      } else if (item.flexColumns?.[0]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.[0]?.text) {
        titleText = item.flexColumns[0].musicResponsiveListItemFlexColumnRenderer.text.runs.map(r => r.text).join("");
      }
      
      const subtitleRuns = item.subtitle?.runs || item.flexColumns?.[1]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs || [];
      const artistInfo = extractArtistInfo(subtitleRuns, "Artista");
      let artistText = artistInfo.artistText;
      let artistId = artistInfo.artistId;
      let durSec = 0;
      if (subtitleRuns && subtitleRuns.length > 0) {
        const lastRun = subtitleRuns[subtitleRuns.length - 1]?.text;
        if (lastRun && /^\d+:\d+(:\d+)?$/.test(lastRun.trim())) {
          durSec = parseDurationToSeconds(lastRun.trim());
        }
      }
      
      let thumbUrl = upgradeThumbQuality(extractThumbnail(item));

      // Correct ID classification: If browseId exists (MPREb_, VL, OLAK, UC), prioritize it as album/playlist/artist!
      let itemId = playlistId || songId;
      let itemType = 'playlist';

      if (playlistId && (playlistId.startsWith('MPREb_') || playlistId.startsWith('VL') || playlistId.startsWith('OLAK5uy_') || playlistId.startsWith('PL') || playlistId.startsWith('MPSP'))) {
        itemId = playlistId;
        itemType = 'playlist';
      } else if (playlistId && playlistId.startsWith('UC')) {
        itemId = playlistId;
        itemType = 'artist';
      } else if (songId) {
        itemId = songId;
        itemType = 'song';
      } else if (playlistId) {
        itemId = playlistId;
        itemType = 'playlist';
      }

      items.push({
        id: itemId || "item_" + Math.random().toString(36).substr(2, 9),
        type: itemType,
        title: titleText,
        artist: artistText,
        artistId: artistId,
        durationSec: durSec,
        artwork: thumbUrl
      });
    });
  }
  return items;
}



function renderHomeOffline() {
  contentArea.innerHTML = '';
  const demoCards = FALLBACK_TRACKS.map(t => ({
    id: t.id,
    type: 'song',
    title: t.title,
    artist: t.artist,
    artwork: t.artwork,
    artistId: t.artistId
  }));
  renderCarouselSection("Selecciones destacadas (Offline)", demoCards);
}

const APPLE_NOVEDADES_DATA = {
  hero: [
    { title: "EQUILIBRIVM II", artist: "Anitta", type: "album", artwork: "https://lh3.googleusercontent.com/9lQ2Lg2r3e6l4K-X=w600-h600", desc: "Anitta amplía su celebración de la música brasileña en EQUILIBRIVM." },
    { title: "Ay Weyy", artist: "Jorsshh", type: "album", artwork: "https://lh3.googleusercontent.com/8xY6b4k=w600-h600", desc: "Jorsshh toma la pluma y el micrófono para poner su firma al frente del corrido." },
    { title: "LEGENDARIO", artist: "LEGADO 7", type: "album", artwork: "https://lh3.googleusercontent.com/7a89k=w600-h600", desc: "Legado 7 se despide a su manera." }
  ],
  tracks: [
    { title: "Camera", artist: "Charli xcx", explicit: true, artwork: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=180" },
    { title: "que te vaya bien", artist: "Ryan Castro", explicit: true, artwork: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=180" },
    { title: "Ella (Acústico)", artist: "Boza, Beéle", explicit: false, artwork: "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=180" },
    { title: "Mejor que Yo", artist: "Maisak", explicit: false, artwork: "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=180" },
    { title: "Pa la Maña", artist: "Natanael Cano, Gabito Ballesteros", explicit: true, artwork: "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=180" },
    { title: "Recuerdos en Común", artist: "Eden Muñoz, Alfredo Olivas", explicit: false, artwork: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=180" },
    { title: "Déjese Querer", artist: "La Adictiva, Xavi", explicit: false, artwork: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=180" },
    { title: "Celosa", artist: "Kidd Voodoo", explicit: false, artwork: "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=180" },
    { title: "MI BB", artist: "Lunay, Omar Courtz", explicit: true, artwork: "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=180" },
    { title: "Azul", artist: "Anitta", explicit: false, artwork: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=180" },
    { title: "Mostaza", artist: "Jorsshh", explicit: true, artwork: "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=180" },
    { title: "Mala y Atrevida", artist: "Alan Arrieta, La Joaqui", explicit: false, artwork: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=180" },
    { title: "Animal", artist: "KATSEYE", explicit: false, artwork: "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=180" },
    { title: "ME MIRAN", artist: "LEGADO 7, Rey Quinto", explicit: true, artwork: "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=180" },
    { title: "MILLONETA", artist: "Jere Klein, Blessd", explicit: false, artwork: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=180" },
    { title: "Exclusive.mp3", artist: "Emilia", explicit: false, artwork: "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=180" }
  ]
};

// Setup interactive floating arrow visibility based on scroll position
function bindCarouselPillArrows(container, btnPrev, btnNext) {
  function updateArrowVisibility() {
    const scrollLeft = container.scrollLeft;
    const maxScroll = container.scrollWidth - container.clientWidth;

    if (scrollLeft <= 5) {
      btnPrev.classList.add('hidden-arrow');
    } else {
      btnPrev.classList.remove('hidden-arrow');
    }

    if (scrollLeft >= maxScroll - 5) {
      btnNext.classList.add('hidden-arrow');
    } else {
      btnNext.classList.remove('hidden-arrow');
    }
  }

  container.addEventListener('scroll', updateArrowVisibility);
  window.addEventListener('resize', updateArrowVisibility);
  setTimeout(updateArrowVisibility, 100);
}

// --- Explore/Novedades Feed (Apple Music Full Parity via InnerTube API) ---
const NOVEDADES_APPLE_DATA = {"featuredAlbums": [{"browseId": "MPREb_I5OryN8szPX", "playlistId": "OLAK5uy_lvsnVNUuwEr_zFQYdcnx1Mrdd4z784vo0", "title": "NOCTURNO", "artists": [{"name": "Eslabon Armado", "id": "UCmqrOR5GZcSNS5BLAAt6hPg"}], "thumbnail": "https://yt3.googleusercontent.com/LiBBldVQV0LnU_o1BfQUNDEJBb8fs9uQPGUC2IiVbE1aep4Cw55DdiPXBaxPuGthe0eTBoHU0hFZoWQ=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_7KWCAlW4pCR", "playlistId": "OLAK5uy_nOJDztdj3BREvx0KefYy2tOBAR-nveUKA", "title": "Tutankamon", "artists": [{"name": "Victor Mendivil", "id": "UCB6w3YIXxmlpbGg8YDVpPgA"}], "thumbnail": "https://yt3.googleusercontent.com/8VQw4Oi6BzBW9Oob_ILS8q0gGtxbOLdjBGECwK7sO10dKliG6rD5AM1Fdv8OCQtuXkWqyGzKZRrjIkjLyg=w60-h60-l90-rj", "year": 2025, "explicit": false}, {"browseId": "MPREb_UEa8d1Rsnuc", "playlistId": "OLAK5uy_nJ4_a9utAdjMghHprwuG6GKtovk3uNa4o", "title": "Asi Como Tu", "artists": [{"name": "Los Tigres Del Norte", "id": "UCaflwdWdaSGPNSyrAG0Uffw"}], "thumbnail": "https://yt3.googleusercontent.com/KFHnIT8N8fHhMzCCYUrDucKvvBIGrBF0rxkZEIFhvNhCXoS431NdEHyMW67B7jEtajYFQz_KnYRI57Ii1w=w60-h60-s-l90-rj", "year": 1997, "explicit": false}, {"browseId": "MPREb_E3hpS0kUInv", "playlistId": "OLAK5uy_lAnF7PGHNIxTF-8iO1FPAM0aJmYJa-6J0", "title": "Loko Soñador", "artists": [{"name": "Kane Rodriguez", "id": "UC5BKBeBWnZqKJw1gY7bwBOA"}], "thumbnail": "https://yt3.googleusercontent.com/1pOeqQkhaPqYQk2mEwG8l8zpf2Sr3GFIcWnYxy8fPlBvJT6rmBsiPAbX4N6dlAO59q0daPlcFna486Y_=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_wX0uESY5LaO", "playlistId": "OLAK5uy_m3wtHkKxeu8fHUOCpSfCNJK_m7dOFqnK8", "title": "sesbianlex", "artists": [{"name": "Chzter", "id": "UCNWdBHFJV6W0t0Q5OCc-exA"}], "thumbnail": "https://yt3.googleusercontent.com/O-kTz5diMvY_tuA69V-e3qEWL-tElliR-GmJIcmyd1KerpfHEGVEQeywJDr2tpHON6ihhWPTQUZ0Ofo=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_1B4JLLNsBKP", "playlistId": "OLAK5uy_lCsscTD6uvPykAzhgVGSRl9IVxOV8gKZs", "title": "Dale Play", "artists": [{"name": "DALE PLAY", "id": "UCFlISIcxweFSgCtbpdS9SFg"}], "thumbnail": "https://yt3.googleusercontent.com/4_7dPRiKETQ7tPc6e8J-swr09MZE1atZLgKxELviWd9onHuIKW_qOwE0ax_DVfnUP45SmfjKIu_fMujE=w60-h60-l90-rj", "year": 2025, "explicit": false}, {"browseId": "MPREb_A4QXe5RQrw7", "playlistId": "OLAK5uy_liz7GMw7cIoCossHUC_7UQ8sOrNqouuIM", "title": "111XPANTIA", "artists": [{"name": "Fuerza Regida", "id": "UC0kxNxFQCK6d2spPz5Sme7Q"}], "thumbnail": "https://yt3.googleusercontent.com/wcctzBOL_SevQ9q9eq-J1V1fnQluovKWND7x5U3aYJLijtTeBXOIMICPrMnHQGL6FmOzasTAv6DsT79V=w60-h60-l90-rj", "year": 2025, "explicit": false}, {"browseId": "MPREb_DirPjWkkgM5", "playlistId": "OLAK5uy_kcQyicoRkCDJ_E0wpnDuqUS7wMOE8pA2s", "title": "Mix: Pop Latino", "artists": [{"name": "Various Artists", "id": null}], "thumbnail": "https://yt3.googleusercontent.com/-XLImbYSFvzYQMOAyK9XU5vRKzboBcm2MjBch8KvEJktR83q1DMDhgwOBy4EyfptPyO59nYJIbSmens=w60-h60-l90-rj", "year": 2023, "explicit": false}, {"browseId": "MPREb_9wf53SPDJGM", "playlistId": "OLAK5uy_neAckvY6_-xnGNgZ4iJTZProZLi6nesmg", "title": "alpha", "artists": [{"name": "Aitana", "id": "UCkyzcFsGWVYSUGaR7YB9OYQ"}], "thumbnail": "https://yt3.googleusercontent.com/fDOCplh1Nxl999BGi2cmQsqPj_pgXdVbgtEEfgV5DcIXXPmwbKrwq03-aLqRf4uKQesqjU8l95s5cpiisA=w60-h60-l90-rj", "year": 2023, "explicit": false}, {"browseId": "MPREb_YMSEP8cAkVN", "playlistId": "OLAK5uy_lhaiuAfWKq_Op2IDWknW48RFimIzBC9GI", "title": "Grandes Exitos", "artists": [{"name": "Shakira", "id": "UCo6JijJGA3IvIiPsawDK3Ww"}], "thumbnail": "https://yt3.googleusercontent.com/M6plWQDM5duPHltLwrSuyIFUAMSiIhvIaFb9J601x5tSE0XFDabazQJkD_EidHKKSEe2v0u7Y0uBc8_h=w60-h60-l90-rj", "year": 2002, "explicit": false}], "featuredNewSongs": [{"id": "GhMX5llEOy8", "title": "F's", "artists": [{"name": "Fuerza Regida", "id": "UC0kxNxFQCK6d2spPz5Sme7Q"}, {"name": "y", "id": null}, {"name": "Gabito Ballesteros", "id": "UCYMm2JZ_mvXYr7vT9-8_thw"}], "thumbnail": "https://yt3.googleusercontent.com/ozzMHKW5Dj7nLOg4_dPiiBlMn5Q-tudXQ847sYrB8CEAtP61sdpK9a6pVfPekXlBfpxjWW6Ce3uMmclV2w=w60-h60-l90-rj", "explicit": true}, {"id": "c2yVqyNalfI", "title": "AMIGOS CON DERECHOS", "artists": [{"name": "Eslabon Armado", "id": "UCmqrOR5GZcSNS5BLAAt6hPg"}, {"name": "y", "id": null}, {"name": "Peso Pluma", "id": "UCzmabbKsmXlWnI9N2kKQ4lA"}], "thumbnail": "https://yt3.googleusercontent.com/LiBBldVQV0LnU_o1BfQUNDEJBb8fs9uQPGUC2IiVbE1aep4Cw55DdiPXBaxPuGthe0eTBoHU0hFZoWQ=w60-h60-l90-rj", "explicit": true}, {"id": "8FLFzOmsb88", "title": "Myke Towers: Bzrp Music Sessions, Vol. 42/66", "artists": [{"name": "Bizarrap", "id": "UCONiUl5u7y2bMaVZJcuRDEQ"}, {"name": "y", "id": null}, {"name": "Myke Towers", "id": "UCYPsIfSIEwWcoynHBP5k1dg"}], "thumbnail": "https://yt3.googleusercontent.com/8QArS95hf1W14AHHEp1sazJrZx63TOm4yfvutbWd7wibAgLn_0_ogqb0X0WHhqrdPjZjD49PYtVRatwD=w60-h60-l90-rj", "explicit": false}, {"id": "bqsAhnJ-QAY", "title": "Wink Wink", "artists": [{"name": "Charli xcx", "id": "UCI4YNnmHjXFaaKvfdmpWvJQ"}], "thumbnail": "https://yt3.googleusercontent.com/AI3LBsdjSNsSC7_TJPXWg0t7BDD3hOyYRdSKnZlBS3TZqANoVR4HcILRLVNHqnD_tfgJPvRZHedWPk6U=w60-h60-l90-rj", "explicit": false}, {"id": "DjqIuXEs1N4", "title": "SI SE ACABA EL MUNDO", "artists": [{"name": "DANNA", "id": "UCu4o_5UkGmx3vyqRkk9kVOQ"}, {"name": "y", "id": null}, {"name": "El Malilla", "id": "UCz0CQ5kSCMG1zXNDQR5jWgQ"}], "thumbnail": "https://yt3.googleusercontent.com/HUsH6fG-Vh5sQxxp2_q7G4HrIusrZ1D-K38qMYaT84ju3cjtJhzSrHtrRu_sRtmWLLvW3NKmGg0IyGNl=w60-h60-l90-rj", "explicit": false}, {"id": "s_pYN0sYk_c", "title": "Watch It Burn", "artists": [{"name": "Katy Perry", "id": "UC_7s69e1mDS3lgcTMJEPjCg"}], "thumbnail": "https://yt3.googleusercontent.com/DCMfRp0y7ZpplOyYrMY3JZxoLyOoTaDvg1IHmgKXzTXFZy9itdeWqBa1pv8SMJoQGiTdvcGik-jpb1Ru=w60-h60-l90-rj", "explicit": true}, {"id": "6As-FdaTwfc", "title": "¿SERÁ EL ALCOHOL?", "artists": [{"name": "Oscar Maydon", "id": "UCxTyOt1nVVexDlQub6kvvNQ"}, {"name": "y", "id": null}, {"name": "Omar Camacho", "id": "UC5TYeA9Hw3L5VlZYtMt3pqg"}], "thumbnail": "https://yt3.googleusercontent.com/gGH8PDWDXL_onkea-afVVeNoKFbpMDC0dxuDadkVjyVPD1H-GfxZvuQfVa0zvg29rwLcibCvH5FK7us=w60-h60-l90-rj", "explicit": true}, {"id": "OsMUzqnrCEQ", "title": "Pensando En Ti", "artists": [{"name": "Xavi", "id": "UCfmeXjlCXi37LGF7O2VT2zA"}, {"name": "y", "id": null}, {"name": "De La Rose", "id": "UCkUHeLHwch0QrYQ3X1wLfzg"}], "thumbnail": "https://yt3.googleusercontent.com/n-fu_8u8xeMO0oJFnL0inHmX79rw8nMcAE-vaiwAY3hSPGWd0sObFIXkJrVbMaNx0kI4j7WRB53pqGQx=w60-h60-l90-rj", "explicit": false}, {"id": "SH556wkCoxY", "title": "NOTA", "artists": [{"name": "Paloma Mami", "id": "UCCKFlwqKsCXP6T9pmidZUwQ"}, {"name": "y", "id": null}, {"name": "Cris MJ", "id": "UCOykHV9q0qb0vrBsxO_5fkQ"}], "thumbnail": "https://yt3.googleusercontent.com/dk6bAKvTK8HFodd6g7WLTdJ3dd_URF8Q4h5z8VbD7vXjS5SEy8euoADkp78XHHvwkXGrtL0I1qDskuI6=w60-h60-l90-rj", "explicit": true}, {"id": "axS35a-ngS8", "title": "Radio", "artists": [{"name": "Future", "id": "UC1_liDR4fRFJgH4HoJeV8cw"}], "thumbnail": "https://yt3.googleusercontent.com/dL51JLPPU_gReX_YC7pTpMk7vNhVaJ0IYWau-95qXFmEdsWpc7AW1T4feRuSeqKrrPOMNlwVueu3KfPX=w60-h60-l90-rj", "explicit": true}, {"id": "kGjNdQxeAoA", "title": "Bandido Estrella", "artists": [{"name": "Jasiel Nuñez", "id": "UCf-KzXFKLRFmK5G2QwNDRaA"}], "thumbnail": "https://yt3.googleusercontent.com/Zup5mKNZEsZRK0ooTA607V_cS_hjPjMx5g1xQIVfxZUaVzAjXETMGYZNTTHPqGLjMPqx0wYKhpWU9Ki7=w60-h60-l90-rj", "explicit": false}, {"id": "mVdnT6w5C2w", "title": "Jealous Lover", "artists": [{"name": "The Rolling Stones", "id": "UCNYhhkQqeFLUc-YEDcLpSYQ"}], "thumbnail": "https://yt3.googleusercontent.com/ioqxnqatCQtOs4O7dRgchSLpkX3W3xZRzcwD17lvm6KlA9N-CfrvLCT3Ri0EbLLDGpUEkqNbsbR3lpE=w60-h60-l90-rj", "explicit": false}, {"id": "bGo20gg0BF8", "title": "ROMO", "artists": [{"name": "Chimbala", "id": "UCj36ACUuHAPPesaM_ydfrxw"}, {"name": "y", "id": null}, {"name": "J Balvin", "id": "UCWw-Guyr5ul9B-d5kJlHMng"}], "thumbnail": "https://yt3.googleusercontent.com/9JrZDa_aFhRH4NmGaWfVFaLzeUBpRIEP2QDya0fc5Cb1ZwNx07MTq2rOVdV2HvKYEJPhiXQcTT8IErrUCg=w60-h60-l90-rj", "explicit": false}, {"id": "roUyOFwVeWA", "title": "PATRONA", "artists": [{"name": "Becky G", "id": "UC3UkDuAQjoRvTH7OEWm3cHQ"}], "thumbnail": "https://yt3.googleusercontent.com/95prktbU4qey6X1FrSN_vuDha4DxAWQhwpa5I7oZTZTLEpss7oXesE3u4jOilDdeYeDfPkDYhPmLLY4=w60-h60-l90-rj", "explicit": true}, {"id": "1yiCb2TK81Q", "title": "Quiero Verte", "artists": [{"name": "Gaby Music", "id": "UCVAtSz_69M2ULexewXFrB_g"}, {"name": "Yandel", "id": "UCc1QpDE0iT0n6ZLckjflNHw"}, {"name": "El Bogueto", "id": "UC8r_j-qnSCj1t4h6EVN5TrQ"}, {"name": "y", "id": null}, {"name": "Luis R Conriquez", "id": "UCm7mbvc5QOn5JzkeCrk-wdw"}], "thumbnail": "https://yt3.googleusercontent.com/HeAVDKa-jXyPk09oirBOwbhg6f3Nh4mAV2l8p2ad2LtM4Cbtoof-Z8JXhVTAVsV2ju5p1fbvyy7qujUH=w60-h60-l90-rj", "explicit": true}, {"id": "TXXAmTEyTlA", "title": "El Klavo", "artists": [{"name": "Jombriel", "id": "UCcg-nLw72KrpK4-urD_9wNw"}, {"name": "Kris R.", "id": "UCGTKMCLP6zCiTumnyJzpyAQ"}, {"name": "y", "id": null}, {"name": "Jøtta", "id": "UC6C7NNrdw54OXhi6PYLILTQ"}], "thumbnail": "https://yt3.googleusercontent.com/u5IRujJjv0CIwBIxzQlXdl7-xUrzkXRw72C3bVQEIY3CMyJ4G_O7kVhc4AH_cvUgvJFDntmhFm-vTVET=w60-h60-l90-rj", "explicit": false}, {"id": "QaHE76FTd7I", "title": "ROMANCE MARGINAL", "artists": [{"name": "MC Rick", "id": "UCkpDNelZmkUPvFLz99im9iA"}, {"name": "EL Bogueto", "id": "UC8r_j-qnSCj1t4h6EVN5TrQ"}, {"name": "y", "id": null}, {"name": "Mc Morena", "id": "UCshLCNqAHZYSCwL48qyfmvw"}], "thumbnail": "https://yt3.googleusercontent.com/aRUjJ30E-yztvqYjiKdSJ7ZvfDvA5JtDMk5RaZi3BQxymb0pf4kt9YnglPOMeSSk_pAKoNcd8kMAoNQ=w60-h60-l90-rj", "explicit": true}, {"id": "XQY5Ap_7GQE", "title": "JET", "artists": [{"name": "Yng Lvcas", "id": "UCOxbTdISAb-MfHMVBDiDJ1w"}], "thumbnail": "https://yt3.googleusercontent.com/ymDtVy22e6i1_dDoWrwC9G2kK-Hrnfgi2p5DHSFB3SNCCuP0QvSHrPMnIfvB4o4qtOTb9qH7h4lmNKw=w60-h60-l90-rj", "explicit": false}, {"id": "xDPbg6ino-U", "title": "AFILANDO (con El Osito Wito, Estrikto, Nuno.w y Chris LA)", "artists": [{"name": "SINAKA", "id": "UCBAymPwzV7tOJb0tx2nV1Nw"}], "thumbnail": "https://yt3.googleusercontent.com/J1e-PlHPMMZK2rdmlciudKdpDa4bjTXZ2mhTmw2jIjkX0bB7XG8gGc171LFEvyXWgA8E0_kvsspmzUrOYA=w60-h60-l90-rj", "explicit": true}, {"id": "FhPUx_CwO8g", "title": "NI SIQUIERA SOMOS AMIGOS", "artists": [{"name": "La Arrolladora Banda el Limón de René Camacho", "id": "UC_vwQJlETC2l_45U43l4QMA"}, {"name": "y", "id": null}, {"name": "Natiuska", "id": "UChy0ZTNl3OFKuD5r4eU05eQ"}], "thumbnail": "https://yt3.googleusercontent.com/G4_sNbctuzPMmsCUu6nLiV_ApOfDfR62-x0jCHIibqZ-mRev174fbvcEV6z2BsvZlGlRrng4SZhrqSnM=w60-h60-l90-rj", "explicit": false}], "newReleaseAlbums": [{"browseId": "MPREb_E3hpS0kUInv", "playlistId": "OLAK5uy_lAnF7PGHNIxTF-8iO1FPAM0aJmYJa-6J0", "title": "Loko Soñador", "artists": [{"name": "Kane Rodriguez", "id": "UC5BKBeBWnZqKJw1gY7bwBOA"}], "thumbnail": "https://yt3.googleusercontent.com/1pOeqQkhaPqYQk2mEwG8l8zpf2Sr3GFIcWnYxy8fPlBvJT6rmBsiPAbX4N6dlAO59q0daPlcFna486Y_=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_I5OryN8szPX", "playlistId": "OLAK5uy_lvsnVNUuwEr_zFQYdcnx1Mrdd4z784vo0", "title": "NOCTURNO", "artists": [{"name": "Eslabon Armado", "id": "UCmqrOR5GZcSNS5BLAAt6hPg"}], "thumbnail": "https://yt3.googleusercontent.com/LiBBldVQV0LnU_o1BfQUNDEJBb8fs9uQPGUC2IiVbE1aep4Cw55DdiPXBaxPuGthe0eTBoHU0hFZoWQ=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_NCeTkyRkVX0", "playlistId": "OLAK5uy_koonMIYTdB6uDTOn-9wzLAF7fAcS5DTFI", "title": "No Signal EP", "artists": [{"name": "pepe arcade", "id": "UCrpv5q6x7vf7mCXZY3ClxHw"}], "thumbnail": "https://yt3.googleusercontent.com/iewcTIqkdKmYO1UH9HTQBP5QZR5PSNwdNsUpj7h-gIN7-OaQPRkBDM1kb0-YnzcZbieKo4qCEYW15Oiu=w60-h60-l90-rj", "year": 2016, "explicit": false}, {"browseId": "MPREb_aG9eSSxgBdE", "playlistId": "OLAK5uy_lSfMvrX-2P2XQf6Q4ZMl0Wd0ggeIsGAZQ", "title": "ROMO", "artists": [{"name": "Chimbala", "id": "UCj36ACUuHAPPesaM_ydfrxw"}], "thumbnail": "https://yt3.googleusercontent.com/5N2qHFpHBdDyZ5GKUuvNN351Q_r8ramqFSdZgFU5NsOMrG48liI549EubFluoAMtDRnQbaL82nbFvnM=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_wX0uESY5LaO", "playlistId": "OLAK5uy_m3wtHkKxeu8fHUOCpSfCNJK_m7dOFqnK8", "title": "sesbianlex", "artists": [{"name": "Chzter", "id": "UCNWdBHFJV6W0t0Q5OCc-exA"}], "thumbnail": "https://yt3.googleusercontent.com/O-kTz5diMvY_tuA69V-e3qEWL-tElliR-GmJIcmyd1KerpfHEGVEQeywJDr2tpHON6ihhWPTQUZ0Ofo=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_9Hg28kW64J0", "playlistId": "OLAK5uy_lbs5fI3m6mdOn77vRcOWguTd8h3Sx25d8", "title": "Epic Phonk Songs Hard Heavy Bass Aggressive Sigma Phonk Beats", "artists": [{"name": "Best Phonk Music", "id": "UCtF33rp7MGelfoGLSpIdFDg"}, {"name": "Phonk Music 2023", "id": "UC53SLi6_c7FJ1E68HC9Dl4A"}, {"name": "y", "id": null}, {"name": "Phonk Christmas Music", "id": "UCRguRDtVPv4eh0lExFWxhzg"}], "thumbnail": "https://yt3.googleusercontent.com/JXUD1gmRO0b8vJ_a332WBlST7-VNm2ANyjQPJkkaOnMBMxls7o4ysPePDGeTO-B6DoO0_42UqHOVzfU=w60-h60-l90-rj", "year": 2024, "explicit": false}, {"browseId": "MPREb_0Hp3qq7YuSg", "playlistId": "OLAK5uy_lEtk5mVlOEAyx0vNqcaJ5T3V4a6hBMdc8", "title": "Tres De Espadas", "artists": [{"name": "1OO1O", "id": "UCZlxRH_fJ4uwE75R588lJLQ"}], "thumbnail": "https://yt3.googleusercontent.com/Pp_zGflGFqehlmEsMyhBcRwTT-EFPaCjoAvi6FVMIZRb-PMf2NcgQZqDpxxN6JvOw1bOZahgF3KOZuM_og=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_wIa93QHCgQo", "playlistId": "OLAK5uy_lSlSEGzIW_ddDxPJq2YzWf7hRowHHIiq0", "title": "ZOMBIES 4: Dawn of the Vampires (Original Soundtrack)", "artists": [{"name": "ZOMBIES – Cast", "id": "UCc2_g_9LC2VK2mNKRYSbykg"}, {"name": "y", "id": null}, {"name": "Disney", "id": "UC0L4FNvqduCM49XPjb8dZHQ"}], "thumbnail": "https://yt3.googleusercontent.com/EABwEqAJxgCA94oPB46qmsacf1y0fK8QZbgcMucb55-xvZwfaaUQYknQNpHH-BjtcdGB8AGUqil7jmhL=w60-h60-l90-rj", "year": 2025, "explicit": false}, {"browseId": "MPREb_wsuDXSGG1AN", "playlistId": "OLAK5uy_lGyANcDWZYDAUOwZEOJE2ZrxiL9iiA7rQ", "title": "perfectas", "artists": [{"name": "Emilia", "id": "UCvXoAG_trv-m2pv5_1HVFvA"}], "thumbnail": "https://yt3.googleusercontent.com/wEseKvxvSkGWiLK_JubR8e68grON45HpoKkquiPTzluFNlsBarCDAfQ2sifix8yQchLLT2EPZoHSYshK=w60-h60-l90-rj", "year": 2025, "explicit": false}, {"browseId": "MPREb_UzCBw0XTqOd", "playlistId": "OLAK5uy_mdscw7L2slizCz6_vuxP0jfxUrMbAWq9o", "title": "Dos", "artists": [{"name": "Fanny Lu", "id": "UCTUdU_6W-hupRABjkM3a5nw"}], "thumbnail": "https://yt3.googleusercontent.com/voFEYrWoPeqysRkRiYY7P8a_s2Zov_eTPoVxipWBeQFm-ksRugIz8-MhTWKbyABauKr8iro2FF11eER8=w60-h60-l90-rj", "year": 2008, "explicit": false}, {"browseId": "MPREb_iEXZr4ChGwH", "playlistId": "OLAK5uy_kcp2iNMwh5jDVu8gE8AGHJL_bFki_hWIc", "title": "SWAG LIVE FROM COACHELLA (Weekend I)", "artists": [{"name": "Justin Bieber", "id": "UCGvj8kfUV5Q6lzECIrGY19g"}], "thumbnail": "https://yt3.googleusercontent.com/82uhjk3fKtkmLF3cr7wgkwuQT4WEDK2ggzXggPo0IAKl3lZmslAoSGV1zgnzNtOSzJ4jXyUXiHm0HtdF=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_MdOeuSgmXVT", "playlistId": "OLAK5uy_mv-9h--NTXamogIucvGvrclMzBV4xB6Zw", "title": "Moana (Original Motion Picture Soundtrack)", "artists": [{"name": "Disney", "id": "UC0L4FNvqduCM49XPjb8dZHQ"}, {"name": "Lin-Manuel Miranda", "id": "UCAFIersvPYEQzT7XPRWUj8g"}, {"name": "Catherine Laga'aia", "id": "UCVDDIg8So6g7TQKua28dcAw"}, {"name": "y", "id": null}, {"name": "Dwayne Johnson", "id": "UCUsyzSctz8ZkE6RBHs8Vwxg"}], "thumbnail": "https://yt3.googleusercontent.com/tABuKQzonum1-pDWTV4Qz8IrIRwtxwtYehFGCAdtnWKQZbbo-Bq4lDuv2-_mzhEH4h3xtvlm3l2IhmCD=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_inXxgnrcJFP", "playlistId": "OLAK5uy_l3mEEq1wu4TkAODa0FZNuY9K3vZa9GbwM", "title": "Free for Profit", "artists": [{"name": "free for profit type beat", "id": "UCG8Poao44Q7cFHpyEvuFYTQ"}], "thumbnail": "https://yt3.googleusercontent.com/BUehGh9NdcsGxfeAl7JVJM-0T4S0QPzp1EYMf4vwEGwMuldgpQW27ynNFQrQeetxIwn9RIfrDnnNMgk=w60-h60-l90-rj", "year": 2024, "explicit": false}, {"browseId": "MPREb_YwhlerQNpTE", "playlistId": "OLAK5uy_mEqFXWyxInmNL9aIkvUONVi_p10SbfCAI", "title": "All We Need Is Now", "artists": [{"name": "Las Robertas", "id": "UC2BXJNoU3u4HVCDS-CpSSVg"}], "thumbnail": "https://yt3.googleusercontent.com/6kpR04luX9pGAUJy22pNy5taEJDYcZbIReA2xCnL7MRom9kWZvIHcEB6hfGFt37c5S4oek1y8nLFrugc=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_w1Vs745RU4V", "playlistId": "OLAK5uy_kc4Tuk8HiOsYnFsknXrQwjcYJ1M5WJKQI", "title": "Public Luxury", "artists": [{"name": "Downtown Boys", "id": "UCnFJGY5md_Ic1QwNFbhkZDg"}], "thumbnail": "https://yt3.googleusercontent.com/P9r7I38JzzCsH8aB-HS8XvG6i6M3qLTwer1gc0fUUchE-jC3l6MMPC64hRJqAvCGxs6v9ewfTdKU_iuzoQ=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_hpp75regaZQ", "playlistId": "OLAK5uy_mY8J8JnfbFFJCJ6QI10nIExJa1E-DtYjU", "title": "Deluxe Notre-Dame", "artists": [{"name": "Morrissey", "id": "UC_Lm9hIkUlRr4pURZg5nE3g"}], "thumbnail": "https://yt3.googleusercontent.com/NavJNXnSGWXbWeMSE5R5MuyIQsLDHQkIT31yv1ffRwuZy3HMuRa7mlXzxiyozOBxSXtqk94FQ3ynoFtm=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_QYjg4PSQ0Ny", "playlistId": "OLAK5uy_mXzCOsgnxTWu723kqSc1WEZUwQsryT1i8", "title": "A Tiempo", "artists": [{"name": "Espinoza Paz", "id": "UCZOfC1kYm_MZeT-Kq-KYHvA"}], "thumbnail": "https://yt3.googleusercontent.com/3eX5l7Q0JF5I_0_CMhevvKbknZTFENDVtVo6zICKxwn27XHgNsa4PsJgsO7GRzmEo35ZkTrvkezwVtQ=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_ejZfpXRQCyY", "playlistId": "OLAK5uy_nO_1llxrmdlKm5Fs2b96vEazL_6e80640", "title": "Offering", "artists": [{"name": "Ibeyi", "id": "UCiQ979Y__qwtNlEw71JEyow"}], "thumbnail": "https://yt3.googleusercontent.com/o8W7upA3fKZYu8ysTOVKk6TABz4OSuDsytb1n3xje11W-L4jNDJPOnnoG82h6dO7Gytvl0tqKy_cktSnIw=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_lbxRnhQDMgs", "playlistId": "OLAK5uy_lnXsIfI2mlcSH8i6o_b0fakl0u1CSHqB4", "title": "Harmony", "artists": [{"name": "Brutalismus 3000", "id": "UCV0kzMt4a95ji7OISiVSWYQ"}], "thumbnail": "https://yt3.googleusercontent.com/kJatWjDtWmQ_cniM9IRaSkujJ9TXXd5EXNucGcohCn9pBWjNFQtSKm6l52nuye9Zx13GHCiZPQ0AHYk=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_hAQrUlKI7Vx", "playlistId": "OLAK5uy_lxvbKXFp0hr5V36WE1_zK3uKHSR9rFwRM", "title": "Music Sounds Better With You", "artists": [{"name": "Stardust", "id": "UCjuD2g8i12Kwl-7NUOthFgw"}], "thumbnail": "https://yt3.googleusercontent.com/jw3j8NVO6TpOHEy_T33qc8repeE5_QKVWhweJD4pGxHSh6tlaQJXU6nu2FJVf9qe65Ov0kiDqMCvke9f=w60-h60-l90-rj", "year": 1998, "explicit": false}], "trendingSongs": [{"id": "kC98BXMwuYk", "title": "Skokka", "artists": [{"name": "Huan 62", "id": "UCtVysF6yD3MqLulPTrtU_bQ"}, {"name": "y", "id": null}, {"name": "Kelvin Avm", "id": "UCDaIKEU7-TyrfdRkJ6MAsMQ"}], "thumbnail": "https://yt3.googleusercontent.com/_WBMx_ici5s0FzIBK76VOvo-tTgl0e-obaNjwVBoM7MNdUxRmyLD0immBi2GpCgHic20aIuHHefRKjmuww=w60-h60-l90-rj", "explicit": true}, {"id": "P7VgXIZSN_w", "title": "stupid song", "artists": [{"name": "Olivia Rodrigo", "id": "UCE5XNpliPM-SmyFEp61tL_g"}], "thumbnail": "https://yt3.googleusercontent.com/q0szuVtXvUdftTC8k9fjwazdEpoaCyWTZ1d5Xa3GWHhQPD6_59W_rPlmZRFa2rSFPLTmfOGEgvPfF9uBVg=w60-h60-l90-rj", "explicit": false}, {"id": "lFQdcPTTzSg", "title": "Dai Dai", "artists": [{"name": "Shakira", "id": "UCo6JijJGA3IvIiPsawDK3Ww"}, {"name": "y", "id": null}, {"name": "Burna Boy", "id": "UCr61sufuLt7_eB7ak1bXHIg"}], "thumbnail": "https://yt3.googleusercontent.com/moXpr_lvHpb9m70jTjr-0GE63_VyHivgmyH7UPBJmgwWPHwwmvhxDPPZtdAhCFo7CdzmzIGwBE3cn4VuCQ=w60-h60-l90-rj", "explicit": false}, {"id": "DAXXRuW5-mM", "title": "Nalguita y Teta", "artists": [{"name": "Neton Vega", "id": "UCHqMn3yv_kft6-kpbiMgDoA"}], "thumbnail": "https://yt3.googleusercontent.com/-ZvtOuzOnCNy5dviZPvNDbMtirOKd3OhH6pOiOjHt-rsYoQFWYuxh7yen--6PFGDEllJejc4vE1s2tU=w60-h60-l90-rj", "explicit": false}, {"id": "iEZVDHGUoi0", "title": "Madrugada", "artists": [{"name": "Chino Pacas", "id": "UCkY8mKtAicIMSCG9dYP94zw"}, {"name": "y", "id": null}, {"name": "FLVCKKA", "id": "UCh7_RzWLmYtlXTD0ua7wcvw"}], "thumbnail": "https://yt3.googleusercontent.com/2Ob7_hapOHhAl8VBsGcnIS7V2I8JAK0OsFXyKjCbx4y6hAPAxw1e4gNoqhhc-OMq4MtgLEMytDmDot7Y=w60-h60-l90-rj", "explicit": true}, {"id": "O6s8PJpeObo", "title": "Entre Tu y Yo", "artists": [{"name": "Bayriton", "id": "UCEhmRulBDpFm3lIoEJr2jIw"}, {"name": "Piero 47", "id": "UCP0AcNtq9vpaotuFR2IPixA"}, {"name": "Raven la R", "id": "UCA1GtMQIoVnIFM9N9rxMRgQ"}, {"name": "y", "id": null}, {"name": "JORGE BAIRON CAMILO NANCUPIL VERA", "id": "UCFeDmjGUtXCa_cIbGbaBd1Q"}], "thumbnail": "https://yt3.googleusercontent.com/FfsiTUxKISoP84OTonJf6k_wzhBaxsUHhpzY90pH15LZEEXbCRwJTkZtRDE7VznDfw7Qwp9lqJadwhA=w60-h60-l90-rj", "explicit": false}, {"id": "ntyEivMRJgs", "title": "GAMINA", "artists": [{"name": "Kris R.", "id": "UCGTKMCLP6zCiTumnyJzpyAQ"}, {"name": "y", "id": null}, {"name": "Los Money Makers", "id": "UC4e3KA1brwpODpV23ansI0A"}], "thumbnail": "https://yt3.googleusercontent.com/m09XfzywKwkxWKZdAbGHvHA1dPjdJe8zQANXSofXdEQVaXLzXSQO3_mvU1lfliDIsNPSDoYiLjizIvFxVQ=w60-h60-l90-rj", "explicit": true}, {"id": "REXM8ueqrJA", "title": "Ware", "artists": [{"name": "Skinny Tlw", "id": "UCqyblkFFIZ4at8itfQB6hAA"}], "thumbnail": "https://yt3.googleusercontent.com/yXsmyNcGh-QOi2gWxfHpTp9J1pWGfdx_RuOWyK8Fv0kdyL5MmCkghR6823qsDB1AnFkeiGOw-e7Taweftg=w60-h60-l90-rj", "explicit": false}, {"id": "xlmJZOxb-Nk", "title": "Just The Way You Are", "artists": [{"name": "Milky", "id": "UC8FfIzD5DkuqxwBu9bTJ1mA"}], "thumbnail": "https://yt3.googleusercontent.com/83z9xXHuINwUZCGZqwOWdmLYJwENbrnYbHzWi6FsP9pDsWdr7j4RCt-iUQTPTucjNi9RQUjpA5tFpTw=w60-h60-l90-rj", "explicit": false}, {"id": "4IYsSKH7uCE", "title": "ME VALE V", "artists": [{"name": "Tito Double P", "id": "UCATfo9SAdXImyc6ygfbzJBg"}], "thumbnail": "https://yt3.googleusercontent.com/1kzWBZnTlchYleUEYiXN4mwIvBHxG26nQsNsqWfJIU4VLTV21FcCdGIZ8Nr0Zc8i2Ro_JxFuysT2ae8=w60-h60-l90-rj", "explicit": true}, {"id": "qOnf2oPUtVE", "title": "PROVÓCATE", "artists": [{"name": "Blessd, Cris Mj y SOG", "id": null}], "thumbnail": "https://yt3.googleusercontent.com/wRE2jz8OVKYjLYbucUlYWAQT439rIMXByizwcjhgnbNyowX25hS52dILdJdbdXiUS7FGk1Ds1ohVY3H8=w60-h60-l90-rj", "explicit": true}, {"id": "7CUz7Ec7cWc", "title": "hate that i made you love me", "artists": [{"name": "Ariana Grande", "id": "UC0076UMUgEng8HORUw_MYHA"}], "thumbnail": "https://yt3.googleusercontent.com/Mq8kh-Qg2QJr9kIjuk25IT2o2Dwyry87xMWt2YV0SOfbjufAu3oZTMigL4LYXx8PbF0WotMBocMPUvSJ=w60-h60-l90-rj", "explicit": false}, {"id": "bTtmUXC8sYU", "title": "Encantadora", "artists": [{"name": "Yandel", "id": "UCc1QpDE0iT0n6ZLckjflNHw"}], "thumbnail": "https://yt3.googleusercontent.com/VHxYqbeMKEiky2hGht-1UL47Ebr7QKn7t_SAzN545QdVkdCwuJz8di15qvK3AfDikNSIkgRk86dcbwd7-Q=w60-h60-l90-rj", "explicit": false}, {"id": "mzhbRUniU4U", "title": "Life Goes On", "artists": [{"name": "Oliver Tree", "id": "UCoOFEu6s0wk7CUljMh8wisw"}], "thumbnail": "https://yt3.googleusercontent.com/2OUpQNjY58jQq1STwHz2CRClwWkzhCCKtSBo6SC2N78LAMmFaXJdSGkUENX7yw0g2CpCQ3RjejB_Fj4=w60-h60-l90-rj", "explicit": false}, {"id": "U1bc5rI4RrE", "title": "UNA BABY EN SANTIAGO", "artists": [{"name": "Lil Naay", "id": "UCu_qmyIu8uT-stU7Mx-kcgw"}], "thumbnail": "https://yt3.googleusercontent.com/3v4LhEiqsXh6dLiLp36qNoJylCkRjaFpUdVUr8RhyCTLhWomidXens5jrUsKXe9YXiB_F6EWberIdpTrFQ=w60-h60-l90-rj", "explicit": true}, {"id": "dkH6bUnsy1Y", "title": "De Lejitos (Remix)", "artists": [{"name": "Jay Wheeler", "id": "UC17u1K8tiqxxFhPMbM50ASA"}, {"name": "y", "id": null}, {"name": "Omar Courtz", "id": "UCECf5Do7fabQCuF3h38sFDQ"}], "thumbnail": "https://yt3.googleusercontent.com/M4gC49M_z88QHhFXnc-Vgnh3pQk7TmJIavtedKqcHxWM7DYa24sCMNE3-C1HMVjT9RRwIVpwWHjN0eHU=w60-h60-l90-rj", "explicit": true}, {"id": "SzJXikN_4wA", "title": "I Knew It, I Knew You (From \"Toy Story 5\")", "artists": [{"name": "Taylor Swift", "id": "UCPC0L1d253x-KuMNwa05TpA"}], "thumbnail": "https://yt3.googleusercontent.com/gugx1oABoi0MrNgzaLtUJib6Xm44OC8aoAYx66zxLM_N1kG6xT_BUH7IO-0eaFAyQzxk43srK4gW7hip=w60-h60-l90-rj", "explicit": false}, {"id": "UKfa__-Ns0I", "title": "Watekeo", "artists": [{"name": "CARIT BR, MAURY, dimelooolexx y AIVAN BEATZ", "id": null}], "thumbnail": "https://yt3.googleusercontent.com/K2eP2mgsQTHXkDwxvFaBKKHbS3G90NFNc5RjLHf1tNun8niKeWjNrcjODAO0XkD5Xdaox-lHo-SlKONf=w60-h60-l90-rj", "explicit": true}, {"id": "ZsWDn821tIY", "title": "Q Hubo Amor (Medellín)", "artists": [{"name": "Ovy On The Drums", "id": "UCtfkGa4ie0BivwCPWCHalOw"}, {"name": "Jere Klein", "id": "UC_MAEhBzOKDyKLhFsVbnAqQ"}, {"name": "y", "id": null}, {"name": "Blessd", "id": "UC3vpYFFzqIzsXbMgneDTSeQ"}], "thumbnail": "https://yt3.googleusercontent.com/JOuAKtu_d1UM96zeIcn63krDznYx1sh2K5bxQVulrOiImXDVnHb7phb6N-MRAxVPf70BL1Gyc962EvBtgA=w60-h60-l90-rj", "explicit": false}, {"id": "wkVKZRtJdcA", "title": "GANAS REMIX", "artists": [{"name": "Kris R.", "id": "UCGTKMCLP6zCiTumnyJzpyAQ"}, {"name": "Ryan Castro", "id": "UCDxiGGtBV3CQiE-NKLEFgUg"}, {"name": "De La Rose", "id": "UC2-PV0lS78r65j_3f5_vPvA"}, {"name": "y", "id": null}, {"name": "Cris Mj", "id": "UCOykHV9q0qb0vrBsxO_5fkQ"}], "thumbnail": "https://yt3.googleusercontent.com/ljiwp_LRnC6DfnVA91onUZoD0V8k0ZyTma5UcOCFmHSzzaAi0167zOEZtx5AJdwZs7m9ImpAzTOEAjB7ow=w60-h60-l90-rj", "explicit": true}], "everyoneListening": [{"id": "T_UEawa2wCk", "title": "u + me = <3", "artists": [{"name": "Olivia Rodrigo", "id": "UCE5XNpliPM-SmyFEp61tL_g"}], "thumbnail": "https://yt3.googleusercontent.com/REA_Otoa5g-YBrDU0lRxSPu5YBP3tZHiIs2DWnO9mqXRZjTWDXyL8Zv83I56-Benty2h5Q0UzX9xBw7g=w60-h60-l90-rj", "explicit": true}, {"id": "5ca_0UYtYXY", "title": "REGRESA Versión Salsa + IA", "artists": [{"name": "GABO CAMPOS -IA PRODUCTIONS", "id": "UCtpWF2Gq56s248XHmbkApXA"}], "thumbnail": "https://yt3.googleusercontent.com/UbxJ3_Ii-IZrWNhc8wttXCw91EDOOLHAEf3cEnSP1IMGken--cRHYzvweiKBSCNDEl2n9EzicE2yviGJ=w60-h60-l90-rj", "explicit": false}, {"id": "jyUUYrJEUcE", "title": "BRILLO", "artists": [{"name": "Tito Double P", "id": "UCATfo9SAdXImyc6ygfbzJBg"}], "thumbnail": "https://yt3.googleusercontent.com/hpCqg7FV_Fyw1ZieuRx-5YAtlFTkCiA0q-9FQOiG-IhERwUolaC-SjJ7i3Bn6aGnAs0y_UI73UpTimt6=w60-h60-l90-rj", "explicit": true}, {"id": "bVav9Z1MqxI", "title": "MENTE POSITIVA", "artists": [{"name": "Blessd", "id": "UC3vpYFFzqIzsXbMgneDTSeQ"}], "thumbnail": "https://yt3.googleusercontent.com/wRE2jz8OVKYjLYbucUlYWAQT439rIMXByizwcjhgnbNyowX25hS52dILdJdbdXiUS7FGk1Ds1ohVY3H8=w60-h60-l90-rj", "explicit": true}, {"id": "lXTU664qsyo", "title": "OK (con Don Toliver)", "artists": [{"name": "Kanye West", "id": "UCRY5dYsbIN5TylSbd7gVnZg"}, {"name": "Ye", "id": "UCWnCIGml93obHBTwlxb2N8g"}, {"name": "y", "id": null}, {"name": "Don Toliver", "id": "UCSzWQmDsKG37iKN2vw1G-2Q"}], "thumbnail": "https://yt3.googleusercontent.com/jNOFpeeRZsiliLBUBCLeKu1UZo67_2Tuqyg2MxfdOAVV0ctETgBkvlKesKx0NGq5oIVHjHUHioBwDTDHgQ=w60-h60-l90-rj", "explicit": true}, {"id": "2WaPoIQLJYs", "title": "POR SI MAÑANA NO ESTOY", "artists": [{"name": "Omar Courtz", "id": "UCieB8sWI6qEOX6r_UnJU12w"}], "thumbnail": "https://yt3.googleusercontent.com/EqBlvzNmm_B2otzdTqYJzhHguR94SVFH96sOWBUlKZ6zJ4CGeXj9YkpcI7MFYEf9DPovp-4RZssESLbv=w60-h60-l90-rj", "explicit": true}, {"id": "vMZRn5bGoSQ", "title": "CÓRCEGA", "artists": [{"name": "Mora", "id": "UCgDpp8Ex8Yz6hhHjbl3W5mg"}, {"name": "y", "id": null}, {"name": "Álvaro Díaz", "id": "UCoHRmfxqEHTHoNTddLS3ErQ"}], "thumbnail": "https://yt3.googleusercontent.com/kODMCeEzi99KVNAlQQCfRlmye5jRb0vaCnPgXmnqpsZn5Nj11R4Qy5HqfA_fMO6AHqi9Siz2ztrgOCFR=w60-h60-l90-rj", "explicit": true}, {"id": "EoAeouOh23k", "title": "Tú_SabeSs", "artists": [{"name": "SAIKO", "id": "UCN7aCXjfOVY-YOj_OWD-hSQ"}, {"name": "Raul Clyde", "id": "UCujg6darOzMZ5iyoI_5hdOw"}, {"name": "Gotay \"El Autentiko\"", "id": "UCZ2GmlVoTqbW0xymZ11Nqcw"}, {"name": "y", "id": null}, {"name": "Jory Boy", "id": "UCPp63ne-_75OVrLrrlWHGng"}], "thumbnail": "https://yt3.googleusercontent.com/qA3-DKk0GXVcp3A5zkTrVSAWGl_taSZtJSlpbfFX931Oakg7RglLvl2pNIHWiu4MlDAAC2QbElZvGlB7=w60-h60-l90-rj", "explicit": true}, {"id": "trY18nXV0fs", "title": "Nunca Voy a Morir", "artists": [{"name": "Omar Camacho", "id": "UC5TYeA9Hw3L5VlZYtMt3pqg"}], "thumbnail": "https://yt3.googleusercontent.com/9F_-c4UktUzhc3W3L7vTU-aluNVE4d6VOmdFlYUgMMR6Fplqen2ooXOzXh8w7HnMuGmLl7pAW2dozTkO=w60-h60-l90-rj", "explicit": true}, {"id": "NBghhjuMNKM", "title": "Moscow Mule", "artists": [{"name": "Bad Bunny", "id": "UCiY3z8HAGD6BlSNKVn2kSvQ"}], "thumbnail": "https://yt3.googleusercontent.com/0Ke36SSY53TQbmpqzKP4hMPoGiWYQiGI0KJ2CzcwjW4ore9vUMUmyzEi7irEU2MR3FCpNPdwIIuNslS6hg=w60-h60-l90-rj", "explicit": true}, {"id": "ObBfqHvlKQM", "title": "Mi Vida (Mix 2024)", "artists": [{"name": "José José", "id": "UCiMcavnI3L8L-ayXZtrrwgA"}], "thumbnail": "https://yt3.googleusercontent.com/KPzjkEmjr2LrfOWsXOxLtcV7V0bN0wmu_nrcKBTPVG0gOImirxBV5un9rJmT0ofVfyMIYeeYeu8WoUSz=w60-h60-l90-rj", "explicit": false}, {"id": "QycL5-xST4U", "title": "Euforia", "artists": [{"name": "Kidd Voodoo", "id": "UCY1ficxbS1vAreE9j1Q58gw"}], "thumbnail": "https://yt3.googleusercontent.com/a_VTOygRjAUKlF635aDFDJddmDMNDpL13htCzZMogJkWmO01_c-SAzZFqPJzvuT2twbdoYr8O6Zi-FSZqA=w60-h60-l90-rj", "explicit": false}, {"id": "hlA-HLmEqfM", "title": "The Way You Make Me Feel (Single Version)", "artists": [{"name": "Michael Jackson", "id": "UCoIOOL7QKuBhQHVKL8y7BEQ"}], "thumbnail": "https://yt3.googleusercontent.com/N3dO2K1MIFWJtEqZNpQ4yfISUC3gDaIIZNYCs6kwcX5G46AHtK6W_fxfHeQ30sdBdRgyyqXkbbqzPuI=w60-h60-l90-rj", "explicit": false}, {"id": "DG8_5fbd0jM", "title": "Mix Salsa Clásica: Idilio / Fuego en el 23 / Me Liberé / Mi Gente / Yo Quisiera / Lluvia / Timbalero (En Vivo)", "artists": [{"name": "Septeto Acarey", "id": "UCndXvQeT7yyq9R-x3pSaybQ"}], "thumbnail": "https://yt3.googleusercontent.com/PsMSDj_QG19aJFctbvTisZA6f7rA5yZJ7v2-3ViQAkPVKmJWslWMj0nRToP4VTZqll0cmU0RugxWwON4=w60-h60-l90-rj", "explicit": false}, {"id": "5MPE_7-HR3k", "title": "La Voz Favorita", "artists": [{"name": "Jay Wheeler", "id": "UC17u1K8tiqxxFhPMbM50ASA"}], "thumbnail": "https://yt3.googleusercontent.com/M4gC49M_z88QHhFXnc-Vgnh3pQk7TmJIavtedKqcHxWM7DYa24sCMNE3-C1HMVjT9RRwIVpwWHjN0eHU=w60-h60-l90-rj", "explicit": true}, {"id": "SmGCO13YiUo", "title": "PIENXA EN MI", "artists": [{"name": "Feid", "id": "UCc3e8O2V5_7OA300ursDyFQ"}, {"name": "y", "id": null}, {"name": "Sfera Ebbasta", "id": "UC7-K3cpctiu-1pUllHQqkvg"}], "thumbnail": "https://yt3.googleusercontent.com/HhIpbEdqcky0pILaQT-oynh8aiaONrcGbbgLH-JSfHNUkPRG1S4T7-KRCokmu1AVXmmreCSNvg9eUf4=w60-h60-l90-rj", "explicit": true}, {"id": "DOBIQfQ_Y2A", "title": "Femme Fatale", "artists": [{"name": "Mon Laferte", "id": "UCxOBODdj5wEIZF74kPma7Gw"}], "thumbnail": "https://yt3.googleusercontent.com/dJSz5X2s4ROujAQHw8p9jnLWbhJdMgw-DqHArRN6iYmEhG5p3GlRfZWBbnLNWrFx5fCT6NbHgrTZG5M3=w60-h60-l90-rj", "explicit": false}, {"id": "u32rPttWrnY", "title": "Enganchado Rock Nacional Argentino #1", "artists": [{"name": "Nico Vallorani DJ", "id": "UCu7N_HCrA2Lrj14iEQOtnMA"}], "thumbnail": "https://yt3.googleusercontent.com/nwxgy2CHmBA0RfH60_ftSMk-PAEAYvRIcrc_v7qnQZ3Y21ZpucvwdZ1xC2FNfvtmeQi3DPwLVpKwI4Ry=w60-h60-l90-rj", "explicit": false}, {"id": "kZJHob65Hbw", "title": "Omerta", "artists": [{"name": "J Balvin", "id": "UCWw-Guyr5ul9B-d5kJlHMng"}, {"name": "Ryan Castro", "id": "UCDxiGGtBV3CQiE-NKLEFgUg"}, {"name": "y", "id": null}, {"name": "SOG", "id": "UCC4ySHYLOmudD7sreG5KWTQ"}], "thumbnail": "https://yt3.googleusercontent.com/DWiey1fJDx3ed0p2VKxZ99xj_65J9JhOV0OcyieH7bGdppuHP1eRb6eYMRa2gxwsJQEqlhVvWJwxaXyA=w60-h60-l90-rj", "explicit": true}, {"id": "C0U1d2ecob4", "title": "Entre dos tierras", "artists": [{"name": "Héroes del Silencio", "id": "UCR85xzwE1PevoGId8HOnErw"}], "thumbnail": "https://yt3.googleusercontent.com/nwqYy3LkBFN2VJCrg_M0Ig1v-MipkiTyepkG9pHIp-f7UTlNqsWFjzEgOXrOoIRwnqzouUEA9ygJozA=w60-h60-l90-rj", "explicit": false}]};


async function loadExploreFeed() {
  setHeaderVisible(true);
  setHeaderSearchPillVisible(false);
  contentArea.innerHTML = `<div class="loading-spinner-container"><div class="spinner"></div><p>Cargando Novedades...</p></div>`;
  
  try {
    contentArea.innerHTML = `
      <div style="padding: 24px 36px 10px 36px; width: 100%; box-sizing: border-box;">
        <h1 style="font-size: 34px; font-weight: 900; letter-spacing: -0.02em; margin-bottom: 24px; color: white;">Novedades</h1>
      </div>
    `;

    // 1. Primer Carrusel (Carrusel Héroe Ancho)
    const featuredBanners = (NOVEDADES_APPLE_DATA.featuredAlbums || []).map(item => ({
      id: item.playlistId || item.browseId,
      title: item.title,
      artist: item.artists?.[0]?.name || 'Artista',
      artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
      type: 'album'
    }));
    renderHeroLandscapeCarousel("Lanzamientos Destacados", featuredBanners);

    // 2. Canciones nuevas destacadas (4 canciones hacia abajo, carrusel hacia la derecha con botones < y >)
    const featuredSongs = (NOVEDADES_APPLE_DATA.featuredNewSongs || []).map(item => ({
      id: item.id,
      title: item.title,
      artist: (item.artists || []).map(a => a.name).filter(n => n !== 'y').join(', ') || 'Artista',
      artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
      explicit: item.explicit,
      type: 'song'
    }));
    renderApple4RowSongGrid("Canciones nuevas destacadas", featuredSongs);

    // 3. Novedades (2 canciones hacia abajo y hartas hacia la derecha)
    const newReleases = (NOVEDADES_APPLE_DATA.newReleaseAlbums || []).map(item => ({
      id: item.id || item.playlistId || item.browseId,
      title: item.title,
      artist: (item.artists || []).map(a => a.name).filter(n => n !== 'y').join(', ') || 'Artista',
      artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
      explicit: item.explicit,
      type: 'song'
    }));
    renderApple2RowSongGrid("Novedades", newReleases);

    // 4. Las canciones del momento (4 canciones hacia abajo y hacia el lado con botones < y >)
    const trendingSongs = (NOVEDADES_APPLE_DATA.trendingSongs || []).map(item => ({
      id: item.id,
      title: item.title,
      artist: (item.artists || []).map(a => a.name).filter(n => n !== 'y').join(', ') || 'Artista',
      artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
      explicit: item.explicit,
      type: 'song'
    }));
    renderApple4RowSongGrid("Las canciones del momento", trendingSongs);

    // 5. Toda la gente está escuchando (carrusel hacia el lado solamente)
    const everyoneListening = (NOVEDADES_APPLE_DATA.everyoneListening || []).map(item => ({
      id: item.id || item.playlistId || item.browseId,
      title: item.title,
      artist: item.artists?.[0]?.name || 'Artista',
      artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
      type: 'song'
    }));
    renderCarouselSection("Toda la gente está escuchando", everyoneListening);

  } catch (err) {
    console.warn("Explore feed error:", err);
    renderExploreOffline();
  }
}

function renderApple2RowSongGrid(title, items) {
  const section = document.createElement('section');
  section.className = "content-section";
  section.style.marginBottom = "36px";
  section.style.padding = "0 32px";

  const sectionHeader = document.createElement('div');
  sectionHeader.className = "section-header";
  sectionHeader.innerHTML = `
    <h2 class="section-title-sub" style="font-size: 22px; font-weight: 800; margin-bottom: 16px; cursor: pointer;">${escapeHtmlAttr(title)} &gt;</h2>
  `;
  section.appendChild(sectionHeader);

  const wrapper = document.createElement('div');
  wrapper.className = "carousel-wrapper";

  const btnPrev = document.createElement('button');
  btnPrev.className = "carousel-float-arrow float-prev hidden-arrow";
  btnPrev.title = "Anterior";
  btnPrev.innerHTML = `<svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/></svg>`;

  const btnNext = document.createElement('button');
  btnNext.className = "carousel-float-arrow float-next";
  btnNext.title = "Siguiente";
  btnNext.innerHTML = `<svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/></svg>`;

  const columnsContainer = document.createElement('div');
  columnsContainer.style.display = "flex";
  columnsContainer.style.gap = "18px";
  columnsContainer.style.overflowX = "auto";
  columnsContainer.style.scrollBehavior = "smooth";
  columnsContainer.style.paddingBottom = "10px";
  columnsContainer.style.scrollbarWidth = "none";

  btnPrev.onclick = () => columnsContainer.scrollBy({ left: -360, behavior: 'smooth' });
  btnNext.onclick = () => columnsContainer.scrollBy({ left: 360, behavior: 'smooth' });

  bindCarouselPillArrows(columnsContainer, btnPrev, btnNext);

  const columnSize = 2; // 2 canciones hacia abajo!
  for (let i = 0; i < items.length; i += columnSize) {
    const chunk = items.slice(i, i + columnSize);
    const col = document.createElement('div');
    col.style.flex = "0 0 340px";
    col.style.width = "340px";
    col.style.display = "flex";
    col.style.flexDirection = "column";
    col.style.gap = "8px";

    chunk.forEach(item => {
      const row = document.createElement('div');
      row.className = "song-row";
      row.style.display = "flex";
      row.style.alignItems = "center";
      row.style.padding = "8px 12px";
      row.style.borderRadius = "10px";
      row.style.cursor = "pointer";
      row.style.background = "rgba(255,255,255,0.03)";
      row.style.border = "1px solid rgba(255,255,255,0.05)";

      row.innerHTML = `
        <img src="${item.artwork}" class="song-thumbnail" style="width: 48px; height: 48px; border-radius: 8px; object-fit: cover; margin-right: 12px; flex-shrink: 0;" />
        <div class="song-info" style="display: flex; flex-direction: column; flex-grow: 1; overflow: hidden;">
          <span class="song-title" style="font-size: 13.5px; font-weight: 700; color: white; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
            ${escapeHtmlAttr(item.title)} ${item.explicit ? `<span style="font-size: 10px; border: 1px solid rgba(255,255,255,0.4); border-radius: 3px; padding: 1px 4px; font-weight: 800; opacity: 0.7; margin-left: 4px;">E</span>` : ''}
          </span>
          <span class="song-artist" style="font-size: 11.5px; color: var(--text-secondary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${escapeHtmlAttr(item.artist || '')}</span>
        </div>
        <button class="btn-ctx-dots-item" style="background: none; border: none; color: var(--text-muted); cursor: pointer; padding: 6px; font-size: 14px; font-weight: 900;" title="Opciones">•••</button>
      `;

      row.onclick = () => {
        if (item.type === 'song' || item.id) {
          playTrackDetails(item.id, item.title, item.artist, item.artwork, item.artistId, item.durationSec || 0);
        } else {
          loadPlaylistContents(item.id, item.title);
        }
      };

      const btnDots = row.querySelector('.btn-ctx-dots-item');
      if (btnDots) {
        btnDots.onclick = (e) => {
          showContextMenu(e, item, 'song');
        };
      }

      col.appendChild(row);
    });
    columnsContainer.appendChild(col);
  }

  wrapper.appendChild(btnPrev);
  wrapper.appendChild(columnsContainer);
  wrapper.appendChild(btnNext);
  section.appendChild(wrapper);
  contentArea.appendChild(section);
}










function renderHomeOffline() {
  contentArea.innerHTML = '';
  const demoCards = FALLBACK_TRACKS.map(t => ({
    id: t.id,
    type: 'song',
    title: t.title,
    artist: t.artist,
    artwork: t.artwork,
    artistId: t.artistId
  }));
  renderCarouselSection("Selecciones destacadas (Offline)", demoCards);
}

// Setup interactive floating arrow visibility based on scroll position
function bindCarouselPillArrows(container, btnPrev, btnNext) {
  function updateArrowVisibility() {
    const scrollLeft = container.scrollLeft;
    const maxScroll = container.scrollWidth - container.clientWidth;

    if (scrollLeft <= 5) {
      btnPrev.classList.add('hidden-arrow');
    } else {
      btnPrev.classList.remove('hidden-arrow');
    }

    if (scrollLeft >= maxScroll - 5) {
      btnNext.classList.add('hidden-arrow');
    } else {
      btnNext.classList.remove('hidden-arrow');
    }
  }

  container.addEventListener('scroll', updateArrowVisibility);
  window.addEventListener('resize', updateArrowVisibility);
  setTimeout(updateArrowVisibility, 100);
}

// --- Explore/Novedades Feed (Apple Music Full Parity via InnerTube API) ---
async function loadExploreFeed() {
  contentArea.innerHTML = `<div class="loading-spinner-container"><div class="spinner"></div><p>Cargando Novedades...</p></div>`;
  
  try {
    contentArea.innerHTML = `
      <div style="padding: 24px 36px 10px 36px; width: 100%; box-sizing: border-box;">
        <h1 style="font-size: 34px; font-weight: 900; letter-spacing: -0.02em; margin-bottom: 24px; color: white;">Novedades</h1>
      </div>
    `;

    // 1. Primer Carrusel (Carrusel Héroe Ancho)
    const featuredBanners = (NOVEDADES_APPLE_DATA.featuredAlbums || []).map(item => ({
      id: item.playlistId || item.browseId,
      title: item.title,
      artist: item.artists?.[0]?.name || 'Artista',
      artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
      type: 'album'
    }));
    renderHeroLandscapeCarousel("Lanzamientos Destacados", featuredBanners);

    // 2. Canciones nuevas destacadas (4 canciones hacia abajo, carrusel hacia la derecha con botones < y >)
    const featuredSongs = (NOVEDADES_APPLE_DATA.featuredNewSongs || []).map(item => ({
      id: item.id,
      title: item.title,
      artist: (item.artists || []).map(a => a.name).filter(n => n !== 'y').join(', ') || 'Artista',
      artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
      explicit: item.explicit,
      type: 'song'
    }));
    renderApple4RowSongGrid("Canciones nuevas destacadas", featuredSongs);

    // 3. Novedades (2 canciones hacia abajo y hartas hacia la derecha)
    const newReleases = (NOVEDADES_APPLE_DATA.newReleaseAlbums || []).map(item => ({
      id: item.id || item.playlistId || item.browseId,
      title: item.title,
      artist: (item.artists || []).map(a => a.name).filter(n => n !== 'y').join(', ') || 'Artista',
      artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
      explicit: item.explicit,
      type: 'song'
    }));
    renderApple2RowSongGrid("Novedades", newReleases);

    // 4. Las canciones del momento (4 canciones hacia abajo y hacia el lado con botones < y >)
    const trendingSongs = (NOVEDADES_APPLE_DATA.trendingSongs || []).map(item => ({
      id: item.id,
      title: item.title,
      artist: (item.artists || []).map(a => a.name).filter(n => n !== 'y').join(', ') || 'Artista',
      artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
      explicit: item.explicit,
      type: 'song'
    }));
    renderApple4RowSongGrid("Las canciones del momento", trendingSongs);

    // 5. Toda la gente está escuchando (carrusel hacia el lado solamente)
    const everyoneListening = (NOVEDADES_APPLE_DATA.everyoneListening || []).map(item => ({
      id: item.id || item.playlistId || item.browseId,
      title: item.title,
      artist: item.artists?.[0]?.name || 'Artista',
      artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
      type: 'song'
    }));
    renderCarouselSection("Toda la gente está escuchando", everyoneListening);

  } catch (err) {
    console.warn("Explore feed error:", err);
    renderExploreOffline();
  }
}

function renderApple2RowSongGrid(title, items) {
  const section = document.createElement('section');
  section.className = "content-section";
  section.style.marginBottom = "36px";
  section.style.padding = "0 32px";

  const sectionHeader = document.createElement('div');
  sectionHeader.className = "section-header";
  sectionHeader.innerHTML = `
    <h2 class="section-title-sub" style="font-size: 22px; font-weight: 800; margin-bottom: 16px; cursor: pointer;">${escapeHtmlAttr(title)} &gt;</h2>
  `;
  section.appendChild(sectionHeader);

  const wrapper = document.createElement('div');
  wrapper.className = "carousel-wrapper";

  const btnPrev = document.createElement('button');
  btnPrev.className = "carousel-float-arrow float-prev hidden-arrow";
  btnPrev.title = "Anterior";
  btnPrev.innerHTML = `<svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/></svg>`;

  const btnNext = document.createElement('button');
  btnNext.className = "carousel-float-arrow float-next";
  btnNext.title = "Siguiente";
  btnNext.innerHTML = `<svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/></svg>`;

  const columnsContainer = document.createElement('div');
  columnsContainer.style.display = "flex";
  columnsContainer.style.gap = "18px";
  columnsContainer.style.overflowX = "auto";
  columnsContainer.style.scrollBehavior = "smooth";
  columnsContainer.style.paddingBottom = "10px";
  columnsContainer.style.scrollbarWidth = "none";

  btnPrev.onclick = () => columnsContainer.scrollBy({ left: -360, behavior: 'smooth' });
  btnNext.onclick = () => columnsContainer.scrollBy({ left: 360, behavior: 'smooth' });

  bindCarouselPillArrows(columnsContainer, btnPrev, btnNext);

  const columnSize = 2; // 2 canciones hacia abajo!
  for (let i = 0; i < items.length; i += columnSize) {
    const chunk = items.slice(i, i + columnSize);
    const col = document.createElement('div');
    col.style.flex = "0 0 340px";
    col.style.width = "340px";
    col.style.display = "flex";
    col.style.flexDirection = "column";
    col.style.gap = "8px";

    chunk.forEach(item => {
      const row = document.createElement('div');
      row.className = "song-row";
      row.style.display = "flex";
      row.style.alignItems = "center";
      row.style.padding = "8px 12px";
      row.style.borderRadius = "10px";
      row.style.cursor = "pointer";
      row.style.background = "rgba(255,255,255,0.03)";
      row.style.border = "1px solid rgba(255,255,255,0.05)";

      row.innerHTML = `
        <img src="${item.artwork}" class="song-thumbnail" style="width: 48px; height: 48px; border-radius: 8px; object-fit: cover; margin-right: 12px; flex-shrink: 0;" />
        <div class="song-info" style="display: flex; flex-direction: column; flex-grow: 1; overflow: hidden;">
          <span class="song-title" style="font-size: 13.5px; font-weight: 700; color: white; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
            ${escapeHtmlAttr(item.title)} ${item.explicit ? `<span style="font-size: 10px; border: 1px solid rgba(255,255,255,0.4); border-radius: 3px; padding: 1px 4px; font-weight: 800; opacity: 0.7; margin-left: 4px;">E</span>` : ''}
          </span>
          <span class="song-artist" style="font-size: 11.5px; color: var(--text-secondary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${escapeHtmlAttr(item.artist || '')}</span>
        </div>
        <button class="btn-ctx-dots-item" style="background: none; border: none; color: var(--text-muted); cursor: pointer; padding: 6px; font-size: 14px; font-weight: 900;" title="Opciones">•••</button>
      `;

      row.onclick = () => {
        if (item.type === 'song' || item.id) {
          playTrackDetails(item.id, item.title, item.artist, item.artwork, item.artistId, item.durationSec || 0);
        } else {
          loadPlaylistContents(item.id, item.title);
        }
      };

      const btnDots = row.querySelector('.btn-ctx-dots-item');
      if (btnDots) {
        btnDots.onclick = (e) => {
          showContextMenu(e, item, 'song');
        };
      }

      col.appendChild(row);
    });
    columnsContainer.appendChild(col);
  }

  wrapper.appendChild(btnPrev);
  wrapper.appendChild(columnsContainer);
  wrapper.appendChild(btnNext);
  section.appendChild(wrapper);
  contentArea.appendChild(section);
}









function renderHomeOffline() {
  contentArea.innerHTML = '';
  const demoCards = FALLBACK_TRACKS.map(t => ({
    id: t.id,
    type: 'song',
    title: t.title,
    artist: t.artist,
    artwork: t.artwork,
    artistId: t.artistId
  }));
  renderCarouselSection("Selecciones destacadas (Offline)", demoCards);
}

// Setup interactive floating arrow visibility based on scroll position
function bindCarouselPillArrows(container, btnPrev, btnNext) {
  function updateArrowVisibility() {
    const scrollLeft = container.scrollLeft;
    const maxScroll = container.scrollWidth - container.clientWidth;

    if (scrollLeft <= 5) {
      btnPrev.classList.add('hidden-arrow');
    } else {
      btnPrev.classList.remove('hidden-arrow');
    }

    if (scrollLeft >= maxScroll - 5) {
      btnNext.classList.add('hidden-arrow');
    } else {
      btnNext.classList.remove('hidden-arrow');
    }
  }

  container.addEventListener('scroll', updateArrowVisibility);
  window.addEventListener('resize', updateArrowVisibility);
  setTimeout(updateArrowVisibility, 100);
}

// --- Explore/Novedades Feed (Apple Music Full Parity via InnerTube API) ---
async function loadExploreFeed() {
  contentArea.innerHTML = `<div class="loading-spinner-container"><div class="spinner"></div><p>Cargando Novedades...</p></div>`;
  
  try {
    contentArea.innerHTML = `
      <div style="padding: 24px 36px 10px 36px; width: 100%; box-sizing: border-box;">
        <h1 style="font-size: 34px; font-weight: 900; letter-spacing: -0.02em; margin-bottom: 24px; color: white;">Novedades</h1>
      </div>
    `;

    // 1. Primer Carrusel (Carrusel Héroe Ancho)
    const featuredBanners = (NOVEDADES_APPLE_DATA.featuredAlbums || []).map(item => ({
      id: item.playlistId || item.browseId,
      title: item.title,
      artist: item.artists?.[0]?.name || 'Artista',
      artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
      type: 'album'
    }));
    renderHeroLandscapeCarousel("Lanzamientos Destacados", featuredBanners);

    // 2. Canciones nuevas destacadas (4 canciones hacia abajo, carrusel hacia la derecha con botones < y >)
    const featuredSongs = (NOVEDADES_APPLE_DATA.featuredNewSongs || []).map(item => ({
      id: item.id,
      title: item.title,
      artist: (item.artists || []).map(a => a.name).filter(n => n !== 'y').join(', ') || 'Artista',
      artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
      explicit: item.explicit,
      type: 'song'
    }));
    renderApple4RowSongGrid("Canciones nuevas destacadas", featuredSongs);

    // 3. Novedades (2 canciones hacia abajo y hartas hacia la derecha)
    const newReleases = (NOVEDADES_APPLE_DATA.newReleaseAlbums || []).map(item => ({
      id: item.id || item.playlistId || item.browseId,
      title: item.title,
      artist: (item.artists || []).map(a => a.name).filter(n => n !== 'y').join(', ') || 'Artista',
      artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
      explicit: item.explicit,
      type: 'song'
    }));
    renderApple2RowSongGrid("Novedades", newReleases);

    // 4. Las canciones del momento (4 canciones hacia abajo y hacia el lado con botones < y >)
    const trendingSongs = (NOVEDADES_APPLE_DATA.trendingSongs || []).map(item => ({
      id: item.id,
      title: item.title,
      artist: (item.artists || []).map(a => a.name).filter(n => n !== 'y').join(', ') || 'Artista',
      artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
      explicit: item.explicit,
      type: 'song'
    }));
    renderApple4RowSongGrid("Las canciones del momento", trendingSongs);

    // 5. Toda la gente está escuchando (carrusel hacia el lado solamente)
    const everyoneListening = (NOVEDADES_APPLE_DATA.everyoneListening || []).map(item => ({
      id: item.id || item.playlistId || item.browseId,
      title: item.title,
      artist: item.artists?.[0]?.name || 'Artista',
      artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
      type: 'song'
    }));
    renderCarouselSection("Toda la gente está escuchando", everyoneListening);

  } catch (err) {
    console.warn("Explore feed error:", err);
    renderExploreOffline();
  }
}

function renderApple2RowSongGrid(title, items) {
  const section = document.createElement('section');
  section.className = "content-section";
  section.style.marginBottom = "36px";
  section.style.padding = "0 32px";

  const sectionHeader = document.createElement('div');
  sectionHeader.className = "section-header";
  sectionHeader.innerHTML = `
    <h2 class="section-title-sub" style="font-size: 22px; font-weight: 800; margin-bottom: 16px; cursor: pointer;">${escapeHtmlAttr(title)} &gt;</h2>
  `;
  section.appendChild(sectionHeader);

  const wrapper = document.createElement('div');
  wrapper.className = "carousel-wrapper";

  const btnPrev = document.createElement('button');
  btnPrev.className = "carousel-float-arrow float-prev hidden-arrow";
  btnPrev.title = "Anterior";
  btnPrev.innerHTML = `<svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/></svg>`;

  const btnNext = document.createElement('button');
  btnNext.className = "carousel-float-arrow float-next";
  btnNext.title = "Siguiente";
  btnNext.innerHTML = `<svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/></svg>`;

  const columnsContainer = document.createElement('div');
  columnsContainer.style.display = "flex";
  columnsContainer.style.gap = "18px";
  columnsContainer.style.overflowX = "auto";
  columnsContainer.style.scrollBehavior = "smooth";
  columnsContainer.style.paddingBottom = "10px";
  columnsContainer.style.scrollbarWidth = "none";

  btnPrev.onclick = () => columnsContainer.scrollBy({ left: -360, behavior: 'smooth' });
  btnNext.onclick = () => columnsContainer.scrollBy({ left: 360, behavior: 'smooth' });

  bindCarouselPillArrows(columnsContainer, btnPrev, btnNext);

  const columnSize = 2; // 2 canciones hacia abajo!
  for (let i = 0; i < items.length; i += columnSize) {
    const chunk = items.slice(i, i + columnSize);
    const col = document.createElement('div');
    col.style.flex = "0 0 340px";
    col.style.width = "340px";
    col.style.display = "flex";
    col.style.flexDirection = "column";
    col.style.gap = "8px";

    chunk.forEach(item => {
      const row = document.createElement('div');
      row.className = "song-row";
      row.style.display = "flex";
      row.style.alignItems = "center";
      row.style.padding = "8px 12px";
      row.style.borderRadius = "10px";
      row.style.cursor = "pointer";
      row.style.background = "rgba(255,255,255,0.03)";
      row.style.border = "1px solid rgba(255,255,255,0.05)";

      row.innerHTML = `
        <img src="${item.artwork}" class="song-thumbnail" style="width: 48px; height: 48px; border-radius: 8px; object-fit: cover; margin-right: 12px; flex-shrink: 0;" />
        <div class="song-info" style="display: flex; flex-direction: column; flex-grow: 1; overflow: hidden;">
          <span class="song-title" style="font-size: 13.5px; font-weight: 700; color: white; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
            ${escapeHtmlAttr(item.title)} ${item.explicit ? `<span style="font-size: 10px; border: 1px solid rgba(255,255,255,0.4); border-radius: 3px; padding: 1px 4px; font-weight: 800; opacity: 0.7; margin-left: 4px;">E</span>` : ''}
          </span>
          <span class="song-artist" style="font-size: 11.5px; color: var(--text-secondary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${escapeHtmlAttr(item.artist || '')}</span>
        </div>
        <button class="btn-ctx-dots-item" style="background: none; border: none; color: var(--text-muted); cursor: pointer; padding: 6px; font-size: 14px; font-weight: 900;" title="Opciones">•••</button>
      `;

      row.onclick = () => {
        if (item.type === 'song' || item.id) {
          playTrackDetails(item.id, item.title, item.artist, item.artwork, item.artistId, item.durationSec || 0);
        } else {
          loadPlaylistContents(item.id, item.title);
        }
      };

      const btnDots = row.querySelector('.btn-ctx-dots-item');
      if (btnDots) {
        btnDots.onclick = (e) => {
          showContextMenu(e, item, 'song');
        };
      }

      col.appendChild(row);
    });
    columnsContainer.appendChild(col);
  }

  wrapper.appendChild(btnPrev);
  wrapper.appendChild(columnsContainer);
  wrapper.appendChild(btnNext);
  section.appendChild(wrapper);
  contentArea.appendChild(section);
}








function renderHomeOffline() {
  contentArea.innerHTML = '';
  const demoCards = FALLBACK_TRACKS.map(t => ({
    id: t.id,
    type: 'song',
    title: t.title,
    artist: t.artist,
    artwork: t.artwork,
    artistId: t.artistId
  }));
  renderCarouselSection("Selecciones destacadas (Offline)", demoCards);
}

// Setup interactive floating arrow visibility based on scroll position
function bindCarouselPillArrows(container, btnPrev, btnNext) {
  function updateArrowVisibility() {
    const scrollLeft = container.scrollLeft;
    const maxScroll = container.scrollWidth - container.clientWidth;

    if (scrollLeft <= 5) {
      btnPrev.classList.add('hidden-arrow');
    } else {
      btnPrev.classList.remove('hidden-arrow');
    }

    if (scrollLeft >= maxScroll - 5) {
      btnNext.classList.add('hidden-arrow');
    } else {
      btnNext.classList.remove('hidden-arrow');
    }
  }

  container.addEventListener('scroll', updateArrowVisibility);
  window.addEventListener('resize', updateArrowVisibility);
  setTimeout(updateArrowVisibility, 100);
}

// --- Explore/Novedades Feed (Apple Music Full Parity via InnerTube API) ---
async function loadExploreFeed() {
  contentArea.innerHTML = `<div class="loading-spinner-container"><div class="spinner"></div><p>Cargando Novedades...</p></div>`;
  
  try {
    contentArea.innerHTML = `
      <div style="padding: 24px 36px 10px 36px; width: 100%; box-sizing: border-box;">
        <h1 style="font-size: 34px; font-weight: 900; letter-spacing: -0.02em; margin-bottom: 24px; color: white;">Novedades</h1>
      </div>
    `;

    // 1. Primer Carrusel (Carrusel Héroe Ancho)
    const featuredBanners = (NOVEDADES_APPLE_DATA.featuredAlbums || []).map(item => ({
      id: item.playlistId || item.browseId,
      title: item.title,
      artist: item.artists?.[0]?.name || 'Artista',
      artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
      type: 'album'
    }));
    renderHeroLandscapeCarousel("Lanzamientos Destacados", featuredBanners);

    // 2. Canciones nuevas destacadas (4 canciones hacia abajo, carrusel hacia la derecha con botones < y >)
    const featuredSongs = (NOVEDADES_APPLE_DATA.featuredNewSongs || []).map(item => ({
      id: item.id,
      title: item.title,
      artist: (item.artists || []).map(a => a.name).filter(n => n !== 'y').join(', ') || 'Artista',
      artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
      explicit: item.explicit,
      type: 'song'
    }));
    renderApple4RowSongGrid("Canciones nuevas destacadas", featuredSongs);

    // 3. Novedades (2 canciones hacia abajo y hartas hacia la derecha)
    const newReleases = (NOVEDADES_APPLE_DATA.newReleaseAlbums || []).map(item => ({
      id: item.id || item.playlistId || item.browseId,
      title: item.title,
      artist: (item.artists || []).map(a => a.name).filter(n => n !== 'y').join(', ') || 'Artista',
      artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
      explicit: item.explicit,
      type: 'song'
    }));
    renderApple2RowSongGrid("Novedades", newReleases);

    // 4. Las canciones del momento (4 canciones hacia abajo y hacia el lado con botones < y >)
    const trendingSongs = (NOVEDADES_APPLE_DATA.trendingSongs || []).map(item => ({
      id: item.id,
      title: item.title,
      artist: (item.artists || []).map(a => a.name).filter(n => n !== 'y').join(', ') || 'Artista',
      artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
      explicit: item.explicit,
      type: 'song'
    }));
    renderApple4RowSongGrid("Las canciones del momento", trendingSongs);

    // 5. Toda la gente está escuchando (carrusel hacia el lado solamente)
    const everyoneListening = (NOVEDADES_APPLE_DATA.everyoneListening || []).map(item => ({
      id: item.id || item.playlistId || item.browseId,
      title: item.title,
      artist: item.artists?.[0]?.name || 'Artista',
      artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
      type: 'song'
    }));
    renderCarouselSection("Toda la gente está escuchando", everyoneListening);

  } catch (err) {
    console.warn("Explore feed error:", err);
    renderExploreOffline();
  }
}

function renderApple2RowSongGrid(title, items) {
  const section = document.createElement('section');
  section.className = "content-section";
  section.style.marginBottom = "36px";
  section.style.padding = "0 32px";

  const sectionHeader = document.createElement('div');
  sectionHeader.className = "section-header";
  sectionHeader.innerHTML = `
    <h2 class="section-title-sub" style="font-size: 22px; font-weight: 800; margin-bottom: 16px; cursor: pointer;">${escapeHtmlAttr(title)} &gt;</h2>
  `;
  section.appendChild(sectionHeader);

  const wrapper = document.createElement('div');
  wrapper.className = "carousel-wrapper";

  const btnPrev = document.createElement('button');
  btnPrev.className = "carousel-float-arrow float-prev hidden-arrow";
  btnPrev.title = "Anterior";
  btnPrev.innerHTML = `<svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/></svg>`;

  const btnNext = document.createElement('button');
  btnNext.className = "carousel-float-arrow float-next";
  btnNext.title = "Siguiente";
  btnNext.innerHTML = `<svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/></svg>`;

  const columnsContainer = document.createElement('div');
  columnsContainer.style.display = "flex";
  columnsContainer.style.gap = "18px";
  columnsContainer.style.overflowX = "auto";
  columnsContainer.style.scrollBehavior = "smooth";
  columnsContainer.style.paddingBottom = "10px";
  columnsContainer.style.scrollbarWidth = "none";

  btnPrev.onclick = () => columnsContainer.scrollBy({ left: -360, behavior: 'smooth' });
  btnNext.onclick = () => columnsContainer.scrollBy({ left: 360, behavior: 'smooth' });

  bindCarouselPillArrows(columnsContainer, btnPrev, btnNext);

  const columnSize = 2; // 2 canciones hacia abajo!
  for (let i = 0; i < items.length; i += columnSize) {
    const chunk = items.slice(i, i + columnSize);
    const col = document.createElement('div');
    col.style.flex = "0 0 340px";
    col.style.width = "340px";
    col.style.display = "flex";
    col.style.flexDirection = "column";
    col.style.gap = "8px";

    chunk.forEach(item => {
      const row = document.createElement('div');
      row.className = "song-row";
      row.style.display = "flex";
      row.style.alignItems = "center";
      row.style.padding = "8px 12px";
      row.style.borderRadius = "10px";
      row.style.cursor = "pointer";
      row.style.background = "rgba(255,255,255,0.03)";
      row.style.border = "1px solid rgba(255,255,255,0.05)";

      row.innerHTML = `
        <img src="${item.artwork}" class="song-thumbnail" style="width: 48px; height: 48px; border-radius: 8px; object-fit: cover; margin-right: 12px; flex-shrink: 0;" />
        <div class="song-info" style="display: flex; flex-direction: column; flex-grow: 1; overflow: hidden;">
          <span class="song-title" style="font-size: 13.5px; font-weight: 700; color: white; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
            ${escapeHtmlAttr(item.title)} ${item.explicit ? `<span style="font-size: 10px; border: 1px solid rgba(255,255,255,0.4); border-radius: 3px; padding: 1px 4px; font-weight: 800; opacity: 0.7; margin-left: 4px;">E</span>` : ''}
          </span>
          <span class="song-artist" style="font-size: 11.5px; color: var(--text-secondary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${escapeHtmlAttr(item.artist || '')}</span>
        </div>
        <button class="btn-ctx-dots-item" style="background: none; border: none; color: var(--text-muted); cursor: pointer; padding: 6px; font-size: 14px; font-weight: 900;" title="Opciones">•••</button>
      `;

      row.onclick = () => {
        if (item.type === 'song' || item.id) {
          playTrackDetails(item.id, item.title, item.artist, item.artwork, item.artistId, item.durationSec || 0);
        } else {
          loadPlaylistContents(item.id, item.title);
        }
      };

      const btnDots = row.querySelector('.btn-ctx-dots-item');
      if (btnDots) {
        btnDots.onclick = (e) => {
          showContextMenu(e, item, 'song');
        };
      }

      col.appendChild(row);
    });
    columnsContainer.appendChild(col);
  }

  wrapper.appendChild(btnPrev);
  wrapper.appendChild(columnsContainer);
  wrapper.appendChild(btnNext);
  section.appendChild(wrapper);
  contentArea.appendChild(section);
}


function renderHeroLandscapeCarousel(title, cards) {
  const section = document.createElement('section');
  section.className = "content-section";
  section.style.marginBottom = "36px";
  section.style.padding = "0 32px";

  const wrapper = document.createElement('div');
  wrapper.className = "carousel-wrapper";

  const btnPrev = document.createElement('button');
  btnPrev.className = "carousel-float-arrow float-prev hidden-arrow";
  btnPrev.title = "Anterior";
  btnPrev.innerHTML = `<svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/></svg>`;

  const btnNext = document.createElement('button');
  btnNext.className = "carousel-float-arrow float-next";
  btnNext.title = "Siguiente";
  btnNext.innerHTML = `<svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/></svg>`;

  const trackContainer = document.createElement('div');
  trackContainer.style.display = "flex";
  trackContainer.style.gap = "20px";
  trackContainer.style.overflowX = "auto";
  trackContainer.style.scrollBehavior = "smooth";
  trackContainer.style.paddingBottom = "12px";
  trackContainer.style.scrollbarWidth = "none";

  btnPrev.onclick = () => trackContainer.scrollBy({ left: -440, behavior: 'smooth' });
  btnNext.onclick = () => trackContainer.scrollBy({ left: 440, behavior: 'smooth' });

  bindCarouselPillArrows(trackContainer, btnPrev, btnNext);

  cards.forEach((card, idx) => {
    const heroCard = document.createElement('div');
    heroCard.style.flex = "0 0 420px";
    heroCard.style.width = "420px";
    heroCard.style.display = "flex";
    heroCard.style.flexDirection = "column";
    heroCard.style.cursor = "pointer";

    const subTag = card.type === 'song' ? 'SINGLE DEBUT' : (idx % 2 === 0 ? 'NUEVO ÁLBUM' : 'DESTACADO');
    const descText = `${card.artist} presenta su nuevo trabajo "${card.title}". Escúchalo ya en RayMusic.`;

    heroCard.innerHTML = `
      <div style="margin-bottom: 8px;">
        <span style="font-size: 11px; font-weight: 800; text-transform: uppercase; color: var(--text-muted); letter-spacing: 0.08em; display: block;">${subTag}</span>
        <h3 style="font-size: 19px; font-weight: 800; color: white; letter-spacing: -0.01em; margin: 2px 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${escapeHtmlAttr(card.title)}</h3>
        <span style="font-size: 14px; color: var(--text-secondary); font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; display: block;">${escapeHtmlAttr(card.artist || '')}</span>
      </div>
      <div style="position: relative; width: 100%; height: 250px; border-radius: 18px; overflow: hidden; box-shadow: 0 14px 36px rgba(0,0,0,0.5); transition: transform 0.2s ease;">
        <img src="${card.artwork}" style="width: 100%; height: 100%; object-fit: cover;">
        <div style="position: absolute; inset: 0; background: linear-gradient(180deg, transparent 50%, rgba(0,0,0,0.85) 100%);"></div>
        <div style="position: absolute; bottom: 14px; left: 16px; right: 60px; color: rgba(255,255,255,0.9); font-size: 12px; font-weight: 600; line-height: 1.35; text-shadow: 0 2px 4px rgba(0,0,0,0.8);">
          ${escapeHtmlAttr(descText)}
        </div>
        <img src="${card.artwork}" style="position: absolute; bottom: 12px; right: 14px; width: 36px; height: 36px; border-radius: 6px; border: 1px solid rgba(255,255,255,0.3); box-shadow: 0 4px 10px rgba(0,0,0,0.6); object-fit: cover;">
      </div>
    `;

    const imgBox = heroCard.querySelector('div:nth-child(2)');
    heroCard.addEventListener('mouseenter', () => {
      if (imgBox) imgBox.style.transform = "scale(1.015)";
    });
    heroCard.addEventListener('mouseleave', () => {
      if (imgBox) imgBox.style.transform = "none";
    });

    heroCard.addEventListener('click', () => {
      if (card.type === 'song') {
        playTrackDetails(card.id, card.title, card.artist, card.artwork, card.artistId, card.durationSec || 0);
      } else {
        loadPlaylistContents(card.id, card.title);
      }
    });

    trackContainer.appendChild(heroCard);
  });

  wrapper.appendChild(btnPrev);
  wrapper.appendChild(trackContainer);
  wrapper.appendChild(btnNext);
  section.appendChild(wrapper);
  contentArea.appendChild(section);
}

function renderApple4RowSongGrid(title, items) {
  const section = document.createElement('section');
  section.className = "content-section";
  section.style.marginBottom = "36px";
  section.style.padding = "0 32px";

  const sectionHeader = document.createElement('div');
  sectionHeader.className = "section-header";
  sectionHeader.innerHTML = `
    <h2 class="section-title-sub" style="font-size: 22px; font-weight: 800; margin-bottom: 16px; cursor: pointer;">${escapeHtmlAttr(title)} &gt;</h2>
  `;
  section.appendChild(sectionHeader);

  const wrapper = document.createElement('div');
  wrapper.className = "carousel-wrapper";

  const btnPrev = document.createElement('button');
  btnPrev.className = "carousel-float-arrow float-prev hidden-arrow";
  btnPrev.title = "Anterior";
  btnPrev.innerHTML = `<svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/></svg>`;

  const btnNext = document.createElement('button');
  btnNext.className = "carousel-float-arrow float-next";
  btnNext.title = "Siguiente";
  btnNext.innerHTML = `<svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/></svg>`;

  const columnsContainer = document.createElement('div');
  columnsContainer.style.display = "flex";
  columnsContainer.style.gap = "18px";
  columnsContainer.style.overflowX = "auto";
  columnsContainer.style.scrollBehavior = "smooth";
  columnsContainer.style.paddingBottom = "10px";
  columnsContainer.style.scrollbarWidth = "none";

  btnPrev.onclick = () => columnsContainer.scrollBy({ left: -360, behavior: 'smooth' });
  btnNext.onclick = () => columnsContainer.scrollBy({ left: 360, behavior: 'smooth' });

  bindCarouselPillArrows(columnsContainer, btnPrev, btnNext);

  const columnSize = 4;
  for (let i = 0; i < items.length; i += columnSize) {
    const chunk = items.slice(i, i + columnSize);
    const col = document.createElement('div');
    col.style.flex = "0 0 340px";
    col.style.width = "340px";
    col.style.display = "flex";
    col.style.flexDirection = "column";
    col.style.gap = "8px";

    chunk.forEach(item => {
      const row = document.createElement('div');
      row.className = "song-row";
      row.style.display = "flex";
      row.style.alignItems = "center";
      row.style.padding = "8px 12px";
      row.style.borderRadius = "10px";
      row.style.cursor = "pointer";
      row.style.background = "rgba(255,255,255,0.03)";
      row.style.border = "1px solid rgba(255,255,255,0.05)";

      row.innerHTML = `
        <img src="${item.artwork}" class="song-thumbnail" style="width: 48px; height: 48px; border-radius: 8px; object-fit: cover; margin-right: 12px; flex-shrink: 0;" />
        <div class="song-info" style="display: flex; flex-direction: column; flex-grow: 1; overflow: hidden;">
          <span class="song-title" style="font-size: 13.5px; font-weight: 700; color: white; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
            ${escapeHtmlAttr(item.title)} <span style="font-size: 10px; border: 1px solid rgba(255,255,255,0.4); border-radius: 3px; padding: 1px 4px; font-weight: 800; opacity: 0.7; margin-left: 4px;">E</span>
          </span>
          <span class="song-artist" style="font-size: 11.5px; color: var(--text-secondary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis;" onclick="event.stopPropagation(); loadArtistPage('${item.artistId}', '${item.artist}')">${escapeHtmlAttr(item.artist || '')}</span>
        </div>
        <button class="btn-ctx-dots-item" style="background: none; border: none; color: var(--text-muted); cursor: pointer; padding: 6px; font-size: 14px; font-weight: 900;" title="Opciones">•••</button>
      `;

      row.onclick = () => {
        if (item.type === 'song') {
          playTrackDetails(item.id, item.title, item.artist, item.artwork, item.artistId, item.durationSec || 0);
        } else {
          loadPlaylistContents(item.id, item.title);
        }
      };

      const btnDots = row.querySelector('.btn-ctx-dots-item');
      if (btnDots) {
        btnDots.onclick = (e) => {
          showContextMenu(e, item, 'song');
        };
      }

      col.appendChild(row);
    });

    columnsContainer.appendChild(col);
  }

  wrapper.appendChild(btnPrev);
  wrapper.appendChild(columnsContainer);
  wrapper.appendChild(btnNext);
  section.appendChild(wrapper);
  contentArea.appendChild(section);
}



function renderExploreOffline() {
  contentArea.innerHTML = '';
  const topReleases = [
    { id: "e-1", type: "album", title: "EQUILIBRIVM II", artist: "Anitta", artwork: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400" },
    { id: "e-2", type: "album", title: "Ay Weyy", artist: "Jorsshh", artwork: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400" },
    { id: "e-3", type: "album", title: "LEGENDARIO", artist: "LEGADO 7", artwork: "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400" },
    { id: "e-4", type: "album", title: "Music, Fashion, Film", artist: "Charli xcx", artwork: "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=400" }
  ];
  renderHeroLandscapeCarousel("Lanzamientos Destacados", topReleases);
  renderApple4RowSongGrid("Canciones nuevas destacadas", [
    { id: "s-1", type: "song", title: "Camera", artist: "Charli xcx", artwork: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=180" },
    { id: "s-2", type: "song", title: "que te vaya bien", artist: "Ryan Castro", artwork: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=180" },
    { id: "s-3", type: "song", title: "Ella (Acústico)", artist: "Boza, Beéle", artwork: "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=180" },
    { id: "s-4", type: "song", title: "Mejor que Yo", artist: "Maisak", artwork: "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=180" }
  ]);
}

// --- Radio Feed (Explore Moods & Genres) ---
async function loadRadioFeed() {
  setHeaderVisible(true);
  setHeaderSearchPillVisible(false);
  try {
    const data = await callInnerTubeAPI('search', { query: "radio hits stations", filter: "FILTER_PLAYLIST" }, WEB_CONTEXT);
    const results = parseInnerTubeSearch(data);
    
    contentArea.innerHTML = '';
    if (results.length > 0) {
      renderGridSection("Radios Recomendadas", results);
    } else {
      renderRadioOffline();
    }
  } catch (err) {
    renderRadioOffline();
  }
}

function renderRadioOffline() {
  contentArea.innerHTML = '';
  const radios = [
    { id: "RDCLAK5uy_k_m089201938596701838", title: "Lo-Fi Beats Radio", artist: "24/7 Lo-Fi Chill", artwork: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=180&h=180&fit=crop&q=80" },
    { id: "RDCLAK5uy_mN_Y1l7885k5w8f119047990176", title: "Dance Hits Radio", artist: "Club & EDM Mix", artwork: "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=180&h=180&fit=crop&q=80" }
  ];
  renderGridSection("Estaciones de Radio (Offline)", radios);
}

// --- Concerts Feed ---
async function loadConcertsFeed() {
  try {
    const data = await callInnerTubeAPI('search', { query: "live concert performance full video", filter: "FILTER_VIDEO" }, WEB_CONTEXT);
    const results = parseInnerTubeSearch(data);
    
    contentArea.innerHTML = '';
    if (results.length > 0) {
      renderGridSection("Conciertos en Vivo", results);
    } else {
      renderConcertsOffline();
    }
  } catch (err) {
    renderConcertsOffline();
  }
}

function renderConcertsOffline() {
  contentArea.innerHTML = '';
  const concerts = [
    { id: "concert1", title: "Live at Wembley Stadium", artist: "Stadium Rock Concert", artwork: "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=180&h=180&fit=crop&q=80" }
  ];
  renderGridSection("Conciertos Destacados (Offline)", concerts);
}

function renderGridSection(title, items) {
  const section = document.createElement('section');
  section.className = "content-section";
  section.innerHTML = `<h2 class="section-title-sub" style="margin-bottom: 16px;">${title}</h2>`;
  
  const grid = document.createElement('div');
  grid.className = "grid-container";
  
  items.forEach(item => {
    const card = document.createElement('div');
    card.className = "album-card";
    card.innerHTML = `
      <div class="album-artwork-wrapper">
        <img class="album-artwork" src="${item.artwork}" alt="Art">
        <button class="play-hover-btn" title="Reproducir">
          <svg viewBox="0 0 24 24" width="24" height="24"><path fill="currentColor" d="M8 5v14l11-7z"/></svg>
        </button>
      </div>
      <div class="album-info">
        <span class="album-name">${item.title}</span>
        <span class="album-artist">${item.artist}</span>
      </div>
    `;
    
    card.addEventListener('click', () => {
      if (item.type === 'playlist') {
        loadPlaylistContents(item.id, item.title);
      } else {
        playTrackDetails(item.id, item.title, item.artist, item.artwork, item.artistId, item.durationSec || 0);
      }
    });
    
    grid.appendChild(card);
  });
  
  section.appendChild(grid);
  contentArea.appendChild(section);
}

// --- Playlist & Album Detail Loader ---

function extractHeaderArtwork(data) {
  try {
    let thumbs = data.header?.musicDetailHeaderRenderer?.thumbnail?.croppedSquareThumbnailRenderer?.thumbnail?.thumbnails
      || data.header?.musicDetailHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails
      || data.header?.musicAlbumReleaseDetailHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails;

    if (thumbs && thumbs.length > 0) {
      return upgradeThumbQuality(thumbs[thumbs.length - 1].url);
    }

    thumbs = data.contents?.twoColumnBrowseResultsRenderer?.tabs?.[0]?.tabRenderer?.content?.sectionListRenderer?.contents?.[0]?.musicResponsiveHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails
      || data.contents?.singleColumnBrowseResultsRenderer?.tabs?.[0]?.tabRenderer?.content?.sectionListRenderer?.contents?.[0]?.musicResponsiveHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails;

    if (thumbs && thumbs.length > 0) {
      return upgradeThumbQuality(thumbs[thumbs.length - 1].url);
    }

    thumbs = data.microformat?.microformatDataRenderer?.thumbnail?.thumbnails;
    if (thumbs && thumbs.length > 0) {
      return upgradeThumbQuality(thumbs[thumbs.length - 1].url);
    }
  } catch (e) {}
  return "";
}

function extractDominantColor(imageUrl, callback) {
  const img = new Image();
  img.crossOrigin = "Anonymous";
  img.src = imageUrl;
  img.onload = () => {
    try {
      const canvas = document.createElement('canvas');
      const ctx = canvas.getContext('2d');
      canvas.width = 40;
      canvas.height = 40;
      ctx.drawImage(img, 0, 0, 40, 40);
      const data = ctx.getImageData(0, 0, 40, 40).data;
      let r = 0, g = 0, b = 0, count = 0;
      for (let i = 0; i < data.length; i += 16) {
        const pr = data[i], pg = data[i+1], pb = data[i+2];
        const brightness = (pr * 299 + pg * 587 + pb * 114) / 1000;
        if (brightness > 20 && brightness < 235) {
          r += pr; g += pg; b += pb; count++;
        }
      }
      if (count > 0) {
        r = Math.floor(r / count);
        g = Math.floor(g / count);
        b = Math.floor(b / count);
        callback(`rgb(${r}, ${g}, ${b})`);
      } else {
        callback('#4a3b32');
      }
    } catch(e) {
      callback('#4a3b32');
    }
  };
  img.onerror = () => callback('#4a3b32');
}

function renderPlaylistView(title, tracks, pageType = "Álbum", creator = "", artworkUrl = "") {
  contentArea.innerHTML = '';
  
  const finalArtwork = artworkUrl || tracks[0]?.artwork || "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300&h=300";
  const artistName = creator || tracks[0]?.artist || "Artista";
  const artistId = tracks[0]?.artistId || "";

  const containerWrapper = document.createElement('div');
  containerWrapper.id = "album-container-wrapper";
  containerWrapper.style.position = "relative";
  containerWrapper.style.minHeight = "100%";
  containerWrapper.style.borderRadius = "24px";
  containerWrapper.style.padding = "36px 36px 40px 36px";
  containerWrapper.style.transition = "background 0.6s ease";
  containerWrapper.style.background = "linear-gradient(180deg, #4a3b32 0%, rgba(20,20,24,0.95) 320px, #0a0a0c 100%)";

  extractDominantColor(finalArtwork, (colorHex) => {
    containerWrapper.style.background = `linear-gradient(180deg, ${colorHex} 0%, rgba(20,20,24,0.96) 420px, #0a0a0c 100%)`;
  });

  const heroHeader = document.createElement('div');
  heroHeader.style.display = "flex";
  heroHeader.style.gap = "36px";
  heroHeader.style.alignItems = "flex-end";
  heroHeader.style.marginBottom = "36px";

  heroHeader.innerHTML = `
    <img src="${finalArtwork}" style="width: 340px; height: 340px; border-radius: 24px; object-fit: cover; box-shadow: 0 20px 48px rgba(0,0,0,0.65); flex-shrink: 0;">
    <div style="display: flex; flex-direction: column; justify-content: flex-end;">
      <h1 style="font-size: 46px; font-weight: 900; letter-spacing: -0.03em; line-height: 1.1; margin-bottom: 12px; color: white; display: flex; align-items: center; gap: 10px;">
        ${title} 
        <span style="font-size: 13px; border: 1px solid rgba(255,255,255,0.4); border-radius: 4px; padding: 2px 6px; font-weight: 700; opacity: 0.8;">E</span>
      </h1>
      
      <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 14px;">
        <div style="width: 24px; height: 24px; border-radius: 50%; background: var(--accent-color); display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 800; color: white;">🎵</div>
        <span class="artist-link" style="font-size: 15px; font-weight: 700; color: white;" onclick="event.stopPropagation(); loadArtistPage('${artistId}', '${artistName}')">${artistName}</span>
        <button style="background: rgba(255,255,255,0.15); border: none; border-radius: 50%; width: 22px; height: 22px; color: white; cursor: pointer; display: flex; align-items: center; justify-content: center; font-weight: 700;" title="Seguir">+</button>
      </div>

      <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 22px;">
        <span style="font-size: 10.5px; font-weight: 800; background: rgba(255,255,255,0.12); color: rgba(255,255,255,0.9); padding: 4px 12px; border-radius: 12px; letter-spacing: 0.05em; text-transform: uppercase;">ALTERNATIVE</span>
        <span style="font-size: 10.5px; font-weight: 800; background: rgba(255,255,255,0.12); color: rgba(255,255,255,0.9); padding: 4px 12px; border-radius: 12px;">2024</span>
        <span style="font-size: 10.5px; font-weight: 800; background: rgba(255,255,255,0.12); color: rgba(255,255,255,0.9); padding: 4px 12px; border-radius: 12px;">🎵 ${tracks.length} TEMAS</span>
      </div>

      <div style="display: flex; align-items: center; gap: 12px;">
        <button id="btn-play-all" style="background: white; color: black; border: none; padding: 11px 26px; border-radius: 24px; font-size: 13.5px; font-weight: 800; cursor: pointer; display: flex; align-items: center; gap: 8px; box-shadow: 0 4px 16px rgba(0,0,0,0.3);">
          <svg viewBox="0 0 24 24" width="18" height="18"><path fill="currentColor" d="M8 5v14l11-7z"/></svg>
          <span>Reproducir</span>
        </button>
        <button id="btn-shuffle-all" style="background: rgba(255,255,255,0.18); color: white; border: none; padding: 11px 26px; border-radius: 24px; font-size: 13.5px; font-weight: 800; cursor: pointer; display: flex; align-items: center; gap: 8px; backdrop-filter: blur(10px);">
          <svg viewBox="0 0 24 24" width="16" height="16"><path fill="currentColor" d="M10.59 9.17L5.41 4 4 5.41l5.17 5.17 1.42-1.41zM14.5 4l2.04 2.04L4 18.59 5.41 20 17.96 7.46 20 9.5V4h-5.5zm.33 9.41l-1.41 1.41 3.13 3.13L14.5 20H20v-5.5l-2.04 2.04-3.13-3.13z"/></svg>
          <span>Aleatorio</span>
        </button>
        <button id="btn-header-add-lib" style="background: rgba(255,255,255,0.18); color: white; border: none; padding: 11px 22px; border-radius: 24px; font-size: 13.5px; font-weight: 800; cursor: pointer; display: flex; align-items: center; gap: 6px; backdrop-filter: blur(10px);">+ Agregar</button>
        <button id="btn-header-more-options" style="background: rgba(255,255,255,0.18); color: white; border: none; width: 42px; height: 42px; border-radius: 50%; font-size: 18px; font-weight: 900; cursor: pointer; display: flex; align-items: center; justify-content: center; backdrop-filter: blur(10px);">•••</button>
      </div>
    </div>
  `;
  containerWrapper.appendChild(heroHeader);

  const btnHeaderAdd = heroHeader.querySelector('#btn-header-add-lib');
  if (btnHeaderAdd) {
    const currentAlb = { id: tracks[0]?.albumId || title, title: title, artist: artistName, artwork: finalArtwork, tracks: tracks };
    btnHeaderAdd.textContent = LibraryStorage.isAlbumSaved(currentAlb.id) ? "✓ Guardado" : "+ Agregar";
    btnHeaderAdd.onclick = () => {
      const isSaved = LibraryStorage.toggleSaveAlbum(currentAlb);
      btnHeaderAdd.textContent = isSaved ? "✓ Guardado" : "+ Agregar";
    };
  }

  const btnHeaderMore = heroHeader.querySelector('#btn-header-more-options');
  if (btnHeaderMore) {
    btnHeaderMore.onclick = (e) => {
      showContextMenu(e, { id: title, title: title, artist: artistName, artwork: finalArtwork, tracks: tracks }, 'album');
    };
  }


  const btnPlayAll = heroHeader.querySelector('#btn-play-all');
  if (btnPlayAll) {
    btnPlayAll.addEventListener('click', () => {
      currentQueue = tracks;
      loadTrack(0, true);
      renderQueue();
    });
  }

  const btnShuffleAll = heroHeader.querySelector('#btn-shuffle-all');
  if (btnShuffleAll) {
    btnShuffleAll.addEventListener('click', () => {
      currentQueue = [...tracks].sort(() => Math.random() - 0.5);
      loadTrack(0, true);
      renderQueue();
    });
  }

  // Tracklist Table
  const table = document.createElement('div');
  table.style.display = "flex";
  table.style.flexDirection = "column";
  table.style.gap = "4px";

  const playingTrack = (currentQueue && activeIndex >= 0 && activeIndex < currentQueue.length) ? currentQueue[activeIndex] : null;

  tracks.forEach((track, idx) => {
    const isCurrentPlaying = (playingTrack && playingTrack.id === track.id);
    const row = document.createElement('div');
    row.className = "album-track-row";
    row.style.display = "flex";
    row.style.alignItems = "center";
    row.style.padding = "12px 18px";
    row.style.borderRadius = "12px";
    row.style.cursor = "pointer";
    row.style.transition = "all 0.15s ease";
    
    if (isCurrentPlaying) {
      row.style.backgroundColor = "#f0a36b"; // Warm highlighted pill from screenshot
      row.style.color = "#1c130d";
    } else {
      row.style.backgroundColor = "transparent";
      row.style.color = "white";
      row.addEventListener('mouseenter', () => { if (!isCurrentPlaying) row.style.backgroundColor = "rgba(255,255,255,0.08)"; });
      row.addEventListener('mouseleave', () => { if (!isCurrentPlaying) row.style.backgroundColor = "transparent"; });
    }

    row.innerHTML = `
      <span class="track-idx-span" style="width: 28px; font-size: 13px; font-weight: 700; color: ${isCurrentPlaying ? '#1c130d' : 'var(--text-muted)'}; text-align: center;">
        ${isCurrentPlaying ? `
          <div class="eq-wave-container" style="display: inline-flex; align-items: flex-end; gap: 2px; height: 14px; width: 14px; justify-content: center; vertical-align: middle;">
            <span style="width: 3px; height: 100%; background: #1c130d; border-radius: 2px; animation: eqAnim 0.8s infinite ease-in-out alternate;"></span>
            <span style="width: 3px; height: 60%; background: #1c130d; border-radius: 2px; animation: eqAnim 0.8s infinite ease-in-out alternate 0.2s;"></span>
            <span style="width: 3px; height: 80%; background: #1c130d; border-radius: 2px; animation: eqAnim 0.8s infinite ease-in-out alternate 0.4s;"></span>
          </div>
        ` : (idx + 1)}
      </span>
      <div style="display: flex; flex-direction: column; flex-grow: 1; overflow: hidden; white-space: nowrap; margin-left: 12px;">
        <span class="track-title-span" style="font-size: 14px; font-weight: ${isCurrentPlaying ? '800' : '600'}; color: ${isCurrentPlaying ? '#1c130d' : 'var(--text-primary)'}; text-overflow: ellipsis; overflow: hidden;">${track.title}</span>
      </div>
      <div style="display: flex; align-items: center; gap: 14px; color: ${isCurrentPlaying ? '#1c130d' : 'var(--text-muted)'}; font-size: 12.5px;">
        <span style="font-size: 13px; font-weight: 600;">${track.durationSec ? formatTime(track.durationSec) : '--:--'}</span>
        <button class="btn-row-ctx-dots" style="background: none; border: none; color: inherit; cursor: pointer; padding: 6px; font-size: 16px; font-weight: 900; line-height: 1;" title="Opciones">•••</button>
      </div>
    `;

    row.addEventListener('click', () => {
      currentQueue = tracks;
      loadTrack(idx, true);
    });

        const btnCtx = row.querySelector('.btn-row-ctx-dots');
    if (btnCtx) {
      btnCtx.onclick = (e) => {
        showContextMenu(e, track, 'song');
      };
    }
    table.appendChild(row);
  });

  containerWrapper.appendChild(table);
  contentArea.appendChild(containerWrapper);
}

function parseSectionItemsFromInnerTube(data, defaultArtistName, isVideoContext = false) {
  const items = [];
  const visited = new Set();

  function processItem(itemContainer) {
    if (!itemContainer) return;

    const item = itemContainer.musicResponsiveListItemRenderer;
    const twoRow = itemContainer.musicTwoRowItemRenderer;

    if (item) {
      const videoId = item.navigationEndpoint?.watchEndpoint?.videoId
        || item.playlistItemData?.videoId
        || item.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId;

      const browseId = item.navigationEndpoint?.browseEndpoint?.browseId
        || item.flexColumns?.[0]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.[0]?.navigationEndpoint?.browseEndpoint?.browseId;

      const title = item.flexColumns?.[0]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.[0]?.text;
      
      const subtitleRuns = item.flexColumns?.[1]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs;
      let artist = defaultArtistName;
      let album = "";
      if (subtitleRuns && subtitleRuns.length > 0) {
        artist = subtitleRuns[0]?.text || defaultArtistName;
        if (subtitleRuns.length >= 3) {
          album = subtitleRuns[2]?.text || "";
        }
      }

      const artwork = upgradeThumbQuality(extractThumbnail(item));

      const key = videoId || browseId || title;
      if (key && !visited.has(key)) {
        visited.add(key);
        if (videoId && title) {
          items.push({ id: videoId, title, artist, album, artwork, type: isVideoContext ? 'video' : 'song' });
        } else if (browseId && title) {
          items.push({ id: browseId, title, artist, artwork, type: 'album' });
        }
      }
    } else if (twoRow) {
      const browseId = twoRow.navigationEndpoint?.browseEndpoint?.browseId;
      const videoId = twoRow.navigationEndpoint?.watchEndpoint?.videoId;
      const title = twoRow.title?.runs?.[0]?.text;
      const subtitleRuns = twoRow.subtitle?.runs;
      let artist = defaultArtistName;
      let yearText = "";
      if (subtitleRuns && subtitleRuns.length > 0) {
        artist = subtitleRuns[0]?.text || defaultArtistName;
        if (subtitleRuns.length >= 2) yearText = subtitleRuns[subtitleRuns.length - 1]?.text || "";
      }
      const artwork = upgradeThumbQuality(extractThumbnail(twoRow));
      const pageType = twoRow.navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType;
      const isArtist = pageType === "MUSIC_PAGE_TYPE_ARTIST";

      const key = browseId || videoId || title;
      if (key && !visited.has(key)) {
        visited.add(key);
        if (browseId && title) {
          items.push({ id: browseId, title, artist, yearText, artwork, type: isArtist ? 'artist' : 'album' });
        } else if (videoId && title) {
          items.push({ id: videoId, title, artist, artwork, type: isVideoContext ? 'video' : 'song' });
        }
      }
    }
  }

  function scanContents(contentsArr) {
    if (!Array.isArray(contentsArr)) return;
    contentsArr.forEach(sec => {
      if (!sec) return;

      const shelfContents = sec.musicShelfRenderer?.contents 
        || sec.musicCarouselShelfRenderer?.contents 
        || sec.gridRenderer?.items 
        || sec.musicPlaylistShelfRenderer?.contents
        || (Array.isArray(sec) ? sec : null);

      if (Array.isArray(shelfContents)) {
        shelfContents.forEach(processItem);
      } else if (sec.musicResponsiveListItemRenderer || sec.musicTwoRowItemRenderer) {
        processItem(sec);
      }

      if (sec.sectionListRenderer?.contents) scanContents(sec.sectionListRenderer.contents);
      if (sec.itemSectionRenderer?.contents) scanContents(sec.itemSectionRenderer.contents);
    });
  }

  // 1. Scan primary sections
  const primarySections = data.contents?.singleColumnBrowseResultsRenderer?.tabs?.[0]?.tabRenderer?.content?.sectionListRenderer?.contents
    || data.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer?.contents
    || data.contents?.sectionListRenderer?.contents
    || data.contents?.gridRenderer?.items
    || data.contents?.musicShelfRenderer?.contents
    || data.contents?.musicPlaylistShelfRenderer?.contents
    || [];

  scanContents(primarySections);

  // 2. Scan continuation actions if present
  if (data.onResponseReceivedActions) {
    data.onResponseReceivedActions.forEach(action => {
      const continuationItems = action.appendContinuationItemsAction?.continuationItems
        || action.reloadContinuationItemsCommand?.continuationItems;
      if (Array.isArray(continuationItems)) {
        continuationItems.forEach(processItem);
      }
    });
  }

  return items;
}

async function loadSectionDetailFromInnerTube(sectionTitle, browseId, params, fallbackItems, artistName, isRelatedArtist = false) {
  contentArea.innerHTML = `<div class="loading-spinner-container"><div class="spinner"></div><p>Cargando ${sectionTitle} de ${artistName} desde InnerTube...</p></div>`;
  setHeaderVisible(false);

  const isVideoSection = sectionTitle.toLowerCase().includes("video") 
    || sectionTitle.toLowerCase().includes("live") 
    || sectionTitle.toLowerCase().includes("actuaci") 
    || sectionTitle.toLowerCase().includes("directo") 
    || sectionTitle.toLowerCase().includes("concierto");

  if (browseId) {
    try {
      const payload = { browseId };
      if (params) payload.params = params;
      const data = await callInnerTubeAPI('browse', payload, WEB_CONTEXT);
      const fetchedItems = parseSectionItemsFromInnerTube(data, artistName, isVideoSection);
      if (fetchedItems && fetchedItems.length > 0) {
        renderSectionDetailView(sectionTitle, fetchedItems, artistName, isRelatedArtist);
        return;
      }
    } catch(err) {
      console.warn("Failed to fetch section detail from InnerTube, using fallback items:", err);
    }
  }

  // Fallback to initial items if no separate browse endpoint
  renderSectionDetailView(sectionTitle, fallbackItems, artistName, isRelatedArtist);
}

function renderSectionDetailView(sectionTitle, items, artistName, isRelatedArtist = false) {
  contentArea.innerHTML = '';
  setHeaderVisible(false);

  const container = document.createElement('div');
  container.style.width = "100%";
  container.style.boxSizing = "border-box";
  container.style.padding = "28px 36px 48px 36px";
  container.style.animation = "fadeIn 0.25s ease-out";

  // Top bar with back button matching mobile screenshot
  const topBar = document.createElement('div');
  topBar.style.display = "flex";
  topBar.style.alignItems = "center";
  topBar.style.gap = "16px";
  topBar.style.marginBottom = "28px";

  const backBtn = document.createElement('button');
  backBtn.style.background = "rgba(255,255,255,0.12)";
  backBtn.style.backdropFilter = "blur(12px)";
  backBtn.style.border = "1px solid rgba(255,255,255,0.2)";
  backBtn.style.color = "#FA243C";
  backBtn.style.width = "42px";
  backBtn.style.height = "42px";
  backBtn.style.borderRadius = "50%";
  backBtn.style.cursor = "pointer";
  backBtn.style.display = "flex";
  backBtn.style.alignItems = "center";
  backBtn.style.justifyContent = "center";
  backBtn.title = "Atrás";
  backBtn.innerHTML = `<svg viewBox="0 0 24 24" width="22" height="22"><path fill="currentColor" d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/></svg>`;
  backBtn.addEventListener('click', () => goBack());

  const titleEl = document.createElement('h1');
  titleEl.style.fontSize = "32px";
  titleEl.style.fontWeight = "900";
  titleEl.style.color = "white";
  titleEl.style.margin = "0";
  titleEl.textContent = sectionTitle;

  topBar.appendChild(backBtn);
  topBar.appendChild(titleEl);
  container.appendChild(topBar);

  const isSongSection = items.length > 0 && items.every(i => i.type === 'song');

  if (isSongSection) {
    // Render Numbered Song List (Matching Mobile App Screenshot)
    const listContainer = document.createElement('div');
    listContainer.style.display = "flex";
    listContainer.style.flexDirection = "column";
    listContainer.style.gap = "4px";
    listContainer.style.width = "100%";
    listContainer.style.boxSizing = "border-box";

    items.forEach((song, idx) => {
      const row = document.createElement('div');
      row.style.display = "flex";
      row.style.alignItems = "center";
      row.style.padding = "10px 16px";
      row.style.borderRadius = "14px";
      row.style.cursor = "pointer";
      row.style.transition = "background-color 0.15s ease";

      row.addEventListener('mouseenter', () => row.style.backgroundColor = "rgba(255,255,255,0.08)");
      row.addEventListener('mouseleave', () => row.style.backgroundColor = "transparent");

      row.innerHTML = `
        <span style="font-size: 15px; font-weight: 700; color: rgba(255,255,255,0.45); width: 36px; text-align: center; flex-shrink: 0;">${idx + 1}</span>
        <img src="${song.artwork}" style="width: 48px; height: 48px; border-radius: 10px; object-fit: cover; margin: 0 16px 0 8px; flex-shrink: 0; box-shadow: 0 4px 14px rgba(0,0,0,0.35);">
        <div style="display: flex; flex-direction: column; flex-grow: 1; overflow: hidden; white-space: nowrap;">
          <span style="font-size: 15px; font-weight: 700; color: white; text-overflow: ellipsis; overflow: hidden; margin-bottom: 2px;">${song.title}</span>
          <span style="font-size: 13px; color: rgba(255,255,255,0.6); text-overflow: ellipsis; overflow: hidden;">${song.artist || artistName}</span>
        </div>
        <button class="track-options-btn" style="background: none; border: none; color: rgba(255,255,255,0.6); cursor: pointer; padding: 6px 12px; font-size: 20px; margin-left: auto; flex-shrink: 0;" title="Opciones">⋮</button>
      `;

      const optsBtn = row.querySelector('.track-options-btn');
      if (optsBtn) {
        optsBtn.addEventListener('click', (e) => {
          e.stopPropagation();
          showTrackOptionsMenu(song, e);
        });
      }

      row.addEventListener('click', () => {
        currentQueue = items;
        loadTrack(idx, true);
        renderQueue();
      });

      listContainer.appendChild(row);
    });

    container.appendChild(listContainer);
  } else {
    const isVideoSection = sectionTitle.toLowerCase().includes("video") 
      || sectionTitle.toLowerCase().includes("live") 
      || sectionTitle.toLowerCase().includes("actuaci") 
      || sectionTitle.toLowerCase().includes("directo") 
      || sectionTitle.toLowerCase().includes("concierto")
      || items.some(i => i.type === 'video');

    // Render Responsive Grid View for Albums, Singles, Videos, Artists
    const grid = document.createElement('div');
    grid.style.display = "grid";
    grid.style.gridTemplateColumns = isVideoSection 
      ? "repeat(auto-fill, minmax(260px, 1fr))"
      : isRelatedArtist 
        ? "repeat(auto-fill, minmax(160px, 1fr))" 
        : "repeat(auto-fill, minmax(185px, 1fr))";
    grid.style.gap = "28px 22px";

    items.forEach((card) => {
      const cardEl = document.createElement('div');
      cardEl.style.display = "flex";
      cardEl.style.flexDirection = "column";
      cardEl.style.cursor = "pointer";
      cardEl.style.transition = "transform 0.2s ease";

      cardEl.addEventListener('mouseenter', () => cardEl.style.transform = "translateY(-4px)");
      cardEl.addEventListener('mouseleave', () => cardEl.style.transform = "none");

      if (isRelatedArtist || card.type === 'artist') {
        cardEl.innerHTML = `
          <img src="${card.artwork}" style="width: 150px; height: 150px; border-radius: 50%; object-fit: cover; box-shadow: 0 8px 22px rgba(0,0,0,0.4); margin: 0 auto 10px auto;">
          <span style="font-size: 14px; font-weight: 700; color: white; text-align: center; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; width: 100%; display: block;">${card.title}</span>
          <span style="font-size: 11.5px; color: rgba(255,255,255,0.6); text-align: center; text-transform: uppercase; margin-top: 2px; display: block;">Artista</span>
        `;
        cardEl.addEventListener('click', () => loadArtistPage(card.id, card.title));
      } else if (isVideoSection || card.type === 'video') {
        cardEl.innerHTML = `
          <div style="width: 100%; aspect-ratio: 16 / 9; border-radius: 14px; overflow: hidden; box-shadow: 0 10px 24px rgba(0,0,0,0.4); margin-bottom: 10px; background: rgba(0,0,0,0.4); position: relative;">
            <img src="${card.artwork}" style="width: 100%; height: 100%; object-fit: cover;">
            <div style="position: absolute; inset: 0; background: linear-gradient(to top, rgba(0,0,0,0.45), transparent); display: flex; align-items: center; justify-content: center;">
              <div style="width: 44px; height: 44px; border-radius: 50%; background: rgba(0,0,0,0.7); backdrop-filter: blur(8px); display: flex; align-items: center; justify-content: center; color: white;">
                <svg viewBox="0 0 24 24" width="22" height="22" style="margin-left: 2px;"><path fill="currentColor" d="M8 5v14l11-7z"/></svg>
              </div>
            </div>
          </div>
          <span style="font-size: 14px; font-weight: 800; color: white; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; width: 100%; display: block; margin-bottom: 2px;">${card.title}</span>
          <span style="font-size: 12px; color: rgba(255,255,255,0.65); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; width: 100%; display: block;">${card.artist || artistName}${card.yearText ? ` • ${card.yearText}` : ''}</span>
        `;

        cardEl.addEventListener('click', () => {
          showVideoPlayerModal(card.id, card.title, card.artist || artistName);
        });
      } else {
        cardEl.innerHTML = `
          <div style="width: 100%; aspect-ratio: 1; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 24px rgba(0,0,0,0.4); margin-bottom: 10px; background: rgba(0,0,0,0.3);">
            <img src="${card.artwork}" style="width: 100%; height: 100%; object-fit: cover;">
          </div>
          <span style="font-size: 14px; font-weight: 800; color: white; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; width: 100%; display: block; margin-bottom: 2px;">${card.title}</span>
          <span style="font-size: 12px; color: rgba(255,255,255,0.65); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; width: 100%; display: block;">${card.artist || artistName}${card.yearText ? ` • ${card.yearText}` : ''}</span>
        `;

        cardEl.addEventListener('click', () => {
          if (card.type === 'song') {
            playTrackDetails(card.id, card.title, card.artist || artistName, card.artwork, card.artistId);
          } else {
            loadPlaylistContents(card.id, card.title);
          }
        });
      }

      grid.appendChild(cardEl);
    });

    container.appendChild(grid);
  }

  contentArea.appendChild(container);
}

function extractBottomStripColor(imageUrl, callback) {
  const img = new Image();
  img.crossOrigin = "Anonymous";
  img.src = imageUrl;
  img.onload = () => {
    try {
      const canvas = document.createElement('canvas');
      const ctx = canvas.getContext('2d');
      canvas.width = 60;
      canvas.height = 60;
      ctx.drawImage(img, 0, 0, 60, 60);
      const data = ctx.getImageData(0, 44, 60, 16).data;
      let r = 0, g = 0, b = 0, count = 0;
      for (let i = 0; i < data.length; i += 4) {
        const pr = data[i], pg = data[i+1], pb = data[i+2];
        r += pr; g += pg; b += pb; count++;
      }
      if (count > 0) {
        r = Math.floor(r / count);
        g = Math.floor(g / count);
        b = Math.floor(b / count);
        callback(`rgb(${r}, ${g}, ${b})`, `rgba(${r}, ${g}, ${b}, 0.5)`);
      } else {
        callback('rgb(36, 40, 48)', 'rgba(36, 40, 48, 0.5)');
      }
    } catch(e) {
      callback('rgb(36, 40, 48)', 'rgba(36, 40, 48, 0.5)');
    }
  };
  img.onerror = () => callback('rgb(36, 40, 48)', 'rgba(36, 40, 48, 0.5)');
}

function extractArtworkMultiPalette(imageUrl, callback) {
  const img = new Image();
  img.crossOrigin = "Anonymous";
  img.src = imageUrl;
  img.onload = () => {
    try {
      const canvas = document.createElement('canvas');
      const ctx = canvas.getContext('2d');
      canvas.width = 100;
      canvas.height = 100;
      ctx.drawImage(img, 0, 0, 100, 100);

      const sampleRegion = (x, y, w, h) => {
        const data = ctx.getImageData(x, y, w, h).data;
        let r = 0, g = 0, b = 0, count = 0;
        for (let i = 0; i < data.length; i += 4) {
          r += data[i]; g += data[i+1]; b += data[i+2]; count++;
        }
        r = Math.floor(r / count); g = Math.floor(g / count); b = Math.floor(b / count);
        return `rgb(${r}, ${g}, ${b})`;
      };

      const cL1 = sampleRegion(0, 0, 30, 20);
      const cL2 = sampleRegion(0, 20, 30, 20);
      const cL3 = sampleRegion(0, 40, 30, 20);
      const cL4 = sampleRegion(0, 60, 30, 20);
      const cL5 = sampleRegion(0, 80, 30, 20);

      const cR1 = sampleRegion(70, 0, 30, 20);
      const cR2 = sampleRegion(70, 20, 30, 20);
      const cR3 = sampleRegion(70, 40, 30, 20);
      const cR4 = sampleRegion(70, 60, 30, 20);
      const cR5 = sampleRegion(70, 80, 30, 20);

      const cTL = cL1;
      const cTR = cR1;
      const cBL = cL5;
      const cBR = cR5;
      const cCenter = sampleRegion(35, 35, 30, 30);

      callback({ cTL, cTR, cBL, cBR, cCenter, cL1, cL2, cL3, cL4, cL5, cR1, cR2, cR3, cR4, cR5 });
    } catch(e) {
      const fb = 'rgb(120, 80, 70)';
      callback({ cTL: fb, cTR: fb, cBL: fb, cBR: fb, cCenter: fb, cL1: fb, cL2: fb, cL3: fb, cL4: fb, cL5: fb, cR1: fb, cR2: fb, cR3: fb, cR4: fb, cR5: fb });
    }
  };
  img.onerror = () => {
    const fb = 'rgb(120, 80, 70)';
    callback({ cTL: fb, cTR: fb, cBL: fb, cBR: fb, cCenter: fb, cL1: fb, cL2: fb, cL3: fb, cL4: fb, cL5: fb, cR1: fb, cR2: fb, cR3: fb, cR4: fb, cR5: fb });
  };
}
// --- Apple Music Motion Video Canvas Provider (Echo-Music integration) ---
let cachedAppleMusicToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IldlYlBsYXlLaWQifQ.eyJpc3OiOiJBTVBXZWJQbGF5IiwiaWF0IjoxNzg0MjE3MDk0LCJleHAiOjE3ODcyNDEwOTQsInJvb3RfaHR0cHNfb3JpZ2luIjpbImFwcGxlLmNvbSJdfQ.K-J9PiqXYvx2Geki1Hh7zVelFrxAul4bAPQk1SKAW8dPm-nIzGaSehbItsaa84pk3jA2dQcBix3qwfjMmx7Fxw";

async function getAppleMusicToken() {
  if (cachedAppleMusicToken) return cachedAppleMusicToken;
  try {
    const htmlRes = await fetch("https://beta.music.apple.com", {
      headers: { 'Origin': 'https://music.apple.com' }
    });
    if (htmlRes.ok) {
      const html = await htmlRes.text();
      const match = html.match(/src="(\/assets\/index-[^"]+\.js)"/) || html.match(/src="(\/assets\/[^\"]+\.js)"/);
      if (match && match[1]) {
        const jsRes = await fetch("https://beta.music.apple.com" + match[1], {
          headers: { 'Origin': 'https://music.apple.com' }
        });
        if (jsRes.ok) {
          const js = await jsRes.text();
          const tokenMatch = js.match(/eyJ[A-Za-z0-9\-_=]+\.[A-Za-z0-9\-_=]+\.[A-Za-z0-9\-_=]+/);
          if (tokenMatch && tokenMatch[0]) {
            cachedAppleMusicToken = tokenMatch[0];
          }
        }
      }
    }
  } catch(e) {}
  return cachedAppleMusicToken;
}

async function fetchArtistMotionVideo(artistName) {
  if (!artistName) return { error: "Nombre de artista vacío" };
  try {
    let token = await getAppleMusicToken();
    let searchUrl = `https://amp-api.music.apple.com/v1/catalog/us/search?term=${encodeURIComponent(artistName)}&types=artists&limit=3`;
    let searchRes = await fetch(searchUrl, {
      headers: {
        'Authorization': 'Bearer ' + token,
        'Origin': 'https://music.apple.com'
      }
    });

    if (searchRes.status === 401) {
      cachedAppleMusicToken = "";
      token = await getAppleMusicToken();
      searchRes = await fetch(searchUrl, {
        headers: {
          'Authorization': 'Bearer ' + token,
          'Origin': 'https://music.apple.com'
        }
      });
    }

    if (!searchRes.ok) {
      return { error: `Buscar en Apple: HTTP ${searchRes.status}` };
    }
    const searchData = await searchRes.json();
    const artistObj = searchData.results?.artists?.data?.[0];
    if (!artistObj || !artistObj.id) {
      return { error: `Sin coincidencias en Apple para "${artistName}"` };
    }

    const detailUrl = `https://amp-api.music.apple.com/v1/catalog/us/artists/${artistObj.id}?extend=editorialVideo,editorialArtwork`;
    const artistRes = await fetch(detailUrl, {
      headers: {
        'Authorization': 'Bearer ' + token,
        'Origin': 'https://music.apple.com'
      }
    });
    if (!artistRes.ok) {
      return { error: `Detalle de Apple: HTTP ${artistRes.status}` };
    }
    const artistData = await artistRes.json();
    const attrs = artistData.data?.[0]?.attributes;
    const ev = attrs?.editorialVideo || attrs?.editorialArtwork;
    if (ev) {
      const preferredKeys = ["motionArtistFullscreen16x9", "motionArtistSquare1x1", "motionArtistWide16x9", "motionDetailRaw", "motionDetailTall", "motionDetailSquare", "motionSquareVideo1x1"];
      for (const k of preferredKeys) {
        if (ev[k] && ev[k].video) {
          return { video: ev[k].video };
        }
      }
      for (const k in ev) {
        if (ev[k] && ev[k].video) {
          return { video: ev[k].video };
        }
      }
    }
    return { error: `Apple no posee video animado para "${artistName}"` };
  } catch(e) {
    return { error: `Excepción de red/CORS: ${e.message}` };
  }
}

// --- Artist Profile Page Loader ---
async function loadArtistPage(artistId, artistName, shouldPushHistory = true) {
  setHeaderVisible(false);
  const displayName = artistName || "Artista";
  contentArea.innerHTML = `<div class="loading-spinner-container"><div class="spinner"></div><p>Cargando Perfil de Artista: ${displayName}...</p></div>`;
  document.getElementById('page-title').textContent = displayName;

  let validArtistId = artistId;
  if (!validArtistId || typeof validArtistId !== 'string' || (!validArtistId.startsWith('UC') && !validArtistId.startsWith('FEmusic'))) {
    if (displayName && displayName !== 'Artista' && displayName !== 'Canción' && displayName !== 'Música') {
      try {
        const searchRes = await callInnerTubeAPI('search', { query: displayName }, WEB_CONTEXT);
        const searchContents = searchRes.contents?.tabbedSearchResultsRenderer?.tabs?.[0]?.tabRenderer?.content?.sectionListRenderer?.contents || [];
        for (const sec of searchContents) {
          const items = sec.musicShelfRenderer?.contents || sec.musicCardShelfRenderer?.contents || [];
          for (const itemContainer of items) {
            const item = itemContainer.musicResponsiveListItemRenderer || itemContainer.musicTwoRowItemRenderer;
            const bId = item?.navigationEndpoint?.browseEndpoint?.browseId
                     || item?.title?.runs?.[0]?.navigationEndpoint?.browseEndpoint?.browseId
                     || item?.subtitle?.runs?.[0]?.navigationEndpoint?.browseEndpoint?.browseId;
            if (bId && (bId.startsWith('UC') || bId.startsWith('FEmusic'))) {
              validArtistId = bId;
              break;
            }
          }
          if (validArtistId) break;
        }
      } catch (e) {
        console.warn('Failed to resolve artist ID by search:', e);
      }
    }
  }

  if (!validArtistId) {
    performSearch(displayName, shouldPushHistory);
    return;
  }

  if (shouldPushHistory) {
    pushNavigation({ name: 'artist', params: { artistId: validArtistId, artistName: displayName } });
  }

  try {
    const data = await callInnerTubeAPI('browse', { browseId: validArtistId }, WEB_CONTEXT);
    contentArea.innerHTML = '';

    // Create wrapper element for artist page with ambient background
    const artistWrapper = document.createElement('div');
    artistWrapper.id = "artist-page-wrapper";
    artistWrapper.style.position = "relative";
    artistWrapper.style.minHeight = "100%";
    artistWrapper.style.borderRadius = "28px";
    artistWrapper.style.padding = "0 0 36px 0";
    artistWrapper.style.transition = "background-color 0.6s ease";
    artistWrapper.style.backgroundColor = "#242730"; // initial fallback ambient color

    // 1. Get header metadata (Banner Image & description)
    let bannerUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=1200&h=600&fit=crop";
    const headerThumbnails = data.header?.musicImmersiveHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails
      || data.header?.musicVisualHeaderRenderer?.foregroundThumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails
      || data.header?.musicHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails;
    if (headerThumbnails && headerThumbnails.length > 0) {
      bannerUrl = upgradeThumbQuality(headerThumbnails[headerThumbnails.length - 1].url);
    }

    const officialName = data.header?.musicImmersiveHeaderRenderer?.title?.runs?.[0]?.text 
                      || data.header?.musicVisualHeaderRenderer?.title?.runs?.[0]?.text
                      || artistName;

    const subCount = data.header?.musicImmersiveHeaderRenderer?.subscriptionButton?.subscribeButtonRenderer?.subscriberCountWithSubscribeText?.runs?.[0]?.text
      || data.header?.musicImmersiveHeaderRenderer?.subscriptionButton2?.subscribeButtonRenderer?.subscriberCountWithSubscribeText?.runs?.[0]?.text
      || "";

    const descriptionText = data.description 
      || data.header?.musicImmersiveHeaderRenderer?.description?.runs?.[0]?.text 
      || `${officialName} es un artista en YouTube Music.`;

    // 2. Parse sections (Top Songs, Albums, Singles, Videos, Related)
    const sections = data.contents?.singleColumnBrowseResultsRenderer?.tabs?.[0]?.tabRenderer?.content?.sectionListRenderer?.contents || [];

    let topSongs = [];
    let topSongsTitle = "Top Songs";
    let latestRelease = null;
    const carousels = [];

    sections.forEach(section => {
      if (section.musicShelfRenderer) {
        const shelf = section.musicShelfRenderer;
        topSongsTitle = shelf.title?.runs?.[0]?.text || "Top Songs";
        
        const shelfHeader = shelf.header?.musicShelfBasicHeaderRenderer;
        const topBrowseEndpoint = shelfHeader?.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint
          || shelfHeader?.title?.runs?.[0]?.navigationEndpoint?.browseEndpoint
          || shelf.title?.runs?.[0]?.navigationEndpoint?.browseEndpoint;
        topSongsBrowseId = topBrowseEndpoint?.browseId || "";
        topSongsParams = topBrowseEndpoint?.params || "";

        shelf.contents.forEach(itemContainer => {
          const item = itemContainer.musicResponsiveListItemRenderer;
          if (!item) return;

          const videoId = item.navigationEndpoint?.watchEndpoint?.videoId;
          if (!videoId) return;

          let songTitle = "Canción";
          const titleRuns = item.flexColumns?.[0]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs;
          if (titleRuns && titleRuns.length > 0) songTitle = titleRuns[0].text;

          let artistText = officialName;
          let albumText = "";
          const subtitleRuns = item.flexColumns?.[1]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs;
          if (subtitleRuns && subtitleRuns.length > 0) {
            artistText = subtitleRuns.map(r => r.text).join("");
            if (subtitleRuns.length >= 3) albumText = subtitleRuns[2].text;
          }

          let thumbUrl = upgradeThumbQuality(extractThumbnail(item));

          topSongs.push({
            id: videoId,
            title: songTitle,
            artist: artistText,
            artistId: artistId,
            album: albumText || officialName,
            artwork: thumbUrl,
            streamUrl: ""
          });
        });
      } else if (section.musicCarouselShelfRenderer) {
        const shelf = section.musicCarouselShelfRenderer;
        const title = shelf.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.[0]?.text || "Álbumes";
        const cards = parseCarouselShelf(shelf);

        const headerRenderer = shelf.header?.musicCarouselShelfBasicHeaderRenderer;
        const browseEndpoint = headerRenderer?.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint
          || headerRenderer?.title?.runs?.[0]?.navigationEndpoint?.browseEndpoint
          || shelf.title?.runs?.[0]?.navigationEndpoint?.browseEndpoint;

        const sectionBrowseId = browseEndpoint?.browseId || "";
        const sectionParams = browseEndpoint?.params || "";

        // Capture latest release candidate from carousel if available
        if (!latestRelease && cards.length > 0 && (title.toLowerCase().includes("lanzamiento") || title.toLowerCase().includes("single") || title.toLowerCase().includes("sencillo") || title.toLowerCase().includes("álbum") || title.toLowerCase().includes("album"))) {
          const first = cards[0];
          latestRelease = {
            id: first.id,
            title: first.title,
            type: first.type === 'song' ? 'Single' : 'Álbum',
            artist: first.artist || officialName,
            artwork: first.artwork,
            dateText: "Último lanzamiento"
          };
        }

        if (cards.length > 0) {
          carousels.push({ title, cards, browseId: sectionBrowseId, params: sectionParams });
        }
      }
    });

    if (!latestRelease && carousels.length > 0 && carousels[0].cards.length > 0) {
      const first = carousels[0].cards[0];
      latestRelease = {
        id: first.id,
        title: first.title,
        type: 'Lanzamiento',
        artist: first.artist || officialName,
        artwork: first.artwork,
        dateText: "Último lanzamiento"
      };
    }

    // Prefetch extra top songs if fewer than 10 to ensure a full filled 10-song grid
    if (topSongs.length < 10 && topSongsBrowseId) {
      try {
        const payload = { browseId: topSongsBrowseId };
        if (topSongsParams) payload.params = topSongsParams;
        const extraData = await callInnerTubeAPI('browse', payload, WEB_CONTEXT);
        const extraItems = parseSectionItemsFromInnerTube(extraData, officialName);
        extraItems.forEach(item => {
          if (item.type === 'song' && !topSongs.some(s => s.id === item.id)) {
            topSongs.push({
              id: item.id,
              title: item.title,
              artist: item.artist || officialName,
              artistId: artistId,
              album: item.album || officialName,
              artwork: item.artwork,
              streamUrl: ""
            });
          }
        });
      } catch(e) {
        console.warn("Could not prefetch extra top songs:", e);
      }
    }

    // --- Render Hero Header (Soft Blurred Transition & Motion Video Layer) ---
    const heroSection = document.createElement('div');
    heroSection.style.position = "relative";
    heroSection.style.width = "100%";
    heroSection.style.height = "760px";
    heroSection.style.borderRadius = "28px 28px 0 0";
    heroSection.style.overflow = "hidden";
    heroSection.style.marginBottom = "36px";
    heroSection.style.display = "flex";
    heroSection.style.flexDirection = "column";
    heroSection.style.justifyContent = "space-between";
    heroSection.style.padding = "24px 32px 10px 32px";

    // 0. Static Banner Image Layer (z-index: 0)
    const staticBg = document.createElement('div');
    staticBg.style.position = "absolute";
    staticBg.style.top = "0";
    staticBg.style.left = "0";
    staticBg.style.width = "100%";
    staticBg.style.height = "100%";
    staticBg.style.backgroundImage = `url('${bannerUrl}')`;
    staticBg.style.backgroundSize = "cover";
    staticBg.style.backgroundPosition = "center center";
    staticBg.style.zIndex = "0";
    heroSection.appendChild(staticBg);

    // 1. Gradient Overlay Layer (z-index: 2)
    const gradientOverlay = document.createElement('div');
    gradientOverlay.style.position = "absolute";
    gradientOverlay.style.top = "0";
    gradientOverlay.style.left = "0";
    gradientOverlay.style.width = "100%";
    gradientOverlay.style.height = "100%";
    gradientOverlay.style.backgroundImage = `linear-gradient(to bottom, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 40%, rgba(36,40,48,0.4) 75%, #242830 100%)`;
    gradientOverlay.style.zIndex = "2";
    gradientOverlay.style.pointerEvents = "none";
    heroSection.appendChild(gradientOverlay);

    // Sample ambient color from the bottom of the artist banner image
    extractBottomStripColor(bannerUrl, (ambientRgb, ambientHalf) => {
      artistWrapper.style.backgroundColor = ambientRgb;
      gradientOverlay.style.backgroundImage = `linear-gradient(to bottom, rgba(0,0,0,0) 0%, rgba(0,0,0,0) 40%, ${ambientHalf} 75%, ${ambientRgb} 100%)`;
    });

    // 2. Motion Video Layer (z-index: 1, powered by HLS.js for .m3u8 streams on Windows)
    fetchArtistMotionVideo(officialName).then(res => {
      if (res && res.video) {
        const motionUrl = res.video;
        const videoBg = document.createElement('video');
        videoBg.autoplay = true;
        videoBg.loop = true;
        videoBg.muted = true;
        videoBg.playsInline = true;
        videoBg.style.position = "absolute";
        videoBg.style.top = "0";
        videoBg.style.left = "0";
        videoBg.style.width = "100%";
        videoBg.style.height = "100%";
        videoBg.style.objectFit = "cover";
        videoBg.style.zIndex = "1";
        videoBg.style.opacity = "1";

        if (typeof Hls !== 'undefined' && Hls.isSupported() && motionUrl.includes('.m3u8')) {
          const hls = new Hls({
            enableWorker: true,
            lowLatencyMode: true
          });
          hls.loadSource(motionUrl);
          hls.attachMedia(videoBg);
          hls.on(Hls.Events.MANIFEST_PARSED, () => {
            videoBg.play().catch(e => console.warn("Video play error:", e));
          });
        } else {
          videoBg.src = motionUrl;
          videoBg.play().catch(e => console.warn("Direct video play error:", e));
        }
        heroSection.appendChild(videoBg);
      }
    });

    // Top-left Back button inside Hero
    const heroTopBar = document.createElement('div');
    heroTopBar.style.position = "relative";
    heroTopBar.style.zIndex = "2";
    heroTopBar.style.display = "flex";
    heroTopBar.style.alignItems = "center";
    heroTopBar.style.justifyContent = "space-between";

    const backBtn = document.createElement('button');
    backBtn.style.background = "rgba(0,0,0,0.35)";
    backBtn.style.backdropFilter = "blur(12px)";
    backBtn.style.border = "1px solid rgba(255,255,255,0.15)";
    backBtn.style.color = "white";
    backBtn.style.width = "38px";
    backBtn.style.height = "38px";
    backBtn.style.borderRadius = "50%";
    backBtn.style.cursor = "pointer";
    backBtn.style.display = "flex";
    backBtn.style.alignItems = "center";
    backBtn.style.justifyContent = "center";
    backBtn.title = "Atrás";
    backBtn.innerHTML = `<svg viewBox="0 0 24 24" width="20" height="20"><path fill="currentColor" d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/></svg>`;
    backBtn.addEventListener('click', () => goBack());

    heroTopBar.appendChild(backBtn);
    heroSection.appendChild(heroTopBar);

    // Bottom center of Hero: Artist Name + Action Row (Info 'i', Big White Play Circle, Favorite Star)
    const heroCenterContent = document.createElement('div');
    heroCenterContent.style.position = "relative";
    heroCenterContent.style.zIndex = "2";
    heroCenterContent.style.display = "flex";
    heroCenterContent.style.flexDirection = "column";
    heroCenterContent.style.alignItems = "center";
    heroCenterContent.style.textAlign = "center";
    heroCenterContent.style.marginBottom = "0px";

    const isArtistFollowedInitial = LibraryStorage.isArtistFollowed(validArtistId);
    const starBg = isArtistFollowedInitial ? "#ff2d55" : "rgba(255,255,255,0.22)";
    const starBorder = isArtistFollowedInitial ? "none" : "1px solid rgba(255,255,255,0.3)";
    const starColor = isArtistFollowedInitial ? "#ffffff" : "white";

    heroCenterContent.innerHTML = `
      <h1 style="font-size: 46px; font-weight: 900; color: white; letter-spacing: -0.02em; margin: 0 0 16px 0; text-shadow: 0 2px 14px rgba(0,0,0,0.4); line-height: 1.1;">${officialName}</h1>
      
      <div style="display: flex; align-items: center; justify-content: center; gap: 20px;">
        <!-- Info Button (i) -->
        <button id="artist-btn-info" title="Información del artista" style="width: 48px; height: 48px; border-radius: 50%; background: rgba(255,255,255,0.22); backdrop-filter: blur(16px); border: 1px solid rgba(255,255,255,0.3); color: white; font-size: 19px; font-weight: 800; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: transform 0.15s ease;">
          i
        </button>

        <!-- Big Play Button (White Circle matching mobile screen - No pitch black dark shadow) -->
        <button id="artist-btn-play-all" title="Reproducir todo" style="width: 72px; height: 72px; border-radius: 50%; background: #ffffff; border: none; color: #000000; cursor: pointer; display: flex; align-items: center; justify-content: center; box-shadow: 0 4px 14px rgba(0,0,0,0.15); transition: transform 0.2s cubic-bezier(0.25, 1, 0.5, 1);">
          <svg viewBox="0 0 24 24" width="34" height="34" style="margin-left: 3px;"><path fill="#000000" d="M8 5v14l11-7z"/></svg>
        </button>

        <!-- Favorite Star Button -->
        <button id="artist-btn-star" title="Guardar en favoritos" style="width: 48px; height: 48px; border-radius: 50%; background: ${starBg}; backdrop-filter: blur(16px); border: ${starBorder}; color: ${starColor}; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: transform 0.15s ease, background-color 0.2s ease;">
          <svg viewBox="0 0 24 24" width="22" height="22"><path fill="currentColor" d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/></svg>
        </button>
      </div>
    `;

    heroSection.appendChild(heroCenterContent);
    artistWrapper.appendChild(heroSection);

    // Attach Play All Event
    const btnPlayAll = heroCenterContent.querySelector('#artist-btn-play-all');
    btnPlayAll.addEventListener('mouseenter', () => btnPlayAll.style.transform = "scale(1.08)");
    btnPlayAll.addEventListener('mouseleave', () => btnPlayAll.style.transform = "none");
    btnPlayAll.addEventListener('click', () => {
      if (topSongs.length > 0) {
        currentQueue = topSongs;
        loadTrack(0, true);
        renderQueue();
      }
    });

    // Attach Info Event (Show Artist Bio Modal)
    const btnInfo = heroCenterContent.querySelector('#artist-btn-info');
    btnInfo.addEventListener('mouseenter', () => btnInfo.style.transform = "scale(1.08)");
    btnInfo.addEventListener('mouseleave', () => btnInfo.style.transform = "none");
    btnInfo.addEventListener('click', () => {
      showArtistBioModal(officialName, bannerUrl, descriptionText, subCount);
    });

    // Attach Star Event
    const btnStar = heroCenterContent.querySelector('#artist-btn-star');
    btnStar.addEventListener('mouseenter', () => btnStar.style.transform = "scale(1.08)");
    btnStar.addEventListener('mouseleave', () => btnStar.style.transform = "none");
    btnStar.addEventListener('click', () => {
      const nowFollowed = LibraryStorage.toggleFollowArtist({ id: validArtistId, name: officialName, artwork: bannerUrl });
      if (nowFollowed) {
        btnStar.style.background = "#ff2d55";
        btnStar.style.border = "none";
        btnStar.style.color = "#ffffff";
      } else {
        btnStar.style.background = "rgba(255,255,255,0.22)";
        btnStar.style.border = "1px solid rgba(255,255,255,0.3)";
        btnStar.style.color = "white";
      }
    });

    // --- Main Split Content: Latest Release + Top Songs ---
    if (latestRelease || topSongs.length > 0) {
      const splitRow = document.createElement('div');
      splitRow.style.display = "flex";
      splitRow.style.gap = "32px";
      splitRow.style.padding = "0 32px";
      splitRow.style.marginBottom = "36px";
      splitRow.style.alignItems = "flex-start";

      // Left Column: Latest Release
      if (latestRelease) {
        const releaseCol = document.createElement('div');
        releaseCol.style.flex = "0 0 270px";
        releaseCol.style.display = "flex";
        releaseCol.style.flexDirection = "column";

        releaseCol.innerHTML = `
          <h2 class="section-title-sub" style="font-size: 19px; font-weight: 800; margin-bottom: 14px; color: white;">Latest Release</h2>
          <div style="width: 100%; border-radius: 18px; overflow: hidden; box-shadow: 0 12px 30px rgba(0,0,0,0.45); margin-bottom: 12px; background: #1a1a1e; cursor: pointer;" id="latest-release-card">
            <img src="${latestRelease.artwork}" style="width: 100%; height: 260px; object-fit: cover;">
          </div>
          <span style="font-size: 11px; font-weight: 700; color: rgba(255,255,255,0.6); text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 3px;">${latestRelease.dateText}</span>
          <span style="font-size: 14px; font-weight: 800; color: white; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-bottom: 2px;">${latestRelease.title}</span>
          <div style="display: flex; align-items: center; justify-content: space-between; margin-top: 4px;">
            <span style="font-size: 12px; color: rgba(255,255,255,0.7); font-weight: 500;">${latestRelease.type}</span>
            <button style="background: rgba(255,255,255,0.12); border: none; border-radius: 50%; width: 28px; height: 28px; color: white; cursor: pointer; font-size: 16px; font-weight: 700; display: flex; align-items: center; justify-content: center;" title="Agregar">+</button>
          </div>
        `;

        const relCard = releaseCol.querySelector('#latest-release-card');
        relCard.addEventListener('click', () => {
          loadPlaylistContents(latestRelease.id, latestRelease.title);
        });

        splitRow.appendChild(releaseCol);
      }

      // Right Column: Top Songs (Grid matching mobile screenshot)
      if (topSongs.length > 0) {
        const songsCol = document.createElement('div');
        songsCol.style.flex = "1";
        songsCol.style.minWidth = "0";

        const songsHeader = document.createElement('div');
        songsHeader.className = "section-header";
        songsHeader.style.marginBottom = "14px";
        songsHeader.style.cursor = "pointer";
        songsHeader.innerHTML = `<h2 class="section-title-sub" style="font-size: 19px; font-weight: 800; color: white;">${topSongsTitle} &gt;</h2>`;
        
        songsHeader.addEventListener('click', () => {
          // Format top songs for section detail view
          const songCards = topSongs.map(s => ({
            id: s.id,
            title: s.title,
            artist: s.artist,
            artwork: s.artwork,
            type: 'song'
          }));
          pushNavigation({ name: 'section_detail', params: { title: topSongsTitle, browseId: topSongsBrowseId, params: topSongsParams, items: songCards, artistName: officialName } });
          loadSectionDetailFromInnerTube(topSongsTitle, topSongsBrowseId, topSongsParams, songCards, officialName, false);
        });

        songsCol.appendChild(songsHeader);

        const songsGrid = document.createElement('div');
        songsGrid.style.display = "grid";
        songsGrid.style.gridTemplateColumns = "repeat(auto-fill, minmax(260px, 1fr))";
        songsGrid.style.gap = "8px 16px";

        topSongs.slice(0, 10).forEach((song, idx) => {
          const row = document.createElement('div');
          row.style.display = "flex";
          row.style.alignItems = "center";
          row.style.padding = "8px 12px";
          row.style.borderRadius = "12px";
          row.style.cursor = "pointer";
          row.style.backgroundColor = "rgba(255,255,255,0.06)";
          row.style.transition = "background-color 0.15s ease, transform 0.15s ease";

          row.addEventListener('mouseenter', () => {
            row.style.backgroundColor = "rgba(255,255,255,0.12)";
            row.style.transform = "translateY(-1px)";
          });
          row.addEventListener('mouseleave', () => {
            row.style.backgroundColor = "rgba(255,255,255,0.06)";
            row.style.transform = "none";
          });

          row.innerHTML = `
            <img src="${song.artwork}" style="width: 44px; height: 44px; border-radius: 8px; object-fit: cover; margin-right: 12px; flex-shrink: 0; box-shadow: 0 4px 12px rgba(0,0,0,0.3);">
            <div style="display: flex; flex-direction: column; flex-grow: 1; overflow: hidden; white-space: nowrap;">
              <span style="font-size: 13.5px; font-weight: 700; color: white; text-overflow: ellipsis; overflow: hidden;">${song.title}</span>
              <span style="font-size: 11.5px; color: rgba(255,255,255,0.7); text-overflow: ellipsis; overflow: hidden;">${song.album || officialName}</span>
            </div>
            <button class="track-options-btn" style="background: none; border: none; color: rgba(255,255,255,0.6); cursor: pointer; padding: 4px 10px; font-size: 18px; margin-left: auto; flex-shrink: 0;" title="Más opciones">⋮</button>
          `;

          const optsBtn = row.querySelector('.track-options-btn');
          if (optsBtn) {
            optsBtn.addEventListener('click', (e) => {
              e.stopPropagation();
              showTrackOptionsMenu(song, e);
            });
          }

          row.addEventListener('click', () => {
            currentQueue = topSongs;
            loadTrack(idx, true);
            renderQueue();
          });

          songsGrid.appendChild(row);
        });

        songsCol.appendChild(songsGrid);
        splitRow.appendChild(songsCol);
      }

      artistWrapper.appendChild(splitRow);
    }

    // --- Render remaining carousels inside artistWrapper with padding ---
    carousels.forEach(c => {
      const isRelatedArtist = c.title.toLowerCase().includes("similares") 
                           || c.title.toLowerCase().includes("fans") 
                           || c.title.toLowerCase().includes("relacionad") 
                           || c.title.toLowerCase().includes("oyente") 
                           || c.title.toLowerCase().includes("escuchan") 
                           || c.title.toLowerCase().includes("like") 
                           || c.title.toLowerCase().includes("artista")
                           || (c.cards && c.cards.length > 0 && c.cards.every(card => card.type === 'artist'));
      
      const isVideoCarousel = c.title.toLowerCase().includes("video") 
        || c.title.toLowerCase().includes("live") 
        || c.title.toLowerCase().includes("actuaci") 
        || c.title.toLowerCase().includes("directo") 
        || c.title.toLowerCase().includes("concierto");
      
      const carouselSection = document.createElement('div');
      carouselSection.style.padding = "0 32px";
      
      const sectionHeader = document.createElement('div');
      sectionHeader.className = "section-header";
      sectionHeader.style.marginBottom = "14px";
      sectionHeader.innerHTML = `
        <h2 class="section-title-sub" style="font-size: 22px; font-weight: 800; color: white; cursor: pointer;">${c.title} &gt;</h2>
        <div class="carousel-nav">
          <button class="carousel-arrow prev" title="Anterior"><svg viewBox="0 0 24 24" width="16" height="16"><path fill="currentColor" d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/></svg></button>
          <button class="carousel-arrow next" title="Siguiente"><svg viewBox="0 0 24 24" width="16" height="16"><path fill="currentColor" d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/></svg></button>
        </div>
      `;

      // Section header click opens full section detail view fetched from InnerTube
      const titleBtn = sectionHeader.querySelector('h2');
      titleBtn.addEventListener('click', () => {
        pushNavigation({ name: 'section_detail', params: { title: c.title, browseId: c.browseId, params: c.params, items: c.cards, artistName: officialName, isRelatedArtist } });
        loadSectionDetailFromInnerTube(c.title, c.browseId, c.params, c.cards, officialName, isRelatedArtist);
      });

      const trackContainer = document.createElement('div');
      trackContainer.style.display = "flex";
      trackContainer.style.gap = "18px";
      trackContainer.style.overflowX = "auto";
      trackContainer.style.scrollBehavior = "smooth";
      trackContainer.style.paddingBottom = "24px";
      trackContainer.style.scrollbarWidth = "none";

      c.cards.forEach(card => {
        const cardEl = document.createElement('div');
        if (isVideoCarousel) {
          cardEl.style.flex = "0 0 250px";
          cardEl.style.width = "250px";
          cardEl.style.minWidth = "0";
          cardEl.style.maxWidth = "250px";
        } else {
          cardEl.style.flex = "0 0 170px";
          cardEl.style.width = "170px";
          cardEl.style.minWidth = "0";
          cardEl.style.maxWidth = "170px";
        }
        cardEl.style.display = "flex";
        cardEl.style.flexDirection = "column";
        cardEl.style.cursor = "pointer";
        cardEl.style.transition = "transform 0.2s ease";

        cardEl.addEventListener('mouseenter', () => cardEl.style.transform = "translateY(-4px)");
        cardEl.addEventListener('mouseleave', () => cardEl.style.transform = "none");

        if (isRelatedArtist || card.type === 'artist') {
          cardEl.innerHTML = `
            <div style="width: 140px; height: 140px; border-radius: 50%; overflow: hidden; box-shadow: 0 8px 22px rgba(0,0,0,0.4); margin: 0 auto 10px auto;">
              <img src="${upgradeThumbQuality(card.artwork)}" style="width: 100%; height: 100%; object-fit: cover;">
            </div>
            <span style="font-size: 13.5px; font-weight: 700; color: white; text-align: center; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; width: 100%; display: block;">${escapeHtmlAttr(card.title)}</span>
            <span style="font-size: 11px; color: rgba(255,255,255,0.6); text-align: center; text-transform: uppercase; margin-top: 2px; display: block;">${escapeHtmlAttr(card.artist || 'Artista')}</span>
          `;
          cardEl.addEventListener('click', () => loadArtistPage(card.id, card.title));
        } else if (isVideoCarousel) {
          cardEl.innerHTML = `
            <div style="width: 250px; height: 140px; border-radius: 14px; overflow: hidden; box-shadow: 0 8px 22px rgba(0,0,0,0.35); margin-bottom: 8px; background: rgba(0,0,0,0.4); flex-shrink: 0; position: relative;">
              <img src="${card.artwork}" style="width: 100%; height: 100%; object-fit: cover;">
              <div style="position: absolute; inset: 0; background: linear-gradient(to top, rgba(0,0,0,0.45), transparent); display: flex; align-items: center; justify-content: center;">
                <div style="width: 40px; height: 40px; border-radius: 50%; background: rgba(0,0,0,0.7); backdrop-filter: blur(8px); display: flex; align-items: center; justify-content: center; color: white;">
                  <svg viewBox="0 0 24 24" width="20" height="20" style="margin-left: 2px;"><path fill="currentColor" d="M8 5v14l11-7z"/></svg>
                </div>
              </div>
            </div>
            <span style="font-size: 13.5px; font-weight: 700; color: white; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; width: 100%; display: block;">${card.title}</span>
            <span class="artist-link" style="font-size: 11.5px; color: rgba(255,255,255,0.7); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; width: 100%; display: block;" onclick="event.stopPropagation(); loadArtistPage('${card.artistId}', '${card.artist}')">${card.artist || officialName}</span>
          `;

          cardEl.addEventListener('click', () => {
            showVideoPlayerModal(card.id, card.title, card.artist || officialName);
          });
        } else {
          cardEl.innerHTML = `
            <div style="width: 170px; height: 170px; border-radius: 14px; overflow: hidden; box-shadow: 0 8px 22px rgba(0,0,0,0.35); margin-bottom: 8px; background: rgba(0,0,0,0.3); flex-shrink: 0;">
              <img src="${card.artwork}" style="width: 100%; height: 100%; object-fit: cover;">
            </div>
            <span style="font-size: 13.5px; font-weight: 700; color: white; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; width: 100%; display: block;">${card.title}</span>
            <span class="artist-link" style="font-size: 11.5px; color: rgba(255,255,255,0.7); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; width: 100%; display: block;" onclick="event.stopPropagation(); loadArtistPage('${card.artistId}', '${card.artist}')">${card.artist || "Música"}</span>
          `;

          cardEl.addEventListener('click', () => {
            loadPlaylistContents(card.id, card.title);
          });
        }

        trackContainer.appendChild(cardEl);
      });

      const arrowPrev = sectionHeader.querySelector('.carousel-arrow.prev');
      const arrowNext = sectionHeader.querySelector('.carousel-arrow.next');
      arrowPrev.addEventListener('click', () => trackContainer.scrollBy({ left: -400, behavior: 'smooth' }));
      arrowNext.addEventListener('click', () => trackContainer.scrollBy({ left: 400, behavior: 'smooth' }));

      carouselSection.appendChild(sectionHeader);
      carouselSection.appendChild(trackContainer);
      artistWrapper.appendChild(carouselSection);
    });

    contentArea.appendChild(artistWrapper);

  } catch (err) {
    console.error("Artist page load error:", err);
    contentArea.innerHTML = `<p class="error-msg">Error al cargar perfil de artista: ${err.message}</p>`;
  }
}

function showArtistBioModal(artistName, bannerUrl, bioText, subCount) {
  const modalOverlay = document.createElement('div');
  modalOverlay.style.position = "fixed";
  modalOverlay.style.top = "0";
  modalOverlay.style.left = "0";
  modalOverlay.style.width = "100vw";
  modalOverlay.style.height = "100vh";
  modalOverlay.style.backgroundColor = "rgba(0,0,0,0.75)";
  modalOverlay.style.backdropFilter = "blur(18px)";
  modalOverlay.style.zIndex = "10000";
  modalOverlay.style.display = "flex";
  modalOverlay.style.alignItems = "center";
  modalOverlay.style.justifyContent = "center";
  modalOverlay.style.padding = "24px";

  const modalBox = document.createElement('div');
  modalBox.style.width = "100%";
  modalBox.style.maxWidth = "520px";
  modalBox.style.backgroundColor = "#16161a";
  modalBox.style.border = "1px solid rgba(255,255,255,0.12)";
  modalBox.style.borderRadius = "24px";
  modalBox.style.overflow = "hidden";
  modalBox.style.boxShadow = "0 24px 60px rgba(0,0,0,0.8)";
  modalBox.style.display = "flex";
  modalBox.style.flexDirection = "column";

  modalBox.innerHTML = `
    <div style="position: relative; width: 100%; height: 180px; background-image: url('${bannerUrl}'); background-size: cover; background-position: center;">
      <div style="position: absolute; inset: 0; background: linear-gradient(to bottom, rgba(0,0,0,0.1), #16161a);"></div>
      <button id="close-bio-modal" style="position: absolute; top: 14px; right: 14px; background: rgba(0,0,0,0.5); border: none; border-radius: 50%; width: 32px; height: 32px; color: white; cursor: pointer; font-size: 16px; font-weight: 700;">✕</button>
      <div style="position: absolute; bottom: 16px; left: 20px;">
        <h2 style="font-size: 26px; font-weight: 900; color: white; margin: 0;">${artistName}</h2>
        ${subCount ? `<span style="font-size: 12px; color: var(--text-secondary); font-weight: 600;">${subCount}</span>` : ''}
      </div>
    </div>
    <div style="padding: 20px 24px 28px 24px; max-height: 320px; overflow-y: auto;">
      <h3 style="font-size: 14px; font-weight: 800; color: var(--accent-color); text-transform: uppercase; letter-spacing: 0.08em; margin-bottom: 10px;">Biografía e Información</h3>
      <p style="font-size: 13.5px; line-height: 1.6; color: var(--text-primary); white-space: pre-wrap;">${bioText}</p>
    </div>
  `;

  modalOverlay.appendChild(modalBox);
  document.body.appendChild(modalOverlay);

  const closeBtn = modalBox.querySelector('#close-bio-modal');
  closeBtn.addEventListener('click', () => modalOverlay.remove());
  modalOverlay.addEventListener('click', (e) => {
    if (e.target === modalOverlay) modalOverlay.remove();
  });
}

function showVideoPlayerModal(videoId, videoTitle, artistName) {
  const cleanId = videoId ? String(videoId).replace('Video', '') : videoId;
  
  if (isPlaying) {
    pauseTrack();
  }

  const modalOverlay = document.createElement('div');
  modalOverlay.style.position = "fixed";
  modalOverlay.style.top = "0";
  modalOverlay.style.left = "0";
  modalOverlay.style.width = "100vw";
  modalOverlay.style.height = "100vh";
  modalOverlay.style.backgroundColor = "rgba(0,0,0,0.85)";
  modalOverlay.style.backdropFilter = "blur(20px)";
  modalOverlay.style.zIndex = "10000";
  modalOverlay.style.display = "flex";
  modalOverlay.style.flexDirection = "column";
  modalOverlay.style.alignItems = "center";
  modalOverlay.style.justifyContent = "center";
  modalOverlay.style.padding = "24px";
  modalOverlay.style.animation = "fadeIn 0.25s ease-out";

  const modalBox = document.createElement('div');
  modalBox.style.width = "100%";
  modalBox.style.maxWidth = "920px";
  modalBox.style.backgroundColor = "#121215";
  modalBox.style.border = "1px solid rgba(255,255,255,0.15)";
  modalBox.style.borderRadius = "24px";
  modalBox.style.overflow = "hidden";
  modalBox.style.boxShadow = "0 24px 64px rgba(0,0,0,0.85)";
  modalBox.style.display = "flex";
  modalBox.style.flexDirection = "column";

  modalBox.innerHTML = `
    <div style="display: flex; align-items: center; justify-content: space-between; padding: 16px 24px; border-bottom: 1px solid rgba(255,255,255,0.08);">
      <div style="display: flex; flex-direction: column;">
        <h3 style="font-size: 17px; font-weight: 800; color: white; margin: 0 0 2px 0;">${videoTitle}</h3>
        <span style="font-size: 12.5px; color: rgba(255,255,255,0.65);">${artistName || "Video"}</span>
      </div>
      <button id="close-video-modal" style="background: rgba(255,255,255,0.12); border: none; border-radius: 50%; width: 36px; height: 36px; color: white; cursor: pointer; font-size: 18px; font-weight: 700; display: flex; align-items: center; justify-content: center;" title="Cerrar video">✕</button>
    </div>
    <div style="position: relative; width: 100%; aspect-ratio: 16 / 9; background: #000;">
      <iframe src="https://www.youtube.com/embed/${cleanId}?autoplay=1&enablejsapi=1&origin=https://www.youtube.com&widget_referrer=https://www.youtube.com&rel=0&modestbranding=1" 
              style="width: 100%; height: 100%; border: none;" 
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" 
              allowfullscreen>
      </iframe>
    </div>
  `;

  modalOverlay.appendChild(modalBox);
  document.body.appendChild(modalOverlay);

  const closeBtn = modalBox.querySelector('#close-video-modal');
  const closeModal = () => modalOverlay.remove();
  closeBtn.addEventListener('click', closeModal);
  modalOverlay.addEventListener('click', (e) => {
    if (e.target === modalOverlay) closeModal();
  });
}

function renderArtistTopSongs(title, songs) {
  // kept for compatibility if called anywhere
}

// --- Search Implementation ---
function initSearchEvents() {
  if (searchInput) {
    searchInput.addEventListener('input', () => {
      clearTimeout(searchTimeout);
      const query = searchInput.value.trim();
      
      if (query.length === 0) {
        if (searchSuggestions) searchSuggestions.classList.add('hidden');
        return;
      }
      
      searchTimeout = setTimeout(async () => {
        try {
          const data = await callInnerTubeAPI('music/get_search_suggestions', { input: query }, WEB_CONTEXT);
          const suggestions = [];
          const contents = data.contents?.[0]?.searchSuggestionsSectionRenderer?.contents || [];
          
          contents.forEach(section => {
            const item = section.searchSuggestionRenderer;
            if (item && item.navigationEndpoint?.searchEndpoint?.query) {
              suggestions.push(item.navigationEndpoint.searchEndpoint.query);
            }
          });
          
          if (suggestions.length > 0) {
            renderSuggestions(suggestions);
          } else {
            if (searchSuggestions) searchSuggestions.classList.add('hidden');
          }
        } catch (err) {
          if (searchSuggestions) searchSuggestions.classList.add('hidden');
        }
      }, 300);
    });

    searchInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        const query = searchInput.value.trim();
        if (query) performSearch(query);
      }
    });
  }

  document.addEventListener('click', (e) => {
    if (!e.target.closest('.search-box') && !e.target.closest('#search-suggestions')) {
      if (searchSuggestions) searchSuggestions.classList.add('hidden');
    }
  });
}

const DATOS_CATEGORIAS = [
  { "name": "Radio", "url": "https://is1-ssl.mzstatic.com/image/thumb/Features221/v4/e4/6e/84/e46e84fa-1fbf-b795-7cee-9a0f7009040e/99630181-a2c8-46ca-adec-f4a1186a4150.png/290x163sr.webp", "color": "#e60049" },
  { "name": "Conciertos", "url": "https://is1-ssl.mzstatic.com/image/thumb/Features221/v4/20/7a/cd/207acdb0-beaf-2a7b-81c0-643ae3c73bb7/a09e1918-cb88-4b25-9179-f38d15502d22.png/290x163sr.webp", "color": "#800020" },
  { "name": "Éxitos", "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/3e/2e/ed/3e2eeda6-984a-6324-e0cf-7bb576cdd91b/5c1f16e0-8cbb-45ad-9f93-ea48a8ac0cb5.png/290x163sr.webp", "color": "#d4af37" },
  { "name": "Charts", "url": "https://is1-ssl.mzstatic.com/image/thumb/Features221/v4/62/61/c4/6261c465-fe72-eb32-5687-b7c015064b39/77b7bb43-5e28-4c40-afbc-97904ab8636a.png/290x163sr.webp", "color": "#556b2f" },
  { "name": "Hip-hop/rap", "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/29/91/b1/2991b17d-fd89-333b-ec0d-46a3a5a2d5ad/da38fd19-e817-4160-9f7d-9f658a14c26e.png/290x163sr.webp", "color": "#2563eb" },
  { "name": "Latinoamérica", "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/21/3a/a3/213aa346-1bc9-f638-72e4-21c8934c29bf/63a97f49-a153-4727-af5f-bf89c8645806.png/290x163sr.webp", "color": "#ec4899" },
  { "name": "Pop latino", "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/c7/6e/7d/c76e7d19-747d-37a1-c3d1-2f73d66b0b15/4846fcb9-336a-4446-9a6c-debaae640138.png/290x163sr.webp", "color": "#db2777" },
  { "name": "Urbano latino", "url": "https://is1-ssl.mzstatic.com/image/thumb/Features211/v4/2d/ce/81/2dce81e0-6d73-201a-92c1-ed4396aa763d/d9f992ca-60de-4c0b-a693-567fa306cf17.png/290x163sr.webp", "color": "#c026d3" },
  { "name": "Rock y alternativa", "url": "https://is1-ssl.mzstatic.com/image/thumb/Features221/v4/b7/c2/ea/b7c2ea5b-4177-8d09-3739-e8a1edabbbda/d784c33e-831c-4bd1-9855-676d3609e828.png/290x163sr.webp", "color": "#ea580c" },
  { "name": "Pop español", "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/25/56/77/2556772a-667e-4e12-712e-d87210c60310/5b5afe9c-fd0a-4d9a-98f5-db9cee277f12.png/290x163sr.webp", "color": "#f43f5e" },
  { "name": "Rock español", "url": "https://is1-ssl.mzstatic.com/image/thumb/Features211/v4/73/27/4e/73274e21-5f2b-be22-88e7-0b54943dbf03/da87eaf5-916f-49bc-a10a-de5ab11374ff.png/290x163sr.webp", "color": "#c2410c" },
  { "name": "Dance", "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/c1/c0/66/c1c06690-0bcd-d60d-1e3d-a702bad679e9/3c7ab098-157c-45d0-994b-b8d5594f89c5.png/290x163sr.webp", "color": "#059669" },
  { "name": "Rock", "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/be/0b/fc/be0bfc31-14e9-4da3-e895-685523e22d14/ef1fe7da-93cb-4c5f-ba6a-2fd00f238d4e.png/290x163sr.webp", "color": "#dc2626" },
  { "name": "Reggae", "url": "https://is1-ssl.mzstatic.com/image/thumb/Features211/v4/0d/ae/a0/0daea000-e59a-5793-2c38-899c7541f041/b137a695-dd77-4218-8d16-9e933e8fd156.png/290x163sr.webp", "color": "#16a34a" },
  { "name": "Fiesta", "url": "https://is1-ssl.mzstatic.com/image/thumb/Features211/v4/13/12/7d/13127dc4-9c81-099c-31a7-afb849518840/06e568d2-c588-448f-8721-1739e6ac2f2f.png/290x163sr.webp", "color": "#7c3aed" },
  { "name": "Pop", "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/78/6f/74/786f7419-b80c-1fe6-b9f3-b2b7a7532822/f23bd272-c6ad-441c-8a80-82b8c0956954.png/290x163sr.webp", "color": "#f43f5e" },
  { "name": "Chill", "url": "https://is1-ssl.mzstatic.com/image/thumb/Features221/v4/0f/17/d5/0f17d5a3-6774-1ae1-4530-2b694d8fb6bf/d7944211-2928-4ccc-b382-f0564bcf00b2.png/290x163sr.webp", "color": "#3b82f6" },
  { "name": "Amor", "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/64/54/4d/64544d03-52cf-3200-e554-d742dcdaa58b/1792da44-b8f1-4eb7-9f07-af7e01ed7b32.png/290x163sr.webp", "color": "#ec4899" },
  { "name": "Electrónica", "url": "https://is1-ssl.mzstatic.com/image/thumb/Features211/v4/e2/51/f1/e251f15a-36f4-93de-e0e8-ee90de2a4ebc/d763cb91-7a93-4579-9a19-88c122889349.png/290x163sr.webp", "color": "#0d9488" },
  { "name": "Tropical", "url": "https://is1-ssl.mzstatic.com/image/thumb/Features126/v4/8b/4a/5e/8b4a5e8e-8719-51c1-d365-2ca5ba51ee16/0c858686-461b-418b-938e-75dd88c71d5b.png/290x163sr.webp", "color": "#f59e0b" },
  { "name": "Fitness", "url": "https://is1-ssl.mzstatic.com/image/thumb/Features221/v4/cf/2f/ef/cf2fef65-9f86-d368-eedc-d18ad4511069/301c121f-9f4a-4de4-8f2d-f3253ee7fb3a.png/290x163sr.webp", "color": "#ef4444" },
  { "name": "Música mexicana", "url": "https://is1-ssl.mzstatic.com/image/thumb/Features/v4/78/1e/f4/781ef4ea-fdc4-037a-e27b-d33ed99018dd/19efa531-fc16-4a2c-9d46-429a9a5d319e.png/290x163sr.webp", "color": "#10b981" },
  { "name": "Infantil", "url": "https://is1-ssl.mzstatic.com/image/thumb/Features211/v4/d3/94/c0/d394c010-2581-bd86-0a1c-fc0db57dd254/3eae3457-602c-438c-956e-b9f667ddd577.png/290x163sr.webp", "color": "#8b5cf6" },
  { "name": "Para dormir", "url": "https://is1-ssl.mzstatic.com/image/thumb/Features211/v4/66/44/63/6644636a-6134-e464-32e6-a7900d583ce8/bcb74429-1303-4fb0-9c3f-a6f0b87eb86e.png/290x163sr.webp", "color": "#6366f1" },
  { "name": "Videos musicales", "url": "https://is1-ssl.mzstatic.com/image/thumb/Features211/v4/3f/11/61/3f11615b-7131-9d7f-73b8-1dd2be3df94e/a9f44d74-8142-48ab-a39e-018790db37e4.png/290x163sr.webp", "color": "#ff2d55" }
];

const CATEGORIAS_APPLE_DATA = {"Éxitos": {"songs": [{"id": "SzJXikN_4wA", "title": "I Knew It, I Knew You (From \"Toy Story 5\")", "artists": [{"name": "Taylor Swift", "id": "UCPC0L1d253x-KuMNwa05TpA"}], "thumbnail": "https://yt3.googleusercontent.com/gugx1oABoi0MrNgzaLtUJib6Xm44OC8aoAYx66zxLM_N1kG6xT_BUH7IO-0eaFAyQzxk43srK4gW7hip=w60-h60-l90-rj", "explicit": false}, {"id": "x30_IHP1x3s", "title": "Janice STFU", "artists": [{"name": "Drake", "id": "UCU6cE7pdJPc6DU2jSrKEsdQ"}], "thumbnail": "https://yt3.googleusercontent.com/HUyruy7aJyHrNv_ZwxLDgReoxZULjLLvbaU9qARvp9VvZ8LDRA4Qja-ZUFhhwN0wEg-EFKnhdZzGM04m=w60-h60-l90-rj", "explicit": true}, {"id": "GqQIhi86k7M", "title": "Look at My Life", "artists": [{"name": "Gracie Abrams", "id": "UCw-0GSqznYHfyfDBBe6a46A"}], "thumbnail": "https://yt3.googleusercontent.com/lYEW8QSpdpVcS1PISLrsGVGnUHv5R4CjEzRRHvNYwDlamAi3UVdQUxc2lTl2urUOLqTvgQRPEDLhOmwB=w60-h60-l90-rj", "explicit": false}, {"id": "P7VgXIZSN_w", "title": "stupid song", "artists": [{"name": "Olivia Rodrigo", "id": "UCE5XNpliPM-SmyFEp61tL_g"}], "thumbnail": "https://yt3.googleusercontent.com/q0szuVtXvUdftTC8k9fjwazdEpoaCyWTZ1d5Xa3GWHhQPD6_59W_rPlmZRFa2rSFPLTmfOGEgvPfF9uBVg=w60-h60-l90-rj", "explicit": false}, {"id": "7CUz7Ec7cWc", "title": "hate that i made you love me", "artists": [{"name": "Ariana Grande", "id": "UC0076UMUgEng8HORUw_MYHA"}], "thumbnail": "https://yt3.googleusercontent.com/Mq8kh-Qg2QJr9kIjuk25IT2o2Dwyry87xMWt2YV0SOfbjufAu3oZTMigL4LYXx8PbF0WotMBocMPUvSJ=w60-h60-l90-rj", "explicit": false}, {"id": "A0ttKAXj1xU", "title": "Be Her", "artists": [{"name": "Ella Langley", "id": "UCecnyZYofHiBVDJpx1XNYOQ"}], "thumbnail": "https://yt3.googleusercontent.com/6gT896CrrZWfBERXUHSzWE59E6baMQgGUUM63nNVd3JAa7exDIh54e5zPRVWdzOHxa8vJZLyZKwyGOX8Yg=w60-h60-l90-rj", "explicit": false}, {"id": "s5oSscNIyIs", "title": "DAISIES", "artists": [{"name": "Justin Bieber", "id": "UCGvj8kfUV5Q6lzECIrGY19g"}], "thumbnail": "https://yt3.googleusercontent.com/3oJ4mIMkvYQyI_QDVsZYHdBRTGo-7rU4yNCJwIiIDhNGdquiZ9Arg9z_wCo50tUruOQP-fk-a2mgaz0=w60-h60-l90-rj", "explicit": false}, {"id": "fsGjRf-N71I", "title": "Man I Need", "artists": [{"name": "Olivia Dean", "id": "UC6GBKGYX6b3guHlqHLx6IzQ"}], "thumbnail": "https://yt3.googleusercontent.com/dmrLFXnrV4zb8-mfiEMJJrJ5v3DXHrwgPKgISOFb39fsZ39vsLa1lMZTQmo9oxWG7p1V5cK5-51HWXaZug=w60-h60-l90-rj", "explicit": false}], "albums": [{"browseId": "MPREb_tQfaWH32ovE", "playlistId": "OLAK5uy_lqcFZTOPHGwcnP0nYMzNuY0IES0fl7Fe4", "title": "Abbey Road (Super Deluxe Edition)", "artists": [{"name": "The Beatles", "id": "UC2XdaAVUannpujzv32jcouQ"}], "thumbnail": "https://yt3.googleusercontent.com/g8bzAg2zxvdnm7ismLMYLA9-9azb4y6VP2uOF56A2G2rpsqLHT6mrJWXRKq_VttXQZ-o-jmVgTFIVgdj=w60-h60-l90-rj", "year": 1969, "explicit": false}, {"browseId": "MPREb_or9GipYFiAw", "playlistId": "OLAK5uy_kR5nATUuJR0fuXd5XabH2VFwT63oVlaMM", "title": "Blonde", "artists": [{"name": "Frank Ocean", "id": "UCETYiBLjt2v-pcKSgf8pe6g"}], "thumbnail": "https://yt3.googleusercontent.com/TWBi2M7D8gIwoo3NmhGfoVKI-PuzDunLVYpmLCbeP8Uw2YWpnjttlxmVvpVaO8uSjmLPjHgy6iGXxlPF=w60-h60-l90-rj", "year": 2016, "explicit": false}, {"browseId": "MPREb_UveM5rGdTAx", "playlistId": "OLAK5uy_mmSLtHX8RxMd-Y8Kp9h44prOMFQLYou7M", "title": "Songs In The Key Of Life", "artists": [{"name": "Stevie Wonder", "id": "UCR83RRXHzXrEQbu_ZQmPMSg"}], "thumbnail": "https://yt3.googleusercontent.com/QPjPdnQN2wTIqNhE6Eg0Bq2QCKht67LoQxXtOBC7ExR4GAk6a26woMHgw3cHSQL9qDCQ8Qs3ajtL3wg=w60-h60-l90-rj", "year": 1976, "explicit": false}, {"browseId": "MPREb_qYhF5ZzV2ws", "playlistId": "OLAK5uy_nwQa0ANAJciFXDP80oAntq2nOpxmXvKhw", "title": "The Miseducation of Lauryn Hill", "artists": [{"name": "Lauryn Hill", "id": "UC3j9UpJkGNY9GDm3EdbEiVA"}], "thumbnail": "https://yt3.googleusercontent.com/kzP53b9Oive-yiNndGp2YBcqZOfzE-CTymboQI0jkkDqBP789RNhTqIu07GXxdYgsUDYLPFqlWhwJtY=w60-h60-l90-rj", "year": 1998, "explicit": false}, {"browseId": "MPREb_dqWTncCjkSp", "playlistId": "OLAK5uy_l1U925dsiDi2DqlG-KCbODG6BaibpxbQE", "title": "Thriller", "artists": [{"name": "Michael Jackson", "id": "UCoIOOL7QKuBhQHVKL8y7BEQ"}], "thumbnail": "https://yt3.googleusercontent.com/URvHCfI2iyGAlAwqqBFeaFhU9DeKk_iuX40OIIIj8Zp0wIT3BVsJ2JRMwLLbUB9EZS7t7oDlMrI2S3OvGA=w60-h60-l90-rj", "year": 1982, "explicit": false}, {"browseId": "MPREb_clp7eWySHQf", "playlistId": "OLAK5uy_nSFpJd6fk5g2u7CcljXZCqauq_CHCoP58", "title": "good kid, m.A.A.d. city", "artists": [{"name": "Kendrick Lamar", "id": "UCprAFmT0C6O4X0ToEXpeFTQ"}], "thumbnail": "https://yt3.googleusercontent.com/Fz9_8koA1VbRz51kyUaOHIVDQu7LCx2W0lDjytEXz4KPGL3VIV5LS2F0uISIHHCvqQpbgHl3oCWIG6I=w60-h60-l90-rj", "year": 2012, "explicit": false}], "playlists": [{"id": "RDCLAK5uy_l2CXCpt8bt8t2IQ_6q0M3RuBdk2rxDqJE", "title": "Puras Románticas", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/0YD6CGQIMrfwWyh93u_GeG4KU9TxFXe1yghgtP4pyvJ1QHTcEyOKhfCTFIYGFH6yAJKqBdNYBL8uiMk=w544-h544-l90-rj"}], "artists": [{"id": "UC6UAOxnKL0yu0ge9WhF9Wtg", "title": "Yung Miami", "thumbnail": "https://yt3.googleusercontent.com/FDZu_c2Qam5qkgUB8ZEf2SXP9cZkChlXSxn5hdY3g89nsPqJW_KH-S-ukBShQbT9IIpgnT7xq6179TRK=w120-h120-p-l90-rj"}, {"id": "UCecnyZYofHiBVDJpx1XNYOQ", "title": "Ella Langley", "thumbnail": "https://lh3.googleusercontent.com/hiPrNPmBjTnGntY7hab2LalE5ezJtYCmM-dKq7_fsaItDDoScebnilCo2nPC98o2QL8oGdgPGiRewjHl=w120-h120-p-l90-rj"}, {"id": "UCU6cE7pdJPc6DU2jSrKEsdQ", "title": "Drake", "thumbnail": "https://yt3.googleusercontent.com/MxNjcRJ-uK4Xvx7u90IhEFLQM8x9LIGTA9VCKHq5U4Wn2jOgiWaMtg-qz329SIzqnCyhdCCB3MpdAGs=w120-h120-p-l90-rj"}, {"id": "UCPC0L1d253x-KuMNwa05TpA", "title": "Taylor Swift", "thumbnail": "https://yt3.googleusercontent.com/RCpTA6EXJQyjVFDosWOKa2SMmqkua_lA9mHPDWWciLwgqpZLz-k8rXWRF_367trrQ7up9BUwCbk6kRk=w120-h120-p-l90-rj"}, {"id": "UCE5XNpliPM-SmyFEp61tL_g", "title": "Olivia Rodrigo", "thumbnail": "https://yt3.googleusercontent.com/41-4WZupE4yY88igineZefzBZ3ud2nrtlBMv61OBWOfOcATol8PhmI5OZ0fLlrTszyZ3Ul9I9sE=w120-h120-l90-rj-dcqUWI7R0J"}, {"id": "UC0076UMUgEng8HORUw_MYHA", "title": "Ariana Grande", "thumbnail": "https://yt3.googleusercontent.com/DU6Kpr5TYKcW6QHvMnsJau5_8QSuix8LCLtf5UEaziZZdXw8SxvcxJ9YWmVIQuzhg2R-MVHYgjdGCQ=w120-h120-p-l90-rj"}]}, "Pop latino": {"songs": [{"id": "0fwCZhmt8cU", "title": "MOVE YOUR CADERA", "artists": [{"name": "Kat DeLuna", "id": "UCHrwrdAetFeJbHpPTDeEQJA"}], "thumbnail": "https://yt3.googleusercontent.com/b8pQm1E4R6e67T8nkBgp6Ic2uu8j4lzd0-VpsDs6efG5vudyLCmJ97Sgq89CJJhLT7wdD0GnI7J3QHd1HQ=w60-h60-l90-rj", "explicit": false}, {"id": "DjqIuXEs1N4", "title": "SI SE ACABA EL MUNDO", "artists": [{"name": "DANNA", "id": "UCu4o_5UkGmx3vyqRkk9kVOQ"}, {"name": "y", "id": null}, {"name": "El Malilla", "id": "UCz0CQ5kSCMG1zXNDQR5jWgQ"}], "thumbnail": "https://yt3.googleusercontent.com/HUsH6fG-Vh5sQxxp2_q7G4HrIusrZ1D-K38qMYaT84ju3cjtJhzSrHtrRu_sRtmWLLvW3NKmGg0IyGNl=w60-h60-l90-rj", "explicit": false}, {"id": "kGjNdQxeAoA", "title": "Bandido Estrella", "artists": [{"name": "Jasiel Nuñez", "id": "UCf-KzXFKLRFmK5G2QwNDRaA"}], "thumbnail": "https://yt3.googleusercontent.com/Zup5mKNZEsZRK0ooTA607V_cS_hjPjMx5g1xQIVfxZUaVzAjXETMGYZNTTHPqGLjMPqx0wYKhpWU9Ki7=w60-h60-l90-rj", "explicit": false}, {"id": "roUyOFwVeWA", "title": "PATRONA", "artists": [{"name": "Becky G", "id": "UC3UkDuAQjoRvTH7OEWm3cHQ"}], "thumbnail": "https://yt3.googleusercontent.com/95prktbU4qey6X1FrSN_vuDha4DxAWQhwpa5I7oZTZTLEpss7oXesE3u4jOilDdeYeDfPkDYhPmLLY4=w60-h60-l90-rj", "explicit": true}, {"id": "OYg6oRNjeFA", "title": "Polos Opuestos", "artists": [{"name": "Abraham Vazquez", "id": "UCIQGM9nwnT0XtRg0-Gl9o1A"}, {"name": "y", "id": null}, {"name": "Jay Silva", "id": "UCxbTNlZbIpVSeqEtHCBXytw"}], "thumbnail": "https://yt3.googleusercontent.com/el1zkeNXsgMlJFlSueO6GGLfJwQcx8p9UwMAOs9EX6v63di38e1iobUx0W5Lf58v_OYd8Ss41KXrlLA=w60-h60-l90-rj", "explicit": true}, {"id": "EaeicSboXCo", "title": "niu!", "artists": [{"name": "Plastikboy", "id": "UCNVJZmDX_v0wbUMmAQv7jdQ"}], "thumbnail": "https://yt3.googleusercontent.com/GH5Ojm4NpsADc3hr1NUoOV-Pt8lB2VbFIdd7IlZS-b5uhO4ANezhHuEy9Sb6UmK6_IsFwgv9KjweWjk=w60-h60-l90-rj", "explicit": false}, {"id": "rnRWjHHe7xw", "title": "mi yo del pasado", "artists": [{"name": "Mayte Meyer", "id": "UCeBFArOEWZgsy_CfU4hPfSg"}], "thumbnail": "https://yt3.googleusercontent.com/NaWcNicOuZ3nGFX2itmlxQgpV-mICvxfHeHt4fP6Y_Ex05QAqoUOz27Iawqvk43VftDsnEI9t1WbdKxR=w60-h60-l90-rj", "explicit": false}, {"id": "2g66cAvNTaA", "title": "La Mala", "artists": [{"name": "Rahzort", "id": "UCvQq8mGMVBHoqrNOIms2sAQ"}, {"name": "y", "id": null}, {"name": "lirah", "id": "UCRO5hF1FFojZdFyrPK82DKA"}], "thumbnail": "https://yt3.googleusercontent.com/yR7Zd7aEUsLPrJcGHjinGClQNtaa2hNRSCgw6Cc_3bnml7UErDGkLnoBVzLNst16mycIz5n_dGIN_53fNw=w60-h60-l90-rj", "explicit": false}], "albums": [{"browseId": "MPREb_NVl6S3DHnR0", "playlistId": "OLAK5uy_n8O29QQJcO-aQA60_RUAH2n-iVBS4oks8", "title": "Pies Descalzos", "artists": [{"name": "Shakira", "id": "UCo6JijJGA3IvIiPsawDK3Ww"}], "thumbnail": "https://yt3.googleusercontent.com/B0PCk94-CUI6ei2dPAHwZoTb9K6g9tdH8djhnljDp7Dqh9Wkj7_gzzitYKsVx6p7CkPIRHm4SFpZEY2S7g=w60-h60-l90-rj", "year": 1995, "explicit": false}, {"browseId": "MPREb_5kpqCBbpIQM", "playlistId": "OLAK5uy_ll88ZdILro5nB98id1JwIPnVXv5_S4FwI", "title": "Planet Pit", "artists": [{"name": "Pitbull", "id": "UC03jIQv4WXBSHdr1DlCLYDw"}], "thumbnail": "https://yt3.googleusercontent.com/YkmX5fO5CPNxzLW2SJJT_BtXrUUTBzxEu2bXWPDjgSeaSzrNd2ZmRZGfoX-Krj-rIZOgO7sh8_Wa84dt=w60-h60-l90-rj", "year": 2011, "explicit": false}, {"browseId": "MPREb_gMFL4cDOQrV", "playlistId": "OLAK5uy_mINKEKb5y0kkGJhlmdsONN-gWHiZa4XIk", "title": "Hasta Ahora", "artists": [{"name": "Sin Bandera", "id": "UCGT2i46yvu0gfp75nR9c86A"}], "thumbnail": "https://yt3.googleusercontent.com/IVJcqadBvdMQXg0dnoBoO0ZU1Bg-V39vmFMTE_BuPAU3lTSmptKCDqi2oDDZ21_uEjzVVnqnCwzaGlI=w60-h60-s-l90-rj", "year": 2007, "explicit": false}, {"browseId": "MPREb_9IJposUHFTQ", "playlistId": "OLAK5uy_kVplzS99MBzDv3s5VaPe4PFnuruBsj8No", "title": "A Medio Vivir", "artists": [{"name": "Ricky Martin", "id": "UC2rtpKv9X9pfwCuaEt9wsrQ"}], "thumbnail": "https://yt3.googleusercontent.com/9is-kaxW10W65aWJjoR3S8tNzgzpjYrv2oNY9EAmJ9HAr0_s-YdJbH93cax7ETSULZU7BxnDfX3u8yHL=w60-h60-l90-rj", "year": 1995, "explicit": false}, {"browseId": "MPREb_wy06u4j760D", "playlistId": "OLAK5uy_mSMNB_f_eVyZmllPx9LwSm5DHUkADoFR8", "title": "Vibras", "artists": [{"name": "J Balvin", "id": "UCWw-Guyr5ul9B-d5kJlHMng"}], "thumbnail": "https://yt3.googleusercontent.com/go4837lx-jM17uwyGdk3b7ygdlRr44NiUuw-AkYt3_tS1b9PmCkz7hhW4ZNArhIfblYsrxLdXumo1jMO=w60-h60-l90-rj", "year": 2018, "explicit": false}, {"browseId": "MPREb_wAgnDB2qDxW", "playlistId": "OLAK5uy_kjFvkkfjwLvZP6EqKuzuLpz8-leZ1Z508", "title": "Quisiera", "artists": [{"name": "Emmanuel", "id": "UCc_d3TKVu75UR8hlHFHCLVA"}], "thumbnail": "https://yt3.googleusercontent.com/uh6DOzKlx_hUzZprl8zvjR5_QJ84QorwNJoePjyy0tSwHl7IWgQboyOy3OMns4W0UdtUO_nUOO9m_J4=w60-h60-l90-rj", "year": 1988, "explicit": false}], "playlists": [{"id": "RDCLAK5uy_l2CXCpt8bt8t2IQ_6q0M3RuBdk2rxDqJE", "title": "Puras Románticas", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/0YD6CGQIMrfwWyh93u_GeG4KU9TxFXe1yghgtP4pyvJ1QHTcEyOKhfCTFIYGFH6yAJKqBdNYBL8uiMk=w544-h544-l90-rj"}, {"id": "RDCLAK5uy_kIMBahqfKbTGdP4KcQ8Q7U2OB9Z2xkDcI", "title": "Divas del Pop Latino", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/L2WIV7u7xRn2wtaIwmBHacQqo-E-nvsS_gOer1foQ1pnwDdohkipRL48AfUSv1huH8hqcwlnO5sSyw=w544-h544-l90-rj"}, {"id": "RDCLAK5uy_kPR9usfd5aQ8n4rTcEv7y1vMm1ewig4", "title": "Diversión en familia", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/B0xOeOhbTHesdcmH7ozS_8ERBOfOlx3TnRXfverufHrIWolc9OHVvs1xJ5MT1fn86__bP_1cza8Vhlw=w544-h544-l90-rj"}], "artists": [{"id": "UCreXLd0FaRP_tZDGKY2mCAA", "title": "Erick Brian Colón", "thumbnail": "https://yt3.googleusercontent.com/DMyrg9PAl7kNu-dUZZEHbez602XfLS8QQzjrKyiCBmCuooHlXXxbMlVXMXAX3_d3aOrHYLQy=w120-h120-l90-rj"}, {"id": "UCfh2j2Dq-aSeLhzuPOsnhVg", "title": "Manuel Turizo", "thumbnail": "https://yt3.googleusercontent.com/3upaCYig0kzy9Gjac_fk98oPieTY6DfbcGzq2Oqhk9jtzZJh5JT58pShM6SxYixSEUQZruSpJrPlAA=w120-h120-p-l90-rj"}, {"id": "UC7n3gWRN0vQzgiOKc51aZ4w", "title": "KAROL G", "thumbnail": "https://lh3.googleusercontent.com/Yzqsh_83kXz4dl0dvPlHTFeQjAkKbX3u-TV-9I7mo-bWm4sCKiHcCBTis2GculkVVNGG76Wwk3uQePeD=w120-h120-p-l90-rj"}, {"id": "UC6ty97iiG-_Wiu-QyFohkKw", "title": "Kenia Os", "thumbnail": "https://yt3.googleusercontent.com/9dyh1hCCphhPMxLHMRv4bzx3AwnkpwFULafh8hNdlCRVrF6jLAv4Q7O1daaTHQ-of-sLxf-euWOyrAL8=w120-h120-p-l90-rj"}, {"id": "UCtKIuEMDzPlUmPgdwBhiGcg", "title": "Maluma", "thumbnail": "https://lh3.googleusercontent.com/OSWSTFd1Ql4lytHxgXtsPTY_0L94Kj55SJcELq64ddGO_DBcShsqXzqw4edEBgAi4-Ls7cry8q5r0v8=w120-h120-p-l90-rj"}, {"id": "UCpjGA1q99cLKEvKlULb1Llw", "title": "ROBI", "thumbnail": "https://lh3.googleusercontent.com/iLtxuV2NJsYUC2q9kdrQnu2V2SKdvtUSilluG9ocAYpGVnUW6b-oGM3JAdyaaic7B72yzHl7zCfk_7I=w120-h120-p-l90-rj"}]}, "Radio": {"songs": [], "albums": [], "playlists": [], "artists": [{"id": "UCn-MpywoDcRk60F-EbyR8Nw", "title": "Pheelz", "thumbnail": "https://lh3.googleusercontent.com/LdUgrbPM89t4mTPbo6oB2ILSSORIJa8Iu6S5hls7DjShZbtWVcpTLowiCcUA9RyVdzxLjRxUimrWtzI=w120-h120-p-l90-rj"}, {"id": "UCPOrBbGGWc5SSg_crPXIPUg", "title": "Fancy Hagood", "thumbnail": "https://yt3.googleusercontent.com/mT3ISHrq83Icv1ARdoZGsGE4UNCSZg-YZn-wNIa7OyF1OO_ZbYBrwJOzzD9ppJyUA92vzndyN9c=w120-h120-l90-rj"}, {"id": "UCmhSvQ8-TnW04vqFAuDZ7HA", "title": "Justin Tranter", "thumbnail": "https://yt3.ggpht.com/ytc/AIdro_lKslHTob0gmE9xOxp8pg8vcpuaethi5WQ-vvuZWd7P0qE=w120-h120-l90-rj-dcOVOUUaUH"}, {"id": "UCoHRmfxqEHTHoNTddLS3ErQ", "title": "Álvaro Díaz", "thumbnail": "https://lh3.googleusercontent.com/NJy0iS3JJWFmWceHeG0tPdjfkw_KCRCHkqVulZB3VuaW_bytEw9Q37fyI-rDjce--BtKbEtgNFCJQdo=w120-h120-l90-rj"}, {"id": "UC5DfLsHxKQSeD4T1CiVIFXQ", "title": "Tyler White", "thumbnail": "https://lh3.googleusercontent.com/ydShAhqNIJauyRV1rvuZPJp6p4UFT_TtLiLQ7aZ4IDEgXcAUAKwkdxVBOBTV0DijMXYXF7gSR3tkng=w120-h120-p-l90-rj"}, {"id": "UCf5GUgSQC4CU8idz_OH8jqQ", "title": "SIENNA SPIRO", "thumbnail": "https://yt3.googleusercontent.com/7TK-PpocD_ql32RKgB9okRhGOVyA4aKAehiM8l1QxXqjrs8b6x_0FPcCIMFK0CpMuf46e04B3A=w120-h120-l90-rj"}]}, "Urbano latino": {"songs": [{"id": "5xJP3p4LSKw", "title": "Las mas bonitas son p#tas", "artists": [{"name": "Anuel AA", "id": "UCbBaYg2UToDaoOwo-R6xi4g"}], "thumbnail": "https://yt3.googleusercontent.com/iSX0Ox29sf_SpZMr51jUbgHDKsvqQ-54KksqIlFADT9i-nFG590d6D-z61kYxMKF7r5lHX7wWvMjdqbA=w60-h60-l90-rj", "explicit": true}, {"id": "_s3PWh0hJp8", "title": "After", "artists": [{"name": "Conep", "id": "UCcL6TjmX01Db1p_e6jkSkBQ"}], "thumbnail": "https://yt3.googleusercontent.com/Njvomoi_tPvfufJKJNE7FtcE-hX3ggV7WdtSOdi4zgknQmFe1cyMRngnxRgk9Ulnmq_mNOc8laWxyMmg=w60-h60-l90-rj", "explicit": true}, {"id": "6As-FdaTwfc", "title": "¿SERÁ EL ALCOHOL?", "artists": [{"name": "Oscar Maydon", "id": "UCxTyOt1nVVexDlQub6kvvNQ"}, {"name": "y", "id": null}, {"name": "Omar Camacho", "id": "UC5TYeA9Hw3L5VlZYtMt3pqg"}], "thumbnail": "https://yt3.googleusercontent.com/gGH8PDWDXL_onkea-afVVeNoKFbpMDC0dxuDadkVjyVPD1H-GfxZvuQfVa0zvg29rwLcibCvH5FK7us=w60-h60-l90-rj", "explicit": true}, {"id": "OsMUzqnrCEQ", "title": "Pensando En Ti", "artists": [{"name": "Xavi", "id": "UCfmeXjlCXi37LGF7O2VT2zA"}, {"name": "y", "id": null}, {"name": "De La Rose", "id": "UCkUHeLHwch0QrYQ3X1wLfzg"}], "thumbnail": "https://yt3.googleusercontent.com/n-fu_8u8xeMO0oJFnL0inHmX79rw8nMcAE-vaiwAY3hSPGWd0sObFIXkJrVbMaNx0kI4j7WRB53pqGQx=w60-h60-l90-rj", "explicit": false}, {"id": "8FLFzOmsb88", "title": "Myke Towers: Bzrp Music Sessions, Vol. 42/66", "artists": [{"name": "Bizarrap", "id": "UCONiUl5u7y2bMaVZJcuRDEQ"}, {"name": "y", "id": null}, {"name": "Myke Towers", "id": "UCYPsIfSIEwWcoynHBP5k1dg"}], "thumbnail": "https://yt3.googleusercontent.com/8QArS95hf1W14AHHEp1sazJrZx63TOm4yfvutbWd7wibAgLn_0_ogqb0X0WHhqrdPjZjD49PYtVRatwD=w60-h60-l90-rj", "explicit": false}, {"id": "1yiCb2TK81Q", "title": "Quiero Verte", "artists": [{"name": "Gaby Music", "id": "UCVAtSz_69M2ULexewXFrB_g"}, {"name": "Yandel", "id": "UCc1QpDE0iT0n6ZLckjflNHw"}, {"name": "El Bogueto", "id": "UC8r_j-qnSCj1t4h6EVN5TrQ"}, {"name": "y", "id": null}, {"name": "Luis R Conriquez", "id": "UCm7mbvc5QOn5JzkeCrk-wdw"}], "thumbnail": "https://yt3.googleusercontent.com/HeAVDKa-jXyPk09oirBOwbhg6f3Nh4mAV2l8p2ad2LtM4Cbtoof-Z8JXhVTAVsV2ju5p1fbvyy7qujUH=w60-h60-l90-rj", "explicit": true}, {"id": "bGo20gg0BF8", "title": "ROMO", "artists": [{"name": "Chimbala", "id": "UCj36ACUuHAPPesaM_ydfrxw"}, {"name": "y", "id": null}, {"name": "J Balvin", "id": "UCWw-Guyr5ul9B-d5kJlHMng"}], "thumbnail": "https://yt3.googleusercontent.com/9JrZDa_aFhRH4NmGaWfVFaLzeUBpRIEP2QDya0fc5Cb1ZwNx07MTq2rOVdV2HvKYEJPhiXQcTT8IErrUCg=w60-h60-l90-rj", "explicit": false}, {"id": "wCO96jn7DHE", "title": "Estoy en mí", "artists": [{"name": "Los Hitmen", "id": "UCZ6JZbFFbUdhVYWkieIuAZA"}, {"name": "y", "id": null}, {"name": "DIA", "id": "UCjym4EsB0WvKNNS4kfou2QQ"}], "thumbnail": "https://yt3.googleusercontent.com/KhmGX5bQdKJB2MWRVnsvUs8gSBEf3LvNhDoDrtcHb-VgOUZHffN9yK_IcGsjFmp15W8aLvhDs3YoVbpBXg=w60-h60-l90-rj", "explicit": true}], "albums": [{"browseId": "MPREb_TIWuPsLzHis", "playlistId": "OLAK5uy_lgK3pyEHNWzPq9S1_lN2_9Vxza9BK6_rA", "title": "Sueños", "artists": [{"name": "Sech", "id": "UCXhOBsxGZ3TkeaYIW5LAHfA"}], "thumbnail": "https://yt3.googleusercontent.com/WkhkB14WOYDe538_pawcZDh975HD9Xed5TWjuULM_BtT0r8bjEtkWgCynGOTEH7Tni0vQdIGDtTcEgJBaw=w60-h60-l90-rj", "year": 2019, "explicit": false}, {"browseId": "MPREb_wy06u4j760D", "playlistId": "OLAK5uy_mSMNB_f_eVyZmllPx9LwSm5DHUkADoFR8", "title": "Vibras", "artists": [{"name": "J Balvin", "id": "UCWw-Guyr5ul9B-d5kJlHMng"}], "thumbnail": "https://yt3.googleusercontent.com/go4837lx-jM17uwyGdk3b7ygdlRr44NiUuw-AkYt3_tS1b9PmCkz7hhW4ZNArhIfblYsrxLdXumo1jMO=w60-h60-l90-rj", "year": 2018, "explicit": false}, {"browseId": "MPREb_pOD9fgSpE2e", "playlistId": "OLAK5uy_nDCKBMpaMTYjj1HeWnltHV-mvQ8U_QTc0", "title": "X 100PRE", "artists": [{"name": "Bad Bunny", "id": "UCiY3z8HAGD6BlSNKVn2kSvQ"}], "thumbnail": "https://yt3.googleusercontent.com/gCb7PCwLzDCQZwC40gIkni0sk-Bo1phK8k73qjmY_cGs9hoM-J7ZceSakSGo7F95NGOBy40Zw7xGtcGiBg=w60-h60-l90-rj", "year": 2018, "explicit": false}, {"browseId": "MPREb_uOF6j15SzFj", "playlistId": "OLAK5uy_nE4l0_V5m2EoH0FaaN3UxaeN_I8iLJZvU", "title": "Fénix", "artists": [{"name": "Nicky Jam", "id": "UCMJ705HLB_EflTzWDAjcKSQ"}], "thumbnail": "https://yt3.googleusercontent.com/XIh7ZQ331WBNELdsoVstD6-py5Co1Hneb_M0CSAIJ_9iuS989k1Mtocw27jcw64y_HmWdGJobFovUnSFKA=w60-h60-l90-rj", "year": 2017, "explicit": false}, {"browseId": "MPREb_om6H5ndOkGP", "playlistId": "OLAK5uy_njdFU8BeNcsat_rj-jc4MMbecFW7izqH0", "title": "Aura", "artists": [{"name": "Ozuna", "id": "UCKEFjh4JL-OyMI8z3h5Coaw"}], "thumbnail": "https://yt3.googleusercontent.com/SZo8jCMIQ4EDFp52J_UQUqebCOuZCQySYj-S9eiuaJvQw05Gpiq_eeEMSbL2foSUcngbSR5_UIP5GJGJ=w60-h60-l90-rj", "year": 2018, "explicit": false}, {"browseId": "MPREb_qdf0sAvVWoh", "playlistId": "OLAK5uy_kQ0AoJdc_0oZHQn01MSOold3bCaVP6HxM", "title": "OASIS", "artists": [{"name": "J Balvin", "id": "UCWw-Guyr5ul9B-d5kJlHMng"}, {"name": "y", "id": null}, {"name": "Bad Bunny", "id": "UCiY3z8HAGD6BlSNKVn2kSvQ"}], "thumbnail": "https://yt3.googleusercontent.com/Rw2pqT1wp-Qp1J-Mex8gS1z1OZEy9fFF-OL4gULrMD9aW64bRkikpKr9qLcJeJK_ktsobZk57ZUjO7Br=w60-h60-l90-rj", "year": 2019, "explicit": false}], "playlists": [], "artists": [{"id": "UC7YJ69rJ9qtVh6SjYFs4cGg", "title": "Eladio Carrion", "thumbnail": "https://yt3.googleusercontent.com/fg0iRY24kWHDSEUX7r5fBnEkqd7zvQuit0Pb9dOgtret1JBhJq8q2byrZice5VN1V_5qB4patBXr0Q=w120-h120-p-l90-rj"}, {"id": "UChZEzxer-QvRMbADJJGOMuQ", "title": "Jhayco", "thumbnail": "https://lh3.googleusercontent.com/ueSDsUyfIfao1LDeCGlV6LqSFrfQ-V7cWvdK96Mrtbjhpt9M6wzKzClTOLlXXu1gprxgmV-g9dy8IRo=w120-h120-p-l90-rj"}, {"id": "UCc3e8O2V5_7OA300ursDyFQ", "title": "Feid", "thumbnail": "https://yt3.googleusercontent.com/CJzeR5n7JZJgXHfR3BdlKMMV2_lRq_WRoxBT0dcOZVjx4bDhCvrrmjQiIPSSkSUUhjKNNUWFFm90kdHq=w120-h120-p-l90-rj"}, {"id": "UCieB8sWI6qEOX6r_UnJU12w", "title": "Omar Courtz", "thumbnail": "https://lh3.googleusercontent.com/Pv9rzAkKxKnCVe0AMsldl2m5vTXw1dJCABk8tuCUZqfDO2JDUVLW5D4mlx6Pb6j0_UYt_PiGgr8juC4=w120-h120-p-l90-rj"}, {"id": "UC2-PV0lS78r65j_3f5_vPvA", "title": "De La Rose", "thumbnail": "https://yt3.googleusercontent.com/wIAQb8i0YkTlizpJwaw9LU-t0i6YyuOTciglVgFIck8vg-8sYf4WVlnXCk8B7HT-3neFXbwA_A=w120-h120-l90-rj-dcLVWQriEI"}, {"id": "UCBQceJ-wnQt_lz0yaDA8dLA", "title": "Clarent", "thumbnail": "https://yt3.googleusercontent.com/uqpkGaHqbFRmHmt0vs6VlTvyJH8ur8yi8uSVb5LmTrkwj8P2ZDXzox-F4n5aa4bjUJQU-c-B=w120-h120-l90-rj"}]}, "Orgullo LGBTTTIQ+": {"songs": [{"id": "CZ8LXnGeT4E", "title": "Justin", "artists": [{"name": "And", "id": null}], "thumbnail": "https://yt3.googleusercontent.com/Sxi8PL5GQd585wFlURXkz01LGhAVhRLnjCpdROEZd2Q03dZQjKNodrjTfXeyYqDHtTFA66PILcqUDIw=w60-h60-l90-rj", "explicit": false}, {"id": "ItiKVU7wOdM", "title": "Happier?", "artists": [{"name": "Teddy Geiger", "id": "UCXxtAbFhFFNIjY_uQEV96iw"}], "thumbnail": "https://yt3.googleusercontent.com/SDT3qL8n_mrGzdXkWrKaqRJzZbnEkfKMGQY6-2FdHQE5b0cJC_Zndo79bDOvvad4sIG9AJV8wsjlwlc=w60-h60-l90-rj", "explicit": false}, {"id": "v9FJXf0p0TU", "title": "Beautiful", "artists": [{"name": "Linda Perry", "id": "UCUmMvO-ND0F0h6KKS8AsGqg"}], "thumbnail": "https://yt3.googleusercontent.com/8E1fArtE1jLufN2vyp_7Nq7Nf752Xk48bfmb356w2pa6yj3zCXB_pbNasE68BKmNS-TIwqhXW17jP0svSQ=w60-h60-l90-rj", "explicit": false}, {"id": "3JJBR5Nwel4", "title": "The Gift of Life", "artists": [{"name": "Desmond Child", "id": "UCdBOOseWRiioKBpBY8Uws4g"}], "thumbnail": "https://yt3.googleusercontent.com/2Otm_t1K-ciIa_ekjipff82HsIz1PjMsg9TUSXpY9SUyZ0zD64T2QHfLwII40I0U2DEs_XTcjUPqqSs=w60-h60-l90-rj", "explicit": false}, {"id": "kSLz8C5k2sQ", "title": "REVERSE!!", "artists": [{"name": "MNEK", "id": "UCme53aQ995Cvswk-Icd_IfA"}], "thumbnail": "https://yt3.googleusercontent.com/6bvsQWxG03SW8wQh0IpRepno0aFVSPTxT_5DPhX_5ZiW2nnMBs_GOIojCLHu-yPj2SqAn3FafBe5LPgIgA=w60-h60-l90-rj", "explicit": true}, {"id": "xdTibWIuapM", "title": "Say Something (with House Gospel Choir) (From the Infinite Disco Livestream)", "artists": [{"name": "Kylie Minogue", "id": "UCSyCovCbUAnejYPJhvaP7sg"}], "thumbnail": "https://yt3.googleusercontent.com/Miykz7-DBSZLpqqY7USpiturZpNtX-Yfk78WkJcChIBFXsgSg5ryvJqKudig88qZ8BJASQe8DAMFteg=w60-h60-l90-rj", "explicit": false}, {"id": "m3sWgMNGuIs", "title": "JON", "artists": [{"name": "Jon Appleton", "id": "UCxWRHWHVs5YZFCjPjnJ-dxA"}, {"name": "y", "id": null}, {"name": "Don Cherry", "id": "UC19jcAPWt1K_L_YK4LZov4g"}], "thumbnail": "https://yt3.googleusercontent.com/HF8gmvn9n-Zx2NK6ARd03imOFxeL-i90MfszYc_fq1rqupTLRkrZl4yMncX0ZmfD8e5dcTsQi7R3eq2M=w60-h60-l90-rj", "explicit": false}, {"id": "J1VcKHJTjaU", "title": "Sophie", "artists": [{"name": "Morgan Willis", "id": "UC9Wejw_F1SZKb4DrA7ghgFw"}], "thumbnail": "https://yt3.googleusercontent.com/gqys099qvtydwzr5q-UZlY7oyyQsi1ildNv00Tp-AvkjOIL4yK31LKGroZrdgEPSp-54qBUIF4s4_ZI=w60-h60-l90-rj", "explicit": false}], "albums": [{"browseId": "MPREb_qdYy9HDWvK0", "playlistId": "OLAK5uy_lozN3cum21ncpMIc981BYa-ysMBN0uANk", "title": "Rebeldes", "artists": [{"name": "Alex Anwandter", "id": "UCMi5JtrFV9TFur0XAvau5EA"}], "thumbnail": "https://yt3.googleusercontent.com/_kM_Ifou7hBNC1RjkJJRPZHO_LvshQirFaisvt3MxHP1e45VsnJDVqI5zq6Hzo-u1909Lfqg_eNT7_qf=w60-h60-l90-rj", "year": 2011, "explicit": false}, {"browseId": "MPREb_SwCOdbW6mAv", "playlistId": "OLAK5uy_kFBOHamyyAMxSn22G7yNTT-4ntBGNEJiM", "title": "La Sustancia X", "artists": [{"name": "Villano Antillano", "id": "UCPy0MALtfhQjfm6Jpk5T6FQ"}], "thumbnail": "https://yt3.googleusercontent.com/iBYhCxMqi3gk6yAbQkl3QtcBg8zLb4xrWZwqVOM2d30LeDSyYPhLiN7ecdj5Ez8ezgPmrMdWyi79h3MT=w60-h60-l90-rj", "year": 2022, "explicit": false}, {"browseId": "MPREb_Plbyk9lGcHI", "playlistId": "OLAK5uy_mGzyBhGPJAh93siV6aaWg3G-0rBbluKMM", "title": "Esquemas Juveniles", "artists": [{"name": "Javiera Mena", "id": "UC7o4QTJ5uKk80vxgbJcHJ6g"}], "thumbnail": "https://yt3.googleusercontent.com/iQCkSNeiaz-Ns5vun0hk9RIqRGHPX3JS5Ju2ueU9y3z6nIpRs9UQXo7cSU9RW-IWnQ9jjkG6qhKEIY4K=w60-h60-l90-rj", "year": 2006, "explicit": false}, {"browseId": "MPREb_SaeaYShr3q6", "playlistId": "OLAK5uy_n41srB8RMlM_mWGq4olNfMiPGQrKtQfL4", "title": "Ricky Martin", "artists": [{"name": "Ricky Martin", "id": "UC2rtpKv9X9pfwCuaEt9wsrQ"}], "thumbnail": "https://yt3.googleusercontent.com/dL6AJ2gKjwzQFC3jYiV0D0eeRXjUYDLO6DJ-sI0-91moC4H2ETbfR1AtZic_i5o_h-jSsJsPoawAHf39=w60-h60-l90-rj", "year": 1999, "explicit": false}, {"browseId": "MPREb_fqCiCvSJQA9", "playlistId": "OLAK5uy_mhhW2Nj2PgVVU5z-bZXulTIeTp2PCAcio", "title": "SOLO LAS MÁS, Vol. 2", "artists": [{"name": "La Más Draga", "id": "UCnWur1UMx-EZ7g_XDY8T__A"}], "thumbnail": "https://yt3.googleusercontent.com/HDk3JTA16sDJzHzXCPSxMNzvVSbslr_keLa86VLdY1fVhfoEiwuU3nXnfjN_i2x9OajGxKcBcSoW5ks=w60-h60-l90-rj", "year": 2024, "explicit": false}], "playlists": [], "artists": []}, "Hip-hop/rap": {"songs": [{"id": "OUJt6zIwt7M", "title": "Tu Forma De Ser", "artists": [{"name": "Gera MX", "id": "UC7FDq_YcP26XTeaC44IBTgA"}, {"name": "Nanpa Básico", "id": "UCcleeh_qNXu3VWSgnsZb-5g"}, {"name": "y", "id": null}, {"name": "Itchy & Buco Sounds", "id": "UCerxzAogJy0QD7mnosgaXHw"}], "thumbnail": "https://yt3.googleusercontent.com/JRDWOxEiflH1QndtFBu5fpeMpwATGQ7F5H_A_PWg_8OeliJ33N7I4HOYa256SJd9xxX8shlEMI9mxy1F=w60-h60-l90-rj", "explicit": false}, {"id": "8FLFzOmsb88", "title": "Myke Towers: Bzrp Music Sessions, Vol. 42/66", "artists": [{"name": "Bizarrap", "id": "UCONiUl5u7y2bMaVZJcuRDEQ"}, {"name": "y", "id": null}, {"name": "Myke Towers", "id": "UCYPsIfSIEwWcoynHBP5k1dg"}], "thumbnail": "https://yt3.googleusercontent.com/8QArS95hf1W14AHHEp1sazJrZx63TOm4yfvutbWd7wibAgLn_0_ogqb0X0WHhqrdPjZjD49PYtVRatwD=w60-h60-l90-rj", "explicit": false}, {"id": "I3a2Ec8BBeA", "title": "Mugrero", "artists": [{"name": "Sandro Malandro", "id": "UCa8QbMHrfhktZHCGGnV-QYA"}], "thumbnail": "https://yt3.googleusercontent.com/8ntZkSW-B0WS6HxJsAODazJdhSd4qYNksi6A0IS0Glyh0Wh82LSajjWKkqh7gNmvmWWsqECINxQVCDJE=w60-h60-l90-rj", "explicit": false}, {"id": "lVu02WIh7po", "title": "GU3RR4", "artists": [{"name": "Santa Fe Klan", "id": "UC1t2O7UhGmJVT903LC4dEUA"}, {"name": "y", "id": null}, {"name": "Zimple", "id": "UCNw678AtsC8DsbrYO18dPpA"}], "thumbnail": "https://yt3.googleusercontent.com/kNKhAEE5WqHfiwY4mVSDtOj57bWNX_AddQAS3L4mVIMPaBgh_b3b4rxHcVvZ5INfr5c2BiyV9wI4hZg=w60-h60-l90-rj", "explicit": true}, {"id": "jD7uOfAmGx4", "title": "Las Muñequitas (Remix)", "artists": [{"name": "Mr. Plata", "id": "UC5ZUqgUUGumFspL92FH5Obw"}, {"name": "Maluma", "id": "UCtKIuEMDzPlUmPgdwBhiGcg"}, {"name": "y", "id": null}, {"name": "El Americano 4KT", "id": "UCA40xfjR5ay232HdLg1D28w"}], "thumbnail": "https://yt3.googleusercontent.com/1uCpqWEXkmXsKuo6leHERFSd4qOM5vRuZBdJmT66K1shZe6mZoMI-GZTi42PSSgw3qXfYcC4YaOPOvw=w60-h60-l90-rj", "explicit": true}, {"id": "DWZixjUQYVc", "title": "TEMBLAR EL PLANETA", "artists": [{"name": "Zeballos", "id": "UCDWeYuYc5OacfpK0Hhu_FbA"}], "thumbnail": "https://yt3.googleusercontent.com/HiDcdLQaQKDS-AdZs_CiY5Qdm_qs3fJZ572CuBQQKlz90I_Lg4_OIdteze2XSK42rCWdylhkvoFgTf_z=w60-h60-l90-rj", "explicit": false}, {"id": "RgtSIje8Sjs", "title": "Cachondos Session #1", "artists": [{"name": "Kevin AMF", "id": "UCWQr5-X9ophlthPmxhlvZLw"}, {"name": "Dani Flow", "id": "UCMA_q6xOJgE-beGScQSMPpQ"}, {"name": "Victor Mendivil", "id": "UCB6w3YIXxmlpbGg8YDVpPgA"}, {"name": "y", "id": null}, {"name": "Tiagz", "id": "UCpANtyw0V23etBPNKMbEEYQ"}], "thumbnail": "https://yt3.googleusercontent.com/PH0kr_bhRfpDOqL_qZBxJFV8KIgHuqs271iR18DRowM1MYT2i5p32xaQldUoVTko5s7xIbdd9eOngTw=w60-h60-l90-rj", "explicit": true}, {"id": "8QlDHUi2Mw4", "title": "King of Watches", "artists": [{"name": "Omar Camacho", "id": "UC5TYeA9Hw3L5VlZYtMt3pqg"}], "thumbnail": "https://yt3.googleusercontent.com/9F_-c4UktUzhc3W3L7vTU-aluNVE4d6VOmdFlYUgMMR6Fplqen2ooXOzXh8w7HnMuGmLl7pAW2dozTkO=w60-h60-l90-rj", "explicit": true}], "albums": [{"browseId": "MPREb_lnR5zBQbOYH", "playlistId": "OLAK5uy_n8h59cHoHoVPkf_9fC-94aU7XUzz1x7FM", "title": "Extinction Level Event: The Final World Front", "artists": [{"name": "Busta Rhymes", "id": "UCaSeNTljGkpoj1ijhs72nag"}], "thumbnail": "https://yt3.googleusercontent.com/DWA55dsI7VcjTzQrsl69gMd-vo1ZKCObe5WMmTdH6J214PXkwDZqp6HHdoVKWo4bMhoe5STDdp64qsOu=w60-h60-l90-rj", "year": 1998, "explicit": false}, {"browseId": "MPREb_tfwaolzhOAG", "playlistId": "OLAK5uy_mDWVH2rrXhKs8kjy75G2BeuJeMUPUF-6Q", "title": "Mucho Barato", "artists": [{"name": "Control Machete", "id": "UCC6DLIBOTh0yoXYOmHR2fLw"}], "thumbnail": "https://yt3.googleusercontent.com/hxoGauokP4E47N_irPoKPGzPiszr6D4QOHCBLJ_Bm90S9pEWy704uF-7fYXdoPWATuvL2cf0ps3naUo=w60-h60-l90-rj", "year": 1997, "explicit": false}, {"browseId": "MPREb_EtMnC4f7i5X", "playlistId": "OLAK5uy_kNRI888asLQRh7q2GquHvNcYaDmZvoYvQ", "title": "The Vault", "artists": [{"name": "Too $hort", "id": "UC7kG2xPW0nmPMkU5MIrukHQ"}], "thumbnail": "https://yt3.googleusercontent.com/b7ajnRp2-u0NVzVRcZlKuL85Shz1DNSk-2yWpQkcU31tMSS0pMOzNRx2cNpkLS1OpSIeLzu7cdRU2g=w60-h60-l90-rj", "year": 2019, "explicit": false}, {"browseId": "MPREb_TCAW5t1GmeQ", "playlistId": "OLAK5uy_lzzopnCavlhYdIfDdxD-EZbbGwdoPggp0", "title": "Bendecido", "artists": [{"name": "Santa Fe Klan", "id": "UC1t2O7UhGmJVT903LC4dEUA"}], "thumbnail": "https://yt3.googleusercontent.com/CnjjJwKgTM9OKNbAXWcxKSyYH4iTsDLHi-sorhO5XnAgaD6rxdqyxeBUsEVL29AbB3PL7v5SD2j_0Y1W=w60-h60-l90-rj", "year": 2019, "explicit": false}, {"browseId": "MPREb_h9Vq3qlABod", "playlistId": "OLAK5uy_l03N6-TBAJXujx5kbRF-ZDGOutL1R0F4Y", "title": "viejo marihuano", "artists": [{"name": "cartel de santa", "id": "UCSMOFW7R9HD-KfCKYxR6QFA"}], "thumbnail": "https://yt3.googleusercontent.com/v0Qz8RLTSklskDNrHmLoV9mFuPB5UGPp3mHAjAJk4BucQBXT2sLj30XSS9h8oCU-BAeTqBph2jPY7d8=w60-h60-l90-rj", "year": 2016, "explicit": false}, {"browseId": "MPREb_ohVaKVreOPF", "playlistId": "OLAK5uy_kvYZD21SbhU3xDY2GLsQUkABEV-c5RnIM", "title": "The Coming", "artists": [{"name": "Busta Rhymes", "id": "UCaSeNTljGkpoj1ijhs72nag"}], "thumbnail": "https://yt3.googleusercontent.com/Bb36CalI9OiSO1CKgX1npK-xNzTT4d1A-WtVoDPuLRlrpz0z9u8P7mQo7x1f43_GLYRB-Q3OWp5z5GE=w60-h60-l90-rj", "year": 1996, "explicit": false}], "playlists": [{"id": "RDCLAK5uy_neriXH6JbZPr7Pf4LOi5bGQP-_lWRZXs4", "title": "Cozy Jazz", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/TSUtbquRFr7lhlYJq_fZnfxn8FnzXpbzWiZLJb8-WscOEWDXfldNPT3OcTxkdBL_1vKFG_Ni0_nJ8P5h=w544-h544-l90-rj"}], "artists": [{"id": "UCXRloA7YFgCfd6sU2_Y12cA", "title": "Daddy Yankee", "thumbnail": "https://yt3.googleusercontent.com/URdyU3ebC7x7PQQw8TMVvFiAJRg3x5b7ZBLI9MN79aXpwJ0w9vdeK9mIHuMGHGehZCL15pbSjHXf3m0=w120-h120-p-l90-rj"}, {"id": "UCSMOFW7R9HD-KfCKYxR6QFA", "title": "Cartel De Santa", "thumbnail": "https://yt3.ggpht.com/ytc/AIdro_mA9_VlykuawIaR2KEg1yzYqzPIilqzrDL46TxM1oL_T0A=w120-h120-l90-rj"}, {"id": "UCeKDV9JgivrXehVluw5bKFA", "title": "SZA", "thumbnail": "https://lh3.googleusercontent.com/c-ILO8kXxjY6HhqSkoClWPUtPfHYQW6iHr51EiQOaZiUZ7IZr_WwwkyqclAOFyZgLpC3R0dPXuZiRt0=w120-h120-p-l90-rj"}]}, "Rock y alternativa": {"songs": [{"id": "s103d2mRlyY", "title": "Hoja al Viento", "artists": [{"name": "María Daniela y Su Sonido Lasser", "id": "UCFTxGsV-Tv4GnDmTzzFRadA"}], "thumbnail": "https://yt3.googleusercontent.com/k3R9hPSkgiYWc8Tz--BCb2jryBOC3lzBDyQZuz27qLCbI-s6o-rd5JfRpseiL7gCYhCsYWjHpVprTaI=w60-h60-l90-rj", "explicit": false}, {"id": "PDOyoAe5Siw", "title": "Vuelve", "artists": [{"name": "Wuicho kun", "id": "UCaDHiuh9SoOg6Y_EOQGzTLg"}, {"name": "y", "id": null}, {"name": "Andie Gago", "id": "UCzZxfelOEvMeEbHepWgFcTw"}], "thumbnail": "https://yt3.googleusercontent.com/bo_lei_ixTKsl1js60cF7gVvgTbCmZCJ8DqXD8GFmFaZqQ8uU1d2L2GUNE6fIrgZXIRjJKpNCNFNjd0=w60-h60-l90-rj", "explicit": false}, {"id": "A6ojjzbDbDQ", "title": "Sombras", "artists": [{"name": "Rubytates", "id": "UC7f_tH-mTj4qVECnHqH8bgQ"}], "thumbnail": "https://yt3.googleusercontent.com/2qn5hGcSB3wQLXZUFGbwUljAU3dupu_K1bMhzC5vYA4ALt1gQgfRfuQXb0qvINiJuVCGIoPSuuiW-HwXlg=w60-h60-l90-rj", "explicit": false}, {"id": "PoeJ7nQzGW8", "title": "Así Soy Yo", "artists": [{"name": "Quelle Rox", "id": "UCyYIcj2mqBH3mCkk1wXvh-A"}], "thumbnail": "https://yt3.googleusercontent.com/Tnj9ai5IYKmec4EsoL7aTY3QHgTzdLab1st268TdLbwRn7REVareoIiOz6skdgEG1LFT9iSm9u_MtnW-=w60-h60-l90-rj", "explicit": true}, {"id": "ORoPkkUNxtQ", "title": "Viste Mister", "artists": [{"name": "LaPeste", "id": "UCBGIr58NPqflHqSOn-2TfBQ"}, {"name": "y", "id": null}, {"name": "Pollo Bruxo", "id": "UCd1MTKSvgklRjPzSUuQmQkA"}], "thumbnail": "https://yt3.googleusercontent.com/JC6S344t2bYGdhaZp-2EbM1CHqrcMoXCn5UaOPtQ8heCaI2Mmgc5oYoFn9AQjMyyB_NfY3KRdCdDKyPUFA=w60-h60-l90-rj", "explicit": true}, {"id": "EaeicSboXCo", "title": "niu!", "artists": [{"name": "Plastikboy", "id": "UCNVJZmDX_v0wbUMmAQv7jdQ"}], "thumbnail": "https://yt3.googleusercontent.com/GH5Ojm4NpsADc3hr1NUoOV-Pt8lB2VbFIdd7IlZS-b5uhO4ANezhHuEy9Sb6UmK6_IsFwgv9KjweWjk=w60-h60-l90-rj", "explicit": false}, {"id": "T_aybfgNFvg", "title": "INVIERNO (en la playa)", "artists": [{"name": "Gepe", "id": "UCLS0Hmgqtif6JIADlBLTw1w"}, {"name": "y", "id": null}, {"name": "Daniela Spalla", "id": "UCLSBwPjNc2kH-3cBn2AbHcA"}], "thumbnail": "https://yt3.googleusercontent.com/gg1-agaHmxuU0UIv9P_N62wyBEF07opQeqsTiyiQnv4q4NTo3CAc_rC7YT1tmKKL_JNVxVYeu_lZMjzqXg=w60-h60-l90-rj", "explicit": false}, {"id": "EuTx_YPccXo", "title": "TENSIÓN", "artists": [{"name": "NORTE", "id": "UClFe_dEb8fUwzT5lvK8v31Q"}], "thumbnail": "https://yt3.googleusercontent.com/7oKE3g50VhnIzVHprIJbTwM1SbSY64HXpLHprpjnWanildHoH3FQfJZkj7qDWEuJ849AUHsfL5AVg3o=w60-h60-l90-rj", "explicit": false}], "albums": [{"browseId": "MPREb_7aQPoDAa8YF", "playlistId": "OLAK5uy_kj78AqoSXmbt0qGZc1hWTLDPhdBfoYSmc", "title": "el campamento de futbol y actuación del rey pelé", "artists": [{"name": "siempre no", "id": "UCJr2_v8Y2ES-PCx71SBDHzA"}], "thumbnail": "https://yt3.googleusercontent.com/HI52mc3zUha0Z0hqOg4qnWGKgkirtvSH9OvhvdIM3FnZtegil9tK6DKr7W7GdnfldmvG8o_uNfhlnG0=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_qIizgkqTUJV", "playlistId": "OLAK5uy_lrq_nJFfX_-EYb-Lr5ZYV_yr9TXhXDHxI", "title": "Escrito en agua", "artists": [{"name": "Diles que no me maten", "id": "UCloSJ8Cjiy7L5wQdGBJgucQ"}], "thumbnail": "https://yt3.googleusercontent.com/QExHxbRtWmhqU240sI2V_71uNDs55jo0gsDPobm888eRo0FqiJJB896rRvlQP2fE7dR_f1wlBHAeVNBV=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_TpTShTaiUsL", "playlistId": "OLAK5uy_kwPKxH4PBD89rNaC8dhTKVM2_fNKvtgWs", "title": "Femme Fatale Vol. 2", "artists": [{"name": "Mon Laferte", "id": "UCxOBODdj5wEIZF74kPma7Gw"}], "thumbnail": "https://yt3.googleusercontent.com/g-LE7N4xIvpVhh32-3XFl-IOLqHGcpO4fqZBLxoUORaofvJy6oclFhcjh4NbB8RI5uSBU2OYnvS5C3u5=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_yqsteJ2Vzz6", "playlistId": "OLAK5uy_mSKAxB85Mn_Wmc5ND1Zf5xMqEIntzA78Y", "title": "DÉJATE QUERER", "artists": [{"name": "NORTE", "id": "UClFe_dEb8fUwzT5lvK8v31Q"}], "thumbnail": "https://yt3.googleusercontent.com/7oKE3g50VhnIzVHprIJbTwM1SbSY64HXpLHprpjnWanildHoH3FQfJZkj7qDWEuJ849AUHsfL5AVg3o=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_do4iOhFC0cw", "playlistId": "OLAK5uy_nRtObt24jTTEkFlLfhUWG2idenvAUeG2U", "title": "Dios Plan", "artists": [{"name": "Dromedarios Mágicos", "id": "UC8RRNmyJN7gVW2LcEp-4hQw"}], "thumbnail": "https://yt3.googleusercontent.com/igYJ2Ftu3SmnNd-zDXwk-GdqI4-yST-8kqseuCqgQfrG7-g3Szd6Tq-IcEKFvwEzr0KcAcho71P5y7nk=w60-h60-l90-rj", "year": 2026, "explicit": false}], "playlists": [], "artists": [{"id": "UCzcQZgkzdF8RY7VfPqQdvew", "title": "Margarita Siempre Viva", "thumbnail": "https://lh3.googleusercontent.com/DVgx6h9Sknj9IixOtdlOgjLAmlrZh-Lf0FTe-0myqwUZ5bIgJ7TseLMsJxFQS59VL-yOXDiZr3WE844=w120-h120-p-l90-rj"}, {"id": "UCXr_G4SWVM-JmYpvkN76Dqw", "title": "Belafonte Sensacional", "thumbnail": "https://yt3.googleusercontent.com/K0jUW1qFLXqqZmVCbmDSJM080KonG4fnVsvrIEnNKKI-tZs3ai8dSpr9vwBHoRNyzfB-tyZFcoY=w120-h120-l90-rj"}, {"id": "UCY67rsaE4JgBbvYm43AJxRA", "title": "Macario Martínez", "thumbnail": "https://yt3.googleusercontent.com/AxCokoBujBlx9D_T7e18MkUuFSBGFMPK0bpWZRCUvlZkn2IPx87GQVnDRonGldiJTUC6p3Um3No=w120-h120-l90-rj"}, {"id": "UCr3Fu0PrDySZXIVmAjFk3zQ", "title": "Silvana Estrada", "thumbnail": "https://lh3.googleusercontent.com/RnXiYk60dApNO0AmLPZqCt50swmLXw1qJ0zaz1YADeXeXI_Gu7nWV1gGpgL_xUEBinGDTxoT9O5u61FP=w120-h120-p-l90-rj"}, {"id": "UCp8bIxQKToBu_tbWR8fMY9w", "title": "Clothing", "thumbnail": "https://yt3.googleusercontent.com/fWLHyNLAJxMqHJLcvTkbHIWQI2soSVx7L5AeV_DX9_QlYDcQR_JwDJuAhblL99P_z-eaF7mEhn_F3eZU=w120-h120-l90-rj"}, {"id": "UCLabAHLHMy2MdTWaZATUmQQ", "title": "Milo J", "thumbnail": "https://lh3.googleusercontent.com/23WI_AMUhyvAo3-6Y9966s0TzEW_fLDOeMEHOOOa8QVnYguki__81-jEvh8eMmibxfbzaCecGuI6Og=w120-h120-p-l90-rj"}]}, "Latinoamérica": {"songs": [{"id": "DjqIuXEs1N4", "title": "SI SE ACABA EL MUNDO", "artists": [{"name": "DANNA", "id": "UCu4o_5UkGmx3vyqRkk9kVOQ"}, {"name": "y", "id": null}, {"name": "El Malilla", "id": "UCz0CQ5kSCMG1zXNDQR5jWgQ"}], "thumbnail": "https://yt3.googleusercontent.com/HUsH6fG-Vh5sQxxp2_q7G4HrIusrZ1D-K38qMYaT84ju3cjtJhzSrHtrRu_sRtmWLLvW3NKmGg0IyGNl=w60-h60-l90-rj", "explicit": false}, {"id": "_s3PWh0hJp8", "title": "After", "artists": [{"name": "Conep", "id": "UCcL6TjmX01Db1p_e6jkSkBQ"}], "thumbnail": "https://yt3.googleusercontent.com/Njvomoi_tPvfufJKJNE7FtcE-hX3ggV7WdtSOdi4zgknQmFe1cyMRngnxRgk9Ulnmq_mNOc8laWxyMmg=w60-h60-l90-rj", "explicit": true}, {"id": "c2yVqyNalfI", "title": "AMIGOS CON DERECHOS", "artists": [{"name": "Eslabon Armado", "id": "UCmqrOR5GZcSNS5BLAAt6hPg"}, {"name": "y", "id": null}, {"name": "Peso Pluma", "id": "UCzmabbKsmXlWnI9N2kKQ4lA"}], "thumbnail": "https://yt3.googleusercontent.com/LiBBldVQV0LnU_o1BfQUNDEJBb8fs9uQPGUC2IiVbE1aep4Cw55DdiPXBaxPuGthe0eTBoHU0hFZoWQ=w60-h60-l90-rj", "explicit": true}, {"id": "5xJP3p4LSKw", "title": "Las mas bonitas son p#tas", "artists": [{"name": "Anuel AA", "id": "UCbBaYg2UToDaoOwo-R6xi4g"}], "thumbnail": "https://yt3.googleusercontent.com/iSX0Ox29sf_SpZMr51jUbgHDKsvqQ-54KksqIlFADT9i-nFG590d6D-z61kYxMKF7r5lHX7wWvMjdqbA=w60-h60-l90-rj", "explicit": true}, {"id": "GhMX5llEOy8", "title": "F's", "artists": [{"name": "Fuerza Regida", "id": "UC0kxNxFQCK6d2spPz5Sme7Q"}, {"name": "y", "id": null}, {"name": "Gabito Ballesteros", "id": "UCYMm2JZ_mvXYr7vT9-8_thw"}], "thumbnail": "https://yt3.googleusercontent.com/ozzMHKW5Dj7nLOg4_dPiiBlMn5Q-tudXQ847sYrB8CEAtP61sdpK9a6pVfPekXlBfpxjWW6Ce3uMmclV2w=w60-h60-l90-rj", "explicit": true}, {"id": "6As-FdaTwfc", "title": "¿SERÁ EL ALCOHOL?", "artists": [{"name": "Oscar Maydon", "id": "UCxTyOt1nVVexDlQub6kvvNQ"}, {"name": "y", "id": null}, {"name": "Omar Camacho", "id": "UC5TYeA9Hw3L5VlZYtMt3pqg"}], "thumbnail": "https://yt3.googleusercontent.com/gGH8PDWDXL_onkea-afVVeNoKFbpMDC0dxuDadkVjyVPD1H-GfxZvuQfVa0zvg29rwLcibCvH5FK7us=w60-h60-l90-rj", "explicit": true}, {"id": "8FLFzOmsb88", "title": "Myke Towers: Bzrp Music Sessions, Vol. 42/66", "artists": [{"name": "Bizarrap", "id": "UCONiUl5u7y2bMaVZJcuRDEQ"}, {"name": "y", "id": null}, {"name": "Myke Towers", "id": "UCYPsIfSIEwWcoynHBP5k1dg"}], "thumbnail": "https://yt3.googleusercontent.com/8QArS95hf1W14AHHEp1sazJrZx63TOm4yfvutbWd7wibAgLn_0_ogqb0X0WHhqrdPjZjD49PYtVRatwD=w60-h60-l90-rj", "explicit": false}, {"id": "OsMUzqnrCEQ", "title": "Pensando En Ti", "artists": [{"name": "Xavi", "id": "UCfmeXjlCXi37LGF7O2VT2zA"}, {"name": "y", "id": null}, {"name": "De La Rose", "id": "UCkUHeLHwch0QrYQ3X1wLfzg"}], "thumbnail": "https://yt3.googleusercontent.com/n-fu_8u8xeMO0oJFnL0inHmX79rw8nMcAE-vaiwAY3hSPGWd0sObFIXkJrVbMaNx0kI4j7WRB53pqGQx=w60-h60-l90-rj", "explicit": false}], "albums": [{"browseId": "MPREb_apGKovFqznV", "playlistId": "OLAK5uy_klSKaA5qIAypwbPooQCS856uR0_0jjjfU", "title": "Pa'l Mundo", "artists": [{"name": "Wisin & Yandel", "id": "UCJ0-R-v3CaJNYMqfp9tivqA"}], "thumbnail": "https://yt3.googleusercontent.com/ul7xMtCBlsMbvsygI8DG-HkjM2i2u6jdIqehER-3Daj52S_BCvQ0QhKJD5ljO1BXOiB3QuNkFriw9Hc=w60-h60-s-l90-rj", "year": 2004, "explicit": false}, {"browseId": "MPREb_foBpxdUCpje", "playlistId": "OLAK5uy_myq-LbQ8LNrzAWkvKxqlSsw8qEej1T8DU", "title": "Bachata Rosa", "artists": [{"name": "Juan Luis Guerra 4.40", "id": "UCxtvuCXJMwEzD0_t8GbZcNg"}], "thumbnail": "https://yt3.googleusercontent.com/gqI5W-iQd8HQQgFTHRAsNqdb4_1d41ZIhbgDkX9aOkla8Xjo95xwN5mLf8P_Bf69-8LAuJxNqFylmN8X=w60-h60-l90-rj", "year": 1990, "explicit": false}, {"browseId": "MPREb_NVl6S3DHnR0", "playlistId": "OLAK5uy_n8O29QQJcO-aQA60_RUAH2n-iVBS4oks8", "title": "Pies Descalzos", "artists": [{"name": "Shakira", "id": "UCo6JijJGA3IvIiPsawDK3Ww"}], "thumbnail": "https://yt3.googleusercontent.com/B0PCk94-CUI6ei2dPAHwZoTb9K6g9tdH8djhnljDp7Dqh9Wkj7_gzzitYKsVx6p7CkPIRHm4SFpZEY2S7g=w60-h60-l90-rj", "year": 1995, "explicit": false}, {"browseId": "MPREb_UkqCThn2o5d", "playlistId": "OLAK5uy_m8LiGY49TQR0q_bBIlQoBGfoiNkQtMZjA", "title": "Canciones de mi Padre", "artists": [{"name": "Linda Ronstadt", "id": "UChehd8Gky9eOVZU9Xv33cnA"}], "thumbnail": "https://yt3.googleusercontent.com/L5Sntq9bmJBIlh2Hl9tOP_xV4JmNOnWApeBG2h2EjiUS56SjLSefHbC1z6sMUf3xNyoljOK56FgzfMA=w60-h60-l90-rj", "year": 2016, "explicit": false}, {"browseId": "MPREb_K9lrPAmVyNF", "playlistId": "OLAK5uy_m9oJgWBiJjx7ejhGJnLkOJ7UaiHxFVV1g", "title": "Fórmula, Vol. 2 (Deluxe Edition)", "artists": [{"name": "Romeo Santos", "id": "UCpB_98tUTs3zSiOxZuGPnOA"}], "thumbnail": "https://yt3.googleusercontent.com/k6CjUshafSvhyNfNJn-jQxUAYJU261qFDLqI4rFN_deD7fyA6ehvU3H7ZYL2xz8WfMXi0_WTKMHIuUZMAA=w60-h60-l90-rj", "year": 2014, "explicit": false}, {"browseId": "MPREb_0hVisipjn1z", "playlistId": "OLAK5uy_k85JRrWP1YNIy6s4Y3B_SQ4DtQncmWCvY", "title": "Travesia", "artists": [{"name": "Víctor Manuelle", "id": "UCQz9VIfqA7dIeBY8oBuj89w"}], "thumbnail": "https://yt3.googleusercontent.com/6kdERSJyvy8WwAgplUk9iZrVYnK5o_FxIYMGDELNuc1yrwcYuyAGOq2iR_VfY-VqGt43kA0Q0GQ0wdyy9w=w60-h60-l90-rj", "year": 2000, "explicit": false}], "playlists": [{"id": "RDCLAK5uy_kPR9usfd5aQ8n4rTcEv7y1vMm1ewig4", "title": "Diversión en familia", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/B0xOeOhbTHesdcmH7ozS_8ERBOfOlx3TnRXfverufHrIWolc9OHVvs1xJ5MT1fn86__bP_1cza8Vhlw=w544-h544-l90-rj"}], "artists": [{"id": "UCfmeXjlCXi37LGF7O2VT2zA", "title": "Xavi", "thumbnail": "https://yt3.googleusercontent.com/1gMAiasXBYufoeYVfzwdzv4O3TUMr8FvuLhDpFbyhoVF0h28RW3ATyMS4eyASamQd9c_TIc2ZohAeQ8=w120-h120-p-l90-rj"}, {"id": "UC7n3gWRN0vQzgiOKc51aZ4w", "title": "KAROL G", "thumbnail": "https://lh3.googleusercontent.com/Yzqsh_83kXz4dl0dvPlHTFeQjAkKbX3u-TV-9I7mo-bWm4sCKiHcCBTis2GculkVVNGG76Wwk3uQePeD=w120-h120-p-l90-rj"}, {"id": "UCieB8sWI6qEOX6r_UnJU12w", "title": "Omar Courtz", "thumbnail": "https://lh3.googleusercontent.com/Pv9rzAkKxKnCVe0AMsldl2m5vTXw1dJCABk8tuCUZqfDO2JDUVLW5D4mlx6Pb6j0_UYt_PiGgr8juC4=w120-h120-p-l90-rj"}, {"id": "UC0kxNxFQCK6d2spPz5Sme7Q", "title": "Fuerza Regida", "thumbnail": "https://yt3.googleusercontent.com/aguiuBCHrlCGgMVj9kvUmTmsidKXKcJT0ny378BItcAigzOP1MW-kyOH6nVBmWuTLC3yOdzvDcJcgcU=w120-h120-p-l90-rj"}, {"id": "UCupC5VkgC20rKyJMyogoswQ", "title": "Beéle", "thumbnail": "https://yt3.googleusercontent.com/flKNEMk7u-H71IFvjWWbzA1aSLJSrBhaBrnr-co7hNDXllU-3gm186xXRIlbniBIqSkM4AWflIVX0Q=w120-h120-p-l90-rj"}, {"id": "UC2-PV0lS78r65j_3f5_vPvA", "title": "De La Rose", "thumbnail": "https://yt3.googleusercontent.com/wIAQb8i0YkTlizpJwaw9LU-t0i6YyuOTciglVgFIck8vg-8sYf4WVlnXCk8B7HT-3neFXbwA_A=w120-h120-l90-rj-dcLVWQriEI"}]}, "Rock español": {"songs": [], "albums": [{"browseId": "MPREb_ElOWQiMdRfv", "playlistId": "OLAK5uy_kFpUIAKIYdqjBfELL9FSDyORp8x38nsxk", "title": "Rock & Ríos (Edición 40º Aniversario)", "artists": [{"name": "Miguel Ríos", "id": "UCVEkiQC4_uBtVwxX1wt3tLg"}], "thumbnail": "https://yt3.googleusercontent.com/OKrp_-noBIyGb5nR54d4G5bQlDtRgPOaoGObnK3z8_qK0KUqgUP0njRdmRuNLBUvV7LrytkqN9Cdubw=w60-h60-l90-rj", "year": 2022, "explicit": false}], "playlists": [{"id": "RDCLAK5uy_kb7EBi6y3GrtJri4_ZH56Ms786DFEimbM", "title": "Lo-fi Loft", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/9I45SYZAfTeR17ByV9i2yDpWhQ5DTWMCrW3Lei8vMIiZzeyUnf2zRFUDBob4TGwgUkXAAR_x1Xa8VA=w544-h544-l90-rj"}], "artists": [{"id": "UC8AAA4gDTXATaKXY67lasCA", "title": "La Polla Records", "thumbnail": "https://yt3.googleusercontent.com/06bmbrYbsktl3VMwQiXxgYsajj4A__p-Gb-ovQYX7xGbcbP9Lx1aAVo20CiN3EPNGBpO_67HOeoTEsfm=w120-h120-l90-rj"}, {"id": "UCuqiFef-6Ee3L5iknK49vfw", "title": "Medina Azahara", "thumbnail": "https://lh3.googleusercontent.com/td6MmI6Rlx4cebkgmBU_rs-HC_Ihakt7wr3w51UwOj9c4t1HTEB4c0AdwiNIDWuKlMmbvFb_DtNP-A=w120-h120-p-l90-rj"}, {"id": "UCmXfHxT5wDU98qxnrLCsFww", "title": "Coque Malla", "thumbnail": "https://yt3.googleusercontent.com/4CYHdwzIpQQaFeS1eWFMt4aXNJKsmmvUG8wVRanDIvin0sWMAVsXSvLKwoIYqil1mMOaIEVDAz645U0=w120-h120-l90-rj"}, {"id": "UCr9E0R2qTXZ9V9e_9OgdF3g", "title": "Marea", "thumbnail": "https://yt3.googleusercontent.com/pZQJK2VmzKC5x2yqhu2j-YlFEYnpXe51IlG9ZPrPcFtdOzb4AMdD0xux8aZkzbYqYGnIaNLxUZc=w120-h120-l90-rj"}, {"id": "UCywxOsguiUxng70kJ_Fxerw", "title": "Ilegales", "thumbnail": "https://yt3.googleusercontent.com/tU4x8hZb7axuV0RyZHan-wzeAYrFNX6-bKuP6pay1twYbB7VuvcJ7wf4WVNXN6MNNv8UTe4eUQ=w120-h120-l90-rj-dcATCeT5UK"}, {"id": "UC1WBBZeH6FA5k6hE_NbNlfg", "title": "Los Enemigos", "thumbnail": "https://yt3.googleusercontent.com/uAGu_2_X5kgmgBE7xT5iSSjEIJTb8ZjwTFW74Mm7f1HdOoNiassUT6dX4rmjWOuSPjg7n28u-6IKDA=w120-h120-p-l90-rj"}]}, "Sonidos del verano": {"songs": [], "albums": [], "playlists": [], "artists": []}, "Pop español": {"songs": [], "albums": [{"browseId": "MPREb_0d16dbfRpEO", "playlistId": "OLAK5uy_nwCFa-CRIPrjuj_fbppK9H3ELWcwN0x8o", "title": "Dos Orillas", "artists": [{"name": "Antonio Orozco", "id": "UCaWMbXN3x1gDmQxhZte3ITQ"}], "thumbnail": "https://yt3.googleusercontent.com/-U3EmK22z4X-zuft_oD4LVmIkLzbXevRDNo5oS0mWWoNTKzLS2A-UQrIP7PlsZaSFHmEE31NaT0xpuDZ=w60-h60-l90-rj", "year": 2013, "explicit": false}, {"browseId": "MPREb_GznihzeBAcf", "playlistId": "OLAK5uy_npELg3Xm4KjLj7npxXRnn72_6DnQDRnJI", "title": "Munay", "artists": [{"name": "Vanesa Martín", "id": "UCuYEp4nYfK8tU6AsTUbzs8A"}], "thumbnail": "https://yt3.googleusercontent.com/ovEHlKGFpyVBWBz_FkIy2tNd-NRcyvIBuV1Dvnv5xwdGYBDthclxyVHwxQv7f11taK_eB6ZEB3qrPiQ7=w60-h60-l90-rj", "year": 2016, "explicit": false}, {"browseId": "MPREb_EVafcFZkEET", "playlistId": "OLAK5uy_n86t-r--ipj4eGS3vG08lLnLCnujCDMh0", "title": "Directo", "artists": [{"name": "Vanesa Martin", "id": "UCuYEp4nYfK8tU6AsTUbzs8A"}], "thumbnail": "https://yt3.googleusercontent.com/Ae58cWxqnZ2CjjmqFiJVWTAxeJQgmx28nac95wr6QlLxHsQd1sbpIz7JFBz8SxQQ64Ju-qDgevYLna1O=w60-h60-s-l90-rj", "year": 2015, "explicit": false}, {"browseId": "MPREb_wliM6Nw59L6", "playlistId": "OLAK5uy_mNAV4D3bFlhsW_u9rizClgoDd_2izvWCg", "title": "No Controles", "artists": [{"name": "Ole Ole", "id": "UCbAmXjI4jZ_nqM5QJVlDm7A"}], "thumbnail": "https://yt3.googleusercontent.com/kB9fT0OEDv6iPU8Jbi44lUwSGY2XgowqAwM12P1-vKvchPORT85kNLMTGNINtvK3WxT6N6YI2GRyOOr0=w60-h60-l90-rj", "year": 2000, "explicit": false}, {"browseId": "MPREb_ppkaywd1vhB", "playlistId": "OLAK5uy_k0-v0WZtedOaG6n1o5Z1_twpj7Y82AgXE", "title": "Todas las mujeres que habitan en mí", "artists": [{"name": "Vanesa Martín", "id": "UCuYEp4nYfK8tU6AsTUbzs8A"}], "thumbnail": "https://yt3.googleusercontent.com/l31jXsmO7HPaAubG2MhF3dX8GDvvR2CirVokTy_AOOCVfAEgG6upd-YxGpow6_8hiJA8KA6rAORIxwKi=w60-h60-l90-rj", "year": 2018, "explicit": false}, {"browseId": "MPREb_V4wCKmWqqfI", "playlistId": "OLAK5uy_kvYDGN212Q9DKYMsH3e5UU6H84TRYZ7H8", "title": "Enrique", "artists": [{"name": "Enrique Iglesias", "id": "UCD-0qTYqRd4t9re9AwRydnQ"}], "thumbnail": "https://yt3.googleusercontent.com/wiIfTXDuski-zIZMDMhsIbdrtjCSBW2q-SkvufPsnuGDeYsq3srMot_RFul1LjPUMFZezHZUT2ZwE4o=w60-h60-s-l90-rj", "year": 1999, "explicit": false}], "playlists": [{"id": "RDCLAK5uy_kPR9usfd5aQ8n4rTcEv7y1vMm1ewig4", "title": "Diversión en familia", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/B0xOeOhbTHesdcmH7ozS_8ERBOfOlx3TnRXfverufHrIWolc9OHVvs1xJ5MT1fn86__bP_1cza8Vhlw=w544-h544-l90-rj"}], "artists": [{"id": "UCjqTl3BnSW8sjgHNYDKMU0g", "title": "Rozalen", "thumbnail": "https://lh3.googleusercontent.com/dgEkoVEsq-rxR2mQNaC0S80lv8xUcUcK49f14mqr52ycutoIQd242kSIHCyJEOOl1SZIyXNVePWxKR2o=w120-h120-p-l90-rj"}, {"id": "UCRn6bfeGI1jrY7Mek3GJ_xA", "title": "David Bisbal", "thumbnail": "https://yt3.googleusercontent.com/z3v9P4_XBWM4w8Sy8B97_VToeD57OSnnofsDSLPJ20tIqKuqYeCADEWqInK1zem3d8iEWb3awHHqOtw=w120-h120-p-l90-rj"}, {"id": "UCu6ct4LWh-sMXxQpreWDY_g", "title": "Amaral", "thumbnail": "https://lh3.googleusercontent.com/-IEVca-qpbq5kYfDnkAkTUol8Mdi1uMIh8IlIceS4Bkboi-8UT7xXnGfhsDpl9eWNvPRLFruRX5cYcI=w120-h120-p-l90-rj"}, {"id": "UCfsuOL_UoowyMumMaLcMnSw", "title": "Cepeda", "thumbnail": "https://yt3.googleusercontent.com/WloWc9DO0KcpXP1pZ-v3uN44lk1owQiF9j1t79rrQ0Hl6cAto04FooCfQvk8GBhnSgIxCK8kyQ=w120-h120-l90-rj"}, {"id": "UCiRMjSDzeSIrY-xHiHXl7sg", "title": "Pablo López", "thumbnail": "https://yt3.googleusercontent.com/JWhbdfYkUaJmuo-3Pu7poY8GvIhs6e4N86cX_ph71zzkQqhkj2Xjalc7bZLIn1FlxrGD0gby0lWkBFzu=w120-h120-p-l90-rj"}, {"id": "UCz34FayZhwHJNdCLqhwVYaw", "title": "Beret", "thumbnail": "https://yt3.googleusercontent.com/XjtryLOAcdpV4YqKprtnGvfdZU9OiAN11fs8X8kOH67qW_T_4IhPrg0zGauRXG4O9twiBScjDw=w120-h120-l90-rj"}]}, "Dance": {"songs": [], "albums": [{"browseId": "MPREb_arKNB44Ct4n", "playlistId": "OLAK5uy_lSWZaOROMRyq-XkAxmqN6NeQfuVIjcG7s", "title": "Recess", "artists": [{"name": "Skrillex", "id": "UCibXKvuw5PoJVmyZJ4qhDIw"}], "thumbnail": "https://yt3.googleusercontent.com/Ki9MBahskwhAiMxmRUuwO3n2-5m8wta5xRpi5xoaXovWKKs-rFAQ2wjBD7c7HmRqfEOmUdyNzb4QVnI=w60-h60-l90-rj", "year": 2014, "explicit": false}, {"browseId": "MPREb_7ltM34kr0mH", "playlistId": "OLAK5uy_mz6eafmqdRHSaR4IwG0ll6J6rgv0_ZpGw", "title": "Discovery", "artists": [{"name": "Daft Punk", "id": "UCRr1xG_2WIDs18a6cIiCxeA"}], "thumbnail": "https://yt3.googleusercontent.com/qrY7xjdjqL0FEneZKHk845JeeFjdWOgexUa_BhsUxskJ2iflhzpbofqGJZBcPHDGU9JjoZflE4yn3P74=w60-h60-l90-rj", "year": 2001, "explicit": false}, {"browseId": "MPREb_K8qWMWVqXGi", "playlistId": "OLAK5uy_kNhM2yaBTOVwrcZJepB1C9P3-n5_Sfy5c", "title": "Random Access Memories", "artists": [{"name": "Daft Punk", "id": "UCRr1xG_2WIDs18a6cIiCxeA"}], "thumbnail": "https://yt3.googleusercontent.com/N55arCGj69gtw6thXK8JUPisxoVYiwuIEQ7I6SGlkEyNcSJ7xIWPe76Vuu1SiUqRyx5w9qvR_zV8fV3CWQ=w60-h60-l90-rj", "year": 2013, "explicit": false}, {"browseId": "MPREb_Wpu4v83ZNm9", "playlistId": "OLAK5uy_lD7wc7DZzXiz8GrZQG7yM6ODW3Vfl_i60", "title": "18 Months", "artists": [{"name": "Calvin Harris", "id": "UCZ0Aezmtk-S2l8A9Ln-2lKw"}], "thumbnail": "https://yt3.googleusercontent.com/9rzfBD_-p0FtfPmPl6TfztXCCozZe142oy1MokdY6kO6zTiu4Y0ieKj3H0s8EqRxoRyv4234VR68Ax3Ebg=w60-h60-l90-rj", "year": 2012, "explicit": false}, {"browseId": "MPREb_KRDbvxor1uN", "playlistId": "OLAK5uy_l14m0BsqM6XDhrTAKyH7R5ScolkCTemKo", "title": "Clarity", "artists": [{"name": "Zedd", "id": "UCGVGIqHPzwLhZg8KQNVaRbA"}], "thumbnail": "https://yt3.googleusercontent.com/tgDKXG9pHrKFMGjwDM9waFSOs_KlSXF05uymprrGyHvLLg6KGl6U-O34pI2cNukVMatNft6lY-SyaEzHxQ=w60-h60-l90-rj", "year": 2012, "explicit": false}, {"browseId": "MPREb_nJh37OXr6e5", "playlistId": "OLAK5uy_kR-piDyx0Z-biqKgyeuCiepEJwGz5Oqyw", "title": "True (Bonus Edition)", "artists": [{"name": "Avicii", "id": "UCuACQmW04T3v9Mz_1_suFYw"}], "thumbnail": "https://yt3.googleusercontent.com/XincHWEjkXhpbavoQEHWRbTcVdvHsujjr7OAw-73KUCILFgjLdevPW8vkoaRMibnwkTtGWkEDyKbuNeK=w60-h60-l90-rj", "year": 2013, "explicit": false}], "playlists": [{"id": "RDCLAK5uy_lRQbFalJOe45Qa-ERq9tTVUIv6WZFW_WA", "title": "Clásicos de la Salsa", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/K-ywNc7QSJRLjMw5hRPFjCqB8L3qKuwhuM1rJSZN8mqjoDAZB7msE4hw7faZSbQcRWt6ysMEAUk0WQ=w544-h544-l90-rj"}], "artists": [{"id": "UCQ-jZEqJkQbQogv3wNV_5Qg", "title": "Mau P  ", "thumbnail": "https://yt3.googleusercontent.com/tAmfY2uy58yFlqIudSnJLvdrkBqkTVL8bzTqnRkDaNDce3l-4JaGxdwEQxp8wr2oHNfumbkAyg=w120-h120-l90-rj"}, {"id": "UCoVxBu47k0Sgq3ybXxw6jSA", "title": "LP Giobbi", "thumbnail": "https://lh3.googleusercontent.com/LPv1-mhaDxRXOnUKBQrz3TnLK_Lgt9VhcLIoL9wuszWOpaW09LqIJ5B-gkaya_j2xWXUUsoCZDvbcg=w120-h120-p-l90-rj"}, {"id": "UC5thcJO5W-7SYUa-E-YIHcQ", "title": "Hayla", "thumbnail": "https://yt3.googleusercontent.com/_PhR1RYMmLtVjUu3DHjkHuIqulFqfeb8NAuhnvR1lbB5SLFYxIQeei9jsVIKxnmaZZLkwgIAAw7qSWg=w120-h120-p-l90-rj"}, {"id": "UC5DgGU95TLIjZpLcnY51HuA", "title": "salute", "thumbnail": "https://lh3.googleusercontent.com/JK0294ZPIeMEAFDPt5OcPNXfDAai4PJ9slmlxWyLcvPHPDnkD6GtEkLcSV88YJBimomqYn0cExDrVns=w120-h120-p-l90-rj"}, {"id": "UCoIevyBHb_rq-bf16vbnd5A", "title": "Barry Can’t Swim", "thumbnail": "https://yt3.googleusercontent.com/na1ovQ0-CH1UipS9_o4wGc9G3ofL3iEm9RoAnCCPS4lD7IdR2i8YBoNLAsDPCotVsq7fyGWc8A=w120-h120-l90-rj-dcgWqSIasH"}, {"id": "UC4m9BqKzJEMk-9aWOukSe4Q", "title": "Sammy Virji", "thumbnail": "https://lh3.googleusercontent.com/mFMiywziClkLdVme3M_DaEi1ytIAyA_18_w8BcQH8zzc8yOVQTcw_j7hgnMTpR0bW0_IWNp4Dn4qjSs=w120-h120-p-l90-rj"}]}, "Rock": {"songs": [{"id": "_jENbuMGHhM", "title": "Idyllwild", "artists": [{"name": "Pete Yorn", "id": "UCyve-Se1niZrxyoqQN62CQg"}], "thumbnail": "https://yt3.googleusercontent.com/M80SKVG6kgEFGMpK_2VvsGh2LgQOYND-83VErlr8jvvSuUiIX2MOBqsJRiT-O8TjGW_M05CBmit5QXM=w60-h60-l90-rj", "explicit": false}, {"id": "bqRaSN64xNQ", "title": "Plans", "artists": [{"name": "Brandon Flowers", "id": "UCzmeRKufJiwZX2ewOPqUPEw"}], "thumbnail": "https://yt3.googleusercontent.com/oZDswd3qJsFaIy3TDCwA8tJe2cdAB8RlhC5IOaqLRT6-WSFX4J8xoOoCNuDu9qpHP8kpCupxzWcIFc74=w60-h60-l90-rj", "explicit": false}, {"id": "dmI2gK2j31M", "title": "Divine Intervention", "artists": [{"name": "The Rolling Stones", "id": "UCNYhhkQqeFLUc-YEDcLpSYQ"}], "thumbnail": "https://yt3.googleusercontent.com/ioqxnqatCQtOs4O7dRgchSLpkX3W3xZRzcwD17lvm6KlA9N-CfrvLCT3Ri0EbLLDGpUEkqNbsbR3lpE=w60-h60-l90-rj", "explicit": false}, {"id": "vpGaal1BS_8", "title": "Ex-Mørtis", "artists": [{"name": "Ice Nine Kills", "id": "UCnQ1QrnSZ9E_O38V0R8fR0w"}], "thumbnail": "https://yt3.googleusercontent.com/cZefVPnRmc21k7uZe1pxp-NP7mx5g3TgnBqvwVoImYXakFBanH7hcdKmu3yngN3qwnGzOFdkL6HXZ9U=w60-h60-l90-rj", "explicit": true}, {"id": "zS6pQCizo8k", "title": "Sun Has Set", "artists": [{"name": "beabadoobee", "id": "UCcGMuu89vageEKV8zUKhwdA"}], "thumbnail": "https://yt3.googleusercontent.com/QyJQUFZOCdqc2DplTlxQrTZBdpJdx86fzaiUWGT8pf4-VsTx4NAHbj2143RtJdcu6QciIL9UZJSv8xyr=w60-h60-l90-rj", "explicit": true}, {"id": "rpHYQQQKmos", "title": "Billy Came Back", "artists": [{"name": "This Is Lorelei", "id": "UCvL0aZMhBZ8FR2vAofqQmVw"}], "thumbnail": "https://yt3.googleusercontent.com/Yy884uTfmekkEDzSJYNy8vzeo9oSbfjgsVrQtTL6oOO_hgurSVt4UIdDB7EuFczezpxYZzf8j0e7Q80=w60-h60-l90-rj", "explicit": false}, {"id": "9yd-Y3Qobc0", "title": "Sweet Escape", "artists": [{"name": "Return to Dust", "id": "UCbd9qNSyTavj5vurolR1vhg"}], "thumbnail": "https://yt3.googleusercontent.com/hf9sckfSlK4QPTahrH_U62eM05r9TfmHFdQFvm5MD89nquN9i6B4Rzr5aCv_h_g0UJxkAFWmjGIqwyek=w60-h60-l90-rj", "explicit": false}, {"id": "uwqMFpvrbkw", "title": "Keep Looping", "artists": [{"name": "Sugar", "id": "UCPuI9c12W8hG8WADlJmJJcA"}], "thumbnail": "https://yt3.googleusercontent.com/wkYEBWZ7J8RQh2fs_-yCs5mdQnLcc9X44K3AbMXasUWipCGdWs1T8jzPeDblLl97-QNcGWZ7KNPxI-yl=w60-h60-l90-rj", "explicit": false}], "albums": [{"browseId": "MPREb_tQfaWH32ovE", "playlistId": "OLAK5uy_lqcFZTOPHGwcnP0nYMzNuY0IES0fl7Fe4", "title": "Abbey Road (Super Deluxe Edition)", "artists": [{"name": "The Beatles", "id": "UC2XdaAVUannpujzv32jcouQ"}], "thumbnail": "https://yt3.googleusercontent.com/g8bzAg2zxvdnm7ismLMYLA9-9azb4y6VP2uOF56A2G2rpsqLHT6mrJWXRKq_VttXQZ-o-jmVgTFIVgdj=w60-h60-l90-rj", "year": 1969, "explicit": false}, {"browseId": "MPREb_NXCr0YMPIpQ", "playlistId": "OLAK5uy_klxWDedbSRqgjGzYYSvyq3KdHY1wbTt_U", "title": "The Rise and Fall of Ziggy Stardust and the Spiders from Mars", "artists": [{"name": "David Bowie", "id": "UCvY1eVE6lTebXsdFbbXUtkQ"}], "thumbnail": "https://yt3.googleusercontent.com/ldn2zd7THwOFCPWJkEc1IM7mL1HcaR6IEF9oUHc90VvkJYu7CHIsRpLg2ajQugWCB_hmsDcXMS4N4XMU=w60-h60-l90-rj", "year": 1972, "explicit": false}, {"browseId": "MPREb_jPOYfjGgApr", "playlistId": "OLAK5uy_lYnxawfGdkGePjdFhIYaS6LjP-Md6UYf0", "title": "Nevermind", "artists": [{"name": "Nirvana", "id": "UCrPe3hLA51968GwxHSZ1llw"}], "thumbnail": "https://yt3.googleusercontent.com/eyKiPBSqEu556sYTd_IyZhfxun5e_hatZ9tAyu8bnmVRgtbM3aW-SXUvhVX-d7s1oU0Yf3a38JOuYMZK5w=w60-h60-l90-rj", "year": 1991, "explicit": false}, {"browseId": "MPREb_eHEOTy3DS94", "playlistId": "OLAK5uy_kNH5_0dq0SINuzQFBDRKoCCcO0aTcGxoo", "title": "Revolver", "artists": [{"name": "The Beatles", "id": "UC2XdaAVUannpujzv32jcouQ"}], "thumbnail": "https://yt3.googleusercontent.com/r8_4I_rvh2kHa9Y-mSTH72Z84ncYx0SzPVLXXqaLEPYQrWqB03dizqePdZXBtAUa_La2woSY6czcx1U=w60-h60-l90-rj", "year": 1966, "explicit": false}, {"browseId": "MPREb_kPYj6JOZHGx", "playlistId": "OLAK5uy_mSOhVYfA4mGwMGSRP_OIh3B7yQXu8I0b0", "title": "Born To Run", "artists": [{"name": "Bruce Springsteen", "id": "UCyFqJ_5TyAeTD7rHpLikbKQ"}], "thumbnail": "https://yt3.googleusercontent.com/8aK3INgtq69MvZFQ8wO9sFsMFkKFpFT1BJSbgBhUTYdfRuGbo57e2mNMVl_vue3BTS-TSlDn7IhTQa8=w60-h60-l90-rj", "year": 1975, "explicit": false}, {"browseId": "MPREb_eg7VIEpYF8I", "playlistId": "OLAK5uy_lfBlhBZMlaqZCHE5LR-kEo7kIrB4QWCDw", "title": "Led Zeppelin II (Deluxe Edition)", "artists": [{"name": "Led Zeppelin", "id": "UCYtap7ujIPaxTS2iCDoMi3g"}], "thumbnail": "https://yt3.googleusercontent.com/S496I1JK8xN_TxN97NJmD_JZBkopZsdy0pPpU6usi2Sm6-JT-yf9XSNgFVWtTlYCOSKrowBpl2ByOP4=w60-h60-l90-rj", "year": 1969, "explicit": false}], "playlists": [{"id": "RDCLAK5uy_moVT3RtwEjOmKiQ29AV1QNtUoLqyD0B50", "title": "Los éxitos de los 50", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/LIox_Mca7phf0F9tFiScSYH8MYiF7NTV93hLhO7VT-hjiY1Ls7AfRu9dvN14gAYeE_1-ApjU_V1wx5eH=w544-h544-l90-rj"}], "artists": [{"id": "UC8nblxXrBTKakN3_3rSvuEg", "title": "Ax and the Hatchetmen", "thumbnail": "https://lh3.googleusercontent.com/J36OrmM7mUENpp4h5ns2Nu_1eZuXfvIFVk_lx-Phhf4-crUI-8ae7Dsu5pwGw00G8Li45B3e2JEqR-8=w120-h120-p-l90-rj"}, {"id": "UC7UK2C414mUyWkAL4zFd7yQ", "title": "Ecca Vandal", "thumbnail": "https://yt3.googleusercontent.com/f_NVeVBIvhZ90MCpYkWSF_crbZF_y7nRKDSs0aAKGZEBhDYt7uTkwKTcTyVVW585cb-qq9o-_ZMQIWIk=w120-h120-l90-rj"}, {"id": "UCmcxkaCBzxwsqkTeUhAh0mg", "title": "Sincere Engineer", "thumbnail": "https://lh3.googleusercontent.com/DmaYMttPbRfTqAoNiBW03mNw4R9IFFxae4MB3hEtH0NumIK4BNkKnlVyc7BNdlAQBF1OGpAn1ywQUA=w120-h120-l90-rj"}, {"id": "UCw7CZ10-EJw5VQuxNDTNCdA", "title": "Violet Grohl", "thumbnail": "https://lh3.googleusercontent.com/Lv6pyRWKOEf388miQyJ4cObX2CubD_DR5msbjtywpiyz7qmCgagW_kGJQTmli5ZUJRUTHmjD_Uu3QSE=w120-h120-p-l90-rj"}, {"id": "UC7Ccjp1JYTpIcTRdM1EpgFw", "title": "Basement", "thumbnail": "https://lh3.googleusercontent.com/oD3m6STlVSznZ4gUYHiVtwGuO5b4V3feaJCfzc-DXNaDrkz7c-CHMtnzamCprbhRe1BXK2tRCT2zzg=w120-h120-p-l90-rj"}, {"id": "UCGFdAlnCuH6ziQceyZvPnBw", "title": "Bilmuri", "thumbnail": "https://yt3.googleusercontent.com/Bi9jXkql33YYRWKXLDbBxV7rFryYtoLv04RBJOtUBOJoRbom3pE1tmszbsSb7AQiq0k1aC7s=w120-h120-l90-rj"}]}, "Fiesta": {"songs": [], "albums": [], "playlists": [], "artists": []}, "Reggae": {"songs": [{"id": "HG4D71-vJeQ", "title": "Celibate", "artists": [{"name": "Teejay", "id": "UCwEhUtquhuVcJ8vDSvKJrdA"}], "thumbnail": "https://yt3.googleusercontent.com/nYchZOEHI5q_LHTNbm8WVdfDyRcrgDBUdVsSxiFLJRk1M7Ra1U4cj40V0XUSH4n2lnVZ1NFsfc_SdMl6=w60-h60-l90-rj", "explicit": true}, {"id": "LFB73INXnqs", "title": "Goodaz Fi Dem", "artists": [{"name": "Intence", "id": "UCIe8MBZHswHIZkvxTffaz0w"}, {"name": "y", "id": null}, {"name": "Jugglerz", "id": "UCKEHvTAD-4kSAD3OdgGmgoA"}], "thumbnail": "https://yt3.googleusercontent.com/jZ0aXwKq1UqmGHTz7H6KZnRJXGGI1ccGaYN-xtx7zOV1tInE9e_hgSf7Otv4d2Aet6K4zmfT0REk41lh=w60-h60-l90-rj", "explicit": true}, {"id": "vH-2tnutenU", "title": "Fall In Love (Refix)", "artists": [{"name": "Popcaan", "id": "UCFjuMqDJBKfvUvzyUm5T-QA"}, {"name": "y", "id": null}, {"name": "Baby G", "id": "UCqH51A-Ew1U9e57ecUW5qDA"}], "thumbnail": "https://yt3.googleusercontent.com/VV8JnDzb0bZEj9Ps2U5HtvFg8eHsFL21r0qXGSqOZ6YRTIk-QH_dGlYBTQzWM5v5eJS6o011sy3d_9c=w60-h60-l90-rj", "explicit": false}, {"id": "tpF6c0S1bus", "title": "Try Again", "artists": [{"name": "Vybz Kartel y Skillibeng", "id": null}], "thumbnail": "https://yt3.googleusercontent.com/VeVBNrDDQyqzp3UjN66b741WDlmpkkdhqhRoIueuxqagAo1qKSM_Z-OkOLc8EjRVKwwMY7zk_XZS00yJ=w60-h60-l90-rj", "explicit": true}, {"id": "WZemVdrrKGo", "title": "House Call - A COLORS SHOW", "artists": [{"name": "Buju Banton", "id": "UCGxTc3-8A_by4P2Y2nB_5KA"}, {"name": "y", "id": null}, {"name": "COLORS", "id": "UCwCjP1efcw7BH7JvUgO788A"}], "thumbnail": "https://yt3.googleusercontent.com/nqiwkh7SBxPMmuwsphQX02248rDBRKFLJp8KbcTznjng8mtcVF2SqC1jJGIjPKV4yqoOltWGv3Tp4E8=w60-h60-l90-rj", "explicit": true}, {"id": "9wiCcQcznQg", "title": "Ryti and Pretti", "artists": [{"name": "Rytikal", "id": "UCXpTWNnugCNtNPB_ylghkZg"}, {"name": "y", "id": null}, {"name": "PRETTII PRETTII", "id": "UC5_CGTy7JnvfuYr-pC6yZsw"}], "thumbnail": "https://yt3.googleusercontent.com/MFXVrrryUiKHkKFbzRtQf1b25lYvy1tVjjbg4OKZA9dsqaUgo6qX3Uj8_Kv9BlCP8s2KdQ9xwfgvR6UH4w=w60-h60-l90-rj", "explicit": true}, {"id": "44uqlBrUdu8", "title": "Babylon (con Anwar)", "artists": [{"name": "YG Marley", "id": "UC7vF6-Qv1e-cMsHzbukngJg"}], "thumbnail": "https://yt3.googleusercontent.com/Ml07vDdq4K5MpMKLQmvvcRTCoXwm-kuwfI00UJz2typdPr_POGeFfAvysx2yA7ulVyCepqMNQl6dX_PXew=w60-h60-l90-rj", "explicit": false}, {"id": "-JKZrusbCEA", "title": "It A Tape [Again] (con Vybz Kartel)", "artists": [{"name": "Likkle Addi", "id": "UCh7n5yg9seUWeaZ_bOtstyQ"}, {"name": "y", "id": null}, {"name": "Tjtorry106", "id": "UCef_lP6ppytr1CLMJcigkQQ"}], "thumbnail": "https://yt3.googleusercontent.com/2bt3dGrUxVAk6cHQGDfz_LBYZNxmuKQw8SPOWprrunJo6ZiMHsWLKu5W_gTLncYyTk1SX-AhUjjzRi8=w60-h60-l90-rj", "explicit": true}], "albums": [{"browseId": "MPREb_7YCKn1TeIk3", "playlistId": "OLAK5uy_mD0w1ps-RA7mL8SamaSonXgwdnQsB2x9E", "title": "Night Nurse", "artists": [{"name": "Gregory Isaacs", "id": "UCrWrf0Dyue7UNulBwPhbI2A"}], "thumbnail": "https://yt3.googleusercontent.com/R-UEEsYTBFgi8Nnp0oiEDsuRisjIjYcMm-TVXXTo9PQr5waqkyO2thgHst7UjKGnreF-FgACKzEPCaZghA=w60-h60-l90-rj", "year": 1982, "explicit": false}, {"browseId": "MPREb_W7lhIYk3yPr", "playlistId": "OLAK5uy_mNzkqznEiTjvBmTVQoaWtrojJVLOk4DCo", "title": "Til Shiloh", "artists": [{"name": "Buju Banton", "id": "UCGxTc3-8A_by4P2Y2nB_5KA"}], "thumbnail": "https://yt3.googleusercontent.com/4CKpChn-cTNyt3cYwcRkak6OP6Ri595ptvmTSQlkfmNk3d9h8T9ZX3gwvjT-6saF6ekJYa9JbHKZp08=w60-h60-s-l90-rj", "year": 1995, "explicit": false}, {"browseId": "MPREb_NZtjwxkbcSS", "playlistId": "OLAK5uy_kbsfAhd6FZV-zAaoHBDGZJzxmxnfOX36U", "title": "Mr. Merciless", "artists": [{"name": "Merciless", "id": "UCJHIWpY9jbCQ-GseZxCzE4g"}], "thumbnail": "https://yt3.googleusercontent.com/IQM8sNzYcwwJd8HTTkxa8GBAH3FnJnrGabQlsZFiLM0VByVHnHXne-ikOEYiRFjf-sIb7SS_2Qu9vDMy=w60-h60-l90-rj", "year": 2009, "explicit": false}, {"browseId": "MPREb_76TXRTmMdBA", "playlistId": "OLAK5uy_k4eLDldmX7LgfR4Dgd2nL6KAKr9ltRi0U", "title": "Legalize It", "artists": [{"name": "Peter Tosh", "id": "UCFLzDxCkznTBfJkRBgJ_hwA"}], "thumbnail": "https://yt3.googleusercontent.com/xVh4ryglkNrcmEq321uzG4EQybXDw26PzT4SfGdsfsYWqjJO0GuFWRdVly17pmbOnSIFmNTljfLhzeY=w60-h60-l90-rj", "year": 1976, "explicit": false}, {"browseId": "MPREb_HCNnHK02yVe", "playlistId": "OLAK5uy_luzgI39tJx3t1GUapVclkfK-TwkVPWjV8", "title": "Legend - The Best Of Bob Marley And The Wailers", "artists": [{"name": "Bob Marley & The Wailers", "id": "UCBfv87kvVXyNi88URZ1zvCA"}], "thumbnail": "https://yt3.googleusercontent.com/fbiVHkhnMAbNmsHA-AVxkMLzFJd7IhjmFkvgvi2kMGGiRMuZt_C5fAj8R4O4D8RCoXa_5046J1OIwASH=w60-h60-l90-rj", "year": 1976, "explicit": false}], "playlists": [], "artists": [{"id": "UCCGwJkS0SUXVra72g9MAaog", "title": "Shenseea", "thumbnail": "https://lh3.googleusercontent.com/xbIjNaClHsL6xYdyFEh8uVoqJDxq8I8EBekqeIkIQ1fyVCVdIHPqt4n4uWWrOLLkQbNI9sHkpxhI_g=w120-h120-p-l90-rj"}, {"id": "UC-b0g7AOuRU4sT4wa9B7vyw", "title": "Kranium", "thumbnail": "https://lh3.googleusercontent.com/juqEjcbca14la1mcjKKA1QHiW0NkpUKh7_Sg9R57-W8wu5HkhtmMf7ZBLhuKsG6_bluWpgbCrSuQVvg=w120-h120-p-l90-rj"}, {"id": "UCJuQbfQ7SEJdMmVqPT7rXZw", "title": "Vybz Kartel", "thumbnail": "https://yt3.googleusercontent.com/9Hvbibknb2BYmbbraODpYCBx8xtawQfFAlpjRRxYX-xsjnzs5bD7iS9BexVRNV5j9Y9WDUXatQ=w120-h120-l90-rj-dcKTSArh0J"}, {"id": "UCETFg57L47ZF-Hz3SqTxyPA", "title": "Skillibeng", "thumbnail": "https://yt3.googleusercontent.com/fJ44_nL_u3ZBCwkyqhQ9SdDdkYKrLNLzlsHJpetzafRZi_AgtxDaVJQjiKACND32CSPLT1jvGJcxb7M=w120-h120-l90-rj"}, {"id": "UCSTyhCNj3MK4gRaxszgxLAw", "title": "Koffee", "thumbnail": "https://yt3.googleusercontent.com/3hKmD2dzy3fw4oYKfmoZa93XuOBDQ-8rGrstsMxoGHdAW2xjOZzCVCn44g_iYpukhNAqQGqUhuBDhSQ=w120-h120-l90-rj"}, {"id": "UCmyAfINxPaCvzEKMCHvjytA", "title": "Konshens", "thumbnail": "https://lh3.googleusercontent.com/2RYemtVhLd5xDFSP3TQ_AaC0Fq4suW946grAobs5W7Bd4PyQMdb2nujzKb7kTPOt3tpdJYeYqH5sqA=w120-h120-p-l90-rj"}]}, "Amor": {"songs": [{"id": "7Z5Y3y_bGE4", "title": "Richest", "artists": [{"name": "Muni Long", "id": "UCmHHiuZ1EvVLF4QlTAXaRAA"}], "thumbnail": "https://yt3.googleusercontent.com/hgPVXxWlaru9rHGA3lnWQKzAZxQNw4D8YCn_djHyNW5IRyMxcFj5E4b-PmVNzGp9_8cBsh92Jm4PUPA=w60-h60-l90-rj", "explicit": true}, {"id": "yj195WtppoI", "title": "touch myself", "artists": [{"name": "kwn", "id": "UCr8-WvB5PPZg_qJsHdg9cmQ"}], "thumbnail": "https://yt3.googleusercontent.com/K9fZp_xpSmE6ee0iqtv-Qav6qHSKlcHhx_VDqKD_XD8fN-kU4NrJnZwXqilr74ePZ67VfXsTP5jHrxH-bw=w60-h60-l90-rj", "explicit": false}, {"id": "gnNcX-Ihq-E", "title": "Mesmerized", "artists": [{"name": "The Aces", "id": "UCo3dP7qmlYNXKO38yySErpw"}], "thumbnail": "https://yt3.googleusercontent.com/fqUkOPbgFzu8ncwOeljsLe97-Kk3zttyUpdh-DGxjEwWaGvSQ0PTqgabXsZSichkNwkQ0j_DphRh92UX=w60-h60-l90-rj", "explicit": false}, {"id": "ymbuIMEfZlo", "title": "All That Matters", "artists": [{"name": "6LACK, Leon Thomas y AZ Chike", "id": null}], "thumbnail": "https://yt3.googleusercontent.com/ob9uziMMNCIPHiuOt6uM8bPt18JTp1qU-1LLyqj7CI0-6Gs44JhvMYvU-JuBsTJHKT6Xqxs0toy2M2vn=w60-h60-l90-rj", "explicit": false}, {"id": "uq3zJCQYVII", "title": "Fallin'", "artists": [{"name": "Latto", "id": "UC7F8P8OwNqd581x9oZIakmw"}], "thumbnail": "https://yt3.googleusercontent.com/yvkwbUXvcubVKuVerlCh3r4xl0fg_1kH9A1rRVNXTZe8b7MoS8HCdql9VhRiPGUS6DyikcC3RsK3opfx=w60-h60-l90-rj", "explicit": true}, {"id": "cl6BQyQoRQc", "title": "SAVE SOME FOR ME (con Kehlani)", "artists": [{"name": "GIVĒON", "id": "UCdcOKli0OmBrQUVfCqHy7PQ"}], "thumbnail": "https://yt3.googleusercontent.com/UohdxiBNxFE8z7ifm7fHS0RgyAQjfttrlBJkWiwrwyGDTVu5WXejNgkzqpSLIUpBshofx2QZ6Xe9dXEZPw=w60-h60-l90-rj", "explicit": false}, {"id": "GONDSIdib10", "title": "Mexico Honey", "artists": [{"name": "Kacey Musgraves", "id": "UC87QNTF9qgTwe8KKN1V8NGw"}], "thumbnail": "https://yt3.googleusercontent.com/D0JJzwqBX4GoLv-FMxl1tW3ybT1xJC_iHxyNNuAhVQ5mxRd0m9ZKuZzpbVfTtI4vRa2R-MFMgqaCvW-M=w60-h60-l90-rj", "explicit": true}, {"id": "NfqC8zpCTcU", "title": "I Need You (con Brandy)", "artists": [{"name": "Kehlani", "id": "UCONwFIjhxe4MR2sY3Cv0adA"}], "thumbnail": "https://yt3.googleusercontent.com/R7G3rnFY12pj0l1-vnBQrx36GN9PyU-zZW1fOoUA9z1V-1WrSWUui8fdkuczBnNAlW_FjHCLpCXREF287A=w60-h60-l90-rj", "explicit": false}], "albums": [{"browseId": "MPREb_5bY4NPCm9wN", "playlistId": "OLAK5uy_l2CFWmQOSrr4k6a8y7hLLIV0eqYsUDtVc", "title": "Labor Of Love", "artists": [{"name": "Blxst", "id": "UCbMATzSZwRRgaMufpSpDKzg"}], "thumbnail": "https://yt3.googleusercontent.com/2NygxNjcUdwrOLxOyf7L1diMZESbeLxPmFqzqACu3zNY3gNv1R8zHRWS50o6l2yBYNGvsldmwZyl4yME=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_Q3KJrZTKVBX", "playlistId": "OLAK5uy_li3RrqAGzQ40XcmsrjHtsmQuutN2hQWq0", "title": "Love is the New Gangsta", "artists": [{"name": "6LACK", "id": "UCgerUw6uCKCZIYvD5sz5ZNg"}], "thumbnail": "https://yt3.googleusercontent.com/duBrwbKdrTo0fENkjSCSlUq52BNNSrf5NtDFoR6scj_RCyOAjHoN8Q9caaEjVGjZgFF8wEaJIjmfEdUm=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_HnvDfknjUCg", "playlistId": "OLAK5uy_mDgFExLTpd_I6szVUoQzPB3fOLFb_YPqo", "title": "LOVE, LOVE, LOVE", "artists": [{"name": "Stephen Sanchez", "id": "UCZukHZtjqY72iSEgkBF1QYA"}], "thumbnail": "https://yt3.googleusercontent.com/fBHrT0rc0QOviwU-Zp6lvun9fZjKBiJWh4PARo03BUWKF_ZXgX6v-gGxdewW3RQQgXLkeZpYYCJ37mw=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_mgGvs8cfWke", "playlistId": "OLAK5uy_mTzH1hpU8--s4ck3AfXj2WQpzI8dwNm3k", "title": "Kehlani", "artists": [{"name": "Kehlani", "id": "UCONwFIjhxe4MR2sY3Cv0adA"}], "thumbnail": "https://yt3.googleusercontent.com/wmvWo2qZkRQEgjVEZ7FgvpOu2_mZ6288TwugJpTo15n8saPJSD0QWSs22whQbwnq6FnLQsnlvTBGYmw=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_kExrJbsITVm", "playlistId": "OLAK5uy_kBG2Uv1K3pQeKogyccXEsNkkh_3ry3QMg", "title": "Big Mama", "artists": [{"name": "Latto", "id": "UC7F8P8OwNqd581x9oZIakmw"}], "thumbnail": "https://yt3.googleusercontent.com/yvkwbUXvcubVKuVerlCh3r4xl0fg_1kH9A1rRVNXTZe8b7MoS8HCdql9VhRiPGUS6DyikcC3RsK3opfx=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_q0FayVwsPcA", "playlistId": "OLAK5uy_nurfX6_Jd781oC26mBh65AeJ-j_lp9Fi8", "title": "THIS MUSIC MAY CONTAIN HOPE.", "artists": [{"name": "RAYE", "id": "UCvyjk7zKlaFyNIPZ-Pyvkng"}], "thumbnail": "https://yt3.googleusercontent.com/6QK6BrV6a5hUjS7VZ_MRTKqN-sYXofTm65KIi8VqFVYQ7x7OdklKmJ3OpM9KW4VXW5U6ejgQlQJWZ08LEA=w60-h60-l90-rj", "year": 2026, "explicit": false}], "playlists": [{"id": "RDCLAK5uy_l2CXCpt8bt8t2IQ_6q0M3RuBdk2rxDqJE", "title": "Puras Románticas", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/0YD6CGQIMrfwWyh93u_GeG4KU9TxFXe1yghgtP4pyvJ1QHTcEyOKhfCTFIYGFH6yAJKqBdNYBL8uiMk=w544-h544-l90-rj"}, {"id": "RDCLAK5uy_nCcUEL4QJFY9mi3St0F51JjVIyDJlG9x8", "title": "Presentando a Darío Gómez", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/5pXrQddBbSLGLaSupwLa94igGhSmOT2HyTFOj2D2L97wVJ9c8zsRuv0EYEm8CBbuAyzM-M4A97VKXTo=w544-h544-l90-rj"}], "artists": []}, "Chill": {"songs": [], "albums": [], "playlists": [{"id": "RDCLAK5uy_kLwgLlrxA4-_EchctXgTyHR4rwRaRv1wk", "title": "Música House 2026", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/AHM4S_P1OOjGGUFWDxHQTUCxpxAFKOV5IzW-mttYW7OQqfgE7qQcU1kq8XFoaXQN1LAibsDu5O--sXI=w544-h544-l90-rj"}, {"id": "UC6bkN0ZmA1e_f9K2KgRGEmw", "title": "Sunny Mornings", "author": {"name": "Peder B. Helland", "id": "UC6bkN0ZmA1e_f9K2KgRGEmw"}, "thumbnail": "https://yt3.googleusercontent.com/XWkZGmXPFb3BuqtuoaHQfcSgT8NNC_rUtLAhJM8jpu3MqOtbYSCH09numS2dkcRELEEpJlIyAcVk4qCN=w120-h120-l90-rj"}], "artists": []}, "Electrónica": {"songs": [{"id": "sv9R2AcJHkk", "title": "Joy Without Sound (Sam Binga Remix)", "artists": [{"name": "TOKiMONSTA", "id": "UCg-MiMmT50XZnbSCllzhyuA"}], "thumbnail": "https://yt3.googleusercontent.com/42dqpvC8Aut2WrNLKAviA7tpeCkiOVRp1G142FtrkCxAInpZqvAqlPV1qC8FAK2bKp24VuNsy4F10dPJaw=w60-h60-l90-rj", "explicit": false}, {"id": "tJAHX-2O8dQ", "title": "Testo Skin Part 1", "artists": [{"name": "Brutalismus 3000", "id": "UCV0kzMt4a95ji7OISiVSWYQ"}], "thumbnail": "https://yt3.googleusercontent.com/kJatWjDtWmQ_cniM9IRaSkujJ9TXXd5EXNucGcohCn9pBWjNFQtSKm6l52nuye9Zx13GHCiZPQ0AHYk=w60-h60-l90-rj", "explicit": true}, {"id": "waolNvoONaY", "title": "Beyond The Brilliant Haze", "artists": [{"name": "Everything Is Recorded", "id": "UCbaeAEqkf1HZ1cL0sUIwcnQ"}, {"name": "Peter Gabriel", "id": "UC7dI7qJoDsMd388DTtcwIZg"}, {"name": "y", "id": null}, {"name": "IDK", "id": "UCl12byCjZfy2we2aS19HWIQ"}], "thumbnail": "https://yt3.googleusercontent.com/oNv-CJ-qpNQAeo4NrzWWXBqgeKNcHs12mV1zYH0z8-9ZUkfCRAjvzvqe8ZrSSN0RMALBPlI1fKlCaVM=w60-h60-l90-rj", "explicit": false}, {"id": "vI0ZepKdTGo", "title": "Red Passion I - A2", "artists": [{"name": "Robert Hood", "id": "UCVLq2zki5Kn7M3MDZf_vCuQ"}], "thumbnail": "https://yt3.googleusercontent.com/h6Ugglcqm40s0bISr-yAAEABYs10HtFTdb8dd4qWt5nu6TOkmtyaoQ9vrjZ08ULwNHAwAGXNtL7Fdi27=w60-h60-l90-rj", "explicit": false}, {"id": "phT4E8jQBvg", "title": "Paris Worldwide", "artists": [{"name": "Busy P", "id": "UCxM_GarkaaRx1P1hBOsM3Cw"}, {"name": "Myd", "id": "UCpz7Gxps3NmieyoQ9FncoTA"}, {"name": "y", "id": null}, {"name": "twinsmatic", "id": "UCHqB0KwH4TXOEElUqFtoe5w"}], "thumbnail": "https://yt3.googleusercontent.com/xsM3Zy0oNLcfj-RHSDb8EvLu0ofFh3vhzk79VZe8YiLzejw6CaKhNCIaWyAMVkMOFjMifmctbDEyhP_O=w60-h60-l90-rj", "explicit": false}, {"id": "87pa2F2aP_Q", "title": "Next Life (con Rromarin)", "artists": [{"name": "Dusky", "id": "UC8lA9PbQS2YkOEuvpcsfR5Q"}], "thumbnail": "https://yt3.googleusercontent.com/jPB1JDs7T6b5fK8NTRyrHoswth3Ys3iuKbr0352ATHa0CYKCHaQnUDIeuP9u1eeMVLr_3OR9Pv4gNsk=w60-h60-l90-rj", "explicit": false}, {"id": "iHEl3X7ox34", "title": "The Edge", "artists": [{"name": "TYSON", "id": "UCDISfBtbnHkncYITMxew9PA"}, {"name": "Nosaj Thing", "id": "UCg-Tm8zz9_VME3VvV9mVN0Q"}, {"name": "y", "id": null}, {"name": "Coby Sey", "id": "UCwCuAjkz02e24IUadzj8JZQ"}], "thumbnail": "https://yt3.googleusercontent.com/3Fr5nbQC3-HfkVGMwO-twbfEqMJ9Y_YPYz6h3GrTWJBTb8siPVucXTSsIZbUVLtA0FLOjmTzrsQe-1c=w60-h60-l90-rj", "explicit": false}, {"id": "qQ2UbGB5LN0", "title": "Coda (Loraine James Remix)", "artists": [{"name": "Rival Consoles", "id": "UC2WctdCmq8m617GDyuynG0w"}], "thumbnail": "https://yt3.googleusercontent.com/T5bOKBjXLod-tV5YQMu3ya0BCECm18_Yv2-kea-MPVMRGWuys1JE-V-6jxYhjDXa2AU-gd1VRpt5aYw=w60-h60-l90-rj", "explicit": false}], "albums": [{"browseId": "MPREb_UKhLIZd7slN", "playlistId": "OLAK5uy_kXVkPcdf0y9h8Im_G6XQ3YM_h18WBGrV0", "title": "Geogaddi", "artists": [{"name": "Boards of Canada", "id": "UCidyEq0ZC6rcqZmwKt_g_2g"}], "thumbnail": "https://yt3.googleusercontent.com/NHjsmfvfTSL8V6Y5OP8MkTlJWywijWgb4FbGeEFi5qOI7Oc-1fi07IoJkOINPD8FBQ3HQ5t1nif8K4IV=w60-h60-l90-rj", "year": 2002, "explicit": false}, {"browseId": "MPREb_G6JQUdInW98", "playlistId": "OLAK5uy_ml6FroEsQ8i3oiTjJNgpTbb-DzmOtxFvU", "title": "Dummy", "artists": [{"name": "Portishead", "id": "UCweAx0TIRdAylp6G1nviqbQ"}], "thumbnail": "https://yt3.googleusercontent.com/IrlcyPIAb0ceQm5CcqFnn0LJZYby1WCnEalLuW5_FixHlflBUH6Zm1nCnqimWY2bYd3UP36PKygOLI-cjA=w60-h60-s-l90-rj", "year": 1994, "explicit": false}, {"browseId": "MPREb_CtOaObihCXA", "playlistId": "OLAK5uy_l3hwK_TnlJHsWku43dzTtHx_IjH3VIujs", "title": "Alive 2007", "artists": [{"name": "Daft Punk", "id": "UCRr1xG_2WIDs18a6cIiCxeA"}], "thumbnail": "https://yt3.googleusercontent.com/WcxKKKOBAyf3a4qI8N2FjOyylr6Th1wDdoofTEO3rDw5kmjI-DF2h0hPJeGURrCqPCs9BIhJAQj3f10=w60-h60-l90-rj", "year": 2007, "explicit": false}, {"browseId": "MPREb_K8qWMWVqXGi", "playlistId": "OLAK5uy_kNhM2yaBTOVwrcZJepB1C9P3-n5_Sfy5c", "title": "Random Access Memories", "artists": [{"name": "Daft Punk", "id": "UCRr1xG_2WIDs18a6cIiCxeA"}], "thumbnail": "https://yt3.googleusercontent.com/N55arCGj69gtw6thXK8JUPisxoVYiwuIEQ7I6SGlkEyNcSJ7xIWPe76Vuu1SiUqRyx5w9qvR_zV8fV3CWQ=w60-h60-l90-rj", "year": 2013, "explicit": false}, {"browseId": "MPREb_7ltM34kr0mH", "playlistId": "OLAK5uy_mz6eafmqdRHSaR4IwG0ll6J6rgv0_ZpGw", "title": "Discovery", "artists": [{"name": "Daft Punk", "id": "UCRr1xG_2WIDs18a6cIiCxeA"}], "thumbnail": "https://yt3.googleusercontent.com/qrY7xjdjqL0FEneZKHk845JeeFjdWOgexUa_BhsUxskJ2iflhzpbofqGJZBcPHDGU9JjoZflE4yn3P74=w60-h60-l90-rj", "year": 2001, "explicit": false}, {"browseId": "MPREb_yDIH3qWqx1q", "playlistId": "OLAK5uy_kSp5_YinmyJIGiwKBV-usy8bmGgd9H9Q8", "title": "Another Green World (2004 Remaster)", "artists": [{"name": "Brian Eno", "id": "UCoWPCwCsQmBwoXBCL_laaIw"}], "thumbnail": "https://yt3.googleusercontent.com/nyf-jkrMuNgl_XpRTGq8OGF9cMp2r552K8BklPyBzKutbqtdxmUMQ4E6qDlQ-Nj5mCzWgFiKTu_nC_o=w60-h60-l90-rj", "year": 1975, "explicit": false}], "playlists": [], "artists": [{"id": "UCCyCRm3QIkARlmi9JM6rZnQ", "title": "Invt & Langi", "thumbnail": "https://yt3.googleusercontent.com/JRTm9No1tU28Z4C8RPoMfOTLa8pEAyQrSnV4TtWpC5s2XGWcMr3yqLZbsFlrWXh1jtbRv_J-QjrRqmwFZg=w120-h120-l90-rj"}, {"id": "UCV0kzMt4a95ji7OISiVSWYQ", "title": "Brutalismus 3000", "thumbnail": "https://yt3.googleusercontent.com/V_s7wgrKPCMOrZRBKy0nJkUxOq461-pRMzSXcpj5K1EE1-QCHVje9GrmIOacz1cyPUxHnO_cCA=w120-h120-l90-rj"}, {"id": "UCIzUuNT1-rqqfJCdRRVGBBA", "title": " TEED", "thumbnail": "https://lh3.googleusercontent.com/pgwGXPiR8sLR7cxI6E7PzuHCuW9iXeNqjc0VzO3Z33GMbjxNDwOlcr6cCK93n07P1wuUzB-5GNunUQ=w120-h120-p-l90-rj"}, {"id": "UCwtK9ozAfl6bsN6wfwrhKbw", "title": "Tim Sweeney", "thumbnail": "https://yt3.googleusercontent.com/6k0yqe2X3dPccZDz7JvvQ5LLCgL_HcPXIxLaKlGRsRCneaWVcFN9Xr3_CNBghEleFkYoBMGB_lqnQphu=w120-h120-l90-rj"}, {"id": "UCTn8anEMvCsxpCMhyajKjrg", "title": "Naina", "thumbnail": "https://yt3.googleusercontent.com/wSihPT0UCMfp_S6xWOzMttRlzFw9pwhwpl1dgtLJRPwTnUie-aacr_CB7QA5hKs7V6ui3w7922WYbvbj=w120-h120-l90-rj"}, {"id": "UCDAG7NYE1huFqS9JF4ouO0Q", "title": "DJ Plead", "thumbnail": "https://yt3.googleusercontent.com/oia9B2Z5hcxDhqEI2M0oVVHRkrRZ3Wnt13Zz7ZWwlyQvX69KSY6Y8IAJzWboyRrqLhQUQ3RrQUHawf0=w120-h120-l90-rj"}]}, "Tropical": {"songs": [{"id": "o69HREcuK7w", "title": "ARREPENTIDA (con Ponce)", "artists": [{"name": "Sbm", "id": "UCKuaD4bwnA7apSchMdI5Rew"}], "thumbnail": "https://yt3.googleusercontent.com/ktiKHsR0st7YWdksde9MbESIWevRsmQQn9Lj7DM0b6NL7sWYL0I9tHBrsLwS2zjHl5cSKIKJ5uGbFDg_=w60-h60-l90-rj", "explicit": false}, {"id": "7Y5QaLh8BUg", "title": "Cariño Gitano", "artists": [{"name": "Daniela Darcourt", "id": "UCNQB-0ylGx6pv2xGqBTTSsw"}], "thumbnail": "https://yt3.googleusercontent.com/P13bi1G2BpmFAZNf4CXeNkp7cR40wG6HyT16KG5hEqn-KwiIEBZRolMorKlXDp0DCO83puWGl_zDSzIy=w60-h60-l90-rj", "explicit": false}, {"id": "nFPo7Vl-lZc", "title": "5 Minutos", "artists": [{"name": "Luis Figueroa", "id": "UCQATiAKZxUgPfbdMkAzx3Vg"}], "thumbnail": "https://yt3.googleusercontent.com/J2o1gaM5QOs3O7LCfznF6zKZN4dtl98YD25dYYvPMOTdXhPhW3AOjG6NG64-5Ak-Djuda6QnmMCwuMQv=w60-h60-l90-rj", "explicit": false}, {"id": "JgA4qXZDZ20", "title": "Rubio", "artists": [{"name": "Jay Wheeler", "id": "UC17u1K8tiqxxFhPMbM50ASA"}], "thumbnail": "https://yt3.googleusercontent.com/M4gC49M_z88QHhFXnc-Vgnh3pQk7TmJIavtedKqcHxWM7DYa24sCMNE3-C1HMVjT9RRwIVpwWHjN0eHU=w60-h60-l90-rj", "explicit": true}, {"id": "adBUQQanvSs", "title": "Ella", "artists": [{"name": "Moa Rivera", "id": "UCzGS2TKs5ie7h8ocycT3GNA"}], "thumbnail": "https://yt3.googleusercontent.com/ThwmQH7oKUGBjXHOJtj7y8sPR6URmbApehD94jZEF0oZrnLPryKI3EtoEq0HsOTTv0yrWm3ERSEbmtD3=w60-h60-l90-rj", "explicit": false}, {"id": "NYQWPmk5dqA", "title": "Mi Reina", "artists": [{"name": "Charlie Cruz", "id": "UCQuoUoWyonxUuaRWtbThxPg"}], "thumbnail": "https://yt3.googleusercontent.com/rPH2THSm0-Fdfx1DrT_0AeTFUqaVLB6cfddugSFd6TIWlPKgbyQ6NFM_fWhbde3Uhat-MwEl8sanaW8=w60-h60-l90-rj", "explicit": false}, {"id": "8N2Z919kT1o", "title": "Vuelvo a Ser Yo", "artists": [{"name": "Luis Enrique", "id": "UCF3wH4rmfMWjjxSK8LyszMg"}, {"name": "y", "id": null}, {"name": "Alain Pérez", "id": "UCyU8vQ_a9O7ErFoaEiuM4Cg"}], "thumbnail": "https://yt3.googleusercontent.com/4JVocAlmSRwnXylCDBbQtkIU8B-thYSxV0X2xJc0fN7e4E7zf4_7KSdhP-GVcs07BdhrvtvH56OYKrBdAg=w60-h60-l90-rj", "explicit": false}, {"id": "QzHH2seZtZk", "title": "Quiero Estar Donde Estés", "artists": [{"name": "Tony Succar y Luis Enrique", "id": null}], "thumbnail": "https://yt3.googleusercontent.com/y8w8CwB67NUzqbNawxPxM5E7tJAedOOQ7_b6OoAEqQY9u2gf4EPmWyzUtP_I6ZEaUx9NbA9Hf5zLE1fx6Q=w60-h60-l90-rj", "explicit": false}], "albums": [{"browseId": "MPREb_zqjmGVPF5Qs", "playlistId": "OLAK5uy_nDlG2NWsozsJIRJiOnr5vu1Fv9MVeniuI", "title": "A Pesar De Todo", "artists": [{"name": "Víctor Manuelle", "id": "UCQz9VIfqA7dIeBY8oBuj89w"}], "thumbnail": "https://yt3.googleusercontent.com/fdXykG4RjuGU1_-gqLfMUuq8OmuRkR09j7naXMYT4V9CBmBpzwq3oZSjUcTVyTda2Y6POUa1S9aGUYVd=w60-h60-s-l90-rj", "year": 1993, "explicit": false}, {"browseId": "MPREb_z0HmSmVcAzT", "playlistId": "OLAK5uy_kexqoJP4QdXaifTjL6KazUHCzc8gN2LnM", "title": "Oxígeno", "artists": [{"name": "Willy Chirino", "id": "UCtWQDzuH1e84SebEyZN_aXw"}], "thumbnail": "https://yt3.googleusercontent.com/KPGvN8mVkUMGmKGkakR7hg14HKkw7MCb9-LEvIrpp8VEmbWr5Yrys-5rnyfDaaEgvVsMWV6Utews41si=w60-h60-l90-rj", "year": 1991, "explicit": false}, {"browseId": "MPREb_wX4449DSyFX", "playlistId": "OLAK5uy_lKssudXXi1wZjAXs80CNn4NVfzxsET1T4", "title": "Ojalá Que Llueva Café", "artists": [{"name": "Juan Luis Guerra", "id": "UCxtvuCXJMwEzD0_t8GbZcNg"}], "thumbnail": "https://yt3.googleusercontent.com/mIqAb9us7hhqcxDz1z6VAjGUS8R457OxkWc58GcdakoHinocV5PUpiOHSjKC7_BfIRLaiS03hohOB10=w60-h60-l90-rj", "year": 1989, "explicit": false}, {"browseId": "MPREb_wXvo0AmcmG2", "playlistId": "OLAK5uy_k8IzwamqPzuFKEj9I0oehzGV4V4QHr4Jk", "title": "3.0", "artists": [{"name": "Marc Anthony", "id": "UC22hscWzcVvD_CEPiuaOIbQ"}], "thumbnail": "https://yt3.googleusercontent.com/wRwHIk-1cuAAwJql_eJksVQxY7B3mrH9AiZGiRrG5VCAEr686NGyHclX-R_fA2UDgGEgO3Npcm2u20A=w60-h60-s-l90-rj", "year": 2013, "explicit": false}, {"browseId": "MPREb_mH0HIK7XJW1", "playlistId": "OLAK5uy_n8sPplnDwCtYuDjaC7zVdebfIWlpZrb4w", "title": "Esos Ojitos Negros", "artists": [{"name": "El Gran Combo de Puerto Rico", "id": "UCCUMiyrr0FKstYGMzCrsSng"}], "thumbnail": "https://yt3.googleusercontent.com/NW4vG-XMGh4la8KEg28mwiavLmtpo4JfncY77IyCrhWrmsgOnOqGJRm7jRvSmIAoMOWOUjCkNFSZvvkn=w60-h60-l90-rj", "year": 1966, "explicit": false}, {"browseId": "MPREb_vh65EBPUHw1", "playlistId": "OLAK5uy_lH8G48dq6zctchQyMn9AghBnL2x5iJLsA", "title": "La 9a Batalla", "artists": [{"name": "Silvestre Dangond", "id": "UCHgCGCFOoCaY6IB3nJC17Yg"}, {"name": "y", "id": null}, {"name": "Rolando Ochoa", "id": "UCT8LrWNWeniRguRu6Nj8w3A"}], "thumbnail": "https://yt3.googleusercontent.com/TjhEZdahmIiaAIv2TyDmsWTiL09eINFb9lphLC7dVpeoSH-TF6baTxEA0VLNZPRLUudXvFhknrasOiH-=w60-h60-l90-rj", "year": 2013, "explicit": false}], "playlists": [{"id": "RDCLAK5uy_mdfCrhJVBuFLwKQ7fnc-als6jwK0K1bOs", "title": "Éxitos de Cumbia 2025", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/_b4rVrr4dJGjryvk20Dzj8qEovtzEuHZ2283sAgXpYY9LDqeCnHftqoXBJljBlIZjD-GCmeSNv7y4jg=w544-h544-l90-rj"}, {"id": "RDCLAK5uy_mfJPO81EB5EY_CIkovCnH0-cVbqr6SHzQ", "title": "Pisto y banda", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/a7bQc3woWNTlZKDRxi32HGTpqQv8OSroPiJ5M9YPXy8cVTCACmd5LcHzjTLZfCslyemSaQE-23od1oU8=w544-h544-l90-rj"}], "artists": [{"id": "UCSOgRRwjSlsQzbNqZSNNeHQ", "title": "Christian Alicea", "thumbnail": "https://lh3.googleusercontent.com/THo50G4M8MHHdQxmVyDik5EsvMlLLBwu5qUzs8vTNHHUQF27hwkfv7pITDPk3F8DHIIC6AnVJVXR6-4=w120-h120-p-l90-rj"}, {"id": "UCNQB-0ylGx6pv2xGqBTTSsw", "title": "Daniela Darcourt", "thumbnail": "https://lh3.googleusercontent.com/jP959A9L4f7ofnIbSYsyzmCN4W7zJjWj1TPaRRR0572820pyKrDgAQDcbnReS2qGN5Q4lGKUe1Z-ow=w120-h120-p-l90-rj"}, {"id": "UCQATiAKZxUgPfbdMkAzx3Vg", "title": "Luis Figueroa", "thumbnail": "https://yt3.googleusercontent.com/iH0DIlCBRs4PJeQndLvsWFKdDgsqofEeMl-PyXpBZ8u4ktam6DmiuaDtiCQC18_-PgEFSCNOCZNB5w=w120-h120-p-l90-rj"}, {"id": "UC22hscWzcVvD_CEPiuaOIbQ", "title": "Marc Anthony", "thumbnail": "https://yt3.googleusercontent.com/2jzwkvlT1a_Jz9rI4Kn_nrb8w9syV-agEJqB9zmlkLxY_0k2VvvEvXbigHN5BqYalRN1ThwVeQlPQhiW=w120-h120-p-l90-rj"}, {"id": "UCpB_98tUTs3zSiOxZuGPnOA", "title": "Romeo Santos", "thumbnail": "https://lh3.googleusercontent.com/RGeXyCNvEmuIeFgv3SwMyeejKoRjEuAHcVZ6BCahIH1tNdIVyFPi75se2ZeAZR4cdmkI7HPLQ-drMxY=w120-h120-p-l90-rj"}, {"id": "UCuDaqwAmogKMCUVn_z3wDTA", "title": "Celia Cruz", "thumbnail": "https://lh3.googleusercontent.com/BYWrvNr34HpAqy8c1nKD0pKiMz3rhiA95E-K3YnDv47NA7Y1nQg3XUqtBZatSXOok8XovrWktGubLA=w120-h120-p-l90-rj"}]}, "Pop": {"songs": [{"id": "bqsAhnJ-QAY", "title": "Wink Wink", "artists": [{"name": "Charli xcx", "id": "UCI4YNnmHjXFaaKvfdmpWvJQ"}], "thumbnail": "https://yt3.googleusercontent.com/AI3LBsdjSNsSC7_TJPXWg0t7BDD3hOyYRdSKnZlBS3TZqANoVR4HcILRLVNHqnD_tfgJPvRZHedWPk6U=w60-h60-l90-rj", "explicit": false}, {"id": "4BsBpOCaTrA", "title": "SPEED DEMON", "artists": [{"name": "Justin Bieber", "id": "UCGvj8kfUV5Q6lzECIrGY19g"}], "thumbnail": "https://yt3.googleusercontent.com/3BZ9hQe5RNIp1ryJRbHc-eaqNZgY3aLu6fIhPEWLIk8BCklb2dzM9_cMuvbIMFX6ZVzaFp8rAvi3Kn8=w60-h60-l90-rj", "explicit": false}, {"id": "n1o0mKbpivc", "title": "The Time Of My Life", "artists": [{"name": "Benson Boone", "id": "UChN7Bcek6HoXA-j8K8T7olQ"}], "thumbnail": "https://yt3.googleusercontent.com/Ur_A6EOE17MzuqHuSMw34jUEgZzdwC70AvyXQdBEl3i-f30Qtp39NobXXF52WTjjRwyvwhtKgbwhUEXi=w60-h60-l90-rj", "explicit": false}, {"id": "GqQIhi86k7M", "title": "Look at My Life", "artists": [{"name": "Gracie Abrams", "id": "UCw-0GSqznYHfyfDBBe6a46A"}], "thumbnail": "https://yt3.googleusercontent.com/lYEW8QSpdpVcS1PISLrsGVGnUHv5R4CjEzRRHvNYwDlamAi3UVdQUxc2lTl2urUOLqTvgQRPEDLhOmwB=w60-h60-l90-rj", "explicit": false}, {"id": "eXw2R_CTlU8", "title": "My Body Isn't Ready", "artists": [{"name": "sombr", "id": "UCyI0V6RmULLsRxegMTCmkWQ"}], "thumbnail": "https://yt3.googleusercontent.com/ZxUlUpFScaaQK4qVMyADJkxs4CyWj_5oMaGvh6-xaPdkjvmedR0jl6GVF3ioB8eiqRC40tWKo7fqPnkj=w60-h60-l90-rj", "explicit": false}, {"id": "iex9D7pxYbc", "title": "Saturday Night", "artists": [{"name": "Ravyn Lenae", "id": "UCN8pGtbprga7vJL5B4dhXrg"}], "thumbnail": "https://yt3.googleusercontent.com/CuarBayZIGXzi-BFJRQZyW8JROTTGYrRyB9L_U1SjSrd0vBePVPCxaG2mad2kVa4YLnxLOYeR-urGYqRbA=w60-h60-l90-rj", "explicit": false}, {"id": "-3D_8tVKuQA", "title": "is it cool? (con SZA)", "artists": [{"name": "Steve Lacy", "id": "UCju-DqP7JNtCnMWFXhLgPHQ"}], "thumbnail": "https://yt3.googleusercontent.com/LO8pSxyVksaGb7_zF30gRTTHkoSqZiMynPAD5fJT884DVXyVyxHpa6Lm35pStA5rVUXJDGtDuvwkKX0mZA=w60-h60-l90-rj", "explicit": true}, {"id": "s_pYN0sYk_c", "title": "Watch It Burn", "artists": [{"name": "Katy Perry", "id": "UC_7s69e1mDS3lgcTMJEPjCg"}], "thumbnail": "https://yt3.googleusercontent.com/DCMfRp0y7ZpplOyYrMY3JZxoLyOoTaDvg1IHmgKXzTXFZy9itdeWqBa1pv8SMJoQGiTdvcGik-jpb1Ru=w60-h60-l90-rj", "explicit": true}], "albums": [{"browseId": "MPREb_5XXbjpMgJar", "playlistId": "OLAK5uy_ncKvykObOe16WxrmZEAlCbWBRZm2Utaqk", "title": "1989 (Taylor's Version)", "artists": [{"name": "Taylor Swift", "id": "UCPC0L1d253x-KuMNwa05TpA"}], "thumbnail": "https://yt3.googleusercontent.com/gj8tAERbPzlAoLI6knG2Yv1JiB9kv_pFwckfFHWUS6RcnQonszeMyxTdDEdOxCRNFmMezvwEBeRPwNSy3Q=w60-h60-l90-rj", "year": 2023, "explicit": false}, {"browseId": "MPREb_QXhvVewyH8p", "playlistId": "OLAK5uy_lT9WWb7WZ6MOzwNkItv6Azu835h17W_L0", "title": "21", "artists": [{"name": "Adele", "id": "UCRw0x9_EfawqmgDI2IgQLLg"}], "thumbnail": "https://yt3.googleusercontent.com/uW_tkmfAQ88AINvZLR-yjUeVlzZZdyifmXZaHHcaQVDpur61duSbBjQJYTJQuansxbuEA06WhU4SlG-Z=w60-h60-l90-rj", "year": 2011, "explicit": false}, {"browseId": "MPREb_OVSyn3JG7Ru", "playlistId": "OLAK5uy_mu3voG4KzkhRlc7eIWpkOieVW-I5GFMKs", "title": "Harry's House", "artists": [{"name": "Harry Styles", "id": "UCVacQ2t5GUZ2t_J3Ia9BynA"}], "thumbnail": "https://yt3.googleusercontent.com/F618Qwn2yRlYhCqlMtEMnFHajg4rGZSGeWOF4ro7l3I9R6y7aGfowqqiNQqj6CgVR0yepTK6T5aRSfAF=w60-h60-l90-rj", "year": 2022, "explicit": false}, {"browseId": "MPREb_tzu0uaSBSSB", "playlistId": "OLAK5uy_l-ANj6gH8Ju8zsPmRkvtzIC2RFb6V_jbw", "title": "Back To Black", "artists": [{"name": "Amy Winehouse", "id": "UCMRsEwcN5cXdvqNP-UBup_A"}], "thumbnail": "https://yt3.googleusercontent.com/cuGStBIsXgVSN_aJdhFPQE8atY1Q5Vi7dkTkZJwkCYWYMqXMwqFmTlxcbsQY3USokdEaF_IKB2vLpT11=w60-h60-l90-rj", "year": 2006, "explicit": false}, {"browseId": "MPREb_iWdtzQKst5b", "playlistId": "OLAK5uy_mqO_kf7YFPjbtEU3FYSILA-IZtxv_NrKQ", "title": "Future Nostalgia", "artists": [{"name": "Dua Lipa", "id": "UCzVb0SIXp9q9PeKCcFjsBtA"}], "thumbnail": "https://yt3.googleusercontent.com/UpJ_IhBqyhQV9b2UGcDxxWDm14kRQ2eY1o9S96AGsbE7Ol8isbpbPA0Yefvg8S8ZGAX9L1g4xaj21zVJ=w60-h60-l90-rj", "year": 2020, "explicit": false}, {"browseId": "MPREb_oEtF7kEGSFW", "playlistId": "OLAK5uy_kEXvla-TA6hgdjALrM04oRCD0xay4xc54", "title": "SOUR", "artists": [{"name": "Olivia Rodrigo", "id": "UCE5XNpliPM-SmyFEp61tL_g"}], "thumbnail": "https://yt3.googleusercontent.com/-GF5jStF-HFmg6bWDY0j9vB--4F0GXBwoGgn5Pe0u3TlltUqFISBip0Y4mYbzYPjaFX97TmZVBw03o1h=w60-h60-l90-rj", "year": 2021, "explicit": false}], "playlists": [{"id": "RDCLAK5uy_l2CXCpt8bt8t2IQ_6q0M3RuBdk2rxDqJE", "title": "Puras Románticas", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/0YD6CGQIMrfwWyh93u_GeG4KU9TxFXe1yghgtP4pyvJ1QHTcEyOKhfCTFIYGFH6yAJKqBdNYBL8uiMk=w544-h544-l90-rj"}, {"id": "RDCLAK5uy_mPolD_J22gS1SKxufARWcTZd1UrAH_0ZI", "title": "deep chill", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/W-d-Yf4kNuESQAswMbEcXhYcyn3A-bbTEZQK9vzJhyrOuIIhnzi0PyKunsVCPNtMOg_Cu_-F0DeP00gk=w544-h544-l90-rj"}, {"id": "RDCLAK5uy_kb7EBi6y3GrtJri4_ZH56Ms786DFEimbM", "title": "Lo-fi Loft", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/9I45SYZAfTeR17ByV9i2yDpWhQ5DTWMCrW3Lei8vMIiZzeyUnf2zRFUDBob4TGwgUkXAAR_x1Xa8VA=w544-h544-l90-rj"}], "artists": [{"id": "UC6GBKGYX6b3guHlqHLx6IzQ", "title": "Olivia Dean", "thumbnail": "https://lh3.googleusercontent.com/PRdjE74hC3IjfFuJvT8F3DDgVWu62knmknY0a8lNoepQbONjZ-TCg9VysjfU_hwcra1Vaqhjj2qErXNZ=w120-h120-p-l90-rj"}, {"id": "UCE5XNpliPM-SmyFEp61tL_g", "title": "Olivia Rodrigo", "thumbnail": "https://yt3.googleusercontent.com/41-4WZupE4yY88igineZefzBZ3ud2nrtlBMv61OBWOfOcATol8PhmI5OZ0fLlrTszyZ3Ul9I9sE=w120-h120-l90-rj-dcqUWI7R0J"}, {"id": "UCPC0L1d253x-KuMNwa05TpA", "title": "Taylor Swift", "thumbnail": "https://yt3.googleusercontent.com/RCpTA6EXJQyjVFDosWOKa2SMmqkua_lA9mHPDWWciLwgqpZLz-k8rXWRF_367trrQ7up9BUwCbk6kRk=w120-h120-p-l90-rj"}, {"id": "UC0076UMUgEng8HORUw_MYHA", "title": "Ariana Grande", "thumbnail": "https://yt3.googleusercontent.com/DU6Kpr5TYKcW6QHvMnsJau5_8QSuix8LCLtf5UEaziZZdXw8SxvcxJ9YWmVIQuzhg2R-MVHYgjdGCQ=w120-h120-p-l90-rj"}, {"id": "UCoIOOL7QKuBhQHVKL8y7BEQ", "title": "Michael Jackson", "thumbnail": "https://lh3.googleusercontent.com/x-Z35q6HsBB98J85-4oNqPnOen4pZBaNCpHawzf_ejs-pkgh6Eh3D2Fu7S1T4gEj0yWZ0c6DOAHCpA=w120-h120-p-l90-rj"}, {"id": "UCGvj8kfUV5Q6lzECIrGY19g", "title": "Justin Bieber", "thumbnail": "https://lh3.googleusercontent.com/4ULlRiFBFglNemZJyKn6_e2-iOIdJEbgBgq_79RQclndG6pge0yGgS2k2On6E1FkCJzenyHkHRzkvjFp=w120-h120-p-l90-rj"}]}, "Música mexicana": {"songs": [{"id": "GhMX5llEOy8", "title": "F's", "artists": [{"name": "Fuerza Regida", "id": "UC0kxNxFQCK6d2spPz5Sme7Q"}, {"name": "y", "id": null}, {"name": "Gabito Ballesteros", "id": "UCYMm2JZ_mvXYr7vT9-8_thw"}], "thumbnail": "https://yt3.googleusercontent.com/ozzMHKW5Dj7nLOg4_dPiiBlMn5Q-tudXQ847sYrB8CEAtP61sdpK9a6pVfPekXlBfpxjWW6Ce3uMmclV2w=w60-h60-l90-rj", "explicit": true}, {"id": "mrMa7iGNNyM", "title": "El Precio", "artists": [{"name": "Kane Rodriguez y Linea Personal", "id": null}], "thumbnail": "https://yt3.googleusercontent.com/1pOeqQkhaPqYQk2mEwG8l8zpf2Sr3GFIcWnYxy8fPlBvJT6rmBsiPAbX4N6dlAO59q0daPlcFna486Y_=w60-h60-l90-rj", "explicit": false}, {"id": "8K66jQwrQXs", "title": "VACAS FLACAS", "artists": [{"name": "Hermanos Espinoza", "id": "UCFKk52XodaYhh_va8y0aOKw"}], "thumbnail": "https://yt3.googleusercontent.com/yEXRdcGutGkkm77it4u38GBZf51WxJqNltIdRJtihVUWN3v6g2YhSXII-7OhPWjDpG4UAxhuX4cQ9Pc8=w60-h60-l90-rj", "explicit": false}, {"id": "dKerBIqzodQ", "title": "Fantasma", "artists": [{"name": "La Maquinaria Norteña", "id": "UCOmcdWJBMfD3uTFeLsXRgkg"}], "thumbnail": "https://yt3.googleusercontent.com/1ytuArYvrGEdHXTvKEpIzdZFE12iTf6DFYe9fgeL5zcQpvXNVaBPS-IINx8q9MUVLD7cpJTFiUNYkN0V=w60-h60-l90-rj", "explicit": false}, {"id": "c2yVqyNalfI", "title": "AMIGOS CON DERECHOS", "artists": [{"name": "Eslabon Armado", "id": "UCmqrOR5GZcSNS5BLAAt6hPg"}, {"name": "y", "id": null}, {"name": "Peso Pluma", "id": "UCzmabbKsmXlWnI9N2kKQ4lA"}], "thumbnail": "https://yt3.googleusercontent.com/LiBBldVQV0LnU_o1BfQUNDEJBb8fs9uQPGUC2IiVbE1aep4Cw55DdiPXBaxPuGthe0eTBoHU0hFZoWQ=w60-h60-l90-rj", "explicit": true}, {"id": "0tZ1wP1Qj2Y", "title": "Tijuana", "artists": [{"name": "Tony Aguirre", "id": "UCGGPvF2zcSkJ5mLXB43Hvzw"}], "thumbnail": "https://yt3.googleusercontent.com/8R8JrvuInN5N5WAXZY9O3zat977wCUNdpSDV4AZcRXr1or-YREZGgci1Z_X2UCtovuo6CFdr8_MYAIbC=w60-h60-l90-rj", "explicit": true}, {"id": "1k7m-VL5a9Q", "title": "Quédate con Ella (Versión Mariachi)", "artists": [{"name": "Natalia Jiménez y Mariachi Internacional CHG De Gamaliel Contreras Huerta", "id": null}], "thumbnail": "https://yt3.googleusercontent.com/u6P1XYi97RKbDYQwtecOfwNjHwXMxw-SrJqkQJP1Sr7W7COFuCiBujarAzyZ7_4QTRmVfZaCuGIbGZs=w60-h60-l90-rj", "explicit": false}, {"id": "ngNk2bTaxEQ", "title": "Explicación real", "artists": [{"name": "Espinoza Paz", "id": "UCZOfC1kYm_MZeT-Kq-KYHvA"}], "thumbnail": "https://yt3.googleusercontent.com/3eX5l7Q0JF5I_0_CMhevvKbknZTFENDVtVo6zICKxwn27XHgNsa4PsJgsO7GRzmEo35ZkTrvkezwVtQ=w60-h60-l90-rj", "explicit": false}], "albums": [{"browseId": "MPREb_YUaibceeR5s", "playlistId": "OLAK5uy_nBVgcTpX9oU-01ES5BKO0njhyjO1_MHwc", "title": "Atrapado en un Sueño", "artists": [{"name": "Junior H", "id": "UCwA5HHGsAlpU1UlhwWD52uA"}], "thumbnail": "https://yt3.googleusercontent.com/QodBd2ZrsKX0bY-pz2YRXa3J77q0fSujkYy8mKCWb38t_eSDhfseQu03I2qM1xOUvPF-9kIhNmdrWLeU=w60-h60-l90-rj", "year": 2020, "explicit": false}, {"browseId": "MPREb_V9AH3Yvnosg", "playlistId": "OLAK5uy_msWBT3_S-_KtQdcTUBAssoRrN68n3AzhM", "title": "Corridos Tumbados", "artists": [{"name": "Natanael Cano", "id": "UC47k7qXysCBKeaYfc1zmkIA"}], "thumbnail": "https://yt3.googleusercontent.com/TRpqwxjClVAKX9xD3wfdXMb3gZeZOq7Vgj7DZ_cszqCa1aOb8YM5Ba8tEO329geXyxSqo6tXZgtWdJuvTw=w60-h60-l90-rj", "year": 2019, "explicit": false}, {"browseId": "MPREb_yprcB9fcqdv", "playlistId": "OLAK5uy_lLmiREEHVVIaKI7IenQn61bhPG06K5KL0", "title": "Vicente Fernandez Para Siempre", "artists": [{"name": "Vicente Fernández", "id": "UChYPL8XRXNqerL5hLkd9E9g"}], "thumbnail": "https://yt3.googleusercontent.com/sl3b10iS4C7SxSrWtoQXsXQ15efwTHEiDQp-4lmGaoTdVQ1SbYgITkFIekgkMVjS43rZDmRso_2utfzoig=w60-h60-s-l90-rj", "year": 2007, "explicit": false}, {"browseId": "MPREb_J27C39cQSZB", "playlistId": "OLAK5uy_k2eEKwuuKzQkZ0EpD1JAwMD0Lxyt9RY7U", "title": "Me Dejé Llevar", "artists": [{"name": "Christian Nodal", "id": "UC_nxbep6wWeIEVb1UKn4euw"}], "thumbnail": "https://yt3.googleusercontent.com/GSXUc-5XUXUdTR4N5QupYKnOt07Cz9IvVg9Qq8ElQLE_avxuTXXbp6Lo1QlQKUIJBsi8iKGDHIeNFQEr=w60-h60-l90-rj", "year": 2017, "explicit": false}, {"browseId": "MPREb_xYwNDFctPlr", "playlistId": "OLAK5uy_n1i2HFfg5YCSClolTrAuGzyyi5cZa1b1c", "title": "Jefe De Jefes", "artists": [{"name": "Los Tigres Del Norte", "id": "UCaflwdWdaSGPNSyrAG0Uffw"}], "thumbnail": "https://yt3.googleusercontent.com/CEZgAmTguRk0RJe2DpXpRen_1kvaq3O7rUEdAack_BQHiglw7b_fqrS4QikqBqqQybwiNqaTlUZw__-zJA=w60-h60-l90-rj", "year": 2006, "explicit": false}, {"browseId": "MPREb_L0ZFTuzDZ45", "playlistId": "OLAK5uy_n2wMgq35J-RP4t-dNdVC_OFNoKTO5ujKs", "title": "IV", "artists": [{"name": "Intocable", "id": "UCANvYQ39J_y0MrXCdnaXpcw"}], "thumbnail": "https://yt3.googleusercontent.com/oUAtTtOZDoaZ_0ava71M7YNV8dtiSSTmYl3tVjaoNbb5aGKRsfiC8WVGVmqF8nQgvN5asc9Z2iLsgwJSlg=w60-h60-l90-rj", "year": 1997, "explicit": false}], "playlists": [], "artists": [{"id": "UCfmeXjlCXi37LGF7O2VT2zA", "title": "Xavi", "thumbnail": "https://yt3.googleusercontent.com/1gMAiasXBYufoeYVfzwdzv4O3TUMr8FvuLhDpFbyhoVF0h28RW3ATyMS4eyASamQd9c_TIc2ZohAeQ8=w120-h120-p-l90-rj"}, {"id": "UC47k7qXysCBKeaYfc1zmkIA", "title": "Natanael Cano", "thumbnail": "https://lh3.googleusercontent.com/0Q4DnMlIcCEFR--aitvZ1qWb7huushxRxRLaKkGGPrBy36ahboWrPfvuUckF0b1mmDSPw6icF2HQIWvf=w120-h120-p-l90-rj"}, {"id": "UCkV67him_HQU_ZQQxFoWN4A", "title": "Linea Personal", "thumbnail": "https://yt3.googleusercontent.com/Lddz2aPgFJZ3YuE5Zu43nqcjOv_xXOPQ8yC0WzOFaiEaUTRu4E8Bb3lO8b3ZaacaVOo5hTCEoCvdYt6f=w120-h120-l90-rj"}, {"id": "UCATfo9SAdXImyc6ygfbzJBg", "title": "Tito Double P", "thumbnail": "https://yt3.googleusercontent.com/35nINBXOwBLHwLZiW9BQcfvo8a-_3OyJ1nSf_OL0ZNY_h_fpO_6AJwK-flmfQr_9lpbXa4BtBQ=w120-h120-l90-rj"}, {"id": "UC5wwVhsK9j26eewBHAucumw", "title": "Chalino Sánchez", "thumbnail": "https://lh3.googleusercontent.com/p9a1KmLLsdDg-t6psPTjQY54jNI3eK5aYzxtm02_V0r1Z3H61w2XThjU6UdfhuJX9ysXmqGcZkGyTYSB=w120-h120-p-l90-rj"}, {"id": "UCpoLluxlphJM_qQWxUmMs5A", "title": "LENCHO", "thumbnail": "https://yt3.googleusercontent.com/i8eO9gabxB6FTZIkGQGew783Ek8f0KzebsS2Q6rhwVTvFxjQ69SHlWuZOalVWJLjyNjPyQYnZRcmrj4=w120-h120-p-l90-rj"}]}, "Para dormir": {"songs": [{"id": "ghXdpIcooFg", "title": "Sonido del Mar", "artists": [{"name": "Sonidos Del Mar", "id": "UCz-iPYW-3dR85zNvu0d81Xw"}, {"name": "Relaxing Music Therapy", "id": "UCaL1WTv7KEoj0VynDzGIlGw"}, {"name": "y", "id": null}, {"name": "Ocean Sounds", "id": "UCtqHdHOiJAN_pa7uBbod_uQ"}], "thumbnail": "https://yt3.googleusercontent.com/1V8ToUJuheicmwVjEoGknMJtHAXg1hSqoA7NzXyQ4AmYq_ezwwz4r2RdDG8_viZqCwvcDl9oYXXEwt0M=w60-h60-l90-rj", "explicit": false}, {"id": "oLuc2hQHoaM", "title": "Arrullos Del Hogar Binaural Calmantes", "artists": [{"name": "Realidad Binaural", "id": "UCifPetSzw_VNEU4eWEhQoHQ"}, {"name": "Ritmos Binaurales Sueño Profundo", "id": "UCsnH-8NrVI_mDhSvqk-E_Yw"}, {"name": "y", "id": null}, {"name": "Conservatorio del sueño del bebé", "id": "UCKJFyX2hy-qksr_jmAJzs_g"}], "thumbnail": "https://yt3.googleusercontent.com/i1tRTGr91cZdMiYpnmhhJYxVbltFTBUTG0OjdNbybetUZq6cE7SeBqOw2xFqeuJOdZlfHvVnqiFog6x3=w60-h60-l90-rj", "explicit": false}, {"id": "BimgsEGAQ74", "title": "Baby Lullaby Music", "artists": [{"name": "Baby Lullaby Academy", "id": "UC1DDYOVxJe4XXLRZSfItMfQ"}], "thumbnail": "https://yt3.googleusercontent.com/A505Bj6wWTHTvXnnYwYpGQwDrUIkPgVOg0svvAk1mRJV47Mj4gOuLl937jAqjmSwOv2ydp6Z3OwizXnh=w60-h60-l90-rj", "explicit": false}, {"id": "E-CtntTB_YI", "title": "Dormir", "artists": [{"name": "Dormir Profundamente", "id": "UCJUgdqLOh4YVN6GM6qZk6SA"}], "thumbnail": "https://yt3.googleusercontent.com/-ZwD-Q53ICV6sWI40QDj2jtNVAp2T2HtsSY4K9u1Tigc8isWLlz6Xz6XIkQFrXTzFFa9OhMpVZ8rUKDV=w60-h60-l90-rj", "explicit": false}, {"id": "h1EgbWREPm4", "title": "Baby", "artists": [{"name": "Sweet Little Band", "id": "UCoGRFkzgwmjmzgZKy3qCkvA"}], "thumbnail": "https://yt3.googleusercontent.com/gWyi3E2Gk-ZX0YEE844GJ3Z9E0K4uragsy_xGHwY1xu6jC2xV8ASK5AqRBoviAg-3SUooDW5vGTypu9W=w60-h60-l90-rj", "explicit": false}, {"id": "es6E1PqRIvg", "title": "Dreaming of You", "artists": [{"name": "Rockabye Baby!", "id": "UC6pJIMdXmwuDW8TY7SpJEyQ"}], "thumbnail": "https://yt3.googleusercontent.com/QT5fyV5I4qG9QsCLw2iZ0glOLswZrfs6ePao7i1SkndwmyofAObezhdwyJTEKAn13pO2qhkOHveKUfxE=w60-h60-l90-rj", "explicit": false}, {"id": "SknuILrqOXc", "title": "Old MacDonald Had a Farm", "artists": [{"name": "The Soft Music Box", "id": "UCZ_dHBETTclh0-9vXStt6AQ"}], "thumbnail": "https://yt3.googleusercontent.com/frPTusm2VtEiDh9O6G0Hnm8C8Ayli3iOidDDyx41zLV1_w5t9fu6bRSXVHO_-CtDx4WIJsYFvie9WtLPkg=w60-h60-l90-rj", "explicit": false}, {"id": "iYrjcfj7XBI", "title": "I See the Light (Tangled)", "artists": [{"name": "Lullaby Baby Trio", "id": "UCox7-Sa94ETpWd0jyjNCTGQ"}], "thumbnail": "https://yt3.googleusercontent.com/Q-gw5seaCtnj_Ac2spzsl8W-XiCAMv0JXEhoDIbEZrfOMLaPn3gTAzUeahC1jDk1GJE33j0gPvVkYAqA=w60-h60-l90-rj", "explicit": false}], "albums": [], "playlists": [], "artists": []}, "Fitness": {"songs": [], "albums": [], "playlists": [], "artists": []}, "Blues": {"songs": [{"id": "J1usGnRy7jQ", "title": "Slipped, Tripped and Fell In Love (Live)", "artists": [{"name": "Janiva Magness", "id": "UCnQxM-RxonlQ0wfRTt6iXAg"}], "thumbnail": "https://yt3.googleusercontent.com/rdZxJDBj6rLU6D54xeBDMw-A_7hP7bQqskEwnEa3WhDS-RFba_hlt3AsbQMPRQfkCPNJLgAvDn2HARE=w60-h60-l90-rj", "explicit": false}, {"id": "wyGkbhcRcpE", "title": "Until Now (con Ron Wood)", "artists": [{"name": "Steve Cropper", "id": "UCeZBHoZyihc10lyVQj4-lrA"}, {"name": "y", "id": null}, {"name": "The Midnight Hour", "id": "UC9ilotLu8C2ZJxA6miCpuGQ"}], "thumbnail": "https://yt3.googleusercontent.com/ndsZ7549FgdrKr9SLfy99oOcBKUVsK_V0W72Rw1FQTnFGm2pVGD3UE5e0BGljzz6g1dG2QB9JwWoPjxe=w60-h60-l90-rj", "explicit": false}, {"id": "PhTvSa4Nmps", "title": "Cut N' Run", "artists": [{"name": "Texas Headhunters", "id": "UCTxABf_KUIQVpJHX3-R9rOw"}, {"name": "Jesse Dayton", "id": "UCXxpU2xKpa2EjI7rvsJs-zA"}, {"name": "Ian Moore", "id": "UChPcQcK3NfhEXMKjiQB-weQ"}, {"name": "y", "id": null}, {"name": "Johnny Moeller", "id": "UCvA5aLwfBY9gKAxvruoAewA"}], "thumbnail": "https://yt3.googleusercontent.com/1Du3rEmNKDItTCtDA0qCpvbx4Ey4hwxAJEESCbWWT-n-Ki9xZ2DEf18Omq_Fk40xdh--b2wyyuDKGq4=w60-h60-l90-rj", "explicit": false}, {"id": "hrGC_NqMfsM", "title": "Back On The Run", "artists": [{"name": "McKinley James", "id": "UC1dHGV4rvwBZgFrMnW22SDw"}], "thumbnail": "https://yt3.googleusercontent.com/36-a779Ol3k6YR-FfbWT-rdsDymkjS93RiAcQzoevppMBNT-Pk7BUDahg6xtRx9xovrF9Dp6EuoIZj5B=w60-h60-l90-rj", "explicit": false}, {"id": "P7B2BZWRazI", "title": "Mean Ole Man Of Mine", "artists": [{"name": "Beth Hart", "id": "UCPYrKpvcaiYk89_Z8ithM-g"}], "thumbnail": "https://yt3.googleusercontent.com/FGdHL2lxUiCDSv_kh8_7tRWkLw8VBCORZjkNk5RHIADv1sm6Vyd1rZ3EWDj_SnKoj4pwVOKYlSWdfK4=w60-h60-l90-rj", "explicit": true}, {"id": "tMyXwXQr2dQ", "title": "Cruisin'", "artists": [{"name": "Sam Morrow", "id": "UCUwXeN2K_njknU7HQ4ewk6A"}], "thumbnail": "https://yt3.googleusercontent.com/0vNFdSqWzHnZHT5MknDS0s4sIIJIlRuhnJg4qCBoZFvIZjrd7fKk5AFexc4zvp4JUPM9mljyQkdDaLsZAQ=w60-h60-l90-rj", "explicit": false}, {"id": "7VeQllmcsrI", "title": "Black Centurion", "artists": [{"name": "Matteo Mancuso", "id": "UCwZn4oTM7DL0hYp7m8_x4vQ"}], "thumbnail": "https://yt3.googleusercontent.com/V5i7FOGlOWIDEfKjwxD3n1iHhf_Uh2-5Vve-oPAWN-J0DOhoHf81p12AeYIItFLYSw9h_Ter3AZD6CHq=w60-h60-l90-rj", "explicit": false}, {"id": "HPtsJ4YJZgk", "title": "Future Soul", "artists": [{"name": "Tedeschi Trucks Band", "id": "UCpShvKHfjSUx1cLpH2wlHGA"}], "thumbnail": "https://yt3.googleusercontent.com/IF_wEFWcJEae7ivDQ8DtDi9hK4NLycnpAcDCKZ9PwzvnocB_2JoqAlolh3iEm1zxzdu_UdbJMCkPhQ3j=w60-h60-l90-rj", "explicit": false}], "albums": [{"browseId": "MPREb_G6KAJ3OySts", "playlistId": "OLAK5uy_mFB9JGYCK2iXgxi9cohUOgUKg5utuqEcI", "title": "Blues Breakers", "artists": [{"name": "John Mayall & The Bluesbreakers", "id": "UC8M9tfY1Dd_WdtO0oAZT0Cg"}], "thumbnail": "https://yt3.googleusercontent.com/BA9vCrqWESMzn7dAYXiXfOH9t7chINxr4lKBE6od81YoETCy3auhSAuqj27Tr6zOHXDSHOoAbcVASnMC=w60-h60-l90-rj", "year": 1966, "explicit": false}, {"browseId": "MPREb_f6OfkQqoOgd", "playlistId": "OLAK5uy_mO3CkHe48efLPVnZ1UHIW4r5qKvzze6wc", "title": "Buddy Guy & Junior Wells Play The Blues", "artists": [{"name": "Buddy Guy & Junior Wells", "id": "UCqQnmSnXS_qlyP6qok7lCAA"}], "thumbnail": "https://yt3.googleusercontent.com/HIlZawp-5_LxKUg8v6JU-8RNlfVVDi_iRt2rwEj6ucs6tggRreZ6qNZzwT0mU1LrWsdU7PgbqnB_5WEx=w60-h60-l90-rj", "year": 2004, "explicit": false}, {"browseId": "MPREb_gFzz85CI4I5", "playlistId": "OLAK5uy_m7DWwzrrE9KKnTI7eYGaJ78z8rstewrt4", "title": "Live At The Regal", "artists": [{"name": "B.B. King", "id": "UCqKrqzF3eX7R17m_MIMte4w"}], "thumbnail": "https://yt3.googleusercontent.com/nV2oozf4Ns6907UioRrcliWEEAbR1fCDXDaaWSkv_LcdBk4Q5Qgq5oduMO0tjLhszNZYLCziGIx4ZQvR=w60-h60-l90-rj", "year": 1997, "explicit": false}, {"browseId": "MPREb_POZ5zoO7M4K", "playlistId": "OLAK5uy_kQG39_Fu4ka5kZPWPwqwFc-kjNXbuC5Co", "title": "T-Bone Blues", "artists": [{"name": "T-Bone Walker", "id": "UCGUpBCmvcFSZUekLZjQnJFQ"}], "thumbnail": "https://yt3.googleusercontent.com/lY_tffSSavJuVHh1AKF_dVuwWplkRhykLu5Ag6qpFLHYesUsLl24n5igI7RouXFUkIvirQrD3e7RCMtb=w60-h60-s-l90-rj", "year": 2019, "explicit": false}, {"browseId": "MPREb_bEnDgdMtdDr", "playlistId": "OLAK5uy_nd4e6HICl4-HzvCFPYdMHNdC6yULqCSWA", "title": "Greatest Hits", "artists": [{"name": "Big Joe Turner", "id": "UCRneEQ55dXpMn1hoEcigCEA"}], "thumbnail": "https://yt3.googleusercontent.com/Us_8p1Dplt3GTMrggJLQeqlKmCm0cfG-otVQx5qXHm_x3RYDz1-HEq25hPP2eNFO-nGIjT6LfZjzra8c=w60-h60-l90-rj", "year": 2024, "explicit": false}, {"browseId": "MPREb_5AN5iAdnXkI", "playlistId": "OLAK5uy_liDbPkGZVqnmMRSnzSMoj7cLmxTLhtc9c", "title": "Where Did You Sleep Last Night: Lead Belly Legacy, Vol. 1", "artists": [{"name": "Lead Belly", "id": "UC7jiqF1wzNVeZhZmJ4EKprA"}], "thumbnail": "https://yt3.googleusercontent.com/0l1phhkNavrzmwFgvgBWH2Jg2e9gWUDnjYQ0o2vhsIZcwyysnCmHQofALBwMXLIi_LtdBoywOmrhEjLJ=w60-h60-l90-rj", "year": 1996, "explicit": false}], "playlists": [{"id": "RDCLAK5uy_kb7EBi6y3GrtJri4_ZH56Ms786DFEimbM", "title": "Lo-fi Loft", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/9I45SYZAfTeR17ByV9i2yDpWhQ5DTWMCrW3Lei8vMIiZzeyUnf2zRFUDBob4TGwgUkXAAR_x1Xa8VA=w544-h544-l90-rj"}], "artists": [{"id": "UCoKpyzX-Z8pYaHmLMlWrh_g", "title": "Cedric Burnside", "thumbnail": "https://yt3.googleusercontent.com/G8psXYjfZyyCkkTmAVprktBJ0WuJSByfXZ0KUPXurBAo0dCi4eB1aub7SPQdZcNBo_4y-qEnddRwJzon=w120-h120-l90-rj"}, {"id": "UCe8HPyPeo-crUSHzdt0Ys2w", "title": "Christone \"Kingfish\" Ingram", "thumbnail": "https://yt3.googleusercontent.com/QFUg6Bq-9C7Z78Om742lG-5Hlq0eSd0Y8H-8Nl-63fiU7qEtPk8xx4fmTePFqFhoB_V019MtWQ=w120-h120-l90-rj-dcHVOK7CEJ"}, {"id": "UCmcVqDWzfQK-FIWuhPb9rFQ", "title": "North Mississippi Allstars", "thumbnail": "https://lh3.googleusercontent.com/8D_1NQD1Xt3s0gRObS6zdRKplksRTeJy47Nok-NCRArUDkD8fx4Vl5QbyaqqBGxQC83hYNOrEm_95Xg=w120-h120-p-l90-rj"}, {"id": "UCchwuI134MUX4GSZqYLpYGA", "title": "The Black Keys", "thumbnail": "https://lh3.googleusercontent.com/_mMeSZ718WAakyt_oe-_8igw_8DWOETE9lAKs5aEC0ahXiwmdQEs3IHvo27tu1QafWFDbCcRob1COFY=w120-h120-p-l90-rj"}, {"id": "UC4phZVcGV9fjBcr6ZthbbYg", "title": "Eric Gales", "thumbnail": "https://lh3.googleusercontent.com/_9h-C4s8qoJy4OqYXyJCoZaDkngdLdrhd8nVyWUKS2DK5h3sKPNMceOLeJoDViS4rpowfGW9K7lRcVw=w120-h120-p-l90-rj"}, {"id": "UCoMXHT2M9St16CxhIkrncxQ", "title": "Albert Cummings", "thumbnail": "https://yt3.googleusercontent.com/563BRPog1s9PqeNUeBarBCWPredzY_bl3ozQFmi3JTT6FRJnJKKtQKdX_3XBJy9jw0u9PGbtgA=w120-h120-l90-rj"}]}, "Infantil": {"songs": [{"id": "7DA0s0Rd8OM", "title": "Vives en Mí (Canción de Halloween y Día de los Muertos)", "artists": [{"name": "Lingokids en Español", "id": "UCgp5_rGyPzZkyZdLB3xCY8w"}], "thumbnail": "https://yt3.googleusercontent.com/Dea9cjH92DPbWK28J-2ZHqbHY_zSByFY7qsMxlbk2uCbSqviSmU1Rd-gcBudvy2_lntIk0vZYnCa-po=w60-h60-l90-rj", "explicit": false}, {"id": "bmwq9jnosBQ", "title": "We Ride Together (con Madison Reyes)", "artists": [{"name": "Barbie", "id": "UCfDqtZ2MUy39kdR5GUMgSjg"}, {"name": "y", "id": null}, {"name": "Mattel", "id": "UC0Jnd1gIHsghvfnlxtEj9EA"}], "thumbnail": "https://yt3.googleusercontent.com/Xcqw5hf21u-bYTfJU38fKzZo82va46vWVdWvXfP4H9dwinS6oW1Jjen_tSPlE7nkOcqb-Cn2suickLZW=w60-h60-l90-rj", "explicit": false}, {"id": "XSvJZ5fgqvE", "title": "Los Dinosaurios Sabios y el Super Avión", "artists": [{"name": "Juani y Nacho", "id": "UCvS-ribwJhQawICmvEiGOaw"}], "thumbnail": "https://yt3.googleusercontent.com/5sfJYct-0H2iHbn5Hpm8XUIvBA5B4xv6J65_xBBvGQl3oePh8QG-JeeB7f1ZIFFz9PHfdmu9pAQIj6Ty=w60-h60-l90-rj", "explicit": false}, {"id": "9RJD0aGWQ4Q", "title": "Piece By Piece", "artists": [{"name": "Pharrell Williams", "id": "UCJw8VyO6e3v6S0327AsgwcQ"}, {"name": "y", "id": null}, {"name": "Princess Anne High School Fabulous Marching Cavaliers", "id": "UC8vANYKhbnKS0oZY4gI5lOA"}], "thumbnail": "https://yt3.googleusercontent.com/WUMIcbtd7DtqPk3J1_ZXn8UjjRrSOk6Yw32KK1B_jaYhH3FrW02EREiukh7_hKHWQ8qBXHklY_CEhURs=w60-h60-l90-rj", "explicit": false}, {"id": "RPinUx6B_Bs", "title": "Superstition", "artists": [{"name": "KIDZ BOP Kids", "id": "UClOpZVfE5v-fFtM8vqFud-g"}], "thumbnail": "https://yt3.googleusercontent.com/cv9lB8-rLngjQXWxsAQml1MyHbMBhhMHGc5yXXzHe_pc2blhEIftr0dsco80ZUNEXJj4r3rYR7ocZf5m=w60-h60-l90-rj", "explicit": false}, {"id": "3D9q2_eDZK4", "title": "Robot Monkey", "artists": [{"name": "Spotty Kites y Zac Parkes", "id": null}], "thumbnail": "https://yt3.googleusercontent.com/O6RgKr1vC-pcutMrB-hAdR8fOp0vnNuhA3P3p4AvNzjYB6R1hlrIs0rpHhuyNHd0wyVAlEZ_O-aXB79t=w60-h60-l90-rj", "explicit": false}, {"id": "0EK0FnJE37U", "title": "Boo Ya!", "artists": [{"name": "MusicClubKids!", "id": "UCorYleaO0EwEdBK2GEx3CvA"}], "thumbnail": "https://yt3.googleusercontent.com/X1CY3Qo-AgMO7Qa_0fX5dUBo79uQKvjF3nHUvJY3fP79Pd08J2bUPi11W1Dnue3MIwpxnGOPKrTgdjZu=w60-h60-l90-rj", "explicit": false}, {"id": "vQ4_pBLCZjw", "title": "Barney's World Theme Song", "artists": [{"name": "Barney", "id": "UC4ZdoxIFkKiK1ZZi_VuiTxw"}, {"name": "y", "id": null}, {"name": "Mattel", "id": "UC0Jnd1gIHsghvfnlxtEj9EA"}], "thumbnail": "https://yt3.googleusercontent.com/Jx7atnywIifcZL45d1k6Mkuqi8_-J_gBenjz_oN24DgcyZ_fvoD34UpI-PdCjshe6iVTiPQyA2JmSedA=w60-h60-l90-rj", "explicit": false}], "albums": [{"browseId": "MPREb_XC4ayZ4iBzs", "playlistId": "OLAK5uy_lE54YrW6HfTM3VsxQTR3AtF7xejycfgo0", "title": "Las 100 Clásicas de Cri Cri, Vol. 2", "artists": [{"name": "Cri-Cri", "id": "UCbUcbkB2ZVTgi9FoLk5o3Yg"}], "thumbnail": "https://yt3.googleusercontent.com/yMnHvPUnZ9Dzxm6bDqvXE9ycGXlO0T7wIkEVvG_5m6WFyfgdR1Lux6koOm6MpEMo3tCv2VNt6XaxY0GW=w60-h60-l90-rj", "year": 2001, "explicit": false}, {"browseId": "MPREb_px409I5mpm9", "playlistId": "OLAK5uy_kGZcCGxYvoPQNOkk5DAFowaVQ1hjLxBeM", "title": "La Granja de Zenón Vol. 3", "artists": [{"name": "El Reino Infantil", "id": "UC9fV5kqK4dP6F1937G66qQA"}, {"name": "y", "id": null}, {"name": "La Granja de Zenón", "id": "UCQ8cXCAuUgXFt4TWcS8ouXA"}], "thumbnail": "https://yt3.googleusercontent.com/sSJlqe-le0apsUrBlrZeB7MbynZJ5Gm-P56pqhPAaEOtF3svhjCCaN2y0mzuf4L7J1nBcTfW96emYLC0=w60-h60-l90-rj", "year": 2016, "explicit": false}, {"browseId": "MPREb_A1rQB3VwhV3", "playlistId": "OLAK5uy_k2PoriHsIRD-NYmlpKrPKCc06McfWFEYs", "title": "Pinkfong Animal Songs", "artists": [{"name": "Pinkfong", "id": "UCx2SCgc7C59qnP3S9Q1gN1g"}], "thumbnail": "https://yt3.googleusercontent.com/U487wEn-Gm0Tn5O38P34b0Afs6Ie8VNXuJGZxc-7jPLyk35I8RYv52t4tTurYokzL2ikQKSMKmCwBDbc=w60-h60-l90-rj", "year": 2017, "explicit": false}], "playlists": [{"id": "RDCLAK5uy_mj_3pEu7cI8H45ilcPKigVPkuw-kCWbzI", "title": "Navidad de Disney", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/m9pzlV2hkJFM0PIbk6JRZqQU_fjklFKHB8KW6iAr4K4XRQ-S6U6MAVG0rDPvVnd9HGT1QiERJ-dSdpw=w544-h544-l90-rj"}, {"id": "RDCLAK5uy_nwGVTzraBvAR-wPGHBPeqp4Qb139ns16Y", "title": "Música de Disney del siglo XXI", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/aKV-pzQMi2zCdVqxE2pV089ASPkocny30Koo0qEip9KH28aqpC5tHxkiSH3TNEu2SvPSwsSdHTnElE8=w544-h544-l90-rj"}], "artists": [{"id": "UCpUSNMK6xAlNlr5gA7GA6_g", "title": "Luli Pampín", "thumbnail": "https://lh3.googleusercontent.com/rwR85GL6NCw-ptTfg0XRjHMKHNSvu_2o9bFg63Iydl9C4nTSjdrDzygXWb23oSallgQzHefoxj3wAWc=w120-h120-p-l90-rj"}, {"id": "UC9fV5kqK4dP6F1937G66qQA", "title": "El Reino Infantil", "thumbnail": "https://yt3.googleusercontent.com/eBxPMBMPQNdy3a2BYH8eG20BCgYg20kNRvA-WxYE_Ug4KufrnBpdhC-0TCt00yzjcomxjL2DD34HWt7HWA=w120-h120-l90-rj"}, {"id": "UCLTS-vf_QXSybOG5LDGw2VA", "title": "Mariana Mallol", "thumbnail": "https://yt3.googleusercontent.com/JKATpPPuIKzmo5tqAIs14sWWtn8vA82wL2Indg44zf2JOfCJ9FmqVYUGbxktYXge3HZ0oP2f4g=w120-h120-l90-rj"}, {"id": "UCGPj0zaV3XBtMuqJEkT71RQ", "title": "Patylu", "thumbnail": "https://yt3.googleusercontent.com/bOxo-JumgzqzpnAM63l5nLjEF12tZ3a9J7vvNRnp1QaI7C4n1pr9XZtY5zksH2cVt-bMjPXx8GjTliCMmQ=w120-h120-l90-rj"}, {"id": "UCHrYl2EFD3oNkSjAAe0F0TQ", "title": "Sesame Street", "thumbnail": "https://yt3.googleusercontent.com/8rFrn_J0XS7BngwmVAtaN15xLXWn5U7fnYtGtKRGK14wa-HZ317a76rZCx88IQ6L29fM3xZmmfIcOq8=w120-h120-p-l90-rj"}, {"id": "UCSgDPZ6SEZjQDiKPRAYlzHw", "title": "Pica-Pica", "thumbnail": "https://lh3.googleusercontent.com/EuKMr-n_QOsgP9dNi_dPyfu2unoxPCmvBBc31Es2PpiEBi24C_U9k3UdzSgXHj8AOP-Ku8gijEr5yoU=w120-h120-p-l90-rj"}]}, "Country": {"songs": [{"id": "ZDg1GllVOEg", "title": "Someday Dream", "artists": [{"name": "Emily Ann Roberts", "id": "UC_Hh7VtBWSkLcaDun3H49gw"}], "thumbnail": "https://yt3.googleusercontent.com/-DGrSeKdEahag1Xi4JeLcNFUWTCgGQ152m6Nfsr3YW1ROOX49M0HH1Ioq_hNZ6qEvYKFyIaAlYtuh-ZKrw=w60-h60-l90-rj", "explicit": false}, {"id": "BQjjkMXJK2Q", "title": "Giving You Up (Acoustic)", "artists": [{"name": "Kameron Marlowe", "id": "UCrCQpeOITZkm7qHzQhPjtBw"}], "thumbnail": "https://yt3.googleusercontent.com/KxcSp8fMdB1PCGgClKxvDQCpSJE7WzchLjDImZjWTTGJ-ZWu3RTE5CLiGiREgZNIh9gSVmYWr4sxkCwV=w60-h60-l90-rj", "explicit": false}, {"id": "9il00hg1_70", "title": "Everybody Needs A Bar", "artists": [{"name": "Tyler Hubbard", "id": "UCPqd7M3F8uG9-G_LgCv-YCA"}], "thumbnail": "https://yt3.googleusercontent.com/jO85zuon-dT88HtqFUQpXjrUgYH5r8PZ6-w-5hRbALAZOvFxkDOyn5uMza4Aay4Xz9kdZHGFsNicSNbRpg=w60-h60-l90-rj", "explicit": false}, {"id": "5nF1Cd7xyhQ", "title": "My Cross To Bear (From The Documentary “Gregg Allman: The Music Of My Soul”)", "artists": [{"name": "Jackson Dean", "id": "UC5OSm0HX6pulTJpbDFFLcWg"}], "thumbnail": "https://yt3.googleusercontent.com/q39I6PQA6X3gzMpPBCgonkRrXKLwjbHGpmx6j-1NUUN3MKaxdfXYEvHaCXQbYQZmMRvLP5SB2xCK4uuV=w60-h60-l90-rj", "explicit": false}, {"id": "ADSP_w5tEPQ", "title": "Shoot The Bull", "artists": [{"name": "Cody Johnson y Luke Combs", "id": null}], "thumbnail": "https://yt3.googleusercontent.com/g8qhsB7epWIukwud2tsFuubdpxs416ZcZx63WfMc-ANVtAsMmHTFfK12hAHpQuilKIljhoiNUgnlTb1o=w60-h60-l90-rj", "explicit": false}, {"id": "wGsdaqeNnlY", "title": "Bad Decisions", "artists": [{"name": "Koe Wetzel", "id": "UCVeh2JXBdVAu7UQFHwZnftw"}], "thumbnail": "https://yt3.googleusercontent.com/gPD1dEjdHBRconqP_YFPZB9cIOh3YlarjcCrtlS3cQuVjy-Q-bdOehEXCH9LTfOPI-IvcibAx-bp3RJw=w60-h60-l90-rj", "explicit": false}, {"id": "v7InNhgUtQs", "title": "Still The One", "artists": [{"name": "Alan Jackson", "id": "UC9KL_2sn3_nuCB0l0w8jV5A"}], "thumbnail": "https://yt3.googleusercontent.com/SWsP4UtVHc6EjFFul-iKPQUz9xYnE25ES69GNYre65R-UCiAkX3HhqTyjlDR5Ek65nXnS4dS9CnHGW8dow=w60-h60-l90-rj", "explicit": false}, {"id": "m2M5dCYEPmw", "title": "Another Drink", "artists": [{"name": "Marshmello", "id": "UCrxpwXq8wCTskOQq5d_KoqQ"}, {"name": "y", "id": null}, {"name": "Kelsea Ballerini", "id": "UCeX-6wYcWQKBpRHtBQOjqjg"}], "thumbnail": "https://yt3.googleusercontent.com/qU8v5rqW9jGrQu_7VOeQZudsNAteBNBMLdNEUXCfFcimPVe2cxYI8w-3_q6hAis-k-Mp8ajh3Kuy0JP5=w60-h60-l90-rj", "explicit": false}], "albums": [{"browseId": "MPREb_gV6DnNobGrq", "playlistId": "OLAK5uy_nDmBdsh79v08KjMsldtwggIWxSqtdulYU", "title": "No Shoes, No Shirt, No Problems", "artists": [{"name": "Kenny Chesney", "id": "UCx-U-6-fMEm6yuxm5tUA7zg"}], "thumbnail": "https://yt3.googleusercontent.com/RM3hZB6bOSj-OS5Gg8Q4mnaB2bn8GTGS6O8pBT9VXuNwUVB_wgTN911cKw51amdPrXJKfbW9t7lbTvQ=w60-h60-l90-rj", "year": 2002, "explicit": false}, {"browseId": "MPREb_1MZ7tKA486L", "playlistId": "OLAK5uy_nuQlaHaCJqGUA_90WvqaHTlmVYqbV6tqA", "title": "Traveller", "artists": [{"name": "Chris Stapleton", "id": "UCEBTD8bc2J7wG7Fqhss7Jow"}], "thumbnail": "https://yt3.googleusercontent.com/nEkBfOAc02tKzDZ1m-jLwgT0udQxJld0qI6dFy1BW2ff5B0dHBYALAP6rtjKledbdGpZybh6fZK0QoeDKw=w60-h60-l90-rj", "year": 2015, "explicit": false}, {"browseId": "MPREb_jjR1POVMzoc", "playlistId": "OLAK5uy_kN_eOO1v_gvMqxxN3hEVl3cG9Ky7TkBkc", "title": "Red Headed Stranger", "artists": [{"name": "Willie Nelson", "id": "UCPmpbAuktEPbIVpwGrY_c4g"}], "thumbnail": "https://yt3.googleusercontent.com/dbeWLiW5JY4Pm34pdtbmJ-l6pKp2-wg4ogV9d5uo4F9TPoXTkWB-_Iy3HR0y--Y9asR9eM0Ryrs0aA3W=w60-h60-l90-rj", "year": 1975, "explicit": false}, {"browseId": "MPREb_uvhVNjF0BiD", "playlistId": "OLAK5uy_kxGKyGp_JhEKjHsl2Kk2sr7He3HjCux74", "title": "The Woman In Me", "artists": [{"name": "Shania Twain", "id": "UC3H5SQg6oJnxeolvQk6ABWA"}], "thumbnail": "https://yt3.googleusercontent.com/vlpzZET1qKtAL8CxTUIxSSe9engE5TqGTSiCUJYvfIgl8Mvjn9xQfRD2GYvBdlYQkWCW7z4KezZTY6CO=w60-h60-l90-rj", "year": 1995, "explicit": false}, {"browseId": "MPREb_3xoWyi9FSFV", "playlistId": "OLAK5uy_m_zfX1ZEVbsCkBeC1xKiZoJVIVqszeLpc", "title": "Highwayman", "artists": [{"name": "The Highwaymen", "id": "UCuDH2oxmERsYDlEzoj6NJdQ"}], "thumbnail": "https://yt3.googleusercontent.com/sQ946XaWS5ZO73Q0zoS8ylCNDLgNF1iLFqmY55E7h69p_EEYP2VcORsSPkgnKX04hr1jWJa3jIy745I=w60-h60-l90-rj", "year": 1985, "explicit": false}, {"browseId": "MPREb_NOAHDj1c9KT", "playlistId": "OLAK5uy_nB2lC8gTuywn9R3PVHrj5YyNIIriB_Giw", "title": "Modern Day Drifter", "artists": [{"name": "Dierks Bentley", "id": "UCKrswkLLh6mWeSLTanR8VAQ"}], "thumbnail": "https://yt3.googleusercontent.com/BBbnNnkLYPH7KOtrfQIauoc7M_Bi2Go_2GOztwoPA82CT5tsqzZ0pvQuHLsg_QCNZS56Hnq2pVZWTevN=w60-h60-l90-rj", "year": 2005, "explicit": false}], "playlists": [{"id": "RDCLAK5uy_kLwgLlrxA4-_EchctXgTyHR4rwRaRv1wk", "title": "Música House 2026", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/AHM4S_P1OOjGGUFWDxHQTUCxpxAFKOV5IzW-mttYW7OQqfgE7qQcU1kq8XFoaXQN1LAibsDu5O--sXI=w544-h544-l90-rj"}, {"id": "UCyxSBq4dOhqhZs-N5DDkfcQ", "title": "Vive mientras brilla el SOL", "author": {"name": "Alientto Musical", "id": "UCyxSBq4dOhqhZs-N5DDkfcQ"}, "thumbnail": "https://yt3.googleusercontent.com/XqXhT5ZHU1amwQXlhL8e6KcwsTfpuBSGgqyaVGHAuA7RHnso4A2E2ayal_jeY5-RnpgyDm99SaIjoevE=w120-h120-l90-rj"}], "artists": [{"id": "UC6315A0OBfvi9JAsqJED5CA", "title": "Parker McCollum", "thumbnail": "https://lh3.googleusercontent.com/B8xxzb3WMcMgW8DovjR1sGBesFccl1J3jEDxUk_88PfUbA1OvXmDX-99FVEGfoW0nlR29tUAXkPudxk=w120-h120-p-l90-rj"}, {"id": "UCV_rVAqZRqyFoPD2uWWfErg", "title": "Megan Moroney", "thumbnail": "https://lh3.googleusercontent.com/JF6UdYKy2Jjo15G8uYCUI8a8cMGJpp4sVaPCQAxVpCtlO1ciQrPvWfPxF9Jag9vf8WmYLYb4iBT9TdM=w120-h120-p-l90-rj"}, {"id": "UCrhAzThZC_WoeXzBMlMBiTg", "title": "Zach Top", "thumbnail": "https://yt3.googleusercontent.com/d_AU1n72TlO8Giy-9GnnTFIdJ1R1HByhgBfZYocLIE02rpt4CEXwKb2YW3IwenWnwOoH3-4N16bpCKjz=w120-h120-p-l90-rj"}, {"id": "UCecnyZYofHiBVDJpx1XNYOQ", "title": "Ella Langley", "thumbnail": "https://lh3.googleusercontent.com/hiPrNPmBjTnGntY7hab2LalE5ezJtYCmM-dKq7_fsaItDDoScebnilCo2nPC98o2QL8oGdgPGiRewjHl=w120-h120-p-l90-rj"}, {"id": "UC0_1glf30IS53tFQWT8xpxw", "title": "Jelly Roll", "thumbnail": "https://yt3.googleusercontent.com/N9mP4D8MCqnIW7e298XC3xxz7UXvnSa8Ca06uScbItmkw6Syp1Fsm4piIJV-pg6BW-NbqMCsuq9hKhQ=w120-h120-p-l90-rj"}, {"id": "UCFZ0p3ETRsTLAacWhcA_tyA", "title": "Lainey Wilson", "thumbnail": "https://yt3.googleusercontent.com/32oC2vyKIoJks5_SyIhQK2LFG0lc3231c6cI7em2ViyhP18oHD-gxwPc8sVL5WpFKLUsYFaKvzLFt-7m=w120-h120-p-l90-rj"}]}, "Afrobeats": {"songs": [{"id": "nNzABA8GOIE", "title": "Uche Jumbo", "artists": [{"name": "taves", "id": "UCdC8pXW8BUdFViF0sbcfd8g"}], "thumbnail": "https://yt3.googleusercontent.com/sZS-Eb1bA7B4YXPfIu6_VVe_Va_jnSoeNhoI40UA_HAKPTDJOhz_e-xPvEqRicFRHdMt4IZWesPzJgV8=w60-h60-l90-rj", "explicit": false}, {"id": "U1TRzVFJrTI", "title": "I Know Who I Be", "artists": [{"name": "Davido", "id": "UCehhqT7086Y04HAQ121lu3g"}, {"name": "JAZZWRLD", "id": "UCg9m64wzmvi6LiZ5b4BUyCw"}, {"name": "y", "id": null}, {"name": "GL_Ceejay", "id": "UCp84kR4r_p8zg8xyJopgSYg"}], "thumbnail": "https://yt3.googleusercontent.com/YS-U0gp3IRJWfhmOeg1sewHnF7Zhvpc8tQqIzZExL_Fm46GMBFCv8VB3KpEMU-QNF1wg3_QRhXjIyrH_=w60-h60-l90-rj", "explicit": false}, {"id": "7K3CYQSOfXM", "title": "Give It Away", "artists": [{"name": "Africaine y Nonso Amadi", "id": null}], "thumbnail": "https://yt3.googleusercontent.com/tU4TGkxFPPlQguaERGlq4GLMFwD_51kGAkVPPXJTYrFU_UFwXWKq5eJCJ2Kor5m3ggXziNDQmxkMrDut=w60-h60-l90-rj", "explicit": true}, {"id": "7eqS1lqqc6c", "title": "YBTM", "artists": [{"name": "Joeboy", "id": "UCHxEAq_D6CJ1sn0tocJ6GdA"}], "thumbnail": "https://yt3.googleusercontent.com/yIY5reMBAMsZ1cNw8cM6WyoANc3KDZ4b1bI1ctdnGyXPHC_V1Jo-A3cJYstIoAQMJ3VSQO8u2aOWU_6-=w60-h60-l90-rj", "explicit": false}, {"id": "r5VlnJBhMq0", "title": "Oshe", "artists": [{"name": "Del B", "id": "UC2G_UVYc7WqD3Y4UYyviNnA"}, {"name": "Wizkid", "id": "UC25DGtgYk5TwdksvtpeWYvA"}, {"name": "y", "id": null}, {"name": "Reminisce", "id": "UCb8NvU8yzbRzf0yXGY5pTOw"}], "thumbnail": "https://yt3.googleusercontent.com/ilK90cylgKDxHvMFz8nPjE1miGQPySdaDwnNZnnLOJ1AC-k-0wX6BQZeSuk7VCFZZRSi_CJzay26xcw=w60-h60-l90-rj", "explicit": false}, {"id": "ulBPaoEeWKs", "title": "JIGGLE", "artists": [{"name": "Chella", "id": "UCw_iV6pDakrsN5tBd3bkKag"}], "thumbnail": "https://yt3.googleusercontent.com/07ymH9VX3603YLLzzFTUPQu2NYjFUkYv-UCqyYYN7kOvpAgJRl2mH9zlxzYqMmtcGSeH6pRrKGwlaDCL=w60-h60-l90-rj", "explicit": false}, {"id": "NMKVVM6rAJ8", "title": "Feel Am", "artists": [{"name": "Magixx", "id": "UC4VD3HwywldtzElu_kTfM_g"}], "thumbnail": "https://yt3.googleusercontent.com/HFyGPd6F-aDbh6sZxPzE0j1r_97umKDjJEUXjDCdV7GPf0jP8O-udl-Dz2chA4Xnp1fvxZWh5UIhwfG8=w60-h60-l90-rj", "explicit": false}, {"id": "py1dscZRqno", "title": "Miss Me", "artists": [{"name": "Majeeed", "id": "UCcqwc4DSWYO0YE3CBQyF8BQ"}], "thumbnail": "https://yt3.googleusercontent.com/gWsfgfj9KE302YBQNSHJGfYepw2eJ5fXhtzpjwj9sL6SV3YRaVeYrJcLiI7w_s5V9KFAXDDOVihkD-yC=w60-h60-l90-rj", "explicit": false}], "albums": [{"browseId": "MPREb_EqX47lUqMEk", "playlistId": "OLAK5uy_lsWz6A9JteuXZS6XPdfpj0nM05HrRG4oU", "title": "Mushin 2 Mo' Hits", "artists": [{"name": "Wande Coal", "id": "UCy51adLS32LxWLM5a_ixezw"}], "thumbnail": "https://yt3.googleusercontent.com/iWqAZzXwvrfecqjeyi6fq9ikLiz0gbJpYA1a2_BXf_t8-no8Psv6kYGLAw3gVPYhXl1Jo4jLIGKoDKQ=w60-h60-l90-rj", "year": 2009, "explicit": false}, {"browseId": "MPREb_xTCK9p8LFwq", "playlistId": "OLAK5uy_nRgDdHR-xQXtrltYVotGsRGr4cgt946Ww", "title": "Outlaw", "artists": [{"name": "Victony", "id": "UCh-VZa381UUDND3OtMKLudg"}], "thumbnail": "https://yt3.googleusercontent.com/WqqhRAvMYCniHsIpX7CBlQ3Eq98R67ckAxu_XM4-48dde_c2fPSm7XTomiCvwIlO5J9nw0fIdPpwX9M=w60-h60-l90-rj", "year": 2022, "explicit": false}, {"browseId": "MPREb_IrOgPmxWCFj", "playlistId": "OLAK5uy_mnaMj10k7XVmUCQXg0P9PoE-g4u01Fwm0", "title": "Presido La Pluto", "artists": [{"name": "Shallipopi", "id": "UCSHBvim2UX3klyRkjuOPllg"}], "thumbnail": "https://yt3.googleusercontent.com/DRsnEyWOgsYh_5ORelTGoKVIs10I3I7vuntrbgx2PAgQsrxVBaxQkdqw1hRgK2r_0UkLGhu4gFf2t9FP=w60-h60-l90-rj", "year": 2023, "explicit": false}, {"browseId": "MPREb_TSK62oHz24R", "playlistId": "OLAK5uy_llvpelGMiLjIqSBMTWBiK11opGHpL8ybU", "title": "19 & Dangerous (Deluxe)", "artists": [{"name": "Ayra Starr", "id": "UCO4TFKYiSDo7vOQC04mr7Fg"}], "thumbnail": "https://yt3.googleusercontent.com/3gdU4oYMVmMtRM57EIF1trLEjVQ0SVS3Zb9KZ69w97vRo5q7r4-FPcA4MhrNp83MxDPYzmeVaermw4yZ=w60-h60-l90-rj", "year": 2022, "explicit": false}, {"browseId": "MPREb_LJmJYkoXV9J", "playlistId": "OLAK5uy_loGHVmlYxnnXq4-kuvybSFulKBp4I28nY", "title": "A Good Time", "artists": [{"name": "Davido", "id": "UCehhqT7086Y04HAQ121lu3g"}], "thumbnail": "https://yt3.googleusercontent.com/Zdldsdg9_DSrk5c7ghTmiPYKeU4hGikzhg2Euf_Nhkyzlv-GoEa-7TUHvxVEttEnoLwkfEQ-TkgTYWtt=w60-h60-l90-rj", "year": 2019, "explicit": false}, {"browseId": "MPREb_ZMAQW1LJkKv", "playlistId": "OLAK5uy_nuA06Zru1zefHqWEQgrvc53vvlsLXljz8", "title": "Mirage", "artists": [{"name": "Mannywellz", "id": "UC36gT2uiz7a5EN16JawuHDQ"}], "thumbnail": "https://yt3.googleusercontent.com/kQtr9WSTMY7kpEzfWE-SW0KyDzv8-JuraLJSRzD8V65QZQEnXd4dYuLCgBAYsaHdrFJ8TVuDCp4JDoE=w60-h60-l90-rj", "year": 2020, "explicit": false}], "playlists": [{"id": "RDCLAK5uy_kb7EBi6y3GrtJri4_ZH56Ms786DFEimbM", "title": "Lo-fi Loft", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/9I45SYZAfTeR17ByV9i2yDpWhQ5DTWMCrW3Lei8vMIiZzeyUnf2zRFUDBob4TGwgUkXAAR_x1Xa8VA=w544-h544-l90-rj"}, {"id": "RDCLAK5uy_mfuAOVrpzolzVSNCU6ekcPE_JrslkppBg", "title": "Éxitos de Naija 2010", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/Ng5CCrFLkstuol3-HKx5wk0Gj5TKdW07UbeJyNS0nbh1cyN5mLn23ZPOvZ004jHaVEbNj2KP2RyYNnE=w544-h544-l90-rj"}], "artists": [{"id": "UCr61sufuLt7_eB7ak1bXHIg", "title": "Burna Boy", "thumbnail": "https://lh3.googleusercontent.com/_QE_u2AbzzmENeNq9ixrSpB_dZHCgX1sX9_4NAzGIWTR8Gd6jHpGcUZlmG4ZjA97F7pHrErkFOdNfw=w120-h120-p-l90-rj"}, {"id": "UCwOi7Er_OmQXcfgz_6KAPTQ", "title": "Bloody Civilian", "thumbnail": "https://lh3.googleusercontent.com/gMwJidwHHRsm-Xp1bjcGkTF3uXmbD5xP2kHdOnH5zmcBg-Bgd7FxJfAB_8BP27o7JFAzJ_Q5QI4LeS1P=w120-h120-p-l90-rj"}, {"id": "UCO4TFKYiSDo7vOQC04mr7Fg", "title": "Ayra Starr", "thumbnail": "https://yt3.googleusercontent.com/ZagERmj0xSzYCSRQqL0OFwD1cicUb4SZ2c3QhFuXuf5ZAftPPCmSIilPALJXTqgIMAMhlDpun1NF-Cw=w120-h120-p-l90-rj"}, {"id": "UCy51adLS32LxWLM5a_ixezw", "title": "Wande Coal", "thumbnail": "https://yt3.googleusercontent.com/PYrIZwAyh-HXMlVPTdux5qwzmk_6nOGXl8cCsbXERnA0JJLHmOd_diyobbCKEsS4G4fFP2F8XdkNLZvJWQ=w120-h120-l90-rj"}, {"id": "UC4mHpkf_26sEWN8M5PMpV_w", "title": "Fola", "thumbnail": "https://yt3.googleusercontent.com/iNIPAZMlv3nM3M9UB9WqFRmUOXrtMWC8hDB37nE6-ZZaX9kl-NFvbNxN2wRDXINeDPIctT4FwA=w120-h120-l90-rj-dcJUWIzSkE"}]}, "Buena vibra": {"songs": [], "albums": [], "playlists": [], "artists": []}, "Amapiano": {"songs": [{"id": "kue1S_ca9s4", "title": "Zimele", "artists": [{"name": "Optimist Music ZA", "id": "UCU8qZ3SHX2KFjjSKzmNmksA"}, {"name": "Scotts Maphuma", "id": "UCXD6Yinh2bCvBf0A1Dq165g"}, {"name": "Buddy Kay", "id": "UCT3rBRz1HouXtJydNJWCZmQ"}, {"name": "y", "id": null}, {"name": "300it", "id": "UCU0XLE6lTAX8AfdOMmeNWvw"}], "thumbnail": "https://yt3.googleusercontent.com/jcQXUKLmsvAvkWz5ZM53JIMRLhTutG8ovuTW3pEDVYi9zyrZ5hGEOljr3W3q3IRnL6C3yrDWC-735yOggg=w60-h60-l90-rj", "explicit": true}, {"id": "jIVyO5rNgp4", "title": "African (con Frank Mabeat, Happy Jazzman y Hulumeni)", "artists": [{"name": "JazzSoul Mdu y Tribal Soul", "id": null}], "thumbnail": "https://yt3.googleusercontent.com/mABYrX5W0FKAQBZnVex1YtEhFhGEWcmVTeBfbwIdLIiNWBoPA_wP366JdXceLxM9iX7z-lvAnxcYjlWJcw=w60-h60-l90-rj", "explicit": false}, {"id": "7z7aZXtkvLA", "title": "Liquid Silk Debris (00:33 Shortcut)", "artists": [{"name": "Justin Humphries", "id": "UCeHnrbWJBSOt-MiI5wiW0Mw"}, {"name": "y", "id": null}, {"name": "Gary McKay", "id": "UCZM1f_COda5uQXpRgiEZcMg"}], "thumbnail": "https://yt3.googleusercontent.com/iVJW9lnM-Y97RRxXVIaeGY7HY5fLxgkLcqzYMKuNd1dFfr5jv_fBUDJaDZoXQdlPUYUEoVCpvDmjwiBN=w60-h60-l90-rj", "explicit": false}, {"id": "fvZyyviUGFg", "title": "Akitsi Jwang (con King P)", "artists": [{"name": "Kati Elimhlophe", "id": "UCnLjHI_RO4ywzCVTD-AG0qg"}, {"name": "y", "id": null}, {"name": "Fata Pata", "id": "UCs82lzYMwXPffiZ309dKVJw"}], "thumbnail": "https://yt3.googleusercontent.com/HSu4upCku9NhRMo99dJWtJocbnMOoCg6MEGX_2cbBq756ga3riKkihQVVimYJS7F-xGJ1TsAsmXLnmM=w60-h60-l90-rj", "explicit": false}, {"id": "cfAVbfjU2d0", "title": "Fokol", "artists": [{"name": "Sdala Deep, Mid9t, Ocean_SA y Slimeboyy_LeGupta", "id": null}], "thumbnail": "https://yt3.googleusercontent.com/drhzRdFLVV-4EStx82uB-Hm9dsHKTXnlnkXCzmP3ffVIFa3E7n9PxgGzjOoEkjdc9M46e5-eH1vK7f8O=w60-h60-l90-rj", "explicit": false}, {"id": "ZEPBQUYo_CQ", "title": "Seducer", "artists": [{"name": "Sbuda Maleather", "id": "UCl4M60Hkj--4eSKuEbrod_g"}, {"name": "M00tion", "id": "UCPV6C6o0AsxqX6EXYS5UvHg"}, {"name": "y", "id": null}, {"name": "Boips", "id": "UCtI0ESTV6cIfA9d7DNHBRkQ"}], "thumbnail": "https://yt3.googleusercontent.com/glgQeqKIuS7Q37QYUyrts6uqDct-Li9LqWKDzuzZLtBct_WtiZ716nmeU1bbnp8Amv2XRqq5Qtzr4vI=w60-h60-l90-rj", "explicit": false}, {"id": "7xTxyNGB2LA", "title": "Grootman (con Scotts Maphuma y Shaunmusiq & Ftears)", "artists": [{"name": "Stady K", "id": "UC3DY-1J1M4odFHg0EZTlDjw"}], "thumbnail": "https://yt3.googleusercontent.com/AWroj_zIGxjHGuuWH3bqsXzQosifGy1H6KzLfRgeKl7bYgVly7lAAH72twNoMjSBTflFblAEvsvrNa2DlQ=w60-h60-l90-rj", "explicit": false}, {"id": "KdU9CA3mMc0", "title": "KU'NJALO (con Masterpiece YVK)", "artists": [{"name": "Musical Jazz", "id": "UCPuWRwEDLHnvhugIPwiREeA"}, {"name": "W4DE", "id": "UCFEojCzxVZlyXF5m8pfTsXg"}, {"name": "y", "id": null}, {"name": "Chley", "id": "UCvqmPFSJFHO6yANt6t6t9Nw"}], "thumbnail": "https://yt3.googleusercontent.com/S-BdlsNro7eDxyQv_DnnKySTYYeMYvLrRsFBp0O8uSpbID9WrVWKxOIoh3ZCrpRfNIgTtlYczs7wrL5f=w60-h60-l90-rj", "explicit": false}], "albums": [], "playlists": [{"id": "RDCLAK5uy_mfuAOVrpzolzVSNCU6ekcPE_JrslkppBg", "title": "Éxitos de Naija 2010", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/Ng5CCrFLkstuol3-HKx5wk0Gj5TKdW07UbeJyNS0nbh1cyN5mLn23ZPOvZ004jHaVEbNj2KP2RyYNnE=w544-h544-l90-rj"}], "artists": [{"id": "UCM-A9dWyo7JfcxwlqB6q9Zg", "title": "Kelvin Momo", "thumbnail": "https://yt3.googleusercontent.com/qzi_BZu9LZST5uukJrWrH7jr3wIWCXDvZVItqdK4ub3s55xRY_grE7017UxZn1eQnbTQjZQpnPQ=w120-h120-l90-rj-dcsTSUkJ0J"}, {"id": "UCM-A9dWyo7JfcxwlqB6q9Zg", "title": "Kelvin Momo", "thumbnail": "https://yt3.googleusercontent.com/qzi_BZu9LZST5uukJrWrH7jr3wIWCXDvZVItqdK4ub3s55xRY_grE7017UxZn1eQnbTQjZQpnPQ=w120-h120-l90-rj-dcsTSUkJ0J"}, {"id": "UCM-A9dWyo7JfcxwlqB6q9Zg", "title": "Kelvin Momo", "thumbnail": "https://yt3.googleusercontent.com/qzi_BZu9LZST5uukJrWrH7jr3wIWCXDvZVItqdK4ub3s55xRY_grE7017UxZn1eQnbTQjZQpnPQ=w120-h120-l90-rj-dcsTSUkJ0J"}, {"id": "UCM-A9dWyo7JfcxwlqB6q9Zg", "title": "Kelvin Momo", "thumbnail": "https://yt3.googleusercontent.com/qzi_BZu9LZST5uukJrWrH7jr3wIWCXDvZVItqdK4ub3s55xRY_grE7017UxZn1eQnbTQjZQpnPQ=w120-h120-l90-rj-dcsTSUkJ0J"}, {"id": "UCM-A9dWyo7JfcxwlqB6q9Zg", "title": "Kelvin Momo", "thumbnail": "https://yt3.googleusercontent.com/qzi_BZu9LZST5uukJrWrH7jr3wIWCXDvZVItqdK4ub3s55xRY_grE7017UxZn1eQnbTQjZQpnPQ=w120-h120-l90-rj-dcsTSUkJ0J"}, {"id": "UCaaFBasSYBSfyFCBdJSihCA", "title": "Uncle Waffles", "thumbnail": "https://yt3.googleusercontent.com/cSjrotOYrzBOuJePo37fYFymUYq-gv2Tlxusox_J5kVJnDloU7xHd2EdfpZBDh8uTXS5eWTj_g=w120-h120-l90-rj-dcrRSIrxEK"}]}, "Clásica": {"songs": [], "albums": [{"browseId": "MPREb_ICTkFOd0ZV6", "playlistId": "OLAK5uy_mFulQM4MOec4kePoxAgXhgbsBvXLNS9pc", "title": "Bach: The Complete Keyboard Concertos", "artists": [{"name": "Mahan Esfahani", "id": "UCOK0EpRAjm02hrthEIEGr4w"}, {"name": "Johann Sebastian Bach", "id": "UCFtSXTlIMFFkyJbHO3V5b7A"}, {"name": "y", "id": null}, {"name": "Britten Sinfonia", "id": "UCh62in6g8K_eFFwJosp22_g"}], "thumbnail": "https://yt3.googleusercontent.com/_YbtwBcRKDIDrTUKa6DqoKT62fMbMN3G_o9jX6S9cFSXC1YVNmIiR4F_xCC7pUDxK1Wkrw8qjcjIsjs5=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_jc0ICHtC7A5", "playlistId": "OLAK5uy_m-__PBfX2xU8Wz7XHxTXiAWq3SwLVxSa0", "title": "Beethoven: Complete Variations for Piano, Vol. 3", "artists": [{"name": "Cédric Tiberghien", "id": "UCdocnwBRcbmmd8nFEPgeX8A"}, {"name": "György Ligeti", "id": "UCOd1bM7vQD9XbOj34d_0zSg"}, {"name": "y", "id": null}, {"name": "Ludwig van Beethoven", "id": "UCnsAooIr-Dsr8zJOCSadQcA"}], "thumbnail": "https://yt3.googleusercontent.com/IHeQ5aPlMsgkRtebSfie2OJT8PnuLDo0kU5kYRpdrrsTeOn9w38nc7dHuS83Ji-2Lmsg5maWDnx4FZQ=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_aMejNvF5MG1", "playlistId": "OLAK5uy_l6fLstHcVnFvC8rriSy_andEYrDQgB5E8", "title": "Disclosure Day (Original Motion Picture Soundtrack)", "artists": [{"name": "John Williams", "id": null}], "thumbnail": "https://yt3.googleusercontent.com/6ECpD4hRj5kTu5XY726fd9fGBgv_9plJ1iFsfEtEPdQ1y1s0KAMH8iNQamas5OIkFCTQtutUtRMg53w=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_Vw1TMLWL0ZM", "playlistId": "OLAK5uy_lR6gpaNYe16EycaumcMSS1Cmh9IP7Vr2c", "title": "Haydn: Sonatas", "artists": [{"name": "Denis Kozhukhin", "id": "UCRiVXB0YnLNzpDz_T-lUToQ"}, {"name": "y", "id": null}, {"name": "Joseph Haydn", "id": "UCud8NJ10_Nd6C9-eV1AFE6g"}], "thumbnail": "https://yt3.googleusercontent.com/-KTEOXC7x6rXI9t9NQRHAtSV54rlYlvK7IxMN11VTAcbEUdwOULZLZZgyqgqKf_nNcCsTXoSPmLownM=w60-h60-l90-rj", "year": 2026, "explicit": false}], "playlists": [{"id": "RDCLAK5uy_kk9Tes94U0LHlttI2bfPQ1Ifm_pdVlBXQ", "title": "Música clásica para estudiar", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/7vKTyDthvdQ8yDpQE3Merh_XsMOH0pVXCBYNjY_Dm2JgtBsiXIr7lmMC2U-32E9zgCFcqiNRco1tnV0=w544-h544-l90-rj"}], "artists": [{"id": "UCAeLFBCQS7FvI8PvBrWvSBg", "title": "Armin van Buuren", "thumbnail": "https://lh3.googleusercontent.com/KVGUvR5K_RMJMEP63mPrFneFDBX0vXHxFY1hrDFmTIbSc1LklAv8yy9KUeBPghfDuqOQ8muK6MpKJpOV=w120-h120-p-l90-rj"}, {"id": "UCFtSXTlIMFFkyJbHO3V5b7A", "title": "Jan Sebastian Bach", "thumbnail": "https://lh3.googleusercontent.com/C5hLBPYdwgyQEKjwZXfOM-0MCgn619Ja_WgljDeIO-fjNHi-SsYzGr_c5piG46OaRj_ROZqhuCYhucmf=w120-h120-p-l90-rj"}]}, "Up Next": {"songs": [{"id": "Wr8ZNq7N1dA", "title": "Ultra Terrorific Fantasy", "artists": [{"name": "Six Sex", "id": "UCmg88UqcHuEOLSuBuQwQoJQ"}], "thumbnail": "https://yt3.googleusercontent.com/llvX2yNJEvkHheW499OubAPNT-eJ6A6KfxvalPAsZjqnc2DQxUzDD5sKbsmM-rbwdGp9NTB0NSHAazoN=w60-h60-l90-rj", "explicit": false}, {"id": "mSTwkh3bG28", "title": "nos keremos tanto (con AKRIILA)", "artists": [{"name": "iza tkm", "id": "UCVD07glj8xlqWFZvOmgxqRA"}], "thumbnail": "https://yt3.googleusercontent.com/KjWdfeXhAAn3xK7QHUPlzQQyAFXVa7TISo1vlE9O-s3v9GEBhXq17BU20R_81tj_TPb-gpIWIoygcGD8=w60-h60-l90-rj", "explicit": false}, {"id": "a5ZRXGsRGlc", "title": "BRAINCHEM", "artists": [{"name": "EMJAY", "id": "UChrf13jz8FqaApYPXVbgaMQ"}], "thumbnail": "https://yt3.googleusercontent.com/si-wo5SW5Tif-Uqwxw32V6UdU8wU2IWXXQRfTSagZyycHC6Q2VxR5fOBIz8AEosKlH4E-3wgk7s9qvtc=w60-h60-l90-rj", "explicit": true}, {"id": "SGBrSaK4prw", "title": "Dime si recuerdas", "artists": [{"name": "Cachirula", "id": "UCrmNjjwwfTpSj_nwTWux-Sw"}, {"name": "y", "id": null}, {"name": "LOOJAN", "id": "UCDA8wmXYjhkLr0qW4Nca3cA"}], "thumbnail": "https://yt3.googleusercontent.com/zBOboG2XcckmJDzSXq_jHw2c0Y5jVlM7_e66RjhLmvoKE6evadA87mQpIgKxQov4ZXKkwS7uCcB_p3LI8w=w60-h60-l90-rj", "explicit": true}, {"id": "E7mA_-8vdwQ", "title": "cariño", "artists": [{"name": "Miranda Santizo", "id": "UC1N_fke-JlMNaTi6CoAQXAA"}], "thumbnail": "https://yt3.googleusercontent.com/VY9qkcl440aF7yRnuYUytjUl7dFlNYBIE4Zt7wBnY0pP6Vm8m81HGt3C6f5L5wvyj6lvxagMBYEZVI9c=w60-h60-l90-rj", "explicit": false}, {"id": "4dc3fwMdtjM", "title": "Muy Maniaco de Mi Parte", "artists": [{"name": "Sanje", "id": "UC5Ufxt_QAv92c84jrKKRDJg"}], "thumbnail": "https://yt3.googleusercontent.com/-eB8tsIqPeyzuJPtTp1veV_nssnMKPAFxBaSJBSnqJwtv7l-a_FhQSW4MCb-ub09ciTdTbIeZjMhs_E=w60-h60-l90-rj", "explicit": false}, {"id": "WWtsQPNL-x8", "title": "la mexicana", "artists": [{"name": "Paloma Morphy", "id": "UCmWYGcKlXd9CYsMtIC9_eiw"}], "thumbnail": "https://yt3.googleusercontent.com/OqHKI_AvXK_OGbSbeFM_Wb3nr6azz5A38bD-CaJU8hQhTxrPY2kjIBl5zgbFOMe1PU3hWJSqtWU3zP4=w60-h60-l90-rj", "explicit": false}, {"id": "fcE602ooTd4", "title": "solo por hoy", "artists": [{"name": "Silvestre y La Naranja", "id": "UCr1bQ7rAL4tatXdokh3xkYw"}, {"name": "y", "id": null}, {"name": "Daniela Spalla", "id": "UCLSBwPjNc2kH-3cBn2AbHcA"}], "thumbnail": "https://yt3.googleusercontent.com/6LVLsBjXDIxMAZ56ktqufcdTJcchrGyD48nAiRyUsS6C9r1Iv78xbDcxLK-Oh2SSN5vDToosV-f9b0gf=w60-h60-l90-rj", "explicit": false}], "albums": [], "playlists": [], "artists": [{"id": "UCA40xfjR5ay232HdLg1D28w", "title": "El Americano 4KT", "thumbnail": "https://yt3.googleusercontent.com/1uCpqWEXkmXsKuo6leHERFSd4qOM5vRuZBdJmT66K1shZe6mZoMI-GZTi42PSSgw3qXfYcC4YaOPOvw=w120-h120-l90-rj"}, {"id": "UCiMcavnI3L8L-ayXZtrrwgA", "title": "José José", "thumbnail": "https://lh3.googleusercontent.com/r4t7lQKmceFe_hrRv3aDMAeZTvMUpYpJW5ts6rBbSJi06Kc2a7qBr2FtltH9YJ44lHWsdYRaOT-Pv2w=w120-h120-p-l90-rj"}, {"id": "UCkmYD_GB8cyNmWXlpE_eOsA", "title": "DALE DURO OTTO", "thumbnail": "https://yt3.googleusercontent.com/btAeLRKhyNvu2pNPASMbR_UAmeeAT4TKzn8vNt4ubNyEjhZDWcZtJQbtxBY3LokNImMZDIi03SvEOReB=w120-h120-l90-rj"}, {"id": "UC_RUm7bmggZttesR7zmeusQ", "title": "El Nuevo Orden", "thumbnail": "https://yt3.googleusercontent.com/rf3iI27iR_TQRoL97dVXCfOA-hNvWFktpH1-1Dn3yaLPtqbYOn2CVbzci46NPTXgN7ykhbXtMXWdDLyK=w120-h120-l90-rj"}, {"id": "UCAPkKU77agA5jf9xklTLSig", "title": "Up Next", "thumbnail": "https://yt3.googleusercontent.com/aMBjIdxU29Fe_2WnX_AhZ61_JRrSVoSutIihOMIsXe70tRie171rWUsdNYkLJulDO6ClvKK405riooQcdg=w120-h120-l90-rj"}, {"id": "UCasWPKpyYI3sac_0JwugluA", "title": "La Clika", "thumbnail": "https://yt3.googleusercontent.com/TiRg9MnYP6fTMPOpp5O2V5YKa9yXtF4eHpBS-c6h1soYOtwEl823M-AJdl-WPeJjb4a3rCjr-A=w120-h120-l90-rj"}]}, "DJ Mixes": {"songs": [], "albums": [], "playlists": [], "artists": []}, "Músicas del mundo": {"songs": [{"id": "Tx00saMRPQ8", "title": "Manhã", "artists": [{"name": "Bruno Berle", "id": "UCzjqtPjs2yAdrOruKs4BxrA"}], "thumbnail": "https://yt3.googleusercontent.com/nfKl46UVFxriEBiAALDHnzWp-oToS6We-ifv-0vnJ4UWMW3JHNxFOq4QraHObHCuXjlCHHAh6gqMgl79=w60-h60-l90-rj", "explicit": false}, {"id": "zxS53bv3myw", "title": "Lingua Do Mundo", "artists": [{"name": "СОЮЗ", "id": "UC5IvYZmcGvQpBD_2YfjdnWA"}, {"name": "y", "id": null}, {"name": "Tim Bernardes", "id": "UCRaqIeFmhD0UEVIXRtQ5MDA"}], "thumbnail": "https://yt3.googleusercontent.com/8misrhoXB3Vkk1dGAld9bI4zgOtaRU5nNQ0w50UQ3H7Ax3p3C432cIFi2RfR3mVe6seJAnAeFcydIRr4=w60-h60-l90-rj", "explicit": false}, {"id": "M5HPaCO0uwk", "title": "Bando", "artists": [{"name": "Angélique Kidjo", "id": "UCKy_RKvarNHSW-1IZzDFtBA"}, {"name": "Pharrell Williams", "id": "UCJw8VyO6e3v6S0327AsgwcQ"}, {"name": "y", "id": null}, {"name": "Quavo", "id": "UC5IkSn-EFsUu3XANYklXc8g"}], "thumbnail": "https://yt3.googleusercontent.com/C1ulhJw61x_pM2EN_q_oxbC7W6RGJTkjfYgwUtUkC10WiY0irQC_lKlp2GAk8NuOFw703T2asc8NHPBFUA=w60-h60-l90-rj", "explicit": false}, {"id": "5QiyB4JIr2E", "title": "Yolele", "artists": [{"name": "Papa Wemba", "id": "UCnhEIN4gNDC-uSLaBxRMFXg"}, {"name": "y", "id": null}, {"name": "Kitty Amor", "id": "UC9MJUseyWLruqI8sPTmjM0Q"}], "thumbnail": "https://yt3.googleusercontent.com/Vy82Hd2qxlKTc-Q0dPYpNiZgz9RywM-WTkNatBjLMCxmjLTkRT3jEs7GTZtALaL85Ye0wvxe8uJm21A=w60-h60-l90-rj", "explicit": false}, {"id": "Xo2bgY_iTvo", "title": "Don't Tell Me Nonsense", "artists": [{"name": "Franck Biyong, Tunji Allen y Tony Allen", "id": null}], "thumbnail": "https://yt3.googleusercontent.com/HBNhCHFgjKb0GWwYcPOkwPaDIA_Q-c2h016CzdC_WYGKQFpV4V5pS6o52iJVV0RRXekugM7XBXOSmZfK=w60-h60-l90-rj", "explicit": false}, {"id": "e84RDz_7iNw", "title": "Pasayadan", "artists": [{"name": "Ganavya", "id": "UC3AjrBh4j2DGlypMN_PT7Iw"}], "thumbnail": "https://yt3.googleusercontent.com/gd8pijhbmHOKYjZ97jj-C7PyBFEemtie33VMtRPZAj87BDzSsxvmtnp3bLOADI31iihG0Azwk6cdz9HpBg=w60-h60-l90-rj", "explicit": false}, {"id": "dEf18kzKERI", "title": "Imidiwan Takyadam (feat. José González)", "artists": [{"name": "Tinariwen", "id": "UC8s6TihH6f3Ry2dcTcZAXyw"}, {"name": "y", "id": null}, {"name": "José González", "id": "UCKkS_TNTBWBs2LtoLWJ5A6w"}], "thumbnail": "https://yt3.googleusercontent.com/BSOx_LZSj5FhnA6x2_TuJfN9DYhRiHvIVEPUhPtXqRUWQ_Fvgal176Nd3PzmMlIKX4oHxmKQlcLhq01k=w60-h60-l90-rj", "explicit": false}, {"id": "9y17DCshS10", "title": "Se Rere", "artists": [{"name": "Lekan Animashaun", "id": null}], "thumbnail": "https://yt3.googleusercontent.com/77kE1bX_5vFKYzZIyGPOQFfiBg8ROoTB-PnlXmsxiAXY5rRUUiRE1wnzBHLIRs5Y0qzYKkJRVmRS4PWc=w60-h60-l90-rj", "explicit": false}], "albums": [{"browseId": "MPREb_rA7M8MCDAqs", "playlistId": "OLAK5uy_lXbN9m4_RzsxxcKyu8WNNRh66uU3czj0A", "title": "Slaves Mass (Expanded)", "artists": [{"name": "Hermeto Pascoal", "id": "UCXx-2CYsOF8gz7P0ce2lsQQ"}], "thumbnail": "https://yt3.googleusercontent.com/mO76CymzI-4JlOfVwxabImIIF7KzeAu0Q947SKgueIVgysoeNjKgeW6qWucbvpVVAIeogWZKcaQ1Mx0=w60-h60-l90-rj", "year": 1977, "explicit": false}, {"browseId": "MPREb_AbbEoZKvPQP", "playlistId": "OLAK5uy_mZ-Web_J4qSmBfR1uf3ewdaQPVYfvQyrU", "title": "Rrakala", "artists": [{"name": "Gurrumul", "id": "UCIHq4rjAu2RIkXJOQ20fyCg"}], "thumbnail": "https://yt3.googleusercontent.com/q8OIlnIhlipWSGk_xZ7LBP0ORTMz9_ymhZl5He7YR3V-6I4flWUwi5KKUVKjrKy_dh16p_KPTFFz0TsU=w60-h60-l90-rj", "year": 2011, "explicit": false}, {"browseId": "MPREb_MTEEXVOXBJc", "playlistId": "OLAK5uy_kRQKDZKp5CQs0PYiBsBtMr0NAAdu2lTcs", "title": "Wenu Wenu", "artists": [{"name": "Omar Souleyman", "id": "UCSAkyW7Pc1xls9YbOv-YQ3w"}], "thumbnail": "https://yt3.googleusercontent.com/WRwKBAX987T6yGfz47slsgAQbhj9GRrrHKuVHBofMXepsG1Rv7s7HsTFPXywvCU7R5nczt2NhCUJsM8E=w60-h60-l90-rj", "year": 2013, "explicit": false}, {"browseId": "MPREb_LIQn9yZeILq", "playlistId": "OLAK5uy_lBATXLxOwrmT4wqA9pUkWwvTrMFfvkUF8", "title": "King of Juju", "artists": [{"name": "King Sunny Adé", "id": "UCq-SwmezM_e9jqiQ66uXrgQ"}], "thumbnail": "https://yt3.googleusercontent.com/R8k11ax6jo6DJhmU6rIMhX0EMVOZhHsNKVAW1ltBFkuDAV5Ic5-7On3AfrOvdMFHHD35fzIRA6ZPe6Vj=w60-h60-l90-rj", "year": 2002, "explicit": false}, {"browseId": "MPREb_g5ZG2RuAjdz", "playlistId": "OLAK5uy_lY1FAItFHoQTvgUuO9YQUaGtynkBAD2ow", "title": "The King Of Slack Key - The Best of Gabby Pahinui, Vol. 1", "artists": [{"name": "Gabby Pahinui", "id": "UCnZHV50M804Pq3wGLmSHZfA"}], "thumbnail": "https://yt3.googleusercontent.com/InzdCKIfK_9-Ep9jBVe8o-9tvTVT8OjAVxvqtmUD85MPdgwHaRcqrQOlm2ZBTssWT3YJuE8D1cZMNRxZ=w60-h60-l90-rj", "year": 2008, "explicit": false}, {"browseId": "MPREb_sLOWff69czl", "playlistId": "OLAK5uy_nc-yspNlHO2y8I1BMWWkigo5ShX5xYJ4E", "title": "The Astrud Gilberto Album", "artists": [{"name": "Astrud Gilberto", "id": "UCHfuC6qjRi-7wQobu_WCuig"}], "thumbnail": "https://yt3.googleusercontent.com/qktsweEmAZQsfKTrQmcrolcN74PLuvZr-JkGu0VAHF4Lhnsz35po8cxHCSe_Jkl1RlK_AkaB7HRlG4o=w60-h60-s-l90-rj", "year": 1965, "explicit": false}], "playlists": [], "artists": [{"id": "UCzEIYuYX27ZmTBTASkIjKZA", "title": "Mulatu Astatke", "thumbnail": "https://yt3.googleusercontent.com/6JUWXlw3MHpu01Z19OKUGlo85xK8mUEPO8n6sgBBDYWi6cLZrRo79Ytqzmi2GmnV8JpqewZ1ipMM6Q=w120-h120-p-l90-rj"}, {"id": "UCryLn7Q9YJC9ZyvjonAlPmw", "title": "Les Amazones d'Afrique", "thumbnail": "https://yt3.googleusercontent.com/HKweQ0suHntKfcoZCcHpV8YxecwxeY1J7LBS882JLr2IxoeF7yp2Xb8fAcptxmnevmPT08vb-uZuWspG=w120-h120-l90-rj"}, {"id": "UCh6JYkn11gYYDqgkXAOIuzw", "title": "Arooj Aftab", "thumbnail": "https://lh3.googleusercontent.com/PemxXieLprHxplM8tZNJudy5jMfaTNn9v2Bk4vVSOZwXlAmXy_ZFocAqLzvVTdyXaBTqFh5huzARQU8=w120-h120-p-l90-rj"}, {"id": "UCV6x4toxCw0-bn8wSTvmwvQ", "title": "海朋森", "thumbnail": "https://yt3.googleusercontent.com/d7dqrbQco8qX7_ZLvxglb_Gz65gt57sjXuavAo7AkD72LgC3eEBcUYEYyapUBJtA5SwVwKgiSryxLrk=w120-h120-l90-rj"}, {"id": "UCbpNiniojLrbl47HWUnW94Q", "title": "Sheherazaad", "thumbnail": "https://yt3.googleusercontent.com/8fxncImUjJDi-7Rz6AecapnPVzpGUbB8fqNRZEa-gSA83Ya-tNlvf3OTjU0URUVeORDiUMl6WiY=w120-h120-l90-rj"}, {"id": "UC8s6TihH6f3Ry2dcTcZAXyw", "title": "Tinariwen", "thumbnail": "https://yt3.googleusercontent.com/oxClhqtHhTgaXhuhdL2pnuTLtZXQO8mgRdXr967Yx-nVFAZVlyUr692KIQKqyPd-KjY-UKLryw=w120-h120-l90-rj"}]}, "Rock clásico": {"songs": [{"id": "lh0w01S7Jnk", "title": "Werewolves of London", "artists": [{"name": "Warren Zevon", "id": "UChDuOKD_xJJqrHcwGtfch8g"}], "thumbnail": "https://yt3.googleusercontent.com/vwFRcTWk2B3WfcLLUXXURyCdWgQ91ziCXQ3HfuWl6kxNa_Ssu2x0k8cDU4eY4JWVnYviDR-YAOMhmLfA=w60-h60-s-l90-rj", "explicit": false}, {"id": "RSZca1Q9IWA", "title": "Gallows Pole (Remaster)", "artists": [{"name": "Led Zeppelin", "id": "UCYtap7ujIPaxTS2iCDoMi3g"}], "thumbnail": "https://yt3.googleusercontent.com/3pSmEJHaiESTU61aZfkobVrQfubuua5_q9isiAmDtRDemuMPlPcP0mpi6Ch3CQTcC6kuzLhzo7p2uMU=w60-h60-l90-rj", "explicit": false}, {"id": "Swe2vw4k8TI", "title": "Heartache Tonight", "artists": [{"name": "Eagles", "id": "UC49r4GNHHpc-eQ9hmD2Rg6A"}], "thumbnail": "https://yt3.googleusercontent.com/rrz5QLIQkOuPQhxc8l_4Cwsg79y10C5TG2A6CBMQDUmjwa-khndmmsD07cYE95Os2d5ntuiL9WUPEd4J=w60-h60-l90-rj", "explicit": false}, {"id": "eHhI7fZ6aIk", "title": "Feel like Makin' Love", "artists": [{"name": "Bad Company", "id": "UCNGSRmH2L_9N4B4tO_LmaXg"}], "thumbnail": "https://yt3.googleusercontent.com/xLUJmeJiCi9y0Kpp24XcTkb1Zyw62nVJS9cEsfbCZn1cS-SIAQqrNWjhmdxLlL4H6bHj9IEi8LYrTCIs=w60-h60-l90-rj", "explicit": false}, {"id": "aEJ7p78YrpE", "title": "Takin' Care Of Business", "artists": [{"name": "Bachman-Turner Overdrive", "id": "UC5mrFYxM2gUV8zkM-kh4rJg"}], "thumbnail": "https://yt3.googleusercontent.com/ELTeO9ddsdF8dBoPsr7Vv1QmXym7Vg8GeRP5eTZjVwRCDJYe4GnjJjO4pEJ7o9OOczXW8vsT8s9Pt_Q=w60-h60-l90-rj", "explicit": false}, {"id": "hU8o6usr_oU", "title": "Street Fighting Man", "artists": [{"name": "The Rolling Stones", "id": "UCNYhhkQqeFLUc-YEDcLpSYQ"}], "thumbnail": "https://yt3.googleusercontent.com/ZOCO8iiimiHyADP4h6n91wtGZXgVHdF72lU3HaIicyki4dr30WPrV2PlMmdXMFzldefzxMQUIYoHffU48Q=w60-h60-s-l90-rj", "explicit": false}, {"id": "_S5CQQPXxL4", "title": "I Drink Alone", "artists": [{"name": "George Thorogood & The Destroyers", "id": "UCqpoLMU6P4AE51kbChCldEQ"}], "thumbnail": "https://yt3.googleusercontent.com/K_jIn0fcDeeQ6P92CleGTWSALF2gIjv7QtBl9pdZVNn2b_AIMT8ANMyWstktDEIYCE9fyX1V3-U5EsixyQ=w60-h60-l90-rj", "explicit": false}, {"id": "qBY_eH8ZNhY", "title": "25 Or 6 To 4", "artists": [{"name": "Chicago", "id": "UC8vVfqDySe2dz-wCNcy6jew"}], "thumbnail": "https://yt3.googleusercontent.com/IRlXdgJV-jcJesWS2GNj-rBhED0G9RbW0h9cTqHXljcSoUd4yfV_g6rqYAJ5BFuqooEGywjTQD96hXHI=w60-h60-l90-rj", "explicit": false}], "albums": [{"browseId": "MPREb_tQfaWH32ovE", "playlistId": "OLAK5uy_lqcFZTOPHGwcnP0nYMzNuY0IES0fl7Fe4", "title": "Abbey Road (Super Deluxe Edition)", "artists": [{"name": "The Beatles", "id": "UC2XdaAVUannpujzv32jcouQ"}], "thumbnail": "https://yt3.googleusercontent.com/g8bzAg2zxvdnm7ismLMYLA9-9azb4y6VP2uOF56A2G2rpsqLHT6mrJWXRKq_VttXQZ-o-jmVgTFIVgdj=w60-h60-l90-rj", "year": 1969, "explicit": false}, {"browseId": "MPREb_1SbgcDGdrT4", "playlistId": "OLAK5uy_lOWvxbyxSakNJBr3T4taG96SuTihpHGn4", "title": "Rumours", "artists": [{"name": "Fleetwood Mac", "id": "UCCzULu3prrEaPvM2ZtkJlYQ"}], "thumbnail": "https://yt3.googleusercontent.com/q9xfDFsJXtmAU8MNFyGCMMHe7upmI6S8eDNrPdbJFRr-jeo_xNy8JDutjP2xdpmtl0xfQJYleYrXQGc=w60-h60-l90-rj", "year": 1977, "explicit": false}, {"browseId": "MPREb_jPOYfjGgApr", "playlistId": "OLAK5uy_lYnxawfGdkGePjdFhIYaS6LjP-Md6UYf0", "title": "Nevermind", "artists": [{"name": "Nirvana", "id": "UCrPe3hLA51968GwxHSZ1llw"}], "thumbnail": "https://yt3.googleusercontent.com/eyKiPBSqEu556sYTd_IyZhfxun5e_hatZ9tAyu8bnmVRgtbM3aW-SXUvhVX-d7s1oU0Yf3a38JOuYMZK5w=w60-h60-l90-rj", "year": 1991, "explicit": false}, {"browseId": "MPREb_T7tsGX2wtUn", "playlistId": "OLAK5uy_nIDvA68GQU6dFSErUt5wnrNRt0d0jnMVk", "title": "Highway 61 Revisited", "artists": [{"name": "Bob Dylan", "id": "UCBqkojCXby4zGkWX86FEY7Q"}], "thumbnail": "https://yt3.googleusercontent.com/TL2EK5rqsv4nf2Fe3keiVfvTALiMJmPmTjhtfPH_6P511Lan2gTIBNCwGv1B0jFuC6omHpPaKtd_F2A=w60-h60-l90-rj", "year": 1965, "explicit": false}, {"browseId": "MPREb_5dJ4JcE3Fah", "playlistId": "OLAK5uy_kSxA3IV3xpP1jCo4iNFKPLcaA8trAJGf0", "title": "Pet Sounds (40th Anniversary Edition)", "artists": [{"name": "The Beach Boys", "id": "UCrnD8aXhy4oQMS0ITw471tA"}], "thumbnail": "https://yt3.googleusercontent.com/bsJIYRo9-3l92b9I2BnCxF1dc4vjYVfNxuT_gNDj9Rwf8YoR5DZFujK_0NVnGfx4pE6cQ97NVg1AGn_pLw=w60-h60-l90-rj", "year": 1966, "explicit": false}, {"browseId": "MPREb_eHEOTy3DS94", "playlistId": "OLAK5uy_kNH5_0dq0SINuzQFBDRKoCCcO0aTcGxoo", "title": "Revolver", "artists": [{"name": "The Beatles", "id": "UC2XdaAVUannpujzv32jcouQ"}], "thumbnail": "https://yt3.googleusercontent.com/r8_4I_rvh2kHa9Y-mSTH72Z84ncYx0SzPVLXXqaLEPYQrWqB03dizqePdZXBtAUa_La2woSY6czcx1U=w60-h60-l90-rj", "year": 1966, "explicit": false}], "playlists": [], "artists": [{"id": "UCPi7wU0ppI6ad7EEpHs7lHg", "title": "Van Halen", "thumbnail": "https://lh3.googleusercontent.com/sCvMbpauIZjmiLEXXF36fZMXyD1wj7ASbh2CllFVdVR3snot7iORVZ0WrkllssChHNLYUIMsWB4fHF2b=w120-h120-p-l90-rj"}, {"id": "UC9dWluHyEwvIj5NNs7KkJ0A", "title": "Grateful Dead", "thumbnail": "https://yt3.googleusercontent.com/JqvGjAZu6e_vpy7IB28maFpMwLSemhKCJVUZYreAJU048tDFha-6-daqHfxmWejn0tqHQn96yJi3JtKm=w120-h120-p-l90-rj"}, {"id": "UCBqkojCXby4zGkWX86FEY7Q", "title": "Bob Dylan", "thumbnail": "https://lh3.googleusercontent.com/tn1ajpYJaL0uVyUIgegB0qw4f6ZhWb8YiHGoIZCTPHPrzFv1aq_vz_vzxzwoA9bgMK98WlzwPYco5A=w120-h120-p-l90-rj"}, {"id": "UCqIQRxCUGi7hyJisyzv9zYQ", "title": "U2", "thumbnail": "https://yt3.googleusercontent.com/AkQ_0Er0h2xS90d0_CrEoFfiW7AjaSoTAJ6SdAJmFdzaKRJh4jywzPWi6cAewGrpP2AywIGGfXUWkBo=w120-h120-p-l90-rj"}, {"id": "UC2XdaAVUannpujzv32jcouQ", "title": "The Beatles", "thumbnail": "https://lh3.googleusercontent.com/z8KZsHNKS-O1qYVyKlSErT_RLMSMwVht89USvSdFAd0EoRlBOppi9DOdRkv609Ye_tfq_Wp8WwhVJbw=w120-h120-p-l90-rj"}, {"id": "UCCL-yoaPLR-7bxZM1xlcQcQ", "title": "Bon Jovi", "thumbnail": "https://lh3.googleusercontent.com/TeMWqsVbQ8nVkgV3oq0SbUwDvlQBNQA6EwHYKcqCo8CvvDadvpAWdETie_fHkZSBOhwXEpYHPjCDxA=w120-h120-p-l90-rj"}]}, "Essentials": {"songs": [], "albums": [{"browseId": "MPREb_cQxqYtJzxSu", "playlistId": "OLAK5uy_kZk99Qkz6LUh1Kn4zR15Tp9ZDY1rX6Efw", "title": "Planet Her", "artists": [{"name": "Doja Cat", "id": "UCwgX_dLqGYna_7Fm8ecf4Ng"}], "thumbnail": "https://yt3.googleusercontent.com/LKHANNfV6CM0WCxrjdjOojWY8N6FSywKXMFbBSXfvtfZ6Fh1ysadjuFl0L266zthTJvnSc6BjBzf7MoEQA=w60-h60-l90-rj", "year": 2021, "explicit": false}, {"browseId": "MPREb_DudzVw1csBH", "playlistId": "OLAK5uy_kAbxBZW77nalziKTiaUDgQcsPhlDy_Mto", "title": "GLOW ON", "artists": [{"name": "Turnstile", "id": "UCvypVcrhYs22La_RvMUntfQ"}], "thumbnail": "https://yt3.googleusercontent.com/ayzWDKUX6nujyCBDrRMYhEEiSJIeIZz3r3Otc0QrIasF1tOs7h6JeSGHzkL2_6kZA4sRe-FKch7FwQJd0g=w60-h60-l90-rj", "year": 2021, "explicit": false}, {"browseId": "MPREb_SkmE4m3BOLh", "playlistId": "OLAK5uy_n7TsTfaJWU4PWbx-ALU6rzvo4n3AXXua8", "title": "VICE VERSA", "artists": [{"name": "Rauw Alejandro", "id": "UCw_m17sBd4GHxuK83-AZtuA"}], "thumbnail": "https://yt3.googleusercontent.com/PGZvpR1LqIYvJd9g_CB0n_3EDumnPAA4kDo7vUqLhQ5vkrwwaNsEkc6jnsmx-ZwPenhnDmNq3Tvt_urCiA=w60-h60-l90-rj", "year": 2021, "explicit": false}, {"browseId": "MPREb_VpXYEs5HeNs", "playlistId": "OLAK5uy_kSIbgi-WxAiBFs28kWN1cMy7bewO3-LCs", "title": "CALL ME IF YOU GET LOST: The Estate Sale", "artists": [{"name": "Tyler, The Creator", "id": "UCo1DYcm1IZ9v3UPkpiAcgtg"}], "thumbnail": "https://yt3.googleusercontent.com/FbvVm2YzUs0b8OADcsH9r-asx513XGM1vN_x9acYyA0384H3xXaobwAAj_mPsfohz6Q94bCH_PHwxKu5YA=w60-h60-l90-rj", "year": 2023, "explicit": false}, {"browseId": "MPREb_pah6lCBHIMA", "playlistId": "OLAK5uy_lgr79u3wxCLGr7ns01X_9dK1FsU-eizgg", "title": "Sometimes I Might Be Introvert", "artists": [{"name": "Little Simz", "id": "UCWMArai9zjUOTgtAF0oyoIA"}], "thumbnail": "https://yt3.googleusercontent.com/TWllI0de14Q58Nw9DSm6Vde0gmVCdD-JzfsiJoa723ekFaH5nRZxxbN64jMLbO3Q8dgkr3scbNo7veGZ=w60-h60-l90-rj", "year": 2021, "explicit": false}, {"browseId": "MPREb_KKDXQyZDR8I", "playlistId": "OLAK5uy_l9UV-n9zBD893UZMdqW9o0Cp4MfKslruI", "title": "Eternal Blue", "artists": [{"name": "Spiritbox", "id": "UC8RF_zYqBLbVXilKjh6sk2g"}], "thumbnail": "https://yt3.googleusercontent.com/SnvEEGF5TRb4KUf5_sv6hzgZzOuBSETNfy_WvoqnWkZgBnmDb63SbaH9mnLZc06kXyiHdrIygegFi39a=w60-h60-l90-rj", "year": 2021, "explicit": false}], "playlists": [], "artists": []}, "Concentración": {"songs": [], "albums": [], "playlists": [], "artists": []}, "Bienestar": {"songs": [], "albums": [], "playlists": [{"id": "RDCLAK5uy_lQ2iiRFCsEvoxLRfTCOyVd6-tdZ75Gw-E", "title": "Sonidos de la lluvia", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/4kY86HKnF6GY9VXvbcvNADz3SSiD-feffTNa32I1JXM7tbjL_UnPc3Q9OSH1esc9am-CzwjzB252tAc=w544-h544-l90-rj"}], "artists": [{"id": "UCMgI3CzClr-_jgsNhqJCm0g", "title": "Robert Bahedry", "thumbnail": "https://yt3.googleusercontent.com/2NlSpFhbHjlA1QGSpgs6wBOSbJ9CdA_qAi-ANRfxRbQOyn_rQm4YFEPQGAqoY8ZMclFFmBVGDjtgn1-x=w120-h120-l90-rj"}, {"id": "UCRJ31hbgKZzuikZoN4AzHQA", "title": "Orchid City", "thumbnail": "https://yt3.googleusercontent.com/SQ443hiFR-RD-hz1XDmjV-WQQLKsPN9rKdWXXbtRpXNnQi-0kztgqL16NKCHommq86P_R9ImoGdgbQEJ=w120-h120-l90-rj"}, {"id": "UCVxy53scuQxzHBQb5Lam17g", "title": "Tala Sky", "thumbnail": "https://yt3.googleusercontent.com/onGAnq0DgTckL-2AmwJAL8AP9iyCWTM3T7kgVJxlrBO5Ek44RNL5Uv88_oz7yFTUvvmypAd-38S_eKyY=w120-h120-l90-rj"}, {"id": "UCg7Cqw_2u-TgM_DS36BoUsQ", "title": "Sara Clark", "thumbnail": "https://yt3.googleusercontent.com/dsiJBu31YQiVlGfaH_w9U76BzYuumRswMscPIlr_ISU2DPX9vjVzFN0DCsl6BpVvb3QrZS38fw3X3SXn=w120-h120-l90-rj"}, {"id": "UCL5rcTC9kD36_5YZFdCDZtw", "title": "Halo Healing", "thumbnail": "https://yt3.googleusercontent.com/_y-VSPcgJStfJ3fzHWxulrMBE_ANAcotq4y9qq56vOiNYJAtKBqcCVLxlWOHvCQr6Zq2FKNaRq3_ADYTrw=w120-h120-l90-rj"}, {"id": "UCrf4ThRrhpazTaDMK5gefFg", "title": "Into the Bliss", "thumbnail": "https://yt3.googleusercontent.com/rvpquPhCaF1GYtYbQrcd5yxlsK8XdA4fPojMYCxliyNEnwTmBTZCv8yhV_sJRornCpEXa6qXPd7ms2c=w120-h120-l90-rj"}]}, "Jazz": {"songs": [{"id": "IXpI7g3Vve4", "title": "Rainbow", "artists": [{"name": "Gretchen Parlato", "id": "UCqBqIOLrQ9iSJSp0TtuSICA"}, {"name": "y", "id": null}, {"name": "Robert Glasper", "id": "UCLFHVgbSwvTgM1PcsOCvHjw"}], "thumbnail": "https://yt3.googleusercontent.com/Jec5DkFQjCZvS4DFP3Bz5iZulo567ZTze5u7xLKGzoqlswuspNhTTowVgPN6kBXx_xzk_QRCCiK-1tw=w60-h60-l90-rj", "explicit": false}, {"id": "eILpVkIWAE0", "title": "Soften", "artists": [{"name": "Laura Misch", "id": "UCant3y1RMmQNUV3MPIpXPVg"}], "thumbnail": "https://yt3.googleusercontent.com/Zb1nJ6PNjSDsa_4Kpge1LcAVJLQu4ENlJROXpGiIvNnOlVAvyQiOgzaKpIQT8r6VLVJYtAAF52naLRAVVw=w60-h60-s-l90-rj", "explicit": false}, {"id": "aW6naJ4XvFA", "title": "Let's Call It", "artists": [{"name": "Glass Trio", "id": "UC4VezzkQGvKiZqjsClbtIcg"}], "thumbnail": "https://yt3.googleusercontent.com/6POTFJKV5AKZwXmlm1U7AyV--rrox20ff_YPxN0ZzvCHg3EJJWyawikXEPQzWvUssWQuvFQvKZND-4w=w60-h60-l90-rj", "explicit": false}, {"id": "-gzRoD9UuXw", "title": "There Will Never Be Another You (Ornithology & Braxton Cook & Henry Wine Remix)", "artists": [{"name": "Chet Baker", "id": "UCVNfRXuaWVILL9giCEg0rQg"}], "thumbnail": "https://yt3.googleusercontent.com/lxpeycMDTPcA-J07knhta_EpLdAPMqupTPUNi3uG_rzwQsk9Wg5UVtWCASYexSh8ylM8suMRBsdRTGe7=w60-h60-l90-rj", "explicit": false}, {"id": "33SAoG1mdHc", "title": "What's going on", "artists": [{"name": "Monty Alexander, Luke Sellick, Jason Brown y Bobby Thomas Jr.", "id": null}], "thumbnail": "https://yt3.googleusercontent.com/_iS7eMjZuN6nXZttL2uTP1JLHH6o5B8GzXRInBjVh6zEdgDmozNid05yYNriOv9aZat3YKVllLQ0qoBU=w60-h60-l90-rj", "explicit": false}, {"id": "BggRmE06XDw", "title": "America The Beautiful / The People United Will Never Be Defeated", "artists": [{"name": "Pat Metheny", "id": "UCFFpzGzXK2Zw8fPher2JObA"}], "thumbnail": "https://yt3.googleusercontent.com/s2mkgnH39ufrhH3kWhPIee-_DJHXa9WUc_1d_hmlwWHHZY3EmaSqd0tAh7QMOOHo8M8PuZxPWK2bq4LzvA=w60-h60-l90-rj", "explicit": false}, {"id": "XQtnL3ImIkM", "title": "I'm In Trouble", "artists": [{"name": "Alessia Cara", "id": "UCrqLG11GjSzO5bVYzXIF8CA"}, {"name": "y", "id": null}, {"name": "Norah Jones", "id": "UCxRuL2yOu2ydTJNZuYdU0qg"}], "thumbnail": "https://yt3.googleusercontent.com/RwOB0Xfg01CmOM9MjCbM-FVfp5NgfcY-yN691KDlxEChLvXKEzG4DqL3H3BLCfYsE-aBtap0yagYAYE=w60-h60-l90-rj", "explicit": false}, {"id": "NKj3Okw33hQ", "title": "Ask Me Now", "artists": [{"name": "Billy Childs", "id": "UCrqmPcBWFwkj9E7_gbxGuhQ"}], "thumbnail": "https://yt3.googleusercontent.com/soiqfSXzu6ietMdxPHzIZj9zzshPc7IUJ0D0KDQG5YUknfD7-1rt0j2vo3lHnjQWKECvMGjxsMp4fn6c=w60-h60-l90-rj", "explicit": false}], "albums": [{"browseId": "MPREb_ZjEIkeVo1zr", "playlistId": "OLAK5uy_lIGZ3d9H9JFcg_2JX3BMUrk4ViFsQ5T-8", "title": "John Coltrane And Johnny Hartman", "artists": [{"name": "John Coltrane", "id": "UCH4T2kv7rr9qnuP0DOXLxMA"}, {"name": "y", "id": null}, {"name": "Johnny Hartman", "id": "UCuPyqdiIV94hGAxl9GwJHOg"}], "thumbnail": "https://yt3.googleusercontent.com/J9IaH4J4V6uqHA5LAzzF9TTtUluP2UsD4h1OYYwCRjyf5jVGEXJmY5Ccg3TzZJG8wEXkr1z3Gzv94O5a=w60-h60-l90-rj", "year": 1963, "explicit": false}, {"browseId": "MPREb_1JwamW5HGAe", "playlistId": "OLAK5uy_nCadsSovaCieKT8xqxSZlxJgK1pg7Uw_c", "title": "The Köln Concert", "artists": [{"name": "Keith Jarrett", "id": "UCm3Mj3cmimqk3qMPK_UJHvw"}], "thumbnail": "https://yt3.googleusercontent.com/iWDU0KPyatFqCU6QwCdYrQqinjWQlM3-UEKilMpX8Rq3r7Dsh65Nhw1yZXcqvuuUduyOLn90600SeiI=w60-h60-l90-rj", "year": 1975, "explicit": false}, {"browseId": "MPREb_7k77YjZciJp", "playlistId": "OLAK5uy_kZ0Qi2myyDm8IkpL_PeNJQzjgvfYKBUCA", "title": "Getz/ Gilberto", "artists": [{"name": "Stan Getz and Joao Gilberto", "id": null}], "thumbnail": "https://yt3.googleusercontent.com/L3yemh8_PivwvJ3XdPVJlyyJqDxcpL-dIphTs-OyV9fe_kyZQonVq8U9FUMnKCT1_LiidQNgk-10440A=w60-h60-l90-rj", "year": 2016, "explicit": false}, {"browseId": "MPREb_iNbKdKjDqyr", "playlistId": "OLAK5uy_kQdrECE-ozwNQzDlLQT2vsgVQp8DfHElE", "title": "Kind Of Blue", "artists": [{"name": "Miles Davis", "id": "UCIev2PktTH5mI-QlGmbUkiw"}], "thumbnail": "https://yt3.googleusercontent.com/l6OLgD6_wU1c0JQiUMf7Bd7Ts7fTkRKY3b6tUU019Bilv6xjilESCeOPTvXIXkXvjVJsvqt9FW1yHcN6=w60-h60-l90-rj", "year": 1959, "explicit": false}, {"browseId": "MPREb_F1OKobr9GVv", "playlistId": "OLAK5uy_lm79pWsUmJcnAPQdRZd9X41Ywv7m79cC0", "title": "Night Train", "artists": [{"name": "Oscar Peterson Trio", "id": "UCGO2MrCyB3dLCID5wLE0JUw"}], "thumbnail": "https://yt3.googleusercontent.com/o8508uW7F0f2VMSkbbiWhrCSq9FN2S7boZ76LItUNfmN--gzgRMYchcyIIf5LVy7ULSgssm3mZgVhCI=w60-h60-l90-rj", "year": 1963, "explicit": false}, {"browseId": "MPREb_d0lggJKktFo", "playlistId": "OLAK5uy_lDXihQ1ZDYmlnvs3D6x9AV1V8IDC9g0pw", "title": "A Love Supreme", "artists": [{"name": "John Coltrane", "id": "UCH4T2kv7rr9qnuP0DOXLxMA"}], "thumbnail": "https://yt3.googleusercontent.com/CTb8zOED76PoNoiziGGytUDU5SUNjGGLvn4-pNSLJYWkD2ZQdAMZOwmimBjGy1b7H_Y1FKY0xzA4AoY=w60-h60-l90-rj", "year": 1965, "explicit": false}], "playlists": [], "artists": [{"id": "UCJg4vr8nG-ofQwsKNKd2prQ", "title": "Aja Monet", "thumbnail": "https://yt3.googleusercontent.com/xXdyT0LARH1LIdU7vpMen93226xQZYtAe6DkMzldTUK0wFszoIRtup5uI8g38D3x-U_co39khg=w120-h120-l90-rj"}, {"id": "UCnaxZ9C8fHQYAn3XEEkU-7g", "title": "Immanuel Wilkins", "thumbnail": "https://yt3.googleusercontent.com/EbhcBugo-XLNQhb6dNv1a9CuAYM_r7UgPIT3rr_3PNNSDVrj2i1t3zVmpN9XsNavZzaOaRAPswV3NUIl=w120-h120-l90-rj"}, {"id": "UCOSoOnuGLFA3K0KymaUq5ww", "title": "Simon Moullier", "thumbnail": "https://yt3.googleusercontent.com/VptMqbVruVdIJwxnRoOLhVJtDztwldlucqHDPdvSduQ-bmTpm_Tg22VJKjnBFWsZ_r6q-oNresQNTrl_=w120-h120-l90-rj"}, {"id": "UC0Rf5u2mStlCvZEE66VURSw", "title": "Hannah Marks", "thumbnail": "https://yt3.googleusercontent.com/p-eXTdM3mgVpxCIbG1Hk6w7YqLaRKK-4-y4O_iYUs2V7RdAVOb2JM3B6BfCKrQoEvr9QMJ2mGcUmZw83=w120-h120-l90-rj"}, {"id": "UCeJcNV9p7TzSnFVDK-bn2XA", "title": "Black Nile", "thumbnail": "https://yt3.googleusercontent.com/ZqHD9cTnOHQzkKTU6uiZVcSNjwCwmbtqK_ushXjZF9sIx3pbgk_5VqkOD5FwWnbDhEfdUGQ6cgEVNvgl=w120-h120-l90-rj"}, {"id": "UC_BX6opGq49YU-_z1of-vbw", "title": "Marquis Hill", "thumbnail": "https://lh3.googleusercontent.com/gBzI8ACN0K2S-UinzTO_210-A9b9P_ljO5XIhxfuvE_on-V-KGJs9aqehJN6EAEBAjqTUnqspLDm2X0=w120-h120-p-l90-rj"}]}, "Metal": {"songs": [{"id": "XXjGUuPKRHc", "title": "Empath", "artists": [{"name": "Russian Circles", "id": "UCqLOZtEdRzXDzF-1CyJFc2Q"}], "thumbnail": "https://yt3.googleusercontent.com/B5jfQN1_Y2msWLQto8WP8AkARPYK53iBcL9dV1cB941k2lDjIezNbre67VFNKriNX1TxyeuhPb0nBVno=w60-h60-l90-rj", "explicit": false}, {"id": "26ShCkmNyf8", "title": "I, Scvlptor", "artists": [{"name": "Behemoth", "id": "UCkXmQ7w1rr63TL2So6Q2qLQ"}], "thumbnail": "https://yt3.googleusercontent.com/RTXm7nw4CA3KMUvhUTNoTfF0oTbxJMzlHCqAiz3JJaQSW0u_oBrB3okCjIYNpYo0xrLOwcI2FMtGUJo2=w60-h60-l90-rj", "explicit": false}, {"id": "Tl61RgFmURc", "title": "No Blade of Grass", "artists": [{"name": "Psycroptic", "id": "UCfv8nUHztGIEVGFR5-oGGjw"}], "thumbnail": "https://yt3.googleusercontent.com/gm2vJXqNmBTI0vMtaP0Rq0lw3gHVFpr-ITi0gKIv0ijsYLzWSd0iXXluyLX2HbX709AkjlOh6iXXscQ=w60-h60-l90-rj", "explicit": false}, {"id": "6y8PQPMdXIw", "title": "Parasitic Infestation", "artists": [{"name": "Across Oceans", "id": "UCOUs4joXKC_JJULm0sJgajw"}], "thumbnail": "https://yt3.googleusercontent.com/iM4JkwdqWMY0s2bqXmaF3RaOOndS-uZe5_w8ANfPolsIOsxKd64akz8IWQB3aFQ1f9dlRY48U_VEu8zh=w60-h60-l90-rj", "explicit": true}, {"id": "hqtxuZ6-XCY", "title": "Doubt Me", "artists": [{"name": "Beartooth", "id": "UCuRncPLFa5JDXq7S2uQyTrQ"}], "thumbnail": "https://yt3.googleusercontent.com/OI_6RDbWbwLMxowW70MbwRgTNzHDikbTNy5Bl1NxJqs5uucZ9KoJkhf4CFHqn9RmxDltKscdJEvwgZdX=w60-h60-l90-rj", "explicit": false}, {"id": "pZNeKadloiQ", "title": "How to Be Lonely", "artists": [{"name": "Like Moths To Flames", "id": "UCsmXGGuAT52Ltznzjx6RuGA"}], "thumbnail": "https://yt3.googleusercontent.com/zQSgx1BNuDDZM-Bf5beGqE3jxy2M46yoS_sk_brviGRRUizT5FKFC1oFcR44QWxF5nUMIUAK7F49OGMH=w60-h60-l90-rj", "explicit": true}, {"id": "yoVVDa442to", "title": "White Noise", "artists": [{"name": "Gradience", "id": "UCPASZyMn9XrxxZP47RvB9Dw"}], "thumbnail": "https://yt3.googleusercontent.com/BKfGQbGL4PRV_S9SzpzqUu7aakptAQ4qlKRUSqZKyKbRfchSEa7BoFNd_meWlMUU3xElwsuu4BZaQNJv=w60-h60-l90-rj", "explicit": true}, {"id": "UoTbOC2rYqY", "title": "Dehumanized", "artists": [{"name": "Bring Me The Horizon", "id": "UCGNMi-3h6Tx72EfbW6f2BxQ"}], "thumbnail": "https://yt3.googleusercontent.com/XFyK9kSwBaDM-uqB6NT2-p_pjLBowK7XSvqiHzGvYWHZo6nj6oMTMcXOVZpN74-RMxjSA_wh-lMiwke6wQ=w60-h60-l90-rj", "explicit": true}], "albums": [{"browseId": "MPREb_A5HaskSiHfx", "playlistId": "OLAK5uy_lEoNs8vBSemJoAjKRklCcOJsn0EHbO70U", "title": "Arktis.", "artists": [{"name": "Ihsahn", "id": "UCXa4vo4XUuPXA6YxSxC6_5Q"}], "thumbnail": "https://yt3.googleusercontent.com/ghng11g4rp4-10u3y9FUhm7uGL8GpiwNzZbnllxaE4oyQfRF_V7ImRu65Ho1myM5H7blodHrmRGX0_dU=w60-h60-l90-rj", "year": 2016, "explicit": false}, {"browseId": "MPREb_wIwoLCpZfqI", "playlistId": "OLAK5uy_nws3m6npBjRXG6Ku9WYf_YGvFz6187DxI", "title": "Enemy of the Sun", "artists": [{"name": "Neurosis", "id": "UCObiEsLgHY9dbcf5XxkQn2Q"}], "thumbnail": "https://yt3.googleusercontent.com/v5iHZAszwm0wvBCVfFYGEBtxNtdoqqSZhqfP9CJrWmUa5X0oncM1LD5ra5NH_d5QCRhx98XrYC_-Mpnd=w60-h60-l90-rj", "year": 1993, "explicit": false}, {"browseId": "MPREb_KtV3PgMEVJL", "playlistId": "OLAK5uy_nNutR-J9j_crO8GqSKSbEF-fQVGnPVBzE", "title": "Mutter", "artists": [{"name": "Rammstein", "id": "UCs6GGpd9zvsYghuYe0VDFUQ"}], "thumbnail": "https://yt3.googleusercontent.com/VzLpbmiJJz1FeC7qHpu7VLQkreNqZwDDIEVFRch-6Mikd3z0VDKcLDpLb_qRPKHkIRmrULJp-GiX_3Oc=w60-h60-s-l90-rj", "year": 2001, "explicit": false}, {"browseId": "MPREb_SXjncHxnAGz", "playlistId": "OLAK5uy_mDbTijylBDFxSzZ4nFWh-1aXnxVkiFpS4", "title": "Arise", "artists": [{"name": "Sepultura", "id": "UCZDm-NPbTrtTGa69EC2dFGw"}], "thumbnail": "https://yt3.googleusercontent.com/CXmpwHb4fmR7suaVboZ1C4LZBMc7erD7TlO5wJEEApbZAhIfACtKM91HJZBH5weLGt9Y7jb2cJbeojWE=w60-h60-l90-rj", "year": 1991, "explicit": false}, {"browseId": "MPREb_x2WfSYBRpr7", "playlistId": "OLAK5uy_mT9GyByZoc2G49XiCAjKH0be9HnxUiwxs", "title": "Jomsviking", "artists": [{"name": "Amon Amarth", "id": "UCOtqJNbDV4s4HI1h1YStWIw"}], "thumbnail": "https://yt3.googleusercontent.com/MxYvhiXDndGKQvRCHSIYUVTeTssyraiXCnV84BuU3eqn7rLpwePi9YmSFu4pSP7k7G1SJHCiwTPENh0a=w60-h60-l90-rj", "year": 2016, "explicit": false}], "playlists": [{"id": "RDCLAK5uy_m50Vt6DvR0QUO6YQXcQlH9fMifwz9f_i0", "title": "Música techno para darte vidilla", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/rnuQ6PEat6xw7AWA3rfti8a1PG_OX_wXffmtDXRxUnd0faoP8Le8tx7RWQF2iuwKN5L77fPy_VDaj0w=w544-h544-l90-rj"}, {"id": "RDCLAK5uy_kb7EBi6y3GrtJri4_ZH56Ms786DFEimbM", "title": "Lo-fi Loft", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/9I45SYZAfTeR17ByV9i2yDpWhQ5DTWMCrW3Lei8vMIiZzeyUnf2zRFUDBob4TGwgUkXAAR_x1Xa8VA=w544-h544-l90-rj"}, {"id": "UCGexNm_Kw4rdQjLxmpb2EKw", "title": "Master of Puppets", "author": {"name": "Metallica", "id": "UCGexNm_Kw4rdQjLxmpb2EKw"}, "thumbnail": "https://yt3.googleusercontent.com/wK5h2K63Ey5234foKQXFeY0zDJ-a53NOCY2DKcmATlaIbhFjawJ_oXUBT6-dhaCN8xgNreuXobcUsIU=w120-h120-l90-rj"}, {"id": "UCJls2FMEbRYxi28jcuKe2vA", "title": "Hail to the King", "author": {"name": "Avenged Sevenfold", "id": "UCJls2FMEbRYxi28jcuKe2vA"}, "thumbnail": "https://yt3.googleusercontent.com/IWHn1s-WN3CLPJTirW9W72RQLbLb00UexhGQJ5e4QlNQ7uD3FVpvHGHFhoghNslXOwaD91KGX-lgxLY=w120-h120-l90-rj"}], "artists": [{"id": "UCYjzkgiNzCLU_NlBAFflrAg", "title": "Anthrax", "thumbnail": "https://lh3.googleusercontent.com/X1sx8gUS0hHi9EBrzILLVpvZHp9GZ3NpkEGhNqndAyfKNgHhO4bw-wGfmlaN5JWwcO1QEUgd9VSHog=w120-h120-p-l90-rj"}, {"id": "UCkXmQ7w1rr63TL2So6Q2qLQ", "title": "Behemoth", "thumbnail": "https://lh3.googleusercontent.com/TL_UVBIPCuYhk9LtzRWxB9Dj8lz9JS_fvRKK5mAoVjq2nNm5V-OF-sP6D23yzws7fSkp2QQOWCPzSpY=w120-h120-p-l90-rj"}, {"id": "UCqLOZtEdRzXDzF-1CyJFc2Q", "title": "Russian Circles", "thumbnail": "https://yt3.googleusercontent.com/p48biKfBT1zwCG_ZkEDWYipBmUMU3IXAaHfU52dXMKmKvGjGXn0jidPdg5YukIP38leEep-Mu3_h0V0=w120-h120-p-l90-rj"}, {"id": "UCGNMi-3h6Tx72EfbW6f2BxQ", "title": "Bring Me the Horizon", "thumbnail": "https://yt3.googleusercontent.com/YgsyFk3KZHvI5cYtcGOcWYwbpu_GJc-IbPeWyF_Xzy2JIDwL1cPefog1szowNlvuIJ-_OkpoyWOYxhg=w120-h120-p-l90-rj"}, {"id": "UCGexNm_Kw4rdQjLxmpb2EKw", "title": "Metallica", "thumbnail": "https://yt3.googleusercontent.com/VqJfYEB0WmZ8sQ4VIP-hlrZYqsOs12NN-VtCFcTFOi9XRqbiZ4Zhb0JZ2d3-fzIctoMR7raDHFeyNoYa=w120-h120-p-l90-rj"}, {"id": "UCfv8nUHztGIEVGFR5-oGGjw", "title": "Psycroptic", "thumbnail": "https://yt3.ggpht.com/ytc/AIdro_ndr4_TIIgY_Jr5YBpTRQSoXxuH9Td5QrvGJaOf2SZXP74=w120-h120-l90-rj"}]}, "Motivación": {"songs": [], "albums": [], "playlists": [{"id": "RDCLAK5uy_lRQbFalJOe45Qa-ERq9tTVUIv6WZFW_WA", "title": "Clásicos de la Salsa", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/K-ywNc7QSJRLjMw5hRPFjCqB8L3qKuwhuM1rJSZN8mqjoDAZB7msE4hw7faZSbQcRWt6ysMEAUk0WQ=w544-h544-l90-rj"}], "artists": [{"id": "UCiY3z8HAGD6BlSNKVn2kSvQ", "title": "Bad Bunny", "thumbnail": "https://yt3.googleusercontent.com/ploU_4iWpDoJlX3FOlwwd_yQcex0I8A0_665lePXAEBbNp1zn5g42eNwg5Q7lvYc2mG2--UNIYcIhww=w120-h120-p-l90-rj"}, {"id": "UC1cYfQGC1eXrSO-hxMd7ESQ", "title": "Chappell Roan", "thumbnail": "https://yt3.googleusercontent.com/1FjS7kPf3kZc0Q2X5ZqhprU6jZkXCFgv4xrKn8o3RzBMgij9X4dNPTuUgLOgFxCYsnWUdrAx0CqOW6hw=w120-h120-p-l90-rj"}, {"id": "UCo1DYcm1IZ9v3UPkpiAcgtg", "title": "Tyler, The Creator", "thumbnail": "https://lh3.googleusercontent.com/huj5tS6L9deNGjgu6RYFZ24tL1YZFHEs84VOOvBIbIQlwEBRF-rqvI6Tom0NA-EFhmN4MBPDrPYLvmA=w120-h120-p-l90-rj"}, {"id": "UCPC0L1d253x-KuMNwa05TpA", "title": "Taylor Swift", "thumbnail": "https://yt3.googleusercontent.com/RCpTA6EXJQyjVFDosWOKa2SMmqkua_lA9mHPDWWciLwgqpZLz-k8rXWRF_367trrQ7up9BUwCbk6kRk=w120-h120-p-l90-rj"}, {"id": "UCERrDZ8oN0U_n9MphMKERcg", "title": "Billie Eilish", "thumbnail": "https://lh3.googleusercontent.com/tQC4rOL6xz6FhmFr0ggQExxyGbYSOsyveXVSnPBh2WjEyIzQ9pMHablLJ-0GlMBrLBlBrbWQGmzrV6KN=w120-h120-p-l90-rj"}, {"id": "UCz51ZodJbYUNfkdPHOjJKKw", "title": "Sabrina Carpenter", "thumbnail": "https://lh3.googleusercontent.com/FMh1mOI0ufvUCAkbUM6aUmU5WK7O5PnndyyXKP1-DCEip20SQz5eeYn3lZ29p-ASb-19ZfBVc_NKe5Ko=w120-h120-p-l90-rj"}]}, "K-pop": {"songs": [], "albums": [{"browseId": "MPREb_WOciJ8HA7tx", "playlistId": "OLAK5uy_luFMOW_sxoO68xckOcm-uJOIPFquAZpj4", "title": "Palette", "artists": [{"name": "IU", "id": "UCTUR0sVEkD8T5MlSHqgaI_Q"}], "thumbnail": "https://yt3.googleusercontent.com/COFBRD6GDIkwnlWYON62L4UwGdvKjw_xAsID5YeI66lpSrM6-Az-WzfTyRFgfGnu5OsL9IWRa8DMU8nr=w60-h60-l90-rj", "year": 2017, "explicit": false}, {"browseId": "MPREb_nzaw1wfaqXY", "playlistId": "OLAK5uy_nxrZ6Dt3U4lDZXL1CDz2wrleuxJdF5po4", "title": "The 1st Album 'XOXO (Kiss&Hug)' Repackage", "artists": [{"name": "EXO", "id": "UCEUX9tUYqTFfPQdAgVNsKTA"}], "thumbnail": "https://yt3.googleusercontent.com/LwLoJUUVpdk7QU0BKzjHAoMiDxX1hadM9idh6MBKPTgJJoqFBPI1IXEofWtyCssl01XlUG9jPCIq1Dc=w60-h60-l90-rj", "year": 2013, "explicit": false}, {"browseId": "MPREb_YiewU7I39qR", "playlistId": "OLAK5uy_motEDJVM-TiGZ4Q1YBuQt39LXzPJ0yOuI", "title": "LOVE YOURSELF 結 'Answer'", "artists": [{"name": "BTS", "id": "UC9vrvNSL3xcWGSkV86REBSg"}], "thumbnail": "https://yt3.googleusercontent.com/-dnQo6KIRWKvUD2L_uLuxYQ-YJfpmdOfuQ77tFBdoNM_1_6N_Wagm_aJMRuIOdXxCqg0wA7hAVuI0M9A=w60-h60-l90-rj", "year": 2018, "explicit": false}, {"browseId": "MPREb_GakNi5Ht7Du", "playlistId": "OLAK5uy_miY7Yu_c4H3-pHCGLwfb4WXdXXg-yFqzM", "title": "Map of the Soul: 7", "artists": [{"name": "BTS", "id": "UC9vrvNSL3xcWGSkV86REBSg"}], "thumbnail": "https://yt3.googleusercontent.com/TwlGKVdZKQoUEA1A7ZwE4X5VUGliaSWcpXDAUaAHUmkb04IBq63PARb8jzH2nr0nC6TaB_O_hG2lB7wi=w60-h60-l90-rj", "year": 2020, "explicit": false}, {"browseId": "MPREb_5akfGIsrUrJ", "playlistId": "OLAK5uy_m1OQNnyJAlg50n3C_TywdGIbF7fki4PmA", "title": "THE ALBUM", "artists": [{"name": "BLACKPINK", "id": "UCkbbMCA40i18i7UdjayMPAg"}], "thumbnail": "https://yt3.googleusercontent.com/ZKINUFCtfMUKB4CH4wJcQyHi6-bmuHz2exRBV6SORUd96KsC1jo2nrDImg2zlLnvHgJPGBxreYVjunGycg=w60-h60-l90-rj", "year": 2020, "explicit": false}, {"browseId": "MPREb_zKAh1TGgEOx", "playlistId": "OLAK5uy_nOP2wHatmCZbGJg4URCQV66iWtGvyVmSk", "title": "PAGE TWO", "artists": [{"name": "TWICE", "id": "UCAq0pFGa2w9SjxOq0ZxKVIw"}], "thumbnail": "https://yt3.googleusercontent.com/cLnmZ8ZAIgUaMSKnqqQUIqJukZhNLoRQIFIkWFTt5tdOsGhPI1DhflnFn4A9bjOE2cU2OxsWK8Y0NMcB=w60-h60-l90-rj", "year": 2016, "explicit": false}], "playlists": [{"id": "RDCLAK5uy_n41V9iqmjro6caBDuDD1E4eWs5yTb5_OY", "title": "Lo-Fi de Bollywood", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/MAUu7Aq7JEEvHZ8mWYIVAtBV-ktRJTvvtgq6yoZoooxqPRuoWChYKk66JLbJgeqdSlzcIhOF0_gPntU=w544-h544-l90-rj"}, {"id": "RDCLAK5uy_nv-tvtQSHjI9sEnN3M6QgK3m5JeaE-6zE", "title": "K-Pop Hits 2025", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/31EekWjPWpJaie1UT7yN91_k8T6Aip3HKRY78LlX372RD-Ij80v0MoFWX76gqkTv97H3HJTd_BwzWw=w544-h544-l90-rj"}], "artists": [{"id": "UCgY89NTMGGuTzEmdOabjOYg", "title": "KickFlip", "thumbnail": "https://yt3.googleusercontent.com/3bW1D7ftwKDkQzsulAmGECd0Mk3Pxt432HuvUvioNe3qhC2ah5w3alXaB4peehqlJlK2cnbA=w120-h120-l90-rj"}, {"id": "UCraxhuo4vXXMMgXJ6Gil4Eg", "title": "CORTIS", "thumbnail": "https://yt3.googleusercontent.com/2IbO6drTOO1y5I4tDPqS5nafZmV0Z54g0YxU32RnLjc0vvYlzH1-c522X0c784WaR9hs9YIDr9yhHKfT=w120-h120-p-l90-rj"}, {"id": "UCA5EhtwgsPi-qhSuFWCWRSQ", "title": "AtHeart", "thumbnail": "https://yt3.googleusercontent.com/L2YWkHgJkNXvwtpHI6uv6O6aIdg9VAGgz8mCREPlaDIkufNLOMEVjwrQKh-mJjrgmOUg-zmRbg=w120-h120-l90-rj"}, {"id": "UCL2Iir3iWo3SA6O3PuVjFWw", "title": "ALLDAY PROJECT", "thumbnail": "https://lh3.googleusercontent.com/r1qNPc3rpDMU_KwqNsu_weCr60qJRcb4_udJaJYATDNG2MkxzjRj4CES1b3AIygky-SIjBsBmhMHyOk=w120-h120-p-l90-rj"}, {"id": "UCNVIo0UJXTdEnsRkCB9Q03w", "title": "ILLIT", "thumbnail": "https://yt3.googleusercontent.com/5GYeWpGUtgi8tPkwqV_Nnq9ZiE6PhSCoRSrfpK1CIdfZkndV5fLHZ-c2g9rAGfdQ6bV81Wvlc8UMjw=w120-h120-p-l90-rj"}, {"id": "UCI4WdEYwhO2h_NXd9mvrQlg", "title": "MEOVV", "thumbnail": "https://yt3.googleusercontent.com/uvn9WRH71wX0gY1zMnSw-wiCRAVdlxtcugtfl475teAx7-KFckOe9GWSssCOvNbBBHDKMsGpNEQ4XyY=w120-h120-p-l90-rj"}]}, "Décadas": {"songs": [], "albums": [], "playlists": [], "artists": []}, "Indie": {"songs": [{"id": "QTzo6MtJgzo", "title": "outta time", "artists": [{"name": "Kelela", "id": "UC_ouv6WbUBzKtDAdMJG1PsQ"}, {"name": "y", "id": null}, {"name": "A. K. Paul", "id": "UCHkZnDCi_QI6kzofEWc-lBQ"}], "thumbnail": "https://yt3.googleusercontent.com/iTpX39xgJrS6YK91VX8dwuFxmdsTe8DWwSClw6mDPvabyjZh0tHKbJiqFoOf5ezAoSFletxW1ddAU1BU=w60-h60-l90-rj", "explicit": true}, {"id": "zS6pQCizo8k", "title": "Sun Has Set", "artists": [{"name": "beabadoobee", "id": "UCcGMuu89vageEKV8zUKhwdA"}], "thumbnail": "https://yt3.googleusercontent.com/QyJQUFZOCdqc2DplTlxQrTZBdpJdx86fzaiUWGT8pf4-VsTx4NAHbj2143RtJdcu6QciIL9UZJSv8xyr=w60-h60-l90-rj", "explicit": true}, {"id": "51gmIdUxPZg", "title": "Lost Boys", "artists": [{"name": "Phoebe Bridgers", "id": "UCprIJgC2-qqSHNVztFjRFmw"}], "thumbnail": "https://yt3.googleusercontent.com/9qW1teT_fVCBPqWNmOGYK61tnecNApgB716zONmYtpJSHBlYedmFAz9rrZOh4V19SF8JktqY-L3rjf5G3g=w60-h60-l90-rj", "explicit": false}, {"id": "iG3Yu1x230U", "title": "Get Away From Me (I Think I'll Love You Soon)", "artists": [{"name": "Julia Jacklin", "id": "UCypJMo2Ed63_4gvbtwkrgVw"}], "thumbnail": "https://yt3.googleusercontent.com/Nb5bqko-dtgEO3htHowPini13IRTOGCT6dq4KD01qxwKU81DdS3LTzibcOk-u3EXt7xq8HxIpL-5EmCZ=w60-h60-l90-rj", "explicit": false}, {"id": "rpHYQQQKmos", "title": "Billy Came Back", "artists": [{"name": "This Is Lorelei", "id": "UCvL0aZMhBZ8FR2vAofqQmVw"}], "thumbnail": "https://yt3.googleusercontent.com/Yy884uTfmekkEDzSJYNy8vzeo9oSbfjgsVrQtTL6oOO_hgurSVt4UIdDB7EuFczezpxYZzf8j0e7Q80=w60-h60-l90-rj", "explicit": false}, {"id": "fr8_S4ulApg", "title": "Baby Steps", "artists": [{"name": "Quadeca", "id": "UCvvnkLlBfbvkP03UHbkIeFA"}], "thumbnail": "https://yt3.googleusercontent.com/xbS40OfPRnVkSKi5WBWUt2wsNHNadTBVY6pmqaQUGY36fDPJlGGfDujEbSzC_MtMFRe2n3iSP0qPshTyFQ=w60-h60-l90-rj", "explicit": false}, {"id": "1Fzc54S2ckI", "title": "Essex_Honey.mp3", "artists": [{"name": "Blood Orange", "id": "UCwItHQ6AZ1ZH4SVNUCrpSPw"}], "thumbnail": "https://yt3.googleusercontent.com/hhS15Q1BKnCSw0szretj5hdspdo_i_2STq8TP6BrJE1m-UtiVOk_IrLjjNab_4uCkdjt6_LGEtSsYFkX=w60-h60-l90-rj", "explicit": false}, {"id": "VrV8l7EuAU0", "title": "Yellow Sun", "artists": [{"name": "Downtown Boys", "id": "UCnFJGY5md_Ic1QwNFbhkZDg"}], "thumbnail": "https://yt3.googleusercontent.com/P9r7I38JzzCsH8aB-HS8XvG6i6M3qLTwer1gc0fUUchE-jC3l6MMPC64hRJqAvCGxs6v9ewfTdKU_iuzoQ=w60-h60-l90-rj", "explicit": false}], "albums": [{"browseId": "MPREb_kx1oMPzTow1", "playlistId": "OLAK5uy_lSHNpESYwefXdb7nPj9hn5PwaBC-bCLro", "title": "Bakesale: Deluxe Edition", "artists": [{"name": "Sebadoh", "id": "UCju-EvozsD8gGa6ggnmwbkg"}], "thumbnail": "https://yt3.googleusercontent.com/3je96eIj0hVuECCzvLc6WImCXlZ4yTwO9UM8RBXHT_sVXroXhiXdwOUv3ijXBx4m5m4aLh47eh6DgOo=w60-h60-l90-rj", "year": 1994, "explicit": false}, {"browseId": "MPREb_U0dFxPnL2hP", "playlistId": "OLAK5uy_k1t5EEG2RSjrddBdTZfW7MKbJ3ZbFsVwo", "title": "Now Here Is Nowhere", "artists": [{"name": "Secret Machines", "id": "UCOfBob1IhrdltMWumwwLayQ"}], "thumbnail": "https://yt3.googleusercontent.com/gMT5WfcwI33gNPmloYjIMVKOT3qxkhPtaC3ayre_Q7NqhJw1er_vBCXPINi8PhuprXyMWTs85PG9pAo=w60-h60-l90-rj", "year": 2004, "explicit": false}, {"browseId": "MPREb_ZUJWiFkEvta", "playlistId": "OLAK5uy_lra1rJP3eNXqK9WyFeD7HJ7s67wyzEciM", "title": "Bows + Arrows", "artists": [{"name": "The Walkmen", "id": "UCnaedb1G9lOh40XYkqjC1Zg"}], "thumbnail": "https://yt3.googleusercontent.com/MRwPxVszHYgTi6NyFOPI_R-OYJxA-e2_Oy_Hcs_9GnPvlHccoyS4jGRHwp5c8dswzFEJTI-j_hVmI_1z=w60-h60-l90-rj", "year": 2004, "explicit": false}, {"browseId": "MPREb_8fLFwsN1VQa", "playlistId": "OLAK5uy_nSBaUFePtDVM06assJEJr3M5efdSAaV3c", "title": "Punisher", "artists": [{"name": "Phoebe Bridgers", "id": "UCprIJgC2-qqSHNVztFjRFmw"}], "thumbnail": "https://yt3.googleusercontent.com/VY0wwAXUICe3lb2nQu7urfjOWiGwoFW1W45HzhwH77Qns7KV5cQHIIP_U5Kt99DPGFYc1zSmtbN-XMXW=w60-h60-l90-rj", "year": 2020, "explicit": false}, {"browseId": "MPREb_iASge95R8rP", "playlistId": "OLAK5uy_kBoWR-flloEuwQ6YRu4Op2QhrB48AZTwI", "title": "I Could Live In Hope", "artists": [{"name": "Low", "id": "UC3IFlr-hvNN5Ex3vZcj8AAA"}], "thumbnail": "https://yt3.googleusercontent.com/5e9xCdpS0o164Jh7WLYxfeQxCLK3iq1DmVypbH0Z1biX3tzUdbihvLc56HJ0ut7YSVCT1Z_byHhDP4U5ag=w60-h60-l90-rj", "year": 1994, "explicit": false}, {"browseId": "MPREb_hcXnen4eYhp", "playlistId": "OLAK5uy_mZUEak7-hPO72EIvbYvA5Ls-zdsZQ_Faw", "title": "Alvvays", "artists": [{"name": "Alvvays", "id": "UCijE8N5dNSqbRkhOcmsxWKw"}], "thumbnail": "https://yt3.googleusercontent.com/VqW504ALwdbaB7spOyKAam9qb1v1fqC2cS4ZO_DW4ZgKZkzDpSzL5uOQPKcnhyRJmG2KmPvMY0swM6NM=w60-h60-l90-rj", "year": 2014, "explicit": false}], "playlists": [{"id": "RDCLAK5uy_lRJL69-EgXkWeTAXA3v__oMk-3IWf2NA0", "title": "Música Indie Coreana en el Sangsu-dong Cafe", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/-xYvXir_VMTO7L6UgZfhyKHXFZ8zURAiJh0NBc2LdkrZWIwCRKCAtwOyBgo3nr8IcV_pEy5Z3pAirA=w544-h544-l90-rj"}, {"id": "RDCLAK5uy_kb7EBi6y3GrtJri4_ZH56Ms786DFEimbM", "title": "Lo-fi Loft", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/9I45SYZAfTeR17ByV9i2yDpWhQ5DTWMCrW3Lei8vMIiZzeyUnf2zRFUDBob4TGwgUkXAAR_x1Xa8VA=w544-h544-l90-rj"}], "artists": [{"id": "UCvvnkLlBfbvkP03UHbkIeFA", "title": "Quadeca", "thumbnail": "https://yt3.googleusercontent.com/14ZH7eaxQlUdrsmLiZU8mYolVKYvK-Xqqh8DCD_qvnp5Hx-TK3jFlpQosqSxtdgy71EvmRM92g=w120-h120-l90-rj"}, {"id": "UCQD-OLpEkxUrCjZaaY48D9A", "title": "PJ Harvey", "thumbnail": "https://lh3.googleusercontent.com/ee-NgfteAe-mslGW92sOGIhS1o8JAdJDAGJSlbHBCQKy91FBo-rhO_-mxuuwTeQeFJmyW6DiNYYIr1M=w120-h120-p-l90-rj"}, {"id": "UCiQ979Y__qwtNlEw71JEyow", "title": "Ibeyi", "thumbnail": "https://yt3.googleusercontent.com/0GkV3KDw4qGk51_2T2BlP8zmBmSTLmActozxqU2htI3AvN_WFL8R5tnD0dLlx2QxtFqqiO6Kxr8qlWs=w120-h120-p-l90-rj"}, {"id": "UCnFJGY5md_Ic1QwNFbhkZDg", "title": "Downtown Boys", "thumbnail": "https://yt3.googleusercontent.com/1t_2vVefS9vaXep8iHZ0fhr5zXQYo4hNtjlFjWtWSHatuidiAxiV_EO1SEPCP2uz_qIfVArhmCu6l34=w120-h120-p-l90-rj"}, {"id": "UC0IFTAQBOS3VMwD1QKRDLJQ", "title": "PUP", "thumbnail": "https://lh3.googleusercontent.com/HogCkwgp_OT-wJzH-fLZd42e31NOdOFoZxFb1GKvmwK1R76hIHISae82J2AiPKxsG1ChwxSvHnTomXA=w120-h120-p-l90-rj"}, {"id": "UCbqms5xGww-H0YCwcT4f9iw", "title": "Grumpy O Sheep", "thumbnail": "https://yt3.googleusercontent.com/kJ2Fuu2RdCJjuf5WOjzRS0HUSA_Pwrcggz8hK8OBG2vJsoS3ytc2ZfT-g71t7LLJvaNHPxQWeDWlH2k=w120-h120-l90-rj"}]}, "Música cristiana": {"songs": [{"id": "bRqpv0N14dQ", "title": "SOMEBODY PRAYED", "artists": [{"name": "Forrest Frank", "id": "UC7K7BLJbNXkLi1Chz8X2zJQ"}, {"name": "y", "id": null}, {"name": "Tate Butts", "id": "UCtEi9wOWhmzjIjhkLBpkzRQ"}], "thumbnail": "https://yt3.googleusercontent.com/HfGCQSKAKU5bkOmWrLQRNWqUQ-zmcSWtpAiDvqJ_299VdBQphJoQJKOi_lHkTukjUyE0fAD_ihyP4hEY=w60-h60-l90-rj", "explicit": false}, {"id": "6zqzKE57G88", "title": "Living Room Floor", "artists": [{"name": "Leanna Crawford", "id": "UCVVDHy2nUUAP18_f4MrnuYw"}], "thumbnail": "https://yt3.googleusercontent.com/qi-M4zsih8mUMS3tjhzoIIcIk-w_aFYrWNHZ6H-kWtbWyG3RS06Hex4bPtV6vKQN43bWutgLlMaABp7N=w60-h60-l90-rj", "explicit": false}, {"id": "Tv1mC0NrFTQ", "title": "The Lord Is Here", "artists": [{"name": "Tiffany Hudson", "id": "UCHia3TQjYgrNMIjKR-PylXQ"}], "thumbnail": "https://yt3.googleusercontent.com/UuAjSXewG25lDw0ahgnlAQygzHs_PzZaKc1gBd3xVsdwAhIbaCU3yd9_ob-yBrLHaktHym3MmHC65apB=w60-h60-l90-rj", "explicit": false}, {"id": "rzvcyw1rsAM", "title": "Revival", "artists": [{"name": "Jeremy Camp", "id": "UCwiBQ2170yRpNVSGjA0579w"}], "thumbnail": "https://yt3.googleusercontent.com/AX_Egr5eQ1GmgbqXQbR_D3WVLNqTcufq5FZQ3MB4JAUHVI3PPn5kqzNYxj-QBLzZcB96k2HYhe_1Ad4=w60-h60-l90-rj", "explicit": false}, {"id": "ZUVvJ6Kk43Q", "title": "In On It", "artists": [{"name": "Gabby Barrett", "id": "UCsYKB_bDQYEWVopjS64mwog"}], "thumbnail": "https://yt3.googleusercontent.com/Ug_fEcVXea0opwf_nIkROxEdAkgiUU6PjbgP4snB0D4zF0LEUIkfjbS79phogsIeaAL8rrgtF8-r5mz5=w60-h60-l90-rj", "explicit": false}, {"id": "zYgd2PyDToc", "title": "Breathe On It (Live)", "artists": [{"name": "Tauren Wells", "id": "UCTIX5HBty3k_j4DfSu3NbBQ"}], "thumbnail": "https://yt3.googleusercontent.com/nIQCRLdEhkZmmSdXlsAvSUMcCNCggiOP2cczIenVhGDJM-cvj_D6lz9rvHeRu5bX29bscyDukS4udF7Q=w60-h60-l90-rj", "explicit": false}, {"id": "qrge-AYb-R8", "title": "Somebody", "artists": [{"name": "116", "id": "UCuOkRMgO5GkToA4hNFSrflA"}, {"name": "CèJae", "id": "UCHu513IAOAcvJJs5-7s1Tjg"}, {"name": "y", "id": null}, {"name": "Anike", "id": "UCV7gx72W-pGLf3WjIKQ67bA"}], "thumbnail": "https://yt3.googleusercontent.com/FD0J2Xps_RxT-iRX5BIeLyFF2pcvYlYjI6pujDmiSmvWqXKGw-QvXyN8-r1_NWx1OHAoyHrNljdOg6Sq=w60-h60-l90-rj", "explicit": false}, {"id": "Esmu1LbNX4g", "title": "Everything and More", "artists": [{"name": "CalledOut Music", "id": "UCK0PsYlMoZgbTDPB6YnRwjw"}], "thumbnail": "https://yt3.googleusercontent.com/6W87WcBH_jHmp_obKheSa0NCa8LUOToWlYUOImcKv4vFBi27P91udux7AsbPX9dJJb1H2vTlLMiYYdYh=w60-h60-l90-rj", "explicit": false}], "albums": [{"browseId": "MPREb_1YOvTlgyI50", "playlistId": "OLAK5uy_kZkyjwik045dwksItDuIQ61E6msatW0aA", "title": "The Beautiful Letdown (Deluxe Version)", "artists": [{"name": "Switchfoot", "id": "UCOGpHC8coci-12OIMkPsopw"}], "thumbnail": "https://yt3.googleusercontent.com/J040pTWWi2qfhyKHut8XC0yvoUG80ftxD2EEE_Ejzdr5U5yBMpdaaV15O2u_W6ZvUW2hlDM7WHMQgKtK=w60-h60-l90-rj", "year": 2003, "explicit": false}, {"browseId": "MPREb_zDNyQyFyCyT", "playlistId": "OLAK5uy_l9oXNtgki0Qwwn2831viQyGT-CaL153p4", "title": "How Can It Be", "artists": [{"name": "Lauren Daigle", "id": "UCYxcuspkcCZj8ApIlm1y4Bw"}], "thumbnail": "https://yt3.googleusercontent.com/2yc9luzIwPxu83zu-tezE20eCW2Z9CI6bxFupK5S5jZ0Yp7eBX2sXZj2FfjyBGw-Ce9QyQCs_70HrX4=w60-h60-l90-rj", "year": 2015, "explicit": false}, {"browseId": "MPREb_2aTPY65y0lT", "playlistId": "OLAK5uy_n7Yu8xYr8_8bLGqOJ60kbTg7Rk9He1rsY", "title": "Jars Of Clay", "artists": [{"name": "Jars Of Clay", "id": "UCa7Za5s4XtxyS1Q4xGnorlA"}], "thumbnail": "https://yt3.googleusercontent.com/hdqbj19WnlVZg96c2Ye53LI90x1SQ5d3iK-s8LEHY4wl7ihEl7KIJQlhXPQkHns_dCXKXQrPJeU2NAs=w60-h60-s-l90-rj", "year": 1995, "explicit": false}, {"browseId": "MPREb_9tEOhFkWkey", "playlistId": "OLAK5uy_mcY9euTstXZM2AkyaTohGNEVC4DG3AGHw", "title": "Disappear", "artists": [{"name": "PFR", "id": "UC2lb-pR3dpkyOye_puhqb_g"}], "thumbnail": "https://yt3.googleusercontent.com/fP0-Meb1MFl32w9MswZk1ne1WyCRZW4HGOmHdPXBGDSTWYVRF8Lj0DkB23kjfeJRkr0WPK3uztA8ggA=w60-h60-l90-rj", "year": 2026, "explicit": false}, {"browseId": "MPREb_H7AMEg9CICV", "playlistId": "OLAK5uy_kWz8PsRqQqshQu15G3Sv_RrqmvI_JHDUA", "title": "Arriving", "artists": [{"name": "Chris Tomlin", "id": "UCv5dVQlpnacBTFboNz4NnyQ"}], "thumbnail": "https://yt3.googleusercontent.com/KjIxhbVO9SdyORV7A6stHqC9YKaZgXXMOV_XWmG5DkKVLgYwTHcPpZ0w9BTMV6EQ0Y34rSDtJdo8sltu=w60-h60-l90-rj", "year": 2004, "explicit": false}, {"browseId": "MPREb_ZRNr8dnrZkq", "playlistId": "OLAK5uy_lbG7a3nlETvOUPEIUI3pkziAXz0O3Ml98", "title": "The Great Adventure", "artists": [{"name": "Steven Curtis Chapman", "id": "UCaDf3HhVharAYCCejjy1EEQ"}], "thumbnail": "https://yt3.googleusercontent.com/Nfg9rJ5mnqCfAtQvxZ_bE5RdGoGwC83f1ejCpOsZvAZg7AI6ZJf5Ous4Nhfr_EA1xG6QgoAKN2cO_es=w60-h60-l90-rj", "year": 1992, "explicit": false}], "playlists": [{"id": "UCJNq4kn1Q0xVpCAw0bjEbhQ", "title": "Quiero Conocer A Jesús (Yeshua) / Nuestro Dios", "author": {"name": "Generación 12", "id": "UCJNq4kn1Q0xVpCAw0bjEbhQ"}, "thumbnail": "https://yt3.googleusercontent.com/fcir0LFc5v7B-vUMIgiXOedR2Ra42stp0wlJe_6wszxH2j-7iy5CiCTYY6u3ZIX7tnIlkY7u2hJHI167=w120-h120-l90-rj"}, {"id": "RDCLAK5uy_n8URvMaXx9-hhGR-PQKwSYSbkakkkyWpM", "title": "Canciones para Papá", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/hcIJnNgYS6Pq3nvk9CYtJCh1O1LwGL7rX6MM80MtddF3-dnzwW2dN6yu2t5B4Jbh0GqGDSYTb9Cvniw=w544-h544-l90-rj"}], "artists": [{"id": "UCh3eCMwMV8jxtU9nV692SVg", "title": "Anne Wilson", "thumbnail": "https://yt3.googleusercontent.com/ZNsOWfYz3RX6avUbw4NRizLcfHFAr2E1971cLyfJD1OaWntv4-MzdkIJ7_78XsM2yzu_KAGMX76Tax0=w120-h120-p-l90-rj"}, {"id": "UCZJaE8aA58tLmMBF749F19Q", "title": "Brandon Lake", "thumbnail": "https://lh3.googleusercontent.com/pUnSXywD2NPVPBiLio48ryaCmbyibR3FYPjT_BwtxRgccDFtDAT3FlnkBC53FeYbuRUSCzwLIhrXDPA=w120-h120-p-l90-rj"}, {"id": "UCv5dVQlpnacBTFboNz4NnyQ", "title": "Chris Tomlin", "thumbnail": "https://yt3.googleusercontent.com/WyRIMRsS6VfTwv1NaAuY9uBeK7Xx5oXjlGHy6TBnVC8AcJRPugl1KJ3rKtG0hVm3lvXRWgQ-PsvdZUU=w120-h120-p-l90-rj"}, {"id": "UC7K7BLJbNXkLi1Chz8X2zJQ", "title": "Forrest Frank", "thumbnail": "https://lh3.googleusercontent.com/u6-z0d1vrSUYKfm2e2eJbDbCZeqaob2J1O7291wmrO52lOA3fz8c1rd7igbBHXsL-JUWADXbMyZ5jTg=w120-h120-p-l90-rj"}, {"id": "UC6qyi6LqMfxyPf8UrIYwANg", "title": "Elevation Worship", "thumbnail": "https://lh3.googleusercontent.com/aoABYdsFcAvelX3aUkCw01autIih21Ol3nqu5lfqp1JRF0sS_RhToPFRsyUAtcyEbVq7OCd6yPfPdw=w120-h120-p-l90-rj"}, {"id": "UCpUGMpTEADS3dSY8DVQ6jgg", "title": "Kirk Franklin", "thumbnail": "https://yt3.googleusercontent.com/7y0kXA4BREnCkOP_BzqSTj-fPP7-5wg6Z7k7EefQhixHIp-Vt8Ydkibq8v2pmp1d2QyxjyI-Z2x2ciQ=w120-h120-p-l90-rj"}]}, "Cine, TV y teatro": {"songs": [], "albums": [{"browseId": "MPREb_jWXPqaAc6KM", "playlistId": "OLAK5uy_mNp9cMWgar4DaYHBoVwSRtxDt5jpasNuY", "title": "KPop Demon Hunters (Soundtrack from the Netflix Film / Deluxe Version)", "artists": [{"name": "KPop Demon Hunters Cast", "id": "UCuCy0Nk0SoKPyioObCZwgSw"}, {"name": "HUNTR/X", "id": "UCB-w1qXuClYVjJV49lVothw"}, {"name": "y", "id": null}, {"name": "Saja Boys", "id": "UCIz8Vvt9ux6x7Ryi2sqG1ww"}], "thumbnail": "https://yt3.googleusercontent.com/WQ5-vpqWEKPunzYVV03bqvod7up-9qSa67ZHIVmcZNDjqyQY1l2VdrTVMidNDrAgxJzThrNkWUzG6cyb=w60-h60-l90-rj", "year": 2025, "explicit": false}, {"browseId": "MPREb_IAPLi0cs4pY", "playlistId": "OLAK5uy_nwBWblTL7oYG_GhBFXww1GcdouKStdaoY", "title": "Wicked: The Soundtrack", "artists": [{"name": "Wicked Movie Cast", "id": "UCjbnhIU-V8Q04QzBifEoW6w"}, {"name": "Cynthia Erivo", "id": "UC8eIfMcWxFajB3BN5ZCkUxw"}, {"name": "Ariana Grande", "id": "UC0076UMUgEng8HORUw_MYHA"}, {"name": "y", "id": null}, {"name": "Stephen Schwartz", "id": "UCVcwtD41eHBi79UbYpTn7kg"}], "thumbnail": "https://yt3.googleusercontent.com/m7OWjkpi8RMZRumIccZ1j2W0UUzShW06Keo6CSPicCxYv3pdXu45QA4Xu3jTCIvDfMuQCptmMyHPbxw=w60-h60-l90-rj", "year": 2024, "explicit": false}, {"browseId": "MPREb_A1glXezFh3t", "playlistId": "OLAK5uy_nXpZeK8l3tUINsfPYg2vZbFc3WnXTLkTM", "title": "Moana (Original Motion Picture Soundtrack/Deluxe Edition)", "artists": [{"name": "Lin-Manuel Miranda", "id": "UCAFIersvPYEQzT7XPRWUj8g"}, {"name": "Mark Mancina", "id": "UCFRNnH-5pL12aTPQOQZ77Bw"}, {"name": "y", "id": null}, {"name": "Disney", "id": "UC0L4FNvqduCM49XPjb8dZHQ"}], "thumbnail": "https://yt3.googleusercontent.com/JMDYExSuxByKqAOUVQUPuuFMz10TPa9qtPB12ea5a1KqElpcihF20-YHCKoJdExx3s00p9y3PzICmz8=w60-h60-l90-rj", "year": 2016, "explicit": false}, {"browseId": "MPREb_TWFEv6Y7vCf", "playlistId": "OLAK5uy_lInVpvsoll6f0Bv4hyPsFZvtyVuc0MpS8", "title": "Purple Rain (Deluxe Expanded Edition)", "artists": [{"name": "Prince", "id": "UCPIZb3A7k6zLefVNtaAmvGg"}], "thumbnail": "https://yt3.googleusercontent.com/SCE6YCgDRJAR6zbrwj3Mpv_1uLtAOEiKuE3JL3zB8-7Wua0lFnksBdUrzWjgg9e-xjtRHx3gp0WXrwmO=w60-h60-l90-rj", "year": 1984, "explicit": false}, {"browseId": "MPREb_UcUQjrttm08", "playlistId": "OLAK5uy_kacAxlvKi-f9JmxNFFePfIM8dOjeh68AY", "title": "The Sound Of Music (50th Anniversary Edition)", "artists": [{"name": "Rodgers & Hammerstein", "id": "UCLdgzfms_jXMSQ6pB6evM4w"}, {"name": "y", "id": null}, {"name": "Julie Andrews", "id": "UChHLaKOrD9WHjksMx0u7wdQ"}], "thumbnail": "https://yt3.googleusercontent.com/PwZWZGK82OiSPFZXnmraAfaJFvkRfz8a91brp4DpkPxzHUxCCzwagIOLnUZ6HsARXbJRxrsHmD2LtTFydw=w60-h60-l90-rj", "year": 2015, "explicit": false}], "playlists": [{"id": "RDCLAK5uy_l2CXCpt8bt8t2IQ_6q0M3RuBdk2rxDqJE", "title": "Puras Románticas", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/0YD6CGQIMrfwWyh93u_GeG4KU9TxFXe1yghgtP4pyvJ1QHTcEyOKhfCTFIYGFH6yAJKqBdNYBL8uiMk=w544-h544-l90-rj"}], "artists": [{"id": "UCdOm-7-Qe3t0KXU9hbZ91zw", "title": "Claudia Sarne", "thumbnail": "https://yt3.googleusercontent.com/0eRYcnH5MsWII-jJJvrADjoXWOFd3ENykg-7WKk-XIhXcJ9CDk77nMNmjNMSjvALk_Qwt1m4eU5GFTO0=w120-h120-l90-rj"}, {"id": "UCwItHQ6AZ1ZH4SVNUCrpSPw", "title": "Blood Orange", "thumbnail": "https://lh3.googleusercontent.com/TcpHWvn9SofQvJ4THQjcznZZIR2qyza_1SSBuBX3V1b6aAwmdI2Qyr-Cl-j-pxqWK-wQP0rWswVp-Ts=w120-h120-p-l90-rj"}, {"id": "UCI525SG53_5lnwJM6QvZhwQ", "title": "John Carpenter", "thumbnail": "https://lh3.googleusercontent.com/s48nMXtMAHFbfQsNvDX0Iby0WP2NWx7I5ehUgf24e9rq3u-6DdJZn8mF6aIzOqrpaBX1tmj2ywqXDQ_y=w120-h120-p-l90-rj"}, {"id": "UCfv8TEQ44ej9uPGF-SNsDBA", "title": "John Williams", "thumbnail": "https://lh3.googleusercontent.com/4J1gAJn8BSieRnoKvYS0BG5jrm55cKiAFXjJDOhi3Qgr-iIOgcdDWHXkjboElSXXHiDLZNowRyZ1YcU=w120-h120-p-l90-rj"}, {"id": "UCpXXBsB1exs7tMZUyw0T0FA", "title": "Randy Newman", "thumbnail": "https://yt3.ggpht.com/ytc/AIdro_l1QnqtT3v61yOl9kwxRvlsWhvnUwL1dIL0mpAsbWiXIg=w120-h120-l90-rj"}, {"id": "UCc8L7uqV-nqoCpiNNEhV6Lw", "title": "Daniel Pemberton", "thumbnail": "https://yt3.googleusercontent.com/_7qC3lQIVQfpeIIQbqMBq3bSkRqFyHpMAHg8dSgCa79ynb7uVpxwihgjqFj5el_Q56Psp2BNpYLXnY18=w120-h120-l90-rj"}]}, "R&B": {"songs": [{"id": "D4L1K4WKEOc", "title": "To Want Someone Badly", "artists": [{"name": "Khamari", "id": "UCU4amv5K_c2juwja_xyBR9w"}], "thumbnail": "https://yt3.googleusercontent.com/EJ2n35mnjDpEnq5FjrKwzdWpdUkdX0AOhqpi-zjAiQ4o7VvTW11zYYNr5ML8Rt7oueZ1f1Esckt_dgATKg=w60-h60-l90-rj", "explicit": false}, {"id": "n7PKSmGcgjE", "title": "Coming Home", "artists": [{"name": "BJRNCK", "id": "UCaCOpX3mC7njTAjeb_xKJdg"}, {"name": "y", "id": null}, {"name": "G Herbo", "id": "UCNUsD0eEXcria_84lGniylQ"}], "thumbnail": "https://yt3.googleusercontent.com/8sHeLwGOxbSwQq_eLTCp45j_YKat4ApzSWm03JVxJAcWGDtWML3Tc0uqYzEgFQtBrNWECL57s5VmQpxu=w60-h60-l90-rj", "explicit": true}, {"id": "_q5k9wgjU5E", "title": "Sugar High", "artists": [{"name": "Mai Anna", "id": "UCmJasFamXMO_hciyg17FWeA"}], "thumbnail": "https://yt3.googleusercontent.com/kX5UIRXN8BOr4Ju8QEkj0wjaKr9I0Z46D9f-f1Y1Bzt9d22cMwJhhZ6vQ83HM7kKLj1jMwSTL7bkwKqf=w60-h60-l90-rj", "explicit": false}, {"id": "3cGc5uyCnhY", "title": "Miss Mango", "artists": [{"name": "Laila!", "id": "UCwhRTQzYuJSuW8W1rY6DxJw"}], "thumbnail": "https://yt3.googleusercontent.com/3OhTH7DN1DWQW_69lq7e-KGfIPhOiYF2Tt7aI9Fc61JkGD2FRsxYuJ6IbFNKooR7WQBLP4GZ2lG6d1Pjmg=w60-h60-l90-rj", "explicit": true}, {"id": "oI13L236R7E", "title": "Miss Your Touch", "artists": [{"name": "Genia", "id": "UCW-ZDdJKJVTS2L-73F3mSXA"}], "thumbnail": "https://yt3.googleusercontent.com/23jbr7tvtVChZZUCuchZ87MtdXzGuCopXMtDWQuBAyX3vlpOTSYQhC5OygtlO8ktYT5Z-Fn9Ie-zUVU=w60-h60-l90-rj", "explicit": true}, {"id": "T0l5hxOoHPU", "title": "Ole", "artists": [{"name": "Sofia Ly", "id": "UCtWRyVPUtbSF9zwCIGdIn0A"}], "thumbnail": "https://yt3.googleusercontent.com/FleR15qQ8S0PpFh4BK6PUgN4s_F0Vx5jPlxu-wTh-_Wt9EpDPiN3phnEBm6xlo-iN5CAjD2rOuJtFTTp=w60-h60-l90-rj", "explicit": false}, {"id": "P544r6uI_Aw", "title": "Does She Know", "artists": [{"name": "Sekou", "id": "UC49SJYAvpdx5-ucjGbLSyOQ"}], "thumbnail": "https://yt3.googleusercontent.com/uz_7963HTkDmH-6XbcuUeIrWf0O_1FiLJUA2YP4keK-boRiVOUo97IuU_UAVmf4HIJRC19Pra-bjh8c=w60-h60-l90-rj", "explicit": false}, {"id": "kt3HcveLxhg", "title": "Work Dat", "artists": [{"name": "Papa Jay", "id": "UCp9Z6pv_L8Enr51RZgv0aRQ"}], "thumbnail": "https://yt3.googleusercontent.com/AGyF1EHn-qmC9WEFEMxVhpNMoBpOfO1J_bAivYa_QvsJDCVeD3_3M6cwvLRvbAyEnWo_dbUa1Ax9HDgJ=w60-h60-l90-rj", "explicit": true}], "albums": [{"browseId": "MPREb_r3jEPg0hFsW", "playlistId": "OLAK5uy_keU6kVBDcgOoQqXxu6e31fvfz2Sx7DiLk", "title": "Ctrl (Deluxe)", "artists": [{"name": "SZA", "id": "UCeKDV9JgivrXehVluw5bKFA"}], "thumbnail": "https://yt3.googleusercontent.com/HaiFcGYCC2nwiu7OD5gNzEIg6CIGV2nCmP2e47bMAN1ZZYJVZKdiXsREAwY1EYSkEaiYj_4CnCBefAhj=w60-h60-l90-rj", "year": 2022, "explicit": false}, {"browseId": "MPREb_6YSlIa9Arss", "playlistId": "OLAK5uy_mSVil6xNYy_hxxJVZYIYpDuPQHWJBDwDU", "title": "Bluestars", "artists": [{"name": "Pretty Ricky", "id": "UCm8o858RP29fWl3TOMlRysA"}], "thumbnail": "https://yt3.googleusercontent.com/dYTKIZ5abL7UbApMdElGULGkGlRDVbpqFW9EK2R284EYErUZkVrc9oicdm9b17lYxm9-OvMLR3XUUVPthg=w60-h60-l90-rj", "year": 2005, "explicit": false}, {"browseId": "MPREb_12qXHnXYW2R", "playlistId": "OLAK5uy_ncKK_z746Ms-m5lpSC3SPZ8mlZ1Riq4WU", "title": "The Introduction of Marcus Cooper", "artists": [{"name": "Pleasure P", "id": "UC-DXhS8Yzf4wiaqmH2hxh3g"}], "thumbnail": "https://yt3.googleusercontent.com/J2v-h5JsnAs0djV-M5wvlQSvZYjZhbKVG9UCEsQtGe19HXIYJomvwD4mDGOhGgjJ-5GCZJyesOUs2oA9=w60-h60-l90-rj", "year": 2009, "explicit": false}, {"browseId": "MPREb_2WzVTTzzoJc", "playlistId": "OLAK5uy_ltvAnceM9UdipqjI7-fJpcE5nZmvlZT9s", "title": "channel ORANGE", "artists": [{"name": "Frank Ocean", "id": "UCETYiBLjt2v-pcKSgf8pe6g"}], "thumbnail": "https://yt3.googleusercontent.com/0yu8Hk3siMPLzbkwOdGqgFpHlIl8ySOz-Ccj4k9KCuFf0HIF2q8xdjp_jCmOb3WoHk1tawPs4q0bjmnwGQ=w60-h60-l90-rj", "year": 2012, "explicit": false}, {"browseId": "MPREb_rosopvW5VWI", "playlistId": "OLAK5uy_mQbVXhmeI2T_E-tJ4NnA-pxQiQQiamxmQ", "title": "B2K", "artists": [{"name": "B2K", "id": "UCRevOHvzlefoYxdI729UICw"}], "thumbnail": "https://yt3.googleusercontent.com/2g4RTGL6LqZ23KVJrMlsm0kpETV9iklDZ5EM_uwdBvCc6B_MpmrwS7ds0pZuQN4NGTwcPqWL1srMGZHI=w60-h60-s-l90-rj", "year": 2002, "explicit": false}, {"browseId": "MPREb_EiGWbNPeSFp", "playlistId": "OLAK5uy_lcy8Wic5NdzT3J4vCthOYD7tSZwZ82mvo", "title": "Forever My Lady", "artists": [{"name": "Jodeci", "id": "UCDXQnFFSRFDGjK50OXKvTOg"}], "thumbnail": "https://yt3.googleusercontent.com/uzAnSzDl1AMqbbY0oMQM_mQiLmd4mkoViso5RlQ1q0Du8ZIeJHaPyuSerc8S6ULzcND8PG_z-9Ozo_PD=w60-h60-s-l90-rj", "year": 1991, "explicit": false}], "playlists": [{"id": "RDCLAK5uy_kb7EBi6y3GrtJri4_ZH56Ms786DFEimbM", "title": "Lo-fi Loft", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/9I45SYZAfTeR17ByV9i2yDpWhQ5DTWMCrW3Lei8vMIiZzeyUnf2zRFUDBob4TGwgUkXAAR_x1Xa8VA=w544-h544-l90-rj"}, {"id": "RDCLAK5uy_kycx4KP0Kj3BcUEZN4RDhGEjUce2UPqEc", "title": "R&B tranquilo de la década del 2000", "author": {"name": "YouTube Music", "id": null}, "thumbnail": "https://yt3.googleusercontent.com/6iE_xkEVqcrdoe6UytGmdKvzX1znJazTW0nMKfxEiBukq4cZ5E9zBRM9hc99I134sFNt-GbDWj1Q1w=w544-h544-l90-rj"}], "artists": [{"id": "UCeKDV9JgivrXehVluw5bKFA", "title": "SZA", "thumbnail": "https://lh3.googleusercontent.com/c-ILO8kXxjY6HhqSkoClWPUtPfHYQW6iHr51EiQOaZiUZ7IZr_WwwkyqclAOFyZgLpC3R0dPXuZiRt0=w120-h120-p-l90-rj"}, {"id": "UCyQT8mwjQ2T0lBVFuX8CxOQ", "title": "Omarion", "thumbnail": "https://lh3.googleusercontent.com/KLglCEJ7wzT9k5tamisbT1pLzkJwS-Qr_iK13kPCvFS7YdgYh6I7OUvpvu2cgoDigZoOklvxrd5rqFtl=w120-h120-p-l90-rj"}, {"id": "UCm8o858RP29fWl3TOMlRysA", "title": "Pretty Ricky", "thumbnail": "https://lh3.googleusercontent.com/pTXoCVMhoT5hxaQhyttzNg8XPkluPGNcQHcOsGrENrVFR5aoDesCxHGkkjTBmkmdhhM1tCl56a_nIvI=w120-h120-p-l90-rj"}, {"id": "UCILuIcqzJMtkxCmftNVjNBQ", "title": "Usher", "thumbnail": "https://yt3.googleusercontent.com/mWYMY57GRNGro1dGyxfyxwB19HWzqI4LEdaiFRL_RHVcTdnl4Gj840UD-B-_JqE5BVBI54xiPdNBGw=w120-h120-p-l90-rj"}, {"id": "UCCnPBkdur_5DHdsLI2pQ1qw", "title": "Bryson Tiller", "thumbnail": "https://lh3.googleusercontent.com/pPP8sDzPgycwft0mqCWcorE4ahsmfZxMDO9-mO2V06iE24DRw3ktndBXlHw27FVv0xyhfwplXYjEv3qu=w120-h120-p-l90-rj"}]}};


async function loadCategoryDetailView(categoryName) {
  setHeaderVisible(true);
  setHeaderSearchPillVisible(true);
  
  contentArea.innerHTML = `<div class="loading-spinner-container"><div class="spinner"></div><p>Cargando categoría "${categoryName}"...</p></div>`;
  document.getElementById('page-title').textContent = categoryName;
  
  pushNavigation({ name: 'category', params: { categoryName } });
  
  let offlineData = CATEGORIAS_APPLE_DATA[categoryName];
  if (!offlineData) {
    const keys = Object.keys(CATEGORIAS_APPLE_DATA);
    const matchedKey = keys.find(k => k.toLowerCase() === categoryName.toLowerCase());
    if (matchedKey) offlineData = CATEGORIAS_APPLE_DATA[matchedKey];
  }
  
  let songs = offlineData?.songs || [];
  let albums = offlineData?.albums || [];
  let playlists = offlineData?.playlists || [];
  let artists = offlineData?.artists || [];
  
  if (songs.length === 0 || playlists.length === 0 || albums.length === 0) {
    try {
      const searchRes = await callInnerTubeAPI('search', { query: categoryName }, WEB_CONTEXT);
      const parsed = parseSearchResultsCategorized(searchRes);
      
      if (songs.length === 0 && parsed['Canciones']) {
        songs = parsed['Canciones'].map(item => ({
          id: item.id,
          title: item.title,
          artists: [{ name: item.artist }],
          thumbnail: item.artwork,
          explicit: false
        }));
      }
      
      if (albums.length === 0 && parsed['Álbumes']) {
        albums = parsed['Álbumes'].map(item => ({
          browseId: item.id,
          playlistId: item.id,
          title: item.title,
          artists: [{ name: item.artist }],
          thumbnail: item.artwork,
          year: 2024,
          explicit: false
        }));
      }
      
      if (playlists.length === 0 && parsed['Playlists']) {
        playlists = parsed['Playlists'].map(item => ({
          id: item.id,
          title: item.title,
          author: { name: item.artist || categoryName },
          thumbnail: item.artwork,
          songCountText: "Playlist"
        }));
      }

      if (artists.length === 0 && parsed['Artistas']) {
        artists = parsed['Artistas'].map(item => ({
          id: item.id,
          title: item.title,
          thumbnail: item.artwork
        }));
      }
    } catch (err) {
      console.warn("Dynamic category search error:", err);
    }
  }

  renderCategoryDetailPage(categoryName, { songs, albums, playlists, artists });
}

function renderCategoryDetailPage(categoryName, data) {
  const { songs, albums, playlists, artists } = data;
  
  const mainContainer = document.createElement('div');
  mainContainer.style.width = "100%";
  mainContainer.style.boxSizing = "border-box";
  mainContainer.style.padding = "24px 36px 60px 36px";
  mainContainer.style.animation = "fadeIn 0.25s ease-out";
  mainContainer.style.color = "white";

  // 1. Header Title
  const headerDiv = document.createElement('div');
  headerDiv.style.marginBottom = "24px";
  headerDiv.innerHTML = `<h1 style="font-size: 32px; font-weight: 900; color: white; letter-spacing: -0.02em; margin: 0;">${categoryName}</h1>`;
  mainContainer.appendChild(headerDiv);

  // 2. Featured Hero Banners (Carrusel Héroe Ancho)
  const heroSection = document.createElement('div');
  heroSection.style.marginBottom = "36px";
  
  const heroBanners = [];
  if (playlists.length > 0) {
    heroBanners.push({
      tag: "PLAYLIST ACTUALIZADA",
      title: playlists[0].title,
      subtitle: playlists[0].author?.name || `Apple Music ${categoryName}`,
      img: playlists[0].thumbnail || "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600",
      badge: "Audio espacial con Dolby Atmos",
      item: playlists[0],
      type: 'playlist'
    });
  }
  if (albums.length > 0) {
    heroBanners.push({
      tag: "NUEVO ÁLBUM",
      title: albums[0].title,
      subtitle: albums[0].artists?.[0]?.name || "Artista",
      img: albums[0].thumbnail || "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600",
      item: albums[0],
      type: 'album'
    });
  }
  if (albums.length > 1) {
    heroBanners.push({
      tag: "ÁLBUM DESTACADO",
      title: albums[1].title,
      subtitle: albums[1].artists?.[0]?.name || "Artista",
      img: albums[1].thumbnail || "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600",
      item: albums[1],
      type: 'album'
    });
  }

  if (heroBanners.length > 0) {
    heroSection.innerHTML = `
      <div style="display: flex; gap: 20px; overflow-x: auto; scrollbar-width: none; padding-bottom: 10px;">
        ${heroBanners.map(b => `
          <div class="hero-banner-card" style="min-width: 420px; max-width: 420px; height: 230px; border-radius: 18px; overflow: hidden; position: relative; cursor: pointer; flex-shrink: 0; box-shadow: 0 12px 32px rgba(0,0,0,0.5); transition: transform 0.2s ease;">
            <img src="${b.img}" style="position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; filter: brightness(0.85);">
            <div style="position: absolute; inset: 0; background: linear-gradient(180deg, rgba(0,0,0,0.7) 0%, rgba(0,0,0,0.2) 50%, rgba(0,0,0,0.85) 100%);"></div>
            
            <div style="position: relative; z-index: 2; padding: 20px; height: 100%; box-sizing: border-box; display: flex; flex-direction: column; justify-content: space-between;">
              <div>
                <span style="font-size: 11px; font-weight: 800; letter-spacing: 0.08em; text-transform: uppercase; color: rgba(255,255,255,0.75);">${b.tag}</span>
                <h2 style="font-size: 22px; font-weight: 900; color: white; margin: 4px 0 2px 0; line-height: 1.2; text-shadow: 0 2px 8px rgba(0,0,0,0.8);">${b.title}</h2>
                <span style="font-size: 14px; font-weight: 600; color: rgba(255,255,255,0.8);">${b.subtitle}</span>
              </div>
              
              ${b.badge ? `
                <div style="display: inline-flex; align-items: center; gap: 6px; background: rgba(0,0,0,0.5); backdrop-filter: blur(8px); padding: 4px 10px; border-radius: 20px; border: 1px solid rgba(255,255,255,0.15); width: fit-content;">
                  <svg viewBox="0 0 24 24" width="12" height="12" fill="white"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 14c-2.21 0-4-1.79-4-4s1.79-4 4-4 4 1.79 4 4-1.79 4-4 4z"/></svg>
                  <span style="font-size: 11px; font-weight: 700; color: white;">${b.badge}</span>
                </div>
              ` : ''}
            </div>
          </div>
        `).join('')}
      </div>
    `;
    mainContainer.appendChild(heroSection);
  }

  // 3. Playlists Section (Playlists >)
  if (playlists.length > 0) {
    const playlistsSec = document.createElement('div');
    playlistsSec.style.marginBottom = "36px";
    playlistsSec.innerHTML = `
      <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px;">
        <h2 style="font-size: 20px; font-weight: 800; color: white; cursor: pointer; display: flex; align-items: center; gap: 6px;">Playlists <span style="font-size: 18px; color: rgba(255,255,255,0.5);">›</span></h2>
      </div>
      <div style="display: flex; gap: 18px; overflow-x: auto; scrollbar-width: none; padding-bottom: 8px;">
        ${playlists.map(pl => `
          <div class="playlist-card-item" data-id="${pl.id}" style="min-width: 175px; max-width: 175px; cursor: pointer; flex-shrink: 0;">
            <div style="width: 175px; height: 175px; border-radius: 12px; overflow: hidden; position: relative; box-shadow: 0 8px 20px rgba(0,0,0,0.35); margin-bottom: 10px;">
              <img src="${pl.thumbnail}" style="width: 100%; height: 100%; object-fit: cover;">
              <div style="position: absolute; top: 8px; right: 8px; background: rgba(0,0,0,0.45); backdrop-filter: blur(6px); padding: 3px 7px; border-radius: 5px; color: white; font-size: 10.5px; font-weight: 800;">Music</div>
            </div>
            <div style="font-size: 14px; font-weight: 700; color: white; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-bottom: 2px;">${pl.title}</div>
            <div style="font-size: 12.5px; color: rgba(255,255,255,0.55); white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${pl.author?.name || 'Playlist recomendada'}</div>
          </div>
        `).join('')}
      </div>
    `;
    
    playlistsSec.querySelectorAll('.playlist-card-item').forEach((card, idx) => {
      card.addEventListener('click', () => {
        const item = playlists[idx];
        renderSectionDetailView(item.title, [], item.author?.name || categoryName);
      });
    });
    mainContainer.appendChild(playlistsSec);
  }

  // 4. Canciones nuevas destacadas (4-row grid with 3-dots)
  if (songs.length > 0) {
    const songsSec = document.createElement('div');
    songsSec.style.marginBottom = "36px";
    songsSec.innerHTML = `
      <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px;">
        <h2 style="font-size: 20px; font-weight: 800; color: white; cursor: pointer; display: flex; align-items: center; gap: 6px;">Canciones nuevas destacadas <span style="font-size: 18px; color: rgba(255,255,255,0.5);">›</span></h2>
      </div>
      <div style="display: grid; grid-template-rows: repeat(4, auto); grid-auto-flow: column; grid-auto-columns: minmax(320px, 360px); gap: 10px 24px; overflow-x: auto; scrollbar-width: none; padding-bottom: 8px;">
        ${songs.map(song => {
          const artistStr = Array.isArray(song.artists) ? song.artists.map(a => a.name).join(', ') : (song.artist || 'Artista');
          return `
            <div class="song-grid-item" data-id="${song.id}" style="display: flex; align-items: center; gap: 12px; padding: 6px 10px; border-radius: 10px; cursor: pointer; transition: background 0.15s ease;">
              <img src="${song.thumbnail}" style="width: 44px; height: 44px; border-radius: 8px; object-fit: cover; flex-shrink: 0; box-shadow: 0 4px 10px rgba(0,0,0,0.3);">
              <div style="display: flex; flex-direction: column; flex-grow: 1; overflow: hidden; white-space: nowrap;">
                <div style="font-size: 14px; font-weight: 700; color: white; text-overflow: ellipsis; overflow: hidden; display: flex; align-items: center; gap: 6px;">
                  <span>${song.title}</span>
                  ${song.explicit ? `<span style="font-size: 9px; font-weight: 800; background: rgba(255,255,255,0.2); padding: 1px 4px; border-radius: 3px; color: rgba(255,255,255,0.8);">E</span>` : ''}
                </div>
                <div style="font-size: 12.5px; color: rgba(255,255,255,0.6); text-overflow: ellipsis; overflow: hidden;">${artistStr}</div>
              </div>
              <button class="song-opts-btn" style="background: transparent; border: none; color: rgba(255,255,255,0.5); cursor: pointer; font-size: 18px; padding: 4px 8px; margin-left: auto;" title="Opciones">⋮</button>
            </div>
          `;
        }).join('')}
      </div>
    `;

    songsSec.querySelectorAll('.song-grid-item').forEach((row, idx) => {
      row.addEventListener('mouseenter', () => row.style.background = "rgba(255,255,255,0.08)");
      row.addEventListener('mouseleave', () => row.style.background = "transparent");
      
      const song = songs[idx];
      const artistStr = Array.isArray(song.artists) ? song.artists.map(a => a.name).join(', ') : (song.artist || 'Artista');
      
      row.addEventListener('click', (e) => {
        if (!e.target.closest('.song-opts-btn')) {
          playTrack({
            id: song.id,
            title: song.title,
            artist: artistStr,
            artwork: song.thumbnail
          });
        }
      });

      const optsBtn = row.querySelector('.song-opts-btn');
      if (optsBtn) {
        optsBtn.addEventListener('click', (e) => {
          e.stopPropagation();
          showTrackContextMenu(e, {
            id: song.id,
            title: song.title,
            artist: artistStr,
            artwork: song.thumbnail
          });
        });
      }
    });

    mainContainer.appendChild(songsSec);
  }

  // 5. Nuevos lanzamientos (Álbumes)
  if (albums.length > 0) {
    const albumsSec = document.createElement('div');
    albumsSec.style.marginBottom = "36px";
    albumsSec.innerHTML = `
      <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px;">
        <h2 style="font-size: 20px; font-weight: 800; color: white; cursor: pointer; display: flex; align-items: center; gap: 6px;">Nuevos lanzamientos <span style="font-size: 18px; color: rgba(255,255,255,0.5);">›</span></h2>
      </div>
      <div style="display: flex; gap: 18px; overflow-x: auto; scrollbar-width: none; padding-bottom: 8px;">
        ${albums.map(alb => {
          const artistStr = Array.isArray(alb.artists) ? alb.artists.map(a => a.name).join(', ') : 'Artista';
          return `
            <div class="album-card-item" data-id="${alb.browseId}" style="min-width: 175px; max-width: 175px; cursor: pointer; flex-shrink: 0;">
              <div style="width: 175px; height: 175px; border-radius: 12px; overflow: hidden; box-shadow: 0 8px 20px rgba(0,0,0,0.35); margin-bottom: 10px;">
                <img src="${alb.thumbnail}" style="width: 100%; height: 100%; object-fit: cover;">
              </div>
              <div style="font-size: 14px; font-weight: 700; color: white; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-bottom: 2px;">${alb.title}</div>
              <div style="font-size: 12.5px; color: rgba(255,255,255,0.55); white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${artistStr}</div>
            </div>
          `;
        }).join('')}
      </div>
    `;

    albumsSec.querySelectorAll('.album-card-item').forEach((card, idx) => {
      card.addEventListener('click', () => {
        const item = albums[idx];
        const artistStr = Array.isArray(item.artists) ? item.artists.map(a => a.name).join(', ') : 'Artista';
        loadAlbumDetailView(item.browseId || item.playlistId, item.title, artistStr, item.thumbnail);
      });
    });

    mainContainer.appendChild(albumsSec);
  }

  // 6. Álbumes imprescindibles
  if (albums.length > 2) {
    const essentialSec = document.createElement('div');
    essentialSec.style.marginBottom = "36px";
    essentialSec.innerHTML = `
      <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px;">
        <h2 style="font-size: 20px; font-weight: 800; color: white; cursor: pointer; display: flex; align-items: center; gap: 6px;">Álbumes imprescindibles <span style="font-size: 18px; color: rgba(255,255,255,0.5);">›</span></h2>
      </div>
      <div style="display: flex; gap: 18px; overflow-x: auto; scrollbar-width: none; padding-bottom: 8px;">
        ${albums.slice().reverse().map(alb => {
          const artistStr = Array.isArray(alb.artists) ? alb.artists.map(a => a.name).join(', ') : 'Artista';
          return `
            <div class="essential-card-item" data-id="${alb.browseId}" style="min-width: 175px; max-width: 175px; cursor: pointer; flex-shrink: 0;">
              <div style="width: 175px; height: 175px; border-radius: 12px; overflow: hidden; box-shadow: 0 8px 20px rgba(0,0,0,0.35); margin-bottom: 10px;">
                <img src="${alb.thumbnail}" style="width: 100%; height: 100%; object-fit: cover;">
              </div>
              <div style="font-size: 14px; font-weight: 700; color: white; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-bottom: 2px;">${alb.title}</div>
              <div style="font-size: 12.5px; color: rgba(255,255,255,0.55); white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${artistStr}</div>
            </div>
          `;
        }).join('')}
      </div>
    `;

    essentialSec.querySelectorAll('.essential-card-item').forEach((card, idx) => {
      card.addEventListener('click', () => {
        const item = albums.slice().reverse()[idx];
        const artistStr = Array.isArray(item.artists) ? item.artists.map(a => a.name).join(', ') : 'Artista';
        loadAlbumDetailView(item.browseId || item.playlistId, item.title, artistStr, item.thumbnail);
      });
    });

    mainContainer.appendChild(essentialSec);
  }

  // 7. Artistas que nos encantan (CIRCULAR AVATARS)
  if (artists.length > 0) {
    const artistsSec = document.createElement('div');
    artistsSec.style.marginBottom = "36px";
    artistsSec.innerHTML = `
      <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px;">
        <h2 style="font-size: 20px; font-weight: 800; color: white; cursor: pointer; display: flex; align-items: center; gap: 6px;">Artistas que nos encantan <span style="font-size: 18px; color: rgba(255,255,255,0.5);">›</span></h2>
      </div>
      <div style="display: flex; gap: 20px; overflow-x: auto; scrollbar-width: none; padding-bottom: 8px;">
        ${artists.map(art => `
          <div class="artist-circle-item" data-id="${art.id}" data-name="${art.title}" style="display: flex; flex-direction: column; align-items: center; min-width: 140px; max-width: 140px; cursor: pointer; flex-shrink: 0;">
            <img src="${art.thumbnail}" style="width: 140px; height: 140px; border-radius: 50%; object-fit: cover; margin-bottom: 10px; box-shadow: 0 8px 24px rgba(0,0,0,0.4); transition: transform 0.2s ease;">
            <span style="font-size: 14px; font-weight: 700; color: white; text-align: center; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; width: 100%;">${art.title}</span>
          </div>
        `).join('')}
      </div>
    `;

    artistsSec.querySelectorAll('.artist-circle-item').forEach(card => {
      card.addEventListener('mouseenter', () => {
        const img = card.querySelector('img');
        if (img) img.style.transform = "scale(1.05)";
      });
      card.addEventListener('mouseleave', () => {
        const img = card.querySelector('img');
        if (img) img.style.transform = "none";
      });
      card.addEventListener('click', () => {
        const artistId = card.getAttribute('data-id');
        const artistName = card.getAttribute('data-name');
        loadArtistDetailView(artistId, artistName);
      });
    });

    mainContainer.appendChild(artistsSec);
  }

  contentArea.innerHTML = '';
  contentArea.appendChild(mainContainer);
}


// Duplicate removed
 [
  { name: "Pop", url: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600" },
  { name: "Urbano Latino", url: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600" },
  { name: "K-Pop", url: "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600" },
  { name: "Rock & Metal", url: "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=600" },
  { name: "Regional Mexicano", url: "https://images.unsplash.com/photo-1511192336575-5a79af67a629?w=600" },
  { name: "Jazz & Soul", url: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600" },
  { name: "Lo-Fi & Chill", url: "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600" },
  { name: "EDM & Electrónica", url: "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=600" },
  { name: "Hip-Hop & Rap", url: "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600" },
  { name: "Salsa & Bachata", url: "https://images.unsplash.com/photo-1504609773096-104ff2c73ba4?w=600" }
];



// --- Search Implementation ---
function initSearchEvents() {
  if (searchInput) {
    searchInput.addEventListener('click', () => {
      if (!searchInput.value.trim()) {
        renderExploreCategoriesView();
      }
    });

    searchInput.addEventListener('focus', () => {
      if (!searchInput.value.trim()) {
        renderExploreCategoriesView();
      }
    });

    searchInput.addEventListener('input', () => {
      clearTimeout(searchTimeout);
      const query = searchInput.value.trim();
      
      if (query.length === 0) {
        if (searchSuggestions) searchSuggestions.classList.add('hidden');
        renderExploreCategoriesView();
        return;
      }
      
      searchTimeout = setTimeout(async () => {
        try {
          const data = await callInnerTubeAPI('music/get_search_suggestions', { input: query }, WEB_CONTEXT);
          const suggestions = [];
          const contents = data.contents?.[0]?.searchSuggestionsSectionRenderer?.contents || [];
          
          contents.forEach(section => {
            const item = section.searchSuggestionRenderer;
            if (item && item.navigationEndpoint?.searchEndpoint?.query) {
              suggestions.push(item.navigationEndpoint.searchEndpoint.query);
            }
          });
          
          if (suggestions.length > 0) {
            renderSuggestions(suggestions);
          } else {
            if (searchSuggestions) searchSuggestions.classList.add('hidden');
          }
        } catch (err) {
          if (searchSuggestions) searchSuggestions.classList.add('hidden');
        }
      }, 300);
    });

    searchInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        const query = searchInput.value.trim();
        if (query) performSearch(query);
      }
    });
  }

  document.addEventListener('click', (e) => {
    if (!e.target.closest('.search-box') && !e.target.closest('#search-suggestions')) {
      if (searchSuggestions) searchSuggestions.classList.add('hidden');
    }
  });
}

function renderSuggestions(list) {
  searchSuggestions.innerHTML = '';
  searchSuggestions.classList.remove('hidden');

  list.forEach(query => {
    const item = document.createElement('div');
    item.className = "suggestion-item";
    item.innerHTML = `
      <svg viewBox="0 0 24 24" width="14" height="14"><path fill="currentColor" d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
      <span>${query}</span>
    `;

    item.addEventListener('click', () => {
      searchInput.value = query;
      searchSuggestions.classList.add('hidden');
      performSearch(query);
    });

    searchSuggestions.appendChild(item);
  });
}

function parseResponsiveListItems(list) {
  const items = [];
  if (!list) return items;
  list.forEach(container => {
    const item = container.musicResponsiveListItemRenderer;
    if (!item) return;
    const parsed = parseSingleResponsiveItem(item);
    if (parsed) items.push(parsed);
  });
  return items;
}

function parseSingleResponsiveItem(item) {
  const titleRuns = item.flexColumns?.[0]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs;
  const titleEndpoint = titleRuns?.[0]?.navigationEndpoint;
  
  const songId = item.playlistItemData?.videoId 
              || item.navigationEndpoint?.watchEndpoint?.videoId 
              || titleEndpoint?.watchEndpoint?.videoId;
              
  const playlistId = item.navigationEndpoint?.browseEndpoint?.browseId 
                  || titleEndpoint?.browseEndpoint?.browseId;
                  
  if (!songId && !playlistId) return null;
  
  let titleText = "Música";
  if (titleRuns && titleRuns.length > 0) titleText = titleRuns[0].text;
  
  const artistRuns = item.flexColumns?.[1]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs || item.subtitle?.runs || [];
  const artistInfo = extractArtistInfo(artistRuns, "Artista");
  let artistText = artistInfo.artistText;
  let artistId = artistInfo.artistId;
  let durSec = 0;
  if (artistRuns && artistRuns.length > 0) {
    const lastRun = artistRuns[artistRuns.length - 1]?.text;
    if (lastRun && /^\d+:\d+(:\d+)?$/.test(lastRun.trim())) {
      durSec = parseDurationToSeconds(lastRun.trim());
    }
  }
  if (!durSec && item.flexColumns?.[2]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.[0]?.text) {
    durSec = parseDurationToSeconds(item.flexColumns[2].musicResponsiveListItemFlexColumnRenderer.text.runs[0].text);
  }
  
  let thumbUrl = upgradeThumbQuality(extractThumbnail(item));
  
  let durationStr = "";
  const durationRegex = /^\d+:\d+(:\d+)?$/;
  if (item.flexColumns) {
    item.flexColumns.forEach(col => {
      const runs = col.musicResponsiveListItemFlexColumnRenderer?.text?.runs;
      if (runs) {
        runs.forEach(r => {
          if (durationRegex.test(r.text.trim())) {
            durationStr = r.text.trim();
          }
        });
      }
    });
  }
  
  return {
    id: songId || playlistId,
    type: songId ? 'song' : (playlistId?.startsWith("UC") ? 'artist' : 'playlist'),
    title: titleText,
    artist: artistText,
    artistId: artistId,
    artwork: thumbUrl,
    durationSec: parseDurationToSeconds(durationStr)
  };
}



// --- Audio Player & Playback Control ---
function loadTrack(index, shouldPlay = true) {
  if (index < 0 || index >= currentQueue.length) return;
  activeIndex = index;

  const track = currentQueue[activeIndex];

  if (track) {
    addToRecentlyPlayed(track);
  }

  // Update UI Elements
  if (songTitleMini) songTitleMini.textContent = track.title;
  if (songArtistMini) songArtistMini.innerHTML = `<span class="artist-link" onclick="event.stopPropagation(); window.safeLoadArtistPage(event, '${escapeHtmlAttr(track.artistId || '')}', '${escapeHtmlAttr(track.artist)}')">${escapeHtmlAttr(track.artist)}</span>`;
  if (songArtworkMini) songArtworkMini.src = upgradeThumbQuality(track.artwork);

  if (nowPlayingTitle) nowPlayingTitle.textContent = track.title;
  if (nowPlayingArtist) nowPlayingArtist.innerHTML = `<span class="artist-link" onclick="event.stopPropagation(); window.safeLoadArtistPage(event, '${escapeHtmlAttr(track.artistId || '')}', '${escapeHtmlAttr(track.artist)}')">${escapeHtmlAttr(track.artist)}</span>`;
  if (nowPlayingLargeArtwork) nowPlayingLargeArtwork.src = upgradeThumbQuality(track.artwork);

  renderQueue();
  loadLyricsForCurrentTrack();

  // Instant Album View background color shift & track row highlight update
  updateAlbumTrackRowsHighlight(index);

  // Reset playback position ALWAYS to second 0
  isPlaying = false;
  if (playbackInterval) clearInterval(playbackInterval);
  currentPlaybackTime = 0;

  const audioEl = document.getElementById('audio-player');
  if (audioEl) {
    try { audioEl.currentTime = 0; } catch(e) {}
  }
  if (isYtReady && ytPlayer && ytPlayer.seekTo) {
    try { ytPlayer.seekTo(0, true); } catch(e) {}
  }

  // STRICT REAL DURATION RESET: Always clear previous song's duration completely
  currentPlaybackTime = 0;
  currentPlaybackDuration = 0;

  if (timelineProgress) timelineProgress.style.width = "0%";
  if (timelineHandle) timelineHandle.style.left = "0%";
  if (timeElapsedLabel) timeElapsedLabel.textContent = "0:00";
  if (timeRemainingLabel) timeRemainingLabel.textContent = "--:--";

  if (track.durationSec && track.durationSec > 0 && track.durationSec !== 180) {
    currentPlaybackDuration = track.durationSec;
    if (timeRemainingLabel) timeRemainingLabel.textContent = "-" + formatTime(track.durationSec);
  }

  // If duration is unknown, resolve it immediately in background to update UI instantly
  if (!currentPlaybackDuration && track.id) {
    const cleanId = String(track.id).replace('Video', '');
    fetchStreamUrl(cleanId).then(res => {
      if (res && res.durationSec) {
        track.durationSec = res.durationSec;
        if (activeIndex === index) {
          currentPlaybackDuration = res.durationSec;
          updateTimelineUI();
          updateExpandedPlayerView();
        }
      }
    });
  }

  if (shouldPlay) {
    playTrack();
  }

  // Load related songs (Autoplay / Watch Next) ONLY for non-collection tracks
  const isCollectionTrack = (track && track.isCollection) || (currentPlaybackContext !== null);
  if (track && !isCollectionTrack) {
    const cleanId = track.id ? String(track.id).replace('Video', '').trim() : '';
    const is11CharVideoId = /^[a-zA-Z0-9_-]{11}$/.test(cleanId);

    const applyRelatedSongs = (suggestions) => {
      if (!suggestions || suggestions.length === 0) return;
      
      // Store all suggestions in relatedSongsCache for the "Canciones Similares" section
      const queueIds = new Set(currentQueue.map(t => t.id));
      relatedSongsCache = suggestions.filter(s => s.id && !queueIds.has(s.id));

      const existingIds = new Set(currentQueue.map(t => t.id));
      const newSuggestions = suggestions.filter(s => s.id && !existingIds.has(s.id));
      if (newSuggestions.length > 0) {
        currentQueue.push(...newSuggestions);
      }
      renderQueue();
      renderExpandedQueue();
      if (typeof updateExpandedPlayerView === 'function') updateExpandedPlayerView();
    };

    const populateQueueWithWatchNext = async (targetVid) => {
      try {
        const suggestions = await fetchWatchNext(targetVid);
        if (suggestions && suggestions.length > 0) {
          logPlayback(`WatchNext returned ${suggestions.length} related songs`, "success");
          applyRelatedSongs(suggestions);
        } else {
          // Fallback: use search-based related songs
          logPlayback(`WatchNext returned 0 results, trying search fallback...`, "warn");
          const fallbackSuggestions = await fetchRelatedBySearch(track.title, track.artist);
          if (fallbackSuggestions && fallbackSuggestions.length > 0) {
            applyRelatedSongs(fallbackSuggestions);
          } else {
            logPlayback(`No related songs found via any method`, "warn");
          }
        }
      } catch (err) {
        logPlayback(`populateQueueWithWatchNext error: ${err.message}`, "error");
        // Try fallback on error too
        try {
          const fallbackSuggestions = await fetchRelatedBySearch(track.title, track.artist);
          if (fallbackSuggestions && fallbackSuggestions.length > 0) {
            applyRelatedSongs(fallbackSuggestions);
          }
        } catch (e2) {}
      }
    };

    if (is11CharVideoId) {
      populateQueueWithWatchNext(cleanId);
    } else {
      logPlayback(`Video ID "${cleanId}" not 11-chars. Auto-resolving for WatchNext...`, "info");
      resolvePlayableVideoId(track.title, track.artist).then(vId => {
        if (vId) {
          track.id = vId;
          populateQueueWithWatchNext(vId);
        } else {
          // Even without a valid video ID, try search fallback
          logPlayback(`Could not resolve video ID, trying search fallback...`, "warn");
          fetchRelatedBySearch(track.title, track.artist).then(fallback => {
            applyRelatedSongs(fallback);
          });
        }
      });
    }
  }
}

function playTrackDetails(id, title, artist, artwork, artistId, durationSec = 0) {
  currentPlaybackContext = null; // Single song mode enables radio/WatchNext
  const cleanId = id ? String(id).replace('Video', '') : id;
  currentPlaybackTime = 0;
  const newTrack = {
    id: cleanId,
    title: title,
    artist: artist,
    artistId: artistId,
    album: "YouTube Music",
    artwork: upgradeThumbQuality(artwork),
    durationSec: durationSec || 0,
    streamUrl: ""
  };

  // Replace queue with single track to load fresh related songs queue (Mobile Behavior)
  currentQueue = [newTrack];
  activeIndex = 0;
  loadTrack(0, true);
}

function playAudioStream(url) {
  const audioEl = document.getElementById('audio-player');
  if (audioEl) {
    logPlayback(`Playing audio stream via HTML5 Audio element...`, "info");
    audioEl.src = url;
    audioEl.volume = currentVolume || 0.8;
    audioEl.play().then(() => {
      isPlaying = true;
      if (playIcon) playIcon.classList.add('hidden');
      if (pauseIcon) pauseIcon.classList.remove('hidden');
      startRealTimePlayback();
    }).catch(err => {
      logPlayback(`HTML5 Audio play error: ${err.message}`, "error");
    });
  }
}

function launchYtPlayer(vid) {
  const host = document.getElementById('yt-player-host');
  if (host) host.style.display = 'block';

  logPlayback(`launchYtPlayer: Initializing YT.Player for videoId="${vid}"...`, "info");

  if (ytPlayer && typeof ytPlayer.loadVideoById === 'function') {
    try {
      logPlayback(`Loading videoId "${vid}" via existing ytPlayer...`, "info");
      ytPlayer.loadVideoById({ videoId: vid, startSeconds: 0 });
      if (ytPlayer.unMute) ytPlayer.unMute();
      if (ytPlayer.setVolume) ytPlayer.setVolume(Math.floor((currentVolume || 0.8) * 100));
      if (ytPlayer.playVideo) ytPlayer.playVideo();
    } catch(e) {
      logPlayback(`ytPlayer.loadVideoById exception: ${e.message}`, "error");
    }
  } else if (window.YT && window.YT.Player) {
    host.innerHTML = '<div id="yt-player" style="width:100%;height:100%;"></div>';
    try {
      ytPlayer = new YT.Player('yt-player', {
        height: '100%',
        width: '100%',
        videoId: vid,
        host: 'https://www.youtube-nocookie.com',
        playerVars: {
          'autoplay': 1,
          'controls': 0,
          'disablekb': 1,
          'fs': 0,
          'rel': 0,
          'playsinline': 1,
          'origin': window.location.origin || 'https://raymusic.app'
        },
        events: {
          'onReady': (event) => {
            logPlayback(`YT.Player onReady fired for "${vid}"! Unmuting and playing...`, "success");
            try {
              event.target.unMute();
              event.target.setVolume(Math.floor((currentVolume || 0.8) * 100));
              event.target.playVideo();
            } catch(e) {}
          },
          'onStateChange': (event) => {
            const stateNames = { '-1': 'UNSTARTED', 0: 'ENDED', 1: 'PLAYING', 2: 'PAUSED', 3: 'BUFFERING', 5: 'CUED' };
            logPlayback(`YT.Player StateChanged: ${stateNames[event.data] || event.data}`, event.data === 1 ? 'success' : 'info');
            if (event.data === 1) { // PLAYING
              isPlaying = true;
              try {
                event.target.unMute();
                event.target.setVolume(Math.floor((currentVolume || 0.8) * 100));
              } catch(e) {}
              if (playIcon) playIcon.classList.add('hidden');
              if (pauseIcon) pauseIcon.classList.remove('hidden');
              startRealTimePlayback();
            } else if (event.data === 2) { // PAUSED
              isPlaying = false;
              if (playIcon) playIcon.classList.remove('hidden');
              if (pauseIcon) pauseIcon.classList.add('hidden');
            } else if (event.data === 0) { // ENDED
              nextTrack();
            }
          },
          'onError': (err) => {
            logPlayback(`YT.Player Error code ${err.data}`, 'error');
          }
        }
      });
    } catch(e) {
      logPlayback(`Failed to create YT.Player: ${e.message}`, "error");
    }
  }
}

function playTrack() {
  unlockWindowsAudioSession();
  isPlaying = true;
  if (playIcon) playIcon.classList.add('hidden');
  if (pauseIcon) pauseIcon.classList.remove('hidden');

  const track = currentQueue[activeIndex];
  if (!track || !track.id) return;

  // Reset duration UI for current track until resolved
  currentPlaybackDuration = track.durationSec || 0;
  if (typeof updateTimelineUI === 'function') updateTimelineUI();

  let cleanId = String(track.id).replace('Video', '');
  logPlayback(`playTrack: title="${track.title}", cleanId="${cleanId}"`, "info");
  LibraryStorage.addRecentlyPlayed(track);
  updatePlayerHeartUI();

  callInnerTubeAPI('player', { videoId: cleanId }, WEB_CONTEXT).then(data => {
    const status = data?.playabilityStatus?.status;
    const reason = data?.playabilityStatus?.reason || "";
    logPlayback(`InnerTube player status="${status}" reason="${reason}"`, status === 'OK' ? 'success' : 'warn');

    if (data?.videoDetails?.lengthSeconds) {
      const durSec = parseInt(data.videoDetails.lengthSeconds, 10);
      if (durSec > 0) {
        track.durationSec = durSec;
        currentPlaybackDuration = durSec;
        if (typeof updateTimelineUI === 'function') updateTimelineUI();
      }
    }

    if (status === 'UNPLAYABLE' || status === 'ERROR' || status === 'LOGIN_REQUIRED' || status === 'LIVE_STREAM_OFFLINE') {
      logPlayback(`ID "${cleanId}" is ${status} (${reason}). Auto-resolving audio track ID...`, "warn");
      resolvePlayableVideoId(track.title, track.artist, track.album).then(playableId => {
        if (playableId) {
          logPlayback(`Using resolved audio track ID "${playableId}" instead of "${cleanId}"`, "success");
          track.id = playableId;
          launchYtPlayer(playableId);
        } else {
          logPlayback(`Launching original song ID "${cleanId}"`, "warn");
          launchYtPlayer(cleanId);
        }
      });
    } else {
      launchYtPlayer(cleanId);
    }
  }).catch(err => {
    logPlayback(`InnerTube player API warning: ${err.message}. Launching "${cleanId}"`, "warn");
    launchYtPlayer(cleanId);
  });

  startRealTimePlayback();
}

function resumeTrack() {
  unlockWindowsAudioSession();
  isPlaying = true;
  if (playIcon) playIcon.classList.add('hidden');
  if (pauseIcon) pauseIcon.classList.remove('hidden');

  if (isYtReady && ytPlayer && typeof ytPlayer.playVideo === 'function') {
    try { ytPlayer.playVideo(); } catch(e) {}
  }
  const iframe = document.getElementById('yt-main-iframe');
  if (iframe && iframe.contentWindow) {
    try {
      iframe.contentWindow.postMessage('{"event":"command","func":"playVideo","args":""}', '*');
    } catch(e) {}
  }
  const audioEl = document.getElementById('audio-player');
  if (audioEl && audioEl.paused && audioEl.src) {
    try { audioEl.play(); } catch(e) {}
  }
  startRealTimePlayback(false);
}

function pauseTrack() {
  isPlaying = false;
  if (playIcon) playIcon.classList.remove('hidden');
  if (pauseIcon) pauseIcon.classList.add('hidden');
  if (isYtReady && ytPlayer && ytPlayer.pauseVideo) {
    try { ytPlayer.pauseVideo(); } catch(e) {}
  }
  const iframe = document.getElementById('yt-main-iframe');
  if (iframe && iframe.contentWindow) {
    try {
      iframe.contentWindow.postMessage('{"event":"command","func":"pauseVideo","args":""}', '*');
    } catch(e) {}
  }
  const audioEl = document.getElementById('audio-player');
  if (audioEl && !audioEl.paused) {
    try { audioEl.pause(); } catch(e) {}
  }
  if (playbackInterval) clearInterval(playbackInterval);
}

function togglePlay() {
  if (!isPlaying) {
    const audioEl = document.getElementById('audio-player');
    const hasActiveMedia = (isYtReady && ytPlayer && typeof ytPlayer.playVideo === 'function') || (audioEl && audioEl.src);
    if (hasActiveMedia && currentPlaybackTime > 0) {
      resumeTrack();
    } else {
      playTrack();
    }
  } else {
    pauseTrack();
  }
}

function prevTrack() {
  if (currentPlaybackTime > 5) {
    currentPlaybackTime = 0;
    updateTimelineUI();
    const track = currentQueue[activeIndex];
    if (isPlaying && track && track.resolvedUrl) {
      playNativeVlc(track.resolvedUrl, 0);
    }
  } else {
    let prevIdx = activeIndex - 1;
    if (prevIdx < 0) prevIdx = currentQueue.length - 1;
    loadTrack(prevIdx);
  }
}

function nextTrack() {
  let nextIdx = activeIndex + 1;
  if (nextIdx >= currentQueue.length) {
    if (isRepeat) {
      nextIdx = 0;
    } else {
      pauseTrack();
      return;
    }
  }
  loadTrack(nextIdx);
}

function startRealTimePlayback(resetTime = true) {
  if (playbackInterval) clearInterval(playbackInterval);
  if (resetTime) {
    currentPlaybackTime = 0;
  }
  playbackInterval = setInterval(() => {
    if (isPlaying) {
      currentPlaybackTime++;
      if (currentPlaybackDuration > 0 && currentPlaybackTime >= currentPlaybackDuration) {
        clearInterval(playbackInterval);
        nextTrack();
      } else {
        updateTimelineUI();
      }
    }
  }, 1000);
}

function updateTimelineUI() {
  const elapsed = currentPlaybackTime;
  const dur = currentPlaybackDuration;
  timeElapsedLabel.textContent = formatTime(elapsed);

  if (dur > 0) {
    const remaining = Math.max(0, dur - elapsed);
    timeRemainingLabel.textContent = "-" + formatTime(remaining);
    const pct = Math.min(100, Math.max(0, (elapsed / dur) * 100));
    timelineProgress.style.width = pct + "%";
    timelineHandle.style.left = pct + "%";
  } else {
    timeRemainingLabel.textContent = "--:--";
    timelineProgress.style.width = "0%";
    timelineHandle.style.left = "0%";
  }

  // Continuously update detailed player time text
  const expandedTimeText = document.getElementById('expanded-time-text');
  if (expandedTimeText) {
    const track = currentQueue[activeIndex];
    const realDur = dur || track?.durationSec || 0;
    const elapsedStr = formatTime(elapsed);
    if (realDur > 0) {
      const remainingSec = Math.max(0, realDur - elapsed);
      expandedTimeText.textContent = `${elapsedStr} / -${formatTime(remainingSec)}`;
    } else {
      expandedTimeText.textContent = `${elapsedStr} / -0:00`;
    }
  }

  updateLyricsHighlight(elapsed);
}

// --- Wire HTML5 Audio Element Events to UI ---
function initAudioEvents() {
  const audioEl = document.getElementById('audio-player');
  if (!audioEl) return;

  audioEl.addEventListener('timeupdate', () => {
    if (!audioEl.paused && audioEl.duration && isFinite(audioEl.duration)) {
      currentPlaybackTime = Math.floor(audioEl.currentTime);
      currentPlaybackDuration = Math.floor(audioEl.duration);
      updateTimelineUI();
    }
  });

  audioEl.addEventListener('loadedmetadata', () => {
    if (audioEl.duration && isFinite(audioEl.duration)) {
      currentPlaybackDuration = Math.floor(audioEl.duration);
      const track = currentQueue[activeIndex];
      if (track) track.durationSec = currentPlaybackDuration;
      updateTimelineUI();
    }
  });

  audioEl.addEventListener('ended', () => {
    nextTrack();
  });

  audioEl.addEventListener('error', (e) => {
    console.warn('Audio element error:', e, audioEl.error);
  });

  audioEl.addEventListener('playing', () => {
    isPlaying = true;
    if (playIcon) playIcon.classList.add('hidden');
    if (pauseIcon) pauseIcon.classList.remove('hidden');
    if (playbackInterval) clearInterval(playbackInterval);
  });

  audioEl.addEventListener('pause', () => {
  });

  audioEl.volume = 0.8;
}

// --- Player Controls Events ---
function initPlayerEvents() {
  if (playBtn) playBtn.addEventListener('click', togglePlay);
  if (prevBtn) prevBtn.addEventListener('click', prevTrack);
  if (nextBtn) nextBtn.addEventListener('click', nextTrack);

  // Seek Timeline
  let isDragging = false;
  if (timelineSlider) {
    timelineSlider.addEventListener('click', (e) => {
      seek(e);
    });
    timelineSlider.addEventListener('mousedown', (e) => {
      isDragging = true;
      seek(e);
    });
  }
  document.addEventListener('mousemove', (e) => {
    if (isDragging) seek(e);
  });
  document.addEventListener('mouseup', () => {
    isDragging = false;
  });

  function seek(e) {
    if (!timelineSlider || !currentPlaybackDuration) return;
    const rect = timelineSlider.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const pct = Math.min(1, Math.max(0, clickX / rect.width));
    const targetSec = Math.floor(pct * currentPlaybackDuration);

    currentPlaybackTime = targetSec;
    updateTimelineUI();

    if (ytPlayer && typeof ytPlayer.seekTo === 'function') {
      try {
        ytPlayer.seekTo(targetSec, true);
      } catch(err) {}
    } else {
      const iframe = document.getElementById('yt-main-iframe');
      if (iframe && iframe.contentWindow) {
        try {
          iframe.contentWindow.postMessage(`{"event":"command","func":"seekTo","args":[${targetSec}, true]}`, '*');
        } catch(err) {}
      }
    }

    const audioEl = document.getElementById('audio-player');
    if (audioEl && isFinite(audioEl.duration)) {
      try {
        audioEl.currentTime = targetSec;
      } catch(err) {}
    }
  }

  // Volume slider
  let isVolumeDragging = false;
  let lastVolume = 0.8;

  if (volumeSlider) {
    volumeSlider.addEventListener('click', (e) => {
      adjustVolume(e);
    });
    volumeSlider.addEventListener('mousedown', (e) => {
      isVolumeDragging = true;
      adjustVolume(e);
    });
  }
  document.addEventListener('mousemove', (e) => {
    if (isVolumeDragging) adjustVolume(e);
  });
  document.addEventListener('mouseup', () => {
    isVolumeDragging = false;
  });

  function adjustVolume(e) {
    if (!volumeSlider) return;
    const rect = volumeSlider.getBoundingClientRect();
    let pct = (e.clientX - rect.left) / rect.width;
    pct = Math.max(0, Math.min(1, pct));
    if (volumeProgress) volumeProgress.style.width = (pct * 100) + "%";
    if (volumeHandle) volumeHandle.style.left = (pct * 100) + "%";
    currentVolume = pct;
    if (pct > 0) lastVolume = pct;

    if (isYtReady && ytPlayer) {
      if (ytPlayer.unMute) ytPlayer.unMute();
      if (ytPlayer.setVolume) ytPlayer.setVolume(pct * 100);
    }
    const iframe = document.getElementById('yt-main-iframe');
    if (iframe && iframe.contentWindow) {
      try {
        iframe.contentWindow.postMessage(`{"event":"command","func":"setVolume","args":[${pct * 100}]}`, '*');
      } catch(e) {}
    }
    const audioEl = document.getElementById('audio-player');
    if (audioEl) {
      audioEl.muted = (pct === 0);
      audioEl.volume = pct;
    }
  }

  volumeIcon.addEventListener('click', () => {
    const audioEl = document.getElementById('audio-player');
    if (currentVolume > 0) {
      // Mute
      lastVolume = currentVolume;
      currentVolume = 0;
      volumeProgress.style.width = "0%";
      volumeHandle.style.left = "0%";
      if (isYtReady && ytPlayer && ytPlayer.mute) ytPlayer.mute();
      if (audioEl) audioEl.muted = true;
    } else {
      // Unmute
      currentVolume = lastVolume || 0.8;
      const pct = currentVolume * 100;
      volumeProgress.style.width = pct + "%";
      volumeHandle.style.left = pct + "%";
      if (isYtReady && ytPlayer) {
        if (ytPlayer.unMute) ytPlayer.unMute();
        if (ytPlayer.setVolume) ytPlayer.setVolume(pct);
      }
      if (audioEl) {
        audioEl.muted = false;
        audioEl.volume = currentVolume;
      }
    }
  });
}

// --- Lyrics Engine & Panel Toggle ---
let selectedLyricsProvider = 'auto';
let currentLyricsTrackId = null;
let parsedLyricsLines = [];
let currentActiveLyricIdx = -1;
let relatedSongsCache = [];



async function fetchLyricsByProvider(track, provider) {
  const cleanTitle = track.title.replace(/\(Live.*?\)/gi, '').replace(/\(Remastered.*?\)/gi, '').trim();
  const cleanArtist = track.artist ? track.artist.split(',')[0].split('&')[0].trim() : '';

  if (provider === 'youtube' || (provider === 'auto' && track.id && !track.id.includes("Video"))) {
    try {
      const nextData = await callInnerTubeAPI('next', { videoId: track.id }, WEB_CONTEXT);
      const tabs = nextData.contents?.singleColumnMusicWatchNextResultsRenderer?.tabbedRenderer?.watchNextTabbedResultsRenderer?.tabs;
      const lyricsTab = tabs?.[1]?.tabRenderer;
      const lyricsBrowseId = lyricsTab?.endpoint?.browseEndpoint?.browseId;

      if (lyricsBrowseId) {
        const lyricsData = await callInnerTubeAPI('browse', { browseId: lyricsBrowseId }, WEB_CONTEXT);
        const descriptionRuns = lyricsData.contents?.sectionListRenderer?.contents?.[0]?.musicDescriptionShelfRenderer?.description?.runs;
        if (descriptionRuns && descriptionRuns.length > 0) {
          return descriptionRuns.map(r => r.text).join("");
        }
      }
    } catch (e) {
      console.warn("InnerTube lyrics fetch failed:", e);
    }
  }

  if (provider === 'lrclib' || provider === 'auto') {
    try {
      const url = `https://lrclib.net/api/get?track_name=${encodeURIComponent(cleanTitle)}&artist_name=${encodeURIComponent(cleanArtist)}`;
      const res = await fetch(url);
      if (res.ok) {
        const json = await res.json();
        const text = json.syncedLyrics || json.plainLyrics;
        if (text) return text;
      }
    } catch(e) {
      console.warn("LrcLib fetch error:", e);
    }
  }

  if (provider === 'lyricsplus' || provider === 'auto') {
    const baseUrls = ["https://lyricsplus.binimum.org", "https://lyricsplus.atomix.one"];
    for (const baseUrl of baseUrls) {
      try {
        const url = `${baseUrl}/v2/lyrics/get?title=${encodeURIComponent(cleanTitle)}&artist=${encodeURIComponent(cleanArtist)}&source=musixmatch,spotify,apple`;
        const res = await fetch(url);
        if (res.ok) {
          const json = await res.json();
          if (json.lyrics && json.lyrics.length > 0) {
            return json.lyrics.map(l => l.text).join('\n');
          }
        }
      } catch(e) {}
    }
  }

  if (provider === 'simpmusic' || provider === 'auto') {
    try {
      const query = encodeURIComponent(`${cleanTitle} ${cleanArtist}`);
      const searchRes = await fetch(`https://api-lyrics.simpmusic.org/search?q=${query}&limit=1`);
      if (searchRes.ok) {
        const searchJson = await searchRes.json();
        const vId = searchJson.data?.[0]?.videoId;
        if (vId) {
          const lRes = await fetch(`https://api-lyrics.simpmusic.org/lyrics?id=${vId}`);
          if (lRes.ok) {
            const lJson = await lRes.json();
            const item = lJson.data?.[0];
            if (item) return item.syncedLyrics || item.plainLyric || "";
          }
        }
      }
    } catch(e) {}
  }

  if (provider === 'kugou') {
    try {
      const kw = encodeURIComponent(`${cleanTitle} - ${cleanArtist}`);
      const sRes = await fetch(`https://lyrics.kugou.com/search?ver=1&man=yes&client=pc&keyword=${kw}`);
      if (sRes.ok) {
        const sJson = await sRes.json();
        const cand = sJson.candidates?.[0];
        if (cand) {
          const dRes = await fetch(`https://lyrics.kugou.com/download?fmt=lrc&charset=utf8&client=pc&ver=1&id=${cand.id}&accesskey=${cand.accesskey}`);
          if (dRes.ok) {
            const dJson = await dRes.json();
            if (dJson.content) {
              return atob(dJson.content);
            }
          }
        }
      }
    } catch(e) {}
  }

  if (provider === 'betterlyrics' || provider === 'auto') {
    try {
      const url = `https://lyrics-api.boidu.dev/getLyrics?s=${encodeURIComponent(cleanTitle)}&a=${encodeURIComponent(cleanArtist)}`;
      const res = await fetch(url);
      if (res.ok) {
        const json = await res.json();
        const ttml = json.ttml;
        if (ttml && ttml.length > 0) return ttml;
        const plainLyrics = json.plainLyrics || json.lyrics;
        if (plainLyrics) return plainLyrics;
      }
    } catch(e) {
      console.warn("BetterLyrics fetch error:", e);
    }
  }

  if (provider === 'youtubesubtitle') {
    try {
      const vid = track.id ? String(track.id).replace('Video', '').trim() : '';
      if (vid) {
        const nextData = await callInnerTubeAPI('next', { videoId: vid }, WEB_CONTEXT);
        const captions = nextData?.captions?.playerCaptionsTracklistRenderer?.captionTracks;
        if (captions && captions.length > 0) {
          const captionUrl = captions[0].baseUrl;
          if (captionUrl) {
            const subRes = await fetch(captionUrl + '&fmt=srv3');
            if (subRes.ok) {
              const xmlText = await subRes.text();
              const parser = new DOMParser();
              const xmlDoc = parser.parseFromString(xmlText, 'text/xml');
              const textNodes = xmlDoc.querySelectorAll('text');
              let subtitleLines = [];
              textNodes.forEach(node => {
                const start = parseFloat(node.getAttribute('start') || '0');
                const text = node.textContent.replace(/&#39;/g, "'").replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>').trim();
                if (text) {
                  const min = Math.floor(start / 60);
                  const sec = Math.floor(start % 60);
                  const cs = Math.floor((start % 1) * 100);
                  subtitleLines.push(`[${String(min).padStart(2,'0')}:${String(sec).padStart(2,'0')}.${String(cs).padStart(2,'0')}] ${text}`);
                }
              });
              if (subtitleLines.length > 0) return subtitleLines.join('\n');
            }
          }
        }
      }
    } catch(e) {
      console.warn("YouTube Subtitle fetch error:", e);
    }
  }

  return "";
}

function showRightPanelTab(tab) {
  const panelQueue = document.getElementById('panel-content-queue');
  const panelLyrics = document.getElementById('panel-content-lyrics');
  const titleText = document.getElementById('panel-title-text');
  const btnLyrics = document.getElementById('btn-toggle-lyrics');
  const btnQueue = document.getElementById('btn-toggle-queue');

  if (tab === 'lyrics') {
    if (panelQueue) panelQueue.classList.add('hidden');
    if (panelLyrics) panelLyrics.classList.remove('hidden');
    if (titleText) titleText.textContent = "Letras";
    if (btnLyrics) btnLyrics.classList.add('active');
    if (btnQueue) btnQueue.classList.remove('active');
    loadLyricsForCurrentTrack();
  } else {
    if (panelLyrics) panelLyrics.classList.add('hidden');
    if (panelQueue) panelQueue.classList.remove('hidden');
    if (titleText) titleText.textContent = "Now Playing";
    if (btnQueue) btnQueue.classList.add('active');
    if (btnLyrics) btnLyrics.classList.remove('active');
  }
}

// --- Navigation & UI Click bindings ---
function initUIEvents() {
  const btnPlayerFav = document.getElementById('player-favorite');
  if (btnPlayerFav) {
    btnPlayerFav.addEventListener('click', (e) => {
      e.stopPropagation();
      const currentTrack = currentQueue[activeIndex];
      if (currentTrack) {
        LibraryStorage.toggleLike(currentTrack);
      }
    });
  }
  updateSidebarCustomPlaylists();
  updatePlayerHeartUI();

  const btnNavPrev = document.getElementById('nav-prev');
  const btnNavNext = document.getElementById('nav-next');
  const btnLyrics = document.getElementById('btn-toggle-lyrics');
  const btnQueue = document.getElementById('btn-toggle-queue');

  if (btnNavPrev) {
    btnNavPrev.addEventListener('click', () => goBack());
  }
  if (btnNavNext) {
    btnNavNext.addEventListener('click', () => goForward());
  }

  if (btnLyrics) {
    btnLyrics.addEventListener('click', () => {
      rightPanel.classList.remove('hidden');
      showRightPanelTab('lyrics');
    });
  }

  if (btnQueue) {
    btnQueue.addEventListener('click', () => {
      rightPanel.classList.remove('hidden');
      showRightPanelTab('queue');
    });
  }

  const navItems = document.querySelectorAll('.nav-item');
  navItems.forEach(item => {
    item.addEventListener('click', (e) => {
      e.preventDefault();
      if (item.id === 'playlist-create') { showCreatePlaylistModal(); return; }
      
      navItems.forEach(n => n.classList.remove('active'));
      item.classList.add('active');

      const span = item.querySelector('span');
      const tabName = span ? span.textContent : '';
      if (tabName) {
        loadTab(tabName);
      }
    });
  });

  if (btnToggleSidebar) {
    btnToggleSidebar.addEventListener('click', () => {
      if (rightPanel) rightPanel.classList.toggle('hidden');
      btnToggleSidebar.classList.toggle('active');
    });
  }

  if (btnRefresh) {
    btnRefresh.addEventListener('click', () => {
      btnRefresh.style.transform = "rotate(360deg)";
      btnRefresh.style.transition = "transform 0.5s ease";
      setTimeout(() => {
        btnRefresh.style.transform = "none";
        btnRefresh.style.transition = "none";
      }, 500);

      const activeNavItem = document.querySelector('.nav-item.active');
      const activeSpan = activeNavItem ? activeNavItem.querySelector('span') : null;
      const activeTab = activeSpan ? activeSpan.textContent : 'Home';
      loadTab(activeTab);
    });
  }

  initSearchEvents();
}

// --- Sidebar Queue list rendering ---


// --- Helpers ---
function formatTime(seconds) {
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}:${s < 10 ? '0' : ''}${s}`;
}

function updateTimeFooter() {
  const date = new Date();
  let hr = date.getHours();
  const min = date.getMinutes();
  const ampm = hr >= 12 ? 'PM' : 'AM';
  hr = hr % 12;
  hr = hr ? hr : 12;
  const minStr = min < 10 ? '0' + min : min;
  
  const footerTime = document.getElementById('panel-footer-time');
  if (footerTime) {
    footerTime.textContent = `${currentQueue.length} items • ${hr}:${minStr} ${ampm}`;
  }
  
  setTimeout(updateTimeFooter, 30000);
}

// Expose loadArtistPage globally so it can be invoked from onclick attributes in inline HTML
window.loadArtistPage = loadArtistPage;

// --- Expanded Fullscreen Player View Management ---
let expandedMode = 'default'; // 'default', 'lyrics', 'queue'

function openExpandedPlayerView() {
  const overlay = document.getElementById('expanded-player-view');
  if (overlay) {
    overlay.classList.remove('hidden');
    updateExpandedPlayerView();
  }
}

function closeExpandedPlayerView() {
  const overlay = document.getElementById('expanded-player-view');
  if (overlay) {
    overlay.classList.add('hidden');
  }
}

function setExpandedMode(mode) {
  if (expandedMode === mode) {
    expandedMode = 'default';
  } else {
    expandedMode = mode;
  }

  const mainContent = document.getElementById('expanded-main-content');
  const sidePanel = document.getElementById('expanded-side-panel');
  const lyricsView = document.getElementById('expanded-lyrics-view');
  const queueView = document.getElementById('expanded-queue-view');
  const btnLyrics = document.getElementById('expanded-btn-lyrics');
  const btnQueue = document.getElementById('expanded-btn-queue');

  if (btnLyrics) btnLyrics.classList.toggle('active', expandedMode === 'lyrics');
  if (btnQueue) btnQueue.classList.toggle('active', expandedMode === 'queue');

  if (expandedMode === 'default') {
    if (mainContent) mainContent.classList.remove('has-side-panel');
    if (sidePanel) sidePanel.classList.add('hidden');
    if (lyricsView) lyricsView.classList.add('hidden');
    if (queueView) queueView.classList.add('hidden');
  } else if (expandedMode === 'lyrics') {
    if (mainContent) mainContent.classList.add('has-side-panel');
    if (sidePanel) sidePanel.classList.remove('hidden');
    if (lyricsView) lyricsView.classList.remove('hidden');
    if (queueView) queueView.classList.add('hidden');
    renderExpandedLyrics();
  } else if (expandedMode === 'queue') {
    if (mainContent) mainContent.classList.add('has-side-panel');
    if (sidePanel) sidePanel.classList.remove('hidden');
    if (lyricsView) lyricsView.classList.add('hidden');
    if (queueView) queueView.classList.remove('hidden');
    renderExpandedQueue();
  }
}

async function renderExpandedLyrics() {
  const container = document.getElementById('expanded-lyrics-list');
  if (!container) return;

  const track = currentQueue[activeIndex];
  if (!track) {
    container.innerHTML = '<div style="padding: 40px; text-align: center; color: var(--text-secondary);">Sin canción en reproducción.</div>';
    return;
  }

  const providers = [
    { key: 'auto', label: 'Auto' },
    { key: 'lrclib', label: 'LRCLIB' },
    { key: 'kugou', label: 'KuGou' },
    { key: 'betterlyrics', label: 'BetterLyrics' },
    { key: 'lyricsplus', label: 'LyricsPlus' },
    { key: 'simpmusic', label: 'SimpMusic' },
    { key: 'youtube', label: 'YouTube Music' },
    { key: 'youtubesubtitle', label: 'YouTube Subtitle' }
  ];

  const badgesHtml = providers.map(p => {
    const isActive = selectedLyricsProvider === p.key;
    const bg = isActive ? 'var(--accent-color)' : 'rgba(255,255,255,0.1)';
    const color = isActive ? '#fff' : '#aaa';
    return `<span class="provider-badge${isActive ? ' active' : ''}" data-provider="${p.key}" style="font-size: 11px; font-weight: 800; background: ${bg}; color: ${color}; padding: 4px 12px; border-radius: 12px; cursor: pointer; transition: all 0.2s ease;">${p.label}</span>`;
  }).join('');

  container.innerHTML = `
    <div style="display: flex; gap: 8px; margin-bottom: 16px; justify-content: center; flex-wrap: wrap;">
      ${badgesHtml}
    </div>
    <div id="expanded-lyrics-lines" style="display: flex; flex-direction: column; gap: 12px; text-align: center;">
      <div style="color: #aaa; padding: 20px;">Cargando letras sincronizadas...</div>
    </div>
  `;

  // Attach click handlers to provider badges
  container.querySelectorAll('.provider-badge').forEach(badge => {
    badge.addEventListener('click', () => {
      const newProvider = badge.dataset.provider;
      if (newProvider && newProvider !== selectedLyricsProvider) {
        selectedLyricsProvider = newProvider;
        currentLyricsTrackId = null;
        renderExpandedLyrics();
        // Also sync sidebar lyrics if visible
        loadLyricsForCurrentTrack();
      }
    });

    badge.addEventListener('mouseenter', () => {
      if (badge.dataset.provider !== selectedLyricsProvider) {
        badge.style.background = 'rgba(255,255,255,0.18)';
        badge.style.color = '#fff';
      }
    });
    badge.addEventListener('mouseleave', () => {
      if (badge.dataset.provider !== selectedLyricsProvider) {
        badge.style.background = 'rgba(255,255,255,0.1)';
        badge.style.color = '#aaa';
      }
    });
  });

  await loadLyricsForCurrentTrack();
}

function renderExpandedQueue() {
  const container = document.getElementById('expanded-queue-list');
  const npImg = document.getElementById('queue-np-img');
  const npTitle = document.getElementById('queue-np-title');
  const npArtist = document.getElementById('queue-np-artist');
  const totalBadge = document.getElementById('queue-total-count');
  const nextBadge = document.getElementById('queue-next-count');
  const durationBadge = document.getElementById('queue-total-duration');

  const currentTrack = currentQueue[activeIndex];
  if (currentTrack) {
    if (npImg) npImg.src = upgradeThumbQuality(currentTrack.artwork);
    if (npTitle) npTitle.textContent = currentTrack.title;
    if (npArtist) npArtist.textContent = `${currentTrack.artist}${currentTrack.album ? ` — ${currentTrack.album}` : ''}`;
  }

  if (totalBadge) totalBadge.textContent = `${currentQueue.length} items`;
  if (nextBadge) nextBadge.textContent = `${Math.max(0, currentQueue.length - activeIndex - 1)} of ${currentQueue.length}`;

  if (durationBadge) {
    let totalSec = 0;
    currentQueue.forEach(t => totalSec += (t.durationSec || 180));
    const h = Math.floor(totalSec / 3600);
    const m = Math.floor((totalSec % 3600) / 60);
    const durationStr = h > 0 ? `${h}h ${m}m` : `${m} min`;
    
    const remainingSec = currentQueue.slice(activeIndex).reduce((acc, t) => acc + (t.durationSec || 180), 0) - currentPlaybackTime;
    const endTime = new Date(Date.now() + Math.max(0, remainingSec) * 1000);
    let endHr = endTime.getHours();
    const endMin = endTime.getMinutes();
    const ampm = endHr >= 12 ? 'PM' : 'AM';
    endHr = endHr % 12 || 12;
    const endMinStr = endMin < 10 ? '0' + endMin : endMin;

    durationBadge.textContent = `${durationStr} → ${endHr}:${endMinStr} ${ampm}`;
  }

  if (!container) return;
  container.innerHTML = '';

  currentQueue.forEach((track, idx) => {
    if (idx <= activeIndex) return;

    const itemEl = document.createElement('div');
    itemEl.className = "expanded-queue-item";
    itemEl.innerHTML = `
      <span style="font-size: 12.5px; font-weight: 700; color: rgba(255,255,255,0.45); width: 28px; text-align: center; flex-shrink: 0;">${idx - activeIndex}</span>
      <img src="${upgradeThumbQuality(track.artwork)}" style="width: 40px; height: 40px; border-radius: 8px; object-fit: cover; margin: 0 12px 0 6px; flex-shrink: 0;">
      <div style="display: flex; flex-direction: column; flex-grow: 1; overflow: hidden; white-space: nowrap;">
        <span style="font-size: 13px; font-weight: 700; color: white; text-overflow: ellipsis; overflow: hidden;">${escapeHtmlAttr(track.title)}</span>
        <span style="font-size: 11.5px; color: rgba(255,255,255,0.6); text-overflow: ellipsis; overflow: hidden;">${escapeHtmlAttr(track.artist)}</span>
      </div>
    `;

    itemEl.addEventListener('click', () => {
      loadTrack(idx, true);
      renderExpandedQueue();
    });

    container.appendChild(itemEl);
  });

  // "Canciones Similares" section in expanded queue (ONLY for non-collection queues)
  const isCollectionQueue = currentPlaybackContext !== null || (currentQueue && currentQueue.length > 0 && currentQueue[0].isCollection);
  if (!isCollectionQueue && relatedSongsCache && relatedSongsCache.length > 0) {
    const queueIds = new Set(currentQueue.map(t => t.id));
    const displaySimilar = relatedSongsCache.filter(s => !queueIds.has(s.id)).slice(0, 10);

    if (displaySimilar.length > 0) {
      const similarSection = document.createElement('div');
      similarSection.className = "queue-section-header-split";
      similarSection.style.marginTop = "16px";
      similarSection.innerHTML = `
        <span style="display: flex; align-items: center; gap: 6px;">
          <svg viewBox="0 0 24 24" width="14" height="14"><path fill="currentColor" d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>
          Canciones Similares
        </span>
        <span class="next-count-badge">${displaySimilar.length} canciones</span>
      `;
      container.appendChild(similarSection);

      displaySimilar.forEach((track, sIdx) => {
        const itemEl = document.createElement('div');
        itemEl.className = "expanded-queue-item";
        itemEl.innerHTML = `
          <span style="font-size: 12.5px; font-weight: 700; color: rgba(255,255,255,0.35); width: 28px; text-align: center; flex-shrink: 0;">${sIdx + 1}</span>
          <img src="${upgradeThumbQuality(track.artwork)}" style="width: 40px; height: 40px; border-radius: 8px; object-fit: cover; margin: 0 12px 0 6px; flex-shrink: 0;">
          <div style="display: flex; flex-direction: column; flex-grow: 1; overflow: hidden; white-space: nowrap;">
            <span style="font-size: 13px; font-weight: 700; color: white; text-overflow: ellipsis; overflow: hidden;">${escapeHtmlAttr(track.title)}</span>
            <span style="font-size: 11.5px; color: rgba(255,255,255,0.6); text-overflow: ellipsis; overflow: hidden;">${escapeHtmlAttr(track.artist)}</span>
          </div>
        `;

        itemEl.addEventListener('click', () => {
          playTrackDetails(track.id, track.title, track.artist, track.artwork, track.artistId, track.durationSec);
          renderExpandedQueue();
        });

        container.appendChild(itemEl);
      });
    }
  }
}

function updateExpandedPlayerView() {
  const overlay = document.getElementById('expanded-player-view');
  if (!overlay || overlay.classList.contains('hidden')) return;

  const track = currentQueue[activeIndex];
  if (!track) return;

  const artworkImg = document.getElementById('expanded-artwork');
  const capsuleTitle = document.getElementById('expanded-capsule-title');
  const capsuleArtist = document.getElementById('expanded-capsule-artist');
  const timeText = document.getElementById('expanded-time-text');
  const playIconExp = document.getElementById('expanded-play-icon');
  const pauseIconExp = document.getElementById('expanded-pause-icon');

  const hdArt = upgradeThumbQuality(track.artwork);
  if (artworkImg) artworkImg.src = hdArt;
  if (capsuleTitle) capsuleTitle.textContent = track.title;
  if (capsuleArtist) capsuleArtist.textContent = `${track.title} — ${track.artist}`;

  const backdropBg = document.getElementById('expanded-backdrop-bg');
  if (backdropBg) {
    backdropBg.style.backgroundImage = `url("${hdArt}")`;
  }

  if (isPlaying) {
    if (playIconExp) playIconExp.classList.add('hidden');
    if (pauseIconExp) pauseIconExp.classList.remove('hidden');
  } else {
    if (playIconExp) playIconExp.classList.remove('hidden');
    if (pauseIconExp) pauseIconExp.classList.add('hidden');
  }

  const elapsedStr = formatTime(currentPlaybackTime);
  const totalStr = formatTime(currentPlaybackDuration || track.durationSec || 180);
  if (timeText) timeText.textContent = `${elapsedStr} / -${totalStr}`;

  const bL1 = document.getElementById('blob-left-1');
  const bL2 = document.getElementById('blob-left-2');
  const bL3 = document.getElementById('blob-left-3');
  const bL4 = document.getElementById('blob-left-4');
  const bL5 = document.getElementById('blob-left-5');

  const bR1 = document.getElementById('blob-right-1');
  const bR2 = document.getElementById('blob-right-2');
  const bR3 = document.getElementById('blob-right-3');
  const bR4 = document.getElementById('blob-right-4');
  const bR5 = document.getElementById('blob-right-5');

  extractArtworkMultiPalette(track.artwork, (p) => {
    if (bL1) bL1.style.background = `radial-gradient(circle at right center, ${p.cL1} 20%, ${p.cL2 || p.cL1} 100%)`;
    if (bL2) bL2.style.background = `radial-gradient(circle at right center, ${p.cL2} 20%, ${p.cL3 || p.cL2} 100%)`;
    if (bL3) bL3.style.background = `radial-gradient(circle at right center, ${p.cL3} 20%, ${p.cL4 || p.cL3} 100%)`;
    if (bL4) bL4.style.background = `radial-gradient(circle at right center, ${p.cL4} 20%, ${p.cL5 || p.cL4} 100%)`;
    if (bL5) bL5.style.background = `radial-gradient(circle at right center, ${p.cL5} 20%, ${p.cL4 || p.cL5} 100%)`;

    if (bR1) bR1.style.background = `radial-gradient(circle at left center, ${p.cR1} 20%, ${p.cR2 || p.cR1} 100%)`;
    if (bR2) bR2.style.background = `radial-gradient(circle at left center, ${p.cR2} 20%, ${p.cR3 || p.cR2} 100%)`;
    if (bR3) bR3.style.background = `radial-gradient(circle at left center, ${p.cR3} 20%, ${p.cR4 || p.cR3} 100%)`;
    if (bR4) bR4.style.background = `radial-gradient(circle at left center, ${p.cR4} 20%, ${p.cR5 || p.cR4} 100%)`;
    if (bR5) bR5.style.background = `radial-gradient(circle at left center, ${p.cR5} 20%, ${p.cR4 || p.cR5} 100%)`;
  });

  if (expandedMode === 'queue') renderExpandedQueue();
  if (expandedMode === 'lyrics') renderExpandedLyrics();
}

function initExpandedPlayerListeners() {
  const miniGroup = document.querySelector('.player-left-group');
  if (miniGroup) {
    miniGroup.style.cursor = "pointer";
    miniGroup.addEventListener('click', (e) => {
      if (e.target.closest('#player-favorite')) return;
      openExpandedPlayerView();
    });
  }

  const btnCollapseTop = document.getElementById('btn-collapse-expanded');
  const btnCollapseBottom = document.getElementById('expanded-btn-close');
  if (btnCollapseTop) btnCollapseTop.addEventListener('click', closeExpandedPlayerView);
  if (btnCollapseBottom) btnCollapseBottom.addEventListener('click', closeExpandedPlayerView);

  const btnPlayExp = document.getElementById('expanded-btn-play');
  if (btnPlayExp) {
    btnPlayExp.addEventListener('click', () => {
      if (playBtn) playBtn.click();
      updateExpandedPlayerView();
    });
  }

  const btnPrevExp = document.getElementById('expanded-btn-prev');
  if (btnPrevExp) {
    btnPrevExp.addEventListener('click', () => {
      if (prevBtn) prevBtn.click();
      setTimeout(updateExpandedPlayerView, 300);
    });
  }

  const btnNextExp = document.getElementById('expanded-btn-next');
  if (btnNextExp) {
    btnNextExp.addEventListener('click', () => {
      if (nextBtn) nextBtn.click();
      setTimeout(updateExpandedPlayerView, 300);
    });
  }

  const btnShuffleExp = document.getElementById('expanded-btn-shuffle');
  if (btnShuffleExp) {
    btnShuffleExp.addEventListener('click', () => {
      if (shuffleBtn) shuffleBtn.click();
    });
  }

  const btnRepeatExp = document.getElementById('expanded-btn-repeat');
  if (btnRepeatExp) {
    btnRepeatExp.addEventListener('click', () => {
      if (repeatBtn) repeatBtn.click();
    });
  }

  const btnLyricsExp = document.getElementById('expanded-btn-lyrics');
  if (btnLyricsExp) {
    btnLyricsExp.addEventListener('click', () => setExpandedMode('lyrics'));
  }

  const btnQueueExp = document.getElementById('expanded-btn-queue');
  if (btnQueueExp) {
    btnQueueExp.addEventListener('click', () => setExpandedMode('queue'));
  }

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      closeExpandedPlayerView();
    }
  });
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initExpandedPlayerListeners);
} else {
  initExpandedPlayerListeners();
}

window.openExpandedPlayerView = openExpandedPlayerView;
window.closeExpandedPlayerView = closeExpandedPlayerView;
window.updateExpandedPlayerView = updateExpandedPlayerView;


function renderAppleTopPicksCarousel(title, cards) {
  const section = document.createElement('section');
  section.className = "content-section";
  section.style.marginBottom = "36px";
  section.style.padding = "0 32px";

  const sectionHeader = document.createElement('div');
  sectionHeader.className = "section-header";
  sectionHeader.style.display = "flex";
  sectionHeader.style.alignItems = "center";
  sectionHeader.style.justifyContent = "space-between";
  sectionHeader.style.marginBottom = "16px";

  sectionHeader.innerHTML = `
    <h2 class="section-title-sub" style="font-size: 24px; font-weight: 900; color: white; cursor: pointer;">${escapeHtmlAttr(title)} &gt;</h2>
    <div class="carousel-nav" style="display: flex; gap: 8px;">
      <button class="carousel-arrow prev" title="Anterior" style="width: 32px; height: 32px; border-radius: 50%; background: rgba(255,255,255,0.12); border: none; color: white; display: flex; align-items: center; justify-content: center; cursor: pointer;"><svg viewBox="0 0 24 24" width="16" height="16"><path fill="currentColor" d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/></svg></button>
      <button class="carousel-arrow next" title="Siguiente" style="width: 32px; height: 32px; border-radius: 50%; background: rgba(255,255,255,0.12); border: none; color: white; display: flex; align-items: center; justify-content: center; cursor: pointer;"><svg viewBox="0 0 24 24" width="16" height="16"><path fill="currentColor" d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/></svg></button>
    </div>
  `;

  const trackContainer = document.createElement('div');
  trackContainer.style.display = "flex";
  trackContainer.style.gap = "18px";
  trackContainer.style.overflowX = "auto";
  trackContainer.style.scrollBehavior = "smooth";
  trackContainer.style.paddingBottom = "12px";
  trackContainer.style.scrollbarWidth = "none";

  const btnPrev = sectionHeader.querySelector('.carousel-arrow.prev');
  const btnNext = sectionHeader.querySelector('.carousel-arrow.next');
  if (btnPrev) btnPrev.onclick = () => trackContainer.scrollBy({ left: -520, behavior: 'smooth' });
  if (btnNext) btnNext.onclick = () => trackContainer.scrollBy({ left: 520, behavior: 'smooth' });

  const tags = ["Made for You", "New Release", "Mood for You", "Featuring Artist", "Station for You"];

  cards.forEach((card, idx) => {
    const cardEl = document.createElement('div');
    cardEl.style.flex = "0 0 250px";
    cardEl.style.width = "250px";
    cardEl.style.height = "355px";
    cardEl.style.borderRadius = "20px";
    cardEl.style.position = "relative";
    cardEl.style.overflow = "hidden";
    cardEl.style.cursor = "pointer";
    cardEl.style.boxShadow = "0 12px 32px rgba(0,0,0,0.5)";
    cardEl.style.background = "#0c0c10";
    cardEl.style.transition = "transform 0.2s ease, box-shadow 0.2s ease";

    const tagText = tags[idx % tags.length];

    let artImg = card.artwork;
    if (!artImg || artImg.includes("data:image/svg") || artImg.length < 10) {
      artImg = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600";
    }

    cardEl.innerHTML = `
      <!-- Top Crisp Square Artwork (250px x 250px - Full 1:1 Ratio) -->
      <div style="position: absolute; top: 0; left: 0; right: 0; height: 250px; overflow: hidden; z-index: 1;">
        <img src="${artImg}" style="width: 100%; height: 100%; object-fit: cover;">
      </div>

      <!-- Bottom Inverted Image Area with Heavy Color-Only Blur (No dark effects!) -->
      <div style="position: absolute; top: 250px; bottom: 0; left: 0; right: 0; overflow: hidden; background: #0e0e12; z-index: 1;">
        <img src="${artImg}" style="width: 100%; height: 250px; object-fit: cover; transform: scaleY(-1); filter: blur(42px) saturate(1.45) brightness(1.05); opacity: 1.0;">
      </div>

      <!-- Text Container (Clean & Readable) -->
      <div style="position: absolute; bottom: 14px; left: 16px; right: 16px; color: white; z-index: 4;">
        <span style="font-size: 11px; font-weight: 800; color: #ffcc00; text-transform: uppercase; letter-spacing: 0.08em; display: block; margin-bottom: 3px; text-shadow: 0 2px 8px rgba(0,0,0,0.85);">${tagText}</span>
        <h3 style="font-size: 16.5px; font-weight: 900; color: white; line-height: 1.25; margin: 0 0 3px 0; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; letter-spacing: -0.01em; text-shadow: 0 2px 10px rgba(0,0,0,0.9);">${escapeHtmlAttr(card.title)}</h3>
        <span style="font-size: 12.5px; font-weight: 600; color: rgba(255,255,255,0.95); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; display: block; text-shadow: 0 1px 8px rgba(0,0,0,0.85);">${escapeHtmlAttr(card.artist || '')}</span>
      </div>
    `;

    cardEl.addEventListener('mouseenter', () => {
      cardEl.style.transform = "translateY(-4px)";
      cardEl.style.boxShadow = "0 16px 36px rgba(0,0,0,0.6)";
    });
    cardEl.addEventListener('mouseleave', () => {
      cardEl.style.transform = "none";
      cardEl.style.boxShadow = "0 12px 30px rgba(0,0,0,0.45)";
    });

    cardEl.addEventListener('click', () => {
      if (card.type === 'song') {
        playTrackDetails(card.id, card.title, card.artist, card.artwork, card.artistId, card.durationSec || 0);
      } else {
        loadPlaylistContents(card.id, card.title);
      }
    });

    trackContainer.appendChild(cardEl);
  });

  const titleEl = sectionHeader.querySelector('.section-title-sub');
  if (titleEl) {
    titleEl.addEventListener('click', () => {
      renderSectionDetailView(title, cards);
    });
  }

  section.appendChild(sectionHeader);
  section.appendChild(trackContainer);
  contentArea.appendChild(section);
}

function renderCarouselSection(title, cards, isArtistCircle = false) {
  const section = document.createElement('section');
  section.className = "content-section";
  section.style.marginBottom = "36px";
  section.style.padding = "0 32px";

  const sectionHeader = document.createElement('div');
  sectionHeader.className = "section-header";
  sectionHeader.style.display = "flex";
  sectionHeader.style.alignItems = "center";
  sectionHeader.style.justifyContent = "space-between";
  sectionHeader.style.marginBottom = "14px";

  sectionHeader.innerHTML = `
    <h2 class="section-title-sub" style="font-size: 22px; font-weight: 800; color: white; cursor: pointer;">${escapeHtmlAttr(title)} &gt;</h2>
    <div class="carousel-nav" style="display: flex; gap: 8px;">
      <button class="carousel-arrow prev" title="Anterior" style="width: 32px; height: 32px; border-radius: 50%; background: rgba(255,255,255,0.12); border: none; color: white; display: flex; align-items: center; justify-content: center; cursor: pointer;"><svg viewBox="0 0 24 24" width="16" height="16"><path fill="currentColor" d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/></svg></button>
      <button class="carousel-arrow next" title="Siguiente" style="width: 32px; height: 32px; border-radius: 50%; background: rgba(255,255,255,0.12); border: none; color: white; display: flex; align-items: center; justify-content: center; cursor: pointer;"><svg viewBox="0 0 24 24" width="16" height="16"><path fill="currentColor" d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/></svg></button>
    </div>
  `;

  const trackContainer = document.createElement('div');
  trackContainer.style.display = "flex";
  trackContainer.style.gap = "18px";
  trackContainer.style.overflowX = "auto";
  trackContainer.style.scrollBehavior = "smooth";
  trackContainer.style.paddingBottom = "12px";
  trackContainer.style.scrollbarWidth = "none";

  cards.forEach(card => {
    const cardEl = document.createElement('div');
    cardEl.style.flex = "0 0 190px";
    cardEl.style.width = "190px";
    cardEl.style.minWidth = "0";
    cardEl.style.maxWidth = "190px";
    cardEl.style.display = "flex";
    cardEl.style.flexDirection = "column";
    cardEl.style.cursor = "pointer";
    cardEl.style.transition = "transform 0.2s ease";

    cardEl.addEventListener('mouseenter', () => cardEl.style.transform = "translateY(-4px)");
    cardEl.addEventListener('mouseleave', () => cardEl.style.transform = "none");

    let artImg = card.artwork;
    if (!artImg || artImg.includes("data:image/svg") || artImg.length < 10) {
      artImg = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600";
    }

    if (isArtistCircle || card.type === 'artist') {
      cardEl.innerHTML = `
        <img src="${upgradeThumbQuality(artImg)}" style="width: 190px; height: 190px; border-radius: 50%; object-fit: cover; box-shadow: 0 10px 24px rgba(0,0,0,0.4); margin: 0 auto 10px auto;">
        <span style="font-size: 14px; font-weight: 700; color: white; text-align: center; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; width: 100%; display: block;">${escapeHtmlAttr(card.title)}</span>
        <span style="font-size: 11.5px; color: var(--text-muted); text-align: center; text-transform: uppercase; margin-top: 2px; display: block; font-weight: 700;">Artista</span>
      `;
      cardEl.addEventListener('click', () => loadArtistPage(card.id, card.title));
    } else {
      cardEl.innerHTML = `
        <div style="width: 190px; height: 190px; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 24px rgba(0,0,0,0.38); margin-bottom: 8px; background: #1a1a1e; flex-shrink: 0; position: relative;">
          <img src="${artImg}" style="width: 100%; height: 100%; object-fit: cover;">
          
        </div>
        <span style="font-size: 14px; font-weight: 700; color: white; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; width: 100%; display: block;">${escapeHtmlAttr(card.title)}</span>
        <span class="artist-link" style="font-size: 12px; color: var(--text-secondary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; width: 100%; display: block; margin-top: 2px;" onclick="event.stopPropagation(); loadArtistPage('${card.artistId}', '${card.artist}')">${escapeHtmlAttr(card.artist || "Música")}</span>
      `;

      cardEl.addEventListener('click', () => {
        if (card.type === 'song') {
          playTrackDetails(card.id, card.title, card.artist, card.artwork, card.artistId, card.durationSec || 0);
          return;
        }

        const isAlbumType = card.type === 'album' || card.type === 'playlist' || card.type === 'single' || card.type === 'ep'
          || (card.id && typeof card.id === 'string' && (card.id.startsWith('MPRE') || card.id.startsWith('VL') || card.id.startsWith('OLAK5uy_')))
          || /\b(álbum|album|playlist|playlists|single|singles|ep|eps|lanzamiento|aparece en|featured on)\b/i.test(title);

        if (isAlbumType) {
          loadPlaylistContents(card.id, card.title);
        } else {
          playTrackDetails(card.id, card.title, card.artist, card.artwork, card.artistId, card.durationSec || 0);
        }
      });
    }

    trackContainer.appendChild(cardEl);
  });

  const arrowPrev = sectionHeader.querySelector('.carousel-arrow.prev');
  const arrowNext = sectionHeader.querySelector('.carousel-arrow.next');
  if (arrowPrev) arrowPrev.addEventListener('click', () => trackContainer.scrollBy({ left: -440, behavior: 'smooth' }));
  if (arrowNext) arrowNext.addEventListener('click', () => trackContainer.scrollBy({ left: 440, behavior: 'smooth' }));

  const titleEl = sectionHeader.querySelector('.section-title-sub');
  if (titleEl) {
    titleEl.addEventListener('click', () => {
      renderSectionDetailView(title, cards);
    });
  }

  section.appendChild(sectionHeader);
  section.appendChild(trackContainer);
  contentArea.appendChild(section);
}




function initRightPanelPillEvents() {
  const btnQueue = document.getElementById('panel-btn-queue');
  const btnShuffle = document.getElementById('panel-btn-shuffle');
  const btnLyrics = document.getElementById('panel-btn-lyrics');
  const btnRepeat = document.getElementById('panel-btn-repeat');
  const btnAutoplay = document.getElementById('panel-btn-autoplay');
  const sleepPill = document.getElementById('right-panel-sleep-pill');
  const contentQueue = document.getElementById('panel-content-queue');
  const contentLyrics = document.getElementById('panel-content-lyrics');

  if (btnQueue) {
    btnQueue.onclick = () => {
      if (contentQueue) contentQueue.classList.remove('hidden');
      if (contentLyrics) contentLyrics.classList.add('hidden');
      btnQueue.style.background = "rgba(255,255,255,0.2)";
      btnQueue.style.color = "white";
      if (btnLyrics) {
        btnLyrics.style.background = "transparent";
        btnLyrics.style.color = "rgba(255,255,255,0.7)";
      }
    };
  }

  if (btnLyrics) {
    btnLyrics.onclick = () => {
      if (contentQueue) contentQueue.classList.add('hidden');
      if (contentLyrics) contentLyrics.classList.remove('hidden');
      btnLyrics.style.background = "rgba(255,255,255,0.2)";
      btnLyrics.style.color = "white";
      if (btnQueue) {
        btnQueue.style.background = "transparent";
        btnQueue.style.color = "rgba(255,255,255,0.7)";
      }
      loadLyricsForCurrentTrack();
    };
  }

  if (btnShuffle) {
    btnShuffle.onclick = () => {
      const isShuffled = toggleShuffle();
      btnShuffle.style.color = isShuffled ? "#ff2d55" : "rgba(255,255,255,0.7)";
    };
  }

  if (btnRepeat) {
    btnRepeat.onclick = () => {
      const mode = toggleRepeat();
      btnRepeat.style.color = mode > 0 ? "#ff2d55" : "rgba(255,255,255,0.7)";
    };
  }

  if (btnAutoplay) {
    btnAutoplay.onclick = () => {
      const active = btnAutoplay.classList.toggle('active');
      btnAutoplay.style.color = active ? "#ff2d55" : "rgba(255,255,255,0.7)";
    };
  }

  if (sleepPill) {
    sleepPill.onclick = () => {
      openSleepTimerModal();
    };
  }
}


document.addEventListener('DOMContentLoaded', initRightPanelPillEvents);

// --- Comprehensive Searchable Apple Music Preferences & Onboarding Modal ---

const ALL_GENRES = [
  "K-Pop", "J-Pop", "Jazz", "Urbano Latino", "Reggaeton", "Pop Latino", "Pop International",
  "Rock en Español", "Rock Classic", "Indie & Alternative", "Regional Mexicano", "Corridos Tumbados",
  "Trap Latino", "Hip-Hop / Rap", "R&B & Soul", "Lo-Fi & Chill", "Electronic / Dance", "EDM & House",
  "Techno", "Afrobeats", "Salsa & Bachata", "Cumbia", "Metal & Hardcore", "Punk Rock",
  "Classical / Piano", "Blues", "Country", "Reggae", "Funk & Disco", "Ambient & Study"
];

const ALL_ARTISTS = [
  // K-Pop / Asian
  "BTS", "BLACKPINK", "NewJeans", "TWICE", "Stray Kids", "SEVENTEEN", "ENHYPEN", "LE SSERAFIM", "AESPA", "TXET",
  // Latino & Reggaeton
  "Bad Bunny", "Karol G", "Feid", "Rauw Alejandro", "Quevedo", "Duki", "Young Miko", "Myke Towers",
  "Anuel AA", "Daddy Yankee", "J Balvin", "Ozuna", "Rosalía", "Maluma", "Bizarrap",
  // Regional Mexicano & Corridos
  "Peso Pluma", "Fuerza Regida", "Xavi", "Natanael Cano", "Junior H", "Grupo Frontera", "Carin León", "Christian Nodal",
  // Pop & Global Legends
  "Michael Jackson", "Taylor Swift", "Bruno Mars", "The Weeknd", "Drake", "Billie Eilish", "Dua Lipa",
  "Ariana Grande", "Justin Bieber", "Coldplay", "Queen", "Ed Sheeran", "Olivia Rodrigo", "Post Malone",
  "Travis Scott", "Kanye West", "Eminem", "Imagine Dragons", "Harry Styles", "Sabrina Carpenter", "Charli xcx", "Kendrick Lamar",
  // Jazz & Soul & Chill
  "Laufey", "Norah Jones", "Miles Davis", "Chet Baker", "John Coltrane", "Frank Sinatra", "Adele", "SZA", "Daniel Caesar"
];

// --- Mobile InicioScreen.kt Parity Home Feed ---

function renderQueue() {
  const container = document.getElementById('queue-list') || document.getElementById('queue-list-container');
  if (!container) return;
  container.innerHTML = '';

  const badge = document.getElementById('queue-item-count');
  if (badge) badge.textContent = `${currentQueue ? currentQueue.length : 0} items`;

  if (!currentQueue || currentQueue.length === 0) {
    container.innerHTML = `<div style="padding: 24px; text-align: center; color: rgba(255,255,255,0.5); font-size: 13px;">No hay canciones en la cola</div>`;
    return;
  }

  const currentTrack = currentQueue[activeIndex];
  
  if (currentTrack) {
    // 0. Title Header Row: "Now Playing" Title on Left + Items Count Pill on Right
    const titleRow = document.createElement('div');
    titleRow.style.display = "flex";
    titleRow.style.alignItems = "center";
    titleRow.style.justifyContent = "space-between";
    titleRow.style.margin = "4px 0 16px 0";

    titleRow.innerHTML = `
      <h3 style="font-size: 22px; font-weight: 900; color: white; margin: 0; line-height: 1.2;">Now Playing</h3>
      <div class="panel-item-count" id="queue-item-count-inner" style="background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.08); color: rgba(255,255,255,0.9); padding: 5px 14px; border-radius: 16px; font-size: 12px; font-weight: 700; white-space: nowrap;">${currentQueue.length} items</div>
    `;
    container.appendChild(titleRow);

    // 1. Current Playing Track Card Box
    const card = document.createElement('div');
    card.style.background = "rgba(255,255,255,0.06)";
    card.style.border = "1px solid rgba(255,255,255,0.08)";
    card.style.borderRadius = "14px";
    card.style.padding = "14px";
    card.style.display = "flex";
    card.style.alignItems = "center";
    card.style.gap = "14px";
    card.style.marginBottom = "24px";

    let artImg = currentTrack.artwork;
    if (!artImg || artImg.includes("data:image/svg") || artImg.length < 10) {
      artImg = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600";
    }

    card.innerHTML = `
      <img src="${artImg}" style="width: 48px; height: 48px; border-radius: 10px; object-fit: cover; flex-shrink: 0; box-shadow: 0 4px 12px rgba(0,0,0,0.4);">
      <div style="display: flex; flex-direction: column; overflow: hidden; white-space: nowrap; flex-grow: 1;">
        <span style="font-size: 14.5px; font-weight: 800; color: white; text-overflow: ellipsis; overflow: hidden; line-height: 1.3;">${escapeHtmlAttr(currentTrack.title)}</span>
        <span class="artist-link" style="font-size: 12px; color: rgba(255,255,255,0.65); text-overflow: ellipsis; overflow: hidden; margin-top: 3px;" onclick="event.stopPropagation(); loadArtistPage('${currentTrack.artistId}', '${currentTrack.artist}')">${escapeHtmlAttr(currentTrack.artist)} ${currentTrack.album ? `— ${escapeHtmlAttr(currentTrack.album)}` : ''}</span>
      </div>
    `;
    container.appendChild(card);

    // 2. AUTOPLAY Header Section (ONLY render for non-collection queues)
    const isCollectionQueue = currentPlaybackContext !== null || (currentQueue && currentQueue.length > 0 && currentQueue[0].isCollection);
    if (!isCollectionQueue) {
      const autoplayHeader = document.createElement('div');
      autoplayHeader.style.padding = "14px 0 10px 0";
      autoplayHeader.style.borderTop = "1px solid rgba(255,255,255,0.06)";
      autoplayHeader.style.marginBottom = "6px";

      autoplayHeader.innerHTML = `
        <div style="display: flex; align-items: center; gap: 8px; color: white; font-size: 15px; font-weight: 800; margin-bottom: 3px;">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M18.6 6.62c-1.44 0-2.8.56-3.77 1.53L12 10.96 9.17 8.15C8.2 7.18 6.84 6.62 5.4 6.62 2.42 6.62 0 9.04 0 12s2.42 5.38 5.4 5.38c1.44 0 2.8-.56 3.77-1.53L12 13.04l2.83 2.81c.97.97 2.33 1.53 3.77 1.53 2.98 0 5.4-2.42 5.4-5.38s-2.42-5.38-5.4-5.38zM5.4 15.38c-1.87 0-3.4-1.51-3.4-3.38s1.53-3.38 3.4-3.38c.91 0 1.76.35 2.38.97l2.02 2.01-2.02 2.01c-.62.62-1.47.97-2.38.97zm13.2 0c-.91 0-1.76-.35-2.38-.97l-2.02-2.01 2.02-2.01c.62-.62 1.47-.97 2.38-.97 1.87 0 3.4 1.51 3.4 3.38s-1.53 3.38-3.4 3.38z"/></svg>
          <span>AutoPlay</span>
        </div>
        <p style="font-size: 12px; color: rgba(255,255,255,0.55); margin: 0; font-weight: 500;">Similar music will continue playing.</p>
      `;
      container.appendChild(autoplayHeader);
    }
  }

  // 3. Queue List items (Tracks)
  const listWrapper = document.createElement('div');
  listWrapper.style.display = "flex";
  listWrapper.style.flexDirection = "column";
  listWrapper.style.padding = "0";

  currentQueue.forEach((track, index) => {
    const item = document.createElement('div');
    const isActive = (index === activeIndex);
    
    item.className = `queue-item ${isActive ? 'active' : ''}`;
    item.style.display = "flex";
    item.style.alignItems = "center";
    item.style.padding = "10px 0";
    item.style.borderBottom = "1px solid rgba(255,255,255,0.05)";
    item.style.cursor = "pointer";
    item.style.transition = "background-color 0.15s";

    item.addEventListener('mouseenter', () => {
      item.style.background = "rgba(255,255,255,0.04)";
    });
    item.addEventListener('mouseleave', () => {
      item.style.background = "transparent";
    });

    let artImg = track.artwork;
    if (!artImg || artImg.includes("data:image/svg") || artImg.length < 10) {
      artImg = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600";
    }

    item.innerHTML = `
      <img class="queue-item-artwork" src="${artImg}" alt="Art" style="width: 40px; height: 40px; border-radius: 6px; object-fit: cover; margin-right: 12px; flex-shrink: 0;">
      <div class="queue-item-details" style="display: flex; flex-direction: column; overflow: hidden; white-space: nowrap; flex-grow: 1;">
        <span class="queue-item-title" style="font-size: 13.5px; font-weight: 700; color: white; text-overflow: ellipsis; overflow: hidden;">${escapeHtmlAttr(track.title)}</span>
        <span class="queue-item-artist" style="font-size: 12px; color: rgba(255,255,255,0.6); text-overflow: ellipsis; overflow: hidden; margin-top: 2px;">${escapeHtmlAttr(track.artist)} ${track.album ? `— ${escapeHtmlAttr(track.album)}` : ''}</span>
      </div>
    `;

    item.addEventListener('click', () => {
      loadTrack(index, true);
    });

    listWrapper.appendChild(item);
  });

  container.appendChild(listWrapper);
}

function isOfficialSong(title) {
  if (!title) return false;
  const t = title.toLowerCase();
  const bannedKeywords = [
    'dlo', 'episode', 'episodio', 'podcast', 'top 202', 'top 201', 'mejores canciones', 
    'mix ', ' mix', 'exitos', 'grandes exitos', 'tribute', 'compilation', 'full album', 
    'discografia', 'recopilacion', 'las mejores', 'enganchados', 'set 202', 'set 201', 
    'popurri', 'megamix', 'best of', 'greatest hits mix', 'lo mejor de', 'éxitos',
    'ia productions', 'versión salsa + ia', 'multivers ai', 'ia remix'
  ];
  return !bannedKeywords.some(kw => t.includes(kw));
}

function isOfficialArtist(name, searchQuery = "") {
  if (!name) return false;
  const n = name.toLowerCase();
  const sq = searchQuery.toLowerCase().trim();

  if (sq && (n === sq || n.includes(sq))) {
    const bannedChannelKeywords = [
      'u7u', 'multivers ai', 'montgomery', ' xx', 'fan', 'ai ', 'tribute', 
      'karaoke', 'remix', 'discografia', 'enganchados', 'edit', 'slowed', 
      'reverbed', 'nightcore', 'speed up', 'unofficial', 'canal de'
    ];
    return !bannedChannelKeywords.some(kw => n.includes(kw));
  }

  const bannedChannelKeywords = [
    'u7u', 'multivers ai', 'montgomery', ' xx', 'fan', 'ai ', 'tribute', 
    'karaoke', 'remix', 'discografia', 'enganchados', 'edit', 'slowed', 
    'reverbed', 'nightcore', 'speed up', 'unofficial', 'canal de'
  ];
  return !bannedChannelKeywords.some(kw => n.includes(kw));
}

// Helper: Extract dominant color & vibrant palette from artwork image via Canvas (Cross-Origin safe)
function getAlbumDominantColor(imgUrl, callback) {
  const fallbackRes = {
    rgb: 'rgb(80, 50, 45)',
    rgba: 'rgba(80, 50, 45, 0.35)',
    hex: '#e89d6c',
    textColor: '#140d07',
    gradient: 'linear-gradient(135deg, rgb(80, 50, 45) 0%, rgba(18, 14, 18, 0.96) 100%)'
  };

  if (!imgUrl || imgUrl.includes("data:image/svg")) {
    if (typeof callback === 'function') callback(fallbackRes.rgb, fallbackRes.rgba, fallbackRes.hex, fallbackRes.textColor, fallbackRes);
    return;
  }
  const img = new Image();
  img.crossOrigin = "Anonymous";
  img.src = imgUrl;
  img.onload = () => {
    try {
      const canvas = document.createElement('canvas');
      canvas.width = 60;
      canvas.height = 60;
      const ctx = canvas.getContext('2d');
      ctx.drawImage(img, 0, 0, 60, 60);
      const imgData = ctx.getImageData(0, 0, 60, 60).data;
      
      let totalR = 0, totalG = 0, totalB = 0, count = 0;
      let maxSat = -1;
      let vibrantR = 232, vibrantG = 157, vibrantB = 108; // default warm peach/gold fallback

      for (let i = 0; i < imgData.length; i += 8) {
        const red = imgData[i];
        const green = imgData[i + 1];
        const blue = imgData[i + 2];
        const sum = red + green + blue;
        
        if (sum > 40 && sum < 710) {
          totalR += red;
          totalG += green;
          totalB += blue;
          count++;

          const maxC = Math.max(red, green, blue);
          const minC = Math.min(red, green, blue);
          const sat = maxC - minC;
          if (sat > maxSat && sum > 100 && sum < 650) {
            maxSat = sat;
            vibrantR = red;
            vibrantG = green;
            vibrantB = blue;
          }
        }
      }

      let r = 232, g = 157, b = 108;
      if (count > 0) {
        if (maxSat > 30) {
          const avgR = totalR / count;
          const avgG = totalG / count;
          const avgB = totalB / count;
          r = Math.floor(vibrantR * 0.7 + avgR * 0.3);
          g = Math.floor(vibrantG * 0.7 + avgG * 0.3);
          b = Math.floor(vibrantB * 0.7 + avgB * 0.3);
        } else {
          r = Math.floor(totalR / count);
          g = Math.floor(totalG / count);
          b = Math.floor(totalB / count);
        }
      }

      const rgbStr = `rgb(${r}, ${g}, ${b})`;
      const rgbaStr = `rgba(${r}, ${g}, ${b}, 0.35)`;
      const hexStr = `#${((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1)}`;
      const lum = (0.299 * r + 0.587 * g + 0.114 * b);
      const txtColor = lum > 130 ? '#140d07' : '#ffffff';
      const gradStr = `linear-gradient(135deg, ${rgbStr} 0%, rgba(18, 14, 18, 0.96) 100%)`;

      const colorObj = { rgb: rgbStr, rgba: rgbaStr, hex: hexStr, textColor: txtColor, gradient: gradStr };
      
      if (typeof callback === 'function') callback(rgbStr, rgbaStr, hexStr, txtColor, colorObj);
    } catch (e) {
      if (typeof callback === 'function') callback(fallbackRes.rgb, fallbackRes.rgba, fallbackRes.hex, fallbackRes.textColor, fallbackRes);
    }
  };
  img.onerror = () => {
    if (typeof callback === 'function') callback(fallbackRes.rgb, fallbackRes.rgba, fallbackRes.hex, fallbackRes.textColor, fallbackRes);
  };
}

// Helper: Update album view active row highlight with predominant cover art color
function updateAlbumTrackRowsHighlight(activeIdx) {
  const trackListContainer = document.getElementById('album-track-list');
  if (!trackListContainer) return;

  const activeTrackObj = (typeof activeIndex === 'number' && Array.isArray(currentQueue) && currentQueue[activeIndex]) ? currentQueue[activeIndex] : null;
  const rows = trackListContainer.querySelectorAll('.album-track-row');
  const domHex = trackListContainer.dataset.dominantColor || "#e89d6c";
  const textColor = trackListContainer.dataset.textColor || "#140d07";

  rows.forEach((row, idx) => {
    const rowTrackId = row.dataset.trackId;
    const isCurrent = (activeTrackObj && rowTrackId) ? (activeTrackObj.id === rowTrackId) : (idx === activeIdx);
    if (isCurrent) {
      row.style.background = domHex;
      row.style.color = textColor;
      row.style.borderRadius = "24px";
      row.style.boxShadow = "0 4px 18px rgba(0,0,0,0.25)";
      
      const numSpan = row.querySelector('.track-idx-span');
      if (numSpan) {
        numSpan.innerHTML = `
          <div class="eq-wave-container" style="display: inline-flex; align-items: flex-end; gap: 2px; height: 14px; width: 14px; justify-content: center; vertical-align: middle;">
            <span style="width: 3px; height: 100%; background: ${textColor}; border-radius: 2px; animation: eqAnim 0.8s infinite ease-in-out alternate;"></span>
            <span style="width: 3px; height: 60%; background: ${textColor}; border-radius: 2px; animation: eqAnim 0.8s infinite ease-in-out alternate 0.2s;"></span>
            <span style="width: 3px; height: 80%; background: ${textColor}; border-radius: 2px; animation: eqAnim 0.8s infinite ease-in-out alternate 0.4s;"></span>
          </div>
        `;
      }
      const titleSpan = row.querySelector('.track-title-span');
      if (titleSpan) {
        titleSpan.style.color = textColor;
        titleSpan.style.fontWeight = "800";
      }
      const durSpan = row.querySelector('.track-dur-span');
      if (durSpan) {
        durSpan.style.color = textColor;
        durSpan.style.opacity = "0.85";
      }
      const optDiv = row.querySelector('.track-opt-div');
      if (optDiv) {
        optDiv.style.color = textColor;
      }
    } else {
      row.style.background = "transparent";
      row.style.color = "white";
      row.style.borderRadius = "12px";
      row.style.boxShadow = "none";

      const numSpan = row.querySelector('.track-idx-span');
      if (numSpan) {
        numSpan.textContent = `${idx + 1}`;
        numSpan.style.color = "rgba(255,255,255,0.5)";
      }
      const titleSpan = row.querySelector('.track-title-span');
      if (titleSpan) {
        titleSpan.style.color = "white";
        titleSpan.style.fontWeight = "700";
      }
      const durSpan = row.querySelector('.track-dur-span');
      if (durSpan) {
        durSpan.style.color = "rgba(255,255,255,0.55)";
        durSpan.style.opacity = "1";
      }
      const optDiv = row.querySelector('.track-opt-div');
      if (optDiv) {
        optDiv.style.color = "rgba(255,255,255,0.5)";
      }
    }
  });
}


async function loadPlaylistContents(playlistId, playlistTitle = "Álbum", shouldPushHistory = true) {
  setHeaderVisible(true);
  setHeaderSearchPillVisible(false);
  contentArea.innerHTML = `<div class="loading-spinner-container"><div class="spinner"></div><p>Cargando ${escapeHtmlAttr(playlistTitle)}...</p></div>`;
  
  if (shouldPushHistory) {
    pushNavigation({ name: 'playlist', params: { playlistId, playlistTitle } });
  }

  // Safe redirect if artist ID passed by mistake
  if (playlistId && typeof playlistId === 'string' && playlistId.startsWith('UC')) {
    loadArtistPage(playlistId, playlistTitle);
    return;
  }

  let title = playlistTitle || "Álbum";
  let artistName = "Artista";
  let artwork = "";
  let releaseYear = "";
  let itemTypeLabel = "ÁLBUM";
  let items = [];

  if (playlistId && (playlistId.startsWith('VL') || playlistId.startsWith('PL'))) {
    itemTypeLabel = "PLAYLIST";
  }

  // Safe depth-bounded recursive track extractor (depth <= 16)
  function extractTracksRecursive(obj, itemsList = [], depth = 0) {
    if (!obj || depth > 16) return itemsList;
    if (typeof obj === 'object') {
      if (obj.musicResponsiveListItemRenderer) {
        const r = obj.musicResponsiveListItemRenderer;
        const vId = r.navigationEndpoint?.watchEndpoint?.videoId 
                 || r.playlistItemData?.videoId 
                 || r.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId
                 || r.doubleTapCommand?.watchEndpoint?.videoId;
        if (vId) {
          const flex0 = r.flexColumns?.[0]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs || [];
          const flex1 = r.flexColumns?.[1]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs || [];
          const flex2 = r.flexColumns?.[2]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs || [];

          const tTitle = flex0.map(rn => rn.text).join("") || "Canción";
          const tArtist = flex1.map(rn => rn.text).join("") || artistName;
          const tAlbum = flex2.map(rn => rn.text).join("") || title;

          let tDur = "";
          const fixed = r.fixedColumns?.[0]?.musicResponsiveListItemFixedColumnRenderer?.text?.runs?.[0]?.text;
          if (fixed && fixed.includes(':')) tDur = fixed.trim();

          const tThumb = upgradeThumbQuality(extractThumbnail(r)) || artwork;

          itemsList.push({
            id: vId,
            title: tTitle,
            artist: tArtist,
            album: tAlbum,
            duration: tDur || "3:30",
            artwork: tThumb,
            type: 'song'
          });
        }
      } else if (obj.playlistPanelVideoRenderer) {
        const r = obj.playlistPanelVideoRenderer;
        const vId = r.videoId;
        if (vId) {
          const tTitle = r.title?.runs?.[0]?.text || "Canción";
          const tArtist = r.shortBylineText?.runs?.[0]?.text || artistName;
          const tDur = r.lengthText?.runs?.[0]?.text || "3:30";
          const tThumb = upgradeThumbQuality(extractThumbnail(r)) || artwork;
          itemsList.push({
            id: vId,
            title: tTitle,
            artist: tArtist,
            album: title,
            duration: tDur,
            artwork: tThumb,
            type: 'song'
          });
        }
      } else {
        for (const key in obj) {
          if (Object.prototype.hasOwnProperty.call(obj, key)) {
            if (key === 'trackingParams' || key === 'responseContext' || key === 'microformat' || key === 'serviceTrackingParams' || key === 'background' || key === 'icon' || key === 'menu') continue;
            extractTracksRecursive(obj[key], itemsList, depth + 1);
          }
        }
      }
    }
    return itemsList;
  }

  try {
    const validId = (playlistId && typeof playlistId === 'string') ? playlistId : "";
    const targetBrowseId = validId.startsWith('VL') ? validId : (validId.startsWith('MPREb_') || validId.startsWith('OLAK5uy_') ? validId : (validId.startsWith('PL') ? `VL${validId}` : validId));
    
    let data = null;
    if (targetBrowseId) {
      data = await callInnerTubeAPI('browse', { browseId: targetBrowseId }, WEB_CONTEXT).catch(() => null);
    }

    if (data) {
      let headerObj = data.header?.musicDetailHeaderRenderer 
                   || data.header?.musicEditablePlaylistDetailHeaderRenderer?.header?.musicDetailHeaderRenderer
                   || data.header?.musicVisualHeaderRenderer;

      // Recursive search for musicResponsiveHeaderRenderer or musicDetailHeaderRenderer inside data
      if (!headerObj) {
        function searchHeader(obj) {
          if (!obj || typeof obj !== 'object' || headerObj) return;
          if (obj.musicResponsiveHeaderRenderer) {
            headerObj = obj.musicResponsiveHeaderRenderer;
            return;
          }
          if (obj.musicDetailHeaderRenderer) {
            headerObj = obj.musicDetailHeaderRenderer;
            return;
          }
          if (obj.musicEditablePlaylistDetailHeaderRenderer) {
            headerObj = obj.musicEditablePlaylistDetailHeaderRenderer.header?.musicDetailHeaderRenderer || obj.musicEditablePlaylistDetailHeaderRenderer;
            return;
          }
          if (Array.isArray(obj)) {
            for (const item of obj) searchHeader(item);
          } else {
            for (const k in obj) {
              if (Object.prototype.hasOwnProperty.call(obj, k)) {
                if (k === 'trackingParams' || k === 'responseContext' || k === 'microformat') continue;
                searchHeader(obj[k]);
              }
            }
          }
        }
        searchHeader(data);
      }

      const micro = data?.microformat?.microformatDataRenderer;

      if (headerObj) {
        title = headerObj.title?.runs?.[0]?.text || micro?.title || playlistTitle;

        // Subtitle runs (contains Album type, year, artist)
        const subRuns = headerObj.subtitle?.runs || headerObj.secondSubtitle?.runs || [];
        const straplineText = headerObj.straplineTextOne?.runs?.[0]?.text;
        if (straplineText) artistName = straplineText;

        subRuns.forEach(r => {
          const txt = (r.text || "").trim();
          if (/^\d{4}$/.test(txt)) {
            releaseYear = txt;
          } else if (['álbum', 'album', 'ep', 'single', 'sencillo', 'playlist', 'lista de reproducción'].some(k => txt.toLowerCase().includes(k))) {
            if (txt.toLowerCase().includes('ep')) itemTypeLabel = "EP";
            else if (txt.toLowerCase().includes('single') || txt.toLowerCase().includes('sencillo')) itemTypeLabel = "SINGLE";
            else if (txt.toLowerCase().includes('playlist')) itemTypeLabel = "PLAYLIST";
          } else if (txt.length > 1 && txt !== '•' && artistName === "Artista") {
            artistName = txt;
          }
        });

        // Search year in secondSubtitle if not found
        if (!releaseYear && headerObj.secondSubtitle?.runs) {
          headerObj.secondSubtitle.runs.forEach(r => {
            const txt = (r.text || "").trim();
            if (/^\d{4}$/.test(txt)) releaseYear = txt;
          });
        }

        // Search year in description / microformat if not found
        if (!releaseYear && micro?.description) {
          const mYear = micro.description.match(/\b(19\d\d|20\d\d)\b/);
          if (mYear) releaseYear = mYear[1];
        }

        // Extract artwork from headerObj or data
        artwork = upgradeThumbQuality(extractThumbnail(headerObj)) || upgradeThumbQuality(extractThumbnail(data));
      }

      if (!artwork || artwork.includes("data:image/svg") || artwork.length < 10) {
        if (micro?.thumbnail?.thumbnails?.length > 0) {
          artwork = upgradeThumbQuality(micro.thumbnail.thumbnails[micro.thumbnail.thumbnails.length - 1].url);
        }
      }

      // Extract tracks from browse data
      const rawExtracted = extractTracksRecursive(data, []);
      const seen = new Set();
      rawExtracted.forEach(it => {
        if (!seen.has(it.id)) {
          seen.add(it.id);
          items.push(it);
        }
      });
    }

    // Fallback 1: Watch Next API if validId is a videoId and items empty
    if (items.length === 0 && validId) {
      const nextData = await callInnerTubeAPI('next', { videoId: validId }, WEB_CONTEXT).catch(() => null);
      if (nextData) {
        const rawNext = extractTracksRecursive(nextData, []);
        const seen = new Set();
        rawNext.forEach(it => {
          if (!seen.has(it.id)) {
            seen.add(it.id);
            items.push(it);
          }
        });
      }
    }

    // Fallback 2: Search API if items still empty
    if (items.length === 0 && title) {
      const searchRes = await callInnerTubeAPI('search', { query: `${title} ${artistName !== 'Artista' ? artistName : ''}` }, WEB_CONTEXT).catch(() => null);
      if (searchRes) {
        const rawSearch = extractTracksRecursive(searchRes, []);
        const seen = new Set();
        rawSearch.forEach(s => {
          if (!seen.has(s.id)) {
            seen.add(s.id);
            items.push(s);
          }
        });
      }
    }
  } catch (err) {
    console.warn("Error during album fetch, proceeding with fallback display:", err);
  }

  if (!artwork || artwork.includes("data:image/svg") || artwork.length < 10) {
    artwork = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600";
  }

  if (items.length === 0) {
    items = FALLBACK_TRACKS;
  }

  // Ensure all collection items inherit high quality artwork and collection flag
  items = items.map(it => ({
    ...it,
    artwork: (it.artwork && !it.artwork.includes("data:image/svg") && it.artwork.length > 10) ? upgradeThumbQuality(it.artwork) : artwork,
    isCollection: true,
    collectionTitle: title,
    collectionType: itemTypeLabel
  }));

  // ALWAYS RENDER DETAIL VIEW MATCHING SCREENSHOT #2
  contentArea.innerHTML = '';
  const wrapper = document.createElement('div');
  wrapper.style.width = "100%";
  wrapper.style.maxWidth = "100%";
  wrapper.style.margin = "0";
  wrapper.style.padding = "20px 24px 60px 24px";
  wrapper.style.boxSizing = "border-box";

  // 1. Header Card Container with dynamic dominant color background & bottom difuminado fade
  const headerCard = document.createElement('div');
  headerCard.id = "album-header-card";
  headerCard.style.display = "flex";
  headerCard.style.gap = "36px";
  headerCard.style.alignItems = "center";
  headerCard.style.padding = "36px 40px 48px 40px";
  headerCard.style.borderRadius = "24px 24px 16px 16px";
  headerCard.style.marginBottom = "0px";
  headerCard.style.background = "linear-gradient(180deg, rgba(80, 50, 45, 0.95) 0%, rgba(50, 30, 28, 0.6) 75%, transparent 100%)";
  headerCard.style.maskImage = "linear-gradient(to bottom, #000 0%, #000 68%, rgba(0, 0, 0, 0.6) 88%, transparent 100%)";
  headerCard.style.webkitMaskImage = "linear-gradient(to bottom, #000 0%, #000 68%, rgba(0, 0, 0, 0.6) 88%, transparent 100%)";
  headerCard.style.boxShadow = "0 10px 40px rgba(0,0,0,0.4)";
  headerCard.style.transition = "background 0.5s ease";

  let collectionColorObj = { hex: '#e89d6c', textColor: '#140d07', gradient: 'linear-gradient(180deg, rgb(80, 50, 45) 0%, rgba(50, 30, 28, 0.6) 75%, transparent 100%)' };

  headerCard.innerHTML = `
    <div style="width: 260px; height: 260px; border-radius: 20px; overflow: hidden; box-shadow: 0 20px 48px rgba(0,0,0,0.65); flex-shrink: 0; background: #1a1a1e;">
      <img src="${artwork}" style="width: 100%; height: 100%; object-fit: cover;">
    </div>
    <div style="display: flex; flex-direction: column; gap: 14px; color: white; flex-grow: 1; min-width: 0;">
      <h1 style="font-size: 38px; font-weight: 900; line-height: 1.1; margin: 0; color: white; letter-spacing: -0.02em; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;">${escapeHtmlAttr(title)}</h1>
      
      <div style="display: flex; align-items: center; gap: 8px;">
        <div style="width: 24px; height: 24px; border-radius: 50%; background: #ff2d55; display: flex; align-items: center; justify-content: center; color: white; font-weight: 800; font-size: 12px; flex-shrink: 0;">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
        </div>
        <span style="font-size: 15px; font-weight: 700; color: white;">${escapeHtmlAttr(artistName)}</span>
        <div style="width: 18px; height: 18px; border-radius: 50%; background: rgba(255,255,255,0.2); display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 800; color: white; cursor: pointer; margin-left: 2px;" title="Seguir">+</div>
      </div>

      <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
        <span style="font-size: 11px; font-weight: 800; text-transform: uppercase; background: rgba(255,255,255,0.15); padding: 5px 12px; border-radius: 12px; color: rgba(255,255,255,0.9); letter-spacing: 0.05em;">${itemTypeLabel}</span>
        ${releaseYear ? `<span style="font-size: 11px; font-weight: 800; background: rgba(255,255,255,0.15); padding: 5px 12px; border-radius: 12px; color: rgba(255,255,255,0.9);">${releaseYear}</span>` : ''}
        <span style="font-size: 11px; font-weight: 800; text-transform: uppercase; background: rgba(255,255,255,0.15); padding: 5px 12px; border-radius: 12px; color: rgba(255,255,255,0.9); letter-spacing: 0.05em;">🎵 ${items.length} TEMAS</span>
      </div>

      <div style="display: flex; align-items: center; gap: 12px; margin-top: 6px; flex-wrap: wrap;">
        <button id="btn-album-play-all" style="background: #ffffff; border: none; color: #000000; padding: 12px 28px; border-radius: 24px; font-size: 14px; font-weight: 800; cursor: pointer; display: flex; align-items: center; gap: 8px; box-shadow: 0 8px 24px rgba(0,0,0,0.3); transition: transform 0.15s ease;">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M8 5v14l11-7z"/></svg> Reproducir
        </button>
        
        <button id="btn-album-shuffle-all" style="background: rgba(255,255,255,0.15); border: none; color: white; padding: 12px 24px; border-radius: 24px; font-size: 14px; font-weight: 700; cursor: pointer; display: flex; align-items: center; gap: 8px; backdrop-filter: blur(10px); transition: background 0.15s ease;">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M10.59 9.17L5.41 4 4 5.41l5.17 5.17 1.42-1.41zM14.5 4l2.04 2.04 17.96 7.45 20 9.5V4h-5.5zm.33 9.41l-1.41 1.41 3.13 3.13L14.5 20H20v-5.5l-2.04 2.04-3.13-3.13z"/></svg> Aleatorio
        </button>

        <button style="background: rgba(255,255,255,0.15); border: none; color: white; padding: 12px 24px; border-radius: 24px; font-size: 14px; font-weight: 700; cursor: pointer; display: flex; align-items: center; gap: 6px;">
          + Agregar
        </button>

        <button style="width: 42px; height: 42px; border-radius: 50%; background: rgba(255,255,255,0.15); border: none; color: white; cursor: pointer; display: flex; align-items: center; justify-content: center;" title="Más opciones">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M6 10c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm12 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm-6 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"/></svg>
        </button>
      </div>
    </div>
  `;

  wrapper.appendChild(headerCard);

  // 2. Desktop Tracks List (Seamlesly connected below the header card difuminado)
  const trackList = document.createElement('div');
  trackList.id = "album-track-list";
  trackList.style.display = "flex";
  trackList.style.flexDirection = "column";
  trackList.style.gap = "4px";
  trackList.style.marginTop = "0px";
  trackList.style.paddingTop = "12px";

  // Extract dynamic dominant color from artwork
  getAlbumDominantColor(artwork, (domRgb, domRgba, domHex, txtColor, cObj) => {
    collectionColorObj = cObj || { rgb: domRgb, rgba: domRgba, hex: domHex, textColor: txtColor, gradient: `linear-gradient(180deg, ${domRgb} 0%, ${domRgba} 75%, transparent 100%)` };
    if (headerCard) {
      headerCard.style.background = `linear-gradient(180deg, ${domRgb} 0%, ${domRgb} 45%, ${domRgba} 80%, transparent 100%)`;
    }
    if (trackList) {
      trackList.dataset.dominantColor = collectionColorObj.hex;
      trackList.dataset.textColor = collectionColorObj.textColor;
      updateAlbumTrackRowsHighlight(activeIndex);
    }
  });

  const activeTrackObj = (typeof activeIndex === 'number' && Array.isArray(currentQueue) && currentQueue[activeIndex]) ? currentQueue[activeIndex] : null;
  items.forEach((t, idx) => {
    const isCurrentPlaying = activeTrackObj && activeTrackObj.id === t.id && (typeof isPlaying !== 'undefined' ? isPlaying : false);

    const row = document.createElement('div');
    row.className = "album-track-row";
    row.dataset.trackId = t.id;
    row.style.display = "flex";
    row.style.alignItems = "center";
    row.style.padding = "12px 18px";
    row.style.borderRadius = "12px";
    row.style.background = isCurrentPlaying ? (trackList.dataset.dominantColor || "#e89d6c") : "transparent";
    row.style.color = isCurrentPlaying ? (trackList.dataset.textColor || "#140d07") : "white";
    row.style.cursor = "pointer";
    row.style.transition = "all 0.15s ease";

    row.addEventListener('mouseenter', () => {
      const activeObjNow = (typeof activeIndex === 'number' && Array.isArray(currentQueue) && currentQueue[activeIndex]) ? currentQueue[activeIndex] : null;
      if (!activeObjNow || activeObjNow.id !== t.id) row.style.background = "rgba(255,255,255,0.07)";
    });
    row.addEventListener('mouseleave', () => {
      const activeObjNow = (typeof activeIndex === 'number' && Array.isArray(currentQueue) && currentQueue[activeIndex]) ? currentQueue[activeIndex] : null;
      if (!activeObjNow || activeObjNow.id !== t.id) row.style.background = "transparent";
    });

    row.innerHTML = `
      <span class="track-idx-span" style="width: 32px; font-size: 14px; font-weight: 700; color: ${isCurrentPlaying ? (trackList.dataset.textColor || '#140d07') : 'rgba(255,255,255,0.5)'};">${isCurrentPlaying ? '📊' : (idx + 1)}</span>
      
      <div style="display: flex; flex-direction: column; flex-grow: 1; min-width: 0; margin-right: 16px;">
        <span class="track-title-span" style="font-size: 15px; font-weight: 700; color: ${isCurrentPlaying ? (trackList.dataset.textColor || '#140d07') : 'white'}; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${escapeHtmlAttr(t.title)}</span>
      </div>

      <span class="track-dur-span" style="font-size: 13.5px; color: ${isCurrentPlaying ? (trackList.dataset.textColor || '#140d07') : 'rgba(255,255,255,0.55)'}; font-weight: 600; margin-right: 16px;">${t.duration || '3:30'}</span>

      <div class="track-opt-div" style="color: ${isCurrentPlaying ? (trackList.dataset.textColor || '#140d07') : 'rgba(255,255,255,0.5)'}; cursor: pointer; padding: 4px;" title="Opciones">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"/></svg>
      </div>
    `;

    row.onclick = () => {
      currentPlaybackContext = { type: itemTypeLabel, title: title, artwork: artwork, colorData: collectionColorObj };
      currentQueue = items.map(it => ({ ...it, artwork: artwork, isCollection: true }));
      loadTrack(idx, true);
      updateAlbumTrackRowsHighlight(idx);
      renderQueue();
    };

    trackList.appendChild(row);
  });

  wrapper.appendChild(trackList);
  contentArea.appendChild(wrapper);

  // Button event bindings
  const btnPlayAll = headerCard.querySelector('#btn-album-play-all');
  if (btnPlayAll) {
    btnPlayAll.onclick = () => {
      if (items.length > 0) {
        currentPlaybackContext = { type: itemTypeLabel, title: title, artwork: artwork, colorData: collectionColorObj };
        currentQueue = items.map(it => ({ ...it, artwork: artwork, isCollection: true }));
        loadTrack(0, true);
        updateAlbumTrackRowsHighlight(0);
        renderQueue();
      }
    };
  }

  const btnShuffleAll = headerCard.querySelector('#btn-album-shuffle-all');
  if (btnShuffleAll) {
    btnShuffleAll.onclick = () => {
      if (items.length > 0) {
        currentPlaybackContext = { type: itemTypeLabel, title: title, artwork: artwork, colorData: collectionColorObj };
        const shuffled = [...items].sort(() => Math.random() - 0.5);
        currentQueue = shuffled.map(it => ({ ...it, artwork: artwork, isCollection: true }));
        loadTrack(0, true);
        updateAlbumTrackRowsHighlight(0);
        renderQueue();
      }
    };
  }
}


async function fetchRealLyrics(track) {
  if (!track || !track.title) return [];
  
  const container = document.getElementById('lyrics-content');
  const expandedList = document.getElementById('expanded-lyrics-list');
  if (container) container.innerHTML = '<div style="padding: 24px; text-align: center; color: rgba(255,255,255,0.6); font-weight: 600;">Buscando letras en tiempo real...</div>';
  if (expandedList) expandedList.innerHTML = '<div style="padding: 24px; text-align: center; color: rgba(255,255,255,0.6); font-weight: 600;">Buscando letras en tiempo real...</div>';

  const cleanTitle = track.title.replace(/\(.*?\)|\[.*?\]/g, '').trim();
  const cleanArtist = (track.artist || '').replace(/\(.*?\)|\[.*?\]/g, '').trim();

  // Fast LRCLIB API direct match
  try {
    const url = `https://lrclib.net/api/get?track_name=${encodeURIComponent(cleanTitle)}&artist_name=${encodeURIComponent(cleanArtist)}`;
    const res = await fetch(url);
    if (res.ok) {
      const data = await res.json();
      if (data.syncedLyrics || data.plainLyrics) {
        return parseLrcContent(data.syncedLyrics || data.plainLyrics);
      }
    }
  } catch(e) {}

  // Fast LRCLIB search fallback
  try {
    const qUrl = `https://lrclib.net/api/search?q=${encodeURIComponent(cleanTitle + ' ' + cleanArtist)}`;
    const qRes = await fetch(qUrl);
    if (qRes.ok) {
      const results = await qRes.json();
      if (results && results.length > 0) {
        const best = results.find(r => r.syncedLyrics || r.plainLyrics) || results[0];
        if (best.syncedLyrics || best.plainLyrics) {
          return parseLrcContent(best.syncedLyrics || best.plainLyrics);
        }
      }
    }
  } catch(e) {}

  // InnerTube Next / Lyrics tab fallback
  try {
    if (track.id) {
      const nextData = await callInnerTubeAPI('next', { videoId: track.id }, WEB_CONTEXT).catch(() => null);
      const tabs = nextData?.contents?.singleColumnMusicWatchNextResultsRenderer?.tabbedRenderer?.watchNextTabbedResultsRenderer?.tabs;
      const lyricsBrowseId = tabs?.[1]?.tabRenderer?.endpoint?.browseEndpoint?.browseId;
      if (lyricsBrowseId) {
        const lyricsData = await callInnerTubeAPI('browse', { browseId: lyricsBrowseId }, WEB_CONTEXT).catch(() => null);
        const descriptionRuns = lyricsData?.contents?.sectionListRenderer?.contents?.[0]?.musicDescriptionShelfRenderer?.description?.runs;
        if (descriptionRuns && descriptionRuns.length > 0) {
          const plain = descriptionRuns.map(r => r.text).join("");
          return parseLrcContent(plain);
        }
      }
    }
  } catch(e) {}

  return [];
}

async function loadLyricsForCurrentTrack() {
  const track = currentQueue[activeIndex];
  if (!track) return;

  const container = document.getElementById('lyrics-content');
  const expandedList = document.getElementById('expanded-lyrics-list');

  const lines = await fetchRealLyrics(track);
  parsedLyricsLines = [];
  currentActiveLyricIdx = -1;

  if (!lines || lines.length === 0) {
    const noLyricsHtml = `<div style="padding: 40px; text-align: center; color: rgba(255,255,255,0.5); font-size: 14px;">No hay letras disponibles para "${escapeHtmlAttr(track.title)}".</div>`;
    if (container) container.innerHTML = noLyricsHtml;
    if (expandedList) expandedList.innerHTML = noLyricsHtml;
    return;
  }

  const renderLyricsNodes = (targetEl) => {
    if (!targetEl) return;
    targetEl.innerHTML = '';
    const wrapper = document.createElement('div');
    wrapper.style.display = "flex";
    wrapper.style.flexDirection = "column";
    wrapper.style.gap = "18px";
    wrapper.style.padding = "24px 16px";

    lines.forEach((lineObj, idx) => {
      const p = document.createElement('p');
      p.style.fontSize = "22px";
      p.style.fontWeight = "700";
      p.style.color = "rgba(255,255,255,0.35)";
      p.style.margin = "0";
      p.style.lineHeight = "1.35";
      p.style.cursor = "pointer";
      p.style.transition = "all 0.25s cubic-bezier(0.2, 0.9, 0.3, 1)";
      p.textContent = lineObj.text;

      if (lineObj.time >= 0) {
        p.onclick = () => {
          if (audioPlayer) audioPlayer.currentTime = lineObj.time;
          currentPlaybackTime = Math.floor(lineObj.time);
          updateTimelineUI();
        };
      }

      wrapper.appendChild(p);
      if (targetEl.id === 'lyrics-content') {
        parsedLyricsLines.push({ timeSec: lineObj.time, element: p });
      }
    });

    targetEl.appendChild(wrapper);
  };

  renderLyricsNodes(container);
  renderLyricsNodes(expandedList);
}

function updateLyricsHighlight(currentTimeSec) {
  if (!parsedLyricsLines || parsedLyricsLines.length === 0) return;
  let activeIdx = -1;
  for (let i = 0; i < parsedLyricsLines.length; i++) {
    if (parsedLyricsLines[i].timeSec >= 0 && currentTimeSec >= parsedLyricsLines[i].timeSec) {
      activeIdx = i;
    } else if (parsedLyricsLines[i].timeSec > currentTimeSec) {
      break;
    }
  }

  if (activeIdx !== currentActiveLyricIdx && activeIdx >= 0) {
    currentActiveLyricIdx = activeIdx;
    parsedLyricsLines.forEach((item, idx) => {
      if (idx === activeIdx) {
        item.element.style.color = "#ffffff";
        item.element.style.fontSize = "26px";
        item.element.style.fontWeight = "900";
        item.element.style.opacity = "1";
        item.element.style.textShadow = "0 0 24px rgba(255,255,255,0.7)";
        item.element.style.transform = "scale(1.04)";
        item.element.scrollIntoView({ behavior: 'smooth', block: 'center' });
      } else {
        item.element.style.color = "rgba(255,255,255,0.32)";
        item.element.style.fontSize = "22px";
        item.element.style.fontWeight = "700";
        item.element.style.opacity = "0.45";
        item.element.style.textShadow = "none";
        item.element.style.transform = "none";
      }
    });
  }
}











function getSearchHistory() {
  try {
    return JSON.parse(localStorage.getItem('raymusic_search_history') || '[]');
  } catch(e) { return []; }
}

function saveSearchQuery(query, artwork = "") {
  if (!query || query.trim().length < 2) return;
  const q = query.trim();
  let list = getSearchHistory().filter(x => (typeof x === 'string' ? x : x.query).toLowerCase() !== q.toLowerCase());
  list.unshift({ query: q, artwork: artwork || "" });
  if (list.length > 10) list = list.slice(0, 10);
  localStorage.setItem('raymusic_search_history', JSON.stringify(list));
}

function removeSearchQuery(query) {
  let list = getSearchHistory().filter(x => (typeof x === 'string' ? x : x.query).toLowerCase() !== query.toLowerCase());
  localStorage.setItem('raymusic_search_history', JSON.stringify(list));
  renderExploreCategoriesView();
}

function renderExploreCategoriesView() {
  setHeaderVisible(true);
  setHeaderSearchPillVisible(true);
  document.getElementById('page-title').textContent = "Explorar categorías";

  const historyList = getSearchHistory();
  
  let historyCardHtml = '';
  if (historyList.length > 0) {
    historyCardHtml = `
      <div style="background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.08); border-radius: 20px; padding: 20px 24px; margin-bottom: 28px; box-shadow: 0 8px 24px rgba(0,0,0,0.3);">
        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px;">
          <h3 style="font-size: 17px; font-weight: 800; color: white; margin: 0;">Búsquedas recientes</h3>
          <button id="btn-clear-search-history" style="background: none; border: none; color: rgba(255,255,255,0.5); font-size: 12px; font-weight: 700; cursor: pointer;">Limpiar historial</button>
        </div>
        <div style="display: flex; flex-wrap: wrap; gap: 10px;">
          ${historyList.map(item => {
            const q = typeof item === 'string' ? item : item.query;
            const art = typeof item === 'string' ? '' : item.artwork;
            const imgHtml = art ? `<img src="${art}" style="width: 22px; height: 22px; border-radius: 50%; object-fit: cover;">` : '';
            return `
              <div class="search-history-pill" data-query="${escapeHtmlAttr(q)}" style="background: rgba(255,255,255,0.08); border: 1px solid rgba(255,255,255,0.12); color: white; padding: 6px 14px; border-radius: 20px; font-size: 13.5px; font-weight: 700; cursor: pointer; display: flex; align-items: center; gap: 8px; transition: all 0.15s ease;">
                ${imgHtml}
                <span>${escapeHtmlAttr(q)}</span>
                <span class="remove-history-btn" data-del="${escapeHtmlAttr(q)}" style="color: rgba(255,255,255,0.5); font-weight: 800; margin-left: 4px;">✕</span>
              </div>
            `;
          }).join('')}
        </div>
      </div>
    `;
  }

  contentArea.innerHTML = `
    <div style="width: 100%; box-sizing: border-box; padding: 28px 36px 48px 36px; animation: fadeIn 0.25s ease-out; color: white;">
      ${historyCardHtml}
      <h1 style="font-size: 26px; font-weight: 900; color: white; margin-bottom: 24px;">Explorar categorías</h1>
      <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(210px, 1fr)); gap: 18px; width: 100%;">
        ${DATOS_CATEGORIAS.map(cat => `
          <div class="category-card-item" data-query="${cat.name}" style="height: 125px; border-radius: 16px; overflow: hidden; position: relative; cursor: pointer; background: rgba(0,0,0,0.4); box-shadow: 0 8px 24px rgba(0,0,0,0.4); transition: transform 0.2s ease;">
            <img src="${cat.url}" style="position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover;">
            <div style="position: absolute; inset: 0; background: linear-gradient(to top, rgba(0,0,0,0.6) 0%, transparent 60%);"></div>
            <span style="position: absolute; bottom: 14px; left: 16px; font-size: 15px; font-weight: 800; color: white; text-shadow: 0 2px 10px rgba(0,0,0,0.9);">${cat.name}</span>
          </div>
        `).join('')}
      </div>
    </div>
  `;

  const clearBtn = document.getElementById('btn-clear-search-history');
  if (clearBtn) {
    clearBtn.onclick = () => {
      localStorage.removeItem('raymusic_search_history');
      renderExploreCategoriesView();
    };
  }

  contentArea.querySelectorAll('.search-history-pill').forEach(pill => {
    pill.onclick = (e) => {
      if (e.target.classList.contains('remove-history-btn')) {
        e.stopPropagation();
        removeSearchQuery(e.target.dataset.del);
      } else {
        const q = pill.dataset.query;
        const searchInput = document.getElementById('header-search-input');
        if (searchInput) searchInput.value = q;
        performSearch(q);
      }
    };
  });

  // Resolve real artist/song artwork for search history pills lacking thumbnails
  historyList.forEach(async (item) => {
    const q = typeof item === 'string' ? item : item.query;
    const art = typeof item === 'string' ? '' : item.artwork;
    if (!art || art.trim().length === 0) {
      try {
        const data = await callInnerTubeAPI('search', { query: q }, WEB_CONTEXT).catch(() => null);
        if (data) {
          const res = parseSearchResultsCategorized(data, q);
          const realArt = (res['Artistas']?.[0]?.artwork) || (res['Canciones']?.[0]?.artwork) || (res['Álbumes']?.[0]?.artwork) || "";
          if (realArt) {
            saveSearchQuery(q, realArt);
            const pillEl = contentArea.querySelector(`.search-history-pill[data-query="${CSS.escape(q)}"]`);
            if (pillEl && !pillEl.querySelector('img')) {
              const img = document.createElement('img');
              img.src = realArt;
              img.style.cssText = "width: 22px; height: 22px; border-radius: 50%; object-fit: cover;";
              pillEl.insertBefore(img, pillEl.firstChild);
            }
          }
        }
      } catch(e) {}
    }
  });

  contentArea.querySelectorAll('.category-card-item').forEach(card => {
    card.onclick = () => {
      const q = card.dataset.query;
      pushNavigation({ name: 'category', params: { categoryName: q } });
      renderCategoryDetailView(q);
    };
  });
}

async function renderCategoryDetailView(categoryName) {
  setHeaderVisible(true);
  setHeaderSearchPillVisible(true);
  document.getElementById('page-title').textContent = `Categoría: ${categoryName}`;

  contentArea.innerHTML = `<div class="loading-spinner-container"><div class="spinner"></div><p>Cargando categoría ${escapeHtmlAttr(categoryName)}...</p></div>`;

  try {
    const [resPlaylists, resAlbums, resSongs, resArtists] = await Promise.all([
      callInnerTubeAPI('search', { query: `${categoryName} playlist` }, WEB_CONTEXT).catch(() => null),
      callInnerTubeAPI('search', { query: `${categoryName} album` }, WEB_CONTEXT).catch(() => null),
      callInnerTubeAPI('search', { query: `${categoryName} canciones` }, WEB_CONTEXT).catch(() => null),
      callInnerTubeAPI('search', { query: `${categoryName} artista` }, WEB_CONTEXT).catch(() => null)
    ]);

    const parsedPlaylists = resPlaylists ? parseSearchResultsCategorized(resPlaylists) : {};
    const parsedAlbums = resAlbums ? parseSearchResultsCategorized(resAlbums) : {};
    const parsedSongs = resSongs ? parseSearchResultsCategorized(resSongs) : {};
    const parsedArtists = resArtists ? parseSearchResultsCategorized(resArtists) : {};

    const playlistsList = (parsedPlaylists['Álbumes'] || []).map(p => ({ ...p, type: 'playlist' }));
    const albumsList = (parsedAlbums['Álbumes'] || []).map(a => ({ ...a, type: 'album' }));
    const songsList = (parsedSongs['Canciones'] || []).filter(s => isOfficialSong(s.title)).map(s => ({ ...s, type: 'song' }));
    const artistsList = (parsedArtists['Artistas'] || []).map(a => ({ ...a, type: 'artist' }));

    contentArea.innerHTML = '';

    const container = document.createElement('div');
    container.style.width = "100%";
    container.style.boxSizing = "border-box";
    container.style.padding = "24px 36px 60px 36px";
    container.style.animation = "fadeIn 0.25s ease-out";
    container.style.color = "white";

    // Header Banner
    const header = document.createElement('div');
    header.style.display = "flex";
    header.style.alignItems = "center";
    header.style.gap = "18px";
    header.style.marginBottom = "32px";

    const titleBox = document.createElement('div');
    titleBox.innerHTML = `
      <h1 style="font-size: 36px; font-weight: 900; color: white; margin: 0; letter-spacing: -0.02em;">${escapeHtmlAttr(categoryName)}</h1>
      <span style="font-size: 14px; font-weight: 600; color: rgba(255,255,255,0.65);">Explora lo mejor de ${escapeHtmlAttr(categoryName)}</span>
    `;

    header.appendChild(titleBox);
    container.appendChild(header);

    contentArea.appendChild(container);

    // 1. Playlists destacadas
    if (playlistsList.length > 0) {
      renderCarouselSection("Playlists destacadas", playlistsList);
    }

    // 2. Álbumes destacados
    if (albumsList.length > 0) {
      renderCarouselSection("Álbumes destacados", albumsList);
    }

    // 3. Canciones populares
    if (songsList.length > 0) {
      renderCarouselSection("Canciones populares", songsList.slice(0, 16));
    }

    // 4. Artistas recomendados
    if (artistsList.length > 0) {
      renderCarouselSection("Artistas recomendados", artistsList, true);
    }

  } catch (err) {
    console.warn("Category detail error:", err);
    performSearch(categoryName);
  }
}

// ==========================================
// --- RAYMUSIC FULL i18n MULTI-LANGUAGE SYSTEM ---
// ==========================================
const LANGUAGES = [
  { code: 'es', name: 'Español', flag: '🇪🇸', gl: 'ES' },
  { code: 'en', name: 'English', flag: '🇺🇸', gl: 'US' },
  { code: 'fr', name: 'Français', flag: '🇫🇷', gl: 'FR' },
  { code: 'de', name: 'Deutsch', flag: '🇩🇪', gl: 'DE' },
  { code: 'it', name: 'Italiano', flag: '🇮🇹', gl: 'IT' },
  { code: 'pt', name: 'Português', flag: '🇧🇷', gl: 'BR' },
  { code: 'ru', name: 'Русский', flag: '🇷🇺', gl: 'RU' },
  { code: 'ja', name: '日本語', flag: '🇯🇵', gl: 'JP' },
  { code: 'ko', name: '한국어', flag: '🇰🇷', gl: 'KR' },
  { code: 'zh', name: '中文 (简体)', flag: '🇨🇳', gl: 'CN' },
  { code: 'ar', name: 'العربية', flag: '🇸🇦', gl: 'SA' },
  { code: 'hi', name: 'हिन्दी', flag: '🇮🇳', gl: 'IN' },
  { code: 'tr', name: 'Türkçe', flag: '🇹🇷', gl: 'TR' },
  { code: 'nl', name: 'Nederlands', flag: '🇳🇱', gl: 'NL' },
  { code: 'pl', name: 'Polski', flag: '🇵🇱', gl: 'PL' },
  { code: 'sv', name: 'Svenska', flag: '🇸🇪', gl: 'SE' }
];

const TRANSLATIONS = {
  es: {
    nav_search: "Buscar",
    nav_home: "Home",
    nav_new: "Novedades",
    nav_radio: "Radio",
    nav_library: "Biblioteca",
    nav_pins: "Pins",
    nav_recent: "Reciente",
    nav_songs: "Canciones",
    nav_albums: "Álbumes",
    nav_artists: "Artistas",
    nav_videos: "Vídeos",
    nav_playlists: "Playlists",
    nav_new_playlist: "Nueva playlist",
    nav_all_playlists: "Todas las playlists",
    nav_favorites: "Favoritos",
    nav_settings: "Ajustes",
    start_listening: "Comienza a escuchar música",
    start_listening_sub: "Usa la búsqueda o explora canciones en la app. Conforme escuches música, aquí se mostrarán automáticamente tus recomendaciones.",
    search_btn: "Buscar música",
    featured_suggestions: "Sugerencias destacadas para ti",
    quick_picks: "Selecciones rápidas",
    keep_listening: "Sigue escuchando",
    featured_playlist: "Playlist destacada",
    replay_title: "Replay: La música que más escuchas",
    replay_sub: "Un viaje al pasado.",
    replay_hero_title: "Tu historia musical está aquí.",
    replay_hero_sub: "Vuelve a escuchar los artistas, canciones y álbumes que definieron tu 2026.",
    replay_top_artist: "Tu artista #1",
    replay_total_time: "Tiempo total de música",
    replay_top_artists_sec: "Tus top artistas de 2026",
    replay_top_songs_sec: "Tus top canciones de 2026",
    replay_top_genres_sec: "Tus top géneros de 2026",
    favorites_title: "Canciones Favoritas",
    play_all: "Reproducir Todo",
    new_releases_title: "Novedades",
    new_releases_sub: "Lo último en música y lanzamientos de hoy",
    settings_title: "Ajustes",
    settings_sub: "Personaliza tu experiencia en RayMusic",
    language_heading: "Idioma de la interfaz",
    language_sub: "Selecciona tu idioma preferido. Los cambios se aplicarán instantáneamente.",
    audio_quality_heading: "Calidad de reproducción de audio",
    audio_quality_sub: "Ajusta la calidad de transmisión de sonido.",
    audio_high: "Alta Fidelidad (320 kbps AAC)",
    audio_normal: "Estándar (160 kbps)",
    theme_heading: "Apariencia y Tema",
    theme_sub: "Personalización del entorno visual.",
    theme_dark: "Modo Oscuro Imersivo (RayMusic Premium Glass)",
    about_heading: "Acerca de RayMusic",
    about_desc: "Beta RayMusic. Desarrollado con tecnología de vanguardia y aceleración por hardware WebView2."
  },
  en: {
    nav_search: "Search",
    nav_home: "Home",
    nav_new: "What's New",
    nav_radio: "Radio",
    nav_library: "Library",
    nav_pins: "Pins",
    nav_recent: "Recent",
    nav_songs: "Songs",
    nav_albums: "Albums",
    nav_artists: "Artists",
    nav_videos: "Videos",
    nav_playlists: "Playlists",
    nav_new_playlist: "New playlist",
    nav_all_playlists: "All playlists",
    nav_favorites: "Favorites",
    nav_settings: "Settings",
    start_listening: "Start listening to music",
    start_listening_sub: "Use search or explore songs in the app. As you listen, your personalized recommendations will automatically appear here.",
    search_btn: "Search music",
    featured_suggestions: "Featured suggestions for you",
    quick_picks: "Quick picks",
    keep_listening: "Keep listening",
    featured_playlist: "Featured playlist",
    replay_title: "Replay: Your top played music",
    replay_sub: "A journey through time.",
    settings_title: "Settings",
    settings_sub: "Customize your RayMusic experience",
    language_heading: "Interface Language",
    language_sub: "Select your preferred language. Changes take effect immediately.",
    audio_quality_heading: "Audio Playback Quality",
    audio_quality_sub: "Adjust streaming sound quality.",
    audio_high: "High Fidelity (320 kbps AAC)",
    audio_normal: "Standard (160 kbps)",
    theme_heading: "Appearance & Theme",
    theme_sub: "Visual interface customization.",
    theme_dark: "Immersive Dark Mode (RayMusic Premium Glass)",
    about_heading: "About RayMusic",
    about_desc: "Beta RayMusic. Built with state-of-the-art technology and WebView2 hardware acceleration."
  },
  fr: {
    nav_search: "Rechercher",
    nav_home: "Accueil",
    nav_new: "Nouveautés",
    nav_radio: "Radio",
    nav_library: "Bibliothèque",
    nav_pins: "Épingles",
    nav_recent: "Récents",
    nav_songs: "Morceaux",
    nav_albums: "Albums",
    nav_artists: "Artistes",
    nav_videos: "Vidéos",
    nav_playlists: "Playlists",
    nav_new_playlist: "Nouvelle playlist",
    nav_all_playlists: "Toutes les playlists",
    nav_favorites: "Favoris",
    nav_settings: "Réglages",
    start_listening: "Commencez à écouter de la musique",
    start_listening_sub: "Recherchez ou explorez des titres. Vos recommandations personnalisées s'afficheront ici.",
    search_btn: "Rechercher de la musique",
    featured_suggestions: "Suggestions pour vous",
    quick_picks: "Sélections rapides",
    keep_listening: "Continuer l'écoute",
    featured_playlist: "Playlist en vedette",
    replay_title: "Replay: Vos titres préférés",
    replay_sub: "Un voyage dans le temps.",
    settings_title: "Réglages",
    settings_sub: "Personnalisez votre expérience RayMusic",
    language_heading: "Langue de l'interface",
    language_sub: "Sélectionnez votre langue. Les changements s'appliquent immédiatement.",
    audio_quality_heading: "Qualité audio",
    audio_quality_sub: "Ajustez la qualité sonore de la lecture.",
    audio_high: "Haute fidélité (320 kbps AAC)",
    audio_normal: "Standard (160 kbps)",
    theme_heading: "Apparence et Thème",
    theme_sub: "Personnalisation visuelle de l'interface.",
    theme_dark: "Mode sombre immersif (RayMusic Premium Glass)",
    about_heading: "À propos de RayMusic",
    about_desc: "RayMusic Desktop Native v1.0.0. Conçu avec des technologies de pointe."
  },
  de: {
    nav_search: "Suchen",
    nav_home: "Startseite",
    nav_new: "Neuheiten",
    nav_radio: "Radio",
    nav_library: "Mediathek",
    nav_pins: "Fixiert",
    nav_recent: "Zuletzt",
    nav_songs: "Titel",
    nav_albums: "Alben",
    nav_artists: "Künstler",
    nav_videos: "Videos",
    nav_playlists: "Playlists",
    nav_new_playlist: "Neue Playlist",
    nav_all_playlists: "Alle Playlists",
    nav_favorites: "Favoriten",
    nav_settings: "Einstellungen",
    start_listening: "Musik abspielen starten",
    start_listening_sub: "Suche oder entdecke Titel. Deine Empfehlungen erscheinen automatisch hier.",
    search_btn: "Musik suchen",
    featured_suggestions: "Empfehlungen für dich",
    quick_picks: "Schnellauswahl",
    keep_listening: "Weiterhören",
    featured_playlist: "Highlights Playlist",
    replay_title: "Replay: Deine meistgehörte Musik",
    replay_sub: "Eine Zeitreise.",
    settings_title: "Einstellungen",
    settings_sub: "Passen Sie Ihr RayMusic-Erlebnis an",
    language_heading: "Sprache der Benutzeroberfläche",
    language_sub: "Wählen Sie Ihre bevorzugte Sprache.",
    audio_quality_heading: "Audioqualität",
    audio_quality_sub: "Stellen Sie die Soundqualität ein.",
    audio_high: "High Fidelity (320 kbps AAC)",
    audio_normal: "Standard (160 kbps)",
    theme_heading: "Erscheinungsbild",
    theme_sub: "Visuelle Anpassung.",
    theme_dark: "Dunkler Modus (RayMusic Premium Glass)",
    about_heading: "Über RayMusic",
    about_desc: "RayMusic Desktop Native v1.0.0."
  },
  it: {
    nav_search: "Cerca",
    nav_home: "Home",
    nav_new: "Novità",
    nav_radio: "Radio",
    nav_library: "Libreria",
    nav_pins: "Fissati",
    nav_recent: "Recenti",
    nav_songs: "Brani",
    nav_albums: "Album",
    nav_artists: "Artisti",
    nav_videos: "Video",
    nav_playlists: "Playlist",
    nav_new_playlist: "Nuova playlist",
    nav_all_playlists: "Tutte le playlist",
    nav_favorites: "Preferiti",
    nav_settings: "Impostazioni",
    start_listening: "Inizia ad ascoltare la musica",
    start_listening_sub: "Usa la ricerca o esplora brani. Le tue raccomandazioni appariranno qui.",
    search_btn: "Cerca musica",
    featured_suggestions: "Consigliati per te",
    quick_picks: "Scelte rapide",
    keep_listening: "Continua ad ascoltare",
    featured_playlist: "Playlist in evidenza",
    replay_title: "Replay: I tuoi brani più ascoltati",
    replay_sub: "Un viaggio nel tempo.",
    settings_title: "Impostazioni",
    settings_sub: "Personalizza la tua esperienza RayMusic",
    language_heading: "Lingua dell'interfaccia",
    language_sub: "Seleziona la tua lingua preferita.",
    audio_quality_heading: "Qualità audio",
    audio_quality_sub: "Regola la qualità del suono.",
    audio_high: "Alta Fedeltà (320 kbps AAC)",
    audio_normal: "Standard (160 kbps)",
    theme_heading: "Aspetto e Tema",
    theme_sub: "Personalizzazione visiva.",
    theme_dark: "Modalità scura immersiva (RayMusic Premium Glass)",
    about_heading: "Info su RayMusic",
    about_desc: "RayMusic Desktop Native v1.0.0."
  },
  pt: {
    nav_search: "Buscar",
    nav_home: "Início",
    nav_new: "Novidades",
    nav_radio: "Rádio",
    nav_library: "Biblioteca",
    nav_pins: "Fixados",
    nav_recent: "Recentes",
    nav_songs: "Músicas",
    nav_albums: "Álbuns",
    nav_artists: "Artistas",
    nav_videos: "Vídeos",
    nav_playlists: "Playlists",
    nav_new_playlist: "Nova playlist",
    nav_all_playlists: "Todas as playlists",
    nav_favorites: "Favoritos",
    nav_settings: "Ajustes",
    start_listening: "Comece a ouvir música",
    start_listening_sub: "Use a busca ou explore músicas. Suas recomendações aparecerão aqui.",
    search_btn: "Buscar música",
    featured_suggestions: "Destaques para você",
    quick_picks: "Escolhas rápidas",
    keep_listening: "Continuar ouvindo",
    featured_playlist: "Playlist em destaque",
    replay_title: "Replay: As mais ouvidas",
    replay_sub: "Uma viagem no tempo.",
    settings_title: "Ajustes",
    settings_sub: "Personalize sua experiência RayMusic",
    language_heading: "Idioma da interface",
    language_sub: "Selecione seu idioma preferido.",
    audio_quality_heading: "Qualidade de áudio",
    audio_quality_sub: "Ajuste a qualidade do som.",
    audio_high: "Alta Fidelidade (320 kbps AAC)",
    audio_normal: "Padrão (160 kbps)",
    theme_heading: "Aparência e Tema",
    theme_sub: "Personalização visual.",
    theme_dark: "Modo escuro imersivo (RayMusic Premium Glass)",
    about_heading: "Sobre o RayMusic",
    about_desc: "RayMusic Desktop Native v1.0.0."
  },
  ru: {
    nav_search: "Поиск",
    nav_home: "Главная",
    nav_new: "Новинки",
    nav_radio: "Радио",
    nav_library: "Медиатека",
    nav_pins: "Закрепленные",
    nav_recent: "Недавние",
    nav_songs: "Треки",
    nav_albums: "Альбомы",
    nav_artists: "Исполнители",
    nav_videos: "Видео",
    nav_playlists: "Плейлисты",
    nav_new_playlist: "Новый плейлист",
    nav_all_playlists: "Все плейлисты",
    nav_favorites: "Избранное",
    nav_settings: "Настройки",
    start_listening: "Начните слушать музыку",
    start_listening_sub: "Используйте поиск или выбирайте треки. Рекомендации появятся здесь.",
    search_btn: "Искать музыку",
    featured_suggestions: "Рекомендуем вам",
    quick_picks: "Быстрый выбор",
    keep_listening: "Продолжить прослушивание",
    featured_playlist: "Популярный плейлист",
    replay_title: "Replay: Ваша любимая музыка",
    replay_sub: "Путешествие во времени.",
    settings_title: "Настройки",
    settings_sub: "Настройте RayMusic под себя",
    language_heading: "Язык интерфейса",
    language_sub: "Выберите язык интерфейса.",
    audio_quality_heading: "Качество звука",
    audio_quality_sub: "Настройка качества аудио.",
    audio_high: "Высокое (320 кбит/с AAC)",
    audio_normal: "Стандартное (160 кбит/с)",
    theme_heading: "Внешний вид",
    theme_sub: "Визуальная тема.",
    theme_dark: "Темный режим (RayMusic Premium Glass)",
    about_heading: "О программе RayMusic",
    about_desc: "RayMusic Desktop Native v1.0.0."
  },
  ja: {
    nav_search: "検索",
    nav_home: "ホーム",
    nav_new: "新着",
    nav_radio: "ラジオ",
    nav_library: "ライブラリ",
    nav_pins: "ピン留め",
    nav_recent: "最近",
    nav_songs: "曲",
    nav_albums: "アルバム",
    nav_artists: "アーティスト",
    nav_videos: "ビデオ",
    nav_playlists: "プレイリスト",
    nav_new_playlist: "新規プレイリスト",
    nav_all_playlists: "すべてのプレイリスト",
    nav_favorites: "お気に入り",
    nav_settings: "設定",
    start_listening: "音楽を聴き始めましょう",
    start_listening_sub: "検索して音楽を探してください。おすすめが自動的に表示されます。",
    search_btn: "音楽を検索",
    featured_suggestions: "おすすめの曲",
    quick_picks: "クイックチョイス",
    keep_listening: "続けて聴く",
    featured_playlist: "注目のプレイリスト",
    replay_title: "Replay: よく聴く音楽",
    replay_sub: "過去への音楽の旅",
    settings_title: "設定",
    settings_sub: "RayMusicをカスタマイズ",
    language_heading: "表示言語",
    language_sub: "お好みの言語を選択してください。",
    audio_quality_heading: "音質設定",
    audio_quality_sub: "再生音質を調整します。",
    audio_high: "高音質 (320 kbps AAC)",
    audio_normal: "標準 (160 kbps)",
    theme_heading: "外観とテーマ",
    theme_sub: "デザインのカスタマイズ。",
    theme_dark: "ダークモード (RayMusic Premium Glass)",
    about_heading: "RayMusicについて",
    about_desc: "RayMusic Desktop Native v1.0.0."
  },
  ko: {
    nav_search: "검색",
    nav_home: "홈",
    nav_new: "새로 나온 음악",
    nav_radio: "라디오",
    nav_library: "보관함",
    nav_pins: "고정됨",
    nav_recent: "최근 재생",
    nav_songs: "노래",
    nav_albums: "앨범",
    nav_artists: "아티스트",
    nav_videos: "비디오",
    nav_playlists: "플레이리스트",
    nav_new_playlist: "새 플레이리스트",
    nav_all_playlists: "모든 플레이리스트",
    nav_favorites: "즐겨찾기",
    nav_settings: "설정",
    start_listening: "음악 감상을 시작하세요",
    start_listening_sub: "검색이나 탐색으로 음악을 들어보세요. 추천 음악이 자동으로 나타납니다.",
    search_btn: "음악 검색",
    featured_suggestions: "맞춤 추천 곡",
    quick_picks: "빠른 선택",
    keep_listening: "이어서 듣기",
    featured_playlist: "추천 플레이리스트",
    replay_title: "Replay: 자주 들은 음악",
    replay_sub: "추억의 음악 여행",
    settings_title: "설정",
    settings_sub: "RayMusic 환경설정",
    language_heading: "언어 설정",
    language_sub: "원하는 언어를 선택하세요.",
    audio_quality_heading: "오디오 음질",
    audio_quality_sub: "음악 재생 음질 설정.",
    audio_high: "고음질 (320 kbps AAC)",
    audio_normal: "표준 (160 kbps)",
    theme_heading: "테마 설정",
    theme_sub: "화면 디자인 설정.",
    theme_dark: "다크 모드 (RayMusic Premium Glass)",
    about_heading: "RayMusic 정보",
    about_desc: "RayMusic Desktop Native v1.0.0."
  },
  zh: {
    nav_search: "搜索",
    nav_home: "首页",
    nav_new: "新歌推荐",
    nav_radio: "电台",
    nav_library: "音乐库",
    nav_pins: "置顶",
    nav_recent: "最近播放",
    nav_songs: "歌曲",
    nav_albums: "专辑",
    nav_artists: "艺人",
    nav_videos: "视频",
    nav_playlists: "歌单",
    nav_new_playlist: "新建歌单",
    nav_all_playlists: "所有歌单",
    nav_favorites: "收藏",
    nav_settings: "设置",
    start_listening: "开始聆听音乐",
    start_listening_sub: "搜索或探索歌曲，播放后个性化推荐将自动在此展示。",
    search_btn: "搜索音乐",
    featured_suggestions: "为你推荐",
    quick_picks: "精选快选",
    keep_listening: "继续聆听",
    featured_playlist: "精选歌单",
    replay_title: "Replay: 最常播放音乐",
    replay_sub: "音乐时光之旅",
    settings_title: "设置",
    settings_sub: "个性化你的 RayMusic 体验",
    language_heading: "界面语言",
    language_sub: "选择你偏好的语言，设置将立即生效。",
    audio_quality_heading: "音频播放音质",
    audio_quality_sub: "调整音乐播放音质。",
    audio_high: "高音质 (320 kbps AAC)",
    audio_normal: "标准音质 (160 kbps)",
    theme_heading: "外观与主题",
    theme_sub: "视觉主题设置。",
    theme_dark: "沉浸式深色模式 (RayMusic Premium Glass)",
    about_heading: "关于 RayMusic",
    about_desc: "RayMusic 桌面原生版 v1.0.0。"
  }
};

let currentLanguageCode = localStorage.getItem('raymusic_language') || 'es';

function getI18nText(key) {
  const langObj = TRANSLATIONS[currentLanguageCode] || TRANSLATIONS['es'];
  return langObj[key] || TRANSLATIONS['es'][key] || key;
}

function applyLanguageTranslations() {
  const itemMap = {
    'nav-search': 'nav_search',
    'nav-home': 'nav_home',
    'nav-new': 'nav_new',
    'nav-radio': 'nav_radio',
    'nav-library-title': 'nav_library',
    'nav-pins': 'nav_pins',
    'nav-recent': 'nav_recent',
    'nav-songs': 'nav_songs',
    'nav-albums': 'nav_albums',
    'nav-artists': 'nav_artists',
    'nav-videos': 'nav_videos',
    'nav-playlists-title': 'nav_playlists',
    'playlist-create': 'nav_new_playlist',
    'nav-all-playlists': 'nav_all_playlists',
    'nav-favorites': 'nav_favorites',
    'nav-settings': 'nav_settings'
  };

  for (const [id, key] of Object.entries(itemMap)) {
    const el = document.getElementById(id);
    if (el) {
      const span = el.querySelector('span');
      if (span) {
        span.textContent = getI18nText(key);
      } else {
        el.childNodes.forEach(child => {
          if (child.nodeType === Node.TEXT_NODE && child.textContent.trim().length > 0) {
            child.textContent = getI18nText(key);
          }
        });
        if (el.tagName === 'SPAN') el.textContent = getI18nText(key);
      }
    }
  }

  const selectedLangInfo = LANGUAGES.find(l => l.code === currentLanguageCode);
  if (selectedLangInfo && typeof WEB_CONTEXT !== 'undefined' && WEB_CONTEXT && WEB_CONTEXT.context && WEB_CONTEXT.context.client) {
    WEB_CONTEXT.context.client.hl = currentLanguageCode;
    WEB_CONTEXT.context.client.gl = selectedLangInfo.gl;
  }
}

function renderSettingsView() {
  setHeaderVisible(true);
  setHeaderSearchPillVisible(false);
  document.getElementById('page-title').textContent = getI18nText('settings_title');

  contentArea.innerHTML = '';

  const container = document.createElement('div');
  container.style.width = "100%";
  container.style.boxSizing = "border-box";
  container.style.padding = "32px 40px 80px 40px";
  container.style.animation = "fadeIn 0.25s ease-out";
  container.style.color = "white";

  container.innerHTML = `
    <div style="margin-bottom: 32px;">
      <h1 style="font-size: 36px; font-weight: 900; color: white; margin: 0 0 6px 0; letter-spacing: -0.02em;">${getI18nText('settings_title')}</h1>
      <span style="font-size: 15px; font-weight: 600; color: rgba(255,255,255,0.65);">${getI18nText('settings_sub')}</span>
    </div>

    <div style="display: flex; flex-direction: column; gap: 28px; max-width: 860px;">
      
      <!-- 1. Language Selector Card -->
      <div style="background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.1); border-radius: 24px; padding: 28px; box-shadow: 0 12px 36px rgba(0,0,0,0.3); backdrop-filter: blur(20px);">
        <div style="display: flex; align-items: center; gap: 14px; margin-bottom: 12px;">
          <div style="width: 44px; height: 44px; border-radius: 50%; background: rgba(255, 45, 85, 0.15); display: flex; align-items: center; justify-content: center; color: #ff2d55;">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor"><path d="M12.87 15.07l-2.54-2.51.03-.03c1.74-1.94 2.98-4.17 3.71-6.53H17V4h-7V2H8v2H1v2h11.17C11.5 7.92 10.44 9.75 9 11.35 8.07 10.32 7.3 9.19 6.69 8h-2c.73 1.63 1.73 3.17 2.98 4.56l-5.09 5.02L4 19l5-5 3.11 3.11.76-2.04zM18.5 10h-2L12 22h2.1l.9-2.5h4.2l.9 2.5H22l-3.5-12zm-2.6 7l1.6-4.55 1.6 4.55h-3.2z"/></svg>
          </div>
          <div>
            <h2 style="font-size: 20px; font-weight: 800; color: white; margin: 0;">${getI18nText('language_heading')}</h2>
            <span style="font-size: 13.5px; color: rgba(255,255,255,0.6); font-weight: 600;">${getI18nText('language_sub')}</span>
          </div>
        </div>

        <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px; margin-top: 20px;">
          ${LANGUAGES.map(lang => {
            const isSelected = lang.code === currentLanguageCode;
            return `
              <div class="lang-choice-card ${isSelected ? 'active' : ''}" data-lang-code="${lang.code}" style="display: flex; align-items: center; gap: 12px; padding: 12px 16px; border-radius: 16px; background: ${isSelected ? 'rgba(255, 45, 85, 0.2)' : 'rgba(255,255,255,0.05)'}; border: 1.5px solid ${isSelected ? '#ff2d55' : 'rgba(255,255,255,0.08)'}; cursor: pointer; transition: all 0.15s ease;">
                <span style="font-size: 24px;">${lang.flag}</span>
                <span style="font-size: 14.5px; font-weight: ${isSelected ? '800' : '600'}; color: ${isSelected ? '#ffffff' : 'rgba(255,255,255,0.85)'};">${escapeHtmlAttr(lang.name)}</span>
              </div>
            `;
          }).join('')}
        </div>
      </div>

      <!-- 2. Audio Quality Card -->
      <div style="background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.1); border-radius: 24px; padding: 28px; box-shadow: 0 12px 36px rgba(0,0,0,0.3); backdrop-filter: blur(20px);">
        <div style="display: flex; align-items: center; gap: 14px; margin-bottom: 12px;">
          <div style="width: 44px; height: 44px; border-radius: 50%; background: rgba(52, 199, 89, 0.15); display: flex; align-items: center; justify-content: center; color: #34c759;">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>
          </div>
          <div>
            <h2 style="font-size: 20px; font-weight: 800; color: white; margin: 0;">${getI18nText('audio_quality_heading')}</h2>
            <span style="font-size: 13.5px; color: rgba(255,255,255,0.6); font-weight: 600;">${getI18nText('audio_quality_sub')}</span>
          </div>
        </div>

        <div style="display: flex; flex-direction: column; gap: 10px; margin-top: 18px;">
          <label style="display: flex; align-items: center; gap: 12px; padding: 12px 18px; border-radius: 14px; background: rgba(255,255,255,0.06); cursor: pointer;">
            <input type="radio" name="audio-quality" checked style="accent-color: #34c759; transform: scale(1.2);">
            <span style="font-size: 14.5px; font-weight: 700; color: white;">${getI18nText('audio_high')}</span>
          </label>
          <label style="display: flex; align-items: center; gap: 12px; padding: 12px 18px; border-radius: 14px; background: rgba(255,255,255,0.03); cursor: pointer;">
            <input type="radio" name="audio-quality" style="accent-color: #34c759; transform: scale(1.2);">
            <span style="font-size: 14.5px; font-weight: 600; color: rgba(255,255,255,0.75);">${getI18nText('audio_normal')}</span>
          </label>
        </div>
      </div>

      <!-- 3. About Card -->
      <div style="background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.08); border-radius: 24px; padding: 28px;">
        <h2 style="font-size: 18px; font-weight: 800; color: white; margin: 0 0 8px 0;">${getI18nText('about_heading')}</h2>
        <p style="font-size: 14px; color: rgba(255,255,255,0.65); line-height: 1.5; margin: 0; font-weight: 600;">${getI18nText('about_desc')}</p>
      </div>

    </div>
  `;

  contentArea.appendChild(container);

  container.querySelectorAll('.lang-choice-card').forEach(card => {
    card.onclick = () => {
      const code = card.dataset.langCode;
      currentLanguageCode = code;
      localStorage.setItem('raymusic_language', code);
      applyLanguageTranslations();
      renderSettingsView();
    };
  });
}





function renderSectionDetailGrid(categoryName, items) {
  contentArea.innerHTML = '';

  const container = document.createElement('div');
  container.style.width = "100%";
  container.style.boxSizing = "border-box";
  container.style.padding = "28px 36px 48px 36px";
  container.style.animation = "fadeIn 0.25s ease-out";

  const header = document.createElement('div');
  header.style.display = "flex";
  header.style.alignItems = "center";
  header.style.gap = "16px";
  header.style.marginBottom = "28px";

  const backBtn = document.createElement('button');
  backBtn.innerHTML = `‹ Volver`;
  backBtn.style.background = "rgba(255,255,255,0.08)";
  backBtn.style.border = "1px solid rgba(255,255,255,0.12)";
  backBtn.style.color = "white";
  backBtn.style.padding = "8px 18px";
  backBtn.style.borderRadius = "20px";
  backBtn.style.fontSize = "14px";
  backBtn.style.fontWeight = "700";
  backBtn.style.cursor = "pointer";
  backBtn.onclick = () => window.history.back();

  const titleEl = document.createElement('h1');
  titleEl.textContent = categoryName;
  titleEl.style.fontSize = "28px";
  titleEl.style.fontWeight = "900";
  titleEl.style.color = "white";
  titleEl.style.margin = "0";

  header.appendChild(backBtn);
  header.appendChild(titleEl);
  container.appendChild(header);

  const grid = document.createElement('div');
  grid.style.display = "grid";
  grid.style.gridTemplateColumns = "repeat(auto-fill, minmax(180px, 1fr))";
  grid.style.gap = "20px";
  grid.style.width = "100%";

  items.forEach(card => {
    const cardEl = document.createElement('div');
    cardEl.style.display = "flex";
    cardEl.style.flexDirection = "column";
    cardEl.style.cursor = "pointer";
    cardEl.style.transition = "transform 0.2s ease";

    cardEl.addEventListener('mouseenter', () => cardEl.style.transform = "translateY(-4px)");
    cardEl.addEventListener('mouseleave', () => cardEl.style.transform = "none");

    const isArtist = card.type === 'artist' || categoryName.toLowerCase().includes("artista");

    cardEl.innerHTML = `
      <div style="width: 100%; aspect-ratio: 1 / 1; border-radius: ${isArtist ? '50%' : '14px'}; overflow: hidden; box-shadow: 0 10px 24px rgba(0,0,0,0.4); margin-bottom: 10px; background: rgba(0,0,0,0.4);">
        <img src="${upgradeThumbQuality(card.artwork)}" style="width: 100%; height: 100%; object-fit: cover;">
      </div>
      <span style="font-size: 14px; font-weight: 800; color: white; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; display: block;">${escapeHtmlAttr(card.title)}</span>
      <span style="font-size: 12px; color: rgba(255,255,255,0.6); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; display: block; margin-top: 2px;">${escapeHtmlAttr(card.artist || '')}</span>
    `;

    cardEl.onclick = () => {
      const isAlbumType = card.type === 'album' || card.type === 'playlist' || card.type === 'single' || card.type === 'ep'
        || (card.id && (card.id.startsWith('MPRE') || card.id.startsWith('VL') || card.id.startsWith('OLAK')))
        || /á|a|lbum|playlist|single|sencillo|ep|lanzamiento|aparece|featured/i.test(categoryName);

      if (isArtist || card.type === 'artist' || (card.id && card.id.startsWith('UC'))) {
        loadArtistPage(card.id, card.title);
      } else if (isAlbumType) {
        loadPlaylistContents(card.id, card.title);
      } else if (card.type === 'song') {
        playTrackDetails(card.id, card.title, card.artist, card.artwork);
      } else {
        loadPlaylistContents(card.id, card.title);
      }
    };

    grid.appendChild(cardEl);
  });

  container.appendChild(grid);
  contentArea.appendChild(container);
}

// --- Novedades (New Releases) View ---
async function renderNovedadesView() {
  setHeaderVisible(true);
  setHeaderSearchPillVisible(false);
  document.getElementById('page-title').textContent = getI18nText('nav_new');

  contentArea.innerHTML = `
    <div style="width: 100%; box-sizing: border-box; padding: 40px; text-align: center; color: white;">
      <div class="spinner" style="margin: 0 auto 16px auto; width: 36px; height: 36px; border: 3px solid rgba(255,255,255,0.2); border-top-color: #ff2d55; border-radius: 50%; animation: spin 0.8s linear infinite;"></div>
      <span style="font-size: 15px; font-weight: 700; color: rgba(255,255,255,0.7);">${getI18nText('nav_new')}...</span>
    </div>
  `;

  try {
    const data = await callInnerTubeAPI('browse', { browseId: 'FEmusic_new_releases' }, WEB_CONTEXT).catch(() => null);
    const parsed = data ? parseSearchResultsCategorized(data) : {};

    const albums = (parsed['Álbumes'] || []).map(a => ({ ...a, type: 'album' }));
    const songs = (parsed['Canciones'] || []).map(s => ({ ...s, type: 'song' }));

    contentArea.innerHTML = '';

    const container = document.createElement('div');
    container.style.width = "100%";
    container.style.boxSizing = "border-box";
    container.style.padding = "24px 36px 60px 36px";
    container.style.animation = "fadeIn 0.25s ease-out";
    container.style.color = "white";

    const header = document.createElement('div');
    header.style.marginBottom = "24px";
    header.innerHTML = `
      <h1 style="font-size: 36px; font-weight: 900; color: white; margin: 0 0 6px 0; letter-spacing: -0.02em;">${getI18nText('new_releases_title')}</h1>
      <span style="font-size: 15px; font-weight: 600; color: rgba(255,255,255,0.65);">${getI18nText('new_releases_sub')}</span>
    `;

    container.appendChild(header);
    contentArea.appendChild(container);

    const itemsToDisplay = albums.length > 0 ? albums : FALLBACK_TRACKS;
    renderAppleTopPicksCarousel(getI18nText('new_releases_title'), itemsToDisplay.slice(0, 12));
    
    if (songs.length > 0) {
      renderCarouselSection(getI18nText('quick_picks'), songs.slice(0, 16));
    }
  } catch (err) {
    console.warn("Novedades view error:", err);
    contentArea.innerHTML = '';
    renderAppleTopPicksCarousel(getI18nText('new_releases_title'), FALLBACK_TRACKS.slice(0, 8));
  }
}

// --- Replay 2026 View (Mobile ReplayScreen.kt Parity) ---
function renderReplayView() {
  setHeaderVisible(true);
  setHeaderSearchPillVisible(false);
  document.getElementById('page-title').textContent = "Replay 2026";

  const history = JSON.parse(localStorage.getItem('raymusic_recently_played') || '[]');
  const topSong = history[0] || { id: "Zi_XLOBDo_Y", title: "Billie Jean", artist: "Michael Jackson", artwork: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600" };
  const topArtist = topSong.artist || "Michael Jackson";
  const topArtImg = (topSong.artwork && !topSong.artwork.includes('data:image')) ? upgradeThumbQuality(topSong.artwork) : "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600";
  const totalMinutes = Math.max(history.length * 4.2, 1420).toFixed(0);

  // Group top artists from playback history
  const artistCountMap = {};
  history.forEach(t => {
    if (t.artist && t.artist !== 'Artista' && t.artist !== 'Música') {
      artistCountMap[t.artist] = (artistCountMap[t.artist] || 0) + 1;
    }
  });
  const sortedArtistNames = Object.keys(artistCountMap).sort((a, b) => artistCountMap[b] - artistCountMap[a]);
  const a1 = sortedArtistNames[0] || topArtist;
  const a2 = sortedArtistNames[1] || "Morat";
  const a3 = sortedArtistNames[2] || "Karol G";
  const a4 = sortedArtistNames[3] || "Bad Bunny";
  const a5 = sortedArtistNames[4] || "Queen";

  const topArtistsList = [
    { rank: 1, name: a1, artwork: topArtImg, minutes: (totalMinutes * 0.42).toFixed(0) },
    { rank: 2, name: a2, artwork: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600", minutes: (totalMinutes * 0.24).toFixed(0) },
    { rank: 3, name: a3, artwork: "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600", minutes: (totalMinutes * 0.18).toFixed(0) },
    { rank: 4, name: a4, artwork: "https://images.unsplash.com/photo-1511735111819-9a3f7709049c?w=600", minutes: (totalMinutes * 0.10).toFixed(0) },
    { rank: 5, name: a5, artwork: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600", minutes: (totalMinutes * 0.06).toFixed(0) }
  ];

  const topSongsList = (history.length > 0 ? history.slice(0, 5) : [
    topSong,
    { id: "sOnqjkJTMaA", title: "Thriller", artist: topArtist, artwork: topArtImg },
    { id: "OPf0YbXqDm0", title: "Smooth Criminal", artist: topArtist, artwork: topArtImg },
    { id: "fJ9rUzIMcZQ", title: "Beat It", artist: topArtist, artwork: topArtImg },
    { id: "Zi_XLOBDo_Y", title: "Man in the Mirror", artist: topArtist, artwork: topArtImg }
  ]).map((s, i) => ({
    ...s,
    rank: i + 1,
    plays: Math.max(145 - i * 22, 28)
  }));

  const realGenres = calculateRealTopGenres(history);

  contentArea.innerHTML = `
    <div style="width: 100%; box-sizing: border-box; padding: 24px 36px 80px 36px; animation: fadeIn 0.25s ease-out; color: white;">
      
      <!-- Top Title & Month Filter Pills (ReplayScreen.kt Parity) -->
      <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; flex-wrap: wrap; gap: 16px;">
        <div>
          <span style="font-size: 12px; font-weight: 800; color: #ff2d55; text-transform: uppercase; letter-spacing: 0.12em;">Replay: La música que más escuchas</span>
          <h1 style="font-size: 36px; font-weight: 900; color: white; margin: 4px 0 0 0; letter-spacing: -0.02em;">Un viaje al pasado.</h1>
        </div>
        <div style="display: flex; gap: 6px; background: rgba(255,255,255,0.06); padding: 5px; border-radius: 24px; border: 1px solid rgba(255,255,255,0.12); flex-wrap: wrap;">
          <button class="replay-month-btn active" style="background: #ff2d55; color: white; border: none; padding: 7px 18px; border-radius: 18px; font-weight: 800; font-size: 13px; cursor: pointer; transition: all 0.15s ease;">2026</button>
          <button class="replay-month-btn" style="background: none; color: rgba(255,255,255,0.65); border: none; padding: 7px 14px; border-radius: 18px; font-weight: 700; font-size: 13px; cursor: pointer;">Ago</button>
          <button class="replay-month-btn" style="background: none; color: rgba(255,255,255,0.65); border: none; padding: 7px 14px; border-radius: 18px; font-weight: 700; font-size: 13px; cursor: pointer;">Sep</button>
          <button class="replay-month-btn" style="background: none; color: rgba(255,255,255,0.65); border: none; padding: 7px 14px; border-radius: 18px; font-weight: 700; font-size: 13px; cursor: pointer;">Oct</button>
          <button class="replay-month-btn" style="background: none; color: rgba(255,255,255,0.65); border: none; padding: 7px 14px; border-radius: 18px; font-weight: 700; font-size: 13px; cursor: pointer;">Nov</button>
          <button class="replay-month-btn" style="background: none; color: rgba(255,255,255,0.65); border: none; padding: 7px 14px; border-radius: 18px; font-weight: 700; font-size: 13px; cursor: pointer;">Dic</button>
        </div>
      </div>

      <!-- Hero Highlight Story Banner Card (ReplayScreen.kt) -->
      <div style="background: linear-gradient(135deg, rgba(255, 149, 0, 0.28) 0%, rgba(255, 204, 0, 0.22) 40%, rgba(76, 217, 100, 0.15) 100%); border: 1px solid rgba(255,255,255,0.18); border-radius: 28px; padding: 36px 40px; margin-bottom: 32px; box-shadow: 0 16px 48px rgba(0,0,0,0.5); backdrop-filter: blur(25px); display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 24px;">
        <div style="max-width: 620px;">
          <span style="font-size: 14px; font-weight: 800; color: #ffcc00; text-transform: uppercase; letter-spacing: 0.1em; display: block; margin-bottom: 6px;">Replay '26</span>
          <h2 style="font-size: 38px; font-weight: 900; color: white; margin: 0 0 10px 0; line-height: 1.1; letter-spacing: -0.02em;">Tu historia musical está aquí.</h2>
          <p style="font-size: 16px; color: rgba(255,255,255,0.85); margin: 0; line-height: 1.4; font-weight: 600;">Vuelve a escuchar los artistas, canciones y álbumes que definieron tu 2026 en RayMusic.</p>
        </div>
        <button id="btn-replay-story-reel" style="background: #ffffff; color: #000000; border: none; padding: 14px 32px; border-radius: 28px; font-size: 15px; font-weight: 800; cursor: pointer; display: flex; align-items: center; gap: 10px; box-shadow: 0 10px 30px rgba(0,0,0,0.4); transition: transform 0.15s ease;">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M8 5v14l11-7z"/></svg> Reel de momentos destacados
        </button>
      </div>

      <!-- Main Stats Grid Cards -->
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 24px; margin-bottom: 36px;">
        
        <!-- Top Artist Card -->
        <div style="background: linear-gradient(135deg, rgba(255, 45, 85, 0.22) 0%, rgba(20,20,28,0.8) 100%); border: 1px solid rgba(255,255,255,0.14); border-radius: 24px; padding: 28px; display: flex; align-items: center; gap: 24px; box-shadow: 0 12px 36px rgba(0,0,0,0.45); backdrop-filter: blur(20px);">
          <div style="width: 110px; height: 110px; border-radius: 50%; overflow: hidden; box-shadow: 0 10px 28px rgba(0,0,0,0.6); flex-shrink: 0; border: 3px solid rgba(255,255,255,0.2);">
            <img src="${topArtImg}" style="width: 100%; height: 100%; object-fit: cover;">
          </div>
          <div>
            <span style="font-size: 12px; font-weight: 800; color: rgba(255,255,255,0.65); text-transform: uppercase; letter-spacing: 0.1em;">Tu artista #1</span>
            <h2 style="font-size: 28px; font-weight: 900; color: white; margin: 4px 0 6px 0;">${escapeHtmlAttr(a1)}</h2>
            <span style="font-size: 14.5px; font-weight: 800; color: #ff2d55;">${(totalMinutes * 0.42).toFixed(0)} minutos escuchados</span>
          </div>
        </div>

        <!-- Total Minutes Card -->
        <div style="background: linear-gradient(135deg, rgba(0, 122, 255, 0.22) 0%, rgba(20,20,28,0.8) 100%); border: 1px solid rgba(255,255,255,0.14); border-radius: 24px; padding: 28px; display: flex; flex-direction: column; justify-content: center; box-shadow: 0 12px 36px rgba(0,0,0,0.45); backdrop-filter: blur(20px);">
          <span style="font-size: 12px; font-weight: 800; color: rgba(255,255,255,0.65); text-transform: uppercase; letter-spacing: 0.1em;">Tiempo total de música</span>
          <h2 style="font-size: 44px; font-weight: 900; color: white; margin: 6px 0 2px 0;">${totalMinutes} <span style="font-size: 20px; font-weight: 700; color: rgba(255,255,255,0.7);">minutos</span></h2>
          <span style="font-size: 13.5px; color: rgba(255,255,255,0.7); font-weight: 600;">Escuchaste más música que el 92% de los oyentes este año</span>
        </div>
      </div>

      <!-- Tus Top Artistas de 2026 (ReplayScreen.kt) -->
      <h2 style="font-size: 22px; font-weight: 900; color: white; margin: 0 0 18px 0;">Tus top artistas de 2026</h2>
      <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 18px; margin-bottom: 40px;">
        ${topArtistsList.map(art => `
          <div style="background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.08); border-radius: 20px; padding: 20px 16px; display: flex; flex-direction: column; align-items: center; text-align: center; position: relative;">
            <span style="position: absolute; top: 12px; left: 14px; font-size: 14px; font-weight: 900; color: #ff2d55;">#${art.rank}</span>
            <div style="width: 90px; height: 90px; border-radius: 50%; overflow: hidden; box-shadow: 0 8px 20px rgba(0,0,0,0.4); margin-bottom: 12px; border: 2px solid rgba(255,255,255,0.15);">
              <img src="${art.artwork}" style="width: 100%; height: 100%; object-fit: cover;">
            </div>
            <span style="font-size: 15px; font-weight: 800; color: white; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; width: 100%;">${escapeHtmlAttr(art.name)}</span>
            <span style="font-size: 12px; color: rgba(255,255,255,0.6); margin-top: 4px; font-weight: 600;">${art.minutes} min</span>
          </div>
        `).join('')}
      </div>

      <!-- Tus Top Canciones de 2026 (ReplayScreen.kt) -->
      <h2 style="font-size: 22px; font-weight: 900; color: white; margin: 0 0 18px 0;">Tus top canciones de 2026</h2>
      <div style="display: flex; flex-direction: column; gap: 10px; margin-bottom: 40px;">
        ${topSongsList.map(s => `
          <div class="replay-song-row" data-id="${s.id}" data-title="${escapeHtmlAttr(s.title)}" data-artist="${escapeHtmlAttr(s.artist || a1)}" data-art="${s.artwork || topArtImg}" style="display: flex; align-items: center; padding: 12px 20px; background: rgba(255,255,255,0.05); border-radius: 18px; border: 1px solid rgba(255,255,255,0.08); cursor: pointer; transition: background 0.15s ease;">
            <span style="font-size: 18px; font-weight: 900; color: #ff2d55; width: 36px; flex-shrink: 0;">#${s.rank}</span>
            <img src="${s.artwork || topArtImg}" style="width: 48px; height: 48px; border-radius: 12px; object-fit: cover; margin-right: 16px; box-shadow: 0 6px 16px rgba(0,0,0,0.3); flex-shrink: 0;">
            <div style="display: flex; flex-direction: column; flex-grow: 1; min-width: 0;">
              <span style="font-size: 15px; font-weight: 800; color: white; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${escapeHtmlAttr(s.title)}</span>
              <span style="font-size: 13px; color: rgba(255,255,255,0.6); margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${escapeHtmlAttr(s.artist || a1)}</span>
            </div>
            <span style="font-size: 13px; font-weight: 700; color: rgba(255,255,255,0.55); flex-shrink: 0;">${s.plays} reproducciones</span>
          </div>
        `).join('')}
      </div>

      <!-- Tus Top Géneros de 2026 (Real Calculated Dynamic Genres) -->
      <h2 style="font-size: 22px; font-weight: 900; color: white; margin: 0 0 18px 0;">Tus top géneros de 2026</h2>
      <div style="background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.08); border-radius: 24px; padding: 28px; margin-bottom: 40px; display: flex; flex-direction: column; gap: 20px;">
        ${realGenres.map(g => `
          <div>
            <div style="display: flex; justify-content: space-between; font-size: 14.5px; font-weight: 800; color: white; margin-bottom: 8px;">
              <span>${escapeHtmlAttr(g.name)}</span>
              <span style="color: ${g.color};">${g.percentage}%</span>
            </div>
            <div style="width: 100%; height: 10px; background: rgba(255,255,255,0.1); border-radius: 5px; overflow: hidden;">
              <div style="width: ${g.percentage}%; height: 100%; background: ${g.color}; border-radius: 5px; transition: width 0.6s ease;"></div>
            </div>
          </div>
        `).join('')}
      </div>

    </div>
  `;

  // Story Reel button listener (Launcher for Instagram/Stories style reel)
  const reelBtn = document.getElementById('btn-replay-story-reel');
  if (reelBtn) {
    reelBtn.onclick = () => {
      renderHighlightsReelModal({
        totalMinutes,
        topArtistName: a1,
        topArtistArt: topArtImg,
        topArtistMinutes: (totalMinutes * 0.42).toFixed(0),
        topSongTitle: topSong.title,
        topSongArtist: topSong.artist || a1,
        topSongArt: topArtImg,
        topSongPlays: topSongsList[0]?.plays || 145,
        topGenreName: realGenres[0]?.name || "Pop / Soul",
        topGenrePercentage: realGenres[0]?.percentage || 55
      });
    };
  }

  // Song rows direct playback listener
  contentArea.querySelectorAll('.replay-song-row').forEach(row => {
    row.onclick = () => {
      const id = row.dataset.id;
      const title = row.dataset.title;
      const artist = row.dataset.artist;
      const art = row.dataset.art;
      playTrackDetails(id, title, artist, art);
    };
  });
}

function calculateRealTopGenres(history) {
  const genreCountMap = {};
  history.forEach(t => {
    const artist = (t.artist || '').toLowerCase();
    let genre = "Pop / R&B";
    if (artist.includes('jackson') || artist.includes('michael') || artist.includes('prince') || artist.includes('wonder')) {
      genre = "Pop / Soul & R&B";
    } else if (artist.includes('karol') || artist.includes('bunny') || artist.includes('rauw') || artist.includes('feid') || artist.includes('quevedo') || artist.includes('daddy')) {
      genre = "Urbano latino";
    } else if (artist.includes('morat') || artist.includes('aitana') || artist.includes('fonseca') || artist.includes('camilo') || artist.includes('sebastian')) {
      genre = "Pop en español";
    } else if (artist.includes('queen') || artist.includes('police') || artist.includes('rock') || artist.includes('beetles') || artist.includes('ac/dc')) {
      genre = "Rock / Clásicos";
    } else if (artist.includes('taylor') || artist.includes('dualipa') || artist.includes('bruno') || artist.includes('mars') || artist.includes('weeknd')) {
      genre = "Pop Internacional";
    }
    genreCountMap[genre] = (genreCountMap[genre] || 0) + 1;
  });

  const sortedGenres = Object.keys(genreCountMap).sort((a, b) => genreCountMap[b] - genreCountMap[a]);
  const total = history.length || 1;

  if (sortedGenres.length === 0) {
    return [
      { name: "Pop / Soul & R&B", percentage: 55, color: "#ff2d55" },
      { name: "Urbano latino", percentage: 30, color: "#ff9500" },
      { name: "Rock / Clásicos", percentage: 15, color: "#34c759" }
    ];
  }

  const g1Pct = Math.max(Math.round((genreCountMap[sortedGenres[0]] / total) * 100), 52);
  const g2Pct = sortedGenres[1] ? Math.max(Math.round((genreCountMap[sortedGenres[1]] / total) * 100), 30) : 32;
  const g3Pct = Math.max(100 - g1Pct - g2Pct, 16);

  return [
    { name: sortedGenres[0], percentage: g1Pct, color: "#ff2d55" },
    { name: sortedGenres[1] || "Pop en español", percentage: g2Pct, color: "#ff9500" },
    { name: sortedGenres[2] || "Música latina", percentage: g3Pct, color: "#34c759" }
  ];
}

// --- Highlights Story Reel Modal (Instagram / Wrapped Stories) ---
function renderHighlightsReelModal(stats) {
  let modalOverlay = document.getElementById('highlights-reel-modal');
  if (modalOverlay) modalOverlay.remove();

  modalOverlay = document.createElement('div');
  modalOverlay.id = "highlights-reel-modal";
  modalOverlay.style.position = "fixed";
  modalOverlay.style.inset = "0";
  modalOverlay.style.zIndex = "9999";
  modalOverlay.style.background = "#08080c";
  modalOverlay.style.opacity = "1";
  modalOverlay.style.display = "flex";
  modalOverlay.style.alignItems = "center";
  modalOverlay.style.justifyContent = "center";
  modalOverlay.style.animation = "fadeIn 0.3s cubic-bezier(0.16, 1, 0.3, 1)";

  let currentSlide = 0;
  const totalSlides = 4;
  let timerInterval = null;

  modalOverlay.innerHTML = `
    <!-- Top Progress Bars Container -->
    <div style="position: absolute; top: 20px; left: 24px; right: 24px; display: flex; gap: 8px; z-index: 10;">
      ${[0, 1, 2, 3].map(i => `
        <div style="flex: 1; height: 4px; background: rgba(255,255,255,0.25); border-radius: 2px; overflow: hidden;">
          <div id="reel-bar-${i}" style="width: 0%; height: 100%; background: #ffffff; transition: width 0.1s linear;"></div>
        </div>
      `).join('')}
    </div>

    <!-- Top Right Close Button -->
    <button id="btn-close-reel" style="position: absolute; top: 32px; right: 28px; width: 44px; height: 44px; border-radius: 50%; background: rgba(255,255,255,0.15); border: none; color: white; display: flex; align-items: center; justify-content: center; cursor: pointer; z-index: 11; font-size: 20px; font-weight: 800;">✕</button>

    <!-- Navigation Tap Areas (Left for prev, Right for next) -->
    <div id="reel-tap-left" style="position: absolute; top: 0; bottom: 0; left: 0; width: 35vw; z-index: 8; cursor: pointer;"></div>
    <div id="reel-tap-right" style="position: absolute; top: 0; bottom: 0; right: 0; width: 35vw; z-index: 8; cursor: pointer;"></div>

    <!-- Central Slide Display Container -->
    <div id="reel-slide-container" style="width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; position: relative; z-index: 5;">
    </div>
  `;

  document.body.appendChild(modalOverlay);

  const slideContainer = modalOverlay.querySelector('#reel-slide-container');
  const closeBtn = modalOverlay.querySelector('#btn-close-reel');
  const tapLeft = modalOverlay.querySelector('#reel-tap-left');
  const tapRight = modalOverlay.querySelector('#reel-tap-right');

  const slidesData = [
    {
      bg: "radial-gradient(circle at center, #5c3500 0%, #08080c 75%)",
      render: () => `
        <div style="text-align: center; animation: zoomIn 0.35s ease-out; color: white; padding: 40px;">
          <span style="font-size: 24px; font-weight: 900; color: #ffcc00; letter-spacing: 0.1em;">2026</span>
          <h2 style="font-size: 22px; font-weight: 600; color: rgba(255,255,255,0.8); margin: 16px 0 32px 0;">Tus minutos totales escuchados</h2>
          <span style="font-size: 80px; font-weight: 900; color: #ffffff; display: block; line-height: 1; letter-spacing: -0.03em;">${stats.totalMinutes}</span>
          <span style="font-size: 24px; font-weight: 800; color: rgba(255,255,255,0.6); margin-top: 10px; display: block;">minutos</span>
        </div>
      `
    },
    {
      bg: "radial-gradient(circle at center, #232070 0%, #08080c 75%)",
      render: () => `
        <div style="text-align: center; animation: zoomIn 0.35s ease-out; color: white; padding: 40px; display: flex; flex-direction: column; align-items: center;">
          <span style="font-size: 18px; font-weight: 900; color: #5856d6; letter-spacing: 0.12em; text-transform: uppercase;">TU ARTISTA TOP #1</span>
          <div style="width: 220px; height: 220px; border-radius: 50%; overflow: hidden; margin: 28px 0; border: 5px solid #5856d6; box-shadow: 0 16px 48px rgba(88, 86, 214, 0.6);">
            <img src="${stats.topArtistArt}" style="width: 100%; height: 100%; object-fit: cover;">
          </div>
          <h2 style="font-size: 38px; font-weight: 900; color: #ffffff; margin: 0 0 8px 0;">${escapeHtmlAttr(stats.topArtistName)}</h2>
          <span style="font-size: 17px; font-weight: 700; color: rgba(255,255,255,0.75);">${stats.topArtistMinutes} minutos escuchados</span>
        </div>
      `
    },
    {
      bg: "radial-gradient(circle at center, #6b0c23 0%, #08080c 75%)",
      render: () => `
        <div style="text-align: center; animation: zoomIn 0.35s ease-out; color: white; padding: 40px; display: flex; flex-direction: column; align-items: center;">
          <span style="font-size: 18px; font-weight: 900; color: #ff2d55; letter-spacing: 0.12em; text-transform: uppercase;">TU CANCIÓN TOP #1</span>
          <div style="width: 220px; height: 220px; border-radius: 24px; overflow: hidden; margin: 28px 0; border: 4px solid #ff2d55; box-shadow: 0 16px 48px rgba(255, 45, 85, 0.6);">
            <img src="${stats.topSongArt}" style="width: 100%; height: 100%; object-fit: cover;">
          </div>
          <h2 style="font-size: 32px; font-weight: 900; color: #ffffff; margin: 0 0 6px 0;">${escapeHtmlAttr(stats.topSongTitle)}</h2>
          <span style="font-size: 18px; font-weight: 700; color: rgba(255,255,255,0.85);">${escapeHtmlAttr(stats.topSongArtist)}</span>
          <span style="font-size: 15px; font-weight: 700; color: #ff2d55; margin-top: 10px;">${stats.topSongPlays} reproducciones</span>
        </div>
      `
    },
    {
      bg: "radial-gradient(circle at center, #0f5424 0%, #08080c 75%)",
      render: () => `
        <div style="text-align: center; animation: zoomIn 0.35s ease-out; color: white; padding: 40px; display: flex; flex-direction: column; align-items: center;">
          <span style="font-size: 18px; font-weight: 900; color: #34c759; letter-spacing: 0.12em; text-transform: uppercase;">TU GÉNERO TOP #1</span>
          <div style="background: rgba(255,255,255,0.06); border: 2px solid #34c759; border-radius: 28px; padding: 36px 48px; margin: 32px 0; box-shadow: 0 16px 48px rgba(52, 199, 89, 0.4); text-align: center;">
            <h2 style="font-size: 42px; font-weight: 900; color: #ffffff; margin: 0 0 8px 0;">${escapeHtmlAttr(stats.topGenreName)}</h2>
            <span style="font-size: 26px; font-weight: 900; color: #34c759;">${stats.topGenrePercentage}% de tu música</span>
          </div>
        </div>
      `
    }
  ];

  function showSlide(idx) {
    if (idx < 0) idx = 0;
    if (idx >= totalSlides) {
      closeModal();
      return;
    }
    currentSlide = idx;
    modalOverlay.style.background = slidesData[currentSlide].bg;
    slideContainer.innerHTML = slidesData[currentSlide].render();

    // Reset progress bars
    for (let i = 0; i < totalSlides; i++) {
      const bar = modalOverlay.querySelector(`#reel-bar-${i}`);
      if (bar) {
        if (i < currentSlide) bar.style.width = "100%";
        else if (i > currentSlide) bar.style.width = "0%";
      }
    }

    startTimer();
  }

  function startTimer() {
    if (timerInterval) clearInterval(timerInterval);
    let progress = 0;
    const currentBar = modalOverlay.querySelector(`#reel-bar-${currentSlide}`);

    timerInterval = setInterval(() => {
      progress += 2.5; // 100% in 4 seconds
      if (currentBar) currentBar.style.width = `${progress}%`;
      if (progress >= 100) {
        clearInterval(timerInterval);
        showSlide(currentSlide + 1);
      }
    }, 100);
  }

  function closeModal() {
    if (timerInterval) clearInterval(timerInterval);
    if (modalOverlay) modalOverlay.remove();
  }

  closeBtn.onclick = closeModal;
  tapLeft.onclick = () => showSlide(currentSlide - 1);
  tapRight.onclick = () => showSlide(currentSlide + 1);

  showSlide(0);
}

// --- Search Engine matching BusquedaScreen.kt ---

function createSongRowElement(song) {
  const row = document.createElement('div');
  row.style.display = "flex";
  row.style.alignItems = "center";
  row.style.padding = "10px 14px";
  row.style.background = "rgba(255,255,255,0.04)";
  row.style.borderRadius = "14px";
  row.style.border = "1px solid rgba(255,255,255,0.06)";
  row.style.cursor = "pointer";
  row.style.transition = "all 0.15s ease";

  row.addEventListener('mouseenter', () => {
    row.style.background = "rgba(255,255,255,0.09)";
  });
  row.addEventListener('mouseleave', () => {
    row.style.background = "rgba(255,255,255,0.04)";
  });

  row.innerHTML = `
    <img src="${upgradeThumbQuality(song.artwork)}" style="width: 44px; height: 44px; border-radius: 10px; object-fit: cover; margin-right: 14px; flex-shrink: 0;">
    <div style="display: flex; flex-direction: column; flex-grow: 1; min-width: 0;">
      <span style="font-size: 14.5px; font-weight: 800; color: white; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${escapeHtmlAttr(song.title)}</span>
      <span style="font-size: 12.5px; color: rgba(255,255,255,0.6); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-top: 2px;">${escapeHtmlAttr(song.artist || '')}</span>
    </div>
    <div style="width: 34px; height: 34px; border-radius: 50%; background: rgba(255,255,255,0.1); display: flex; align-items: center; justify-content: center; color: white; margin-left: 12px;">
      <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
    </div>
  `;

  row.onclick = () => {
    playTrackDetails(song.id, song.title, song.artist, song.artwork);
  };

  return row;
}

let currentSearchSelectedTab = 0; // 0: Todo, 1: Artistas, 2: Álbumes, 3: Canciones

function renderSearchResultsCategorized(resultsMap, selectedTab = 0) {
  setHeaderVisible(true);
  setHeaderSearchPillVisible(true);
  currentSearchSelectedTab = selectedTab;

  const artists = resultsMap?.["Artistas"] || [];
  const albums = resultsMap?.["Álbumes"] || [];
  const songs = resultsMap?.["Canciones"] || [];
  const topResult = resultsMap?.["TopResult"];

  if (!resultsMap || (artists.length === 0 && albums.length === 0 && songs.length === 0 && !topResult)) {
    contentArea.innerHTML = `
      <div style="padding: 60px 36px; text-align: center; color: rgba(255,255,255,0.7);">
        <svg viewBox="0 0 24 24" width="48" height="48" fill="currentColor" style="opacity: 0.5; margin-bottom: 16px;"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
        <h2 style="font-size: 22px; font-weight: 800; color: white; margin: 0 0 8px 0;">No se encontraron resultados</h2>
        <p style="font-size: 14px; margin: 0;">Intenta buscar con otros términos o el nombre de un artista.</p>
      </div>
    `;
    return;
  }

  contentArea.innerHTML = '';

  const container = document.createElement('div');
  container.style.width = "100%";
  container.style.boxSizing = "border-box";
  container.style.padding = "20px 32px 60px 32px";
  container.style.animation = "fadeIn 0.25s ease-out";

  // 1. Filter Tabs Bar matching mobile BusquedaScreen.kt
  const tabsBar = document.createElement('div');
  tabsBar.style.display = "flex";
  tabsBar.style.gap = "10px";
  tabsBar.style.marginBottom = "24px";
  tabsBar.style.overflowX = "auto";
  tabsBar.style.paddingBottom = "4px";

  const tabNames = ["Todo", "Artistas", "Álbumes", "Canciones"];
  tabNames.forEach((name, idx) => {
    const isSelected = idx === selectedTab;
    const btn = document.createElement('button');
    btn.textContent = name;
    btn.style.background = isSelected ? "var(--accent-red, #e91e63)" : "rgba(255,255,255,0.08)";
    btn.style.border = "none";
    btn.style.color = isSelected ? "#ffffff" : "rgba(255,255,255,0.75)";
    btn.style.padding = "8px 20px";
    btn.style.borderRadius = "20px";
    btn.style.fontSize = "14px";
    btn.style.fontWeight = isSelected ? "800" : "600";
    btn.style.cursor = "pointer";
    btn.style.transition = "all 0.15s ease";
    btn.style.whiteSpace = "nowrap";

    btn.onclick = () => {
      renderSearchResultsCategorized(resultsMap, idx);
    };

    tabsBar.appendChild(btn);
  });

  container.appendChild(tabsBar);

  // Build items array depending on selected tab (matching mobile BusquedaScreen.kt logic)
  let itemsToRender = [];
  if (selectedTab === 0) {
    // "Todo": Prioritize Top Result / Exact Artist match at position #1
    const seen = new Set();
    if (topResult && topResult.id) {
      seen.add(topResult.id);
      itemsToRender.push(topResult);
    }
    
    // Top artists matching exact query
    const topArtist = artists[0];
    if (topArtist && topArtist.id && !seen.has(topArtist.id)) {
      seen.add(topArtist.id);
      itemsToRender.push(topArtist);
    }

    // Top songs & albums
    songs.forEach(it => {
      if (it && it.id && !seen.has(it.id)) {
        seen.add(it.id);
        itemsToRender.push(it);
      }
    });

    albums.forEach(it => {
      if (it && it.id && !seen.has(it.id)) {
        seen.add(it.id);
        itemsToRender.push(it);
      }
    });

    artists.forEach(it => {
      if (it && it.id && !seen.has(it.id)) {
        seen.add(it.id);
        itemsToRender.push(it);
      }
    });

  } else if (selectedTab === 1) {
    itemsToRender = artists;
  } else if (selectedTab === 2) {
    itemsToRender = albums;
  } else if (selectedTab === 3) {
    itemsToRender = songs;
  }

  if (itemsToRender.length === 0) {
    const emptyMsg = document.createElement('div');
    emptyMsg.style.padding = "40px 0";
    emptyMsg.style.textAlign = "center";
    emptyMsg.style.color = "rgba(255,255,255,0.6)";
    emptyMsg.innerHTML = `<p style="font-size: 15px; font-weight: 600;">No hay resultados en esta categoría.</p>`;
    container.appendChild(emptyMsg);
    contentArea.appendChild(container);
    return;
  }

  // 2. Render Vertical List of Item Rows matching BusquedaScreen.kt
  const listContainer = document.createElement('div');
  listContainer.style.display = "flex";
  listContainer.style.flexDirection = "column";
  listContainer.style.gap = "4px";

  itemsToRender.forEach(item => {
    const isArtist = item.type === 'artist' || (item.id && item.id.startsWith('UC'));
    const isAlbum = item.type === 'album' || item.type === 'playlist' || item.type === 'single' || item.type === 'ep' || (item.id && (item.id.startsWith('MPRE') || item.id.startsWith('VL') || item.id.startsWith('OLAK')));

    const row = document.createElement('div');
    row.style.display = "flex";
    row.style.alignItems = "center";
    row.style.padding = "10px 14px";
    row.style.borderRadius = "12px";
    row.style.background = "transparent";
    row.style.cursor = "pointer";
    row.style.transition = "background-color 0.15s ease";

    row.addEventListener('mouseenter', () => {
      row.style.background = "rgba(255,255,255,0.07)";
    });
    row.addEventListener('mouseleave', () => {
      row.style.background = "transparent";
    });

    let typeLabel = "Canción";
    if (isArtist) typeLabel = "Artista";
    else if (isAlbum) typeLabel = "Álbum";

    const subtitleText = isArtist ? "Artista" : `${typeLabel} · ${escapeHtmlAttr(item.artist || '')}`;

    row.innerHTML = `
      <div style="width: 56px; height: 56px; border-radius: ${isArtist ? '50%' : '8px'}; overflow: hidden; background: #222226; flex-shrink: 0; margin-right: 14px; box-shadow: 0 4px 12px rgba(0,0,0,0.3);">
        <img src="${upgradeThumbQuality(item.artwork)}" style="width: 100%; height: 100%; object-fit: cover;">
      </div>
      <div style="display: flex; flex-direction: column; flex-grow: 1; min-width: 0; margin-right: 12px;">
        <span style="font-size: 15px; font-weight: 600; color: white; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${escapeHtmlAttr(item.title)}</span>
        <span style="font-size: 13px; color: rgba(255,255,255,0.55); margin-top: 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${subtitleText}</span>
      </div>
      <div class="btn-item-options" style="width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: rgba(255,255,255,0.5); cursor: pointer; flex-shrink: 0;" title="Opciones">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"/></svg>
      </div>
    `;

    row.onclick = (e) => {
      if (e.target.closest('.btn-item-options')) {
        e.stopPropagation();
        return;
      }

      if (isArtist) {
        loadArtistPage(item.id, item.title);
      } else if (isAlbum) {
        loadPlaylistContents(item.id, item.title);
      } else {
        playTrackDetails(item.id, item.title, item.artist, item.artwork);
      }
    };

    listContainer.appendChild(row);
  });

  container.appendChild(listContainer);
  contentArea.appendChild(container);
}

// --- Search Engine categorization matching BusquedaScreen.kt ---

function parseSearchResultsCategorized(data, searchQuery = "") {
  const categories = { "Artistas": [], "Álbumes": [], "Canciones": [] };
  if (!data) return categories;

  const isUnofficialArtist = (name) => {
    if (!name || typeof name !== 'string') return true;
    const lower = name.toLowerCase();
    const banned = ["u7u", "multivers ai", "montgomery", "xx", "tribute", "farewell", "fan club", "karaoke", "remix", "instrumental", "parody", "covers", "slowed", "reverb"];
    return banned.some(b => lower.includes(b));
  };

  const isPodcast = (title, sub) => {
    const text = (title + " " + sub).toLowerCase();
    return ["podcast", "episodio", "episode", "show", "hablando de", "conversaciones"].some(w => text.includes(w));
  };

  const isStandaloneVideo = (title, sub) => {
    const text = (title + " " + sub).toLowerCase();
    return ["video oficial", "official video", "music video", "[video]", "(video)", "lyric video", "video musical"].some(w => text.includes(w));
  };

  const sectionList = data.contents?.sectionListRenderer?.contents
    || data.contents?.tabbedSearchResultsRenderer?.tabs?.[0]?.tabRenderer?.content?.sectionListRenderer?.contents
    || [];

  sectionList.forEach(sec => {
    if (sec.musicCardShelfRenderer) {
      const card = sec.musicCardShelfRenderer;
      const cardTitle = card.title?.runs?.[0]?.text || card.header?.musicCardShelfHeaderBasicRenderer?.title?.runs?.[0]?.text;
      const cardSub = card.subtitle?.runs?.[0]?.text || card.subtitle?.runs?.map(r => r.text).join('') || "";
      const cardThumb = upgradeThumbQuality(extractThumbnail(card));
      const cardBrowse = card.title?.runs?.[0]?.navigationEndpoint?.browseEndpoint?.browseId || card.navigationEndpoint?.browseEndpoint?.browseId;
      const cardVideo = card.buttons?.[0]?.buttonRenderer?.navigationEndpoint?.watchEndpoint?.videoId || card.onTap?.watchEndpoint?.videoId;

      if (cardTitle && !isPodcast(cardTitle, cardSub) && !isStandaloneVideo(cardTitle, cardSub)) {
        const subLower = cardSub.toLowerCase();
        let cardType = 'song';
        if ((subLower.includes('artista') || subLower.includes('oyentes') || subLower.includes('suscriptores')) && !cardVideo) cardType = 'artist';
        else if (subLower.includes('álbum') || subLower.includes('album') || subLower.includes('playlist') || (cardBrowse && (cardBrowse.startsWith('MPRE') || cardBrowse.startsWith('VL') || cardBrowse.startsWith('OLAK')))) cardType = 'album';

        categories["TopResult"] = {
          id: (cardType === 'artist' || cardType === 'album') ? (cardBrowse || cardVideo || "top-1") : (cardVideo || cardBrowse || "top-1"),
          title: cardTitle,
          artist: cardSub || "Música",
          artwork: cardThumb,
          type: cardType
        };
      }
    }

    const rawItems = [];
    if (sec.musicShelfRenderer) {
      rawItems.push(...(sec.musicShelfRenderer.contents || []));
    } else if (sec.itemSectionRenderer) {
      rawItems.push(...(sec.itemSectionRenderer.contents || []));
    } else if (sec.musicCarouselShelfRenderer) {
      rawItems.push(...(sec.musicCarouselShelfRenderer.contents || sec.musicCarouselShelfRenderer.items || []));
    }

    rawItems.forEach(it => {
      const item = it.musicResponsiveListItemRenderer || it.musicTwoRowItemRenderer;
      if (!item) return;

      const itemTitle = item.title?.runs?.[0]?.text || item.flexColumns?.[0]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.[0]?.text || "Item";
      const flex1Runs = item.subtitle?.runs || item.flexColumns?.[1]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs || [];
      const subtitleText = flex1Runs.map(r => r.text).join("");
      const typeStr = flex1Runs[0]?.text || "";

      if (isPodcast(itemTitle, subtitleText) || isStandaloneVideo(itemTitle, subtitleText)) {
        return;
      }

      const thumb = upgradeThumbQuality(extractThumbnail(item));
      let videoId = item.navigationEndpoint?.watchEndpoint?.videoId || item.playlistItemData?.videoId || item.doubleTapCommand?.watchEndpoint?.videoId;
      
      // TITLE BROWSE ID (Safe for Albums & Playlists!)
      let titleBrowseId = item.navigationEndpoint?.browseEndpoint?.browseId || item.flexColumns?.[0]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.[0]?.navigationEndpoint?.browseEndpoint?.browseId;
      
      // Fallback browseId (only if titleBrowseId is missing)
      let browseId = titleBrowseId || flex1Runs[0]?.navigationEndpoint?.browseEndpoint?.browseId;

      const typeLower = typeStr.toLowerCase();
      const subLower = subtitleText.toLowerCase();
      const titleLower = itemTitle.toLowerCase();

      // STRICT EXCLUSION OF ALL VIDEO & PROFILE ITEMS!
      const isVideoOrProfile = typeLower.includes('video') || typeLower.includes('vídeo') || typeLower.includes('profile')
        || subLower.includes('video') || subLower.includes('vídeo') || subLower.includes('views') || subLower.includes('vistas') || subLower.includes('profile') || subLower.includes('@')
        || titleLower.includes('vlog') || titleLower.includes('imitador') || titleLower.includes('tributo');

      if (isVideoOrProfile) return;

      const isArtist = (typeLower === 'artista' || typeLower === 'artist' || subLower.includes('artista') || subLower.includes('oyentes') || subLower.includes('suscriptores')) 
                    && !videoId 
                    && !['canción', 'cancion', 'song', 'vídeo', 'video', 'álbum', 'album', 'playlist', 'single', 'ep', 'sencillo'].some(k => typeLower.includes(k));

      const isAlbum = ['álbum', 'album', 'playlist', 'single', 'ep', 'sencillo'].some(k => typeLower.includes(k)) 
                   || (titleBrowseId && (titleBrowseId.startsWith('MPRE') || titleBrowseId.startsWith('VL') || titleBrowseId.startsWith('OLAK')));

      let itemType = 'song';
      if (isArtist) itemType = 'artist';
      else if (isAlbum) itemType = 'album';

      // For albums/playlists: ONLY use titleBrowseId or videoId (NEVER take artist UC... from subtitle!)
      let itemId = videoId || browseId;
      if (itemType === 'artist') {
        itemId = browseId || videoId;
      } else if (itemType === 'album') {
        itemId = (titleBrowseId && !titleBrowseId.startsWith('UC')) ? titleBrowseId : (videoId || titleBrowseId);
      }

      const itemObj = {
        id: itemId || "item_" + Math.random().toString(36).substr(2, 9),
        title: itemTitle,
        artist: subtitleText || "Música",
        artwork: thumb,
        type: itemType
      };

      if (itemType === 'artist') {
        if (!isUnofficialArtist(itemTitle) && !categories["Artistas"].some(x => x.id === itemObj.id)) categories["Artistas"].push(itemObj);
      } else if (itemType === 'album') {
        if (!categories["Álbumes"].some(x => x.id === itemObj.id)) categories["Álbumes"].push(itemObj);
      } else {
        if (isOfficialSong(itemTitle) && !categories["Canciones"].some(x => x.id === itemObj.id)) categories["Canciones"].push(itemObj);
      }
    });
  });

  // Relevance Sorting: Exact match to query first!
  const qLower = searchQuery.toLowerCase().trim();
  if (qLower) {
    const rankScore = (it) => {
      const t = it.title.toLowerCase().trim();
      if (t === qLower) return 0;
      if (t.startsWith(qLower)) return 1;
      if (t.includes(qLower)) return 2;
      return 3;
    };

    categories["Artistas"].sort((a, b) => rankScore(a) - rankScore(b));
    categories["Álbumes"].sort((a, b) => rankScore(a) - rankScore(b));
    categories["Canciones"].sort((a, b) => rankScore(a) - rankScore(b));
  }

  return categories;
}


// --- Home Feed Algorithm matching InicioScreen.kt ---

async function performSearch(query, shouldPushHistory = true) {
  if (!query || query.trim().length === 0) {
    renderExploreCategoriesView();
    return;
  }

  const q = query.trim();
  setHeaderVisible(true);
  setHeaderSearchPillVisible(true);
  contentArea.innerHTML = `<div class="loading-spinner-container"><div class="spinner"></div><p>Buscando "${escapeHtmlAttr(q)}"...</p></div>`;
  document.getElementById('page-title').textContent = `Búsqueda: ${q}`;

  if (shouldPushHistory) {
    pushNavigation({ name: 'search', params: { query: q } });
  }

  try {
    let data = await callInnerTubeAPI('search', { query: q }, WEB_CONTEXT).catch(() => null);
    if (!data) {
      // Fallback search directly
      data = await callInnerTubeAPI('search', { query: `${q} canciones` }, WEB_CONTEXT).catch(() => null);
    }

    const resultsMap = parseSearchResultsCategorized(data, q);

    let artistArt = "";
    if (resultsMap['Artistas'] && resultsMap['Artistas'].length > 0) {
      artistArt = resultsMap['Artistas'][0].artwork || "";
    }
    saveSearchQuery(q, artistArt);

    renderSearchResultsCategorized(resultsMap);
  } catch (err) {
    console.warn("Search error:", err);
    // Never fall back to categories view! Render search results with query term!
    const fallbackMap = {
      "Top resultados": [{ id: "search-fallback-1", title: q, artist: "Búsqueda en RayMusic", artwork: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600", type: "song" }]
    };
    renderSearchResultsCategorized(fallbackMap);
  }
}

async function loadHomeFeed() {
  setHeaderVisible(true);
  setHeaderSearchPillVisible(false);

  try {
    const savedRecent = JSON.parse(localStorage.getItem('raymusic_recently_played') || '[]');
    if (Array.isArray(savedRecent) && savedRecent.length > 0) {
      recentlyPlayed = savedRecent;
    } else {
      recentlyPlayed = [];
    }

    // IF NO MUSIC HAS BEEN LISTENED TO YET: Show clean initial empty state!
    if (!recentlyPlayed || recentlyPlayed.length === 0) {
      contentArea.innerHTML = `
        <div style="padding: 32px 36px; width: 100%; box-sizing: border-box; color: white; animation: fadeIn 0.25s ease-out;">
          <h1 style="font-size: 34px; font-weight: 900; letter-spacing: -0.02em; color: white; margin: 0 0 20px 0;">Inicio</h1>
          
          <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 75px 24px; text-align: center; background: rgba(255,255,255,0.03); border: 1px dashed rgba(255,255,255,0.12); border-radius: 28px; margin-top: 10px;">
            <div style="width: 76px; height: 76px; border-radius: 50%; background: rgba(255, 45, 85, 0.15); display: flex; align-items: center; justify-content: center; margin-bottom: 20px; box-shadow: 0 8px 24px rgba(255, 45, 85, 0.2);">
              <svg viewBox="0 0 24 24" width="38" height="38" fill="#ff2d55"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>
            </div>
            <h2 style="font-size: 26px; font-weight: 900; color: white; margin: 0 0 10px 0; letter-spacing: -0.01em;">Comienza a escuchar música</h2>
            <p style="font-size: 15px; color: rgba(255,255,255,0.65); max-width: 480px; line-height: 1.45; margin: 0 0 28px 0; font-weight: 600;">Usa la búsqueda o explora canciones en la app. Conforme escuches música, aquí se mostrarán automáticamente tus recomendaciones de canciones, artistas y playlists.</p>
            <button onclick="document.getElementById('search-input')?.focus()" style="background: #ff2d55; color: white; border: none; padding: 13px 32px; border-radius: 24px; font-size: 14.5px; font-weight: 800; cursor: pointer; box-shadow: 0 8px 28px rgba(255, 45, 85, 0.45); transition: transform 0.15s ease;">Buscar música</button>
          </div>
        </div>
      `;
      return;
    }

    contentArea.innerHTML = `<div class="loading-spinner-container"><div class="spinner"></div><p>Cargando Inicio...</p></div>`;

    // 1. Extract valid listened seeds from recentlyPlayed
    const validSeeds = [];
    const invalidNames = ['song', 'playlist', 'artista', 'música', 'musica', '2018', '2019', '2020', '2021', '2022', '2023', '2024', '2025', '2026', 'undefined', 'null'];

    recentlyPlayed.forEach(t => {
      if (t.artist && typeof t.artist === 'string' && t.id) {
        const a = t.artist.trim();
        if (a.length > 1 && !invalidNames.includes(a.toLowerCase())) {
          if (!validSeeds.some(s => s.artist.toLowerCase() === a.toLowerCase())) {
            validSeeds.push({ id: t.id, title: t.title, artist: a, artwork: t.artwork });
          }
        }
      }
    });

    if (validSeeds.length === 0) {
      const first = recentlyPlayed[0];
      if (first && first.title) {
        validSeeds.push({ id: first.id || "Zi_XLOBDo_Y", title: first.title, artist: first.artist || "Música", artwork: first.artwork });
      }
    }

    const seed1 = validSeeds[0];
    const seed2 = validSeeds[1] || validSeeds[0];
    const seed3 = validSeeds[2] || validSeeds[0];

    const artist1 = seed1.artist;
    const artist2 = seed2.artist;
    const artist3 = seed3.artist;

    // Call Watch Next / Search APIs in parallel for listened seeds
    const [resArt1, resArt2, resArt3, resPlaylists, watchNextRes] = await Promise.all([
      callInnerTubeAPI('search', { query: `${artist1} canciones` }, WEB_CONTEXT).catch(() => null),
      callInnerTubeAPI('search', { query: `${artist2} canciones` }, WEB_CONTEXT).catch(() => null),
      callInnerTubeAPI('search', { query: `${artist3} canciones` }, WEB_CONTEXT).catch(() => null),
      callInnerTubeAPI('search', { query: `${artist1} playlist` }, WEB_CONTEXT).catch(() => null),
      callInnerTubeAPI('next', { videoId: seed1.id }, WEB_CONTEXT).catch(() => null)
    ]);

    contentArea.innerHTML = `
      <div style="padding: 24px 36px 10px 36px; width: 100%; box-sizing: border-box;">
        <h1 style="font-size: 34px; font-weight: 900; letter-spacing: -0.02em; color: white; margin: 0;">Inicio</h1>
      </div>
    `;

    const watchNextItems = [];
    if (watchNextRes) {
      const results = watchNextRes.contents?.singleColumnMusicWatchNextResultsRenderer?.tabbedRenderer?.watchNextTabbedResultsRenderer?.tabs?.[0]?.tabRenderer?.content?.musicQueueRenderer?.content?.playlistPanelRenderer?.contents || [];
      results.forEach(it => {
        const item = it.playlistPanelVideoRenderer;
        if (item && item.videoId) {
          const title = item.title?.runs?.[0]?.text || "Canción";
          const artist = item.shortBylineText?.runs?.[0]?.text || artist1;
          const thumb = upgradeThumbQuality(extractThumbnail(item));
          if (isOfficialSong(title)) {
            watchNextItems.push({
              id: item.videoId,
              title: title,
              artist: artist,
              artwork: thumb,
              type: 'song'
            });
          }
        }
      });
    }

    const parsedArt1 = resArt1 ? parseSearchResultsCategorized(resArt1) : {};
    const parsedArt2 = resArt2 ? parseSearchResultsCategorized(resArt2) : {};
    const parsedArt3 = resArt3 ? parseSearchResultsCategorized(resArt3) : {};
    const parsedPlaylists = resPlaylists ? parseSearchResultsCategorized(resPlaylists) : {};

    const isCleanSong = (s) => {
      if (!s || !s.title || !s.artist) return false;
      const t = s.title.toLowerCase();
      const a = s.artist.toLowerCase();
      return !t.includes('video') && !t.includes('vídeo') && !t.includes('vlog') && !t.includes('tributo') && !t.includes('imitador')
          && !a.includes('video') && !a.includes('vídeo') && !a.includes('views') && !a.includes('vistas') && !a.includes('profile') && !a.includes('@');
    };

    const songs1 = (parsedArt1['Canciones'] || []).filter(s => isOfficialSong(s.title) && isCleanSong(s)).map(s => ({ ...s, type: 'song' }));
    const songs2 = (parsedArt2['Canciones'] || []).filter(s => isOfficialSong(s.title) && isCleanSong(s)).map(s => ({ ...s, type: 'song' }));
    const songs3 = (parsedArt3['Canciones'] || []).filter(s => isOfficialSong(s.title) && isCleanSong(s)).map(s => ({ ...s, type: 'song' }));

    // 1. Sugerencias destacadas para ti (Songs)
    const featured = [...watchNextItems, ...songs1, ...songs2].filter(s => s.type === 'song' && isCleanSong(s)).slice(0, 20);
    if (featured.length > 0) renderAppleTopPicksCarousel(getI18nText('featured_suggestions'), featured);

    // 2. Selecciones rápidas
    const quick = [...songs2, ...songs3, ...watchNextItems].filter(s => s.type === 'song' && isCleanSong(s)).slice(0, 12);
    if (quick.length > 0) renderCarouselSection(getI18nText('quick_picks'), quick);

    // 3. Sigue escuchando
    if (recentlyPlayed && recentlyPlayed.length > 0) {
      renderCarouselSection(getI18nText('keep_listening'), recentlyPlayed);
    }

    // 4. Similar a [Nombre del Artista Escuchado 1]
    const similar1 = (parsedArt1['Artistas'] && parsedArt1['Artistas'].length > 0) 
      ? [...parsedArt1['Artistas'].slice(0, 2), ...songs1] 
      : (songs1.length > 0 ? songs1 : watchNextItems);
    renderCarouselSection(`Similar a ${artist1}`, similar1);

    // 5. Similar a [Nombre del Artista Escuchado 2]
    if (artist2 !== artist1 && songs2.length > 0) {
      const similar2 = (parsedArt2['Artistas'] && parsedArt2['Artistas'].length > 0) 
        ? [...parsedArt2['Artistas'].slice(0, 2), ...songs2] 
        : songs2;
      renderCarouselSection(`Similar a ${artist2}`, similar2);
    }

    // 6. Playlist destacada
    let featuredPlaylists = (parsedPlaylists['Álbumes'] || parsedArt1['Álbumes'] || []).map(p => ({
      id: p.id,
      title: p.title,
      artist: p.artist || artist1,
      artwork: p.artwork,
      type: 'playlist'
    }));
    if (featuredPlaylists.length === 0) {
      featuredPlaylists = [
        { id: "VLPL4fGSI1pDJn6O1LS0XSdF3RyO0Aq_6mUp", title: `${artist1} Essentials`, artist: "RayMusic Curated", artwork: seed1.artwork || "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600", type: "playlist" },
        { id: "VLPL4fGSI1pDJn6O1LS0XSdF3RyO0Aq_6mUp", title: `Éxitos de ${artist1}`, artist: "RayMusic Hits", artwork: seed1.artwork || "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600", type: "playlist" }
      ];
    }
    renderCarouselSection(getI18nText('featured_playlist'), featuredPlaylists);

    // 7. Porque escuchaste a [Nombre del Artista Escuchado 3]
    if (artist3) {
      const because3 = (parsedArt3['Artistas'] && parsedArt3['Artistas'].length > 0) 
        ? [...parsedArt3['Artistas'].slice(0, 2), ...songs3] 
        : (songs3.length > 0 ? songs3 : songs1);
      renderCarouselSection(`Porque escuchaste a ${artist3}`, because3);
    }

    // 8. Replay: La música que más escuchas
    renderReplayHomeSection();

  } catch (err) {
    console.warn("Home feed error:", err);
    renderHomeOffline();
  }
}

function renderReplayHomeSection() {
  const section = document.createElement('section');
  section.className = "content-section";
  section.style.marginBottom = "44px";
  section.style.padding = "0 32px";

  const sectionHeader = document.createElement('div');
  sectionHeader.className = "section-header";
  sectionHeader.style.marginBottom = "14px";
  sectionHeader.innerHTML = `
    <h2 class="section-title-sub" style="font-size: 22px; font-weight: 800; color: white; margin: 0 0 2px 0; cursor: pointer;">${getI18nText('replay_title')} &gt;</h2>
    <span style="font-size: 13.5px; font-weight: 600; color: rgba(255,255,255,0.65);">${getI18nText('replay_sub')}</span>
  `;

  const cardEl = document.createElement('div');
  cardEl.style.width = "280px";
  cardEl.style.height = "380px";
  cardEl.style.borderRadius = "20px";
  cardEl.style.background = "linear-gradient(135deg, #FF9500 0%, #FF2D55 35%, #5856D6 70%, #5AC8FA 100%)";
  cardEl.style.padding = "28px";
  cardEl.style.boxSizing = "border-box";
  cardEl.style.display = "flex";
  cardEl.style.flexDirection = "column";
  cardEl.style.justifyContent = "space-between";
  cardEl.style.cursor = "pointer";
  cardEl.style.boxShadow = "0 16px 40px rgba(0,0,0,0.5)";
  cardEl.style.border = "1px solid rgba(255,255,255,0.2)";
  cardEl.style.transition = "transform 0.2s ease, box-shadow 0.2s ease";

  cardEl.innerHTML = `
    <div style="display: flex; flex-direction: column;">
      <span style="font-size: 28px; font-weight: 900; color: rgba(255,255,255,0.92); letter-spacing: -0.02em; margin-bottom: 14px;">Replay</span>
      <span style="font-size: 32px; font-weight: 900; color: #FFCC00; line-height: 1.15; letter-spacing: -0.02em;">Tu</span>
      <span style="font-size: 32px; font-weight: 900; color: #FFFFFF; line-height: 1.15; letter-spacing: -0.02em;">historia musical</span>
      <span style="font-size: 32px; font-weight: 900; color: #5AC8FA; line-height: 1.15; letter-spacing: -0.02em;">está aquí.</span>
    </div>
    
    <span style="font-size: 14px; font-weight: 700; color: rgba(255,255,255,0.88); line-height: 1.35;">${getI18nText('replay_hero_sub')}</span>
  `;

  cardEl.addEventListener('mouseenter', () => {
    cardEl.style.transform = "translateY(-6px) scale(1.02)";
    cardEl.style.boxShadow = "0 22px 50px rgba(0,0,0,0.65)";
  });
  cardEl.addEventListener('mouseleave', () => {
    cardEl.style.transform = "none";
    cardEl.style.boxShadow = "0 16px 40px rgba(0,0,0,0.5)";
  });

  cardEl.addEventListener('click', () => {
    renderReplayView();
  });

  const headerTitle = sectionHeader.querySelector('.section-title-sub');
  if (headerTitle) {
    headerTitle.addEventListener('click', () => renderReplayView());
  }

  section.appendChild(sectionHeader);
  section.appendChild(cardEl);
  contentArea.appendChild(section);
}

document.addEventListener('DOMContentLoaded', () => {
  applyLanguageTranslations();

  const bindNav = (id, handler) => {
    const el = document.getElementById(id);
    if (el) {
      el.onclick = (e) => {
        e.preventDefault();
        document.querySelectorAll('.sidebar-nav .nav-item').forEach(i => i.classList.remove('active'));
        el.classList.add('active');
        handler();
      };
    }
  };

  bindNav('nav-home', () => { pushNavigation({ name: 'home' }); loadHomeFeed(); });
  bindNav('nav-search', () => { pushNavigation({ name: 'search' }); performSearch(""); });
  bindNav('nav-new', () => { pushNavigation({ name: 'new' }); renderNovedadesView(); });
  bindNav('nav-radio', () => { pushNavigation({ name: 'radio' }); renderRadioView(); });
  bindNav('nav-pins', () => { pushNavigation({ name: 'pins' }); renderPinnedItemsView(); });
  bindNav('nav-recent', () => { pushNavigation({ name: 'recent' }); renderRecentlyPlayedView(); });
  bindNav('nav-songs', () => { pushNavigation({ name: 'songs' }); renderLikedSongsView(); });
  bindNav('nav-albums', () => { pushNavigation({ name: 'albums' }); renderSavedAlbumsView(); });
  bindNav('nav-artists', () => { pushNavigation({ name: 'artists' }); renderFollowedArtistsView(); });
  bindNav('nav-videos', () => { pushNavigation({ name: 'videos' }); renderVideosView(); });
  bindNav('nav-all-playlists', () => { pushNavigation({ name: 'playlists' }); renderAllPlaylistsView(); });
  bindNav('nav-favorites', () => { pushNavigation({ name: 'favorites' }); renderLikedSongsView(); });
  bindNav('playlist-create', () => { showCreatePlaylistModal(); });
  bindNav('nav-settings', () => { pushNavigation({ name: 'settings' }); renderSettingsView(); });
});

