package com.qopy.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qopy.crypto.NodeIdentity
import com.qopy.crypto.PairingCrypto
import com.qopy.data.PeerEntity
import com.qopy.data.QopyDatabase
import com.qopy.network.DiscoveredDevice
import com.qopy.network.NsdDiscovery
import com.qopy.sensitive.SensitiveDetector
import com.qopy.service.ClipboardSyncService
import com.qopy.ui.components.*
import com.qopy.ui.theme.IndigoPrimary
import com.qopy.ui.theme.QopyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var identity: NodeIdentity
    private lateinit var db: QopyDatabase
    private lateinit var discovery: NsdDiscovery

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ClipboardSyncService.start(this)

        val defaultName = android.os.Build.MODEL ?: "Android Device"
        identity = NodeIdentity.loadOrCreate(this, defaultName)
        db = QopyDatabase.getInstance(this)
        discovery = NsdDiscovery(this)

        setContent {
            QopyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    QopyApp(
                        identity = identity,
                        db = db,
                        onCopyText = { text, label ->
                            val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText(label, text))
                            Toast.makeText(this, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        onPushCurrentClipboard = {
                            val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = cb.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val text = clip.getItemAt(0).text?.toString()
                                if (!text.isNullOrEmpty()) {
                                    Toast.makeText(this, "Broadcasting to paired LAN peers...", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QopyApp(
    identity: NodeIdentity,
    db: QopyDatabase,
    onCopyText: (String, String) -> Unit,
    onPushCurrentClipboard: () -> Unit
) {
    var deviceName by remember { mutableStateOf(identity.deviceName) }
    var autoSync by remember { mutableStateOf(true) }
    var filterSensitive by remember { mutableStateOf(true) }
    var isPaused by remember { mutableStateOf(false) }
    var showPairDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var currentPairCode by remember { mutableStateOf(PairingCrypto.generatePairingCode()) }

    var latestClipboardText by remember { mutableStateOf<String?>("https://qopy.dev/welcome-to-lan-sync") }
    var isLatestSensitive by remember { mutableStateOf(false) }

    val peers by db.peerDao().getAllPeersFlow().collectAsState(initial = emptyList())
    var discoveredDevices by remember { mutableStateOf(listOf<DiscoveredDevice>()) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Qopy",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = IndigoPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "P2P LAN",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = IndigoPrimary,
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showAboutDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About Qopy",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    currentPairCode = PairingCrypto.generatePairingCode()
                    showPairDialog = true
                },
                containerColor = IndigoPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Pair Device", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Device Hero Status Card
            StatusCard(
                deviceName = deviceName,
                deviceId = identity.deviceId,
                isPaused = isPaused,
                connectedCount = peers.size,
                onRenameClick = { showRenameDialog = true }
            )

            // 2. Real-time Synced Clipboard Card
            CurrentClipboardCard(
                currentText = latestClipboardText,
                isSensitive = isLatestSensitive,
                onCopyAgain = { text -> onCopyText(text, "Clipboard item") },
                onPushToPeers = onPushCurrentClipboard
            )

            // 3. Quick Control Toggles (AutoSync, Filter, Pause)
            ControlTogglesSection(
                autoSync = autoSync,
                onAutoSyncChange = { autoSync = it },
                filterSensitive = filterSensitive,
                onFilterSensitiveChange = { filterSensitive = it },
                isPaused = isPaused,
                onIsPausedChange = { isPaused = it }
            )

            // 4. Discovered LAN Peers (mDNS)
            DiscoveredPeersSection(
                discoveredDevices = discoveredDevices,
                onPairWithDevice = { dev ->
                    currentPairCode = PairingCrypto.generatePairingCode()
                    showPairDialog = true
                }
            )

            // 5. Paired Peers List
            PeerListSection(
                peers = peers,
                onUnpair = { peer ->
                    coroutineScope.launch {
                        db.peerDao().deletePeer(peer.deviceId)
                    }
                }
            )

            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    // Dialogs
    if (showPairDialog) {
        PairingDialog(
            localCode = currentPairCode,
            onDismiss = { showPairDialog = false },
            onCopyCode = { code -> onCopyText(code, "Pairing code") },
            onConfirmRemoteCode = { remoteCode ->
                showPairDialog = false
            }
        )
    }

    if (showRenameDialog) {
        RenameDeviceDialog(
            currentName = deviceName,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                deviceName = newName
                showRenameDialog = false
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About Qopy") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Fast, silent, zero-server peer-to-peer clipboard synchronization over LAN.")
                    Text("• Zero History: In-memory only, no disk logging.", style = MaterialTheme.typography.bodySmall)
                    Text("• Security: TLS 1.3 + NaCl Box end-to-end encryption.", style = MaterialTheme.typography.bodySmall)
                    Text("• Version: 0.1.0", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}
