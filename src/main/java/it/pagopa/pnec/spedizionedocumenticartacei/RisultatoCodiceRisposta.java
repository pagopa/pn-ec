package it.pagopa.pnec.spedizionedocumenticartacei;

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
	
//	public String toJson() {
//		JSONObject json = new JSONObject(this);
//		return json.toString();
//	}
//	
//	public String toJson(int indent) {
//		JSONObject json = new JSONObject(this);
//		return json.toString(indent);
//	}

}
