
async function loadHomeFeed() {
  contentArea.innerHTML = `<div class="loading-spinner-container"><div class="spinner"></div><p>Cargando Inicio personalizado...</p></div>`;

  try {
    // 1. Get real recently played songs from localStorage
    const savedRecent = JSON.parse(localStorage.getItem('raymusic_recently_played') || '[]');
    if (Array.isArray(savedRecent) && savedRecent.length > 0) {
      recentlyPlayed = savedRecent;
    }

    contentArea.innerHTML = `
      <div style="padding: 24px 36px 10px 36px; width: 100%; box-sizing: border-box;">
        <h1 style="font-size: 34px; font-weight: 900; letter-spacing: -0.02em; margin-bottom: 24px; color: white;">Inicio</h1>
      </div>
    `;

    const listenedArtists = [];
    const listenedSongIds = [];
    if (recentlyPlayed && recentlyPlayed.length > 0) {
      recentlyPlayed.forEach(t => {
        if (t.artist && !listenedArtists.includes(t.artist) && t.artist !== 'Artista') {
          listenedArtists.push(t.artist);
        }
        if (t.id) listenedSongIds.push(t.id);
      });
    }

    const artist1 = listenedArtists[0] || "Michael Jackson";
    const artist2 = listenedArtists[1] || "Karol G";
    const because1Artist = listenedArtists[2] || "Bruno Mars";
    const because2Artist = listenedArtists[3] || "Queen";

    let homeTubeSections = [];
    try {
      const tubeData = await callInnerTubeAPI('browse', { browseId: "FEmusic_home" }, WEB_CONTEXT, 3000).catch(() => null);
      if (tubeData?.contents?.singleColumnBrowseResultsRenderer?.tabs?.[0]?.tabRenderer?.content?.sectionListRenderer?.contents) {
        homeTubeSections = tubeData.contents.singleColumnBrowseResultsRenderer.tabs[0].tabRenderer.content.sectionListRenderer.contents;
      }
    } catch (e) { }

    const [searchArt1, searchArt2, searchBec1, searchBec2, searchMood] = await Promise.all([
      callInnerTubeAPI('search', { query: `${artist1} hits` }, WEB_CONTEXT).catch(() => null),
      callInnerTubeAPI('search', { query: `${artist2} hits` }, WEB_CONTEXT).catch(() => null),
      callInnerTubeAPI('search', { query: `${because1Artist} canciones` }, WEB_CONTEXT).catch(() => null),
      callInnerTubeAPI('search', { query: `${because2Artist} canciones` }, WEB_CONTEXT).catch(() => null),
      callInnerTubeAPI('search', { query: "musica alegre exitos" }, WEB_CONTEXT).catch(() => null)
    ]);

    const parsedArt1 = searchArt1 ? parseSearchResultsCategorized(searchArt1) : {};
    const parsedArt2 = searchArt2 ? parseSearchResultsCategorized(searchArt2) : {};
    const parsedBec1 = searchBec1 ? parseSearchResultsCategorized(searchBec1) : {};
    const parsedBec2 = searchBec2 ? parseSearchResultsCategorized(searchBec2) : {};
    const parsedMood = searchMood ? parseSearchResultsCategorized(searchMood) : {};

    let featuredSuggestions = [];
    if (parsedArt1['Canciones'] && parsedArt1['Canciones'].length > 0) {
      featuredSuggestions = parsedArt1['Canciones'].slice(0, 4).map(s => ({
        id: s.id,
        title: s.title,
        artist: s.artist || artist1,
        artwork: s.artwork,
        type: 'song'
      }));
    } else if (recentlyPlayed && recentlyPlayed.length > 0) {
      featuredSuggestions = recentlyPlayed.slice(0, 4).map(s => ({
        id: s.id,
        title: s.title,
        artist: s.artist,
        artwork: s.artwork,
        type: 'song'
      }));
    } else {
      featuredSuggestions = (INICIO_DEFAULT_DATA.featuredSuggestions || []).map(item => ({
        id: item.id,
        title: item.title,
        artist: (item.artists || []).join(', ') || 'Artista',
        artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
        type: 'song'
      }));
    }
    renderHeroLandscapeCarousel("Sugerencias destacadas para ti", featuredSuggestions);

    let quickPicks = [];
    if (parsedArt2['Canciones'] && parsedArt2['Canciones'].length > 0) {
      quickPicks = parsedArt2['Canciones'].slice(0, 10).map(s => ({
        id: s.id,
        title: s.title,
        artist: s.artist || artist2,
        artwork: s.artwork,
        type: 'song'
      }));
    } else if (recentlyPlayed && recentlyPlayed.length > 1) {
      quickPicks = recentlyPlayed.slice(1, 12).map(s => ({
        id: s.id,
        title: s.title,
        artist: s.artist,
        artwork: s.artwork,
        type: 'song'
      }));
    } else {
      quickPicks = (INICIO_DEFAULT_DATA.quickPickSongs || []).map(item => ({
        id: item.id,
        title: item.title,
        artist: (item.artists || []).join(', ') || 'Artista',
        artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
        type: 'song'
      }));
    }
    renderCarouselSection("Selecciones rápidas", quickPicks);

    const keepListening = (recentlyPlayed && recentlyPlayed.length > 0) ? recentlyPlayed : (INICIO_DEFAULT_DATA.seleccionesParaTi || []).map(item => ({
      id: item.id,
      title: item.title,
      artist: (item.artists || []).join(', ') || 'Artista',
      artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
      type: 'song'
    }));
    renderCarouselSection("Sigue escuchando", keepListening);

    let similar1 = (parsedArt1['Canciones'] || parsedArt1['Álbumes'] || []).map(s => ({
      id: s.id,
      title: s.title,
      artist: s.artist || artist1,
      artwork: s.artwork,
      type: s.type || 'song'
    }));
    if (similar1.length === 0) {
      similar1 = (INICIO_DEFAULT_DATA.quickPickSongs || []).slice(0, 6).map(item => ({
        id: item.id,
        title: item.title,
        artist: artist1,
        artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
        type: 'song'
      }));
    }
    renderCarouselSection(`Similar a ${artist1}`, similar1);

    let similar2 = (parsedArt2['Canciones'] || parsedArt2['Álbumes'] || []).map(s => ({
      id: s.id,
      title: s.title,
      artist: s.artist || artist2,
      artwork: s.artwork,
      type: s.type || 'song'
    }));
    if (similar2.length === 0) {
      similar2 = (INICIO_DEFAULT_DATA.seleccionesParaTi || []).slice(0, 6).map(item => ({
        id: item.id,
        title: item.title,
        artist: artist2,
        artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
        type: 'song'
      }));
    }
    renderCarouselSection(`Similar a ${artist2}`, similar2);

    // 5. Playlist destacada
    let featuredPlaylists = (parsedArt1['Playlists'] || parsedArt2['Playlists'] || []).map(p => ({
      id: p.id,
      title: p.title,
      artist: p.artist || 'RayMusic',
      artwork: p.artwork,
      type: 'playlist'
    }));
    if (featuredPlaylists.length === 0) {
      featuredPlaylists = (INICIO_DEFAULT_DATA.featuredPlaylists || []).map(item => ({
        id: item.id,
        title: item.title,
        artist: item.author || 'RayMusic',
        artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
        type: 'playlist'
      }));
    }
    renderCarouselSection("Playlist destacada", featuredPlaylists);

    // 6. Porque escuchaste a [Artista] (2 veces, una debajo de la otra)
    let because1Items = (parsedBec1['Canciones'] || []).map(s => ({
      id: s.id,
      title: s.title,
      artist: s.artist || because1Artist,
      artwork: s.artwork,
      type: 'song'
    }));
    if (because1Items.length === 0) {
      because1Items = (INICIO_DEFAULT_DATA.featuredSuggestions || []).map(item => ({
        id: item.id,
        title: item.title,
        artist: because1Artist,
        artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
        type: 'song'
      }));
    }
    renderCarouselSection(`Porque escuchaste a ${because1Artist}`, because1Items);

    let because2Items = (parsedBec2['Canciones'] || []).map(s => ({
      id: s.id,
      title: s.title,
      artist: s.artist || because2Artist,
      artwork: s.artwork,
      type: 'song'
    }));
    if (because2Items.length === 0) {
      because2Items = (INICIO_DEFAULT_DATA.quickPickSongs || []).map(item => ({
        id: item.id,
        title: item.title,
        artist: because2Artist,
        artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
        type: 'song'
      }));
    }
    renderCarouselSection(`Porque escuchaste a ${because2Artist}`, because2Items);

    // 7. Melodías alegres
    let happyMelodies = (parsedMood['Canciones'] || []).map(s => ({
      id: s.id,
      title: s.title,
      artist: s.artist || 'Varios Artistas',
      artwork: s.artwork,
      type: 'song'
    }));
    if (happyMelodies.length === 0) {
      happyMelodies = (INICIO_DEFAULT_DATA.seleccionesParaTi || []).map(item => ({
        id: item.id,
        title: item.title,
        artist: (item.artists || []).join(', ') || 'Artista',
        artwork: (item.thumbnail || '').replace('=w60-h60-l90-rj', '=w600-h600-l90-rj'),
        type: 'song'
      }));
    }
    renderCarouselSection("Melodías alegres", happyMelodies);

    // 8. Replay: La música que más escuchas
    const topSong = recentlyPlayed && recentlyPlayed.length > 0 ? recentlyPlayed[0] : null;
    const replayCard = [{
      id: topSong?.id || "replay-2026",
      title: "Replay 2026",
      artist: topSong ? `Tu canción más escuchada: "${topSong.title}" por ${topSong.artist}` : "Tus canciones y artistas favoritos del año",
      artwork: topSong?.artwork || "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600",
      desc: "Revive tus estadísticas de reproducción, artistas preferidos e hitos musicales en RayMusic.",
      type: topSong ? "song" : "playlist"
    }];
    renderHeroLandscapeCarousel("Replay: La música que más escuchas", replayCard);

  } catch (err) {
    console.warn("Home feed error:", err);
    renderHomeOffline();
  }
}
