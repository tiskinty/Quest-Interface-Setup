/**
 * This class will parse an HL7 Message and discretely make some of the
 * individual fields available.
 * 
 * Copyright 2006 MedPlus, Inc.
 * 
 * @author MedPlus, Inc. 2006
 */

package hub.sample;

public class ParsedHL7Message {

    private static final int SENDING_APPLICATION_FIELD = 3;
    private static final int SENDING_FACILITY_FIELD = 4;
    private static final int RECEIVING_APPLICATION_FIELD = 5;
    private static final int RECEIVING_FACILITY_FIELD = 6;
    private static final int PROVIDER_NAME_FIELD = 4;
    private static final int PROVIDER_ACCT_FIELD = 6;
    private static final int MESSAGE_CONTROL_ID_FIELD = 10;

    private String providerName;
    private String providerAcct;
    private String receivingApplication;
    private String receivingFacility;
    private String sendingApplication;
    private String sendingFacility;
    private String messageControlId;

    ParsedHL7Message(String message) {
        parse(message);
    }

    private void parse(String message) {
        String[] fields = message.split("\\|", 12);

        sendingApplication = getField(fields, SENDING_APPLICATION_FIELD);
        sendingFacility = getField(fields, SENDING_FACILITY_FIELD);
        receivingApplication = getField(fields, RECEIVING_APPLICATION_FIELD);
        receivingFacility = getField(fields, RECEIVING_FACILITY_FIELD);
        messageControlId = getField(fields, MESSAGE_CONTROL_ID_FIELD);
        providerName = getField(fields, PROVIDER_NAME_FIELD);
        providerAcct = getField(fields, PROVIDER_ACCT_FIELD);
    }

    String getField(String[] fields, int fieldIndex) {
        String fieldValue = null;

        if (fieldIndex > 1 && fieldIndex < fields.length) {
            fieldValue = fields[fieldIndex - 1];
        }

        return fieldValue;
    }

    String getProviderName() {
        return providerName;
    }

    String getProviderAcct() {
        return providerAcct;
    }

    String getReceivingApplication() {
        return receivingApplication;
    }

    String getReceivingFacility() {
        return receivingFacility;
    }

    String getSendingApplication() {
        return sendingApplication;
    }

    String getSendingFacility() {
        return sendingFacility;
    }

    String getMessageControlId() {
        return messageControlId;
    }
}
