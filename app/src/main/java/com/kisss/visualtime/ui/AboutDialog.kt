package com.kisss.visualtime.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kisss.visualtime.R
import com.kisss.visualtime.ui.theme.DarkCharcoal
import com.kisss.visualtime.ui.theme.SandGold
import com.kisss.visualtime.ui.theme.SoftWhite

@Suppress("DEPRECATION")
internal fun installedAppVersion(context: Context): String =
    context.packageManager
        .getPackageInfo(context.packageName, 0)
        .versionName
        ?.takeIf(String::isNotBlank)
        ?: context.getString(R.string.about_version_unknown)

internal fun openAboutLink(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            context,
            context.getString(R.string.about_browser_unavailable),
            Toast.LENGTH_SHORT,
        ).show()
    } catch (_: SecurityException) {
        Toast.makeText(
            context,
            context.getString(R.string.about_browser_unavailable),
            Toast.LENGTH_SHORT,
        ).show()
    }
}

@Composable
internal fun AboutDialog(
    versionName: String,
    onDismiss: () -> Unit,
    onOpenLink: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCharcoal,
        titleContentColor = SoftWhite,
        textContentColor = SoftWhite.copy(alpha = 0.88f),
        title = {
            Text(
                text = stringResource(R.string.about_title),
                fontWeight = FontWeight.Medium,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(R.string.about_app_name, stringResource(R.string.app_name)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.about_version, versionName),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.about_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.about_developer),
                    style = MaterialTheme.typography.bodyMedium,
                )
                AboutLink(
                    label = R.string.about_website,
                    url = R.string.about_website_url,
                    onOpenLink = onOpenLink,
                )
                AboutLink(
                    label = R.string.about_privacy_policy,
                    url = R.string.about_privacy_policy_url,
                    onOpenLink = onOpenLink,
                )
                AboutLink(
                    label = R.string.about_terms_of_use,
                    url = R.string.about_terms_of_use_url,
                    onOpenLink = onOpenLink,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.about_close))
            }
        },
    )
}

@Composable
private fun AboutLink(
    @StringRes label: Int,
    @StringRes url: Int,
    onOpenLink: (String) -> Unit,
) {
    val urlText = stringResource(url)
    TextButton(
        onClick = { onOpenLink(urlText) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        Text(
            text = stringResource(R.string.about_link, stringResource(label), urlText),
            color = SandGold,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
