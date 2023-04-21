package it.pagopa.pn.ec.spedizionedocumenticartacei;


import it.pagopa.pn.ec.rest.v1.dto.PaperDeliveryProgressesResponse;

public class ProgressiviConsegnaRispostaCartacea {

	protected PaperDeliveryProgressesResponse papDelProgrResp;

	public PaperDeliveryProgressesResponse getPapDelProgrResp() {
		return papDelProgrResp;
	}

	public void setPapDelProgrResp(PaperDeliveryProgressesResponse papDelProgrResp) {
		this.papDelProgrResp = papDelProgrResp;
	}
	@Override
	public String toString() {
		return "ProgressiviConsegnaRispostaCartacea [papDelProgrResp=" + papDelProgrResp + "]";
	}
	
}
