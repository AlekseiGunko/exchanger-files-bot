package ru.gunko.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.gunko.entity.AppPhoto;

public interface AppPhotoDAO extends JpaRepository<AppPhoto, Long> {
}
