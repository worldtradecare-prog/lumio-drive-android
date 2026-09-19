/*
 * Lumio Drive — branded login over Nextcloud Android
 *
 * SPDX-FileCopyrightText: 2026 WorldTrade
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.lumio

import android.accounts.Account
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.DialogFragment
import com.nextcloud.client.di.Injectable
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.authentication.LoginUrlInfo
import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.lib.common.operations.RemoteOperationResult

/**
 * WorldTrade-style username/password login with fixed Lumio Drive server.
 * Reuses [AuthenticatorActivity.login] so account creation and FileDisplay hand-off stay stock.
 */
class LumioLoginActivity :
    AuthenticatorActivity(),
    Injectable {

    private var busy by mutableStateOf(false)
    private var errorMessage by mutableStateOf<String?>(null)
    private var initialUsername by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        val account = intent?.getParcelableExtra<Account>(EXTRA_ACCOUNT)
        if (account != null) {
            // account.name is usually "user@host"
            initialUsername = account.name.substringBefore("@")
        }
        // Replace stock account-setup chrome with WorldTrade card UI.
        // accountSetupBinding from super stays alive for status helpers during auth.
        setContent {
            LumioLoginScreen(
                busy = busy,
                error = errorMessage,
                initialUsername = initialUsername,
                onSignIn = { username, password -> submitLogin(username, password) },
            )
        }
    }

    private fun submitLogin(username: String, password: String) {
        if (busy) {
            return
        }
        val user = username.trim()
        if (user.isEmpty() || password.length < 4) {
            errorMessage = "Enter username and password"
            return
        }
        busy = true
        errorMessage = null
        login(LoginUrlInfo(FIXED_SERVER, user, password))
    }

    override fun onAuthenticatorTaskCallback(result: RemoteOperationResult<UserInfo?>?) {
        if (result != null && result.isSuccess) {
            busy = false
            super.onAuthenticatorTaskCallback(result)
            return
        }
        // Stock AuthenticatorActivity finishes when show_server_url_input is false,
        // which drops the user onto the server-address screen. Stay here instead.
        busy = false
        dismissWaitingUi()
        val detail = result?.getLogMessage(this)
        errorMessage = if (detail.isNullOrBlank()) {
            getString(com.owncloud.android.R.string.auth_unauthorized)
        } else {
            detail
        }
    }

    private fun dismissWaitingUi() {
        val frag = supportFragmentManager.findFragmentByTag("WAIT_DIALOG")
        if (frag is DialogFragment) {
            try {
                frag.dismiss()
            } catch (_: IllegalStateException) {
                frag.dismissAllowingStateLoss()
            }
        }
    }

    companion object {
        const val FIXED_SERVER = "https://lumiodrive.worldtrade.com.my"
    }
}
