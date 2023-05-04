package ru.gunko.service;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.gunko.entity.AppDocument;
import ru.gunko.entity.AppPhoto;

public interface FileService {

    AppDocument processDoc(Message telegramMessage);
    AppPhoto processPhoto(Message telegramMessage);
}
