package it.pagopa.pn.ec.repositorymanager.controller;

import it.pagopa.pn.ec.repositorymanager.dto.ClientConfigurationDto;
import it.pagopa.pn.ec.repositorymanager.service.RepositoryManagerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@RestController
public class ClientConfigurationController {

	private final RepositoryManagerService repositoryManagerService;

	public ClientConfigurationController(RepositoryManagerService repositoryManagerService) {
		this.repositoryManagerService = repositoryManagerService;
	}

	@PostMapping(path ="/clients" ,produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<ClientConfigurationDto>> insertClient(@Valid @RequestBody ClientConfigurationDto ccDtoI) {
		ClientConfigurationDto ccDtoO = repositoryManagerService.insertClient(ccDtoI);
		return Mono.just(ResponseEntity.ok().body(ccDtoO));
	}
	
	@GetMapping(path ="/clients/{cxId}" ,produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<ClientConfigurationDto>> getClient(@PathVariable String cxId){
		ClientConfigurationDto clientConfDto = repositoryManagerService.getClient(cxId);
		return Mono.just(ResponseEntity.ok().body(clientConfDto));
	}
	
	@PutMapping(path ="/clients/{cxId}" ,produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<ClientConfigurationDto>> updateClient(@PathVariable String cxId, @Valid @RequestBody ClientConfigurationDto ccDtoI) {
		ClientConfigurationDto ccDtoO = repositoryManagerService.updateClient(cxId, ccDtoI);
		return Mono.just(ResponseEntity.ok().body(ccDtoO));
	}

	@DeleteMapping(path ="/clients/{cxId}" ,produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<Void>> deleteClient(@PathVariable String cxId){
		repositoryManagerService.deleteClient(cxId);
		return Mono.just(new ResponseEntity<>(HttpStatus.OK));
	}
}
