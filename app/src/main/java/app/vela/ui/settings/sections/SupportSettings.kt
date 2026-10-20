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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.vela.R
import app.vela.ui.dpadHighlight
import app.vela.ui.icons.SymOutlined
import app.vela.ui.settings.SettingsGroup
import app.vela.ui.settings.SettingsScaffold

/** The support account, without the @: it is also the t.me path. */
private const val SUPPORT_HANDLE = "TAGI7VIP"

/** Telegram's own brand blue, for the one button on the page. */
private val TelegramBlue = Color(0xFF229ED9)

/**
 * Support sub-screen: the help message and one way to reach the team.
 *
 * Deliberately just those two things. It replaced the About card in the hub, but the About page
 * itself stays in the build (the version line, the in-app updater and the map-data licenses);
 * Support no longer carries a door to it, so this page reads as one message with one button. The
 * Telegram button needs no permission, opens no WebView and adds no intermediate screen: it hands
 * @TAGI7VIP to the Telegram app, falls back to the t.me page, and reports a phone with neither.
 */
@Composable
internal fun SupportSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    SettingsScaffold(stringResource(R.string.support_center_title), onBack) { topRow ->
        Spacer(Modifier.height(4.dp))
        SettingsGroup {
            Column(
                // Centered column: the message reads as one block under the title and the button
                // sits in the card's horizontal middle, the balance the page was missing.
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.support_welcome),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                // The message is one paragraph, not three stacked ones: the gap that followed
                // "... استخدام التطبيق." came from a second block with its own padding.
                Text(
                    stringResource(R.string.support_message),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.bodyLarge.fontSize * 1.7f,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.support_telegram_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(18.dp))
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(SymOutlined.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.support_telegram_button), fontWeight = FontWeight.SemiBold)
                    }
                }
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
