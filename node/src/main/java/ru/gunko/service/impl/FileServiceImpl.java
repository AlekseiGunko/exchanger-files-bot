package ru.gunko.service.impl;

import lombok.extern.log4j.Log4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import ru.gunko.dao.AppDocumentDAO;
import ru.gunko.dao.AppPhotoDAO;
import ru.gunko.dao.BinaryContentDAO;
import ru.gunko.entity.AppDocument;
import ru.gunko.entity.AppPhoto;
import ru.gunko.entity.BinaryContent;
import ru.gunko.exceptions.UploadFileException;
import ru.gunko.service.FileService;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

@Service
@Log4j
public class FileServiceImpl implements FileService {

    @Value("${token}")
    private String token;
    @Value("${service.file_info.uri}")
    private String fileInfoUri;
    @Value("${service.file_storage.uri}")
    private String fileStorageUri;
    private final AppDocumentDAO appDocumentDAO;
    private final AppPhotoDAO appPhotoDAO;
    private final BinaryContentDAO binaryContentDAO;

    public FileServiceImpl(AppDocumentDAO appDocumentDAO, AppPhotoDAO appPhotoDAO, BinaryContentDAO binaryContentDAO) {
        this.appDocumentDAO = appDocumentDAO;
        this.appPhotoDAO = appPhotoDAO;
        this.binaryContentDAO = binaryContentDAO;
    }

    @Override
    public AppDocument processDoc(Message telegramMessage) {

        Document telegramDoc = telegramMessage.getDocument();
        String filedId = telegramDoc.getFileId();
        ResponseEntity<String> response = getFilePath(filedId);
        if (response.getStatusCode() == HttpStatus.OK) {
            BinaryContent persistentBinaryContent = getPersistentBinaryContent(response);
            AppDocument transientAppDocument = buildTransientAppDoc(telegramDoc, persistentBinaryContent);
            return appDocumentDAO.save(transientAppDocument);
        } else {
            throw new UploadFileException("Неверный запрос к сервису telegram " + response);
        }
    }

    @Override
    public AppPhoto processPhoto(Message telegramMessage) {

        //TODO обрабатываем пока что одно фото
        PhotoSize telegramPhoto = telegramMessage.getPhoto().get(0);
        String filedId = telegramPhoto.getFileId();
        ResponseEntity<String> response = getFilePath(filedId);
        if (response.getStatusCode() == HttpStatus.OK) {
            BinaryContent persistentBinaryContent = getPersistentBinaryContent(response);
            AppPhoto transientAppDocument = buildTransientAppPhoto(telegramPhoto, persistentBinaryContent);
            return appPhotoDAO.save(transientAppDocument);
        } else {
            throw new UploadFileException("Неверный запрос к сервису telegram " + response);
        }
    }


    private BinaryContent getPersistentBinaryContent(ResponseEntity<String> response) {
        String filePath = getFilePath(response);
        byte[] fileByte = downloadFile(filePath);
        BinaryContent transientBinaryContent = BinaryContent.builder()
                .fileArrayOfBytes(fileByte)
                .build();

        return binaryContentDAO.save(transientBinaryContent);
    }

    private static String getFilePath(ResponseEntity<String> response) {
        JSONObject jsonObject = new JSONObject(response.getBody());

        return String.valueOf(jsonObject
                .getJSONObject("result")
                .getString("file_path"));
    }


    private AppDocument buildTransientAppDoc(Document telegramDoc, BinaryContent persistentBinaryContent) {

        return AppDocument.builder()
                .telegramFiledId(telegramDoc.getFileId())
                .docName(telegramDoc.getFileName())
                .binaryContent(persistentBinaryContent)
                .mimeType(telegramDoc.getMimeType())
                .fileSize(Long.valueOf(telegramDoc.getFileSize()))
                .build();
    }

    private AppPhoto buildTransientAppPhoto(PhotoSize telegramPhoto, BinaryContent persistentBinaryContent) {

        return AppPhoto.builder()
                .telegramFiledId(telegramPhoto.getFileId())
                .binaryContent(persistentBinaryContent)
                .fileSize(Long.valueOf(telegramPhoto.getFileSize()))
                .build();
    }

    private byte[] downloadFile(String filePath) {

        String fullUri = fileStorageUri.replace("{token}", token)
                .replace("{filePath}", filePath);
        URL urlObj = null;
        try{
            urlObj = new URL(fullUri);
        } catch (MalformedURLException e) {
            throw new UploadFileException(e);
        }

        try(InputStream is = urlObj.openStream()){
            return is.readAllBytes();
        }catch (IOException e) {
            throw new UploadFileException(urlObj.toExternalForm(), e);
        }
    }

    private ResponseEntity<String> getFilePath(String filedId) {

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        HttpEntity<String> request = new HttpEntity<>(httpHeaders);

        return restTemplate.exchange(
                fileInfoUri,
                HttpMethod.GET,
                request,
                String.class,
                token,
                filedId
        );
    }
}
