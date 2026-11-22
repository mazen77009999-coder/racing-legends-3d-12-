package com.example.multiplayer

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class MultiplayerPlayerSlot(
    val playerId: String,
    val driverName: String,
    val carId: String,
    val paintHex: String,
    val isReady: Boolean,
    val isHost: Boolean,
    val pingMs: Int
)

@JsonClass(generateAdapter = true)
data class LobbyRoomState(
    val roomCode: String,
    val trackId: String,
    val laps: Int,
    val players: List<MultiplayerPlayerSlot>,
    val raceStarting: Boolean = false
)

@JsonClass(generateAdapter = true)
data class TelemetryPacket(
    val playerId: String,
    val speedKmh: Int,
    val lap: Int,
    val progressU: Float,
    val timestampMs: Long
)

@JsonClass(generateAdapter = true)
data class WireEnvelope(
    val type: String,
    val roomCode: String? = null,
    val playerId: String? = null,
    val driverName: String? = null,
    val carId: String? = null,
    val paintHex: String? = null,
    val trackId: String? = null,
    val ready: Boolean? = null,
    val lobby: LobbyRoomState? = null,
    val telemetry: TelemetryPacket? = null,
    val sentAtMs: Long? = null
)

object MultiplayerCodec {
    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val envelopeAdapter = moshi.adapter(WireEnvelope::class.java)

    fun encode(envelope: WireEnvelope): String = envelopeAdapter.toJson(envelope)

    fun decode(json: String): WireEnvelope? = runCatching {
        envelopeAdapter.fromJson(json)
    }.getOrNull()
}
