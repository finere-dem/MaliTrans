package com.malitrans.transport.mapper;

import com.malitrans.transport.dto.UtilisateurDTO;
import com.malitrans.transport.model.Utilisateur;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UtilisateurMapper {
    UtilisateurMapper INSTANCE = Mappers.getMapper(UtilisateurMapper.class);

    // toDto: Map all fields that exist in DTO (id, username, role, firstName, lastName, phone, address, companyName)
    // Automatically ignores: password, enabled, vehicleType, rating, fcmToken, identityDocumentUrl, status
    UtilisateurDTO toDto(Utilisateur utilisateur);
    
    // toEntity: ignore fields not provided in DTO
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "vehicleType", ignore = true)
    @Mapping(target = "rating", ignore = true)
    @Mapping(target = "fcmToken", ignore = true)
    @Mapping(target = "identityDocumentUrl", ignore = true)
    Utilisateur toEntity(UtilisateurDTO dto);
}
