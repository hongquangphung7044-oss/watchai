package com.example.watchai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watchai.data.ConversationSnapshot
import com.example.watchai.viewmodel.ChatViewModel
import com.example.watchai.viewmodel.Screen
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(viewModel: ChatViewModel) {
    val history by viewModel.history.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1520))
            .padding(horizontal = 24.dp)
            .padding(top = 26.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.CHAT) }, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(17.dp))
            }
            Text("历史对话", color = Color.White, fontSize = 13.sp)
            Spacer(Modifier.size(30.dp))
        }

        Spacer(Modifier.height(8.dp))

        if (history.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📭", fontSize = 24.sp)
                    Text("暂无历史对话", color = Color(0xFF4A5568), fontSize = 12.sp,
                        textAlign = TextAlign.Center)
                    Text("新建对话时自动保存", color = Color(0xFF2D3748), fontSize = 10.sp,
                        textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(history, key = { it.id }) { snapshot ->
                    HistoryItem(
                        snapshot = snapshot,
                        onClick  = { viewModel.restoreConversation(snapshot) },
                        onDelete = { viewModel.deleteConversation(snapshot.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(
    snapshot: ConversationSnapshot,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val timeStr = remember(snapshot.id) {
        SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(snapshot.id))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF141F2E))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Chat, null, tint = Color(0xFF2B6CB0), modifier = Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(snapshot.title, color = Color(0xFFCBD5E0), fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$timeStr · ${snapshot.messages.size}条",
                color = Color(0xFF4A5568), fontSize = 9.sp)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Delete, null, tint = Color(0xFF4A5568), modifier = Modifier.size(13.dp))
        }
    }
}
