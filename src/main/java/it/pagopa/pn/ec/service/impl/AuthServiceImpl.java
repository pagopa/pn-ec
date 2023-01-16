package it.pagopa.pn.ec.service.impl;

import it.pagopa.pn.ec.exception.IdClientNotFoundException;
import it.pagopa.pn.ec.service.AuthService;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Override
    public void checkIdClient(String idClient) throws IdClientNotFoundException {
        // TODO -> write checkIdClient implementation
    }
}
