package com.example.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.ChatMessageEntity
import com.example.data.entity.ConversationEntity
import com.example.ui.components.ChatBubble
import com.example.ui.components.PerformanceHud
import com.example.ui.components.ThermalBadge
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.RedSevere
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToModels: () -> Unit,
    modifier: Modifier = Modifier
) {
    val models by viewModel.allModels.collectAsStateWithLifecycle()
    val selectedModelId by viewModel.selectedModelId.collectAsStateWithLifecycle()
    val conversations by viewModel.allConversations.collectAsStateWithLifecycle()
    val activeConversationId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val liveUpdate by viewModel.liveTokenUpdate.collectAsStateWithLifecycle()
    val thermalState by viewModel.thermalGovernor.thermalState.collectAsStateWithLifecycle()
    val config by viewModel.inferenceConfig.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var conversationToRename by remember { mutableStateOf<ConversationEntity?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var showClearAllConfirmDialog by remember { mutableStateOf(false) }
    var conversationToDelete by remember { mutableStateOf<ConversationEntity?>(null) }

    val currentModel = models.find { it.id == selectedModelId } ?: models.firstOrNull()

    // Auto-scroll on new messages or streaming tokens
    LaunchedEffect(messages.size, liveUpdate?.accumulatedText?.length) {
        val totalItems = messages.size + if (isGenerating) 1 else 0
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    // Rename Conversation Dialog
    if (conversationToRename != null) {
        AlertDialog(
            onDismissRequest = { conversationToRename = null },
            title = {
                Text(
                    text = "Rename Conversation",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a title for this chat:",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate400)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = renameInputText,
                        onValueChange = { renameInputText = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = Slate700
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rename_chat_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        conversationToRename?.let {
                            viewModel.renameConversation(it.id, renameInputText)
                        }
                        conversationToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanNeon,
                        contentColor = Slate950
                    )
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToRename = null }) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }

    // Delete Single Conversation Confirmation Dialog
    if (conversationToDelete != null) {
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            title = {
                Text(
                    text = "Delete Chat Session?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${conversationToDelete?.title}\"? All messages in this session will be permanently removed.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Slate400)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        conversationToDelete?.let { viewModel.deleteConversation(it.id) }
                        conversationToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedSevere,
                        contentColor = Color.White
                    )
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToDelete = null }) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }

    // Clear All Confirmation Dialog
    if (showClearAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirmDialog = false },
            title = {
                Text(
                    text = "Clear All Chat History?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Text(
                    text = "This will permanently delete all chat conversations and messages from local storage.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Slate400)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllConversations()
                        showClearAllConfirmDialog = false
                        scope.launch { drawerState.close() }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedSevere,
                        contentColor = Color.White
                    )
                ) {
                    Text("Clear All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirmDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Slate950,
                modifier = Modifier.width(320.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Drawer Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Chat History",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${conversations.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Slate400,
                                    fontWeight = FontWeight.Normal
                                )
                            )
                        }

                        IconButton(
                            onClick = { scope.launch { drawerState.close() } },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Drawer",
                                tint = Slate400
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // + New Chat Button
                    Button(
                        onClick = {
                            viewModel.createNewConversation("New Chat")
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drawer_new_chat_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanNeon,
                            contentColor = Slate950
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Start New Chat",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Slate800, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Conversations List
                    if (conversations.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No chat history yet.\nStart a new conversation!",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Slate400,
                                    lineHeight = 18.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(conversations, key = { it.id }) { conv ->
                                val isActive = conv.id == activeConversationId
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            width = if (isActive) 1.5.dp else 1.dp,
                                            color = if (isActive) CyanNeon else Slate800,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            viewModel.selectConversation(conv.id)
                                            scope.launch { drawerState.close() }
                                        }
                                        .testTag("conversation_item_${conv.id}"),
                                    color = if (isActive) CyanNeon.copy(alpha = 0.12f) else Slate900
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = conv.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = if (isActive) CyanNeon else Color.White,
                                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = formatTimestamp(conv.updatedAt),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Slate400,
                                                    fontSize = 10.sp
                                                )
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    conversationToRename = conv
                                                    renameInputText = conv.title
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Rename",
                                                    tint = Slate400,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { conversationToDelete = conv },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = RedSevere.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Drawer Footer: Clear All History
                    if (conversations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Slate800, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { showClearAllConfirmDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("clear_all_history_btn"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedSevere),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RedSevere.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear All Chats", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Slate950,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("chat_history_drawer_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Chat History",
                                tint = Color.White
                            )
                        }
                    },
                    title = {
                        // Model selector in top bar
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Slate850)
                                    .clickable { modelDropdownExpanded = true }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = null,
                                    tint = CyanNeon,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentModel?.name ?: "Select Model",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select",
                                    tint = Slate400,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = modelDropdownExpanded,
                                onDismissRequest = { modelDropdownExpanded = false },
                                modifier = Modifier.background(Slate900)
                            ) {
                                models.forEach { m ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = m.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        color = if (m.id == currentModel?.id) CyanNeon else Color.White,
                                                        fontWeight = if (m.id == currentModel?.id) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                )
                                                Text(
                                                    text = "${m.architecture.uppercase()} • ${m.paramCount} • ${m.quantType}",
                                                    style = MaterialTheme.typography.labelSmall.copy(color = Slate400)
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectModel(m.id)
                                            modelDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        val isFlash = config.performanceProfile == com.example.core.inference.PerformanceProfile.FLASH_TURBO
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isFlash) CyanNeon.copy(alpha = 0.2f) else Slate800)
                                .border(1.dp, if (isFlash) CyanNeon else Slate700, RoundedCornerShape(8.dp))
                                .clickable { viewModel.toggleFlashSpeed() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("flash_speed_toggle"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isFlash) "⚡ FLASH" else "🚀 HIGH PERF",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isFlash) CyanNeon else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        ThermalBadge(thermalState = thermalState)
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { viewModel.createNewConversation("New Chat") },
                            modifier = Modifier.testTag("new_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Chat",
                                tint = CyanNeon
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate950)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Live Real-Time Performance HUD
                PerformanceHud(
                    tokenUpdate = liveUpdate,
                    isGenerating = isGenerating
                )

                // Message stream
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
                ) {
                    // Empty state greeting if no messages
                    if (messages.isEmpty() && !isGenerating) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(CyanNeon.copy(alpha = 0.15f))
                                        .border(1.dp, CyanNeon.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Memory,
                                        contentDescription = null,
                                        tint = CyanNeon,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = currentModel?.name ?: "On-Device LLM Runner",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "100% Offline • Sandboxed Execution • Zero Cloud Latency",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Slate400)
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = "SUGGESTED QUERIES",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = CyanNeon,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                QuickPromptButton("Explain attention mechanisms in transformers") {
                                    viewModel.sendMessage(it)
                                }
                                QuickPromptButton("Write an optimized SIMD vector dot-product in Kotlin") {
                                    viewModel.sendMessage(it)
                                }
                                QuickPromptButton("Why do on-device LLMs throttle and how does PID help?") {
                                    viewModel.sendMessage(it)
                                }
                            }
                        }
                    }

                    // Render saved messages
                    items(messages, key = { it.id }) { msg ->
                        ChatBubble(message = msg)
                    }

                    // Live generation bubble
                    if (isGenerating && liveUpdate != null) {
                        item {
                            ChatBubble(
                                message = ChatMessageEntity(
                                    id = -1L,
                                    conversationId = activeConversationId ?: 0L,
                                    role = "ASSISTANT",
                                    content = liveUpdate?.accumulatedText.orEmpty().ifEmpty { "Generating tokens..." },
                                    decodeTokPerSec = liveUpdate?.currentTokensPerSec ?: 0.0,
                                    timeToFirstTokenMs = liveUpdate?.timeToFirstTokenMs ?: 0L
                                )
                            )
                        }
                    }
                }

                // Input Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Slate900,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    text = if (isGenerating) "Generating response..." else "Ask ${currentModel?.name ?: "LLM"}...",
                                    color = Slate400,
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input_field"),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Slate850,
                                unfocusedContainerColor = Slate850,
                                focusedBorderColor = CyanNeon,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (inputText.isNotBlank() && !isGenerating) {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                }
                            }),
                            enabled = !isGenerating
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        if (isGenerating) {
                            IconButton(
                                onClick = { viewModel.stopGeneration() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(RedSevere)
                                    .testTag("stop_generation_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop",
                                    tint = Color.White
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        viewModel.sendMessage(inputText)
                                        inputText = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (inputText.isNotBlank()) CyanNeon else Slate800)
                                    .testTag("send_message_btn"),
                                enabled = inputText.isNotBlank()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = if (inputText.isNotBlank()) Slate950 else Slate400
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickPromptButton(prompt: String, onClick: (String) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Slate700, RoundedCornerShape(12.dp))
            .clickable { onClick(prompt) },
        color = Slate850
    ) {
        Text(
            text = prompt,
            style = MaterialTheme.typography.bodySmall.copy(color = Slate400),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val formatTime = SimpleDateFormat("h:mm a", Locale.getDefault())
    val formatDate = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return when {
        diff < 60_000L -> "Just now"
        diff < 86_400_000L -> "Today, ${formatTime.format(Date(timestamp))}"
        diff < 172_800_000L -> "Yesterday, ${formatTime.format(Date(timestamp))}"
        else -> formatDate.format(Date(timestamp))
    }
}
