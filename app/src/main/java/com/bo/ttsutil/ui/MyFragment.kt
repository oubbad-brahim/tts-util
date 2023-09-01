/*
 * TTS Util
 *
 * Authors: Dane Finlay <dane@danefinlay.net>
 *
 * Copyright (C) 2022 Dane Finlay
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bo.ttsutil.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import com.bo.ttsutil.ApplicationEx
import com.bo.ttsutil.DIR_SELECT_CONT_CODE
import com.bo.ttsutil.R
import com.bo.ttsutil.REQUEST_EXTERNAL_STORAGE
import com.bo.ttsutil.TASK_ID_IDLE
import com.bo.ttsutil.TASK_ID_PROCESS_FILE
import com.bo.ttsutil.TASK_ID_READ_TEXT
import com.bo.ttsutil.TASK_ID_WRITE_FILE
import org.jetbrains.anko.AlertDialogBuilder
import splitties.toast.longToast

abstract class MyFragment : Fragment(), FragmentInterface {

    // This useful little function was extracted from Anko (Support.kt).
    inline fun <reified T : View> find(id: Int): T = view?.findViewById(id) as T

    protected val ctx: Context
        get() = this.requireContext()

    protected val myApplication: ApplicationEx
        get() = ctx.applicationContext as ApplicationEx

    protected val activityInterface: ActivityInterface?
        get() = context as? ActivityInterface

    private var tempStoragePermissionBlock: (granted: Boolean) -> Unit = {}

    abstract fun updateStatusField(text: String)
    abstract fun updateTaskCountField(count: Int)

    protected fun onStatusUpdate(event: ActivityEvent.StatusUpdateEvent) {
        val statusTextId = when (event.taskId) {
            TASK_ID_READ_TEXT -> R.string.status_text_reading_text
            TASK_ID_WRITE_FILE -> R.string.status_text_writing_file
            TASK_ID_PROCESS_FILE -> R.string.status_text_processing_file
            else -> R.string.status_text_idle
        }

        // Get the formatted status text.
        var statusText: String = if (event.taskId != TASK_ID_IDLE) {
            // Add the percentage for statuses other than IDLE.
            // Show "stopped" if progress<0.
            val parenthetical = "(" + if (event.progress < 0) {
                getString(R.string.status_text_task_stopped)
            } else {
                "${event.progress}%"
            } + ")"
            getString(statusTextId, parenthetical)
        } else {
            getString(statusTextId)
        }
        statusText = getString(R.string.status_text_field, statusText)

        // Update the status text field.
        updateStatusField(statusText)
    }

    private fun onTaskQueueChange(event: ActivityEvent.TaskQueueChangeEvent) {
        // Update the task count field.
        updateTaskCountField(event.remainingTasks)
    }

    override fun handleActivityEvent(event: ActivityEvent) {
        // Handle events common to all fragments.
        when (event) {
            is ActivityEvent.StatusUpdateEvent -> onStatusUpdate(event)
            is ActivityEvent.TaskQueueChangeEvent -> onTaskQueueChange(event)
            else -> {}
        }
    }

    fun withStoragePermission(block: (granted: Boolean) -> Unit) {
        // Check if we have write permission.
        if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 29) {
            val permission = ctx.checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission, so prompt the user.
                requestPermissions(PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE)

                // Store the function so we can execute it later if the user
                // grants us storage permission.
                tempStoragePermissionBlock = block
            } else {
                // We have permission, so execute the function.
                block(true)
            }
        } else {
            // No need to check permission before Android Marshmallow or after
            // Android Q, so execute the function.
            block(true)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (permissions.contentEquals(PERMISSIONS_STORAGE)) {
            // Check that all permissions were granted.
            var allGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    // Permission wasn't granted.
                    allGranted = false
                    break
                }
            }

            // Execute the storage permission block and replace it.
            tempStoragePermissionBlock(allGranted)
            tempStoragePermissionBlock = {}
        }
    }

    @CallSuper
    protected open fun onClickPlay() {
        // Attempt to initialize TTS.
        activityInterface?.initializeTTS(null)
    }

    @CallSuper
    protected open fun onClickSave() {
        // Attempt to initialize TTS.
        activityInterface?.initializeTTS(null)
    }

    protected fun buildNoPermissionAlertDialog(block: (granted: Boolean) -> Unit):
            AlertDialogBuilder {
        return AlertDialogBuilder(ctx).apply {
            title(R.string.no_storage_permission_title)
            message(R.string.no_storage_permission_message)
            positiveButton(R.string.grant_permission_positive_message) {
                // Try asking for storage permission again.
                withStoragePermission { granted -> block(granted) }
            }
            negativeButton(R.string.alert_negative_message_1)
        }
    }

    private fun showDirChooserCompat() {
        // Choosing the output directory is not possible on versions older than
        // Android Lollipop (21).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activityInterface?.showDirChooser(DIR_SELECT_CONT_CODE)
        } else {
            ctx.longToast(R.string.sdk_18_choose_dir_message)
        }
    }

    protected fun buildUnavailableDirAlertDialog(): AlertDialogBuilder {
        val title = R.string.unavailable_dir_dialog_title
        val message = R.string.unavailable_dir_dialog_message
        return AlertDialogBuilder(ctx).apply {
            title(title)
            message(message)
            positiveButton(R.string.alert_positive_message_1) {
                showDirChooserCompat()
            }
            negativeButton(R.string.alert_negative_message_1)
        }
    }

    protected fun buildUnwritableOutDirAlertDialog(): AlertDialogBuilder {
        val title = R.string.unwritable_out_dir_dialog_title
        val message = R.string.unwritable_out_dir_dialog_message
        return AlertDialogBuilder(ctx).apply {
            title(title)
            message(message)
            positiveButton(R.string.alert_positive_message_2) {
                showDirChooserCompat()
            }
            negativeButton(R.string.alert_negative_message_2)
        }
    }

    companion object {
        // Storage Permissions
        private val PERMISSIONS_STORAGE: Array<String> by lazy {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            } else {
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }
    }
}
