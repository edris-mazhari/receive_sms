package com.receivesms.receive_sms

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.core.app.ActivityCompat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class ReceiveSmsPlugin : FlutterPlugin, ActivityAware {
    private var permissionResult: MethodChannel.Result? = null
    private var sendSmsResult: MethodChannel.Result? = null
    private var sendSmsAddress: String? = null
    private var sendSmsBody: String? = null
    private var activityBinding: ActivityPluginBinding? = null
    private var hasRequestedPermission = false
    private var sentReceiver: BroadcastReceiver? = null

    companion object {
        private const val SMS_EVENTS_CHANNEL = "com.greenhouse.greenhouse/sms_events"
        private const val PERMISSION_CHANNEL = "com.greenhouse.greenhouse/permission"
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val SEND_SMS_REQUEST_CODE = 1002
        private const val SMS_SENT_ACTION = "com.receivesms.receive_sms.SMS_SENT"
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        EventChannel(
            flutterPluginBinding.binaryMessenger,
            SMS_EVENTS_CHANNEL
        ).setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
                SmsBroadcastReceiver.eventSink = events
            }

            override fun onCancel(arguments: Any?) {
                SmsBroadcastReceiver.eventSink = null
            }
        })

        MethodChannel(
            flutterPluginBinding.binaryMessenger,
            PERMISSION_CHANNEL
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "requestSmsPermission" -> requestSmsPermission(result)
                "openAppSettings" -> openAppSettings(result)
                "canRequestPermission" -> canRequestPermission(result)
                "sendSms" -> sendSms(call, result)
                "openSmsApp" -> openSmsApp(call, result)
                "isDefaultSmsApp" -> isDefaultSmsApp(result)
                "requestDefaultSmsApp" -> requestDefaultSmsApp(result)
                else -> result.notImplemented()
            }
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        SmsBroadcastReceiver.eventSink = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activityBinding = binding
        binding.addRequestPermissionsResultListener { requestCode, permissions, grantResults ->
            when (requestCode) {
                PERMISSION_REQUEST_CODE -> {
                    handlePermissionResult(grantResults)
                    true
                }
                SEND_SMS_REQUEST_CODE -> {
                    handleSendSmsPermissionResult(grantResults)
                    true
                }
                else -> false
            }
        }
    }

    override fun onDetachedFromActivity() {
        activityBinding = null
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    private fun handlePermissionResult(grantResults: IntArray) {
        hasRequestedPermission = true
        val granted = grantResults.isNotEmpty() &&
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }

        val activity = activityBinding?.activity
        val canRequest = when {
            granted -> true
            activity == null -> true
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M -> true
            !hasRequestedPermission -> true
            !activity.shouldShowRequestPermissionRationale(Manifest.permission.RECEIVE_SMS) -> false
            else -> true
        }

        permissionResult?.success(
            mapOf("granted" to granted, "canRequest" to canRequest)
        )
        permissionResult = null
    }

    private fun handleSendSmsPermissionResult(grantResults: IntArray) {
        val activity = activityBinding?.activity
        val granted = when {
            activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M ->
                grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            else ->
                grantResults.isNotEmpty() &&
                        grantResults.all { it == PackageManager.PERMISSION_GRANTED } &&
                        activity.checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        }
        if (granted) {
            sendSmsInternal(sendSmsAddress ?: "", sendSmsBody ?: "", sendSmsResult)
        } else {
            sendSmsResult?.success(
                mapOf("success" to false, "error" to "Permission denied")
            )
        }
        sendSmsResult = null
        sendSmsAddress = null
        sendSmsBody = null
    }

    private fun requestSmsPermission(result: MethodChannel.Result) {
        val activity = activityBinding?.activity ?: run {
            result.success(mapOf("granted" to false, "canRequest" to true))
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            result.success(mapOf("granted" to true, "canRequest" to true))
            return
        }
        if (activity.checkSelfPermission(Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            result.success(mapOf("granted" to true, "canRequest" to true))
            return
        }

        if (hasRequestedPermission &&
            !activity.shouldShowRequestPermissionRationale(Manifest.permission.RECEIVE_SMS)
        ) {
            result.success(mapOf("granted" to false, "canRequest" to false))
            return
        }

        permissionResult = result
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.RECEIVE_SMS),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun canRequestPermission(result: MethodChannel.Result) {
        val activity = activityBinding?.activity

        if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            result.success(true)
            return
        }

        if (activity.checkSelfPermission(Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            result.success(true)
            return
        }

        if (!hasRequestedPermission) {
            result.success(true)
            return
        }

        result.success(activity.shouldShowRequestPermissionRationale(Manifest.permission.RECEIVE_SMS))
    }

    private fun openAppSettings(result: MethodChannel.Result) {
        try {
            val activity = activityBinding?.activity ?: run {
                result.success(false)
                return
            }
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivity(intent)
            result.success(true)
        } catch (e: Exception) {
            result.success(false)
        }
    }

    private fun isDefaultSmsApp(result: MethodChannel.Result) {
        val context = activityBinding?.activity ?: run {
            result.success(false)
            return
        }
        val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(context)
        result.success(defaultSmsPackage == context.packageName)
    }

    private fun requestDefaultSmsApp(result: MethodChannel.Result) {
        val activity = activityBinding?.activity ?: run {
            result.success(false)
            return
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = activity.getSystemService(android.app.role.RoleManager::class.java)
                val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_SMS)
                activity.startActivity(intent)
            } else {
                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                    putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, activity.packageName)
                }
                activity.startActivity(intent)
            }
            result.success(true)
        } catch (e: Exception) {
            result.success(false)
        }
    }

    private fun sendSms(call: MethodCall, result: MethodChannel.Result) {
        val address = call.argument<String>("address") ?: ""
        val body = call.argument<String>("body") ?: ""

        val activity = activityBinding?.activity
        if (activity == null) {
            result.success(mapOf("success" to false, "error" to "No available activity"))
            return
        }

        if (address.isBlank()) {
            result.success(mapOf("success" to false, "error" to "Address cannot be empty"))
            return
        }

        if (body.isBlank()) {
            result.success(mapOf("success" to false, "error" to "Message body cannot be empty"))
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            activity.checkSelfPermission(Manifest.permission.SEND_SMS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            sendSmsResult = result
            sendSmsAddress = address
            sendSmsBody = body
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.SEND_SMS),
                SEND_SMS_REQUEST_CODE
            )
            return
        }

        sendSmsInternal(address, body, result)
    }

    private fun getSmsManager(context: Context): SmsManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
    }

    private fun sendSmsInternal(
        address: String,
        body: String,
        result: MethodChannel.Result?
    ) {
        if (result == null) return
        val activity = activityBinding?.activity
        val context = activity ?: return

        try {
            val smsManager = getSmsManager(context)
            val sentIntent = Intent(SMS_SENT_ACTION)
            val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                address.hashCode(),
                sentIntent,
                pendingFlags
            )

            sentReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    when (resultCode) {
                        Activity.RESULT_OK -> {
                            result.success(mapOf("success" to true))
                        }
                        SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                            val noDefault = intent.getBooleanExtra("noDefault", false)
                            if (noDefault) {
                                result.success(
                                    mapOf("success" to false, "error" to "No default SMS app is set on this device.")
                                )
                            } else {
                                openSmsAppInternal(address, body, result)
                            }
                        }
                        SmsManager.RESULT_ERROR_NO_SERVICE -> {
                            result.success(mapOf("success" to false, "error" to "No SMS service available"))
                        }
                        SmsManager.RESULT_ERROR_RADIO_OFF -> {
                            result.success(mapOf("success" to false, "error" to "Radio is off"))
                        }
                        SmsManager.RESULT_ERROR_NULL_PDU -> {
                            result.success(mapOf("success" to false, "error" to "Null PDU"))
                        }
                        else -> {
                            openSmsAppInternal(address, body, result)
                        }
                    }
                    unregisterSentReceiver(context)
                }
            }

            val filter = IntentFilter(SMS_SENT_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(sentReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(sentReceiver, filter)
            }

            smsManager.sendTextMessage(address, null, body, pendingIntent, null)
        } catch (e: SecurityException) {
            unregisterSentReceiver(context)
            openSmsAppInternal(address, body, result)
        } catch (e: Exception) {
            unregisterSentReceiver(context)
            result.success(mapOf("success" to false, "error" to (e.message ?: "Unknown error")))
        }
    }

    private fun unregisterSentReceiver(context: Context) {
        try {
            sentReceiver?.let { context.unregisterReceiver(it) }
            sentReceiver = null
        } catch (_: Exception) {}
    }

    private fun openSmsApp(address: String, body: String, result: MethodChannel.Result) {
        val activity = activityBinding?.activity
        if (activity == null) {
            result.success(false)
            return
        }
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${Uri.encode(address)}")
                putExtra("sms_body", body)
            }
            activity.startActivity(intent)
            result.success(true)
        } catch (e: Exception) {
            result.success(false)
        }
    }

    private fun openSmsAppInternal(address: String, body: String, result: MethodChannel.Result) {
        openSmsApp(address, body,
            object : MethodChannel.Result {
                override fun success(resultData: Any?) {
                    if (resultData == true) {
                        result.success(mapOf("success" to true, "viaFallback" to true))
                    } else {
                        result.success(
                            mapOf("success" to false, "error" to "Direct SMS send failed and no SMS app available")
                        )
                    }
                }

                override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
                    result.success(
                        mapOf("success" to false, "error" to (errorMessage ?: "Direct SMS send failed"))
                    )
                }

                override fun notImplemented() {
                    result.success(
                        mapOf("success" to false, "error" to "Direct SMS send failed")
                    )
                }
            }
        )
    }

    private fun openSmsApp(call: MethodCall, result: MethodChannel.Result) {
        val address = call.argument<String>("address") ?: ""
        val body = call.argument<String>("body") ?: ""
        openSmsApp(address, body, result)
    }
}
