package it.pagopa.pn.ec.spedizionedocumenticartacei;


import it.pagopa.pn.ec.rest.v1.dto.PaperEngageRequestAttachments;

public class AllegatiImpegnoRichiestaCartaceo {
	
		protected PaperEngageRequestAttachments papEngReqAtt;

		public PaperEngageRequestAttachments getPapEngReqAtt() {
			return papEngReqAtt;
		}

		public void setPapEngReqAtt(PaperEngageRequestAttachments papEngReqAtt) {
			this.papEngReqAtt = papEngReqAtt;
		}

		@Override
		public String toString() {
			return "AllegatiImpegnoRichiestaCartaceo [papEngReqAtt=" + papEngReqAtt + "]";
		}
		
}
