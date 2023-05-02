package ru.gunko.service.impl;

import lombok.extern.log4j.Log4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.gunko.service.ConsumerService;
import ru.gunko.service.ProducerService;

import static ru.gunko.model.RabbitQueue.*;

@Service
@Log4j
public class ConsumerServiceImpl implements ConsumerService {
    private final ProducerService producerService;

    public ConsumerServiceImpl(ProducerService producerService) {
        this.producerService = producerService;
    }

    @Override
    @RabbitListener(queues = TEXT_MESSAGE_UPDATE)
    public void ConsumerTextMessageUpdate(Update update) {

        log.debug("NODE: Text message is received");

        var message = update.getMessage();
        var sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText("Hello node");
        producerService.producerAnswer(sendMessage);

    }

    @Override
    @RabbitListener(queues = DOC_MESSAGE_UPDATE)
    public void ConsumerDocMessageUpdate(Update update) {

        log.debug("NODE: Doc message is received");
    }

    @Override
    @RabbitListener(queues = PHOTO_MESSAGE_UPDATE)
    public void ConsumerPhotoMessageUpdate(Update update) {

        log.debug("NODE: Photo message is received");

    }
}
