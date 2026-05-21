package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MessageEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(viewModel: MainViewModel, onOpenSettings: () -> Unit) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val activeProvider by viewModel.activeProvider.collectAsStateWithLifecycle()
    
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ChatHeader(activeProvider?.provider ?: "No Active Key", onOpenSettings) },
        bottomBar = { 
            ChatInput(
                value = inputText,
                onValueChange = { inputText = it },
                onSend = {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                },
                isGenerating = isGenerating
            )
        }
    ) { padding ->
        if (messages.isEmpty()) {
            EmptyChatState(modifier = Modifier.padding(padding).fillMaxSize())
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(16.dp)) }
                items(messages) { msg ->
                    ChatBubble(msg)
                }
                if (isGenerating) {
                    item {
                        GeneratingIndicator()
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun ChatHeader(providerName: String, onOpenSettings: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline) // Neobrutalist top border
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentBlue)
                    .border(2.dp, AccentCyan, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Y", color = AccentCyan, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Y-HAI", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("Hyperagent Intelligence", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            
            // Provider Info Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clickable { onOpenSettings() }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (providerName.contains("No")) Color.Red else AccentLime))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(providerName, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: MessageEntity) {
    val isUser = msg.role == "user"
    val bgColor = if (isUser) UserBubble else ModelBubble
    val align = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (isUser) RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    val borderColor = if (isUser) AccentBlue else OutlineColor

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = align) {
        Row(verticalAlignment = Alignment.Bottom) {
            if (!isUser) {
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(AccentCyan), contentAlignment = Alignment.Center) {
                    Text("Y", color = DarkBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(shape)
                    .background(bgColor)
                    .border(1.dp, borderColor, shape)
                    .padding(14.dp)
            ) {
                Text(text = msg.text, color = TextPrimary)
            }
        }
    }
}

@Composable
fun ChatInput(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit, isGenerating: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(28.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message Y-HAI...", color = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                maxLines = 4
            )
            IconButton(
                onClick = onSend,
                enabled = value.isNotBlank() && !isGenerating,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (value.isNotBlank() && !isGenerating) AccentCyan else MaterialTheme.colorScheme.outline)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = DarkBackground, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun GeneratingIndicator() {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.CenterStart) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(AccentCyan), contentAlignment = Alignment.Center) {
                Text("Y", color = DarkBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("Thinking...", color = TextSecondary)
        }
    }
}

@Composable
fun EmptyChatState(modifier: Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .border(2.dp, AccentCyan.copy(alpha = 0.3f), CircleShape)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(AccentCyan),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(AccentBlue))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "How can I assist you today?",
            style = MaterialTheme.typography.titleLarge,
            fontStyle = FontStyle.Italic,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Access Gemini, Groq, and custom APIs\nin one secure intelligence hub.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
