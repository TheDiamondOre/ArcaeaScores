package io.github.rosemoe.arcaeaScores.arc

import org.json.JSONObject
import java.io.InputStream
import java.util.Locale

class ArcaeaAvatar(avatarFile: InputStream) {

    private val mappingName = mutableMapOf<Int, String>()
    private val mappingFile = mutableMapOf<Int, String>()
    private var avatarCount = 0
    init {
        val language = Locale.getDefault().language
        val json = JSONObject(avatarFile.reader().readText())
        val avatar = json.getJSONArray("avatars")
        avatarCount = avatar.length()
        for (i in 0 until avatar.length()) {
            val avatarID = avatar.getJSONObject(i)
            mappingName[i] = avatarID.getJSONObject("name_localized").getString(language)
            mappingFile[i]= avatarID.getString("file")
        }
    }
    fun queryForAvatarLength() : Int {
        return avatarCount
    }
    fun queryForAvatarFile(id: Int) : String {
        return mappingFile[id] ?: "unknown_icon"
    }

    fun queryForAvatarName(id: Int) : String {
        return mappingName[id] ?: "Unknown"
    }
}