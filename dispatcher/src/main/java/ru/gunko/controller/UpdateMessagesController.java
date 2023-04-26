package ru.gunko.controller;

import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.gunko.service.UpdateProducer;
import ru.gunko.utils.MessageUtils;

import static ru.gunko.model.RabbitQueue.*;

@Component
@Log4j
public class UpdateMessagesController {

    private TelegramBot telegramBot;
    private MessageUtils messageUtils;
    private UpdateProducer updateProducer;

    public UpdateMessagesController(MessageUtils messageUtils, UpdateProducer updateProducer) {
        this.messageUtils = messageUtils;
        this.updateProducer = updateProducer;
    }

    public void registerBot(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    public void processUpdate(Update update) {
        if (update == null) {
            log.error("Полученное обновление null");
            return;
        }

        if (update.getMessage() != null) {
            distributeMessageByType(update);
        } else {
            log.error("Получен неподдерживаемый тип сообщения " + update);
        }


    }

    private void distributeMessageByType(Update update) {

        var message = update.getMessage();

        if (message.getText() != null) {
            processTextMessage(update);
        } else if (message.getDocument() != null) {
            processDocumentMessage(update);
        } else if (message.getPhoto() != null) {
            processPhotoMessage(update);
        } else {
            setUnsupportedMessageTypeView(update);
        }

    }

    private void setUnsupportedMessageTypeView(Update update) {

        var sendMessage = messageUtils.generateSendMessageWithText(update,
                "Неподдерживаемый тип сообщения");
        setView(sendMessage);
    }

    private void setFileIsReceivedView(Update update) {

        var sendMessage = messageUtils.generateSendMessageWithText(update,
                "Файл получен! Идет обработка...");
        setView(sendMessage);
    }

    private void setView(SendMessage sendMessage) {
        telegramBot.sendAnswerMessage(sendMessage);
    }


    private void processPhotoMessage(Update update) {

        updateProducer.produce(PHOTO_MESSAGE_UPDATE, update);
        setFileIsReceivedView(update);
    }

    private void processDocumentMessage(Update update) {

        updateProducer.produce(DOC_MESSAGE_UPDATE, update);
        setFileIsReceivedView(update);
    }

    private void processTextMessage(Update update) {

        updateProducer.produce(TEXT_MESSAGE_UPDATE, update);
    }
}
