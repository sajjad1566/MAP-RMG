package app.vela.ui.settings.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.vela.R
import app.vela.ui.dpadClickable
import app.vela.ui.dpadHighlight
import app.vela.ui.icons.SymOutlined
import app.vela.ui.settings.PageIntro
import app.vela.ui.settings.SettingsGroup
import app.vela.ui.settings.SettingsScaffold

/** The support account, without the @: it is also the t.me path. */
private const val SUPPORT_HANDLE = "TAGI7VIP"

/** Telegram's own brand blue, for the one button on the page. */
private val TelegramBlue = Color(0xFF229ED9)

/**
 * Support sub-screen: who to write to and how. Replaces the About card in the hub, but the About
 * page itself is still one row down the page, so the version line, the in-app updater and the
 * map-data license notices keep a door (nothing about them was deleted).
 *
 * No permission, no WebView, no in-app browser: the Telegram button hands the account to the
 * Telegram app and falls back to the t.me page when no app answers.
 */
@Composable
internal fun SupportSettingsScreen(
    onBack: () -> Unit,
    onOpenAppInfo: () -> Unit,
) {
    val context = LocalContext.current
    SettingsScaffold(stringResource(R.string.support_center_title), onBack) { topRow ->
        Spacer(Modifier.height(4.dp))
        PageIntro(stringResource(R.string.support_welcome))
        SettingsGroup {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    stringResource(R.string.support_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.support_telegram_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { openTelegram(context) },
                    // The top focusable control here: Back routes its DOWN to this button, and the
                    // ring uses the button's own pill shape.
                    modifier = topRow.dpadHighlight(CircleShape),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TelegramBlue,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(SymOutlined.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.support_telegram_button), fontWeight = FontWeight.Medium)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        SettingsGroup(title = stringResource(R.string.support_app_info_group)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .dpadHighlight(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                    .dpadClickable(onClick = onOpenAppInfo)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    SymOutlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.support_app_info), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.support_app_info_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    SymOutlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(56.dp))
    }
}

/**
 * Opens [SUPPORT_HANDLE] in Telegram. The app's own scheme is tried first; an install that does
 * not answer it (no Telegram, or a client that registers only the web links) falls through to the
 * t.me page, which a browser or Telegram itself opens. Every failure is caught: a phone with
 * neither app shows a toast instead of crashing the settings page.
 */
private fun openTelegram(context: android.content.Context) {
    val inApp = android.content.Intent(
        android.content.Intent.ACTION_VIEW,
        android.net.Uri.parse("tg://resolve?domain=$SUPPORT_HANDLE"),
    )
    val onWeb = android.content.Intent(
        android.content.Intent.ACTION_VIEW,
        android.net.Uri.parse("https://t.me/$SUPPORT_HANDLE"),
    )
    if (context !is android.app.Activity) {
        // The settings page is drawn inside the activity, so this is only a guard for a caller
        // that one day hands in another context.
        inApp.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        onWeb.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val opened = runCatching { context.startActivity(inApp) }.isSuccess ||
        runCatching { context.startActivity(onWeb) }.isSuccess
    if (!opened) {
        android.widget.Toast.makeText(context, R.string.support_telegram_failed, android.widget.Toast.LENGTH_LONG).show()
    }
}
