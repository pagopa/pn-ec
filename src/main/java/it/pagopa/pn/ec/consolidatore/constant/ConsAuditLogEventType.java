package it.pagopa.pn.ec.consolidatore.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ConsAuditLogEventType {

    ERR_CONS("[ERR_CONS]"),
    ERR_CONS_BAD_IUN("[ERR_CONS_BAD_IUN]"),
    ERR_CONS_BAD_STATUS_CODE("[ERR_CONS_BAD_STATUS_CODE]"),
    ERR_CONS_BAD_STATUS("[ERR_CONS_BAD_STATUS]"),
    ERR_CONS_BAD_DEL_FAILURE_CAUSE("[ERR_CONS_BAD_DEL_FAILURE_CAUSE]"),
    ERR_CONS_EMPTY_ATTACH("[ERR_CONS_EMPTY_ATTACH]"),
    ERR_CONS_REQ_ID_NOT_FOUND("[ERR_CONS_REQ_ID_NOT_FOUND]"),
    ERR_CONS_ATTACH_NOT_FOUND("[ERR_CONS_ATTACH_NOT_FOUND]"),
    ERR_CONS_BAD_URI("[ERR_CONS_BAD_URI]"),
    ERR_CONS_BAD_API_KEY("[ERR_CONS_BAD_API_KEY]"),
    ERR_CONS_BAD_SERVICE_ID("[ERR_CONS_BAD_SERVICE_ID]"),
    ERR_CONS_BAD_SHA_256("[ERR_CONS_BAD_SHA_256]"),
    ERR_CONS_BAD_CONTENT_TYPE("[ERR_CONS_BAD_CONTENT_TYPE]"),
    ERR_CONS_BAD_PRELOAD_IDX("[ERR_CONS_BAD_PRELOAD_IDX]"),
    ERR_CONS_BAD_JSON_FORMAT("[ERR_CONS_BAD_JSON_FORMAT]");

    final String value;
}
