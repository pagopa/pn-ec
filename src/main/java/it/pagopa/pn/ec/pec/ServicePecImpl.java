package it.pagopa.pn.ec.pec;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServicePecImpl {


    public boolean getPecMessages(ParametriRicercaPec param) {
        if (param.getEmail() == null || param.getEmail().equals("test?test.com")) {
            return true;
        } else {
            return false;
        }

    }

    public Boolean getPecMessagesId(ParametriRicercaPec param) {
        Integer result = param.getMailId();
        if (result == null || param.getMailId() < 0) {
            return true;
        } else {
            return false;
        }
    }

    public Boolean getNameAttach(ParametriRicercaPec param) {
        if (param.getUserId() < 0 || param.getUserId() == null || param.getMailId() < 0 || param.getMailId() == null) {
            return true;
        } else {
            return false;
        }
    }

    public Boolean getAttach(ParametriRicercaPec param) {
        if (param.getUserId() < 0 || param.getUserId() == null || param.getMailId() < 0 || param.getMailId() == null ||
            param.getNomeAttach().equals("failed.test") || param.getNomeAttach() == null) {
            return true;
        } else {
            return false;
        }
    }

    public Boolean getMessageCount(Integer count) {
        if (count < 0 || count == null) {
            return true;
        } else {
            return false;
        }
    }

    public Boolean getQuota(ParametriRicercaPec param) {
        if (param.getMailboxSpaceLeft() < 0 || param.getMailboxSpaceLeft() == null || param == null) {
            return true;
        } else {
            return false;
        }
    }

    public Boolean sendMail(PecObject pec) {
        if (pec.getSize() <= 0 || pec.getSize() > 16) {
            return true;
        } else {
            return false;
        }
    }

    public Boolean mailboxLogin(ParametriRicercaPec param) {
        if (this.getQuota(param)) {
            return false;
        } else {
            return true;
        }

    }
}
