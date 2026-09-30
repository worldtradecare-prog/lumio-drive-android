/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter.localFileList

import android.content.Context
import android.widget.ImageView
import com.nextcloud.utils.extensions.getSmallThumbnail
import com.owncloud.android.R
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.ThumbnailsCacheManager.AsyncMediaThumbnailDrawable
import com.owncloud.android.datamodel.ThumbnailsCacheManager.MediaThumbnailGenerationTask
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.io.File
import java.util.concurrent.Executors

object LocalFileThumbnailBinder {
    private val TAG: String = LocalFileThumbnailBinder::class.java.simpleName
    private val thumbnailExecutor = Executors.newCachedThreadPool()

    fun setThumbnail(file: File, thumbnailView: ImageView, context: Context, viewThemeUtils: ViewThemeUtils) {
        if (file.isDirectory) {
            thumbnailView.setImageDrawable(MimeTypeUtil.getDefaultFolderIcon(context, viewThemeUtils))
            return
        }

        thumbnailView.tag = file.hashCode()

        // Images and videos: show a real frame/thumbnail at a glance in the upload picker.
        if (MimeTypeUtil.isImage(file) || MimeTypeUtil.isVideo(file)) {
            val cachedThumbnail = file.getSmallThumbnail()
            if (cachedThumbnail != null) {
                thumbnailView.setImageBitmap(cachedThumbnail)
                return
            }

            val placeholder = if (MimeTypeUtil.isVideo(file)) {
                ThumbnailsCacheManager.mDefaultVideo
            } else {
                ThumbnailsCacheManager.mDefaultImg
            }
            thumbnailView.setImageDrawable(AsyncMediaThumbnailDrawable(context.resources, placeholder))

            val task = MediaThumbnailGenerationTask(thumbnailView, context, viewThemeUtils)
            task.executeOnExecutor(thumbnailExecutor, file)
            Log_OC.v(TAG, "Executing media thumbnail task for ${file.name}")
            return
        }

        thumbnailView.setImageDrawable(
            MimeTypeUtil.getFileTypeIcon(null, file.name, context, viewThemeUtils)
        )
    }
}
