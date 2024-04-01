
package io.github.rosemoe.arcaeaScores.app

import android.app.ProgressDialog
import android.content.ContentValues
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.util.Linkify
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.rosemoe.arcaeaScores.R
import io.github.rosemoe.arcaeaScores.arc.ArcaeaAvatar
import io.github.rosemoe.arcaeaScores.arc.readDatabase
import io.github.rosemoe.arcaeaScores.util.showMsgDialog
import io.github.rosemoe.arcaeaScores.util.showToast
import io.github.rosemoe.arcaeaScores.util.toScaledString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private val copySave = registerForActivityResult(ActivityResultContracts.GetContent()){ uri ->
        if (uri != null) {
            val dbPath = Paths.get(applicationContext.filesDir.path + "/../databases/st3.db")
            val fileInputStream = contentResolver.openInputStream(uri)!!
            fileInputStream.use { input ->
                Files.deleteIfExists(dbPath)
                Files.copy(input, dbPath)
                updateScoreList()
                showToast(R.string.tip_update_finished)
                prefs.edit {
                    putLong("date", System.currentTimeMillis())
                }
            }
        }
    }
    companion object {
        private const val TAG = "SAVE_BITMAP"
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        findViewById<ListView>(R.id.score_list).dividerHeight = 0
        findViewById<FloatingActionButton>(R.id.button_refresh).let {
            it.setOnClickListener {
                onUpdateScoreClicked()
            }
        }
        val exoSemiBold = Typeface.createFromAsset(assets, "fonts/Exo-SemiBold.ttf")
        val exoTypeface = Typeface.createFromAsset(assets, "fonts/Exo-Regular.ttf")
        findViewById<ImageView>(R.id.player_avatar).let{
            it.setImageBitmap(prefs.getString("player_avatar", "char/unknown_icon.png")?.let { it1 -> readBitmapFromAssets(it1) })
            it.setOnClickListener{
                onAvatarClicked()
            }
        }
        findViewById<TextView>(R.id.player_name).let {
            it.setOnClickListener {
                onSetNameClicked()
            }
            it.text = prefs.getString("player_name", getString(R.string.click_to_set_name))
            it.typeface = exoSemiBold
        }
        findViewById<TextView>(R.id.max_potential).typeface = exoTypeface
        findViewById<TextView>(R.id.date).typeface = exoTypeface
        updateScoreList()
    }

    private fun readBitmapFromAssets(fileName: String): Bitmap?{
        val bitmap : Bitmap?
        val inputStream = assets.open(fileName)
        bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        return bitmap
    }

    private fun createViewBitmap(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }
    
    private fun saveBitmapImage(bitmap: Bitmap) {
        val timestamp = System.currentTimeMillis()

        //Tell the media scanner about the new file so that it is immediately available to the user.
        val values = ContentValues()
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        values.put(MediaStore.Images.Media.DATE_ADDED, timestamp)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.DATE_TAKEN, timestamp)
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + getString(R.string.app_name))
            values.put(MediaStore.Images.Media.IS_PENDING, true)
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                try {
                    val outputStream = contentResolver.openOutputStream(uri)
                    if (outputStream != null) {
                        try {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                            outputStream.close()
                        } catch (e: Exception) {
                            Log.e(TAG, "saveBitmapImage: ", e)
                        }
                    }
                    values.put(MediaStore.Images.Media.IS_PENDING, false)
                    contentResolver.update(uri, values, null, null)
                    Toast.makeText(this, "Saved...", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "saveBitmapImage: ", e)
                }
            }
        } else {
            val imageFileFolder = File(Environment.getExternalStorageDirectory().toString() + '/' + getString(R.string.app_name))
            if (!imageFileFolder.exists()) {
                imageFileFolder.mkdirs()
            }
            val mImageName = "$timestamp.png"
            val imageFile = File(imageFileFolder, mImageName)
            try {
                val outputStream: OutputStream = FileOutputStream(imageFile)
                try {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    outputStream.close()
                } catch (e: Exception) {
                    Log.e(TAG, "saveBitmapImage: ", e)
                }
                values.put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                Toast.makeText(this, "Saved...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "saveBitmapImage: ", e)
            }
        }
    }

    private fun onUpdateScoreClicked() {
        if (prefs.getBoolean("agree_using_root", false)) {
            refreshScores()
        } else {
            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_root)
                .setMessage(R.string.dialog_msg_root)
                .setPositiveButton(R.string.action_permit) { _, _ ->
                    prefs.edit {
                        putBoolean("agree_using_root", true)
                    }
                    refreshScores()
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    showToast(R.string.tip_reject_root)
                }.show()
        }
    }

    private fun onSetNameClicked() {
        val et = EditText(this)
        et.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        AlertDialog.Builder(this)
            .setTitle(R.string.set_name)
            .setView(et)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (et.text.isEmpty()) {
                    showToast(R.string.name_empty)
                } else {
                    prefs.edit {
                        putString("player_name", et.text.toString())
                    }
                    findViewById<TextView>(R.id.player_name).text = et.text
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun onAvatarClicked() {
        val avatars = ArcaeaAvatar(assets.open("avatars.json"))
        var avatarName = emptyArray<String>()
        for (i in 0 until avatars.queryForAvatarLength()){
            avatarName += avatars.queryForAvatarName(i)
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_avatar))
            .setItems(avatarName) {_, which ->
                findViewById<ImageView>(R.id.player_avatar).setImageBitmap(readBitmapFromAssets("char/${avatars.queryForAvatarFile(which)}"))
                prefs.edit{
                    putString("player_avatar", "char/${avatars.queryForAvatarFile(which)}")
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun onCopySaveClicked() {
        copySave.launch("*/*")
    }


    @Suppress("DEPRECATION")
    private fun refreshScores() {
        val pd = ProgressDialog.show(this, getString(R.string.pd_dialog_title), "...", true, false)

        suspend fun update(text: String) = withContext(Dispatchers.Main) {
            pd.setMessage(text)
        }

        lifecycleScope.launch {
            runCatching {
                // Copy `st3` database
                val process = Runtime.getRuntime().exec("su -mm")
                update(getString(R.string.state_obtaining_root))
                process.outputStream.writer().use {
                    it.write("mkdir /data/data/io.github.rosemoe.arcaeaScores/databases/\n" +
                            "cp -f /data/data/moe.low.arc/files/st3 /data/data/io.github.rosemoe.arcaeaScores/databases/st3.db\n" +
                            "chmod 777 /data/data/io.github.rosemoe.arcaeaScores/databases/\n" +
                            "chmod 666 /data/data/io.github.rosemoe.arcaeaScores/databases/st3.db\n" +
                            "exit\n")
                    it.flush()
                }
                update(getString(R.string.state_reading_save))
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    throw Exception("Non-zero exit code: $exitCode\nError Output:\n${process.errorStream.reader().readText()}")
                }
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    prefs.edit {
                        putLong("date", System.currentTimeMillis())
                    }
                    updateScoreList()
                    pd.cancel()
                    showToast(R.string.tip_update_finished)
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    showMsgDialog(getString(R.string.update_failed), it.stackTraceToString())
                    pd.cancel()
                }
            }
        }
    }

    private fun updateScoreList() {
        if (!getDatabasePath("st3.db").exists()) {
            return
        }
        val updateTime = prefs.getLong("date", 0)
        lifecycleScope.launch {
            runCatching {
                readDatabase(this@MainActivity)
            }.onSuccess { record ->
                withContext(Dispatchers.Main) {
                    findViewById<ListView>(R.id.score_list).adapter =
                        ArcaeaScoreAdapter(this@MainActivity, record.records)
                    findViewById<TextView>(R.id.date).text = "Update Time: " + SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.DEFAULT,SimpleDateFormat.DEFAULT,Locale.ENGLISH).format(Date(updateTime))
                    findViewById<TextView>(R.id.max_potential).text = "Best30: " + record.best30Potential.toScaledString() + "  Max Ptt: " + record.maxPotential.toScaledString()
                }
            }.onFailure {
                it.printStackTrace()
                withContext(Dispatchers.Main) {
                    showMsgDialog(getString(R.string.update_failed), it.stackTraceToString())
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val text =
                    Editable.Factory.getInstance().newEditable(getString(R.string.dialog_msg_about))
                showMsgDialog(getString(R.string.about_app), text).apply {
                    findViewById<TextView>(android.R.id.message).apply {
                        autoLinkMask = Linkify.WEB_URLS
                        isClickable = true
                        linksClickable = true
                        this.text = text
                    }
                }
                true
            }
            R.id.action_saveimage -> {
                saveBitmapImage(createViewBitmap(findViewById<LinearLayout>(R.id.list_full)))
                true
            }
            R.id.action_import -> {
                onCopySaveClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
