package com.udacity

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.os.bundleOf
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private var downloadID: Long = -1L

    private lateinit var notificationManager: NotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        notificationManager =
            ContextCompat.getSystemService(applicationContext, NotificationManager::class.java)!!

        custom_button.setOnClickListener {
            when (radio_group.checkedRadioButtonId) {
                R.id.radio_button_glide -> {
                    download(GLIDE_URL, getString(R.string.text_download_option_glide))
                }
                R.id.radio_button_load_app -> {
                    download(UDACITY_URL, getString(R.string.text_download_option_load_app))
                }
                R.id.radio_button_retrofit -> {
                    download(RETROFIT_URL, getString(R.string.text_download_option_retrofit))
                }
                else -> {
                    startActivity(Intent(applicationContext, DetailActivity::class.java).apply {
                        putExtras(
                            bundleOf(
                                DetailActivity.EXTRA_KEY_REPO_NAME to "askdmasd",
                                DetailActivity.EXTRA_KEY_DOWNLOAD_SUCCESS to true,
                                DetailActivity.EXTRA_KEY_NOTIFICATION_ID to 1
                            )
                        )
                    })
                    //showToast(R.string.text_toast_select_file_to_download)
                }
            }
        }
    }

    private fun download(url: String, repoName: String) {
        if (downloadID != -1L) {
            showToast(R.string.text_toast_download_in_progress)

            return
        }

        val request =
            DownloadManager.Request(Uri.parse(url))
                .setTitle(getString(R.string.app_name))
                .setDescription(getString(R.string.app_description))
                .setRequiresCharging(false)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadID =
            downloadManager.enqueue(request)// enqueue puts the download request in the queue.

        setLoadingState()
        registerForStatusUpdates(downloadManager, repoName)
    }

    private fun registerForStatusUpdates(downloadManager: DownloadManager, repoName: String) {
        with(CoroutineScope(Dispatchers.IO)) {
            launch {
                var isDownloading = true
                while (isDownloading) {
                    if (downloadID == -1L) return@launch

                    val cursor =
                        downloadManager.query(DownloadManager.Query().setFilterById(downloadID))
                    if (cursor.moveToFirst()) {
                        when (cursor.getIntOrNull(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                            DownloadManager.STATUS_RUNNING -> {

                                val total =
                                    cursor.getLongOrNull(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                                if (total != null && total > 0) {
                                    val downloaded =
                                        cursor.getLongOrNull(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                                    downloaded?.let {
                                        val progress = ((it * 100L) / total).toFloat()
                                        setButtonProgress(progress)
                                    }
                                }

                            }
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                isDownloading = false
                                Log.i("Download", "SUCCESS")
                                onDownloadSuccess(downloadID, repoName)
                            }
                            DownloadManager.STATUS_FAILED -> {
                                isDownloading = false
                                Log.i("Download", "FAILED")
                                onDownloadFailed(downloadID, repoName)
                            }
                        }
                    }
                    cursor.close()
                }
            }
        }
    }

    private fun onDownloadFailed(downloadId: Long, repoName: String) {
        downloadID = -1L
        custom_button.buttonState = ButtonState.Completed
        sendNotification(downloadId, repoName, false)
    }

    private fun onDownloadSuccess(downloadId: Long, repoName: String) {
        downloadID = -1L
        custom_button.buttonState = ButtonState.Completed
        sendNotification(downloadId, repoName, true)
    }

    private fun setLoadingState() {
        custom_button.text = getString(R.string.text_downloading)
        custom_button.buttonState = ButtonState.Loading
    }

    private fun setButtonProgress(progress: Float) {
        custom_button.progress = progress
    }

    private fun showToast(stringResId: Int) {
        applicationContext?.let {
            Toast.makeText(
                it,
                stringResId,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun sendNotification(downloadId: Long, repoName: String, success: Boolean) {
        val notificationId = downloadId.toInt()
        createChannel()

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            Intent(applicationContext, DetailActivity::class.java).apply {
                putExtras(
                    bundleOf(
                        DetailActivity.EXTRA_KEY_REPO_NAME to repoName,
                        DetailActivity.EXTRA_KEY_DOWNLOAD_SUCCESS to success,
                        DetailActivity.EXTRA_KEY_NOTIFICATION_ID to notificationId
                    )
                )
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val action = NotificationCompat.Action.Builder(
            null,
            applicationContext.getString(R.string.download_notification_action),
            pendingIntent
        ).build()

        val notification = NotificationCompat.Builder(
            applicationContext,
            CHANNEL_ID
        ).apply {
            setSmallIcon(R.drawable.ic_assistant_black_24dp)
            setContentTitle(applicationContext.getString(R.string.download_notification_title))
            setContentText(applicationContext.getString(if (success) R.string.download_notification_message_success else R.string.download_notification_message_failed))
            addAction(action)
        }.build()

        notificationManager.notify(notificationId, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationChannel.description = CHANNEL_DESCRIPTION

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    companion object {
        private const val GLIDE_URL =
            "https://github.com/bumptech/glide/archive/refs/heads/master.zip"
        private const val UDACITY_URL =
            "https://github.com/udacity/nd940-c3-advanced-android-programming-project-starter/archive/master.zip"
        private const val RETROFIT_URL =
            "https://github.com/square/retrofit/archive/refs/heads/master.zip"

        private const val CHANNEL_ID = "downloadNotificationId"
        private const val CHANNEL_NAME = "Downloads"
        private const val CHANNEL_DESCRIPTION = "Downloads notification channel"

    }

}
