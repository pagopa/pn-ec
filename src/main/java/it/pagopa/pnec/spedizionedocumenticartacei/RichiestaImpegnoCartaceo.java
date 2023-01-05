package it.pagopa.pnec.spedizionedocumenticartacei;

import org.openapitools.client.model.PaperEngageRequest;

public class RichiestaImpegnoCartaceo {
	
	protected PaperEngageRequest papEngReq;

	public PaperEngageRequest getPapEngReq() {
		return papEngReq;
	}

	public void setPapEngReq(PaperEngageRequest papEngReq) {
		this.papEngReq = papEngReq;
	}

	@Override
	public String toString() {
		return "RichiestaImpegnoCartaceo [papEngReq=" + papEngReq + "]";
	}
}
