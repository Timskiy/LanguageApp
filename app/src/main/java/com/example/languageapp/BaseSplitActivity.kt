package com.example.languageapp

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.splitcompat.SplitCompat

abstract class BaseSplitActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val ctx = LanguageHelper.getLanguageConfigurationContext(newBase)
        super.attachBaseContext(ctx)
        SplitCompat.installActivity(this)
    }
}