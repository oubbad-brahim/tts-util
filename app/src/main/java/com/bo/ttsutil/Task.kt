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

package com.bo.ttsutil

import android.content.Context

interface Task {
    val id: Int

    fun begin(): Int
    fun getBeginTaskMessage(ctx: Context): String
    fun getShortDescription(ctx: Context): String
    fun getLongDescription(ctx: Context, remainingTasks: Int): String
    fun getZeroLengthInputMessage(ctx: Context): String
    fun finalize()
}
