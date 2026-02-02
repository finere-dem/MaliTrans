package com.malitrans.transport.mapper;

import com.malitrans.transport.dto.RideRequestDTO;
import com.malitrans.transport.model.FlowType;
import com.malitrans.transport.model.RideRequest;
import com.malitrans.transport.model.ValidationStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface RideRequestMapper {
    RideRequestMapper INSTANCE = Mappers.getMapper(RideRequestMapper.class);

    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "supplier.id", target = "supplierId", nullValuePropertyMappingStrategy = org.mapstruct.NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "chauffeur.id", target = "chauffeurId")
    @Mapping(source = "flowType", target = "flowType")
    @Mapping(source = "validationStatus", target = "validationStatus")
    @Mapping(source = "createdAt", target = "createdAt")
    RideRequestDTO toDto(RideRequest rideRequest);
    
    default java.time.LocalDateTime mapCreatedAt(java.time.LocalDateTime createdAt) {
        return createdAt != null ? createdAt : java.time.LocalDateTime.now();
    }

    @Mapping(target = "client.id", source = "clientId")
    @Mapping(target = "supplier", ignore = true) // CRITICAL: P2P model - supplier is not linked, handled manually in service
    @Mapping(target = "chauffeur", ignore = true) // CRITICAL: Handled manually in service to prevent TransientPropertyValueException
    @Mapping(target = "flowType", ignore = true) // Handled manually in service
    @Mapping(target = "validationStatus", ignore = true) // Handled manually in service
    @Mapping(target = "qrCodePickup", ignore = true) // Generated in service
    @Mapping(target = "qrCodeDelivery", ignore = true) // Generated in service
    RideRequest toEntity(RideRequestDTO dto);
    
    default String map(FlowType flowType) {
        return flowType != null ? flowType.name() : null;
    }
    
    default String map(ValidationStatus validationStatus) {
        return validationStatus != null ? validationStatus.name() : null;
    }
}
