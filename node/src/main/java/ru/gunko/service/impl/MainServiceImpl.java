package ru.gunko.service.impl;

import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.gunko.dao.AppUserDAO;
import ru.gunko.dao.RawDataDAO;
import ru.gunko.entity.AppDocument;
import ru.gunko.entity.AppPhoto;
import ru.gunko.entity.AppUser;
import ru.gunko.entity.RawData;
import ru.gunko.exceptions.UploadFileException;
import ru.gunko.service.FileService;
import ru.gunko.service.MainService;
import ru.gunko.service.ProducerService;
import ru.gunko.service.enums.ServiceCommands;

import static ru.gunko.entity.enums.UserState.BASIC_STATE;
import static ru.gunko.entity.enums.UserState.WAIT_FOR_EMAIL_STATE;
import static ru.gunko.service.enums.ServiceCommands.*;

@Service
@Log4j
public class MainServiceImpl implements MainService {

    private final RawDataDAO rawDataDAO;
    private final ProducerService producerService;
    private final AppUserDAO appUserDAO;
    private final FileService fileService;

    public MainServiceImpl(RawDataDAO rawDataDAO, ProducerService producerService,
                           AppUserDAO appUserDAO, FileService fileService) {
        this.rawDataDAO = rawDataDAO;
        this.producerService = producerService;
        this.appUserDAO = appUserDAO;
        this.fileService = fileService;
    }

    @Override
    public void processTextMessage(Update update) {

        saveRawData(update);

        var appUser = findOrSaveAppUser(update);
        var userState = appUser.getState();
        var text = update.getMessage().getText();
        var output = "";

        var serviceCommands = ServiceCommands.fromValue(text);

        if (CANCEL.equals(serviceCommands)) {
            output = cancelProcess(appUser);
        } else if (BASIC_STATE.equals(userState)) {
            output = processServiceCommand(appUser, text);
        } else if (WAIT_FOR_EMAIL_STATE.equals(userState)) {
            //TODO добавить обработку e-mail
        } else {
            log.error("Неизвестный статус пользователя " + userState);
            output = "неизвестная ошибка! Введите /cancel и попробуйте снова";
        }

        var chatId = update.getMessage().getChatId();
        sendAnswer(output, chatId);
    }

    @Override
    public void processDocMessage(Update update) {

        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var chatId = update.getMessage().getChatId();
        if (isNotAllowToSendContent(chatId, appUser)) {
            return;
        }

        try {
            AppDocument doc = fileService.processDoc(update.getMessage());
            //TODO добавить генерацию ссылки для скачивания документа
            var answer = "Документ успешно загружен! Ссылка для скачивания: skachaityt.com";
            sendAnswer(answer, chatId);
        } catch (UploadFileException e) {
            log.error(e);
            String error = "Файл не был загружен. Повторите попытку позже.";
            sendAnswer(error, chatId);
        }
    }

    @Override
    public void processPhotoMessage(Update update) {

        saveRawData(update);
        var appUser = findOrSaveAppUser(update);
        var chatId = update.getMessage().getChatId();
        if (isNotAllowToSendContent(chatId, appUser)) {
            return;
        }

        try{
            AppPhoto photo = fileService.processPhoto(update.getMessage());
            //TODO добавить генерацию ссылки для скачивания фото
            var answer = "Фото успешно загружено! Ссылка для скачивания: skachaityt.com";
            sendAnswer(answer, chatId);
        } catch (UploadFileException e) {
            log.error(e);
            String error = "Фото не было загружено. Повторите попытку позже.";
            sendAnswer(error, chatId);
        }
    }

    private boolean isNotAllowToSendContent(Long chatId, AppUser appUser) {
        var userState = appUser.getState();
        if (!appUser.getIsActive()) {
            var error = "Зарегистрируйтесь или активируйте свою учетную запись для загрузки контента";
            sendAnswer(error, chatId);
            return true;
        } else if (!BASIC_STATE.equals(userState)) {
            var error = "Отмените текущую команду с помощью /cancel для отправки файлов";
            sendAnswer(error, chatId);
            return true;
        }

        return false;
    }



    private void sendAnswer(String output, Long chatId) {

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(output);
        producerService.producerAnswer(sendMessage);
    }

    private String processServiceCommand(AppUser appUser, String cmd) {

        if (REGISTRATION.equals(cmd)) {
            //TODO добавить регистрацию
            return "Сервис временно недоступен";
        } else if (HELP.equals(cmd)) {
            return help();
        } else if (START.equals(cmd)) {
            return "Привет! Я бот-файлообменник." + "\n" +  "Введите /help чтобы посмотреть, что я умею";
        } else {
            return "Неизвестная команда!" + "\n" + "Введите /help";
        }
    }

    private String help() {
        return "Список доступных команд: \n"
                + "/cancel - отмена выполнения текущей команды \n"
                + "/registration - регистрация пользователя";

    }

    private String cancelProcess(AppUser appUser) {

        appUser.setState(BASIC_STATE);
        appUserDAO.save(appUser);
        return "Команда отменена";
    }

    private AppUser findOrSaveAppUser(Update update) {
        User telegramUser = update.getMessage().getFrom();
        AppUser persistentAppUser = appUserDAO.findAppUserByTelegramUserId(telegramUser.getId());

        if (persistentAppUser == null) {
            AppUser transientAppUser = AppUser.builder()
                    .telegramUserId(telegramUser.getId())
                    .userName(telegramUser.getUserName())
                    .firstName(telegramUser.getFirstName())
                    .lastName(telegramUser.getLastName())
                    //TODO поменять значение по умолчанию после регистрации
                    .isActive(true)
                    .state(BASIC_STATE)
                    .build();

            return appUserDAO.save(transientAppUser);
        }

        return persistentAppUser;
    }

    private void saveRawData(Update update) {
        RawData rawData = RawData.builder()
                                 .event(update)
                                 .build();
        rawDataDAO.save(rawData);
    }
}
