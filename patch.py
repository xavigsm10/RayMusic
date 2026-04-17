with open('c:\\Users\\Xavi\\Documents\\RayMusic\\app\\src\\main\\java\\com\\mrtdk\\liquid_glass\\ui\\screens\\PlayerScreen.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

new_lines = []
skip = False
for i, line in enumerate(lines):
    if line.strip() == '// Contenedor principal anclado (sin mover) pero desvaneciéndose':
        skip = True
        new_lines.append(line)
        new_lines.append('''        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(dominantColor.copy(alpha = bgAlpha))
                .pointerInput(showLyrics, showQueue) {
                    if (!showLyrics && !showQueue) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (offsetY > 150f) {
                                    onClose()
                                } else {
                                    offsetY = 0f
                                }
                            }
                        ) { change, dragAmount ->
                            if (dragAmount > 0f || offsetY > 0f) {
                                offsetY = (offsetY + dragAmount * 0.7f).coerceAtLeast(0f)
                            }
                        }
                    }
                }
        ) {
            val maxWidth = maxWidth
            val maxHeight = maxHeight

            val lyricsImageSize = 84.dp
            val playerImageSize = maxWidth * 0.85f
            
            val isOverlayActive = showLyrics || showQueue
            
            val imageCornerTarget = if (isOverlayActive) 8.dp else cornerRadius
            val imageAlignOffsetXTarget = if (isOverlayActive) 24.dp else (maxWidth - playerImageSize) / 2
            val imageAlignOffsetYTarget = if (isOverlayActive) 32.dp else offsetY.dp + (maxHeight * 0.05f)

            val imageSizeAnim by androidx.compose.animation.core.animateDpAsState(if (isOverlayActive) lyricsImageSize else playerImageSize)
            val imgOffsetX by androidx.compose.animation.core.animateDpAsState(imageAlignOffsetXTarget)
            val imgOffsetY by androidx.compose.animation.core.animateDpAsState(imageAlignOffsetYTarget)
            val imgCorner by androidx.compose.animation.core.animateDpAsState(imageCornerTarget)

            // THE MAIN IMAGE
            Box(
                modifier = Modifier
                    .offset(x = imgOffsetX, y = imgOffsetY)
                    .size(imageSizeAnim)
                    .graphicsLayer {
                        shape = RoundedCornerShape(imgCorner.toPx())
                        clip = true
                    }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(hdArtUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (!isOverlayActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.75f to Color.Transparent,
                                    1f to dominantColor.copy(alpha = contentAlpha)
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 8.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(modifier = Modifier.width(40.dp).height(5.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.5f)))
                    }
                }
            }

            // LYRICS / QUEUE OVERLAY
            androidx.compose.animation.AnimatedVisibility(
                visible = showLyrics || showQueue,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut()
            ) {
                 Column(modifier = Modifier.fillMaxSize()) {
                      Row(
                          modifier = Modifier.fillMaxWidth().padding(top = 32.dp, start = 120.dp, end = 24.dp), 
                          verticalAlignment = Alignment.CenterVertically
                      ) {
                           Column(modifier = Modifier.weight(1f)) {
                               Text(playerState.title, color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                               Text(playerState.artist, color = contentColor.copy(alpha=0.7f), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                           }
                           Icon(Icons.Default.Star, "Fav", tint = contentColor, modifier = Modifier.size(24.dp))
                           Spacer(modifier = Modifier.width(20.dp))
                           Icon(Icons.Default.MoreVert, "More", tint = contentColor, modifier = Modifier.size(24.dp))
                      }
                      
                      if (showQueue) {
                          Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 120.dp, end = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                               Box(modifier = Modifier.width(88.dp).height(40.dp).clip(RoundedCornerShape(50)).background(contentColor.copy(alpha=0.15f)).clickable{}, contentAlignment=Alignment.Center) {
                                   Icon(painterResource(id = R.drawable.shuffle), "Shuffle", tint=contentColor, modifier=Modifier.size(36.dp))
                               }
                               Box(modifier = Modifier.width(88.dp).height(40.dp).clip(RoundedCornerShape(50)).background(contentColor.copy(alpha=0.15f)).clickable{}, contentAlignment=Alignment.Center) {
                                   Icon(painterResource(id = R.drawable.repeat), "Repeat", tint=contentColor, modifier=Modifier.size(36.dp))
                               }
                          }
                          Text("Continue Playing", color=contentColor, fontSize=18.sp, fontWeight=FontWeight.Bold, modifier = Modifier.padding(top=32.dp, start=24.dp, end=24.dp, bottom=16.dp))
                          
                          LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                              items(10) { i ->
                                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                      Text("${i+1}", color = contentColor.copy(alpha=0.6f), modifier = Modifier.width(30.dp))
                                      Column(modifier = Modifier.weight(1f)) {
                                          Text("Queue Track ${i}", color = contentColor, fontSize = 16.sp, maxLines = 1, fontWeight = FontWeight.SemiBold)
                                          Text(playerState.artist, color = contentColor.copy(alpha=0.6f), fontSize = 14.sp, maxLines = 1)
                                      }
                                      Icon(Icons.Default.Menu, contentDescription=null, tint=contentColor.copy(alpha=0.6f))
                                  }
                              }
                          }
                      } else if (showLyrics) {
                           Box(modifier = Modifier.weight(1f)) {
                                if (lyricsLines != null && lyricsLines!!.isNotEmpty()) {
                                    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                                    
                                    LaunchedEffect(currentPosition) {
                                        val currentIdx = lyricsLines!!.indexOfLast { it.timeMs != -1L && it.timeMs <= currentPosition + 500 }
                                        if (currentIdx >= 0 && !listState.isScrollInProgress) {
                                            listState.animateScrollToItem(currentIdx.coerceAtLeast(0), scrollOffset = -200)
                                        }
                                    }
                                    
                                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                        item { Spacer(modifier = Modifier.height(20.dp)) }
                                        items(lyricsLines!!.size) { i ->
                                            val line = lyricsLines!![i]
                                            val isCurrent = line.timeMs != -1L && currentPosition >= line.timeMs && 
                                                (i == lyricsLines!!.lastIndex || currentPosition < lyricsLines!![i+1].timeMs)
                                            val isPast = line.timeMs != -1L && currentPosition > line.timeMs
                                            
                                            val targetAlpha = if (line.timeMs == -1L || isCurrent) 1f else if (isPast) 0.5f else 0.3f
                                            val scale = if (isCurrent) 1.05f else 1f
                                            val animAlpha by androidx.compose.animation.core.animateFloatAsState(targetAlpha, label="")
                                            val animScale by androidx.compose.animation.core.animateFloatAsState(scale, label="")
                                            
                                            Text(
                                                text = line.text,
                                                color = contentColor.copy(alpha = animAlpha),
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Bold,
                                                lineHeight = 36.sp,
                                                modifier = Modifier.fillMaxWidth()
                                                    .graphicsLayer { scaleX = animScale; scaleY = animScale; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f) }
                                                    .clickable { if(line.timeMs!=-1L) onSeek(line.timeMs) }
                                            )
                                        }
                                        item { Spacer(modifier = Modifier.height(200.dp)) }
                                    }
                                } else {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = contentColor)
                                    }
                                }
                           }
                      }
                      
                      Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, dominantColor.copy(alpha=0.9f), dominantColor)))) {
                          PlayerBottomControls(
                              progress = progress, currentPosition = currentPosition, duration = duration,
                              isPlaying = isPlaying, contentColor = contentColor, volumePosition = volumePosition,
                              showLyrics = showLyrics, showQueue = showQueue,
                              onSeek = onSeek, onTogglePlayPause = onTogglePlayPause, onVolumeChange = { v -> volumePosition = v; onVolumeChange(v) },
                              onToggleLyrics = { showLyrics = !showLyrics; showQueue = false }, onToggleQueue = { showQueue = !showQueue; showLyrics = false },
                              includeVolumeAndIcons = true,
                              includeProgress = false
                          )
                      }
                 }
            }

            // DETAILS AND CONTROLS (WHEN NO QUEUE AND NO LYRICS)
            androidx.compose.animation.AnimatedVisibility(
                 visible = !isOverlayActive,
                 modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                 enter = androidx.compose.animation.fadeIn(),
                 exit = androidx.compose.animation.fadeOut()
            ) {
                 Column(
                      modifier = Modifier
                          .fillMaxWidth()
                          .padding(horizontal = 24.dp)
                          .graphicsLayer { alpha = contentAlpha }
                 ) {
                      Row(
                          modifier = Modifier.fillMaxWidth(),
                          verticalAlignment = Alignment.CenterVertically
                      ) {
                          Column(modifier = Modifier.weight(1f)) {
                              Text(
                                  text = playerState.title,
                                  color = contentColor,
                                  fontSize = 24.sp,
                                  fontWeight = FontWeight.Bold,
                                  maxLines = 1,
                                  overflow = TextOverflow.Ellipsis
                              )
                              Spacer(modifier = Modifier.height(2.dp))
                              Text(
                                  text = playerState.artist,
                                  color = contentColor.copy(alpha = 0.7f),
                                  fontSize = 18.sp,
                                  maxLines = 1,
                                  overflow = TextOverflow.Ellipsis
                              )
                          }
                          Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                              Box(
                                  modifier = Modifier.size(32.dp).clip(CircleShape).background(contentColor.copy(alpha = 0.15f)).clickable { },
                                  contentAlignment = Alignment.Center
                              ) {
                                  Icon(Icons.Default.Star, contentDescription = "Fav", tint = contentColor, modifier = Modifier.size(18.dp))
                              }
                              Box(
                                  modifier = Modifier.size(32.dp).clip(CircleShape).background(contentColor.copy(alpha = 0.15f)).clickable { },
                                  contentAlignment = Alignment.Center
                              ) {
                                  androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
                                      val r = 1.5.dp.toPx()
                                      val space = 4.dp.toPx()
                                      val cx = size.width / 2f
                                      val cy = size.height / 2f
                                      drawCircle(contentColor, radius = r, center = Offset(cx - space - r * 2, cy))
                                      drawCircle(contentColor, radius = r, center = Offset(cx, cy))
                                      drawCircle(contentColor, radius = r, center = Offset(cx + space + r * 2, cy))
                                  }
                              }
                          }
                      }
                      
                      Spacer(modifier = Modifier.height(24.dp))
                      
                      Slider(
                          value = progress, onValueChange = { onSeek((it * duration).toLong()) },
                          modifier = Modifier.fillMaxWidth().height(16.dp),
                          colors = SliderDefaults.colors(
                              thumbColor = Color.Transparent,
                              activeTrackColor = contentColor.copy(alpha = 0.9f),
                              inactiveTrackColor = contentColor.copy(alpha = 0.3f)
                          )
                      )
                      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                          Text(formatDuration(currentPosition), color = contentColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                          Text("-${formatDuration(duration - currentPosition)}", color = contentColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                      }
                      
                      Spacer(modifier = Modifier.height(32.dp))
                      
                      Box(
                          modifier = Modifier
                              .fillMaxWidth()
                              .padding(bottom = 32.dp)
                      ) {
                          PlayerBottomControls( // We only want the icons, volume and stuff here, NO PROGRESS
                              progress = progress, currentPosition = currentPosition, duration = duration,
                              isPlaying = isPlaying, contentColor = contentColor, volumePosition = volumePosition,
                              showLyrics = showLyrics, showQueue = showQueue,
                              onSeek = onSeek, onTogglePlayPause = onTogglePlayPause, onVolumeChange = { v -> volumePosition = v; onVolumeChange(v) },
                              onToggleLyrics = { showLyrics = !showLyrics; showQueue = false }, onToggleQueue = { showQueue = !showQueue; showLyrics = false },
                              includeVolumeAndIcons = true,
                              includeProgress = false
                          )
                      }
                 }
            }
        }
    }
}
''')
        continue

    if skip:
        if line.strip() == '@Composable':
            skip = False
            new_lines.append(line)
        continue
    
    if not skip:
        new_lines.append(line)

with open('c:\\Users\\Xavi\\Documents\\RayMusic\\app\\src\\main\\java\\com\\mrtdk\\liquid_glass\\ui\\screens\\PlayerScreen.kt', 'w', encoding='utf-8') as f:
    f.writelines(new_lines)
