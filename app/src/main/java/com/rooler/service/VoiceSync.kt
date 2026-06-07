package com.rooler.service

import android.content.Context
import android.util.Base64
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Синхронизация записей озвучки между устройствами.
 *
 * Firebase Storage в проекте не подключён, поэтому записи хранятся прямо в
 * Cloud Firestore: коллекция `voices`, документ с ID = имя записи (num_1,
 * time_ended, ...), поле `b64` — содержимое .m4a в Base64.
 * Клипы короткие (секунды), помещаются в лимит документа Firestore (1 МБ).
 *
 * При записи фразы — файл уходит в облако (uploadVoice).
 * При запуске на новом устройстве — все записи скачиваются (syncDown).
 */
object VoiceSync {
    private const val COLLECTION = "voices"
    private const val FIELD = "b64"
    /** Запас под Base64 (×1.37) + служебные поля; за лимитом 1 МБ Firestore отклонит запись. */
    private const val MAX_RAW_BYTES = 700_000

    private val db by lazy { FirebaseFirestore.getInstance() }

    /** Загрузить локальную запись в облако. Возвращает true при успехе. */
    suspend fun uploadVoice(context: Context, name: String): Boolean {
        val file = VoicePlayer.voiceFile(context, name)
        if (!file.exists()) return false
        val bytes = file.readBytes()
        if (bytes.isEmpty() || bytes.size > MAX_RAW_BYTES) return false
        return runCatching {
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            db.collection(COLLECTION).document(name)
                .set(mapOf(FIELD to b64))
                .await()
            true
        }.getOrDefault(false)
    }

    /**
     * Скачать записи из облака, которых нет локально (или обновить все при force).
     * Возвращает количество скачанных файлов.
     */
    suspend fun syncDown(context: Context, force: Boolean = false): Int {
        var downloaded = 0
        runCatching {
            val snap = db.collection(COLLECTION).get().await()
            for (doc in snap.documents) {
                val name = doc.id
                val local = VoicePlayer.voiceFile(context, name)
                if (!force && local.exists()) continue
                val b64 = doc.getString(FIELD) ?: continue
                runCatching {
                    val bytes = Base64.decode(b64, Base64.NO_WRAP)
                    local.parentFile?.mkdirs()
                    local.writeBytes(bytes)
                    downloaded++
                }
            }
        }
        return downloaded
    }

    /** Удалить запись из облака. */
    suspend fun deleteVoice(name: String) {
        runCatching { db.collection(COLLECTION).document(name).delete().await() }
    }
}
