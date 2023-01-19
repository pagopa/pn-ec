package it.pagopa.pn.ec.repositorymanager.controller;

import javax.validation.Valid;

import it.pagopa.pn.ec.repositorymanager.dto.ClientConfigurationDto;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import it.pagopa.pn.ec.repositorymanager.model.ClientConfiguration;
import it.pagopa.pn.ec.repositorymanager.service.RepositoryManagerService;
import reactor.core.publisher.Mono;

@RestController
public class ClientConfigurationController {

	private final RepositoryManagerService repositoryManagerService;

	public ClientConfigurationController(RepositoryManagerService repositoryManagerService) {
		this.repositoryManagerService = repositoryManagerService;
	}

	@PostMapping(path ="/client" ,produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<ClientConfigurationDto>> insertClient(@Valid @RequestBody ClientConfigurationDto ccDtoI) {
		ClientConfigurationDto ccDtoO = repositoryManagerService.insertClient(ccDtoI);
		Mono<ResponseEntity<ClientConfigurationDto>> result = Mono.just(ResponseEntity.ok().body(ccDtoO));
		return result;
	}
	
	@GetMapping(path ="/client/{cxId}" ,produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<ClientConfigurationDto>> getClient(@PathVariable String cxId){
		ClientConfigurationDto clientConfDto = repositoryManagerService.getClient(cxId);
		Mono<ResponseEntity<ClientConfigurationDto>> result = Mono.just(ResponseEntity.ok().body(clientConfDto));
		return result;
	}
	
	@PutMapping(path ="/client/{cxId}" ,produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<ClientConfigurationDto>> updateClient(@RequestBody ClientConfiguration cci) { 
		ClientConfigurationDto clientConfDto = repositoryManagerService.updateClient(cci);
		Mono<ResponseEntity<ClientConfigurationDto>> result = Mono.just(ResponseEntity.ok().body(clientConfDto));
		return result;
	}

	@DeleteMapping(path ="/client/{cxId}" ,produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<ClientConfigurationDto>> deleteClient(@RequestParam("cxId") String cxId){
		ClientConfigurationDto clientConfDto = repositoryManagerService.deleteClient(cxId);
		Mono<ResponseEntity<ClientConfigurationDto>> result = Mono.just(ResponseEntity.ok().body(clientConfDto));
		return result;
	}
}
