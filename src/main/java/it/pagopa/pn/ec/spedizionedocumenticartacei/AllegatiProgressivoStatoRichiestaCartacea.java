package it.pagopa.pn.ec.spedizionedocumenticartacei;


import it.pagopa.pn.ec.rest.v1.consolidatore.dto.PaperProgressStatusEventAttachments;

public class AllegatiProgressivoStatoRichiestaCartacea {

    protected PaperProgressStatusEventAttachments papProgStEvAtt;

    public PaperProgressStatusEventAttachments getPapProgStEvAtt() {
        return papProgStEvAtt;
    }

    public void setPapProgStEvAtt(PaperProgressStatusEventAttachments papProgStEvAtt) {
        this.papProgStEvAtt = papProgStEvAtt;
    }

    @Override
    public String toString() {
        return "AllegatiProgressivoStatoRichiestaCartacea [papProgStEvAtt=" + papProgStEvAtt + "]";
    }

}
