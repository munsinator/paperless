package at.technikum.api.repository;

import at.technikum.api.entity.DocumentType;
import at.technikum.api.entity.DocumentTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentTypeRepository extends JpaRepository<DocumentType, DocumentTypeEnum> {
}
