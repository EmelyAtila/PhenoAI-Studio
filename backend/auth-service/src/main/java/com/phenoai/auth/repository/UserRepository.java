package com.phenoai.auth.repository;

<<<<<<< HEAD
import com.phenoai.auth.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
=======
import com.phenoai.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
>>>>>>> development

import java.util.Optional;
import java.util.UUID;

<<<<<<< HEAD
@Repository
=======
>>>>>>> development
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
