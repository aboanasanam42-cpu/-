package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class ScheduledMessageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val phone = intent.getStringExtra("recipientPhone").orEmpty()
        val text = intent.getStringExtra("messageText").orEmpty()
        val name = intent.getStringExtra("recipientName").orEmpty()
        val channel = intent.getStringExtra("channel").orEmpty()
        if (phone.isBlank() || text.isBlank()) return

        if (channel == "SMS" || channel == "كلاهما") {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                runCatching { SmsManager.getDefault().sendTextMessage(phone, null, text, null, null) }
            } else {
                postNotice(context, "تعذر إرسال SMS تلقائياً", "امنح التطبيق إذن SMS لإرسال الرسائل المجدولة.")
            }
        }

        if (channel == "واتساب" || channel == "كلاهما") {
            val encoded = Uri.encode(text)
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=${phone.replace("+", "")}&text=$encoded")
            val openWhatsApp = Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            val pending = PendingIntent.getActivity(
                context, (System.currentTimeMillis() % Int.MAX_VALUE).toInt(), openWhatsApp,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            postNotice(context, "رسالة واتساب مجدولة: $name", "اضغط لإكمال الإرسال في واتساب.", pending)
        }
    }

    private fun postNotice(context: Context, title: String, body: String, action: PendingIntent? = null) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "scheduled_messages"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(channelId, "الرسائل المجدولة", NotificationManager.IMPORTANCE_HIGH))
        }
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        action?.let { builder.setContentIntent(it) }
        if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), builder.build())
        }
    }
}
