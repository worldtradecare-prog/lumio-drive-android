/*
 * Lumio Drive — Library by year dialog (matches web gallery widget)
 *
 * SPDX-FileCopyrightText: 2026 WorldTrade
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.lumio

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.databinding.DialogLumioLibraryByYearBinding
import com.owncloud.android.databinding.ItemLumioYearRowBinding
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LumioLibraryByYearDialog :
    DialogFragment(),
    Injectable {

    @Inject
    lateinit var accountManager: UserAccountManager

    private var _binding: DialogLumioLibraryByYearBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogLumioLibraryByYearBinding.inflate(LayoutInflater.from(requireContext()))
        binding.btnClose.setOnClickListener { dismiss() }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        loadYears()
        return dialog
    }

    private fun loadYears() {
        binding.progress.visibility = View.VISIBLE
        binding.error.visibility = View.GONE
        binding.content.visibility = View.GONE

        val user = accountManager.user
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    GetLumioYearsOperation().execute(user, requireContext())
                } catch (e: Exception) {
                    Log_OC.e(TAG, "years fetch failed", e)
                    null
                }
            }
            if (!isAdded) return@launch
            binding.progress.visibility = View.GONE
            if (result == null || !result.isSuccess || result.resultData == null) {
                binding.error.visibility = View.VISIBLE
                binding.error.text = getString(R.string.library_by_year_error)
                return@launch
            }
            bindData(result.resultData)
        }
    }

    private fun bindData(data: LumioYearsResponse) {
        binding.content.visibility = View.VISIBLE
        binding.yearsList.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        for (year in data.years) {
            val row = ItemLumioYearRowBinding.inflate(inflater, binding.yearsList, false)
            row.year.text = year.year.toString()
            row.breakdown.text = getString(
                R.string.library_by_year_photos_videos,
                year.photos,
                year.videos
            )
            row.total.text = year.total.toString()
            binding.yearsList.addView(row.root)
        }
        binding.totalBreakdown.text = getString(
            R.string.library_by_year_photos_videos,
            data.totalPhotos,
            data.totalVideos
        ) + " · " + data.total
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "LumioLibraryByYearDialog"
        fun newInstance(): LumioLibraryByYearDialog = LumioLibraryByYearDialog()
    }
}
