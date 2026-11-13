package app.podium.desktop

import app.podium.core.common.Outcome
import app.podium.core.common.PodiumError
import kotlinx.serialization.KSerializer
import java.io.File

/**
 * Desktop credential store using file-based storage with simple encryption.
 * For MVP, uses simple XOR obfuscation. Replace with proper DPAPI/Keystore later.
 */
class DesktopCredentialStore {
    
    private val credDir = File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "Podium/credentials")
    
    init {
        credDir.mkdirs()
    }
    
    /**
     * Stores a credential (simple XOR obfuscation for MVP).
     */
    fun <T> store(key: String, value: T, serializer: KSerializer<T>): Outcome<Unit> {
        return try {
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            val serialized = json.encodeToString(serializer, value)
            val obfuscated = obfuscate(serialized)
            
            val credFile = File(credDir, "$key.dat")
            credFile.writeText(obfuscated)
            
            Outcome.Success(Unit)
        } catch (e: Exception) {
            Outcome.Failure(PodiumError.Unexpected(e))
        }
    }
    
    /**
     * Retrieves and deobfuscates a credential.
     */
    fun <T> read(key: String, serializer: KSerializer<T>): Outcome<T> {
        return try {
            val credFile = File(credDir, "$key.dat")
            
            if (!credFile.exists()) {
                return Outcome.Failure(PodiumError.NotFound(key))
            }
            
            val obfuscated = credFile.readText()
            val serialized = deobfuscate(obfuscated)
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            val value = json.decodeFromString(serializer, serialized)
            Outcome.Success(value)
        } catch (e: Exception) {
            Outcome.Failure(PodiumError.Unexpected(e))
        }
    }
    
    /**
     * Deletes a credential.
     */
    fun delete(key: String): Outcome<Unit> {
        return try {
            val credFile = File(credDir, "$key.dat")
            if (credFile.exists()) {
                credFile.delete()
            }
            Outcome.Success(Unit)
        } catch (e: Exception) {
            Outcome.Failure(PodiumError.Unexpected(e))
        }
    }
    
    private fun obfuscate(text: String): String {
        // Simple XOR obfuscation for MVP - replace with proper encryption later
        val key = "PodiumDesktop2024".toByteArray()
        val bytes = text.toByteArray()
        val result = ByteArray(bytes.size)
        for (i in bytes.indices) {
            result[i] = (bytes[i] xor key[i % key.size]).toByte()
        }
        return java.util.Base64.getEncoder().encodeToString(result)
    }
    
    private fun deobfuscate(obfuscated: String): String {
        val key = "PodiumDesktop2024".toByteArray()
        val bytes = java.util.Base64.getDecoder().decode(obfuscated)
        val result = ByteArray(bytes.size)
        for (i in bytes.indices) {
            result[i] = (bytes[i] xor key[i % key.size]).toByte()
        }
        return result.decodeToString()
    }
}