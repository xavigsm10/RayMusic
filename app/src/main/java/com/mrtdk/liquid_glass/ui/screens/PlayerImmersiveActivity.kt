package com.mrtdk.liquid_glass.ui.screens

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.mrtdk.liquid_glass.R
import com.mrtdk.liquid_glass.databinding.ActivityPlayerImmersiveBinding
import com.mrtdk.liquid_glass.utils.BlurTransformation

/**
 * An immersive Music Player Activity implementing a glassmorphism background effect.
 *
 * Implements a strict 4-layer layout structure:
 * Capa 0: Background ImageView (imgBackground) with a 100f blur.
 * Capa 1: Semi-transparent solid dark overlay (bgOverlay) to guarantee readability.
 * Capa 2: Sharp ImageView (imgCover) in the top half.
 * Capa 3: Transparent controls container (controlsContainer) in the bottom half.
 *
 * All Palette API integrations, solid background colors, and color gradients have been removed.
 */
class PlayerImmersiveActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerImmersiveBinding
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize ViewBinding
        binding = ActivityPlayerImmersiveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPlaybackControls()
        
        // Load initial mock song metadata & artwork
        updateSongMetadata(
            title = "After Hours",
            artist = "The Weeknd",
            cover = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=800&auto=format&fit=crop&q=60"
        )
    }

    /**
     * Updates the player UI with song metadata and loads the artwork on both views.
     */
    fun updateSongMetadata(title: String, artist: String, cover: Any?) {
        binding.txtSongTitle.text = title
        binding.txtSongArtist.text = artist
        
        setCoverImage(cover)
    }

    /**
     * Sets the cover art on both the main sharp ImageView and the blurred background ImageView.
     * Applies RenderEffect.createBlurEffect with Shader.TileMode.MIRROR to the background on API 31+.
     *
     * @param cover The cover image source (e.g., URL String, Bitmap, Uri, or Resource ID)
     */
    private fun setCoverImage(cover: Any?) {
        if (cover == null) {
            binding.imgCover.setImageResource(R.drawable.albumspeakerlarge)
            binding.imgBackground.setImageResource(R.drawable.albumspeakerlarge)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                binding.imgBackground.setRenderEffect(null)
            }
            return
        }

        // 1. Load sharp artwork into the main cover ImageView
        binding.imgCover.load(cover) {
            crossfade(true)
            placeholder(R.drawable.albumspeakerlarge)
            error(R.drawable.albumspeakerlarge)
        }

        // 2. Load blurred artwork into the background ImageView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+): Apply 100f RenderEffect with MIRROR TileMode (hardware-accelerated)
            binding.imgBackground.load(cover) {
                crossfade(true)
                placeholder(R.drawable.albumspeakerlarge)
                error(R.drawable.albumspeakerlarge)
                listener(
                    onSuccess = { _, _ ->
                        // Intense blur effect using Shader.TileMode.MIRROR
                        val blurEffect = RenderEffect.createBlurEffect(
                            100f, 
                            100f, 
                            Shader.TileMode.MIRROR
                        )
                        binding.imgBackground.setRenderEffect(blurEffect)
                    },
                    onError = { _, _ ->
                        binding.imgBackground.setRenderEffect(null)
                    }
                )
            }
        } else {
            // Android < 12 (API < 31): Apply custom BlurTransformation via Coil (RenderScript + downsampling)
            binding.imgBackground.load(cover) {
                crossfade(true)
                placeholder(R.drawable.albumspeakerlarge)
                error(R.drawable.albumspeakerlarge)
                transformations(
                    BlurTransformation(
                        context = this@PlayerImmersiveActivity,
                        radius = 25f,
                        sampling = 4f
                    )
                )
            }
        }
    }

    /**
     * Setup mock listeners for playback controls.
     */
    private fun setupPlaybackControls() {
        binding.btnPlayPause.setOnClickListener {
            isPlaying = !isPlaying
            if (isPlaying) {
                binding.btnPlayPause.setImageResource(R.drawable.pause)
                Toast.makeText(this, "Play clicked", Toast.LENGTH_SHORT).show()
            } else {
                binding.btnPlayPause.setImageResource(R.drawable.resume)
                Toast.makeText(this, "Pause clicked", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnPrevious.setOnClickListener {
            Toast.makeText(this, "Previous song clicked", Toast.LENGTH_SHORT).show()
        }

        binding.btnNext.setOnClickListener {
            Toast.makeText(this, "Next song clicked", Toast.LENGTH_SHORT).show()
        }

        binding.seekBarProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val minutes = progress / 60
                    val seconds = progress % 60
                    binding.txtCurrentTime.text = String.format("%d:%02d", minutes, seconds)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    Toast.makeText(this@PlayerImmersiveActivity, "Seeked to ${it.progress}s", Toast.LENGTH_SHORT).show()
                }
            }
        })

        // Initial progress states
        binding.seekBarProgress.max = 225 // 3:45
        binding.seekBarProgress.progress = 84 // 1:24
        binding.txtCurrentTime.text = "1:24"
        binding.txtTotalTime.text = "3:45"
    }
}
