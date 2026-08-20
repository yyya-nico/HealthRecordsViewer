package co.yyyanico.healthrecordsviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.yyyanico.healthrecordsviewer.ui.theme.HealthRecordsViewerTheme

class PrivacyPolicyActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthRecordsViewerTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.privacy_policy_title)) },
                            actions = {
                                TextButton(onClick = ::finish) {
                                    Text(stringResource(R.string.close))
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    PrivacyPolicy(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyPolicy(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        PolicySection(
            title = stringResource(R.string.privacy_policy_data_title),
            body = stringResource(R.string.privacy_policy_data_body)
        )
        PolicySection(
            title = stringResource(R.string.privacy_policy_purpose_title),
            body = stringResource(R.string.privacy_policy_purpose_body)
        )
        PolicySection(
            title = stringResource(R.string.privacy_policy_handling_title),
            body = stringResource(R.string.privacy_policy_handling_body)
        )
        PolicySection(
            title = stringResource(R.string.privacy_policy_permissions_title),
            body = stringResource(R.string.privacy_policy_permissions_body)
        )
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(text = body, style = MaterialTheme.typography.bodyLarge)
    }
}
