package com.udacity

import android.app.NotificationManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_detail.*
import kotlinx.android.synthetic.main.content_detail.*

class DetailActivity : AppCompatActivity() {

    private var repoName: String = ""
    private var downloadSuccess: Boolean = false
    private lateinit var notificationManager: NotificationManager

    companion object {
        const val EXTRA_KEY_REPO_NAME = "com.udacity.DetailActivity.EXTRA_KEY_REPO_NAME"
        const val EXTRA_KEY_DOWNLOAD_SUCCESS =
            "com.udacity.DetailActivity.EXTRA_KEY_DOWNLOAD_SUCCESS"
        const val EXTRA_KEY_NOTIFICATION_ID =
            "com.udacity.DetailActivity.EXTRA_KEY_NOTIFICATION_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        setSupportActionBar(toolbar)

        notificationManager =
            ContextCompat.getSystemService(applicationContext, NotificationManager::class.java)!!

        button_ok.setOnClickListener {
            motion_layout.transitionToStart {
                finish()
            }
        }

        repoName = this.intent.getStringExtra(EXTRA_KEY_REPO_NAME) ?: ""
        downloadSuccess = this.intent.getBooleanExtra(EXTRA_KEY_DOWNLOAD_SUCCESS, false)

        val notificationId = intent.getIntExtra(EXTRA_KEY_NOTIFICATION_ID, -1)
        if (notificationId > -1) {
            notificationManager.cancel(notificationId)
        }

        bindData()
    }

    override fun onResume() {
        super.onResume()
        motion_layout.transitionToState(R.id.end)
    }

    private fun bindData() {
        text_file_name_value.text = repoName
        text_status_value.setText(if (downloadSuccess) R.string.text_status_success else R.string.text_status_failed)
        text_status_value.setTextColor(applicationContext.getColor(if (downloadSuccess) R.color.colorSuccess else R.color.colorFailed))
    }

}
