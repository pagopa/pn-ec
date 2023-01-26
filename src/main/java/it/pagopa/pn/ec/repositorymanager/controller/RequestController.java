package it.pagopa.pn.ec.repositorymanager.controller;

import it.pagopa.pn.ec.repositorymanager.dto.RequestDto;
import it.pagopa.pn.ec.repositorymanager.dto.UpdatedEventDto;
import it.pagopa.pn.ec.repositorymanager.service.RepositoryManagerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@RestController
public class RequestController {

	private final RepositoryManagerService repositoryManagerService;

	public RequestController(RepositoryManagerService repositoryManagerService) {
		this.repositoryManagerService = repositoryManagerService;
	}

	@PostMapping(path ="/requests" ,produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<RequestDto>> insertRequest(@Valid @RequestBody RequestDto reqDtoI) {
		RequestDto reqDtoO = repositoryManagerService.insertRequest(reqDtoI);
		return Mono.just(ResponseEntity.ok().body(reqDtoO));
	}
	
	@GetMapping(path ="/requests/{requestId}" ,produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<RequestDto>> getRequest(@PathVariable String requestId) {
		RequestDto reqDto = repositoryManagerService.getRequest(requestId);
		return Mono.just(ResponseEntity.ok().body(reqDto));
	}
	
	@PatchMapping(path ="/requests/{requestId}" ,produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<RequestDto>> updateRequest(@PathVariable String requestId, @Valid @RequestBody UpdatedEventDto updatedEventDtoI) {
		RequestDto reqDtoO = repositoryManagerService.updateRequest(requestId, updatedEventDtoI);
		return Mono.just(ResponseEntity.ok().body(reqDtoO));
	}
	
	@DeleteMapping(path ="/requests/{requestId}" ,produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<Void>> deleteRequest(@PathVariable String requestId) {
		repositoryManagerService.deleteRequest(requestId);
		return Mono.just(new ResponseEntity<>(HttpStatus.OK));
	}
}
