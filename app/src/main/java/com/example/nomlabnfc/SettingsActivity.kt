package com.example.nomlabnfc

import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    companion object {
        const val KEY_SLACK_TOKEN = "slack_token"
        const val KEY_CHANNEL = "channel"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }

        intent?.let {
            if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
                handleIntent()
            }
        }
    }

    private fun handleIntent() {
        intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.also { rawMessages ->

            val ndefMessage: NdefMessage = rawMessages.map { it as NdefMessage }.first()
            val message = ndefMessage.records.firstOrNull()?.let { record ->
                String(record.payload.drop(3).toByteArray())
            } ?: return

            val preference = PreferenceManager.getDefaultSharedPreferences(this)
            val channelName = preference.getString(KEY_CHANNEL, "")!!

            lifecycleScope.launch(Dispatchers.Default) {
                SlackMessenger().also {
                    val response = it.postMessage(
                        token = preference.getString(KEY_SLACK_TOKEN, "")!!,
                        channelName = channelName,
                        message = message
                    )

                    withContext(Dispatchers.Main){
                        if (response.isOk) {
                            Toast.makeText(this@SettingsActivity, "$message を #${channelName} に投稿しました", Toast.LENGTH_LONG)
                                .show()
                        } else {
                            Toast.makeText(
                                this@SettingsActivity,
                                "メッセージの送信に失敗しました（${response.error}）",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            val screen = preferenceManager.createPreferenceScreen(context)

            EditTextPreference(context).apply {
                key = KEY_SLACK_TOKEN
                title = "Slack API Token"
                dialogTitle = "Slack API Token"
                isIconSpaceReserved = false
                summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
            }.let {
                screen.addPreference(it)
            }

            ListPreference(context).apply {
                key = KEY_CHANNEL
                title = "投稿先チャンネル"
                entries = arrayOf("sandbox", "attendance")
                entryValues = arrayOf("sandbox", "attendance")
                setDefaultValue("sandbox")
                isIconSpaceReserved = false
                summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
            }.let {
                screen.addPreference(it)
            }
            preferenceScreen = screen
        }
    }

}