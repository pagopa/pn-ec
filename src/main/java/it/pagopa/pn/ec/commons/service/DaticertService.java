package it.pagopa.pn.ec.commons.service;

import it.pec.daticert.Postacert;

public interface DaticertService {

    Postacert getPostacertFromByteArray(byte[] bytes);
}
