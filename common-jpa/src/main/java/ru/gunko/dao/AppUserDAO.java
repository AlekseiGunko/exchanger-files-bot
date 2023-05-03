package ru.gunko.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.gunko.entity.AppUser;

public interface AppUserDAO extends JpaRepository<AppUser, Long> {

    AppUser findAppUserByTelegramUserId(Long id);
}
