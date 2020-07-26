package `in`.thenvn.artista.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat

abstract class PermissionUtils {

    companion object {

        private const val TAG = "PermissionUtils"

        /**
         * Check if a permission is granted
         *
         * @param context Context from which to check
         * @param permission Permission which is to be checked
         * @return Boolean indicating if permission is granted or denied
         */
        fun isPermissionGranted(context: Context, permission: String): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }

        /**
         * Show an alert dialog with a rationale string for any permission
         *
         * @param context Context to use for dialog
         * @param rationale Rationale text to display
         * @param callback Callback for the positive button of dialog
         */
        fun showRationale(context: Context, rationale: String, callback: () -> Unit) {
            AlertDialog.Builder(context)
                .setMessage(rationale)
                .setCancelable(false)
                .setPositiveButton("ALLOW") { dialog, which ->
                    dialog.dismiss()
                    callback()
                }.show()
        }

        /**
         * Show an alert dialog with rationale string & a button to go to app's settings (to allow
         * permissions)
         *
         * @param activity Activity to build dialog from & finish it if negative option selected
         * @param rationale Rationale text to display in dialog
         */
        fun showRationaleWithSettings(activity: Activity, rationale: String) {
            AlertDialog.Builder(activity)
                .setMessage(rationale)
                .setCancelable(false)
                .setMessage(rationale)
                .setPositiveButton("OPEN SETTINGS") { dialog, which ->
                    dialog.dismiss()
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", activity.packageName, null)
                    }
                    activity.startActivity(intent)
                }
                .setNegativeButton("DENY") { dialog, which ->
                    dialog.dismiss()
                    Toast.makeText(activity, "Required Permission Denied!", Toast.LENGTH_SHORT)
                        .show()
                    activity.finish()
                }
                .show()
        }

    }
}