/*
 * Lumio Drive — year library API models
 *
 * SPDX-FileCopyrightText: 2026 WorldTrade
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.lumio

data class LumioYearCount(
    val year: Int,
    val photos: Int,
    val videos: Int,
    val total: Int
)

data class LumioYearsResponse(
    val years: List<LumioYearCount>,
    val totalPhotos: Int,
    val totalVideos: Int,
    val total: Int
)
