package at.technikum.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String title;
    private String content; // for ocr

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type")
    private DocumentType type;

    private LocalDate created;
    private LocalTime added;
    private LocalTime modified;
}