package cachet.plugins.health

import android.app.Activity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.Scopes
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry

class HealthPlugin : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {
    private lateinit var channel: MethodChannel
    private var result: MethodChannel.Result? = null
    private var activityBinding: ActivityPluginBinding? = null
    private val REQUEST_CODE_GOOGLE_FIT_PERMISSIONS = 1001

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, "health")
        channel.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activityBinding = binding
        binding.addActivityResultListener { requestCode, resultCode, data ->
            if (requestCode == REQUEST_CODE_GOOGLE_FIT_PERMISSIONS) {
                val currentResult = result
                this.result = null
                if (resultCode == Activity.RESULT_OK) {
                    currentResult?.success(true)
                } else {
                    currentResult?.error("AUTH_FAILED", "Google Fit authorization failed", null)
                }
                true
            } else {
                false
            }
        }
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activityBinding = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {
        activityBinding = null
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "checkAvailability" -> {
                // Simplified: Assume Google Fit is available
                result.success(true)
            }
            "hasPermissions" -> {
                // Simplified: Check if Google Fit permissions are granted
                val activity = activityBinding?.activity
                if (activity == null) {
                    result.error("NO_ACTIVITY", "Activity not available", null)
                    return
                }
                val types = call.argument<List<String>>("types") ?: emptyList()
                val permissions = call.argument<List<Int>>("permissions") ?: emptyList()
                val hasPermission = GoogleSignIn.hasPermissions(
                    GoogleSignIn.getLastSignedInAccount(activity),
                    Scope(Scopes.FITNESS_ACTIVITY_READ)
                )
                result.success(hasPermission)
            }
            "requestAuthorization" -> {
                val activity = activityBinding?.activity
                if (activity == null) {
                    result.error("NO_ACTIVITY", "Activity not available", null)
                    return
                }

                // Parse arguments
                val types = call.argument<List<String>>("types") ?: emptyList()
                val permissions = call.argument<List<Int>>("permissions") ?: emptyList()

                // Request Google Fit authorization
                val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestScopes(Scope(Scopes.FITNESS_ACTIVITY_READ))
                    .build()
                val client = GoogleSignIn.getClient(activity, signInOptions)
                val signInIntent = client.signInIntent
                activity.startActivityForResult(signInIntent, REQUEST_CODE_GOOGLE_FIT_PERMISSIONS)

                // Store the result for later
                this.result = result
            }
            "getData" -> {
                // Simplified: Return empty data for now (we'll implement this later)
                result.success(emptyList<Map<String, Any>>())
            }
            else -> result.notImplemented()
        }
    }

    companion object {
        @JvmStatic
        fun registerWith(registrar: PluginRegistry.Registrar) {
            val plugin = HealthPlugin()
            registrar.activity()?.let {
                plugin.onAttachedToActivity(ActivityPluginBindingStub(it))
            }
        }
    }
}

// Stub for older Flutter versions (if needed)
class ActivityPluginBindingStub(private val activity: Activity) : ActivityPluginBinding {
    override fun getActivity(): Activity = activity
    override fun addActivityResultListener(listener: PluginRegistry.ActivityResultListener) {}
    override fun removeActivityResultListener(listener: PluginRegistry.ActivityResultListener) {}
    override fun addRequestPermissionsResultListener(listener: PluginRegistry.RequestPermissionsResultListener) {}
    override fun removeRequestPermissionsResultListener(listener: PluginRegistry.RequestPermissionsResultListener) {}
    override fun addOnNewIntentListener(listener: PluginRegistry.NewIntentListener) {}
    override fun removeOnNewIntentListener(listener: PluginRegistry.NewIntentListener) {}
    override fun addOnUserLeaveHintListener(listener: PluginRegistry.UserLeaveHintListener) {}
    override fun removeOnUserLeaveHintListener(listener: PluginRegistry.UserLeaveHintListener) {}
    override fun addOnWindowFocusChangedListener(listener: PluginRegistry.WindowFocusChangedListener) {}
    override fun removeOnWindowFocusChangedListener(listener: PluginRegistry.WindowFocusChangedListener) {}
}