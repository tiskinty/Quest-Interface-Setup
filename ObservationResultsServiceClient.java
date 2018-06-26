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

import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;
import javax.xml.rpc.soap.SOAPFaultException;

import observation.webservice.results.serviceHub.medplus.com.AcknowledgedResult;
import observation.webservice.results.serviceHub.medplus.com.Acknowledgment;
import observation.webservice.results.serviceHub.medplus.com.ObservationResult;
import observation.webservice.results.serviceHub.medplus.com.ObservationResultRequest;
import observation.webservice.results.serviceHub.medplus.com.ObservationResultResponse;
import observation.webservice.results.serviceHub.medplus.com.ProviderAccount;

import com.medplus.hub.observation.webservice.ObservationResultService;
import com.medplus.hub.observation.webservice.ObservationResultServiceLocator;
import com.medplus.hub.observation.webservice.ObservationResultServicePort;
import com.medplus.hub.observation.webservice.ObservationResultServicePortStub;

/**
 * class ResultsServiceClient retrieves lab results (HL7 messages) from Hub Information Services.
 * 
 */

public class ObservationResultsServiceClient {
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
    private static final String ENDPOINT = "https://cert.hub.care360.com/observation/result/service";

    private static final String MAX_MESSAGES = "5";
    private static final String START_DATE = "01/01/2004";
    private static final String END_DATE = "01/01/2009";
    

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
     */
    public static void main(String[] args) {
        ObservationResultServicePort proxy;

        try {
            // Get the stub.
            proxy = getObservationResultsServicePort();
            
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
            ProviderAccount[] providerAccountsArray;
            providerAccountsArray = proxy.getProviderAccounts();
            
            ProviderAccount[] subset = new ProviderAccount[1];
            subset[0] = providerAccountsArray[0];
            
            System.out.println("Performing new results search, using one provider account...");
            getResults(proxy, subset, MAX_MESSAGES, null, null);
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
     * The buildAckMessage() method constructs the Ack message needed in
     * response to retrieving the lab results. The return valid is used in the
     * acknowledgeResults() call.
     */
    private static AcknowledgedResult buildAcknowledgement(ObservationResult result) {
    	
    	AcknowledgedResult ack = new AcknowledgedResult();
    	ack.setResultId(result.getResultId());
    	ack.setAckCode("ACK");
    	
    	int pdfCount = result.getDocuments() != null ? result.getDocuments().length : 1;
    	
    	if(result.getDocuments() != null) {
    		String[] docIds = new String[pdfCount];
    		for (int i = 0; i < result.getDocuments().length; i++) {
				docIds[i] = result.getDocuments()[i].getDocumentId();
			}
    		ack.setDocumentIds(docIds);
    	}
    	
        return ack;
    }

    /**
     * The getResultsServicePort() method gets and initializes a reference to
     * the web service "port/stub".
     */
    private static ObservationResultServicePort getObservationResultsServicePort() throws IOException, ServiceException {
        ObservationResultService service = new ObservationResultServiceLocator();
        ObservationResultServicePortStub port = null;

        port = (ObservationResultServicePortStub) service.getObservationResultServicePort();
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
    private static ObservationResultRequest buildResultsRequest(ProviderAccount[] providerAccountsArray, String maxMessages, 
                                                      String startDate, String endDate) {
    	ObservationResultRequest request = new ObservationResultRequest();

        // ------------------------------------------------------------------
        // STEP 1: SET PROVIDER ACCOUNTS
        // ------------------------------------------------------------------
        if (providerAccountsArray != null) {
            request.setProviderAccounts(providerAccountsArray);            
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

    private static AcknowledgedResult[] generateAckMessages(ObservationResultResponse resultsResponse) {
		// Get the results from the response object.
		ObservationResult[] results = resultsResponse.getObservationResults();

		System.out.println("Acknowledging " + results.length + " results.");

		AcknowledgedResult[] ackMessages = new AcknowledgedResult[results.length];

		for (int i = 0; i < results.length; i++) {
			// Process each message and build an Acknowledgement object to
			// acknowledge the result.
			String currentHL7 = new String(results[i].getHL7Message());
			System.out.println("Message: " + i);
			System.out.println("---------");
			System.out.println("The HL7 Message: " + currentHL7);
			String pdfCount = results[i].getDocuments() != null ? "" + results[i].getDocuments().length : "0";
			System.out.println("There are " + pdfCount + " attached PDFs.\n\n");
			// Process Result.
			// Send an Ack/Nack message back for each result received.
			ackMessages[i] = buildAcknowledgement(results[i]);
		}

		return ackMessages;
	}
    
    /**
     * The getResults method will:
     * 
     * 1. On the first pass, create a ResultsRequest object through the function buildResultsRequest,
     *    and call getResults() to get the ObservationResultResponse object containing the first batch 
     *    of results. 
     * 2. Store the request ID in case there are more results to retrieve.
     * 3. Generate acknowledgment message for the returned results (if any). 
     * 4. Acknowledge the results with the acknowledgeResults method. 
     * 5. Output the number of results acknowledged on this pass.
     * 6. If more results are available, call getMoreResults().  Repeat from step 2.
     * 
     */
    private static void getResults(ObservationResultServicePort proxy, ProviderAccount[] providerAccountsArray,
			String maxMessages, String startDate, String endDate) throws RemoteException {

		boolean initialRequest = true;
		boolean processing = true;
		String requestID = "";

		ObservationResultRequest resultsRequest = null;
		ObservationResultResponse response = null;

		while (processing) {
			if (initialRequest) {
				initialRequest = false;
				System.out.println("Making a first pass...");

				// STEP 1: GENERATE RESULTS REQUEST OBJECT
				resultsRequest = buildResultsRequest(providerAccountsArray, maxMessages, startDate, endDate);

				// STEP 2: SUBMIT RESULTS REQUEST TO THE HUB
				response = proxy.getResults(resultsRequest);

				if (response == null) {
					System.out.println("Failed to properly call getHL7Results WebService method.");
				}

			} else {
				System.out.println("Making another pass...");
				response = proxy.getMoreResults(requestID);

				if (response == null) {
					System.out.println("Failed to properly call getMoreHL7Results WebService method.");
				}
			}

			requestID = response.getRequestId();
			System.out.println("Request ID: " + requestID);

			if (response.getObservationResults() == null) {
				// empty array, no HL7 messages returned
				System.out.println("# of Messages on Pass: 0");
				processing = false;
			} else {
				System.out.println("# of Messages on Pass: " + response.getObservationResults().length);

				Acknowledgment ack = new Acknowledgment();
				ack.setRequestId(requestID);
				ack.setAcknowledgedResults(generateAckMessages(response));
				proxy.acknowledgeResults(ack);

				processing = response.getIsMore().booleanValue();
			}
		}
	}     
}