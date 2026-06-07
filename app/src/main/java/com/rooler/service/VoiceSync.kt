package com.rooler.service

import android.content.Context
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * Синхронизация записей озвучки с Firebase Storage.
 * При записи фразы — файл уходит в облако (uploadVoice).
 * При запуске на новом устройстве — недостающие файлы скачиваются (syncDown).
 * Путь в облаке: voices/<имя>.m4a (общий для всех устройств одного заведения).
 */
object VoiceSync {
    private val storage by lazy { FirebaseStorage.getInstance() }
    private fun ref(name: String) = storage.reference.child("voices/$name.m4a")

    /** Загрузить локальную запись в облако. */
    suspend fun uploadVoice(context: Context, name: String) {
        val file = VoicePlayer.voiceFile(context, name)
        if (!file.exists()) return
        runCatching {
            ref(name).putFile(android.net.Uri.fromFile(file)).await()
        }
    }

    /**
     * Скачать все записи из облака, которых нет локально (или обновить все).
     * Возвращает количество скачанных файлов.
     */
    suspend fun syncDown(context: Context, force: Boolean = false): Int {
        var downloaded = 0
        runCatching {
            val list = storage.reference.child("voices").listAll().await()
            for (item in list.items) {
                val name = item.name.removeSuffix(".m4a")
                val local = VoicePlayer.voiceFile(context, name)
                if (force || !local.exists()) {
                    runCatching {
                        local.parentFile?.mkdirs()
                        item.getFile(local).await()
                        downloaded++
                    }
                }
            }
        }
        return downloaded
    }

    /** Удалить запись и из облака (при перезаписи можно не вызывать — putFile перезапишет). */
    suspend fun deleteVoice(name: String) {
        runCatching { ref(name).delete().await() }
    }
}
