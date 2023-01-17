package it.pagopa.pnec.repositorymanager.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.pagopa.pnec.repositorymanager.dto.RequestDto;
import it.pagopa.pnec.repositorymanager.model.Request;
import it.pagopa.pnec.repositorymanager.service.RepositoryManagerService;
import reactor.core.publisher.Mono;

@RestController
public class RequestController {

	RepositoryManagerService rms = new RepositoryManagerService();
	
	@PostMapping(path ="/request" ,produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<RequestDto>> insertRequest(@RequestBody Request r) {
		RequestDto reqDto = rms.insertRequest(r);
		Mono<ResponseEntity<RequestDto>> result = Mono.just(ResponseEntity.ok().body(reqDto));
		return result;
	}
	
	@GetMapping(path ="/request/{requestId}" ,produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<RequestDto>> getRequest(@RequestParam("requestId") String requestId) {
		RequestDto reqDto = rms.getRequest(requestId);
		Mono<ResponseEntity<RequestDto>> result = Mono.just(ResponseEntity.ok().body(reqDto));
		return result;
	}
	
	@PatchMapping(path ="/request/{requestId}" ,produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<RequestDto>> updateRequest(@RequestBody Request r) {
		RequestDto reqDto = rms.updateRequest(r);
		Mono<ResponseEntity<RequestDto>> result = Mono.just(ResponseEntity.ok().body(reqDto));
		return result;
	}
	
	@DeleteMapping(path ="/request/{requestId}" ,produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<RequestDto>> deleteRequest(@RequestParam("requestId") String requestId) {
		RequestDto reqDto = rms.deleteRequest(requestId);
		Mono<ResponseEntity<RequestDto>> result = Mono.just(ResponseEntity.ok().body(reqDto));
		return result;
	}
}
