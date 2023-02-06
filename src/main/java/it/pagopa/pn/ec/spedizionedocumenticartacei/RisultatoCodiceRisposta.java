package it.pagopa.pn.ec.spedizionedocumenticartacei;

import org.openapitools.client.model.OperationResultCodeResponse;

public class RisultatoCodiceRisposta {

	protected OperationResultCodeResponse opResCodeResp;

	public OperationResultCodeResponse getOpResCodeResp() {
		return opResCodeResp;
	}

	public void setOpResCodeResp(OperationResultCodeResponse opResCodeResp) {
		this.opResCodeResp = opResCodeResp;
	}

	@Override
	public String toString() {
		return "RisultatoCodiceRisposta [opResCodeResp=" + opResCodeResp + "]";
	}
}
