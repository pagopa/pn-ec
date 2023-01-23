package it.pagopa.pn.ec.spedizionedocumenticartacei;

import org.openapitools.client.model.PaperProgressStatusEvent;

public class ProgressivoStatoEventoCartaceo {

	protected PaperProgressStatusEvent papProgStEv;

	public PaperProgressStatusEvent getPapProgStEv() {
		return papProgStEv;
	}

	public void setPapProgStEv(PaperProgressStatusEvent papProgStEv) {
		this.papProgStEv = papProgStEv;
	}
	@Override
	public String toString() {
		return "ProgressivoStatoEventoCartaceo [papProgStEv=" + papProgStEv + "]";
	}

}
