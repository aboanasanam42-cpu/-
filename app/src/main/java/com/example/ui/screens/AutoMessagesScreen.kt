package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ScheduledMessage
import com.example.ui.components.AppHeaderBanner
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MedicalViewModel
import com.example.util.MessageHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoMessagesScreen(
    viewModel: MedicalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messages by viewModel.allScheduledMessages.collectAsStateWithLifecycle()
    var showScheduleDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppHeaderBanner(
                title = "الرسائل التلقائية للمرضى والمرتبطين بالعمل",
                showBack = true,
                onBack = { viewModel.navigateTo(AppScreen.DASHBOARD) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showScheduleDialog = true },
                containerColor = Color(0xFFEA580C),
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_message")
            ) {
                Icon(Icons.Default.AddComment, contentDescription = "إنشاء رسالة تلقائية جديدة")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SurfaceBackground)
                .padding(horizontal = 12.dp)
        ) {
            // Top Overview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .shadow(3.dp, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFC2410C), Color(0xFFEA580C))
                            )
                        )
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("الرسائل التلقائية المجدولة", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        Text("${messages.size} رسالة", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { showScheduleDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("رسالة جديدة", color = Color(0xFFEA580C), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد رسائل تلقائية مجدولة حالياً", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { msg ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = msg.recipientName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MedicalNavy
                                        )
                                        Text(
                                            text = "الهاتف: ${msg.recipientPhone} | القناة: ${msg.channel}",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    Badge(
                                        containerColor = if (msg.status == "تم الإرسال") Color(0xFFDCFCE7) else Color(0xFFFFEDD5),
                                        contentColor = if (msg.status == "تم الإرسال") Color(0xFF15803D) else Color(0xFFC2410C)
                                    ) {
                                        Text(msg.status, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = msg.messageText,
                                        fontSize = 12.5.sp,
                                        color = TextPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("الموعد: ${msg.scheduledDate} (${msg.scheduledTime})", fontSize = 11.5.sp, color = TextMuted)

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // Send button
                                        Button(
                                            onClick = {
                                                if (msg.channel == "واتساب") {
                                                    MessageHelper.sendWhatsAppMessage(context, msg.recipientPhone, msg.messageText)
                                                } else {
                                                    MessageHelper.sendSmsMessage(context, msg.recipientPhone, msg.messageText)
                                                }
                                                viewModel.updateScheduledMessageStatus(msg, "تم الإرسال")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (msg.channel == "واتساب") Color(0xFF25D366) else Color(0xFF2563EB)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(if (msg.channel == "واتساب") Icons.Default.Chat else Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("إرسال الآن", fontSize = 11.sp)
                                        }

                                        // Delete button
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteScheduledMessage(msg)
                                                Toast.makeText(context, "تم حذف الرسالة", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showScheduleDialog) {
        AddScheduleMessageDialog(
            onDismiss = { showScheduleDialog = false },
            onSave = { newMsg ->
                viewModel.addScheduledMessage(newMsg)
                showScheduleDialog = false
                Toast.makeText(context, "تمت جدولة الرسالة بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun AddScheduleMessageDialog(
    onDismiss: () -> Unit,
    onSave: (ScheduledMessage) -> Unit
) {
    var recipientName by remember { mutableStateOf("") }
    var recipientPhone by remember { mutableStateOf("") }
    var messageType by remember { mutableStateOf("تذكير موعد مراجعة") }
    var channel by remember { mutableStateOf("واتساب") }
    var messageText by remember { mutableStateOf("عزيزنا المريض نود تذكيركم بموعد مراجعتكم في العيادة غداً. نتمنى لكم دوام الصحة والعافية.") }

    val templates = listOf(
        "تذكير موعد مراجعة" to "عزيزنا المريض نود تذكيركم بموعد مراجعتكم في العيادة غداً. نتمنى لكم دوام الصحة والعافية.",
        "تذكير متابعة علاج" to "عزيزنا المريض نرجو الالتزام بجرعات العلاج الموصوفة والمتابعة الدورية معنا.",
        "إشعار كشف حساب ومتبقي" to "عزيزنا المريض يرجى التكرم بتسديد المبلغ المتبقي على حسابكم، شكراً لتعاونكم.",
        "إشعار للكادر الطبي" to "الزملاء الأعزاء في الكادر الطبي، نود إعلامكم بالجدول والمواعيد المقررة ليوم الغد."
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("إنشاء رسالة تلقائية / مجدولة", fontWeight = FontWeight.Bold, color = Color(0xFFEA580C), fontSize = 16.sp)
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = recipientName,
                        onValueChange = { recipientName = it },
                        label = { Text("اسم المستلم (المريض أو الموظف) *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = recipientPhone,
                        onValueChange = { recipientPhone = it },
                        label = { Text("رقم الهاتف *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Text("قناة الإرسال:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = channel == "واتساب",
                            onClick = { channel = "واتساب" },
                            label = { Text("واتساب") }
                        )
                        FilterChip(
                            selected = channel == "SMS",
                            onClick = { channel = "SMS" },
                            label = { Text("رسالة نصية SMS") }
                        )
                    }
                }
                item {
                    Text("اختر قالب الرسالة:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        templates.forEach { (type, text) ->
                            FilterChip(
                                selected = messageType == type,
                                onClick = {
                                    messageType = type
                                    messageText = text
                                },
                                label = { Text(type, fontSize = 11.sp) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        label = { Text("نص الرسالة *") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (recipientName.isNotBlank() && recipientPhone.isNotBlank()) {
                        val msg = ScheduledMessage(
                            recipientName = recipientName.trim(),
                            recipientPhone = recipientPhone.trim(),
                            messageText = messageText.trim(),
                            scheduledDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                            scheduledTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                            messageType = messageType,
                            channel = channel,
                            status = "مجدولة"
                        )
                        onSave(msg)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
            ) {
                Text("حفظ وجدولة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color.Gray)
            }
        }
    )
}
