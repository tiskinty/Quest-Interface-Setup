/**
 * An example web service client for requesting HL7 results.
 * 
 * Copyright 2006 MedPlus, Inc.
 * 
 * @author MedPlus, Inc. 2006
 * 
 */
package hub.sample;

import java.io.IOException;
import java.rmi.RemoteException;
import java.text.MessageFormat;

import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;
import javax.xml.rpc.soap.SOAPFaultException;

import com.medplus.hub.results.webservice.HL7Message;
import com.medplus.hub.results.webservice.HL7ResultsResponse;
import com.medplus.hub.results.webservice.ProviderAccount;
import com.medplus.hub.results.webservice.ResultsRequest;
import com.medplus.hub.results.webservice.ResultsService;
import com.medplus.hub.results.webservice.ResultsServiceLocator;
import com.medplus.hub.results.webservice.ResultsServicePort;
import com.medplus.hub.results.webservice.ResultsServicePortStub;

/**
 * class ResultsServiceClient retrieves lab results (HL7 messages) from Hub Information Services.
 * 
 */

public class HL7ResultsServiceClient {
    /**
     * Replace "test" and "customer1" with the username and password MedPlus has
     * assigned to you.
     * 
     * You will pass the username and password when you get the stub later in
     * this example. Via the stub, you'll make the method calls to the Results
     * Web Service to retrieve lab results.
     */
    private static final String USERNAME = "test";
    private static final String PASSWORD = "customer1";
    private static final String ENDPOINT = "https://cert.hub.care360.com/resultsHub/observations/hl7";

    private static final String MAX_MESSAGES = "5";
    private static final String START_DATE = "01/01/2004";
    private static final String END_DATE = "01/01/2009";
    
    /**
     * The HL7 Ack message template - The following "template" is used in
     * constructing the Ack. After calling getHL7Results(), you will acknowledge
     * receipt of the results by calling acknowledgeHL7Results(). The second
     * parameter of the acknowledgeHL7Results() is an acknowledge message
     * constructed from the HL7 message retrieved.
     * 
     * 0 = sending application (field 3) 
     * 1 = sending facility (field 4) 
     * 2 = receiving application (field 5) 
     * 3 = receiving facility (field 6) 
     * 4 = date time (field 7) 
     * 5 = message control id (field 10) 
     * 6 = message control id of message
     * 
     */
    private static final String ACK_MESSAGE = "MSH|^~\\&|{0}|{1}|{2}|{3}|{4,date,yyyyMMddHHmm}||ACK|{5,number,#}|D|2.3\r"
            + "MSA|CA|{6}\r";

    /**
     * The main method will:
     * 
     * 1. Create a web service proxy. 
     * 2. Perform a new results search by calling getResults with no dates provided.
     * 3. Call getProviderAccounts() to retrieve a String array with a provider account 
     *    which represents a subset of all the provider accounts the hub account has
     *    access to.
     * 4. Perform a new results search using the provider accounts array, to get all 
     *    results just for that provider account. 
     * 5. Perform a date range results search by calling getResults with a start and end date.
     * 
     */
    public static void main(String[] args) {
        ResultsServicePort proxy;

        try {
            // Get the stub.
            proxy = getResultsServicePort();
            
            // EXAMPLE 1:
            // Perform a new results search using all provider accounts.
            // This is the most common scenario for developing vendors.
            // Return max of 5 results.
            System.out.println("Performing new results search, using all provider accounts...");
            getResults(proxy, null, MAX_MESSAGES, null, null);
            System.out.println("Done with new results search using all provider accounts.");

            // EXAMPLE 2:
            // Perform a new results search using just one provider account. 
            // Return max of 5 results.
            String[] providerAccountsArray;
            providerAccountsArray = getProviderAccounts(proxy);
            System.out.println("Performing new results search, using one provider account...");
            getResults(proxy, providerAccountsArray, MAX_MESSAGES, null, null);
            System.out.println("Done with new results search using one provider account.");
            
            // EXAMPLE 3:
            // Date range search using all provider accounts. Max of 5 results, will 
            // acknowledge those results and then get more until they're all processed
            System.out.println("Performing limited date range search...");
            getResults(proxy, null, MAX_MESSAGES, START_DATE, END_DATE);
            System.out.println("Done with date range results search.");

        } catch (Exception e) {
            if (e.getCause() instanceof SOAPFaultException) {
                SOAPFaultException sfe = (SOAPFaultException) e.getCause();
                System.out.println(sfe.getFaultString());
            }

            e.printStackTrace();
        }

    }

    /**
     * 
     * The buildAckMessage() method constructs the Ack message needed in
     * response to retrieving the lab results. The return valid is used in the
     * acknowledgeHL7Results() call.
     * 
     */
    private static HL7Message buildAckMessage(String hl7) {
        // Return value.
        String ack = null;
        HL7Message retValue;

        // Build an Ack message for the HL7.
        Object[] msgParams = new Object[7];
        if (hl7 != null) {
            // This class would be used to parse the HL7 message (not supplied).
            ParsedHL7Message hl7Message = new ParsedHL7Message(hl7);

            // 0 = sending application (field 3)
            // 1 = sending facility (field 4)
            // 2 = receiving application (field 5)
            // 3 = receiving facility (field 6)
            // 4 = date time (field 7)
            // 5 = message control id (field 10)
            // 6 = message control id of message
            // reverse sending/receiving
            msgParams[0] = hl7Message.getReceivingApplication();
            msgParams[1] = hl7Message.getReceivingFacility();
            msgParams[2] = hl7Message.getSendingApplication();
            msgParams[3] = hl7Message.getSendingFacility();
            msgParams[4] = new java.util.Date();
            msgParams[5] = new Long(System.currentTimeMillis()); // New message control ID.
            msgParams[6] = hl7Message.getMessageControlId();
            ack = MessageFormat.format(ACK_MESSAGE, msgParams);
        }
        retValue = new HL7Message();
        retValue.setMessage(ack.getBytes());
        return retValue;
    }

    /**
     * The getResultsServicePort() method gets and initializes a reference to
     * the web service "port/stub".
     * 
     */
    private static ResultsServicePort getResultsServicePort() throws IOException, ServiceException {
        ResultsService service = new ResultsServiceLocator();
        ResultsServicePortStub port = null;

        port = (ResultsServicePortStub) service.getResultsServicePort();
        port._setProperty(Stub.USERNAME_PROPERTY, USERNAME);
        port._setProperty(Stub.PASSWORD_PROPERTY, PASSWORD);
        port._setProperty(Stub.ENDPOINT_ADDRESS_PROPERTY, ENDPOINT);
        return port;
    }

    /**
     * 
     * The buildResultsRequest function will take the parameters for a
     * results request and generate the object to pass to the web method
     * 
     */
    private static ResultsRequest buildResultsRequest(String[] providerAccountsArray, String maxMessages, 
                                                      String startDate, String endDate) {
        ResultsRequest request = new ResultsRequest();

        // ------------------------------------------------------------------
        // STEP 1: SET PROVIDER ACCOUNTS
        // ------------------------------------------------------------------
        if (providerAccountsArray != null) {
            request.setProviderAccountIds(providerAccountsArray);            
        }
        
        // ------------------------------------------------------------------
        // STEP 2: SET MAXIMUM NUMBER OF MESSAGES TO RETRIEVE AT ONCE
        // ------------------------------------------------------------------
        if (maxMessages.length() > 0) {
            Integer myMaxMessages = Integer.valueOf(maxMessages);
            request.setMaxMessages(myMaxMessages);
        }

        // ------------------------------------------------------------------
        // STEP 3: SET DATE RANGE TO GET RESULTS
        // ------------------------------------------------------------------
        request.setEndDate(endDate);
        request.setStartDate(startDate);

        return request;

    }

    private static HL7Message[] generateHL7AckMessages(HL7ResultsResponse resultsResponse) {
        // ackMessages sets one of the parameters in the response object. The
        // response object is used in the acknowledgeHL7Results() call.

        // Get the HL7 messages from the response object.
        HL7Message[] myHL7Messages = resultsResponse.getHL7Messages();

        // Format the ackMessages using the ACK_MESSAGE template.
        System.out.println("Acknowledging " + myHL7Messages.length + " results.");

        HL7Message[] ackMessages = new HL7Message[myHL7Messages.length];

        for (int i = 0; i < myHL7Messages.length; i++) {
            // Process each message and build an HL7 Ack message to acknowledge
            // the result,
            // or to reject the result if there is a problem.
            String currentHL7 = new String(myHL7Messages[i].getMessage());
            System.out.println("Message: " + i);
            System.out.println("---------");
            System.out.println("The real HL7 Message: " + currentHL7);
            
            // Process HL7 message.
            // Send an Ack/Nack message back for each HL7 message received.
            ackMessages[i] = buildAckMessage(currentHL7);
        }

        return ackMessages;
    }
    
    /**
     * The getResults method will:
     * 
     * 1. On the first pass, create a ResultsRequest object through the function buildResultsRequest,
     *    and call getHL7Results() to get the HL7ResultsResponse object containing the first batch 
     *    of results. 
     * 2. Store the request ID in case there are more results to retrieve.
     * 3. Generate acknowledgment HL7 messages for the returned results (if any). 
     * 4. Acknowledge the results with the acknowledgeResults method. 
     * 5. Output the number of results acknowledged on this pass.
     * 6. If more results are available, call getMoreHL7Results().  Repeat from step 2.
     * 
     */
    private static void getResults(ResultsServicePort proxy, String[] providerAccountsArray, String maxMessages, String startDate, String endDate) throws RemoteException {
        boolean initialRequest = true;
        boolean processing = true;
        String requestID = "";
        ResultsRequest hl7ResultsRequest = null;
        HL7ResultsResponse hl7Response = null;
        
        while (processing) {
            if (initialRequest) {
                initialRequest = false;
                System.out.println("Making a first pass...");

                // STEP 1: GENERATE RESULTS REQUEST OBJECT
                hl7ResultsRequest = buildResultsRequest(providerAccountsArray, maxMessages, startDate, endDate);

                // STEP 2: SUBMIT RESULTS REQUEST TO THE HUB
                hl7Response = proxy.getHL7Results(hl7ResultsRequest);

                if (hl7Response == null) {
                    System.out.println("Failed to properly call getHL7Results WebService method.");
                }
                
            } else {
                System.out.println("Making another pass...");
                hl7Response = proxy.getMoreHL7Results(requestID);

                if (hl7Response == null) {
                    System.out.println("Failed to properly call getMoreHL7Results WebService method.");
                }
            }

            requestID = hl7Response.getRequestId();
            System.out.println("Request ID: " + requestID);

            if (hl7Response.getHL7Messages() == null) {
                // empty array, no HL7 messages returned
                System.out.println("# of Messages on Pass: 0");
                processing = false;
            } else {
                System.out.println("# of Messages on Pass: " + hl7Response.getHL7Messages().length);
                
                HL7Message[] hl7ACKMessages = generateHL7AckMessages(hl7Response);
                proxy.acknowledgeHL7Results(requestID, hl7ACKMessages);

                processing = hl7Response.getIsMore().booleanValue();
            }
        }
        
    }
    
    /**
     * The getProviderAccounts method will call the Hub and get a list
     * of provider accounts the hub user has access to.  It then places
     * the first one in a String array and returns the array, which can 
     * be passed to the getHL7Results method to get results for a subset 
     * of the provider accounts the hub user has access to.
     * 
     */
    private static String[] getProviderAccounts(ResultsServicePort proxy) {
        try {
            // Get Provider Accounts
            ProviderAccount[] providerAccounts;
            String[] providerAccountsArray = null;
            System.out.println("Getting Provider Accounts...");

            providerAccounts = proxy.getProviderAccounts();

            if (providerAccounts != null) {                
                if (providerAccounts.length > 0) {
                    // loop through provider accounts and display all of them
                    for (int i = 0; i < providerAccounts.length; i++) {
                        System.out.println("ID: " + providerAccounts[i].getAccountId() + 
                                           ", Name: " + providerAccounts[i].getAccountName() +
                                           ", Provider: " + providerAccounts[i].getProvider().getName());
                        }
     
                    // create the Provider Accounts array with only one element
                    // and set it to the first returned provider account
                    providerAccountsArray = new String[1];
                    providerAccountsArray[0] = providerAccounts[0].getAccountId().trim();   
                }
            }
            
            return providerAccountsArray;
        } catch (Exception e) {
            if (e.getCause() instanceof SOAPFaultException) {
                SOAPFaultException sfe = (SOAPFaultException) e.getCause();
                System.out.println(sfe.getFaultString());
            }

            e.printStackTrace();
            return null;
        }
        
    }
        
}