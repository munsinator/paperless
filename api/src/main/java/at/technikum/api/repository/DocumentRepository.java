package at.technikum.api.repository;

import at.technikum.api.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
	java.util.List<Document> findByTitleContainingIgnoreCaseOrOcrTextContainingIgnoreCase(String title, String content);

}