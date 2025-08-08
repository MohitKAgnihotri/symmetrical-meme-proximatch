package com.proxilocal.hyperlocal

import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Compact BLE intent payloads for shadow mode.
 *
 * Wire format (fixed width, little-endian):
 *   [0]   opcode
 *   [1]   version = 0x01
 *   [2..9)   senderId (8 bytes)
 *   [10..17) targetId (8 bytes)  // can be all zeroes if N/A
 *
 * Total = 18 bytes
 */
object ProxiPayloads {
    private const val TAG = "ProxiPayloads"
    const val VERSION: Byte = 0x01
    const val ID_BYTES: Int = 8
    const val TOTAL_LEN: Int = 1 + 1 + ID_BYTES + ID_BYTES

    data class Msg(
        val opcode: Byte,
        val senderId: ByteArray, // len = 8
        val targetId: ByteArray  // len = 8
    )

    fun encode(opcode: Byte, senderId: ByteArray, targetId: ByteArray = ByteArray(ID_BYTES)): ByteArray {
        require(senderId.size == ID_BYTES) { "senderId must be $ID_BYTES bytes" }
        require(targetId.size == ID_BYTES) { "targetId must be $ID_BYTES bytes" }
        val buf = ByteBuffer.allocate(TOTAL_LEN).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(opcode)
        buf.put(VERSION)
        buf.put(senderId)
        buf.put(targetId)
        return buf.array()
    }

    fun decode(payload: ByteArray?): Msg? {
        if (payload == null || payload.size != TOTAL_LEN) return null
        return try {
            val buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
            val op = buf.get()
            val ver = buf.get()
            if (ver != VERSION) return null
            val s = ByteArray(ID_BYTES); buf.get(s)
            val t = ByteArray(ID_BYTES); buf.get(t)
            Msg(op, s, t)
        } catch (e: Throwable) {
            Log.w(TAG, "decode failed", e); null
        }
    }

    fun log(prefix: String, payload: ByteArray) {
        val m = decode(payload)
        if (m != null) {
            Log.d(TAG, "$prefix op=0x${(m.opcode.toInt() and 0xFF).toString(16)} s=${m.senderId.toHex()} t=${m.targetId.toHex()}")
        } else {
            Log.d(TAG, "$prefix (unparsed ${payload.size} bytes)")
        }
    }
}

/** Hex helpers (no allocations in hot paths other than String) */
internal fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
