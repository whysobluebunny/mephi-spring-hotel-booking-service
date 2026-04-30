package ru.mephi.abondarenko.booking.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.mephi.abondarenko.booking.domain.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);
}
