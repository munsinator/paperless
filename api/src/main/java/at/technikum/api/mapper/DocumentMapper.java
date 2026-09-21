package at.technikum.api.mapper;

import at.technikum.api.dto.request.RequestDocumentDTO;
import at.technikum.api.dto.response.ResponseDocumentDTO;
import at.technikum.api.entity.Document;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    // Ingoing: RequestDto -> Entity for DB-Speicherung
    Document dtoToEntity(RequestDocumentDTO dto);

    // Outgoing: Entity from DB -> ResponseDto for Client
    ResponseDocumentDTO entityToDto(Document entity);
}