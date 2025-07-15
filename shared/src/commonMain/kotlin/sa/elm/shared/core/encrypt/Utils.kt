package sa.elm.shared.core.encrypt

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.AES
import kotlinx.datetime.Clock
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

@OptIn(ExperimentalEncodingApi::class, DelicateCryptographyApi::class)
object EncryptDecryptUtility {
    var dynamicKey: String? = null

    var staticKeyId: String = "static_key" //TODO

    private val cryptographyProvider = CryptographyProvider.Default
    private val aes = cryptographyProvider.get(AES.CBC)

    private val fixedIv = ByteArray(16) { 0 }

    private suspend fun encrypt(data: String, secretKey: AES.CBC.Key): String {
        return try {
            val cipher = secretKey.cipher()
            val encryptedBytes = cipher.encryptWithIv(fixedIv, data.encodeToByteArray())
            Base64.encode(encryptedBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Encryption failed", e)
        }
    }

    private suspend fun decrypt(data: String, secretKey: AES.CBC.Key): String {
        return try {
            val cipher = secretKey.cipher()
            val decodedBytes = Base64.decode(data)
            val decryptedBytes = cipher.decryptWithIv(fixedIv, decodedBytes)
            decryptedBytes.decodeToString()
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Decryption failed", e)
        }
    }

    private suspend fun encryptWithStaticKey(str: String, keyBytes: ByteArray): String {
        return try {
            val secretKey = aes.keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, keyBytes)
            val cipher = secretKey.cipher()
            val encryptedBytes = cipher.encryptWithIv(fixedIv, str.encodeToByteArray())
            Base64.encode(encryptedBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun generateOtpSecret(specialCharLength: Int): String {
        val numbers = "0123456789"
        val specialChars = "&*@$"
        val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val random = Random.Default
        val sb = StringBuilder()

        repeat(2) { sb.append(numbers[random.nextInt(numbers.length)]) }
        repeat(specialCharLength) { sb.append(specialChars[random.nextInt(specialChars.length)]) }
        repeat(2) { sb.append(alphabet[random.nextInt(alphabet.length)]) }

        return sb.toString()
    }

    suspend fun getOTPrandomStr(firstTime: Boolean): String {
        val today = Clock.System.now().toEpochMilliseconds()
        val secret = generateOtpSecret(if (firstTime) 3 else 2)

        val combined = "$secret-$today"
        val encryptedString = encrypt(combined, generateDynamicSecretKey())

        return Base64.encode(encryptedString.encodeToByteArray())
    }

    private suspend fun generateDynamicSecretKey(): AES.CBC.Key {
        val keyString = dynamicKey
        require(!keyString.isNullOrEmpty()) { "Dynamic key is not set." }

        val keyBytes = keyString.encodeToByteArray()
        require(keyBytes.size == 16 || keyBytes.size == 24 || keyBytes.size == 32) {
            "Invalid key size: The dynamic key must be 128, 192, or 256 bits."
        }

        return aes.keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, keyBytes)
    }

    private fun generateNumericUUID(): String {
        val random = Random.Default
        return (1..5).joinToString(separator = "") { random.nextInt(10).toString() }
    }

    suspend fun generateExtHeader(): String {
        val uuid = generateNumericUUID()
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val data = "$uuid-$timestamp"
        val secretKey = generateDynamicSecretKey()
        return encrypt(data, secretKey)
    }

    suspend fun generateExtKey(applicationName: String): String {
        val secretKey = generateDynamicSecretKey()
        return encrypt(applicationName, secretKey)
    }

    suspend fun encryptURI(str: String): String {
        return try {
            val keyBytes = staticKeyId.encodeToByteArray()
            require(keyBytes.size == 16 || keyBytes.size == 24 || keyBytes.size == 32) {
                "Invalid static key size: The static key must be 128, 192, or 256 bits."
            }

            // Use zero IV like in Android implementation
            encryptWithStaticKey(str, keyBytes)

        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    suspend fun decodeAndVerify(data: String): String {
        return try {
            val secretKey = generateDynamicSecretKey()
            decrypt(data, secretKey)
        } catch (e: Exception) {
            // Log the exception for debugging purposes
            e.printStackTrace()
            "Invalid or corrupt key"
        }
    }
}