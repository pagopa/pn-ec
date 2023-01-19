package it.pagopa.pnec.repositorymanager.controller;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.pagopa.pnec.repositorymanager.dto.ClientConfigurationDto;
import it.pagopa.pnec.repositorymanager.model.ClientConfiguration;
import it.pagopa.pnec.repositorymanager.service.RepositoryManagerService;
import reactor.core.publisher.Mono;

@RestController
public class ClientConfigurationController {

	@Autowired
	RepositoryManagerService rms = new RepositoryManagerService();
	
	@PostMapping(path ="/client" ,produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<ClientConfigurationDto>> insertClient(@Valid @RequestBody ClientConfigurationDto ccDtoI) { 
		ClientConfigurationDto ccDtoO = rms.insertClient(ccDtoI);
		Mono<ResponseEntity<ClientConfigurationDto>> result = Mono.just(ResponseEntity.ok().body(ccDtoO));
		return result;
	}
	
	@GetMapping(path ="/client/{cxId}" ,produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<ClientConfigurationDto>> getClient(@RequestParam("cxId") String cxId){
		ClientConfigurationDto clientConfDto = rms.getClient(cxId);
		Mono<ResponseEntity<ClientConfigurationDto>> result = Mono.just(ResponseEntity.ok().body(clientConfDto));
		return result;
	}
	
	@PutMapping(path ="/client/{cxId}" ,produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<ClientConfigurationDto>> updateClient(@RequestBody ClientConfiguration cci) { 
		ClientConfigurationDto clientConfDto = rms.updateClient(cci);
		Mono<ResponseEntity<ClientConfigurationDto>> result = Mono.just(ResponseEntity.ok().body(clientConfDto));
		return result;
	}

	@DeleteMapping(path ="/client/{cxId}" ,produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<ClientConfigurationDto>> deleteClient(@RequestParam("cxId") String cxId){
		ClientConfigurationDto clientConfDto = rms.deleteClient(cxId);
		Mono<ResponseEntity<ClientConfigurationDto>> result = Mono.just(ResponseEntity.ok().body(clientConfDto));
		return result;
	}
	
}
