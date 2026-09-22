package com.musicdrop.tv.data.youtube.potoken

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses the raw challenge data obtained from the Create endpoint and returns a JSON object
 * string that can be embedded directly in a JavaScript snippet (as the `data` global).
 *
 * Ported from NewPipe's util/potoken/JavaScriptUtil.kt (org.json instead of nanojson).
 */
fun parseChallengeData(rawChallengeData: String): String {
    val scrambled = JSONArray(rawChallengeData)

    val challengeData: JSONArray = if (scrambled.length() > 1 && scrambled.opt(1) is String) {
        JSONArray(descramble(scrambled.getString(1)))
    } else {
        scrambled.getJSONArray(0)
    }

    val messageId = challengeData.getString(0)
    val interpreterHash = challengeData.getString(3)
    val program = challengeData.getString(4)
    val globalName = challengeData.getString(5)
    val clientExperimentsStateBlob = challengeData.optString(7)

    val safeScriptWrappedValue = challengeData.optJSONArray(1)
        ?.firstStringElement()
    val trustedResourceUrlWrappedValue = challengeData.optJSONArray(2)
        ?.firstStringElement()

    val interpreterJavascript = JSONObject()
        .put("privateDoNotAccessOrElseSafeScriptWrappedValue",
            safeScriptWrappedValue ?: JSONObject.NULL)
        .put("privateDoNotAccessOrElseTrustedResourceUrlWrappedValue",
            trustedResourceUrlWrappedValue ?: JSONObject.NULL)

    return JSONObject()
        .put("interpreterJavascript", interpreterJavascript)
        .put("interpreterHash", interpreterHash)
        .put("program", program)
        .put("globalName", globalName)
        .put("clientExperimentsStateBlob", clientExperimentsStateBlob)
        .toString()
}

/**
 * Parses the raw integrity token data obtained from the GenerateIT endpoint into a JavaScript
 * `Uint8Array` expression that can be embedded directly in JavaScript code, plus the token
 * duration in seconds.
 */
fun parseIntegrityTokenData(rawIntegrityTokenData: String): Pair<String, Long> {
    val integrityTokenData = JSONArray(rawIntegrityTokenData)
    return base64ToU8(integrityTokenData.getString(0)) to integrityTokenData.getLong(1)
}

/**
 * Converts a string (usually the identifier used as input to `obtainPoToken`) to a JavaScript
 * `Uint8Array` expression that can be embedded directly in JavaScript code.
 */
fun stringToU8(identifier: String): String {
    return newUint8Array(identifier.toByteArray(Charsets.UTF_8))
}

/**
 * Takes a poToken encoded as a sequence of bytes represented as integers separated by commas
 * (e.g. "97,98,99" would be "abc"), which is the output of `Uint8Array::toString()` in
 * JavaScript, and converts it to the specific base64 (URL-safe) representation for poTokens.
 */
fun u8ToBase64(poTokenU8: String): String {
    val bytes = poTokenU8.split(",")
        .map { it.trim().toInt() and 0xFF }
        .fold(ArrayList<Byte>(poTokenU8.length)) { acc, v -> acc.add(v.toByte()); acc }
        .toByteArray()
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
        .replace("+", "-")
        .replace("/", "_")
}

/**
 * Takes the scrambled challenge, decodes it from base64, adds 97 to each byte.
 */
private fun descramble(scrambledChallenge: String): String {
    return String(base64ToByteString(scrambledChallenge)
        .map { ((it.toInt() + 97) and 0xFF).toByte() }
        .toByteArray(), Charsets.UTF_8)
}

/**
 * Decodes a base64 string encoded in the specific base64 representation used by YouTube, and
 * returns a JavaScript `Uint8Array` expression that can be embedded directly in JavaScript code.
 */
private fun base64ToU8(base64: String): String {
    return newUint8Array(base64ToByteString(base64))
}

private fun newUint8Array(contents: ByteArray): String {
    return "new Uint8Array([" +
        contents.joinToString(separator = ",") { (it.toInt() and 0xFF).toString() } + "])"
}

/**
 * Decodes a base64 string encoded in the specific base64 representation used by YouTube.
 */
private fun base64ToByteString(base64: String): ByteArray {
    val base64Mod = base64
        .replace('-', '+')
        .replace('_', '/')
        .replace('.', '=')

    return try {
        Base64.decode(base64Mod, Base64.DEFAULT)
    } catch (t: Throwable) {
        throw PoTokenException("Cannot base64 decode")
    }
}

private fun JSONArray.firstStringElement(): String? {
    for (i in 0 until length()) {
        val v = opt(i)
        if (v is String) return v
    }
    return null
}
