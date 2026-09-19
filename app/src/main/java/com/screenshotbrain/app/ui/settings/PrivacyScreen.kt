package com.trove.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trove.app.R
import com.trove.app.ui.components.KitCard
import com.trove.app.ui.components.ScreenHeader
import com.trove.app.ui.theme.Spacing

@Composable
fun PrivacyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { ScreenHeader(title = stringResource(R.string.privacy_policy_title), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md),
        ) {
            KitCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(Spacing.lg)) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.height(Spacing.md))
                    Text(stringResource(R.string.privacy_headline), style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        stringResource(R.string.privacy_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))
            Text(
                stringResource(R.string.privacy_updated),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                stringResource(R.string.privacy_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            POLICY_SECTIONS.forEach { (title, body) ->
                PolicySection(title, body)
            }
            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

private val POLICY_SECTIONS = listOf(
    R.string.privacy_accesses_title to R.string.privacy_accesses_body,
    R.string.privacy_stores_title to R.string.privacy_stores_body,
    R.string.privacy_processing_title to R.string.privacy_processing_body,
    R.string.privacy_sharing_title to R.string.privacy_sharing_body,
    R.string.privacy_usage_title to R.string.privacy_usage_body,
    R.string.privacy_permissions_title to R.string.privacy_permissions_body,
    R.string.privacy_control_title to R.string.privacy_control_body,
    R.string.privacy_changes_title to R.string.privacy_changes_body,
    R.string.privacy_contact_title to R.string.privacy_contact_body,
)

@Composable
private fun PolicySection(@StringRes title: Int, @StringRes body: Int) {
    Spacer(Modifier.height(Spacing.lg))
    Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(Spacing.xs))
    Text(
        stringResource(body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
