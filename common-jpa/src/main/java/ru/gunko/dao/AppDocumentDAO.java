package ru.gunko.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.gunko.entity.AppDocument;

public interface AppDocumentDAO extends JpaRepository<AppDocument, Long> {
}
