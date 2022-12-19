package it.pagopa.pnec.macchinastatifniti.model;

import lombok.Data;

@Data
public class Parametri {
	
	String idStato;
	String idMacchina;
	
	public String getIdStato() {
		return idStato;
	}
	public void setIdStato(String idStato) {
		this.idStato = idStato;
	}
	public String getIdMacchina() {
		return idMacchina;
	}
	public void setIdMacchina(String idMacchina) {
		this.idMacchina = idMacchina;
	}

	
	
}
