package com.jason.mypersonalai.android.adapters

import android.os.Environment
import android.os.StatFs
import com.jason.mypersonalai.android.capabilities.StorageInfo
import com.jason.mypersonalai.android.capabilities.StorageInfoProvider

/**
 * Real Android implementation of [StorageInfoProvider], backed by
 * [StatFs] on the device's data directory.
 *
 * Permission: NONE required. Reading free/total space on the device's
 * general storage area does not require any permission.
 */
class AndroidStorageInfoProvider : StorageInfoProvider {

    override fun getStorageInfo(): StorageInfo {
        val stat = StatFs(Environment.getDataDirectory().path)
        return StorageInfo(
            availableBytes = stat.availableBytes,
            totalBytes = stat.totalBytes
        )
    }
}
