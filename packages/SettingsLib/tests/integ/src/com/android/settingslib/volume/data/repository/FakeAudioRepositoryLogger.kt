/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settingslib.volume.data.repository

import com.android.settingslib.volume.shared.AudioLogger
import com.android.settingslib.volume.shared.model.AudioStream
import com.android.settingslib.volume.shared.model.AudioStreamModel

class FakeAudioRepositoryLogger : AudioLogger {

    private val mutableLogs: MutableList<String> = mutableListOf()
    val logs: List<String>
        get() = mutableLogs

    override fun onSetVolumeRequested(audioStream: AudioStream, volume: Int) {
        synchronized(mutableLogs) {
            mutableLogs.add("onSetVolumeRequested audioStream=$audioStream")
        }
    }

    override fun onVolumeUpdateReceived(audioStream: AudioStream, model: AudioStreamModel) {
        synchronized(mutableLogs) {
            mutableLogs.add("onVolumeUpdateReceived audioStream=$audioStream")
        }
    }
}
