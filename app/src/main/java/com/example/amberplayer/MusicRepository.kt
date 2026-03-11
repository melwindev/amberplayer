package com.example.amberplayer

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.File


object MusicRepository {

    // This function returns a list of our Song objects
    fun getAudioFiles(context: Context): List<Song> {
        val songs = mutableListOf<Song>()

        // 1. The "Columns" we want from the database
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )

        // 2. Filter out non-music files (like system notification sounds)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        // 3. Sort alphabetically by title
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        // 4. Execute the query using the ContentResolver
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->

            // Map the database columns to variables
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            // Loop through the results (similar to iterating through an array)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown Title"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val duration = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)

                // Create the URI that points directly to the audio file
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                )

                // Create the URI that points to the embedded album art
                val artworkUri = Uri.parse("content://media/external/audio/albumart")
                val albumArtUri = ContentUris.withAppendedId(artworkUri, albumId)

                // Instantiate a new Song object and add it to our list
                songs.add(Song(id, title, artist, duration, contentUri, albumArtUri))
            }
        }
        return songs
    }
    // ADD THIS NEW FUNCTION:
    fun getAudioFolders(context: Context): Map<String, List<Song>> {
        val folderMap = mutableMapOf<String, MutableList<Song>>()

        val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA // <-- CRITICAL: We need the absolute file path to find the folder!
        )

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown Title"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val duration = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)

                // Get the file path and extract the parent folder's name
                val path = cursor.getString(dataColumn)
                val folderName = File(path).parentFile?.name ?: "Unknown Folder"

                val contentUri = android.content.ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                val albumArtUri = android.content.ContentUris.withAppendedId(android.net.Uri.parse("content://media/external/audio/albumart"), albumId)

                val song = Song(
                    id = id,
                    uri = contentUri,
                    title = title,
                    artist = artist,
                    duration = duration,
                    albumArtUri = albumArtUri
                )

                // Group the song into its respective folder
                if (!folderMap.containsKey(folderName)) {
                    folderMap[folderName] = mutableListOf()
                }
                folderMap[folderName]?.add(song)
            }
        }
        return folderMap
    }
}

