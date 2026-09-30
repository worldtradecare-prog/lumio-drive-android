/*
 * Lumio Drive — branded login over Nextcloud Android
 *
 * SPDX-FileCopyrightText: 2026 WorldTrade
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.lumio

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.operations.PostMethod
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.authentication.LoginUrlInfo
import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import org.json.JSONObject
import javax.inject.Inject

/**
 * Lumio Drive login with a fixed server, using Nextcloud Login Flow v2: the password and
 * 2FA code are entered on the server's own page in the browser, and the app only ever
 * receives a revocable app password. Account creation is handed to [AuthenticatorActivity.login].
 */
class LumioLoginActivity :
    AuthenticatorActivity(),
    Injectable {

    @Inject
    lateinit var lumioClientFactory: ClientFactory

    private var busy by mutableStateOf(false)
    private var waitingForBrowser by mutableStateOf(false)
    private var errorMessage by mutableStateOf<String?>(null)
    private var loginPageUrl: String? = null
    private var flowJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Replace stock account-setup chrome with WorldTrade card UI.
        // accountSetupBinding from super stays alive for status helpers during auth.
        setContent {
            LumioLoginScreen(
                busy = busy,
                waitingForBrowser = waitingForBrowser,
                error = errorMessage,
                onSignIn = ::startBrowserLogin,
                onReopenBrowser = { loginPageUrl?.let(::openLoginPage) },
                onCancel = ::cancelBrowserLogin,
            )
        }
    }

    private fun startBrowserLogin() {
        if (busy) {
            return
        }
        busy = true
        errorMessage = null
        flowJob = lifecycleScope.launch {
            val flow = withContext(Dispatchers.IO) { requestLoginFlow() }
            if (flow == null) {
                showError("Couldn't reach Lumio Drive. Check your connection and try again.")
                return@launch
            }
            loginPageUrl = flow.loginUrl
            waitingForBrowser = true
            if (!openLoginPage(flow.loginUrl)) {
                return@launch
            }
            val credentials = pollForAppPassword(flow)
            waitingForBrowser = false
            if (credentials == null) {
                showError("Sign-in expired. Tap Sign In to try again.")
                return@launch
            }
            login(credentials)
        }
    }

    private fun cancelBrowserLogin() {
        flowJob?.cancel()
        flowJob = null
        waitingForBrowser = false
        busy = false
    }

    private fun requestLoginFlow(): LoginFlow? {
        val post = PostMethod(FIXED_SERVER + WEB_LOGIN, false, FormBody.Builder().build())
        return try {
            val status = post.execute(lumioClientFactory.createPlainClient())
            if (status != HTTP_OK) {
                Log_OC.e(TAG, "login flow start failed, status: $status")
                return null
            }
            val json = JSONObject(post.getResponseBodyAsString())
            val poll = json.getJSONObject("poll")
            LoginFlow(
                loginUrl = json.getString("login"),
                pollEndpoint = poll.getString("endpoint"),
                pollToken = poll.getString("token"),
            ).takeIf { isFixedServerUrl(it.loginUrl) && isFixedServerUrl(it.pollEndpoint) }
        } catch (e: Exception) {
            Log_OC.e(TAG, "login flow start failed", e)
            null
        } finally {
            post.releaseConnection()
        }
    }

    private suspend fun pollForAppPassword(flow: LoginFlow): LoginUrlInfo? {
        val deadline = System.currentTimeMillis() + FLOW_LIFETIME_MS
        while (System.currentTimeMillis() < deadline) {
            val credentials = withContext(Dispatchers.IO) { pollOnce(flow) }
            if (credentials != null) {
                return credentials
            }
            delay(POLL_INTERVAL_MS)
        }
        return null
    }

    /** Returns credentials once the user grants access; null while pending or on transient errors. */
    private fun pollOnce(flow: LoginFlow): LoginUrlInfo? {
        val body = FormBody.Builder().add("token", flow.pollToken).build()
        val post = PostMethod(flow.pollEndpoint, false, body)
        return try {
            if (post.execute(lumioClientFactory.createPlainClient()) != HTTP_OK) {
                return null
            }
            val json = JSONObject(post.getResponseBodyAsString())
            LoginUrlInfo(
                server = json.getString("server"),
                loginName = json.getString("loginName"),
                appPassword = json.getString("appPassword"),
            ).takeIf { isFixedServerUrl(it.server) && it.isValid(HTTP_OK) }
        } catch (e: Exception) {
            Log_OC.w(TAG, "login flow poll failed: ${e.message}")
            null
        } finally {
            post.releaseConnection()
        }
    }

    private fun openLoginPage(url: String): Boolean {
        val uri = Uri.parse(url)
        try {
            CustomTabsIntent.Builder().build().launchUrl(this, uri)
            return true
        } catch (_: ActivityNotFoundException) {
            Log_OC.w(TAG, "no Custom Tabs browser, falling back to ACTION_VIEW")
        }
        return try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (_: ActivityNotFoundException) {
            cancelBrowserLogin()
            errorMessage = "No web browser found. Install Chrome or another browser to sign in."
            false
        }
    }

    private fun showError(message: String) {
        waitingForBrowser = false
        busy = false
        errorMessage = message
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
        waitingForBrowser = false
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

    private data class LoginFlow(val loginUrl: String, val pollEndpoint: String, val pollToken: String)

    companion object {
        private const val TAG = "LumioLoginActivity"
        const val FIXED_SERVER = "https://lumiodrive.worldtrade.com.my"
        private const val HTTP_OK = 200

        // Nextcloud expires Login Flow v2 tokens after 20 minutes.
        private const val FLOW_LIFETIME_MS = 20 * 60 * 1000L
        private const val POLL_INTERVAL_MS = 2_000L

        private fun isFixedServerUrl(url: String): Boolean =
            url.trimEnd('/') == FIXED_SERVER || url.startsWith("$FIXED_SERVER/")
    }
}
