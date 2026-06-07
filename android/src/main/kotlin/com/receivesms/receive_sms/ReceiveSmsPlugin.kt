package com.receivesms.receive_sms

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel

class ReceiveSmsPlugin : FlutterPlugin, ActivityAware {
    private var permissionResult: MethodChannel.Result? = null
    private var activityBinding: ActivityPluginBinding? = null
    private var hasRequestedPermission = false

    companion object {
        private const val SMS_EVENTS_CHANNEL = "com.greenhouse.greenhouse/sms_events"
        private const val PERMISSION_CHANNEL = "com.greenhouse.greenhouse/permission"
        private const val PERMISSION_REQUEST_CODE = 1001
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
}
