package it.pagopa.pn.ec.spedizionedocumenticartacei;


import it.pagopa.pn.ec.rest.v1.dto.PaperEngageRequest;

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
