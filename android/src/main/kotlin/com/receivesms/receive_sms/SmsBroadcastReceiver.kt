package com.receivesms.receive_sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import io.flutter.plugin.common.EventChannel

class SmsBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        messages.forEach { msg ->
            val data = mapOf(
                "address" to (msg.originatingAddress ?: ""),
                "body" to (msg.messageBody ?: ""),
                "timestamp" to msg.timestampMillis.toString()
            )
            eventSink?.success(data)
        }
    }

    companion object {
        var eventSink: EventChannel.EventSink? = null
    }
}
