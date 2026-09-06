package com.qopy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qopy.ui.theme.*

@Composable
fun PairingDialog(
    localCode: String,
    onDismiss: () -> Unit,
    onCopyCode: (String) -> Unit,
    onConfirmRemoteCode: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var remoteCodeInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pair New Device",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = IndigoPrimary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("My Code") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Enter Code") }
                    )
                }

                if (selectedTab == 0) {
                    Text(
                        text = "Enter this 8-character verification code on your other device:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    // Formatted Crockford Base32 Code (XXXX - XXXX)
                    val formattedCode = if (localCode.length == 8) {
                        "${localCode.take(4)} - ${localCode.takeLast(4)}"
                    } else {
                        localCode
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formattedCode,
                            modifier = Modifier.padding(vertical = 20.dp, horizontal = 12.dp),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center,
                            letterSpacing = 4.sp
                        )
                    }

                    Button(
                        onClick = { onCopyCode(localCode) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                    ) {
                        Text("Copy Code to Clipboard")
                    }
                } else {
                    Text(
                        text = "Enter the 8-character code shown on your other device:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = remoteCodeInput,
                        onValueChange = { input ->
                            if (input.length <= 9) {
                                remoteCodeInput = input.uppercase().filter { it.isLetterOrDigit() || it == '-' }
                            }
                        },
                        label = { Text("Pairing Code") },
                        placeholder = { Text("e.g. K9X2-7M4P") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    val cleanInput = remoteCodeInput.replace("-", "")
                    Button(
                        onClick = { onConfirmRemoteCode(cleanInput) },
                        enabled = cleanInput.length == 8,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                    ) {
                        Text("Verify & Pair")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
