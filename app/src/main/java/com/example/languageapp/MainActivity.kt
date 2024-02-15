package com.example.languageapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.Group
import com.google.android.gms.ads.MobileAds
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import java.util.Locale

class MainActivity : BaseSplitActivity() {

    /** Listener used to handle changes in state for install requests. */
    private val listener = SplitInstallStateUpdatedListener { state ->
        val langsInstall = state.languages().isNotEmpty()

        val names = if (langsInstall) {
            // We always request the installation of a single language in this sample
            state.languages().first()
        } else state.moduleNames().joinToString(" - ")

        when (state.status()) {
            SplitInstallSessionStatus.DOWNLOADING -> {
                //  In order to see this, the application has to be uploaded to the Play Store.
                displayLoadingState(state, getString(R.string.downloading, names))
            }

            SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                /*
                  This may occur when attempting to download a sufficiently large module.

                  In order to see this, the application has to be uploaded to the Play Store.
                  Then features can be requested until the confirmation path is triggered.
                 */
                manager.startConfirmationDialogForResult(state, this, CONFIRMATION_REQUEST_CODE)
            }

            SplitInstallSessionStatus.INSTALLED -> {
                if (langsInstall) {
                    onSuccessfulLanguageLoad(names)
                }
            }

            SplitInstallSessionStatus.INSTALLING -> displayLoadingState(
                state,
                getString(R.string.installing, names)
            )

            SplitInstallSessionStatus.FAILED -> {
                toastAndLog(
                    getString(
                        R.string.error_for_module, state.errorCode(),
                        state.moduleNames()
                    )
                )
            }
        }
    }

    /** This is needed to handle the result of the manager.startConfirmationDialogForResult
    request that can be made from SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION
    in the listener above. */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CONFIRMATION_REQUEST_CODE) {
            // Handle the user's decision. For example, if the user selects "Cancel",
            // you may want to disable certain functionality that depends on the module.
            if (resultCode == Activity.RESULT_CANCELED) {
                toastAndLog(getString(R.string.user_cancelled))
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private val clickListener by lazy {
        View.OnClickListener {
            when (it.id) {
                R.id.lang_en -> loadAndSwitchLanguage(LANG_EN)
                R.id.lang_es -> loadAndSwitchLanguage(LANG_ES)
            }
        }
    }

    private lateinit var manager: SplitInstallManager

    private lateinit var progress: Group
    private lateinit var buttons: Group
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.app_name)
        setContentView(R.layout.activity_main)
        manager = SplitInstallManagerFactory.create(this)
        initializeViews()

        MobileAds.initialize(this) {
            Log.d("LANG_TEST", "mobileAds initialized")
        }

        val tvLanguage: TextView = findViewById(R.id.tvLanguage)
        val btnCheckContext: Button = findViewById(R.id.btnCheckContext)
        btnCheckContext.setOnClickListener {
            tvLanguage.setText(getString(R.string.language))
        }
    }

    override fun onResume() {
        // Listener can be registered even without directly triggering a download.
        manager.registerListener(listener)
        super.onResume()
    }

    override fun onPause() {
        // Make sure to dispose of the listener once it's no longer needed.
        manager.unregisterListener(listener)
        super.onPause()
    }

    /**
     * Load language splits by language name.
     * @param lang The language code to load (without the region part, e.g. "en", "fr" or "pl").
     */
    private fun loadAndSwitchLanguage(lang: String) {
        updateProgressMessage(getString(R.string.loading_language, lang))
        // Skip loading if the language is already installed. Perform success action directly.
        if (manager.installedLanguages.contains(lang)) {
            updateProgressMessage(getString(R.string.already_installed))
            onSuccessfulLanguageLoad(lang)
            return
        }

        // Create request to install a language by name.
        val request = SplitInstallRequest.newBuilder()
            .addLanguage(Locale.forLanguageTag(lang))
            .build()

        // Load and install the requested language.
        manager.startInstall(request)

        updateProgressMessage(getString(R.string.starting_install_for, lang))
    }

    private fun onSuccessfulLanguageLoad(lang: String) {
        LanguageHelper.language = lang

        recreate()
    }


    /** Display a loading state to the user. */
    private fun displayLoadingState(state: SplitInstallSessionState, message: String) {
        displayProgress()

        progressBar.max = state.totalBytesToDownload().toInt()
        progressBar.progress = state.bytesDownloaded().toInt()

        updateProgressMessage(message)
    }

    /** Set up all view variables. */
    private fun initializeViews() {
        buttons = findViewById(R.id.buttons)
        progress = findViewById(R.id.progress)
        progressBar = findViewById(R.id.progress_bar)
        progressText = findViewById(R.id.progress_text)
        setupClickListener()
    }

    /** Set all click listeners required for the buttons on the UI. */
    private fun setupClickListener() {
        setClickListener(R.id.lang_en, clickListener)
        setClickListener(R.id.lang_es, clickListener)
    }

    private fun setClickListener(id: Int, listener: View.OnClickListener) {
        findViewById<View>(id).setOnClickListener(listener)
    }

    private fun updateProgressMessage(message: String) {
        if (progress.visibility != View.VISIBLE) displayProgress()
        progressText.text = message
    }

    /** Display progress bar and text. */
    private fun displayProgress() {
        progress.visibility = View.VISIBLE
        buttons.visibility = View.GONE
    }
}

fun MainActivity.toastAndLog(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    Log.d(TAG, text)
}

private const val CONFIRMATION_REQUEST_CODE = 1
private const val TAG = "DynamicFeatures"