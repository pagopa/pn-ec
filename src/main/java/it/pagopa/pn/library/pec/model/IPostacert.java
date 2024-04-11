package it.pagopa.pn.library.pec.model;


import it.pagopa.pn.library.pec.model.pojo.Dati;
import it.pagopa.pn.library.pec.model.pojo.Intestazione;

public interface IPostacert {

    String getTipo();

    void setTipo(String value);

    String getErrore();

    void setErrore(String value);


    Intestazione getIntestazione();

    void setIntestazione(Intestazione value);

    Dati getDati();

    void setDati(Dati value);

}
