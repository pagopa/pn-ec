package it.pagopa.pnec.spedizionedocumenticartacei;

import org.openapitools.client.model.PaperDeliveryProgressesResponse;

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
