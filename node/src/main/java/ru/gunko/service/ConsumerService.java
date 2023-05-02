package ru.gunko.service;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface ConsumerService {

    void ConsumerTextMessageUpdate(Update update);
    void ConsumerDocMessageUpdate(Update update);
    void ConsumerPhotoMessageUpdate(Update update);
}
