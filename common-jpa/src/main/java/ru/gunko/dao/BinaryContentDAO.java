package ru.gunko.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.gunko.entity.BinaryContent;

public interface BinaryContentDAO extends JpaRepository<BinaryContent, Long> {
}
