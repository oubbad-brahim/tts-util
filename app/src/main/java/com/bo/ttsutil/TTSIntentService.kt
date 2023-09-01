/*
 * TTS Util
 *
 * Authors: Dane Finlay <dane@danefinlay.net>
 *
 * Copyright (C) 2019 Dane Finlay
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

package com.bo.ttsutil

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.speech.tts.TextToSpeech.QUEUE_ADD
import com.bo.ttsutil.ui.EditReadActivity
import org.jetbrains.anko.ctx
import org.jetbrains.anko.runOnUiThread
import splitties.toast.longToast

// IntentService actions.
private const val ACTION_READ_TEXT = "${APP_NAME}.action.READ_TEXT"
private const val ACTION_EDIT_READ_TEXT = "${APP_NAME}.action.EDIT_READ_TEXT"
const val ACTION_STOP_TASK = "${APP_NAME}.action.STOP_TASK"
const val ACTION_READ_CLIPBOARD = "${APP_NAME}.action.READ_CLIPBOARD"
const val ACTION_EDIT_READ_CLIPBOARD = "${APP_NAME}.action.EDIT_READ_CLIPBOARD"

// Parameter constants (for Intent extras).
private const val EXTRA_TEXT = "${APP_NAME}.extra.TEXT"

/**
 * An [IntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
class TTSIntentService : IntentService("TTSIntentService") {

    private val myApplication: ApplicationEx
        get() = application as ApplicationEx

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return

        // Retrieve text to handle, if any.
        val text = intent.getStringExtra(EXTRA_TEXT) ?: ""

        // Handle actions.
        when (intent.action) {
            ACTION_EDIT_READ_TEXT -> handleActionEditReadText(text)
            ACTION_READ_CLIPBOARD -> handleActionReadClipboard()
            ACTION_EDIT_READ_CLIPBOARD -> handleActionEditReadClipboard(false)
            ACTION_STOP_TASK -> handleActionStopTask()
            ACTION_READ_TEXT -> {
                val description = getString(R.string.read_text_source_description)
                handleActionReadText(text, description)
            }
        }
    }

    /**
     * Handle action ReadText in the provided background thread with the provided
     * parameters.
     */
    private fun handleActionReadText(text: String?, sourceDescription: String) {
        // Read specified text and handle the result.
        val inputSource = InputSource.CharSequence(text ?: "", sourceDescription)
        val result = myApplication.enqueueReadInputTask(inputSource, QUEUE_ADD)
        myApplication.handleTaskResult(result)
    }

    /**
     * Handle action EditReadText in the provided background thread with the
     * provided parameters.
     */
    private fun handleActionEditReadText(text: String) {
        val intent = Intent(ctx, EditReadActivity::class.java).apply {
            addFlags(START_ACTIVITY_FLAGS)
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(intent)
    }

    /**
     * Handle action ReadClipboard in the provided background thread.
     */
    private fun handleActionReadClipboard() {
        // Show a warning message about this action on Android 10 and open the edit
        // activity instead, instructing playback to begin on start (without further
        // user interaction).
        if (Build.VERSION.SDK_INT >= 29) {
            runOnUiThread {
                longToast(R.string.sdk_29_read_clipboard_message)
            }
            handleActionEditReadClipboard(true)
            return
        }

        // Read clipboard text.
        val description = getString(R.string.read_clipboard_source_description)
        handleActionReadText(ctx.getClipboardText(), description)
    }

    /**
     * Handle action EditReadClipboard in the provided background thread.
     */
    private fun handleActionEditReadClipboard(playbackOnStart: Boolean) {
        val intent = Intent(ctx, EditReadActivity::class.java).apply {
            addFlags(START_ACTIVITY_FLAGS)
            action = ACTION_EDIT_READ_CLIPBOARD
            putExtra("playbackOnStart", playbackOnStart)
        }
        startActivity(intent)
    }

    /**
     * Handle action StopSpeaking in the provided background thread with the provided
     * parameters.
     */
    private fun handleActionStopTask() {
        // Stop the current task.  This also clears any tasks in the queue.
        myApplication.stopTask()

        // Cancel the task progress notification, if necessary.
        myApplication.cancelNotification(PROGRESS_NOTIFICATION_ID)
    }

    companion object {
        private inline fun startAction(
            ctx: Context, actionString: String,
            block: Intent.() -> Unit
        ) {
            val intent = Intent(ctx, TTSIntentService::class.java)
            intent.action = actionString
            intent.block()
            ctx.startService(intent)
        }

        private fun startTextAction(
            ctx: Context, actionString: String,
            text: String?
        ) {
            startAction(ctx, actionString) { putExtra(EXTRA_TEXT, text) }
        }

        /**
         * Starts this service to perform action ReadText. If the service is already
         * performing a task this action will be queued.
         *
         * @see IntentService
         */
        @JvmStatic
        fun startActionReadText(ctx: Context, text: String?) =
            startTextAction(ctx, ACTION_READ_TEXT, text)

        /**
         * Starts this service to perform action EditReadText. If the service is
         * already performing a task this action will be queued.
         *
         * @see IntentService
         */
        @JvmStatic
        fun startActionEditReadText(ctx: Context, text: String) =
            startTextAction(ctx, ACTION_EDIT_READ_TEXT, text)

        /**
         * Starts this service to perform action ReadClipboard. If the service is
         * already performing a task this action will be queued.
         *
         * @see IntentService
         */
        @JvmStatic
        fun startActionReadClipboard(ctx: Context) =
            startAction(ctx, ACTION_READ_CLIPBOARD) {}

        /**
         * Starts this service to perform action EditReadClipboard. If the service
         * is already performing a task this action will be queued.
         *
         * @see IntentService
         */
        @JvmStatic
        fun startActionEditReadClipboard(ctx: Context) =
            startAction(ctx, ACTION_EDIT_READ_CLIPBOARD) {}

        /**
         * Starts this service to perform action StopSpeaking. If the service is
         * already performing a task this action will be queued.
         *
         * @see IntentService
         */
        @JvmStatic
        fun startActionStopSpeaking(ctx: Context, taskId: Int) =
            startAction(ctx, ACTION_STOP_TASK) {
                putExtra("taskId", taskId)
            }
    }
}
