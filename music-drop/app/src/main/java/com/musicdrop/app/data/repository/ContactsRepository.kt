package com.musicdrop.app.data.repository

import android.content.ContentProviderOperation
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.musicdrop.app.data.model.MediaItem
import com.musicdrop.app.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Exports the device's contacts into a single .vcf (vCard) file for P2P transfer,
 * and imports a received .vcf file back into the device's Contacts provider.
 *
 * Export uses Android's built-in per-contact vCard endpoint
 * (ContactsContract.Contacts.CONTENT_VCARD_URI) so formatting/compatibility matches
 * whatever the stock Contacts app itself would produce, then concatenates every
 * contact's vCard into one file — multi-vCard .vcf files are a standard format every
 * contacts app (including this one's import path) can read back.
 */
class ContactsRepository(private val context: Context) {

    suspend fun getContactsCount(): Int = withContext(Dispatchers.IO) {
        var count = 0
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts._ID),
                null, null, null
            )
            cursor?.use { count = it.count }
        } catch (_: Exception) {}
        count
    }

    /**
     * Reads every contact's vCard and concatenates them into a single .vcf file
     * under the app cache dir, ready to hand to the P2P transfer engine as a MediaItem.
     * Returns null if there are no contacts, permission is missing, or export fails.
     */
    suspend fun exportContactsToVcf(): MediaItem? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val cursor = resolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.LOOKUP_KEY),
                null, null, null
            ) ?: return@withContext null

            val outFile = File(context.cacheDir, "FileDrop_Contacts_${System.currentTimeMillis()}.vcf")
            var exported = 0

            outFile.outputStream().use { out ->
                cursor.use {
                    val lookupCol = it.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)
                    while (it.moveToNext()) {
                        val lookupKey = it.getString(lookupCol) ?: continue
                        val vcardUri = Uri.withAppendedPath(
                            ContactsContract.Contacts.CONTENT_VCARD_URI,
                            lookupKey
                        )
                        try {
                            resolver.openInputStream(vcardUri)?.use { input ->
                                input.copyTo(out)
                                exported++
                            }
                        } catch (_: Exception) {
                            // Skip contacts the provider refuses to export (rare, e.g. corrupted rows)
                        }
                    }
                }
            }

            if (exported == 0) {
                outFile.delete()
                return@withContext null
            }

            MediaItem(
                id = outFile.hashCode().toLong(),
                uri = Uri.fromFile(outFile),
                name = "Contacts ($exported).vcf",
                size = outFile.length(),
                dateAdded = System.currentTimeMillis() / 1000,
                mimeType = "text/x-vcard",
                mediaType = MediaType.DOCUMENT,
                filePath = outFile.absolutePath,
                bucketName = "Phone Clone"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Bulk-imports a received .vcf file directly into the Contacts provider, without
     * requiring the user to open the stock Contacts app and tap through each card.
     * Returns the number of contacts successfully imported.
     */
    suspend fun importVcfFile(file: File): Int = withContext(Dispatchers.IO) {
        var imported = 0
        try {
            val text = file.readText(Charsets.UTF_8)
            for (card in splitVCards(text)) {
                try {
                    if (importSingleVCard(card)) imported++
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        imported
    }

    private fun splitVCards(text: String): List<String> {
        val cards = mutableListOf<String>()
        val builder = StringBuilder()
        var inCard = false
        for (line in text.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.equals("BEGIN:VCARD", ignoreCase = true)) {
                inCard = true
                builder.clear()
            }
            if (inCard) builder.append(line).append("\r\n")
            if (trimmed.equals("END:VCARD", ignoreCase = true)) {
                inCard = false
                cards.add(builder.toString())
            }
        }
        return cards
    }

    /**
     * Minimal vCard 2.1/3.0 parser covering FN, N, TEL, EMAIL — enough to reconstruct a
     * usable contact for a clone, not a full RFC 6350 implementation (no photos, addresses,
     * or organizations). Good enough since the vCard was produced by this same repository's
     * export, using the stock Android vCard format.
     */
    private fun importSingleVCard(vcard: String): Boolean {
        var displayName: String? = null
        val phones = mutableListOf<Pair<Int, String>>()
        val emails = mutableListOf<Pair<Int, String>>()

        for (rawLine in vcard.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val colonIdx = line.indexOf(':')
            if (colonIdx == -1) continue
            val key = line.substring(0, colonIdx).uppercase()
            val value = line.substring(colonIdx + 1).trim()
            if (value.isEmpty()) continue

            when {
                key == "FN" || key.startsWith("FN;") -> displayName = value
                key == "N" || key.startsWith("N;") -> {
                    if (displayName.isNullOrBlank()) {
                        displayName = value.split(";").filter { it.isNotBlank() }.joinToString(" ")
                    }
                }
                key.startsWith("TEL") -> {
                    val type = when {
                        key.contains("CELL") || key.contains("MOBILE") -> ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                        key.contains("WORK") -> ContactsContract.CommonDataKinds.Phone.TYPE_WORK
                        key.contains("HOME") -> ContactsContract.CommonDataKinds.Phone.TYPE_HOME
                        else -> ContactsContract.CommonDataKinds.Phone.TYPE_OTHER
                    }
                    phones.add(type to value)
                }
                key.startsWith("EMAIL") -> {
                    val type = if (key.contains("WORK")) ContactsContract.CommonDataKinds.Email.TYPE_WORK
                    else ContactsContract.CommonDataKinds.Email.TYPE_HOME
                    emails.add(type to value)
                }
            }
        }

        if (displayName.isNullOrBlank() && phones.isEmpty() && emails.isEmpty()) return false

        val ops = ArrayList<ContentProviderOperation>()
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build()
        )
        if (!displayName.isNullOrBlank()) {
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                    .build()
            )
        }
        for ((type, number) in phones) {
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, type)
                    .build()
            )
        }
        for ((type, email) in emails) {
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                    .withValue(ContactsContract.CommonDataKinds.Email.TYPE, type)
                    .build()
            )
        }

        return try {
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        } catch (_: Exception) {
            false
        }
    }
}
