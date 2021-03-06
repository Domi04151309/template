package io.github.domi04151309.custom.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.widget.RemoteViews
import android.os.StatFs
import android.net.ConnectivityManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.os.RemoteException
import android.telephony.TelephonyManager
import android.app.AppOpsManager
import android.Manifest.permission.READ_PHONE_STATE
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.support.v4.content.PermissionChecker.checkPermission
import io.github.domi04151309.custom.PermissionActivity
import io.github.domi04151309.custom.R

class Dashboard : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.dashboard)
        views.setTextViewText(R.id.dataUsed, getDataUsed(context))
        views.setTextViewText(R.id.availableStorage, getAvailableStorage())
        views.setTextViewText(R.id.battery, getBatteryPercentage(context))
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getDataUsed(c: Context): String {
        var size = getAllBytesMobile(c)
        var suffix = "B"
        if (size >= 1024) {
            suffix = "KB"
            size /= 1024
            if (size >= 1024) {
                suffix = "MB"
                size /= 1024
            }
        }
        val resultBuffer = StringBuilder(size.toString())
        var commaOffset = resultBuffer.length - 3
        while (commaOffset > 0) {
            resultBuffer.insert(commaOffset, ',')
            commaOffset -= 3
        }
        resultBuffer.append(suffix)
        return resultBuffer.toString()
    }

    private fun getAvailableStorage(): String {
        val stat = StatFs(Environment.getDataDirectory().path)
        return "${(stat.availableBlocksLong.toFloat() / stat.blockCountLong.toFloat() * 100).toInt()}%"
    }

    private fun getBatteryPercentage(c: Context): String {
        return "${
            c.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        }%"
    }

    @SuppressLint("MissingPermission", "HardwareIds")
    private fun getAllBytesMobile(c: Context): Long {
        return if (hasPermissions(c)) {
            val bucket: NetworkStats.Bucket
            try {
                bucket = (c.applicationContext.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager)
                        .querySummaryForDevice(ConnectivityManager.TYPE_MOBILE,
                                (c.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).subscriberId,
                                getFirstDayOfMonth(),
                                System.currentTimeMillis()
                        )
                bucket.txBytes + bucket.rxBytes
            } catch (e: RemoteException) {
                0
            }
        } else 0
    }

    private fun hasPermissions(c: Context): Boolean {
        return hasPermissionToReadNetworkHistory(c) && hasPermissionToReadPhoneStats(c)
    }


    private fun hasPermissionToReadPhoneStats(c: Context): Boolean {
        return if (checkPermission(c, READ_PHONE_STATE, android.os.Process.myPid(), c.applicationInfo.uid, c.packageName) == PackageManager.PERMISSION_DENIED) {
            c.startActivity(Intent(c, PermissionActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            false
        } else true
    }

    private fun hasPermissionToReadNetworkHistory(c: Context): Boolean {
        return if (
                (c.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager)
                        .checkOpNoThrow(
                                AppOpsManager.OPSTR_GET_USAGE_STATS,
                                android.os.Process.myUid(), c.packageName
                        ) == AppOpsManager.MODE_ALLOWED
        ) {
            true
        } else {
            c.startActivity(Intent(c, PermissionActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            false
        }
    }

    private fun getFirstDayOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.clear(Calendar.MINUTE)
        cal.clear(Calendar.SECOND)
        cal.clear(Calendar.MILLISECOND)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return cal.timeInMillis
    }

}

