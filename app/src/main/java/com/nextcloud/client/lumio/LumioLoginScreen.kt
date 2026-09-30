/*
 * Lumio Drive — WorldTrade-style login composable
 *
 * SPDX-FileCopyrightText: 2026 WorldTrade
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.lumio

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.owncloud.android.R

private val WtmBg = Color(0xFFF5F6FA)
private val WtmPrimary = Color(0xFF2563EB)
private val LoginErrorText = Color(0xFFDC2626)
private val SubtitleGray = Color(0xFF64748B)

@Composable
fun LumioLoginScreen(
    busy: Boolean,
    waitingForBrowser: Boolean,
    error: String?,
    onSignIn: () -> Unit,
    onReopenBrowser: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WtmBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.lumio_side_logo),
                contentDescription = "Lumio Drive",
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(72.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "Welcome to Lumio Drive",
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (waitingForBrowser) {
                    "Finish signing in on the secure page, then tap \"Grant access\". " +
                        "You'll come back here automatically."
                } else {
                    "Sign in on Lumio Drive's secure page with your password and authenticator code."
                },
                color = SubtitleGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )

            error?.takeIf { it.isNotBlank() }?.let { msg ->
                Spacer(Modifier.height(12.dp))
                Text(msg, color = LoginErrorText, textAlign = TextAlign.Center, fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = if (waitingForBrowser) onReopenBrowser else onSignIn,
                enabled = waitingForBrowser || !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WtmPrimary,
                    contentColor = Color.White,
                    disabledContainerColor = WtmPrimary.copy(alpha = 0.35f),
                ),
            ) {
                when {
                    waitingForBrowser -> Text("Open sign-in page again", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    busy -> CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                    else -> Text("Sign In", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            if (waitingForBrowser) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = SubtitleGray, fontSize = 14.sp)
                }
            }
        }
    }
}
