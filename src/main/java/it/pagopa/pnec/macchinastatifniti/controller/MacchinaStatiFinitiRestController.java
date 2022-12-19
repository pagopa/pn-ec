package it.pagopa.pnec.macchinastatifniti.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import it.pagopa.pnec.macchinastatifniti.model.Parametri;
import it.pagopa.pnec.macchinastatifniti.model.StatoRequest;
import it.pagopa.pnec.macchinastatifniti.service.StatoService;


@RestController
public class MacchinaStatiFinitiRestController {

//	  @Autowired
//	  StatoService  signService;
//
//    
//
//    @PostMapping(path ="/" ,produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity <StatoRequest> createStaterecord(@RequestBody Parametri requestParametri) {
//    	StatoRequest response= signService.createStateRecord(requestParametri);
//		
//		return ResponseEntity.ok()
//                .body(response);
//
//    }
// 
//	@GetMapping(path ="/",produces = MediaType.APPLICATION_JSON_VALUE )
//    public ResponseEntity<String> getIdstato( @RequestParam(name ="id") String id ) throws Exception {  
//		
//		String response= signService.getIdStato(id);
//		
//		return ResponseEntity.ok()
//                .body(response);
//    }
//
//
//	
//	 @PutMapping(path ="/{id}" ,produces = MediaType.APPLICATION_JSON_VALUE )
////	 public Stato updateStato(@PathVariable String id, @RequestBody Parametri requestParametri)    {	
//	 public  ResponseEntity<String> updateStato(@PathVariable String id)    {
//		 String response= signService.updateState(id);
//			
//			return ResponseEntity.ok()
//	                .body(response);	 
//	}
//	 
//	 @PutMapping(path ="/{id}" ,produces = MediaType.APPLICATION_JSON_VALUE )
////	 public Stato insertStatoFinale(@PathVariable String id, @RequestBody Parametri requestParametri)    {		
//	 public  ResponseEntity<String> insertStatoFinal( @RequestParam(name ="id") String id)    {	
//		 String response= signService.insertFinalState(id);
//			
//			return ResponseEntity.ok()
//	                .body(response);
//	}
	

}
