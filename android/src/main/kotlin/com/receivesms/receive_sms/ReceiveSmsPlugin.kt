package com.receivesms.receive_sms

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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

    companion object {
        private const val SMS_EVENTS_CHANNEL = "com.greenhouse.greenhouse/sms_events"
        private const val PERMISSION_CHANNEL = "com.greenhouse.greenhouse/permission"
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val SEND_SMS_REQUEST_CODE = 1002
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
            if (requestCode == PERMISSION_REQUEST_CODE) {
                hasRequestedPermission = true
                val granted = grantResults.isNotEmpty() &&
                        grantResults.all { it == PackageManager.PERMISSION_GRANTED }

                val activity = activityBinding?.activity
                val canRequest = if (granted) {
                    true
                } else if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    activity.shouldShowRequestPermissionRationale(Manifest.permission.RECEIVE_SMS)
                } else {
                    true
                }

                permissionResult?.success(
                    mapOf("granted" to granted, "canRequest" to canRequest)
                )
                permissionResult = null
                true
            } else if (requestCode == SEND_SMS_REQUEST_CODE) {
                val granted = grantResults.isNotEmpty() &&
                        grantResults.all { it == PackageManager.PERMISSION_GRANTED }
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
                true
            } else {
                false
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
            arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS),
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

    private fun sendSms(call: MethodCall, result: MethodChannel.Result) {
        val address = call.argument<String>("address") ?: ""
        val body = call.argument<String>("body") ?: ""

        val activity = activityBinding?.activity
        if (activity == null) {
            result.success(mapOf("success" to false, "error" to "No available activity"))
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            activity.checkSelfPermission(Manifest.permission.SEND_SMS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            if (activity.shouldShowRequestPermissionRationale(Manifest.permission.SEND_SMS) ||
                sendSmsResult == null
            ) {
                sendSmsResult = result
                sendSmsAddress = address
                sendSmsBody = body
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.SEND_SMS),
                    SEND_SMS_REQUEST_CODE
                )
            } else {
                result.success(
                    mapOf("success" to false, "error" to "Permission permanently denied")
                )
            }
            return
        }

        sendSmsInternal(address, body, result)
    }

    private fun sendSmsInternal(
        address: String,
        body: String,
        result: MethodChannel.Result?
    ) {
        if (result == null) return
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(address, null, body, null, null)
            result.success(mapOf("success" to true))
        } catch (e: Exception) {
            result.success(mapOf("success" to false, "error" to (e.message ?: "Unknown error")))
        }
    }
}
