
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
    <h2 class="section-title-sub" style="font-size: 24px; font-weight: 900; color: white; cursor: pointer;">${title} &gt;</h2>
    <div class="carousel-nav" style="display: flex; gap: 8px;">
      <button class="carousel-arrow prev" title="Anterior" style="width: 32px; height: 32px; border-radius: 50%; background: rgba(255,255,255,0.1); border: none; color: white; display: flex; align-items: center; justify-content: center; cursor: pointer;"><svg viewBox="0 0 24 24" width="16" height="16"><path fill="currentColor" d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/></svg></button>
      <button class="carousel-arrow next" title="Siguiente" style="width: 32px; height: 32px; border-radius: 50%; background: rgba(255,255,255,0.1); border: none; color: white; display: flex; align-items: center; justify-content: center; cursor: pointer;"><svg viewBox="0 0 24 24" width="16" height="16"><path fill="currentColor" d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/></svg></button>
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
  if (btnPrev) btnPrev.onclick = () => trackContainer.scrollBy({ left: -440, behavior: 'smooth' });
  if (btnNext) btnNext.onclick = () => trackContainer.scrollBy({ left: 440, behavior: 'smooth' });

  const tags = ["Made for You", "New Release", "Mood for You", "Featuring Artist", "Station for You"];

  cards.forEach((card, idx) => {
    const cardEl = document.createElement('div');
    cardEl.style.flex = "0 0 215px";
    cardEl.style.width = "215px";
    cardEl.style.height = "280px";
    cardEl.style.borderRadius = "18px";
    cardEl.style.position = "relative";
    cardEl.style.overflow = "hidden";
    cardEl.style.cursor = "pointer";
    cardEl.style.boxShadow = "0 12px 30px rgba(0,0,0,0.45)";
    cardEl.style.transition = "transform 0.2s ease, box-shadow 0.2s ease";

    const tagText = tags[idx % tags.length];

    cardEl.innerHTML = `
      <img src="${card.artwork}" style="width: 100%; height: 100%; object-fit: cover;">
      <div style="position: absolute; inset: 0; background: linear-gradient(180deg, rgba(0,0,0,0.05) 0%, rgba(0,0,0,0.3) 50%, rgba(0,0,0,0.88) 100%);"></div>
      <div style="position: absolute; top: 12px; right: 12px; font-weight: 800; font-size: 13px; color: white; text-shadow: 0 2px 6px rgba(0,0,0,0.6); display: flex; align-items: center; gap: 3px;">
        <svg viewBox="0 0 170 170" width="14" height="14" fill="currentColor"><path d="M150.37 130.25c-2.45 5.66-5.35 10.87-8.71 15.66-4.58 6.53-8.33 11.05-11.22 13.56-4.48 4.12-9.28 6.23-14.42 6.35-3.69 0-8.14-1.05-13.32-3.18-5.19-2.12-9.97-3.17-14.34-3.17-4.58 0-9.49 1.05-14.75 3.17-5.26 2.13-9.5 3.24-12.74 3.35-4.34.13-9.14-1.9-14.4-6.1-3.69-3.05-7.77-7.85-12.24-14.4-6.42-9.39-11.45-19.82-15.09-31.3-3.64-11.48-5.46-22.61-5.46-33.39 0-14.34 3.73-26.17 11.19-35.49 7.46-9.32 16.74-14.07 27.84-14.26 4.48 0 9.5 1.15 15.07 3.45 5.56 2.3 9.42 3.45 11.58 3.45 1.95 0 5.86-1.15 11.74-3.45 5.88-2.3 10.66-3.38 14.33-3.26 9.47.4 17.65 3.99 24.54 10.77-8.47 5.12-12.63 12.38-12.48 21.78.16 7.3 2.87 13.43 8.14 18.39 5.27 4.96 11.66 7.6 19.18 7.92-2.5 7.4-5.86 14.77-10.09 22.12zM119.22 31.84c0-6.73 2.45-13.31 7.35-19.74 4.9-6.43 11.08-10.42 18.53-11.97.22 1.3.33 2.44.33 3.42 0 6.64-2.52 13.2-7.56 19.68-5.04 6.48-11.18 10.47-18.42 11.97-.08-.94-.23-2.06-.23-3.36z"/></svg>Music
      </div>
      <div style="position: absolute; bottom: 14px; left: 14px; right: 14px; color: white;">
        <span style="font-size: 11px; font-weight: 700; color: rgba(255,255,255,0.75); text-transform: uppercase; letter-spacing: 0.05em; display: block; margin-bottom: 2px;">${tagText}</span>
        <h3 style="font-size: 18px; font-weight: 800; color: white; line-height: 1.25; margin: 0 0 3px 0; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;">${escapeHtmlAttr(card.title)}</h3>
        <span style="font-size: 12px; font-weight: 600; color: rgba(255,255,255,0.7); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; display: block;">${escapeHtmlAttr(card.artist || '')}</span>
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
