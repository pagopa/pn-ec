package it.pagopa.pn.ec.commons.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.CustomLog;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@CustomLog
public class RestUtils {

    private final ObjectMapper objectMapper;

    public RestUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <E, D> E startCreateRequest(D dto, Class<E> entity) {
        return objectMapper.convertValue(dto, entity);
    }

    public <E, D> E startUpdateRequest(D dto, Class<E> entity) {
        return objectMapper.convertValue(dto, entity);
    }

    public <E, D> ResponseEntity<D> endCreateOrUpdateRequest(E entity, Class<D> dto) {
        return ResponseEntity.ok(objectMapper.convertValue(entity, dto));
    }

    public <E, D> ResponseEntity<D> endReadRequest(E entity, Class<D> dto) {
        return ResponseEntity.ok(objectMapper.convertValue(entity, dto));
    }

    public <E, D> ResponseEntity<D> endDeleteRequest(E entity, Class<D> dto) {
        return ResponseEntity.ok(objectMapper.convertValue(entity, dto));
    }

    public <E, D> D entityToDto(E entity, Class<D> dto) {
        return objectMapper.convertValue(entity, dto);
    }

}
