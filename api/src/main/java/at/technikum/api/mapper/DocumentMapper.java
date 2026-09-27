package at.technikum.api.mapper;

import at.technikum.api.dto.request.RequestDocumentDTO;
import at.technikum.api.dto.response.ResponseDocumentDTO;
import at.technikum.api.entity.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    // Ingoing: RequestDto -> Entity for DB-Speicherung
    @Mapping(target = "type", ignore = true)
    Document dtoToEntity(RequestDocumentDTO dto);

    // Outgoing: Entity from DB -> ResponseDto for Client
    @Mapping(target = "documentType", source = "type.type")
    ResponseDocumentDTO entityToDto(Document entity);

}
