package io.livekit.android.sample

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.ajalt.timberkt.Timber
import io.livekit.android.ConnectOptions
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import io.livekit.android.room.RoomListener
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.participant.RemoteParticipant
import kotlinx.coroutines.launch

class CallViewModel(
    val url: String,
    val token: String,
    application: Application
) : AndroidViewModel(application), RoomListener {
    private val mutableRoom = MutableLiveData<Room>()
    val room: LiveData<Room> = mutableRoom
    private val mutableRemoteParticipants = MutableLiveData<List<RemoteParticipant>>()
    val remoteParticipants: LiveData<List<RemoteParticipant>> = mutableRemoteParticipants
    private val participants = HashMap<String, LiveData<RemoteParticipant>>()

    init {
        viewModelScope.launch {
            val room = LiveKit.connect(
                application,
                url,
                token,
                ConnectOptions(false),
                this@CallViewModel
            )
            updateParticipants(room)
            for (p in room.remoteParticipants.values) {
                if (p.identity == null) {
                    continue
                }
                participants[p.identity!!] = MutableLiveData(p)
            }
            mutableRoom.value = room
        }
    }

    private fun updateParticipants(room: Room) {
        mutableRemoteParticipants.postValue(
            room.remoteParticipants
                .keys
                .sortedBy { it }
                .mapNotNull { room.remoteParticipants[it] }
        )
    }

    fun getParticipantObservable(identity: String): LiveData<RemoteParticipant>? {
        return participants[identity]
    }

    override fun onCleared() {
        super.onCleared()
        mutableRoom.value?.disconnect()
        mutableRoom.value = null
    }

    override fun onDisconnect(room: Room, error: Exception?) {
    }

    override fun onParticipantConnected(
        room: Room,
        participant: RemoteParticipant
    ) {
        updateParticipants(room)
        participants[participant.identity!!] = MutableLiveData(participant)

    }

    override fun onParticipantDisconnected(
        room: Room,
        participant: RemoteParticipant
    ) {
        updateParticipants(room)
        participants.remove(participant.identity!!)
    }

    override fun onFailedToConnect(room: Room, error: Exception) {
    }

    override fun onActiveSpeakersChanged(speakers: List<Participant>, room: Room) {
    }

    override fun onMetadataChanged(participant: Participant, prevMetadata: String?, room: Room) {
        Timber.i { "Participant metadata changed: ${participant.identity}" }
    }
}
