package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ApiKeyEntity
import com.example.data.MessageEntity
import com.example.ui.theme.*

@Composable
fun MainScreen(viewModel: MainViewModel) {
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        SettingsScreen(viewModel, onBack = { showSettings = false })
    } else {
        ChatScreen(viewModel, onOpenSettings = { showSettings = true })
    }
}

@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val keys by viewModel.apiKeys.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack) {
                        Text("← Back", color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text("API Keys Vault", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { showDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Key", tint = AccentCyan)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(keys) { key ->
                ApiKeyCard(
                    apiKey = key,
                    onDelete = { viewModel.deleteApiKey(key.provider) },
                    onActivate = { viewModel.setActiveProvider(key.provider) }
                )
            }
            if (keys.isEmpty()) {
                item {
                    Text(
                        "No API keys configured.\nAdd a Gemini or Groq key to get started.",
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp)
                    )
                }
            }
        }
        
        if (showDialog) {
            AddKeyDialog(
                onDismiss = { showDialog = false },
                onAdd = { provider, key ->
                    viewModel.addApiKey(provider, key)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun ApiKeyCard(apiKey: ApiKeyEntity, onDelete: () -> Unit, onActivate: () -> Unit) {
    val borderColor = if (apiKey.isActive) AccentCyan else MaterialTheme.colorScheme.outline
    val bgColor = if (apiKey.isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onActivate() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = AccentLime)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(apiKey.provider, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Key: ${apiKey.apiKey.take(4)}...${apiKey.apiKey.takeLast(4)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            if (apiKey.isActive) {
                Badge(containerColor = AccentBlue, contentColor = AccentCyan) { Text("ACTIVE") }
                Spacer(modifier = Modifier.width(8.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}

@Composable
fun AddKeyDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var provider by remember { mutableStateOf("Groq") }
    var key by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Add API Key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = provider == "Groq",
                        onClick = { provider = "Groq" },
                        label = { Text("Groq Cloud") }
                    )
                    FilterChip(
                        selected = provider == "Gemini",
                        onClick = { provider = "Gemini" },
                        label = { Text("Gemini") }
                    )
                }
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (key.isNotBlank()) onAdd(provider, key) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}
