/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 WorldTrade / Lumio Drive
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter.localFileList

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.core.content.FileProvider
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import java.io.File

/**
 * Press-and-hold video peek for the upload picker: show+play while held, dismiss on release.
 * Dialog is not touchable so ACTION_UP still reaches the list row underneath.
 */
object LocalVideoHoldPreview {
    private const val TAG = "LocalVideoHoldPreview"

    private var dialog: Dialog? = null
    private var videoView: VideoView? = null

    @JvmStatic
    fun show(activity: Activity, file: File) {
        if (activity.isFinishing) return
        dismiss()

        val uri = try {
            FileProvider.getUriForFile(
                activity,
                activity.getString(R.string.file_provider_authority),
                file
            )
        } catch (e: IllegalArgumentException) {
            Log_OC.e(TAG, "Cannot preview outside FileProvider paths: $e")
            return
        }

        val container = FrameLayout(activity).apply {
            setBackgroundColor(Color.parseColor("#CC000000"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val vv = VideoView(activity).apply {
            val lp = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
            layoutParams = lp
            setVideoURI(uri)
            setOnPreparedListener { mp ->
                mp.isLooping = true
                // Fit without stretching too aggressively
                start()
            }
            setOnErrorListener { _, what, extra ->
                Log_OC.e(TAG, "VideoView error what=$what extra=$extra for ${file.name}")
                dismiss()
                true
            }
        }
        container.addView(vv)

        val d = Dialog(activity).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(container)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            window?.let { w ->
                w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                w.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // Let the finger release still hit the list row under the overlay.
                w.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                w.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
        }

        dialog = d
        videoView = vv
        d.show()
        Log_OC.d(TAG, "Hold preview started for ${file.name}")
    }

    @JvmStatic
    fun dismiss() {
        try {
            videoView?.stopPlayback()
        } catch (_: Exception) {
        }
        try {
            dialog?.dismiss()
        } catch (_: Exception) {
        }
        videoView = null
        dialog = null
    }

    @JvmStatic
    fun isShowing(): Boolean = dialog?.isShowing == true
}
