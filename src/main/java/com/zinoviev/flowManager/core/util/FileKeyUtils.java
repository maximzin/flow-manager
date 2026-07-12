package com.zinoviev.flowManager.core.util;

public class FileKeyUtils {

    // Приватный пустой конструктор для запрета создания экземпляров класса
    private FileKeyUtils() {}

    private static String parseFileNameWithExtension(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return "";
        }

        // Убираем возможный слеш в конце (если это "папка")
        String normalizedKey = fileKey.endsWith("/")
                ? fileKey.substring(0, fileKey.length() - 1)
                : fileKey;

        // Получаем имя файла (всё после последнего слеша)
        String fileName = normalizedKey.substring(
                normalizedKey.lastIndexOf('/') + 1);

        return fileName;
    }
}
