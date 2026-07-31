package com.jtcozart.planetracker.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.jtcozart.planetracker.data.SpottedAircraft
import com.jtcozart.planetracker.data.StreakState
import java.io.File
import java.io.FileOutputStream

/** Renders a shareable "streak card" image and launches the system share sheet. */
fun shareStreakCard(context: Context, streak: StreakState, spotted: List<SpottedAircraft>) {
    val bitmap = renderStreakCard(context, streak, spotted)

    val cacheDir = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(cacheDir, "streak.png")
    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(
            Intent.EXTRA_TEXT,
            "I'm on a ${streak.currentStreak}-day PlaneTracker spotting streak!",
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share your streak"))
}

private fun renderStreakCard(context: Context, streak: StreakState, spotted: List<SpottedAircraft>): Bitmap {
    val width = 1080
    val height = 1350
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    canvas.drawColor(Color.parseColor("#0B0B0F"))

    val radarGreen = Color.parseColor("#00E676")
    val amber = Color.parseColor("#F9A825")

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 56f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val flamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = amber
        textSize = 220f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val streakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = radarGreen
        textSize = 130f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        textSize = 42f
        textAlign = Paint.Align.CENTER
    }
    val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 46f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 40f
    }

    canvas.drawText("PlaneTracker", 60f, 100f, titlePaint)

    canvas.drawText("🔥", width / 2f, 400f, flamePaint)
    canvas.drawText("${streak.currentStreak} DAY STREAK", width / 2f, 540f, streakPaint)
    canvas.drawText("Longest streak: ${streak.longestStreak} days", width / 2f, 610f, subPaint)

    canvas.drawText("TOP AIRCRAFT SPOTTED", 60f, 760f, sectionPaint)

    val topModels = spotted
        .groupingBy { it.type.ifEmpty { "Unknown type" } }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(6)

    var y = 850f
    topModels.forEach { (model, count) ->
        canvas.drawText(model, 60f, y, rowPaint)
        canvas.drawText("x$count", width - 60f - rowPaint.measureText("x$count"), y, rowPaint)
        y += 70f
    }

    canvas.drawText(
        "Total spotted: ${spotted.size}",
        width / 2f,
        height - 60f,
        subPaint,
    )

    return bitmap
}
