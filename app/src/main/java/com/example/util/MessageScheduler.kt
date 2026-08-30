package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Locale

object MessageScheduler {
    private const val ACTION_SEND = "com.example.medicalaccounting.SEND_SCHEDULED_MESSAGE"

    fun schedule(context: Context, message: com.example.data.local.ScheduledMessage) {
        val triggerAt = runCatching {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .parse("${message.scheduledDate} ${message.scheduledTime}")?.time
        }.getOrNull() ?: return
        if (triggerAt <= System.currentTimeMillis()) return

        val intent = Intent(context, ScheduledMessageReceiver::class.java).apply {
            action = ACTION_SEND
            putExtra("recipientName", message.recipientName)
            putExtra("recipientPhone", message.recipientPhone)
            putExtra("messageText", message.messageText)
            putExtra("channel", message.channel)
        }
        val requestCode = message.id.toInt().takeIf { it != 0 } ?: (triggerAt % Int.MAX_VALUE).toInt()
        val pending = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
    }

    fun cancel(context: Context, message: com.example.data.local.ScheduledMessage) {
        val requestCode = message.id.toInt()
        if (requestCode == 0) return
        val intent = Intent(context, ScheduledMessageReceiver::class.java).apply { action = ACTION_SEND }
        val pending = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending)
    }
}
