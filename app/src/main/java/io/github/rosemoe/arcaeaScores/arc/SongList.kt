package io.github.rosemoe.arcaeaScores.arc

import org.json.JSONObject
import java.io.InputStream

class ArcaeaTitles(songListJsonFile: InputStream) {

    private val mappingLocalizedTitle = mutableMapOf<String, String>()
    private val mappingNeedDownloading = mutableMapOf<String, Boolean>()
    init {
        val root = JSONObject(songListJsonFile.reader().readText())
        val songs = root.getJSONArray("songs")
        for (i in 0 until songs.length()) {
            val song = songs.getJSONObject(i)
            mappingLocalizedTitle[song.getString("id")] = song.getJSONObject("title_localized").getString("en")
            mappingNeedDownloading[song.getString("id")]= song.has("remote_dl")
        }
    }

    fun queryForLocalizedTitle(id: String) : String {
        return mappingLocalizedTitle[id] ?: id
    }

    fun queryForNeedDownloading(id: String) : Boolean {
        return mappingNeedDownloading[id] ?: false
    }
}