package it.pagopa.pn.ec.commons.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RestUtils {

    private final ObjectMapper objectMapper;

    public RestUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <E, D> E startCreateRequest(D dto, Class<E> entity) {
        E convertedEntity = objectMapper.convertValue(dto, entity);
        log.info("Try to insert {} ↓\n{}", entity.getSimpleName(), convertedEntity);
        return convertedEntity;
    }

    public <E, D> E startUpdateRequest(D dto, Class<E> entity) {
        E convertedEntity = objectMapper.convertValue(dto, entity);
        log.info("Try to update {} ↓\n{}", entity.getSimpleName(), convertedEntity);
        return convertedEntity;
    }

    public <E, D> ResponseEntity<D> endCreateOrUpdateRequest(E entity, Class<D> dto) {
        return ResponseEntity.ok(objectMapper.convertValue(entity, dto));
    }

    public <E, D> ResponseEntity<D> endReadRequest(E entity, Class<D> dto) {
        log.info("Retrieved {} ↓\n{}", entity.getClass().getSimpleName(), entity);
        return ResponseEntity.ok(objectMapper.convertValue(entity, dto));
    }

    public <E, D> ResponseEntity<D> endDeleteRequest(E entity, Class<D> dto) {
        log.info("Deleted {} ↓\n{}", entity.getClass().getSimpleName(), entity);
        return ResponseEntity.ok(objectMapper.convertValue(entity, dto));
    }

    public <E, D> D dtoToEntity(E entity, Class<D> dto) {
        return objectMapper.convertValue(entity, dto);
    }
}
