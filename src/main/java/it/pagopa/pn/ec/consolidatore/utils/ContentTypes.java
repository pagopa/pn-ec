package it.pagopa.pn.ec.consolidatore.utils;

import java.util.List;

public class ContentTypes {

    private ContentTypes() {
    }

    public static final String APPLICATION_PDF = "application/pdf";
    public static final String APPLICATION_ZIP = "application/zip";
    public static final String IMAGE_TIFF = "image/tiff";
    public static final String APPLICATION_XML = "application/xml";

    public static final List<String> CONTENT_TYPE_LIST = List.of(APPLICATION_PDF, APPLICATION_ZIP, IMAGE_TIFF);

}
