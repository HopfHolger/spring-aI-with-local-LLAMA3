package it.gdorsi.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.gdorsi.repository.model.Autor;

public interface AuthorRepository extends JpaRepository<Autor, Long> {

    @Query(value = "SELECT * FROM autors ORDER BY author_embedding <=> :queryVector LIMIT 5", nativeQuery = true)
    List<Autor> findClosestAuthors(@Param("queryVector") float[] queryVector);

}
