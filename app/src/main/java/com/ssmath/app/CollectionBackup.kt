package com.ssmath.app

import java.io.IOException
import java.io.InputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal enum class CollectionImportMode { REPLACE, MERGE }

@Serializable
internal data class CollectionBackup(
    val format: String,
    val version: Int,
    val rewards: Map<RewardType, RewardBalance>,
    val pokemons: Set<Celebration>
) {
    init {
        require(format == FORMAT) { "This is not an ssAcademy collection file." }
        require(version == VERSION) { "Unsupported collection file version. Update ssAcademy on both devices." }
    }

    companion object {
        const val FORMAT = "ssAcademy-collections"
        const val VERSION = 1
        const val MAX_BYTES = 256 * 1024
        private val json = Json { encodeDefaults = true; prettyPrint = true }

        fun from(snapshot: PracticeSnapshot) =
            CollectionBackup(FORMAT, VERSION, snapshot.rewards, snapshot.pokemons)

        fun encode(backup: CollectionBackup): String = json.encodeToString(serializer(), backup)

        fun read(input: InputStream): CollectionBackup {
            val bytes = ByteArray(MAX_BYTES + 1)
            var size = 0
            while (size < bytes.size) {
                val count = input.read(bytes, size, bytes.size - size)
                if (count == -1) break
                size += count
            }
            if (size > MAX_BYTES) throw IOException("Collection file is too large (maximum 256 KB).")
            return try {
                json.decodeFromString(serializer(), bytes.decodeToString(0, size, throwOnInvalidSequence = true)
                    .removePrefix("\uFEFF"))
            } catch (error: SerializationException) {
                throw IOException("Invalid or unsupported collection file. Export again using the latest ssAcademy.", error)
            } catch (error: IllegalArgumentException) {
                throw IOException("Invalid or unsupported collection file. Export again using the latest ssAcademy.", error)
            }
        }
    }
}
