package io.github.rosemoe.arcaeaScores.app

import android.os.Bundle
import android.app.Activity
import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import io.github.rosemoe.arcaeaScores.R
import io.github.rosemoe.arcaeaScores.arc.ArcaeaPlayResult
import io.github.rosemoe.arcaeaScores.arc.clearTypeShortString
import io.github.rosemoe.arcaeaScores.arc.difficultyMainColor
import io.github.rosemoe.arcaeaScores.arc.scoreGrade
import io.github.rosemoe.arcaeaScores.arc.toScoreText

class ArcaeaScoreAdapter(private val activity: Activity, private val data: List<ArcaeaPlayResult>) :
    BaseAdapter() {
    private val scoreTypeface = Typeface.createFromAsset(activity.assets, "fonts/GeosansLight.ttf")
    private val titleTypeface = Typeface.createFromAsset(activity.assets, "fonts/L2-Regular.ttf")
    private val exoTypeface = Typeface.createFromAsset(activity.assets, "fonts/Exo-Regular.ttf")

    fun readBitmapFromAssets(fileName: String): Bitmap?{
        val assetManager : AssetManager = activity.assets
        var bitmap : Bitmap? = null
        val inputStream = assetManager.open(fileName)
        bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        return bitmap;
    }
    override fun getCount() = data.size

    override fun getItem(position: Int) = data[position]

    override fun getItemId(position: Int) = 0L

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(activity)
            .inflate(R.layout.list_item, parent, false).also {
                it.layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                it.findViewById<TextView>(R.id.songId).typeface = titleTypeface
                it.findViewById<TextView>(R.id.score).typeface = scoreTypeface
                it.findViewById<TextView>(R.id.clearType).typeface = exoTypeface
                it.findViewById<TextView>(R.id.notes).typeface = exoTypeface
                it.findViewById<TextView>(R.id.potential).typeface = exoTypeface
            }
        val i = getItem(position)
        view.findViewById<TextView>(R.id.songId).text = i.title
        when(i.needDownloading){
            true -> {
                view.findViewById<ImageView>(R.id.song_cover).setImageBitmap(readBitmapFromAssets("songs/dl_${i.name}/base_256.jpg"))
            }
            false -> {
                view.findViewById<ImageView>(R.id.song_cover).setImageBitmap(readBitmapFromAssets("songs/${i.name}/base_256.jpg"))
            }
        }
        view.findViewById<View>(R.id.difficulty_color)
            .setBackgroundColor(difficultyMainColor(i.difficulty))
        view.findViewById<TextView>(R.id.clearType).text = "${scoreGrade(i.score)}|${clearTypeShortString(i.clearType)}"
        view.findViewById<TextView>(R.id.potential).text = "${i.constant} > ${String.format("%.2f",i.playPotential)}"
        view.findViewById<TextView>(R.id.score).text = toScoreText(i.score)
        view.findViewById<TextView>(R.id.rank).text = "#${position + 1}"
        view.findViewById<TextView>(R.id.notes).text = "P: ${i.pure}(+${i.maxPure}) \nF: ${i.far} \nL: ${i.lost}"
        return view
    }
}