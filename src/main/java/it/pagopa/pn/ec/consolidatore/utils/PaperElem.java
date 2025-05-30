package it.pagopa.pn.ec.consolidatore.utils;

import java.util.List;
import java.util.Map;

public class PaperElem {
	
	private PaperElem() {
	}
	
	// Eventi relativi alla fase di "Stampa/Postalizzazione"
    public static final String CON020 = "CON020";
	public static final String CON998 = "CON998";
	public static final String CON997 = "CON997";
	public static final String CON996 = "CON996";
	public static final String CON995 = "CON995";
	public static final String CON080 = "CON080";
	public static final String CON993 = "CON993";
	public static final String CON010 = "CON010";
	public static final String CON011 = "CON011";
	public static final String CON012 = "CON012";
	public static final String CON992 = "CON992";
	public static final String CON09A = "CON09A";
	public static final String CON016 = "CON016";
	public static final String CON018 = "CON018";
	public static final String CON991 = "CON991";
	// Eventi relativi alla fase di "Recapito"
	public static final String RECRS010 = "RECRS010";
	public static final String RECRS011 = "RECRS011";
	public static final String RECRS001C = "RECRS001C";
    public static final String RECRS002A = "RECRS002A";
    public static final String RECRS002B = "RECRS002B";
    public static final String RECRS002C = "RECRS002C";
    public static final String RECRS002D = "RECRS002D";
    public static final String RECRS002E = "RECRS002E";
    public static final String RECRS002F = "RECRS002F";
    public static final String RECRS003C = "RECRS003C";
    public static final String RECRS004A = "RECRS004A";
    public static final String RECRS004B = "RECRS004B";
    public static final String RECRS004C = "RECRS004C";
    public static final String RECRS005A = "RECRS005A";
    public static final String RECRS005B = "RECRS005B";
    public static final String RECRS005C = "RECRS005C";
    public static final String RECRS006 = "RECRS006";
    public static final String RECRN010 = "RECRN010";
    public static final String RECRN011 = "RECRN011";
    public static final String RECRN001A = "RECRN001A";
    public static final String RECRN001B = "RECRN001B";
    public static final String RECRN001C = "RECRN001C";
    public static final String RECRN002A = "RECRN002A";
    public static final String RECRN002B = "RECRN002B";
    public static final String RECRN002C = "RECRN002C";
    public static final String RECRN002D = "RECRN002D";
    public static final String RECRN002E = "RECRN002E";
    public static final String RECRN002F = "RECRN002F";
    public static final String RECRN003A = "RECRN003A";
    public static final String RECRN003B = "RECRN003B";
    public static final String RECRN003C = "RECRN003C";
    public static final String RECRN004A = "RECRN004A";
    public static final String RECRN004B = "RECRN004B";
    public static final String RECRN004C = "RECRN004C";
    public static final String RECRN005A = "RECRN005A";
    public static final String RECRN005B = "RECRN005B";
    public static final String RECRN005C = "RECRN005C";
    public static final String RECRN006 = "RECRN006";
    public static final String RECAG010 = "RECAG010";
    public static final String RECAG011A = "RECAG011A";
    public static final String RECAG011B = "RECAG011B";
    public static final String RECAG012 = "RECAG012";
    public static final String RECAG001A = "RECAG001A";
    public static final String RECAG001B = "RECAG001B";
    public static final String RECAG001C = "RECAG001C";
    public static final String RECAG002A = "RECAG002A";
    public static final String RECAG002B = "RECAG002B";
    public static final String RECAG002C = "RECAG002C";
    public static final String RECAG003A = "RECAG003A";
    public static final String RECAG003B = "RECAG003B";
    public static final String RECAG003C = "RECAG003C";
    public static final String RECAG003D = "RECAG003D";
    public static final String RECAG003E = "RECAG003E";
    public static final String RECAG003F = "RECAG003F";
    public static final String RECAG004 = "RECAG004";
    public static final String RECAG005A = "RECAG005A";
    public static final String RECAG005B = "RECAG005B";
    public static final String RECAG005C = "RECAG005C";
    public static final String RECAG006A = "RECAG006A";
    public static final String RECAG006B = "RECAG006B";
    public static final String RECAG006C = "RECAG006C";
    public static final String RECAG007A = "RECAG007A";
    public static final String RECAG007B = "RECAG007B";
    public static final String RECAG007C = "RECAG007C";
    public static final String RECAG008A = "RECAG008A";
    public static final String RECAG008B = "RECAG008B";
    public static final String RECAG008C = "RECAG008C";
    public static final String RECRI001 = "RECRI001";
    public static final String RECRI002 = "RECRI002";
    public static final String RECRI003A = "RECRI003A";
    public static final String RECRI003B = "RECRI003B";
    public static final String RECRI003C = "RECRI003C";
    public static final String RECRI004A = "RECRI004A";
    public static final String RECRI004B = "RECRI004B";
    public static final String RECRI004C = "RECRI004C";
    public static final String RECRI005 = "RECRI005";
    public static final String RECRSI001 = "RECRSI001";
    public static final String RECRSI002 = "RECRSI002";
    public static final String RECRSI003C = "RECRSI003C";
    public static final String RECRSI004A = "RECRSI004A";
    public static final String RECRSI004B = "RECRSI004B";
    public static final String RECRSI004C = "RECRSI004C";
    public static final String RECRSI005 = "RECRSI005";
    public static final String REC090 = "REC090";
    public static final String RECRS013 = "RECRS013";
    public static final String RECRS015 = "RECRS015";
    public static final String RECRN013 = "RECRN013";
    public static final String RECRN015 = "RECRN015";
    public static final String RECAG013 = "RECAG013";
    public static final String RECAG015 = "RECAG015";

	public static final String MANCATA_CONSEGNA_PRESSO_PUNTI_DI_GIACENZA_FASCICOLO_CHIUSO = "Mancata consegna presso Punti di Giacenza - Fascicolo Chiuso";
	public static final String IRREPERIBILITA_ASSOLUTA_FASCICOLO_CHIUSO = "Irreperibilità Assoluta - Fascicolo Chiuso";
	public static final String CONSEGNATO_IN_DEMATERIALIZZAZIONE = "Consegnato - In Dematerializzazione";
	public static final String CONSEGNATO_FASCICOLO_CHIUSO = "Consegnato - Fascicolo Chiuso";
	public static final String COMPIUTA_GIACENZA_FASCICOLO_CHIUSO = "Compiuta giacenza - Fascicolo Chiuso";
	public static final String CONSEGNATO_PRE_ESITO = "Consegnato - pre-esito";
	public static final String FURTO_SMARRIMANTO_DETERIORAMENTO = "Furto/Smarrimanto/deterioramento";
	public static final String IRREPERIBILITA_ASSOLUTA_PRE_ESITO = "Irreperibilità Assoluta - pre-esito";
	public static final String CONSEGNATO_PRESSO_PUNTI_DI_GIACENZA_FASCICOLO_CHIUSO = "Consegnato presso Punti di Giacenza - Fascicolo Chiuso";
	public static final String INESITO = "Inesito";
	public static final String MANCATA_CONSEGNA_IN_DEMATERIALIZZAZIONE = "Mancata consegna - In Dematerializzazione";
	public static final String MANCATA_CONSEGNA_PRESSO_PUNTI_DI_GIACENZA_PRE_ESITO = "Mancata consegna presso Punti di Giacenza - pre-esito";
	public static final String MANCATA_CONSEGNA_PRE_ESITO = "Mancata consegna - pre-esito";
	public static final String IRREPERIBILITA_ASSOLUTA_IN_DEMATERIALIZZAZIONE = "Irreperibilità Assoluta - In Dematerializzazione";
	public static final String MANCATA_CONSEGNA_PRESSO_PUNTI_DI_GIACENZA_IN_DEMATERIALIZZAZIONE = "Mancata consegna presso Punti di Giacenza - In Dematerializzazione";
	public static final String IN_GIACENZA = "In giacenza";
	public static final String MANCATA_CONSEGNA_FASCICOLO_CHIUSO = "Mancata consegna - Fascicolo Chiuso";
	public static final String COMPIUTA_GIACENZA_IN_DEMATERIALIZZAZIONE = "Compiuta giacenza - In Dematerializzazione";
	public static final String COMPIUTA_GIACENZA_PRE_ESITO = "Compiuta giacenza pre-esito";
	private static final Map<String, String> statusCodeDescriptionMap = Map.ofEntries(
							// Eventi relativi alla fase di "Stampa/Postalizzazione"
							Map.entry(CON998,"Scartato NODOC"),
							Map.entry(CON020,"Affido conservato"),
							Map.entry(CON997,"Scartato CAP/INTERNAZIONALE"),
							Map.entry(CON996,"Scartato PDF"),
							Map.entry(CON995,"Errore Stampa"),
							Map.entry(CON080,"Stampato ed Imbustato"),
							Map.entry(CON993,"Errore Stampa - parziale"),
							Map.entry(CON010,"Distinta Elettronica inviata a Recapitista"),
							Map.entry(CON011,"Distinta Elettronica Sigillata"),
							Map.entry(CON012,"OK Distinta Elettronica da Recapitista"),
							Map.entry(CON992,"KO Distinta Elettronica da Recapitista"),
							Map.entry(CON09A,"Materialità Pronta"),
							Map.entry(CON016,"PICKUP Sigillata"),
							Map.entry(CON018,"Accettazione Recapitista"),
							Map.entry(CON991,"Mancata Accetazione Recapitsita"),
							// Eventi relativi alla fase di "Recapito"
							Map.entry(RECRS010, INESITO),
							Map.entry(RECRS011, IN_GIACENZA),
							Map.entry(RECRS001C, CONSEGNATO_FASCICOLO_CHIUSO),
				            Map.entry(RECRS002A, MANCATA_CONSEGNA_PRE_ESITO),
				            Map.entry(RECRS002B, MANCATA_CONSEGNA_IN_DEMATERIALIZZAZIONE),
				            Map.entry(RECRS002C, MANCATA_CONSEGNA_FASCICOLO_CHIUSO),
				            Map.entry(RECRS002D, IRREPERIBILITA_ASSOLUTA_PRE_ESITO),
				            Map.entry(RECRS002E, IRREPERIBILITA_ASSOLUTA_IN_DEMATERIALIZZAZIONE),
				            Map.entry(RECRS002F, IRREPERIBILITA_ASSOLUTA_FASCICOLO_CHIUSO),
				            Map.entry(RECRS003C, CONSEGNATO_PRESSO_PUNTI_DI_GIACENZA_FASCICOLO_CHIUSO),
				            Map.entry(RECRS004A, MANCATA_CONSEGNA_PRESSO_PUNTI_DI_GIACENZA_PRE_ESITO),
				            Map.entry(RECRS004B, MANCATA_CONSEGNA_PRESSO_PUNTI_DI_GIACENZA_IN_DEMATERIALIZZAZIONE),
				            Map.entry(RECRS004C, MANCATA_CONSEGNA_PRESSO_PUNTI_DI_GIACENZA_FASCICOLO_CHIUSO),
				            Map.entry(RECRS005A, COMPIUTA_GIACENZA_PRE_ESITO),
				            Map.entry(RECRS005B, COMPIUTA_GIACENZA_IN_DEMATERIALIZZAZIONE),
				            Map.entry(RECRS005C, COMPIUTA_GIACENZA_FASCICOLO_CHIUSO),
				            Map.entry(RECRS006, FURTO_SMARRIMANTO_DETERIORAMENTO),
				            Map.entry(RECRN010, INESITO),
				            Map.entry(RECRN011, IN_GIACENZA),
				            Map.entry(RECRN001A, CONSEGNATO_PRE_ESITO),
				            Map.entry(RECRN001B, CONSEGNATO_IN_DEMATERIALIZZAZIONE),
				            Map.entry(RECRN001C, CONSEGNATO_FASCICOLO_CHIUSO),
				            Map.entry(RECRN002A, MANCATA_CONSEGNA_PRE_ESITO),
				            Map.entry(RECRN002B, MANCATA_CONSEGNA_IN_DEMATERIALIZZAZIONE),
				            Map.entry(RECRN002C, MANCATA_CONSEGNA_FASCICOLO_CHIUSO),
				            Map.entry(RECRN002D, IRREPERIBILITA_ASSOLUTA_PRE_ESITO),
				            Map.entry(RECRN002E, IRREPERIBILITA_ASSOLUTA_IN_DEMATERIALIZZAZIONE),
				            Map.entry(RECRN002F, IRREPERIBILITA_ASSOLUTA_FASCICOLO_CHIUSO),
				            Map.entry(RECRN003A,"Consegnato presso Punti di Giacenza - pre-esito"),
				            Map.entry(RECRN003B,"Consegnato presso Punti di Giacenza - In Dematerializzazione"),
				            Map.entry(RECRN003C, CONSEGNATO_PRESSO_PUNTI_DI_GIACENZA_FASCICOLO_CHIUSO),
				            Map.entry(RECRN004A, MANCATA_CONSEGNA_PRESSO_PUNTI_DI_GIACENZA_PRE_ESITO),
				            Map.entry(RECRN004B, MANCATA_CONSEGNA_PRESSO_PUNTI_DI_GIACENZA_IN_DEMATERIALIZZAZIONE),
				            Map.entry(RECRN004C, MANCATA_CONSEGNA_PRESSO_PUNTI_DI_GIACENZA_FASCICOLO_CHIUSO),
				            Map.entry(RECRN005A, COMPIUTA_GIACENZA_PRE_ESITO),
				            Map.entry(RECRN005B, COMPIUTA_GIACENZA_IN_DEMATERIALIZZAZIONE),
				            Map.entry(RECRN005C, COMPIUTA_GIACENZA_FASCICOLO_CHIUSO),
				            Map.entry(RECRN006, FURTO_SMARRIMANTO_DETERIORAMENTO),
				            Map.entry(RECAG010, INESITO),
				            Map.entry(RECAG011A, IN_GIACENZA),
				            Map.entry(RECAG011B,"In giacenza - In Dematerializzazione"),
				            Map.entry(RECAG012,"Accettazione 23L"),
				            Map.entry(RECAG001A, CONSEGNATO_PRE_ESITO),
				            Map.entry(RECAG001B, CONSEGNATO_IN_DEMATERIALIZZAZIONE),
				            Map.entry(RECAG001C, CONSEGNATO_FASCICOLO_CHIUSO),
				            Map.entry(RECAG002A,"Consegnato a persona abilitata - pre-esito"),
				            Map.entry(RECAG002B,"Consegnato a persona abilitata - In Dematerializzazione"),
				            Map.entry(RECAG002C,"Consegnato a persona abilitata - Fascicolo Chiuso"),
				            Map.entry(RECAG003A, MANCATA_CONSEGNA_PRE_ESITO),
				            Map.entry(RECAG003B, MANCATA_CONSEGNA_IN_DEMATERIALIZZAZIONE),
				            Map.entry(RECAG003C, MANCATA_CONSEGNA_FASCICOLO_CHIUSO),
				            Map.entry(RECAG003D, IRREPERIBILITA_ASSOLUTA_PRE_ESITO),
				            Map.entry(RECAG003E, IRREPERIBILITA_ASSOLUTA_IN_DEMATERIALIZZAZIONE),
				            Map.entry(RECAG003F, IRREPERIBILITA_ASSOLUTA_FASCICOLO_CHIUSO),
				            Map.entry(RECAG004, FURTO_SMARRIMANTO_DETERIORAMENTO),
				            Map.entry(RECAG005A,"Consegnato presso Punti di Giacenza - pre-esito"),
				            Map.entry(RECAG005B,"Consegnato presso Punti di Giacenza - In Dematerializzazione"),
				            Map.entry(RECAG005C, CONSEGNATO_PRESSO_PUNTI_DI_GIACENZA_FASCICOLO_CHIUSO),
				            Map.entry(RECAG006A,"Consegna a persona abilitata presso Punti di Giacenza - pre-esito"),
				            Map.entry(RECAG006B,"Consegna a persona abilitata presso Punti di Giacenza - In Dematerializzazione"),
				            Map.entry(RECAG006C,"Consegna a persona abilitata presso Punti di Giacenza - Fascicolo Chiuso"),
				            Map.entry(RECAG007A, MANCATA_CONSEGNA_PRESSO_PUNTI_DI_GIACENZA_PRE_ESITO),
				            Map.entry(RECAG007B, MANCATA_CONSEGNA_PRESSO_PUNTI_DI_GIACENZA_IN_DEMATERIALIZZAZIONE),
				            Map.entry(RECAG007C, MANCATA_CONSEGNA_PRESSO_PUNTI_DI_GIACENZA_FASCICOLO_CHIUSO),
				            Map.entry(RECAG008A,"Compiuta giacenza - pre-esito"),
				            Map.entry(RECAG008B, COMPIUTA_GIACENZA_IN_DEMATERIALIZZAZIONE),
				            Map.entry(RECAG008C, COMPIUTA_GIACENZA_FASCICOLO_CHIUSO),
				            Map.entry(RECRI001,"Avviato all'estero"),
				            Map.entry(RECRI002,"Ingresso nel paese estero"),
				            Map.entry(RECRI003A, CONSEGNATO_PRE_ESITO),
				            Map.entry(RECRI003B, CONSEGNATO_IN_DEMATERIALIZZAZIONE),
				            Map.entry(RECRI003C, CONSEGNATO_FASCICOLO_CHIUSO),
				            Map.entry(RECRI004A,"Non Consegnato - pre-esito"),
				            Map.entry(RECRI004B,"Non Consegnato - In Dematerializzazione"),
				            Map.entry(RECRI004C,"Non Consegnato - fascicolo Chiuso"),
				            Map.entry(RECRI005, FURTO_SMARRIMANTO_DETERIORAMENTO),
				            Map.entry(RECRSI001,"Avviato all'estero"),
				            Map.entry(RECRSI002,"Ingresso nel paese estero"),
				            Map.entry(RECRSI003C, CONSEGNATO_FASCICOLO_CHIUSO),
				            Map.entry(RECRSI004A,"Non Consegnato - pre-esito"),
				            Map.entry(RECRSI004B,"Non Consegnato - In Dematerializzazione"),
				            Map.entry(RECRSI004C,"Non Consegnato - fascicolo Chiuso"),
				            Map.entry(RECRSI005, FURTO_SMARRIMANTO_DETERIORAMENTO),
				            Map.entry(REC090,"Archiviazione fisica materialità di ritorno"),
				            Map.entry(RECRS013,"Non rendicontabile"),
				            Map.entry(RECRS015,"Causa di forza maggiore"),
				            Map.entry(RECRN013,"Non Rendicontabile"),
				            Map.entry(RECRN015,"Causa Forza Maggiore"),
				            Map.entry(RECAG013,"Non Rendicontabile"),
				            Map.entry(RECAG015,"Causa Forza Maggiore")
						);
	
	public static Map<String, String> statusCodeDescriptionMap() {
		return statusCodeDescriptionMap;
	}
	
	public static final String PRODUCT_TYPE_AR = "AR";
	public static final String PRODUCT_TYPE_890 = "890";
	public static final String PRODUCT_TYPE_RS = "RS";
	public static final String PRODUCT_TYPE_RIS = "RIS";
	public static final String PRODUCT_TYPE_RIR = "RIR";
	
	private static final Map<String, String> productTypeMap = Map.ofEntries(
			Map.entry("AR","Raccomandata Andata e Ritorno nazionale"),
			Map.entry("890","Recapito a norma della legge 890/1982"),
			Map.entry("RS","Raccomandata Semplice nazionale (per Avviso di mancato Recapito)"),
			Map.entry("RIS","Raccomandata Internazionale Semplice"),
			Map.entry("RIR","Raccomandata Internazionale con AR"));
	
	public static Map<String, String> productTypeMap() {
		return productTypeMap;
	}

	private static final Map<String, String> deliveryFailureCauseMap = Map.ofEntries(
			Map.entry("M01","destinatario irreperibile"),
			Map.entry("M02","destinatario deceduto"),
			Map.entry("M03","destinatario sconosciuto"),
			Map.entry("M04","destinatario trasferito"),
			Map.entry("M05","invio rifiutato"),
			Map.entry("M06","indirizzo inesatto"),
			Map.entry("M07","indirizzo inesistente"),
			Map.entry("M08","indirizzo insufficiente"),
			Map.entry("M09","altre motivazioni"),
			Map.entry("F01","in caso di furto"),
			Map.entry("F02","in caso di smarrimento"),
			Map.entry("F03","in caso di deterioramento"),
			Map.entry("F04","in caso di rapina"),
			Map.entry("C01","incendio"),
			Map.entry("C02","strada chiusa per lavori in corso o frana"),
			Map.entry("C03","strada chiusa dalle autorità per eventi eccezionali"),
			Map.entry("C04","maltempo: Alluvione, Neve, Allagamento"),
			Map.entry("C05","terremoto"),
			Map.entry("C06","eruzione vulcanica"));
	
	public static Map<String, String> deliveryFailureCausemap() {
		return deliveryFailureCauseMap;
	}
	
	public static final String ATTACHMENT_DOCUMENT_TYPE_23L = "23L";
	public static final String ATTACHMENT_DOCUMENT_TYPE_ARCAD = "ARCAD";
	public static final String ATTACHMENT_DOCUMENT_TYPE_CAD = "CAD";
	public static final String ATTACHMENT_DOCUMENT_TYPE_CAN = "CAN";
	public static final String ATTACHMENT_DOCUMENT_TYPE_PLICO = "Plico";
	public static final String ATTACHMENT_DOCUMENT_TYPE_INDAGINE = "Indagine";
	public static final String ATTACHMENT_DOCUMENT_TYPE_AR = "AR";
	
	private static final List<String> attachmentDocumentTypeMap = List.of(
			ATTACHMENT_DOCUMENT_TYPE_23L, 
			ATTACHMENT_DOCUMENT_TYPE_ARCAD,
			ATTACHMENT_DOCUMENT_TYPE_CAD,
			ATTACHMENT_DOCUMENT_TYPE_CAN,
			ATTACHMENT_DOCUMENT_TYPE_PLICO,
			ATTACHMENT_DOCUMENT_TYPE_INDAGINE,
			ATTACHMENT_DOCUMENT_TYPE_AR);

	public static List<String> attachmentDocumentTypeMap() {
		return attachmentDocumentTypeMap;
	}
	
}
