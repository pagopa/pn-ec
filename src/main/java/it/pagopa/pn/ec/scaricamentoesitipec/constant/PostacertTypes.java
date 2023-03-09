package it.pagopa.pn.ec.scaricamentoesitipec.constant;

public final class PostacertTypes {

    private PostacertTypes() {
        throw new IllegalStateException("PostacertTypes is a final class");
    }

    public static final String PRESA_IN_CARICO = "presa-in-carico";
    public static final String ACCETTAZIONE = "accettazione";
    public static final String NON_ACCETTAZIONE = "non-accettazione";
    public static final String AVVENUTA_CONSEGNA = "avvenuta-consegna";
    public static final String RILEVAZIONE_VIRUS = "rilevazione-virus";
    public static final String ERRORE_CONSEGNA = "errore-consegna";
    public static final String PREAVVISO_ERRORE_CONSEGNA = "preavviso-errore-consegna";
    public static final String POSTA_CERTIFICATA = "posta-certificata";
}
