package it.gdorsi.repository;

import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.gdorsi.repository.model.Autor;
import jakarta.transaction.Transactional;

public interface AuthorRepository extends JpaRepository<Autor, Long> {


    boolean existsByName(String name);

    void deleteByName(String name);

    @Modifying
    @Transactional
    @Query(value = "UPDATE authors SET bio = :biografie, vector = cast(:vector as vector) WHERE name = :name", nativeQuery = true)
    int updateBiografieByName(
            @Param("name") String name,
            @Param("biografie") String biografie,
            @Param("vector") float[] vector
    );
}
