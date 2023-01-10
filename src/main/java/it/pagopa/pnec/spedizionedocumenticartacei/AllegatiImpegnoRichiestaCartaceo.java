package it.pagopa.pnec.spedizionedocumenticartacei;

import org.openapitools.client.model.PaperEngageRequestAttachments;

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
