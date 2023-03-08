package it.pagopa.pn.ec.cartaceo.utils;

import java.util.Map;

public class PaperElem {
	
	private PaperElem() {
	}
	
	public static Map<String, String> statusCodeDescriptionMap() {
		return statusCodeDescriptionMap;
	}
	
	private static final Map<String, String> statusCodeDescriptionMap = Map.ofEntries(
							// Eventi relativi alla fase di "Stampa/Postalizzazione"
							Map.entry("CON998","Scartato NODOC"),
							Map.entry("CON997","Scartato CAP/INTERNAZIONALE"),
							Map.entry("CON996","Scartato PDF"),
							Map.entry("CON995","Errore Stampa"),
							Map.entry("CON080","Stampato ed Imbustato"),
							Map.entry("CON993","Errore Stampa - parziale"),
							Map.entry("CON010","Distinta Elettronica inviata a Recapitista"),
							Map.entry("CON011","Distinta Elettronica Sigillata"),
							Map.entry("CON012","OK Distinta Elettronica da Recapitista"),
							Map.entry("CON992","KO Distinta Elettronica da Recapitista"),
							Map.entry("CON09A","Materialità Pronta"),
							Map.entry("CON016","PICKUP Sigillata"),
							Map.entry("CON018","Accettazione Recapitista"),
							Map.entry("CON991","Mancata Accetazione Recapitsita"),
							// Eventi relativi alla fase di "Recapito"
							Map.entry("RECRS010","Inesito"),
							Map.entry("RECRS011","In giacenza"),
							Map.entry("RECRS001C","Consegnato - Fascicolo Chiuso"), 
				            Map.entry("RECRS002A","Mancata consegna - pre-esito"),
				            Map.entry("RECRS002B","Mancata consegna - In Dematerializzazione"),
				            Map.entry("RECRS002C","Mancata consegna - Fascicolo Chiuso"),
				            Map.entry("RECRS002D","Irreperibilità Assoluta - pre-esito"),
				            Map.entry("RECRS002E","Irreperibilità Assoluta - In Dematerializzazione"),
				            Map.entry("RECRS002F","Irreperibilità Assoluta - Fascicolo Chiuso"),
				            Map.entry("RECRS003C","Consegnato presso Punti di Giacenza - Fascicolo Chiuso"),
				            Map.entry("RECRS004A","Mancata consegna presso Punti di Giacenza - pre-esito"),
				            Map.entry("RECRS004B","Mancata consegna presso Punti di Giacenza - In Dematerializzazione"),
				            Map.entry("RECRS004C","Mancata consegna presso Punti di Giacenza - Fascicolo Chiuso"),
				            Map.entry("RECRS005A","Compiuta giacenza pre-esito"),
				            Map.entry("RECRS005B","Compiuta giacenza - In Dematerializzazione"),
				            Map.entry("RECRS005C","Compiuta giacenza - Fascicolo Chiuso"),
				            Map.entry("RECRS006","Furto/Smarrimanto/deterioramento"),
				            Map.entry("RECRN010","Inesito"),
				            Map.entry("RECRN011","In giacenza"),
				            Map.entry("RECRN001A","Consegnato - pre-esito"),
				            Map.entry("RECRN001B","Consegnato - In Dematerializzazione"),
				            Map.entry("RECRN001C","Consegnato - Fascicolo Chiuso"),
				            Map.entry("RECRN002A","Mancata consegna - pre-esito"),
				            Map.entry("RECRN002B","Mancata consegna - In Dematerializzazione"),
				            Map.entry("RECRN002C","Mancata consegna - Fascicolo Chiuso"),
				            Map.entry("RECRN002D","Irreperibilità Assoluta - pre-esito"),
				            Map.entry("RECRN002E","Irreperibilità Assoluta - In Dematerializzazione"),
				            Map.entry("RECRN002F","Irreperibilità Assoluta - Fascicolo Chiuso"),
				            Map.entry("RECRN003A","Consegnato presso Punti di Giacenza - pre-esito"),
				            Map.entry("RECRN003B","Consegnato presso Punti di Giacenza - In Dematerializzazione"),
				            Map.entry("RECRN003C","Consegnato presso Punti di Giacenza - Fascicolo Chiuso"),
				            Map.entry("RECRN004A","Mancata consegna presso Punti di Giacenza - pre-esito"),
				            Map.entry("RECRN004B","Mancata consegna presso Punti di Giacenza - In Dematerializzazione"),
				            Map.entry("RECRN004C","Mancata consegna presso Punti di Giacenza - Fascicolo Chiuso"),
				            Map.entry("RECRN005A","Compiuta giacenza pre-esito"),
				            Map.entry("RECRN005B","Compiuta giacenza - In Dematerializzazione"),
				            Map.entry("RECRN005C","Compiuta giacenza - Fascicolo Chiuso"),
				            Map.entry("RECRN006","Furto/Smarrimanto/deterioramento"),
				            Map.entry("RECAG010","Inesito"),
				            Map.entry("RECAG011A","In giacenza"),
				            Map.entry("RECAG011B","In giacenza - In Dematerializzazione"),
				            Map.entry("RECAG012","Accettazione 23L"),
				            Map.entry("RECAG001A","Consegnato - pre-esito"),
				            Map.entry("RECAG001B","Consegnato - In Dematerializzazione"),
				            Map.entry("RECAG001C","Consegnato - Fascicolo Chiuso"),
				            Map.entry("RECAG002A","Consegnato a persona abilitata - pre-esito"),
				            Map.entry("RECAG002B","Consegnato a persona abilitata - In Dematerializzazione"),
				            Map.entry("RECAG002C","Consegnato a persona abilitata - Fascicolo Chiuso"),
				            Map.entry("RECAG003A","Mancata consegna - pre-esito"),
				            Map.entry("RECAG003B","Mancata consegna - In Dematerializzazione"),
				            Map.entry("RECAG003C","Mancata consegna - Fascicolo Chiuso"),
				            Map.entry("RECAG003D","Irreperibilità Assoluta - pre-esito"),
				            Map.entry("RECAG003E","Irreperibilità Assoluta - In Dematerializzazione"),
				            Map.entry("RECAG003F","Irreperibilità Assoluta - Fascicolo Chiuso"),
				            Map.entry("RECAG004","Furto/Smarrimanto/deterioramento"),
				            Map.entry("RECAG005A","Consegnato presso Punti di Giacenza - pre-esito"),
				            Map.entry("RECAG005B","Consegnato presso Punti di Giacenza - In Dematerializzazione"),
				            Map.entry("RECAG005C","Consegnato presso Punti di Giacenza - Fascicolo Chiuso"),
				            Map.entry("RECAG006A","Consegna a persona abilitata presso Punti di Giacenza - pre-esito"),
				            Map.entry("RECAG006B","Consegna a persona abilitata presso Punti di Giacenza - In Dematerializzazione"),
				            Map.entry("RECAG006C","Consegna a persona abilitata presso Punti di Giacenza - Fascicolo Chiuso"),
				            Map.entry("RECAG007A","Mancata consegna presso Punti di Giacenza - pre-esito"),
				            Map.entry("RECAG007B","Mancata consegna presso Punti di Giacenza - In Dematerializzazione"),
				            Map.entry("RECAG007C","Mancata consegna presso Punti di Giacenza - Fascicolo Chiuso"),
				            Map.entry("RECAG008A","Compiuta giacenza - pre-esito"),
				            Map.entry("RECAG008B","Compiuta giacenza - In Dematerializzazione"),
				            Map.entry("RECAG008C","Compiuta giacenza - Fascicolo Chiuso"),
				            Map.entry("RECRI001","Avviato all'estero"),
				            Map.entry("RECRI002","Ingresso nel paese estero"),
				            Map.entry("RECRI003A","Consegnato - pre-esito"),
				            Map.entry("RECRI003B","Consegnato - In Dematerializzazione"),
				            Map.entry("RECRI003C","Consegnato - Fascicolo Chiuso"),
				            Map.entry("RECRI004A","Non Consegnato - pre-esito"),
				            Map.entry("RECRI004B","Non Consegnato - In Dematerializzazione"),
				            Map.entry("RECRI004C","Non Consegnato - fascicolo Chiuso"),
				            Map.entry("RECRI005","Furto/Smarrimanto/deterioramento"),
				            Map.entry("RECRSI001","Avviato all'estero"),
				            Map.entry("RECRSI002","Ingresso nel paese estero"),
				            Map.entry("RECRSI003C","Consegnato - Fascicolo Chiuso"),
				            Map.entry("RECRSI004A","Non Consegnato - pre-esito"),
				            Map.entry("RECRSI004B","Non Consegnato - In Dematerializzazione"),
				            Map.entry("RECRSI004C","Non Consegnato - fascicolo Chiuso"),
				            Map.entry("RECRSI005","Furto/Smarrimanto/deterioramento"),
				            Map.entry("REC090","Archiviazione fisica materialità di ritorno")
						);
	
	public static Map<String, String> productTypeMap() {
		return productTypeMap;
	}

	private static final Map<String, String> productTypeMap = Map.ofEntries(
			Map.entry("AR","Raccomandata Andata e Ritorno nazionale"),
			Map.entry("890","Recapito a norma della legge 890/1982"),
			Map.entry("RS","Raccomandata Semplice nazionale (per Avviso di mancato Recapito)"),
			Map.entry("RIS","Raccomandata Internazionale Semplice"),
			Map.entry("RIR","Raccomandata Internazionale con AR"));
	
	public static Map<String, String> deliveryFailureCausemap() {
		return deliveryFailureCauseMap;
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
			Map.entry("F01","in caso di furto"),
			Map.entry("F02","in caso di smarrimento"),
			Map.entry("F03","in caso di deterioramento"));
	
}
