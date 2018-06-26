/**
 * An example web service client for requesting results.
 * 
 * Copyright 2006 MedPlus, Inc.
 * 
 * @author MedPlus, Inc. 2006
 * 
 */
package hub.sample;

import java.io.IOException;
import java.io.*;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;
import javax.xml.rpc.soap.SOAPFaultException;

import com.medplus.hub.results.webservice.PrintableResultsResponse;
import com.medplus.hub.results.webservice.ProviderAccount;
import com.medplus.hub.results.webservice.ResultsRequest;
import com.medplus.hub.results.webservice.PrintableResultsService;
import com.medplus.hub.results.webservice.PrintableResultsServiceLocator;
import com.medplus.hub.results.webservice.PrintableResultsServicePort;
import com.medplus.hub.results.webservice.PrintableResultsServicePortStub;

/**
 * class PrintableResultsServiceClient retrieves lab results (PDFs) from Hub Information Services.
 * 
 */

public class PrintableResultsServiceClient {
    /**
     * Replace "test" and "customer1" with the username and password Medplus has
     * assigned to you.
     * 
     * You will pass the username and password when you get the stub later in
     * this example. Via the stub, you'll make the method calls to the Results
     * Web Service to retrieve lab results.
     */
    private static final String USERNAME = "HORI41930test";
    private static final String PASSWORD = "DuhsU0Ym";
    private static final String ENDPOINT = "https://certhubservices.quanum.com/results/retrieval/printable";

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
     * 
     */
    public static void main(String[] args) {
        PrintableResultsServicePort proxy;

        // Get the stub.
        try {
            proxy = getResultsServicePort();

            // EXAMPLE 1:
            // Perform a new results search using all provider accounts.
            // This is the most common scenario for developing vendors.
            // Return max of 5 results.
            System.out.println("Performing new results search, using all provider accounts...");
            getResults(proxy, null, null, null);
            System.out.println("Done with new results search using all provider accounts.");

            // EXAMPLE 2:
            // Perform a new results search using just one provider account. 
            // Return max of 5 results.
            String[] providerAccountsArray;
            providerAccountsArray = getProviderAccounts(proxy);
            System.out.println("Performing new results search, using one provider account...");
            getResults(proxy, providerAccountsArray, null, null);
            System.out.println("Done with new results search using one provider account.");
            
            // EXAMPLE 3:
            // Date range search using all provider accounts. Max of 5 results, will 
            // acknowledge those results and then get more until they're all processed
            System.out.println("Performing limited date range search...");
            getResults(proxy, null, START_DATE, END_DATE);
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
     * The getResultsServicePort() method gets and initializes a reference to
     * the web service "port/stub".
     * 
     */
    private static PrintableResultsServicePort getResultsServicePort() throws IOException, ServiceException {
        PrintableResultsService service = new PrintableResultsServiceLocator();
        PrintableResultsServicePortStub port = null;

        port = (PrintableResultsServicePortStub) service.getPrintableResultsServicePort();
        port._setProperty(Stub.USERNAME_PROPERTY, USERNAME);
        port._setProperty(Stub.PASSWORD_PROPERTY, PASSWORD);
        port._setProperty(Stub.ENDPOINT_ADDRESS_PROPERTY, ENDPOINT);

        return port;
    }


    /**
     * 
     * The buildResultsRequest function will take the parameters for a
     * results request and gernerate the object to pass to the web method
     * 
     */
    private static ResultsRequest buildResultsRequest(String[] providerAccountsArray, String startDate, String endDate) {
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
        request.setMaxMessages(new Integer(1));

        // ------------------------------------------------------------------
        // STEP 3: SET DATE RANGE TO GET RESULTS
        // ------------------------------------------------------------------
        request.setEndDate(endDate);
        request.setStartDate(startDate);

        return request;
    }

    /**
     * The getResults method will:
     * 
     * 1. On the first pass, create a ResultsRequest object through the function buildResultsRequest,
     *    and call getResults() to get the PrintableResultsResponse object containing the first batch 
     *    of results. 
     * 2. Store the request ID in case there are more results to retrieve.
     * 3. Process the returned result (if available) and acknowledge it with the acknowledgeResults method. 
     * 4. Output the file name of the result.
     * 5. If more results are available, call getMoreResults().  Repeat from step 2.
     * 6. After processing the last result, output the number of results processed.
     * 
     */
    private static void getResults(PrintableResultsServicePort proxy, String[] providerAccountsArray, String startDate, String endDate) throws RemoteException {
        boolean initialRequest = true;
        boolean processing = true;
        String requestID = "";
        ResultsRequest printableResultsRequest = null;
        PrintableResultsResponse printableResponse = null;
        int numPDFs = 0;            
        
        while (processing) {
            if (initialRequest) {
                initialRequest = false;
                System.out.println("Making a first pass...");

                // STEP 1: GENERATE RESULTS REQUEST OBJECT
                printableResultsRequest = buildResultsRequest(providerAccountsArray, startDate, endDate);

                // STEP 2: SUBMIT RESULTS REQUEST TO THE HUB
                printableResponse = proxy.getResults(printableResultsRequest);

                if (printableResponse == null) {
                    System.out.println("Failed to properly call getResults WebService method.");
                }
                
            } else {
                System.out.println("Making another pass...");
                printableResponse = proxy.getMoreResults(requestID);

                if (printableResponse == null) {
                    System.out.println("Failed to properly call getMoreResults WebService method.");
                }
            }

            requestID = printableResponse.getRequestId();
            System.out.println("Request ID: " + requestID);

            if (printableResponse.getResultInfo() == null) {
                // empty array, no PDF messages returned
                System.out.println("PDF not found.");
                processing = false;
            } else {
                numPDFs++;
                System.out.println("PDF found: " + printableResponse.getFileName());

                writeResultDataToFile(printableResponse);
                
                // Acknowledge result
                System.out.println("Acknowledging PDF file...");
                proxy.acknowledgeResults(requestID, printableResponse.getResultInfo(), null);

                processing = printableResponse.getIsMore().booleanValue();
            }
        }

        System.out.println("Total PDFs found: " + numPDFs);
        
    }
    
    /**
     * The getProviderAccounts method will call the Hub and get a list
     * of provider accounts the hub user has access to.  It then places
     * the first one in a String array and returns the array, which can 
     * be passed to the getResults method to get results for a subset 
     * of the provider accounts the hub user has access to.
     * 
     */
    private static String[] getProviderAccounts(PrintableResultsServicePort proxy) {
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

    // Example of how to write the PDF out to a file
    private static void writeResultDataToFile(PrintableResultsResponse printableResponse) {
        try {
            // Open a file with the file name provided in the response object.  
            OutputStream out = new FileOutputStream(printableResponse.getFileName());

            try {
                // Write the contents of the byte[] out to the file
                out.write(printableResponse.getResultData());
            } catch (FileNotFoundException e) {
                e.printStackTrace();                    
            } finally {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
