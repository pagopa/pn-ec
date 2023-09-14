package it.pagopa.pn.ec.commons.service;

import it.pagopa.pn.ec.commons.model.pojo.pec.PnPostacert;

public interface DaticertService {

    PnPostacert getPostacertFromByteArray(byte[] bytes);
}
