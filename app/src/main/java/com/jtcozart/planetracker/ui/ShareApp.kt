package com.jtcozart.planetracker.ui

import android.content.Context
import android.content.Intent

fun shareApp(context: Context) {
    val packageName = context.packageName
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_TEXT,
            "Check out PlaneTracker — it scans for aircraft near you in real time: " +
                "https://play.google.com/store/apps/details?id=$packageName",
        )
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share PlaneTracker"))
}
