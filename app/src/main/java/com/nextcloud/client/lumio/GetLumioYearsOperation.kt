/*
 * Lumio Drive — GET /apps/lumio_ui/years
 *
 * SPDX-FileCopyrightText: 2026 WorldTrade
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.lumio

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.methods.GetMethod
import org.json.JSONObject

/**
 * Fetches Library-by-year counts from the Lumio UI app on the Nextcloud server.
 * Endpoint: GET {base}/apps/lumio_ui/years
 */
class GetLumioYearsOperation : RemoteOperation<LumioYearsResponse>() {

    override fun run(client: OwnCloudClient): RemoteOperationResult<LumioYearsResponse> {
        var getMethod: GetMethod? = null
        return try {
            val url = client.baseUri.toString().trimEnd('/') + YEARS_PATH
            getMethod = GetMethod(url)
            getMethod.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE)
            val status = client.executeMethod(getMethod, READ_TIMEOUT_MS, CONNECTION_TIMEOUT_MS)
            if (status != HttpStatus.SC_OK) {
                client.exhaustResponse(getMethod.responseBodyAsStream)
                return RemoteOperationResult<LumioYearsResponse>(false, getMethod)
            }
            val body = getMethod.responseBodyAsString ?: ""
            val parsed = parse(body)
            RemoteOperationResult<LumioYearsResponse>(true, getMethod).also {
                it.resultData = parsed
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, "Failed to fetch Lumio years", e)
            RemoteOperationResult(e)
        } finally {
            getMethod?.releaseConnection()
        }
    }

    private fun parse(body: String): LumioYearsResponse {
        val root = JSONObject(body)
        val yearsJson = root.optJSONArray("years")
        val years = mutableListOf<LumioYearCount>()
        if (yearsJson != null) {
            for (i in 0 until yearsJson.length()) {
                val y = yearsJson.getJSONObject(i)
                years.add(
                    LumioYearCount(
                        year = y.optInt("year"),
                        photos = y.optInt("photos"),
                        videos = y.optInt("videos"),
                        total = y.optInt("total")
                    )
                )
            }
        }
        return LumioYearsResponse(
            years = years,
            totalPhotos = root.optInt("totalPhotos"),
            totalVideos = root.optInt("totalVideos"),
            total = root.optInt("total")
        )
    }

    companion object {
        private val TAG = GetLumioYearsOperation::class.java.simpleName
        private const val YEARS_PATH = "/apps/lumio_ui/years"
        private const val READ_TIMEOUT_MS = 30_000
        private const val CONNECTION_TIMEOUT_MS = 5_000
    }
}
