package co.yyyanico.healthrecordsviewer

//import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
//import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
//import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.ExtendedFloatingActionButton
//import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
//import androidx.health.connect.client.changes.Change
//import androidx.health.connect.client.changes.DeletionChange
//import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
//import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import co.yyyanico.healthrecordsviewer.ui.theme.HealthRecordsViewerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
//import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private lateinit var requestPermissions: ActivityResultLauncher<Set<String>>
    private val permissions =
        setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class)
        )
    private var healthConnectClient: HealthConnectClient? = null
//    private var changesToken: String? = null
    private val weights = mutableMapOf<String, Double>()
    private var steps = -1L
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthRecordsViewerTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(title = {
                            Text(getString(R.string.app_name))
                        })
                    },
                    content = { innerPadding ->
                        Box(
                            modifier = Modifier
                                .padding(innerPadding)
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 72.dp)
                        ) {
                            Text(
                                text = getString(R.string.please_wait),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                )
            }
        }
        // Create the permissions launcher
        val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()

        requestPermissions = registerForActivityResult(requestPermissionActivityContract) { granted ->
            if (granted.containsAll(permissions)) {
                // Permissions successfully granted
                CoroutineScope(Dispatchers.Main).launch {
                    healthConnectClient?.let { getData(it) }
                    setContent {
                        HealthRecordsViewerTheme {
                            Scaffold(
                                topBar = {
                                    TopAppBar(
                                        title = {
                                            Text(getString(R.string.app_name))
                                        },
                                        actions = {
                                            IconButton(onClick = ::startHealthConnectApp) {
                                                Icon(Icons.Outlined.Settings, getString(R.string.open_health_connect))
                                            }
                                        }
                                    )
                                },
                                content = { innerPadding ->
                                    HealthRecordsViewer(
                                        weights,
                                        steps,
                                        context = applicationContext,
                                        modifier = Modifier.padding(innerPadding)
                                    )
                                },
//                                floatingActionButton = {
//                                    ExtendedFloatingActionButton(
//                                        onClick = ::startHealthConnectApp
//                                    ) {
//                                        Icon(Icons.Filled.Settings, getString(R.string.open_health_connect))
//                                        Spacer(modifier = Modifier.width(8.dp))
//                                        Text(getString(R.string.open_health_connect))
//                                    }
//                                },
//                                floatingActionButtonPosition = FabPosition.Center
                            )
                        }
                    }
                }
            } else {
                Toast.makeText(applicationContext, getString(R.string.no_permissions), Toast.LENGTH_SHORT).show()
            }
        }

        healthConnectWrapper()
    }

    override fun onResume() {
        super.onResume()

        CoroutineScope(Dispatchers.Main).launch {
            healthConnectClient?.let { checkPermissionsAndRun(it) }
        }
    }

    private fun healthConnectWrapper() {
        val context = applicationContext
        val providerPackageName = "com.google.android.apps.healthdata"
        val availabilityStatus = HealthConnectClient.getSdkStatus(context, providerPackageName)
        if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE) {
            return // early return as there is no viable integration
        }
        if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
            // Optionally redirect to package installer to find a provider, for example:
            val uriString = "market://details?id=$providerPackageName&url=healthconnect%3A%2F%2Fonboarding"
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setPackage("com.android.vending")
                    data = Uri.parse(uriString)
                    putExtra("overlay", true)
                    putExtra("callerId", context.packageName)
                }
            )
            return
        }

        healthConnectClient = HealthConnectClient.getOrCreate(context)
//        CoroutineScope(Dispatchers.Main).launch {
//            changesToken = getChangesToken()
//        }
    }

    private fun startHealthConnectApp() {
        val intent = Intent().apply {
            action = "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"
        }
        startActivity(intent)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private suspend fun checkPermissionsAndRun(healthConnectClient: HealthConnectClient) {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        if (granted.containsAll(permissions)) {
            // Permissions already granted; proceed with inserting or reading data
            getData(healthConnectClient)
            setContent {
                HealthRecordsViewerTheme {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(getString(R.string.app_name))
                                },
                                actions = {
                                    IconButton(onClick = ::startHealthConnectApp) {
                                        Icon(Icons.Outlined.Settings, getString(R.string.open_health_connect))
                                    }
                                }
                            )
                        },
                        content = { innerPadding ->
                            HealthRecordsViewer(
                                weights,
                                steps,
                                context = applicationContext,
                                modifier = Modifier.padding(innerPadding)
                            )
                        },
//                        floatingActionButton = {
//                            ExtendedFloatingActionButton(
//                                onClick = ::startHealthConnectApp
//                            ) {
//                                Icon(Icons.Filled.Settings, getString(R.string.open_health_connect))
//                                Spacer(modifier = Modifier.width(8.dp))
//                                Text(getString(R.string.open_health_connect))
//                            }
//                        },
//                        floatingActionButtonPosition = FabPosition.Center
                    )
                }
            }
        } else {
            requestPermissions.launch(permissions)
        }
    }

//    private suspend fun getChangesToken(): String {
//        return healthConnectClient!!.getChangesToken(
//            ChangesTokenRequest(
//                setOf(
//                    StepsRecord::class,
//                    WeightRecord::class
//                )
//            )
//        )
//    }

//    private sealed class ChangesMessage {
//        data class NoMoreChanges(val nextChangesToken: String) : ChangesMessage()
//        data class ChangeList(val changes: List<Change>) : ChangesMessage()
//    }

//    private suspend fun getChanges(token: String): Flow<ChangesMessage> = flow {
//        var nextChangesToken = token
//        do {
//            val response = healthConnectClient!!.getChanges(nextChangesToken)
//            if (response.changesTokenExpired) {
//                throw IOException("Changes token has expired")
//            }
//            emit(ChangesMessage.ChangeList(response.changes))
//            nextChangesToken = response.nextChangesToken
//        } while (response.hasMore)
//        emit(ChangesMessage.NoMoreChanges(nextChangesToken))
//    }

//    private var isFirst = true
//    private val changes = mutableListOf<Change>()
    private suspend fun getData(healthConnectClient: HealthConnectClient) {
//        if (!isFirst) {
//            changesToken?.let { token ->
//                getChanges(token).collect { message ->
//                    when (message) {
//                        is ChangesMessage.ChangeList -> {
//                            changes.addAll(message.changes)
//                        }
//                        is ChangesMessage.NoMoreChanges -> {
//                            changesToken = message.nextChangesToken
//                            Log.i(TAG, "Updating changes token: $changesToken")
//                        }
//                    }
//                }
//            }
//        } else {
//            isFirst = false
//        }
//        val upsertionChanges = changes.filterIsInstance<UpsertionChange>()
//        val deletionChanges = changes.filterIsInstance<DeletionChange>()

        val now = LocalDateTime.now()
        val day = if (now.hour in 0..<4) {
            now.dayOfMonth - 1
        } else {
            now.dayOfMonth
        }
        val weightsStartDateTime = LocalDateTime.of(now.year, now.month, day, 4, 0)
        val weightsEndDateTime = LocalDateTime.of(now.year, now.month, day + 1, 4, 0)

        readWeightsByTimeRange(healthConnectClient, weightsStartDateTime, weightsEndDateTime/*, upsertionChanges, deletionChanges*/)

        val stepsStartDateTime = LocalDateTime.of(now.year, now.month, day, 0, 0)
        val stepsEndDateTime = LocalDateTime.of(now.year, now.month, day + 1, 0, 0)

//        val formatter = DateTimeFormatter.ofPattern(getString(R.string.datetime_pattern))
//        val formattedStartDateTime = stepsStartDateTime.format(formatter)
//        val formattedEndDateTime = stepsEndDateTime.format(formatter)
//        Log.i(TAG, """
//            StartDateTime: $formattedStartDateTime
//            EndDateTime: $formattedEndDateTime
//            """.trimIndent())

        aggregateSteps(healthConnectClient, stepsStartDateTime, stepsEndDateTime/*, upsertionChanges, deletionChanges*/)
    }

    private suspend fun readWeightsByTimeRange(
        healthConnectClient: HealthConnectClient,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
//        upsertionChanges: List<UpsertionChange>,
//        deletionChanges: List<DeletionChange>
    ) {
        val context = applicationContext
        try {
            val response =
                healthConnectClient.readRecords(
                    ReadRecordsRequest(
                        WeightRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                    )
                )
//            val records = response.records.filter { !deletionChanges.map{ it.recordId }.contains(it.metadata.id) }
//                .plus(upsertionChanges.map { it.record }.filterIsInstance<WeightRecord>())
//            Log.i(TAG, """
//                Records: ${response.records.count()} - ${deletionChanges.count()} = ${response.records.filter { !deletionChanges.map{ it.recordId }.contains(it.metadata.id) }.count()}
//                NewRecords: ${upsertionChanges.map { it.record }.filterIsInstance<WeightRecord>().count()}
//                """.trimIndent())
            for (weightRecord in response.records) {
                // Process each step record
                val ldt = LocalDateTime.ofInstant(weightRecord.time, ZoneId.systemDefault())
                val formatter = DateTimeFormatter.ofPattern(getString(R.string.datetime_pattern))
                val formattedDateTime = ldt.format(formatter)
                weights[formattedDateTime] = weightRecord.weight.inKilograms
            }
        } catch (e: Exception) {
            Toast.makeText(context, "${getString(R.string.cannot_get_info)} $e", Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun aggregateSteps(
        healthConnectClient: HealthConnectClient,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
//        upsertionChanges: List<UpsertionChange>,
//        deletionChanges: List<DeletionChange>
    ) {
        val context = applicationContext
        try {
            val response = healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            // The result may be null if no data is available in the time range
//            val records = upsertionChanges.map { it.record }.filterIsInstance<StepsRecord>().filter{ it.endTime < endTime.atZone(ZoneId.systemDefault()).toInstant()}
//            steps = (response[StepsRecord.COUNT_TOTAL] ?: 0) + records.sumOf { it.count }
            steps = response[StepsRecord.COUNT_TOTAL] ?: -1L
//            Log.i(TAG, "Steps: ${(response[StepsRecord.COUNT_TOTAL] ?: 0)} + ${records.sumOf { it.count }} = $steps")
//            if (steps == 0L) {
//                steps = -1L
//            }
        } catch (e: Exception) {
            Toast.makeText(context, "${getString(R.string.cannot_get_info)} $e", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun HealthRecordsViewer(weights: Map<String, Number>, steps: Long, context: Context?, modifier: Modifier) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 72.dp),
        modifier = modifier
    ) {
        item {
            Text(
                text = context?.getString(R.string.todays_records) ?: "todays records",
                style = MaterialTheme.typography.displayLarge
            )
            Column(
                modifier = Modifier
                    .padding(start = 20.dp, top = 48.dp, end = 20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = context?.getString(R.string.weights) ?: "weigths",
                    style = MaterialTheme.typography.headlineLarge
                )
                if (weights.isEmpty()) {
                    DescriptionItem(
                        key = context?.getString(R.string.weights) ?: "weights",
                        value = context?.getString(R.string.no_records) ?: "no records"
                    )
                } else {
                    weights.forEach {
                        with(it) {
                            DescriptionItem(
                                key = key,
                                value = "${"%.1f".format(value)} ${context?.getString(R.string.weights_num) ?: "weights num"}")
                        }
                    }
                }
                Text(
                    text = context?.getString(R.string.steps) ?: "steps",
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center
                )
                if (steps == -1L) {
                    DescriptionItem(
                        key = context?.getString(R.string.steps) ?: "steps",
                        value = context?.getString(R.string.no_records) ?: "no records"
                    )
                } else {
                    DescriptionItem(
                        key = context?.getString(R.string.steps) ?: "steps",
                        value = "$steps ${context?.getString(R.string.steps_num) ?: "steps num"}")
                }
            }
        }
    }
}

@Composable
fun DescriptionItem(key: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(vertical = 12.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun HealthRecordsViewerPreview() {
    val formatter = DateTimeFormatter.ofPattern("LLL d h:mm a")
    val formattedDateTime = LocalDateTime.now().format(formatter)
    HealthRecordsViewerTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("app name")
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Outlined.Settings, "open health connect")
                        }
                    }
                )
            },
            content = { innerPadding ->
                HealthRecordsViewer(
                    weights = mapOf("9:00" to 61.0, formattedDateTime to 60.0),
                    steps = 8000L,
                    context = null,
                    modifier = Modifier.padding(innerPadding)
                )
            },
//            floatingActionButton = {
//                ExtendedFloatingActionButton(
//                    onClick = {}
//                ) {
//                    Icon(Icons.Filled.Settings, getString(R.string.open_health_connect))
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Text(getString(R.string.open_health_connect))
//                }
//            },
//            floatingActionButtonPosition = FabPosition.Center
        )
    }
}