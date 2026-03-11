package com.example.amberplayer

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
// --- GESTURES ---
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

// --- HAPTICS (Vibrations) ---
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

// --- ANIMATIONS & TIMERS ---
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import kotlinx.coroutines.delay

// --- ICONS ---
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind

// --- STATE DELEGATION (If you have red errors on the word "by") ---
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.FeaturedPlayList
import android.media.AudioManager
import android.content.Context
import androidx.palette.graphics.Palette
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.compose.ui.graphics.StrokeCap
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.Manifest
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.compose.AsyncImage
import com.example.amberplayer.ui.theme.AmberPlayerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random
import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.graphics.Brush

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen() // THIS TRIGGERS THE ANIMATION!
        super.onCreate(savedInstanceState)
        setContent {
            AmberPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionScreen()
                }
            }
        }
    }
}

@Composable
fun rememberMediaController(context: android.content.Context): State<MediaController?> {
    val mediaController = remember { mutableStateOf<MediaController?>(null) }
    DisposableEffect(context) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            { mediaController.value = controllerFuture.get() },
            ContextCompat.getMainExecutor(context)
        )
        onDispose {
            MediaController.releaseFuture(controllerFuture)
            mediaController.value?.release()
        }
    }
    return mediaController
}

@Composable
fun PermissionScreen() {
    var hasPermission by remember { mutableStateOf(false) }
    var audioFolders by remember { mutableStateOf<Map<String, List<Song>>>(emptyMap()) }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var songList by remember { mutableStateOf<List<Song>>(emptyList()) }
    var currentSong by remember { mutableStateOf<Song?>(null) }
    var isPlaylistOpen by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var player by remember { mutableStateOf<Player?>(null) }

    // This connects your UI to the background PlaybackService
    LaunchedEffect(Unit) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener(
            { player = controllerFuture.get() },
            ContextCompat.getMainExecutor(context)
        )
    }

    val settingsManager = remember { SettingsManager(context) }

    // Read the saved data from the disk. We give it default values just in case it's the first time the app is opened.
    val savedSongUri by settingsManager.songUriFlow.collectAsState(initial = null)
    val savedVolume by settingsManager.volumeFlow.collectAsState(initial = -1f)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasPermission = isGranted }
    )

    BackHandler(enabled = selectedFolder != null) {
        if (isPlaylistOpen) {
            isPlaylistOpen = false
        } else {
            selectedFolder = null
            player?.stop()
        }
    }

    LaunchedEffect(hasPermission, player) {
        if (hasPermission && player != null) {
            val folders = MusicRepository.getAudioFolders(context)
            if (folders.isNotEmpty()) { audioFolders = folders }
        }
    }

    // --- STARTUP LOGIC: Restore Volume and Last Played Song ---
    LaunchedEffect(savedSongUri, savedVolume, audioFolders) {
        // 1. Restore the Volume (if we have a saved value)
        if (savedVolume != -1f) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, savedVolume.toInt(), 0)
        }

        // 2. Restore the Last Played Song
        // We only do this if the currentSong is null (meaning the app just booted up)
        if (savedSongUri != null && currentSong == null && audioFolders.isNotEmpty()) {

            // Flatten your folders to search through all songs
            val allSongs = audioFolders.values.flatten()
            val lastPlayedSong = allSongs.find { it.uri.toString() == savedSongUri }

            if (lastPlayedSong != null) {
                currentSong = lastPlayedSong // Update the UI

                // Load it into Media3 (ExoPlayer) but DON'T auto-play
                player?.setMediaItem(androidx.media3.common.MediaItem.fromUri(lastPlayedSong.uri))
                player?.prepare()
            }
        }
    }

    DisposableEffect(player, songList) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = player?.currentMediaItemIndex ?: -1
                if (index in songList.indices) { currentSong = songList[index] }
            }
        }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }

    val onPlayNext: (Song) -> Unit = { songToMove ->
        val currentIndex = player?.currentMediaItemIndex ?: -1
        val fromIndex = songList.indexOf(songToMove)
        if (currentIndex != -1 && fromIndex != -1 && currentIndex != fromIndex) {
            var insertIndex = currentIndex + 1
            if (fromIndex < insertIndex) insertIndex -= 1
            player?.moveMediaItem(fromIndex, insertIndex)
            val mutableList = songList.toMutableList()
            val removedSong = mutableList.removeAt(fromIndex)
            mutableList.add(insertIndex, removedSong)
            songList = mutableList
        }
    }

    val onMoveSong: (Song, Int) -> Unit = { songToMove, directionOffset ->
        val currentIndex = songList.indexOf(songToMove)
        val targetIndex = currentIndex + directionOffset
        if (currentIndex != -1 && targetIndex in songList.indices) {
            player?.moveMediaItem(currentIndex, targetIndex)
            val mutableList = songList.toMutableList()
            val removed = mutableList.removeAt(currentIndex)
            mutableList.add(targetIndex, removed)
            songList = mutableList
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (player == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (!hasPermission) {
            // --- NEW: Glass Permission Screen with Vibrant Gradient ---
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(Color(0xFF673AB7), Color(0xFFE91E63), Color(0xFFFF9800))))
                        .blur(100.dp)
                )
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))

                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Filled.List, contentDescription = "Music", tint = Color.White, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("AmberPlayer", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("We need storage access to find your local music.", color = Color.White.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
                            permissionLauncher.launch(permissionToRequest)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Grant Permission", color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }
            }
        } else if (audioFolders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)))
            Text("No music folders found!", color = Color.White, modifier = Modifier.align(Alignment.Center))
        } else {
            if (selectedFolder == null) {
                FolderScreen(audioFolders = audioFolders) { folderName ->
                    selectedFolder = folderName
                    val folderSongs = audioFolders[folderName] ?: emptyList()
                    songList = folderSongs
                    val mediaItems = folderSongs.map { MediaItem.fromUri(it.uri) }
                    player!!.setMediaItems(mediaItems)
                    player!!.prepare()
                    player!!.play()
                }
            } else {
                if (currentSong != null) {
                    FullScreenPlayer(
                        song = currentSong!!,
                        player = player!!,
                        onOpenPlaylist = { isPlaylistOpen = true },
                        onOpenFolders = {
                            selectedFolder = null // Go back to folder view
                            player?.stop()
                        }
                    )
                }

                AnimatedVisibility(visible = isPlaylistOpen, enter = fadeIn(), exit = fadeOut()) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable { isPlaylistOpen = false })
                }

                AnimatedVisibility(visible = isPlaylistOpen, enter = slideInHorizontally(initialOffsetX = { -it }), exit = slideOutHorizontally(targetOffsetX = { -it })) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.85f).clickable(enabled = false) {}) {
                        PlaylistScreen(
                            songs = songList, player = player!!, currentSong = currentSong,
                            onClose = { isPlaylistOpen = false }, onPlayNext = onPlayNext, onMoveSong = onMoveSong
                        )
                    }
                }
            }
        }
    }
}

// --- UPDATED: Folder Screen with Global Glass Background ---
@Composable
fun FolderScreen(audioFolders: Map<String, List<Song>>, onFolderClick: (String) -> Unit) {
    // Grab the album art of the very first song in the entire collection to act as the global background
    val globalBackgroundUri = remember(audioFolders) {
        audioFolders.values.firstOrNull()?.firstOrNull()?.albumArtUri
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. The Glass Background
        if (globalBackgroundUri != null) {
            AsyncImage(
                model = globalBackgroundUri,
                contentDescription = "Blurred Background",
                modifier = Modifier.fillMaxSize().blur(100.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray))
        }

        // 2. The Dark Tint
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)))

        // 3. The Content (Forced to White for readability)
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "Your Music",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                modifier = Modifier.padding(24.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(audioFolders.keys.toList()) { folderName ->
                    val trackCount = audioFolders[folderName]?.size ?: 0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFolderClick(folderName) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Glass Folder Icon
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.List, contentDescription = "Folder", tint = Color.White, modifier = Modifier.size(32.dp))
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = folderName,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$trackCount tracks",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                }
            }
        }
    }
}
@Composable
fun PlaylistScreen(songs: List<Song>, player: Player, currentSong: Song?, onClose: () -> Unit, onPlayNext: (Song) -> Unit, onMoveSong: (Song, Int) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val isSearching = searchQuery.isNotBlank()

    val filteredSongs = remember(searchQuery, songs) {
        if (isSearching) songs.filter { it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true) } else songs
    }

    Box(modifier = Modifier.fillMaxSize()) {
        currentSong?.let { song ->
            AsyncImage(model = song.albumArtUri, contentDescription = "Blurred Menu Background", modifier = Modifier.fillMaxSize().blur(radius = 80.dp), contentScale = ContentScale.Crop)
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)))

        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
                Spacer(modifier = Modifier.width(16.dp))
                Text("Playlist", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search songs...", color = Color.White.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.White.copy(alpha = 0.7f)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = Color.White.copy(alpha = 0.7f)) }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.1f), unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredSongs, key = { it.uri.toString() }) { song ->
                    SongItem(
                        song = song,
                        songs = songs,
                        player = player,
                        isSearching = isSearching, // Disable drag if searching
                        onClick = onClose,
                        onPlayNext = onPlayNext,
                        onMoveSong = onMoveSong
                    )
                }
            }
        }
    }
}

// --- UPDATED: SongItem with Vertical Drag & Drop Physics ---
@Composable
fun SongItem(song: Song, songs: List<Song>, player: Player, isSearching: Boolean, onClick: () -> Unit, onPlayNext: (Song) -> Unit, onMoveSong: (Song, Int) -> Unit) {
    var horizontalOffset by remember { mutableFloatStateOf(0f) }
    var verticalOffset by remember { mutableFloatStateOf(0f) }

    val animatedOffsetX by animateFloatAsState(targetValue = horizontalOffset, label = "swipe")

    // Calculate how far to drag vertically before snapping (approx height of the row)
    val density = LocalDensity.current
    val itemHeightPx = remember(density) { with(density) { 80.dp.toPx() } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (horizontalOffset > 200f) onPlayNext(song)
                        horizontalOffset = 0f
                    }
                ) { change, dragAmount ->
                    change.consume()
                    if (horizontalOffset + dragAmount > 0) horizontalOffset += dragAmount
                }
            }
    ) {
        val swipeAlpha = (animatedOffsetX / 150f).coerceIn(0f, 1f)

        Box(
            modifier = Modifier.fillMaxSize().padding(start = 24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(swipeAlpha)) {
                Icon(Icons.Filled.Add, contentDescription = "Play Next", tint = Color.White, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play Next", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }

        // Apply both X (horizontal swipe) and Y (vertical drag) offsets
        Row(
            modifier = Modifier
                .offset { IntOffset(animatedOffsetX.roundToInt(), verticalOffset.roundToInt()) }
                .fillMaxWidth()
                .background(Color.Transparent)
                .clickable {
                    val index = songs.indexOf(song)
                    if (index != -1) { player.seekTo(index, 0L); player.play() }
                    onClick()
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.albumArtUri, contentDescription = "Album Cover",
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = song.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = song.artist, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            // --- THE NEW VERTICAL DRAG HANDLE ---
            // Only show it if we are NOT searching (reordering filtered lists breaks things!)
            if (!isSearching) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Drag to reorder",
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(32.dp)
                        .padding(start = 8.dp)
                        // Physics engine for moving up and down
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = { verticalOffset = 0f },
                                onDragCancel = { verticalOffset = 0f }
                            ) { change, dragAmount ->
                                change.consume()
                                verticalOffset += dragAmount

                                // If dragged down past the height of the row, swap down!
                                if (verticalOffset > itemHeightPx) {
                                    onMoveSong(song, 1)
                                    verticalOffset -= itemHeightPx // Reset visually so it doesn't fly off screen
                                }
                                // If dragged up past the height of the row, swap up!
                                else if (verticalOffset < -itemHeightPx) {
                                    onMoveSong(song, -1)
                                    verticalOffset += itemHeightPx
                                }
                            }
                        }
                )
            }
        }
    }
}
@SuppressLint("PrivateResource")
@Composable
fun FullScreenPlayer(
    song: Song,
    player: Player,
    onOpenPlaylist: () -> Unit,
    onOpenFolders: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // --- NEW: DataStore Setup ---
    val settingsManager = remember { SettingsManager(context) }
    val scope = rememberCoroutineScope()

    // --- STATE ---
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var currentPosition by remember { mutableStateOf(player.currentPosition) }
    var volumeLevel by remember {
        mutableStateOf(
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
        )
    }
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()

    // Dynamic Colors from Palette API
    var dominantColor by remember { mutableStateOf(Color(0xFF3E2723)) }
    var mutedColor by remember { mutableStateOf(Color(0xFF1D1B19)) }
    var transitionDirection by remember { mutableIntStateOf(1) } // 1 for Next, -1 for Prev
    // --- EFFECTS ---
    // 1. Extract Colors from Album Art
    LaunchedEffect(song.albumArtUri) {
        val request = ImageRequest.Builder(context)
            .data(song.albumArtUri)
            .allowHardware(false)
            .build()
        val result = context.imageLoader.execute(request)
        if (result is SuccessResult) {
            val bitmap = result.drawable.toBitmap()
            val palette = Palette.from(bitmap).generate()
            dominantColor = palette.dominantSwatch?.rgb?.let { Color(it) } ?: Color(0xFF5D4037)
            mutedColor = palette.darkMutedSwatch?.rgb?.let { Color(it) } ?: Color(0xFF1A1A1A)
        }
    }
    // Save the current song URI to DataStore whenever it changes
    LaunchedEffect(song.uri) {
        settingsManager.saveSongUri(song.uri.toString())
    }

    // 2. Update Playback Time
    LaunchedEffect(player) {
        while (true) {
            currentPosition = player.currentPosition
            isPlaying = player.isPlaying
            kotlinx.coroutines.delay(100)
        }
    }

    // --- UI LAYOUT ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        dominantColor.copy(alpha = 0.8f),
                        mutedColor
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 1. ALBUM ART & GESTURES (Now cleanly separated!)
            // 1. ALBUM ART WITH SLIDE ANIMATION
            AnimatedContent(
                targetState = song,
                transitionSpec = {
                    if (transitionDirection == 1) {
                        // Sliding to Next Song (Slide in from right, exit to left)
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut())
                    } else {
                        // Sliding to Previous Song (Slide in from left, exit to right)
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut())
                    }
                },
                label = "Album Art Transition"
            ) { targetSong ->
                AlbumArtWithGestures(
                    song = targetSong,
                    player = player,
                    onSwipeNext = {
                        transitionDirection = 1
                        player.seekToNextMediaItem()
                    },
                    onSwipePrev = {
                        transitionDirection = -1
                        player.seekToPreviousMediaItem()
                    }
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // 2. WAVEFORM SEEKER
            AmberolWaveform(
                song = song,
                progress = if (song.duration > 0) currentPosition.toFloat() / song.duration.toFloat() else 0f,
                onSeek = { percentage -> player.seekTo((percentage * song.duration).toLong()) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formatTime(currentPosition),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
                val remaining = song.duration - currentPosition
                Text(
                    "-${formatTime(remaining)}",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. TRACK INFO
            Text(
                text = song.title,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.weight(1f))

            // 4. MAIN PLAYBACK CONTROLS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly, // This spaces them out side-by-side!
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Button
                IconButton(
                    onClick = {
                        transitionDirection = -1
                        player.seekToPreviousMediaItem()
                    },
                    modifier = Modifier.size(56.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        painterResource(androidx.media3.ui.R.drawable.exo_icon_previous),
                        contentDescription = "Previous",
                        tint = Color.White
                    )
                }

                // Play/Pause Button
                IconButton(
                    onClick = { if (isPlaying) player.pause() else player.play() },
                    modifier = Modifier.size(72.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    val iconRes =
                        if (isPlaying) androidx.media3.ui.R.drawable.exo_icon_pause else androidx.media3.ui.R.drawable.exo_icon_play
                    Icon(
                        painterResource(iconRes),
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Next Button
                IconButton(
                    onClick = {
                        transitionDirection = 1
                        player.seekToNextMediaItem()
                    },
                    modifier = Modifier.size(56.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        painterResource(androidx.media3.ui.R.drawable.exo_icon_next),
                        contentDescription = "Next",
                        tint = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            // 5. VOLUME SLIDER
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Icon(
                    Icons.Filled.VolumeMute,
                    contentDescription = "Volume Down",
                    tint = Color.White.copy(alpha = 0.7f)
                )
                Slider(
                    value = volumeLevel,
                    onValueChange = {
                        volumeLevel = it
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, it.toInt(), 0)
                    },
                    // NEW: Save the volume to DataStore when the user lets go
                    onValueChangeFinished = {
                        scope.launch {
                            settingsManager.saveVolume(volumeLevel)
                        }
                    },
                    valueRange = 0f..maxVolume,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White.copy(alpha = 0.5f),
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )
                Icon(
                    Icons.Filled.VolumeUp,
                    contentDescription = "Volume Up",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 6. FOOTER NAVIGATION
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenPlaylist,
                    modifier = Modifier.size(48.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        Icons.Filled.FeaturedPlayList,
                        contentDescription = "Playlist",
                        tint = Color.White
                    )
                }

                Row {
                    IconButton(
                        onClick = { player.shuffleModeEnabled = !player.shuffleModeEnabled },
                        modifier = Modifier.size(48.dp).background(
                            if (player.shuffleModeEnabled) Color.White.copy(alpha = 0.3f) else Color.White.copy(
                                alpha = 0.1f
                            ), CircleShape
                        )
                    ) {
                        Icon(
                            painterResource(androidx.media3.ui.R.drawable.exo_icon_shuffle_on),
                            contentDescription = "Shuffle",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(
                        onClick = {
                            player.repeatMode =
                                if (player.repeatMode == Player.REPEAT_MODE_OFF) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
                        },
                        modifier = Modifier.size(48.dp).background(
                            if (player.repeatMode != Player.REPEAT_MODE_OFF) Color.White.copy(alpha = 0.3f) else Color.White.copy(
                                alpha = 0.1f
                            ), CircleShape
                        )
                    ) {
                        Icon(
                            painterResource(androidx.media3.ui.R.drawable.exo_icon_repeat_all),
                            contentDescription = "Repeat",
                            tint = Color.White
                        )
                    }
                }

                IconButton(
                    onClick = onOpenFolders,
                    modifier = Modifier.size(48.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Filled.Menu, contentDescription = "Folders", tint = Color.White)
                }
            }
        }
    }
}
// --- NEW COMPONENT: Deterministic Waveform Generator ---
@Composable
fun AmberolWaveform(song: Song, progress: Float, onSeek: (Float) -> Unit) {
    // Generate a consistent waveform based on the song's title and ID.
    // This creates a mathematically accurate *looking* waveform that is unique to each song,
    // avoiding the heavy CPU cost of decoding actual MP3 bytes on the main thread.
    val amplitudes = remember(song.id) {
        val random = Random(song.id + song.title.hashCode())
        List(60) { random.nextFloat() * 0.8f + 0.2f } // 60 bars, heights between 20% and 100%
    }

    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(40.dp)
        .pointerInput(Unit) {
            detectTapGestures { offset ->
                onSeek(offset.x / size.width)
            }
        }
    ) {
        val barWidth = size.width / amplitudes.size
        val gap = 4.dp.toPx()
        val effectiveBarWidth = barWidth - gap

        amplitudes.forEachIndexed { index, amp ->
            val x = index * barWidth + (barWidth / 2)
            val barHeight = size.height * amp
            val startY = (size.height - barHeight) / 2
            val endY = startY + barHeight

            // If the bar is behind the current playback progress, paint it bright white. Otherwise, dim it.
            val isPlayed = (index.toFloat() / amplitudes.size) <= progress
            val color = if (isPlayed) Color.White else Color.White.copy(alpha = 0.3f)

            drawLine(
                color = color,
                start = Offset(x, startY),
                end = Offset(x, endY),
                strokeWidth = effectiveBarWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

// Helper function to format milliseconds to M:SS

@Composable
fun WaveformSlider(progress: Float, onProgressChange: (Float) -> Unit, waveformColor: Color, modifier: Modifier = Modifier) {
    val amplitudes = remember { List(60) { Random.nextFloat().coerceIn(0.2f, 1.0f) } }
    Canvas(
        modifier = modifier.fillMaxWidth().height(48.dp)
            .pointerInput(Unit) { detectTapGestures { offset -> onProgressChange((offset.x / size.width).coerceIn(0f, 1f)) } }
            .pointerInput(Unit) { detectHorizontalDragGestures { change, _ -> change.consume(); onProgressChange((change.position.x / size.width).coerceIn(0f, 1f)) } }
    ) {
        val barWidth = size.width / amplitudes.size
        val gap = barWidth * 0.2f
        val actualBarWidth = barWidth - gap
        amplitudes.forEachIndexed { index, amplitude ->
            val x = index * barWidth
            val isPlayed = (x / size.width) <= progress
            val heightMultiplier = if (index < 3 || index > amplitudes.size - 4) 0.5f else 1f
            val barHeight = size.height * amplitude * heightMultiplier
            val y = (size.height - barHeight) / 2f
            drawRoundRect(color = if (isPlayed) waveformColor else waveformColor.copy(alpha = 0.3f), topLeft = Offset(x = x, y = y), size = Size(width = actualBarWidth, height = barHeight), cornerRadius = CornerRadius(actualBarWidth / 2, actualBarWidth / 2))
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    // Uses padStart to always ensure two digits for seconds (e.g., 0:05)
    return "${minutes}:${seconds.toString().padStart(2, '0')}"
}

@Composable
fun AlbumArtWithGestures(
    song: Song,
    player: Player,
    onSwipeNext: () -> Unit, // NEW: Callback for next
    onSwipePrev: () -> Unit  // NEW: Callback for prev
) {
    val haptic = LocalHapticFeedback.current
    var showSkipForward by remember { mutableStateOf(false) }
    var showSkipBack by remember { mutableStateOf(false) }

    LaunchedEffect(showSkipForward) {
        if (showSkipForward) {
            kotlinx.coroutines.delay(800)
            showSkipForward = false
        }
    }

    LaunchedEffect(showSkipBack) {
        if (showSkipBack) {
            kotlinx.coroutines.delay(800)
            showSkipBack = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .aspectRatio(1f)
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val isRightSide = offset.x > size.width / 2
                        if (isRightSide) {
                            player.seekTo(player.currentPosition + 10000)
                            showSkipForward = true
                        } else {
                            player.seekTo(player.currentPosition - 10000)
                            showSkipBack = true
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                var offsetX = 0f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX > 150) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSwipePrev() // Call the new callback
                        } else if (offsetX < -150) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSwipeNext() // Call the new callback
                        }
                        offsetX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount -> offsetX += dragAmount }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = song.albumArtUri,
            contentDescription = "Album Art",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        AnimatedVisibility(
            visible = showSkipForward,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 32.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                Text("+10s", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        AnimatedVisibility(
            visible = showSkipBack,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 32.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.FastRewind, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                Text("-10s", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}