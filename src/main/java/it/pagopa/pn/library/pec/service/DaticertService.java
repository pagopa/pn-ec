package it.pagopa.pn.library.pec.service;

import it.pagopa.pn.library.pec.model.IPostacert;

public interface DaticertService {

    IPostacert getPostacertFromByteArray(byte[] bytes);
}
