package ru.gunko.service.impl;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.gunko.controller.UpdateMessagesController;
import ru.gunko.service.AnswerConsumer;

import static ru.gunko.model.RabbitQueue.ANSWER_MESSAGE;

@Service
public class AnswerConsumerImpl implements AnswerConsumer {
    private final UpdateMessagesController updateMessagesController;

    public AnswerConsumerImpl(UpdateMessagesController updateMessagesController) {
        this.updateMessagesController = updateMessagesController;
    }

    @Override
    @RabbitListener(queues = ANSWER_MESSAGE)
    public void consume(SendMessage sendMessage) {

        updateMessagesController.setView(sendMessage);

    }
}
