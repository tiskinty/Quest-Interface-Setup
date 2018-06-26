/**
 * An example web service client for requesting HL7 and PDF results.
 * 
 * Copyright 2012 MedPlus, Inc.
 * 
 * @author MedPlus, Inc. 2012
 * 
 */
package hub.sample;

import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.xml.namespace.QName;

import com.medplus.results.AckResultsRequest;
import com.medplus.results.AcknowledgeResults;
import com.medplus.results.Hl7Message;
import com.medplus.results.RequestParameter;
import com.medplus.results.Result;
import com.medplus.results.ResultDocument;
import com.medplus.results.RetrieveResultsPortType;
import com.medplus.results.RetrieveResultsRequest;
import com.medplus.results.RetrieveResultsResponse;
import com.medplus.results.RetrieveResultsService;
import com.medplus.results.SOAPException_Exception;

/**
 * Retrieves lab results (HL7, Printable, and Observation) 
 * from Hub Information Services.
 */
public class ResultsRetrievalServiceClient {

    // Constants that define the three result service types.
    private static final String RESULT_SERVICE_TYPE_HL7 = "HL7";
    private static final String RESULT_SERVICE_TYPE_PRINTABLE = "Printable";
    private static final String RESULT_SERVICE_TYPE_OBSERVATION = "Observation";

    // Constants that define request parameter names.
    private static final String REQUEST_PARAMETER_NAME_MAX_MESSAGES = "maxMessages";
    private static final String REQUEST_PARAMETER_NAME_MESSAGE_CONTROL_ID = "messageControlId";
    private static final String REQUEST_PARAMETER_NAME_PROVIDER_ACCT_ID = "providerAcctId";

    /**
     * The HL7/Observation ACK message template - The following "template" is
     * used in constructing an ACK request message. After calling getResults()
     * for HL7 or Observation results, you will acknowledge receipt of the
     * results by calling acknowledgeResults(). The acknowledgeResults() request
     * contains an acknowledge message constructed from the HL7 message
     * retrieved.
     * 
     * 0 = sending application (field MSH.03) 
     * 1 = sending facility (field MSH.04) 
     * 2 = receiving application (field MSH.05) 
     * 3 = receiving facility (field MSH.06) 
     * 4 = date time (field MSH.07) 
     * 5 = message control id (field MSH.10) 
     * 6 = message control id of message (field MSA.02)
     */
    private static final String HL7_OBS_ACK_MESSAGE = "MSH|^~\\&|{0}|{1}|{2}|{3}|{4,date,yyyyMMddHHmm}||ACK|{5,number,#}|D|2.3\r"
            + "MSA|CA|{6}\r";

    /**
     * Replace "HORI3670test" and "03hori3670" with the username and password MedPlus has
     * assigned to you, or create a 'retrieveResults.properties' file with the
     * settings contained within
     * 
     * You will pass the username and password when you get the stub later in
     * this example. Via the stub, you'll make the method calls to the Retrieve
     * Results Web Service to retrieve lab results.
     */
    private String username = "HORI41930test";
    private String password = "DuhsU0Ym";
    private String endpoint = "https://certhubservices.quanum.com/results/retrieval/service";
    private String maxMessages = "5";
    private String resultServiceType = "Observation";

    /**
     * Main method
     * 
     * @param args
     *            none
     */
    public static void main(String[] args) {
        new ResultsRetrievalServiceClient().execute();
    }

    /**
     * Loads the settings from a properties file, if present. Otherwise,
     * defaults will be used
     */
    public String ResultsRetrievalServiceClientprintout(RetrieveResultsPortType proxy) {
    	for (Field field : proxy.getClass().getDeclaredFields()) {
    	    field.setAccessible(true);
    	    String name = field.getName();
    	    Object value;
			try {
				value = field.get(proxy);
				System.out.printf("%s: %s%n", name, value);
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	    //System.out.printf("%s: %s%n", name, value);
    	}
    	return "";
    }
    
    

    /**
     * The main method will:
     * 
     * 1. Create a web service proxy. 
     * 2. Perform a new results search.
     */
    public void execute() {
        RetrieveResultsPortType proxy;

        // -------------------------------------------------
        // SET UP AUTHENTICATION
        // -------------------------------------------------

        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password.toCharArray());
            }
        });

        try {
            // Get the stub.
            proxy = getRetrieveResultsServicePort();
            System.out.println("proxy : " + ResultsRetrievalServiceClientprintout(proxy));
            // Perform a new results search for the given result service type.
            // Return max of 5 results.
            System.out.println("Performing new results search...");
            getResults(proxy, maxMessages, null, null, resultServiceType);
            System.out.println("Done with new results search.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Builds the acknowledgment object needed to ACK retrieved results.
     * 
     * @param response a message retrieved from the result service
     * @param resultServiceType one of the following:  HL7, Printable, or Observation
     * @return an AcknowledgeResults object to be used to ACK the retrieved results
     */
    private AcknowledgeResults buildAcknowledgement(RetrieveResultsResponse response, String resultServiceType) {

        AcknowledgeResults acknowledgeResults = new AcknowledgeResults();
        AckResultsRequest acknowledgment = new AckResultsRequest();
        acknowledgment.setRequestId(response.getRequestId());
        acknowledgment.setResultServiceType(resultServiceType);
        acknowledgeResults.setRetrieveResultsAcknowledge(acknowledgment);

        System.out.println("Acknowledging " + response.getResults().size() + " results.");

        int i = 0;
        for (Result result : response.getResults()) {
            
            System.out.println("Message: " + i++);
            System.out.println("---------");
                
            if (resultServiceType.equals(RESULT_SERVICE_TYPE_HL7)
                    || resultServiceType.equals(RESULT_SERVICE_TYPE_OBSERVATION)) {

                String currentHL7 = new String(result.getHL7Message().getMessage());
                int pdfCount = result.getDocuments() != null ? result.getDocuments().size() : 0;

                System.out.println("The HL7 Message:\n" + currentHL7);
                System.out.println("There are " + pdfCount + " attached PDFs.\n\n");

                Hl7Message ackMessage = buildHl7ObsAckMessage(currentHL7);
                
                if (resultServiceType.equals(RESULT_SERVICE_TYPE_OBSERVATION)) {
                    String resultId = result.getResultId();
                    ackMessage.setControlId(resultId);
                } 
                
                acknowledgment.getAckMessages().add(ackMessage);
            } else {
                for (ResultDocument resultDoc : result.getDocuments()) {
                    
                    String messageControlId = resultDoc.getResultInfo().getMessageControlId();
                    String providerAccountId = resultDoc.getResultInfo().getProviderAcctId();
                    
                    System.out.println("Printable result: Message Control ID = " + messageControlId);
                    
                    RequestParameter param = new RequestParameter();
                    param.setParameterName(REQUEST_PARAMETER_NAME_MESSAGE_CONTROL_ID);
                    param.setParameterValue(messageControlId);
                    acknowledgment.getRequestParameters().add(param);
                    
                    param = new RequestParameter();
                    param.setParameterName(REQUEST_PARAMETER_NAME_PROVIDER_ACCT_ID);
                    param.setParameterValue(providerAccountId);
                    acknowledgment.getRequestParameters().add(param);
                }
            }
        }
        return acknowledgeResults;
    }

    /**
     * Gets and initializes a reference to the web service "port/stub".
     */
    private RetrieveResultsPortType getRetrieveResultsServicePort() throws IOException {
        RetrieveResultsService service = new RetrieveResultsService(new URL(endpoint + "?wsdl"), new QName(
                "http://medplus.com/results", "RetrieveResultsService"));
        System.out.println("Retrieving the port from the following service: " + service);
        System.out.println(service.getRetrieveResultsPortTypePort());
        return service.getRetrieveResultsPortTypePort();
    }

    /**
     * Takes the parameters for a results request and generates the object to pass to the web service.
     * 
     * @param maxMessages maximum number of messages to return in a single response
     * @param messageControlId the message control ID that identifies a result document
     * @param providerAcctId the provider account ID that identifies a result document
     * @param resultServiceType one of the following:  HL7, Printable, or Observation
     * @return a RetrieveResultsRequest object that is submitted to the results service to retrieve results
     */
    private RetrieveResultsRequest buildResultsRequest(String maxMessages, String messageControlId,
            String providerAcctId, String resultServiceType) {

        RetrieveResultsRequest request = new RetrieveResultsRequest();

        // ------------------------------------------------------------------
        // STEP 1: Set the request parameters.
        // ------------------------------------------------------------------
        if (maxMessages != null && !resultServiceType.equals(RESULT_SERVICE_TYPE_PRINTABLE)) {
            RequestParameter param = new RequestParameter();
            param.setParameterName(REQUEST_PARAMETER_NAME_MAX_MESSAGES);
            param.setParameterValue(maxMessages);
            request.getRequestParameters().add(param);
        }

        if (messageControlId != null) {
            RequestParameter param = new RequestParameter();
            param.setParameterName(REQUEST_PARAMETER_NAME_MESSAGE_CONTROL_ID);
            param.setParameterValue(messageControlId);
            request.getRequestParameters().add(param);
        }

        if (providerAcctId != null) {
            RequestParameter param = new RequestParameter();
            param.setParameterName(REQUEST_PARAMETER_NAME_PROVIDER_ACCT_ID);
            param.setParameterValue(providerAcctId);
            request.getRequestParameters().add(param);
        }

        // ------------------------------------------------------------------
        // STEP 2: Set the result service type.
        // ------------------------------------------------------------------
        request.setResultServiceType(resultServiceType);
        return request;
    }

    /**
     * The getResults method will:
     * 
     * 1. On the first pass, create a RetrieveResultsRequest object and call 
     *    getResults() to get the RetrieveResultsResponse object containing 
     *    the first batch of results. 
     * 2. Store the request ID in case there are more results to retrieve.
     * 3. Generate an acknowledgment message for the returned results (if any). 
     * 4. Acknowledge the results with the acknowledgeResults method. 
     * 5. Output the number of results acknowledged on this pass. 
     * 6. If more results are available, call getMoreResults(). Repeat from step 2.
     */
    private void getResults(RetrieveResultsPortType proxy, String maxMessages, String messageControlId,
            String providerAcctId, String resultServiceType) throws RemoteException {
        String requestID = "";

        RetrieveResultsRequest resultsRequest = null;
        RetrieveResultsResponse response = null;

        boolean processing = true;

        System.out.println("Getting results...");
        System.out.println("Making a first pass...");

        while (processing) {

            // STEP 1: GENERATE RESULTS REQUEST OBJECT
            resultsRequest = buildResultsRequest(maxMessages, messageControlId, providerAcctId, resultServiceType);

            // STEP 2: SUBMIT RESULTS REQUEST TO THE HUB
            try {
                response = proxy.getResults(resultsRequest);

                if (response == null) {
                    System.out.println("Failed to properly call getResults WebService method.");
                }

                requestID = response.getRequestId();
                System.out.println("Request ID: " + requestID);

                if (response.getResults() == null) {
                    // empty array, no HL7 messages returned
                    System.out.println("# of Messages retrieved: 0");
                    processing = false;
                } else {
                    System.out.println("# of Messages retrieved: " + response.getResults().size());

                    // send Acks
                    if (response.getResults().size() > 0) {

                        // Write PDF data to disk (optional)
                        writeResultDataToFile(response.getResults());
                        
                        // STEP 3: BUILD ACKNOWLEDGEMENT
                        AcknowledgeResults acks = buildAcknowledgement(response, resultServiceType);

                        // STEP 4: SEND ACKNOWLEDGEMENT
                        proxy.acknowledgeResults(acks.getRetrieveResultsAcknowledge());

                        // STEP 5: Print the number of messages ACKed
                        System.out.println("# of Messages acknowledged: " + response.getResults().size());

                        // STEP 6: Determine if we need to call again. We will
                        // not call again if a date range is defined
                        // because it will always return the same results
                        processing = response.isIsMore();
                        if (processing) {
                            System.out.println("More results available, calling again");
                        }
                    } else {
                        processing = false;
                    }
                }
            } catch (SOAPException_Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Constructs a message to acknowledge an individual result.
     * 
     * @param hl7 Result message in HL7 format.
     * @return an HL7 ACK message to be used in acknowledging the result.
     */
    private static Hl7Message buildHl7ObsAckMessage(String hl7) {

        Hl7Message retValue;

        // Build an ACK message for an HL7 result.
        String ackMessage = null;
        Object[] msgParams = new Object[7];
        if (hl7 != null) {
            // This class would be used to parse the HL7 message (not supplied).
            ParsedHL7Message hl7Message = new ParsedHL7Message(hl7);

            // Populate the fields of the HL7 message, making sure to reverse
            // sending/receiving fields between response and request.
            // 0 = sending application (field MSH.03)
            // 1 = sending facility (field MSH.04)
            // 2 = receiving application (field MSH.05)
            // 3 = receiving facility (field MSH.06)
            // 4 = date time (field MSH.07)
            // 5 = message control id (field MSH.10)
            // 6 = message control id of message (field MSA.02)
            msgParams[0] = hl7Message.getReceivingApplication();
            msgParams[1] = hl7Message.getReceivingFacility();
            msgParams[2] = hl7Message.getSendingApplication();
            msgParams[3] = hl7Message.getSendingFacility();
            msgParams[4] = new java.util.Date();
            msgParams[5] = new Long(System.currentTimeMillis()); // New message
                                                                 // control ID.
            msgParams[6] = hl7Message.getMessageControlId();
            ackMessage = MessageFormat.format(HL7_OBS_ACK_MESSAGE, msgParams);
        }
        retValue = new Hl7Message();
        retValue.setMessage(ackMessage.getBytes());
        retValue.setControlId((String) msgParams[6]);
        return retValue;
    }
    
    // Example of how to write the PDF out to a file
    private void writeResultDataToFile(List<Result> results)
    {
        try
        {
            for (Result result : results) {
                
                if ((result.getDocuments() != null) && (!result.getDocuments().isEmpty()))
                {
                    for (ResultDocument document : result.getDocuments())
                    {
                        // Open a file with the file name provided in the response object. 
                        File file = new File(document.getDocumentId() + "-" + document.getFileName());
                        FileOutputStream fs = new FileOutputStream(file, false);
                        System.out.println("Writing " + file.getAbsolutePath() + " to disk");
                        fs.write(document.getDocumentData());
                        fs.flush();
                        fs.close();
                    }
                }
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
    
}