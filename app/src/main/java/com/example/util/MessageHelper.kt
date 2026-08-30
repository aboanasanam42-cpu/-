package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

object MessageHelper {

    fun sendWhatsAppMessage(context: Context, phoneNumber: String, messageText: String) {
        try {
            // Clean phone number: remove non-digits, keep '+' if present or format for Yemen / local
            var cleanPhone = phoneNumber.replace(Regex("[^0-9+]"), "")
            if (!cleanPhone.startsWith("+") && !cleanPhone.startsWith("00")) {
                if (cleanPhone.length == 9 && (cleanPhone.startsWith("7") || cleanPhone.startsWith("1"))) {
                    cleanPhone = "967$cleanPhone"
                }
            } else if (cleanPhone.startsWith("+")) {
                cleanPhone = cleanPhone.substring(1)
            }

            val encodedMessage = URLEncoder.encode(messageText, "UTF-8")
            val url = if (cleanPhone.isNotEmpty()) {
                "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage"
            } else {
                "https://api.whatsapp.com/send?text=$encodedMessage"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق واتساب: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun sendSmsMessage(context: Context, phoneNumber: String, messageText: String) {
        try {
            val uri = Uri.parse("smsto:${phoneNumber.trim()}")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", messageText)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق الرسائل القصيرة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun makePhoneCall(context: Context, phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${phoneNumber.trim()}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق الهاتف: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
