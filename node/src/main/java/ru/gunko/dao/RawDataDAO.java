package ru.gunko.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.gunko.entity.RawData;

public interface RawDataDAO extends JpaRepository<RawData, Long> {

}
