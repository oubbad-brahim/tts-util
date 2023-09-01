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

import android.content.res.Resources
import android.os.Build
import java.util.*

/**
 * Return the system's current locale.
 *
 * This will be a Locale object representing the user's preferred language as
 * set in the system settings.
 */
val currentSystemLocale: Locale?
    get() {
        val systemConfig = Resources.getSystem().configuration
        val systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            systemConfig?.locales?.get(0)
        } else {
            @Suppress("deprecation")
            systemConfig?.locale
        }

        // Return the system locale.
        return systemLocale
    }
