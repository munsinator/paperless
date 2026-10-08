package at.technikum.api.mapper;

import at.technikum.api.dto.request.RequestDocumentDTO;
import at.technikum.api.dto.response.ResponseDocumentDTO;
import at.technikum.api.entity.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    // Entity -> ResponseDTO (Kategorie-Name aus dem Category-Objekt lesen)
    @Mapping(target = "category", source = "category.name")
    ResponseDocumentDTO entityToDto(Document entity);

    // RequestDTO -> Entity (Category wird im Service gesetzt)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "category", ignore = true)
    Document dtoToEntity(RequestDocumentDTO dto);
}