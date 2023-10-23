package it.pagopa.pn.ec.commons;

public class ChooseStepException extends RuntimeException {

    public ChooseStepException(String step) {
        super(String.format("Error in step %s retry: this step is not valid!", step));
    }

}
