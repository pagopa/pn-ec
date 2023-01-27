package it.pagopa.pn.ec.repositorymanager.controller;

import it.pagopa.pn.ec.repositorymanager.service.RepositoryManagerService;
import it.pagopa.pn.ec.rest.v1.api.ConfigurazioneClientApi;
import it.pagopa.pn.ec.rest.v1.dto.ClientConfigurationDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class ClientConfigurationController implements ConfigurazioneClientApi {

    private final RepositoryManagerService repositoryManagerService;

    public ClientConfigurationController(RepositoryManagerService repositoryManagerService) {
        this.repositoryManagerService = repositoryManagerService;
    }

    @Override
    public Mono<ResponseEntity<ClientConfigurationDto>> insertClient(Mono<ClientConfigurationDto> clientConfigurationDto,
                                                                     ServerWebExchange exchange) {
        return clientConfigurationDto.flatMap(repositoryManagerService::insertClient)
                                     .flatMap(insertedClient -> Mono.just(ResponseEntity.ok().body(insertedClient)));
    }

    //	@PostMapping(path ="/clients" ,produces = MediaType.APPLICATION_JSON_VALUE)
//	public Mono<ResponseEntity<ClientConfigurationDto>> insertClient(@Valid @RequestBody ClientConfigurationDto ccDtoI) {
//		ClientConfigurationDto ccDtoO = repositoryManagerService.insertClient(ccDtoI);
//		return Mono.just(ResponseEntity.ok().body(ccDtoO));
//	}
//
//	@GetMapping(path ="/clients/{cxId}" ,produces = MediaType.APPLICATION_JSON_VALUE)
//	public Mono<ResponseEntity<ClientConfigurationDto>> getClient(@PathVariable String cxId){
//		ClientConfigurationDto clientConfDto = repositoryManagerService.getClient(cxId);
//		return Mono.just(ResponseEntity.ok().body(clientConfDto));
//	}
//
//	@PutMapping(path ="/clients/{cxId}" ,produces = MediaType.APPLICATION_JSON_VALUE)
//	public Mono<ResponseEntity<ClientConfigurationDto>> updateClient(@PathVariable String cxId, @Valid @RequestBody ClientConfigurationDto
//	ccDtoI) {
//		ClientConfigurationDto ccDtoO = repositoryManagerService.updateClient(cxId, ccDtoI);
//		return Mono.just(ResponseEntity.ok().body(ccDtoO));
//	}
//
//	@DeleteMapping(path ="/clients/{cxId}" ,produces = MediaType.APPLICATION_JSON_VALUE)
//	public Mono<ResponseEntity<Void>> deleteClient(@PathVariable String cxId){
//		repositoryManagerService.deleteClient(cxId);
//		return Mono.just(new ResponseEntity<>(HttpStatus.OK));
//	}
}
