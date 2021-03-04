package com.colvir.core.services

import com.colvir.core.android.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics
import infrastructure.Logger
import services.ExceptionManager

// Enable Crashlytics debug logging
// https://firebase.google.com/docs/crashlytics/test-implementation?authuser=0&platform=android

class ExceptionManagerImpl : ExceptionManager {

    companion object {
        const val TAG = "EXM"
    }

    // Sends exception to a diagnostic server
    override fun send(ex: Exception, tag: String, message: String?, attachments: List<String>?, properties: Map<String, String>?) {

        Logger.i(TAG, "send release:${BuildConfig.BUILD_TYPE}  exp:${ex.message}")

        val crashlytics = FirebaseCrashlytics.getInstance()

        crashlytics.sendUnsentReports()

        // properties
        val propr = mutableMapOf<String, String>()
        if (properties != null) {
            propr.putAll(properties)
        }
        if (!message.isNullOrEmpty() && !propr.contains(ExceptionManager.TAGS.MESSAGE)) {
            propr.put(ExceptionManager.TAGS.MESSAGE, message)
        }
        for (prop in propr) {
            crashlytics.setCustomKey(prop.key, prop.value);
        }

        // Attachments
        if (attachments?.isNotEmpty() == true) {
            for (i in 0 until attachments.size) {
                crashlytics.log(attachments[i])
            }
        }

        Logger.e(tag, message ?: ex.toString(), ex)
        if (BuildConfig.BUILD_TYPE == "release") {
            crashlytics.recordException(ex)
            crashlytics.sendUnsentReports()
        }

    }

    override fun setUserId(userId: String) {
        Logger.i(TAG, "setUserId id:${userId}")
        FirebaseCrashlytics.getInstance().setUserId(userId)
    }


}
