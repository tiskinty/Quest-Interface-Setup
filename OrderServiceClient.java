/**
 * An example web service client for submitting orders.
 *
 * Copyright 2005 MedPlus, Inc.
 * @author MedPlus, Inc. 2005
 *
 */

package hub.sample;

import java.io.IOException;
import java.text.MessageFormat;

import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;
import javax.xml.rpc.soap.SOAPFaultException;

import support.orders.serviceHub.medplus.com.OrderSupportDocument;

import com.medplus.hub.orders.webservice.Order;
import com.medplus.hub.orders.webservice.OrderResponse;
import com.medplus.hub.orders.webservice.OrderService;
import com.medplus.hub.orders.webservice.OrderServiceLocator;
import com.medplus.hub.orders.webservice.OrderServicePort;
import com.medplus.hub.orders.webservice.OrderServicePortStub;
import com.medplus.hub.orders.webservice.OrderSupportServiceRequest;
import com.medplus.hub.orders.webservice.OrderSupportServiceResponse;
import com.medplus.hub.orders.webservice.ResponseProperty;


/**
 * class OrderServiceClient submits lab order (HL7 messages) to the MedPlus Hub
 * platform.  Encapsulates the sending of an HL7 order to a Quest Lab
 * via the Hub�s SOAP Web service.
 * 
 */
public class OrderServiceClient {
    /**
     * You will pass the username and password when you get the stub later in
     * this example. Via the stub, you'll make the method calls to the Results
     * Web Service to retrieve lab results.
     */    
    private static final String USERNAME = "test";
    private static final String PASSWORD = "customer1";
    private static final String ENDPOINT = "https://cert.hub.care360.com/orders/service";  
    
    /*
    private static final String USERNAME = "shiwan";
    private static final String PASSWORD = "Shili0$8";
    private static final String ENDPOINT = "http://hubfuture.dev.medplus.com/orders/service";*/
    
    // SENDING_APPLICATION designates the application that is sending the order
    // message to Hub
    private static final String SENDING_APPLICATION = "HUBWS";    

    // SENDING_FACILITY designates the account number provided to you by Quest
    // for the businessunit you are ordering tests with
    private static final String SENDING_FACILITY = "2135800";
    //private static final String SENDING_FACILITY = "shiwanpa";    

    // RECEIVING_FACILITY designates the business unit within Quest from which
    // the labs are being ordered
    private static final String RECEIVING_FACILITY = "THO"; 
    //private static final String RECEIVING_FACILITY = "shiwanprovider";    
    

    /*
     * The HL7 Order message template - The following "template" is used in
     * constucting the Order.
     * 
     * 0 = sending application (field 3) 
     * 1 = sending facility (field 4) represents the account with Quest Diagnostics 
     * 2 = receiving facility (field 6) The Quest Business Unit for above account 
     * 3 = date time (field 7) 
     * 4 = message control id (field 10) Unique Number identifying this order message
     * 
     */
    private static final String ORDER_MESSAGE = "MSH|^~\\&|{0}|{1}||{2}|{3,date,yyyyMMddHHmm}||ORM^O01|{4,number,#}|P|2.3\r"
            + "PID|1|11111|||TEST^WIFE||19451212|M|||||3102222222||||||123456789|\r"
            + "IN1|1||AUHSC|AETNA|123 ANYSTREET^2^CHICAGO^IL^60305|||A12345||||||||TEST^HUSBAND^|2||123 ANYSTREET^^CHICAGO^IL^60305|||||||||||||||||P123456R|||||||||||T|\r"
            + "GT1|1||TEST^HUSBAND^||123 ANYSTREET^^CHICAGO^IL^60305|3102222222||19451212|M|\r"
            + "ORC|NW|{4,number,#}||||||||||OTH030^MICHIGAN^JOHN^^^^^UPIN|\r"
            + "OBR|1|{4,number,#}||^^^6399^CBC|||20051223094800|||||||||OTH030^MICHIGAN^JOHN^^^^^UPIN|||||||||||1^^^^^R|\r"
            + "DG1|1|ICD|0039|SALMONELLA INFECTION NOS|\r";

    public static void main(String[] args) {
        // Validate the Order
        sendOrder(true);
        
        // Submit the Order
        sendOrder(false); 
        
        // Get order documents (ABN, REQ)
        getOrderDocuments();
    }

    /**
     * 
     * The getOrderDocuments() method will:
     * 
     * 1. Create a proxy for making SOAP calls 
     * 2. Create an Order request object which contains a valid HL7 Order message and document request type
     * 3. Call getOrderDocuments(). 
     * 4. Output response values to console.
     * 
     */
    private static void getOrderDocuments() {
        OrderServicePort proxy;

        try {
            // -------------------------------------------------
            // STEP 1: CREATE WEB SERVICE PROXY WITH CREDENTIALS
            // -------------------------------------------------
            proxy = getOrderServicePort();

            OrderSupportServiceRequest request = null;
            OrderSupportServiceResponse response = null;
            
            // ------------------------------------------------------------------
            // STEP 2: call function that will create the OrderSupportRequest
            // Object
            // ------------------------------------------------------------------
            request = getOrderSupportRequest();
            
            // --------------------------------------------------------------
            // STEP 3: call WebService function that get order documents           
            // --------------------------------------------------------------
            response = proxy.getOrderDocuments(request);
       
            // ------------------------------------------------
            // STEP 4: Examine response coming back for the Hub
            // ------------------------------------------------
            System.out.println("Status: " + response.getStatus() + 
                               "\nTransaction ID: " + response.getOrderTransactionUid() +
                               "\nMessage Control ID: " + response.getMessageControlId() +
                               "\nResponse Message: " + response.getResponseMsg());     
            
            if (response.getResponseProperties() != null && response.getResponseProperties().length > 0) {
                ResponseProperty[] properties = response.getResponseProperties();
                for (int i = 0; i < properties.length; i++) {
                    System.out.println(properties[i].getPropertyName() + " - " + properties[i].getPropertyValue());
                }
            }
            System.out.println("Order Documents - ");
            OrderSupportDocument[] documents = response.getOrderSupportDocuments();
            for (int j = 0; j < documents.length; j++) {
                if (documents[j].getDocumentData() != null) {
                    System.out.println("Document Data: " + new String(documents[j].getDocumentData()));                    
                }
                System.out.println(
                        "Document Type: " + documents[j].getDocumentType() +
                        "\nRequest Status: " + documents[j].getRequestStatus() +
                        "\nResponse Message: " + documents[j].getResponseMessage());
            }
            

            String[] valErrors = response.getValidationErrors();
            
            if (null != valErrors) {
                for (int ndx = 0; ndx < valErrors.length; ndx++) {
                    System.out.println("\tValidation Error: " + valErrors[ndx] + ".");
                }
            }
        } catch (Exception e) {
            if (e.getCause() instanceof SOAPFaultException) {
                SOAPFaultException sfe = (SOAPFaultException) e.getCause();
                System.out.println(sfe.getFaultString());
            }

            e.printStackTrace();
        }
    }
    
    
    private static void sendOrder(boolean validateOnly) {
        OrderServicePort proxy;

        try {
            // -------------------------------------------------
            // STEP 1: CREATE WEB SERVICE PROXY WITH CREDENTIALS
            // -------------------------------------------------
            proxy = getOrderServicePort();

            Order order = null;
            OrderResponse response = null;
            
            // ------------------------------------------------------------------
            // STEP 2: call function that will create the WebService Order
            // Object
            // ------------------------------------------------------------------
            order = getWebServiceOrder();
            
            // --------------------------------------------------------------
            // STEP 3: call WebService function that submits order to the
            // Hub
            // --------------------------------------------------------------
            if (validateOnly) {
                response = proxy.validateOrder(order);
            } else {
                response = proxy.submitOrder(order);             
            }

            // ------------------------------------------------
            // STEP 4: Examine response coming back for the Hub
            // ------------------------------------------------
            System.out.println("Status: " + response.getStatus() + 
                               "\nTransaction ID: " + response.getOrderTransactionUid() +
                               "\nMessage Control ID: " + response.getMessageControlId() +
                               "\nResponse Message: " + response.getResponseMsg());                

            String[] valErrors = response.getValidationErrors();
            
            if (null != valErrors) {
                for (int ndx = 0; ndx < valErrors.length; ndx++) {
                    System.out.println("\tValidation Error: " + valErrors[ndx] + ".");
                }
            }
        } catch (Exception e) {
            if (e.getCause() instanceof SOAPFaultException) {
                SOAPFaultException sfe = (SOAPFaultException) e.getCause();
                System.out.println(sfe.getFaultString());
            }

            e.printStackTrace();
        }
    }
    
    
    

    /**
     * getWebServiceOrder: construct the WebService request object using member data
     * 
     */
    private static Order getWebServiceOrder() {
        Order retval = new Order();
        // orderMessage is used to set the only parameter in the order request object.
        String orderMessage = buildOrderMessage(SENDING_APPLICATION, SENDING_FACILITY, RECEIVING_FACILITY);
        retval.setHl7Order(orderMessage.getBytes());
        return retval;
    }
    
    private static OrderSupportServiceRequest getOrderSupportRequest() {
        OrderSupportServiceRequest request = new OrderSupportServiceRequest();
        String orderMessage = buildOrderMessage(SENDING_APPLICATION, SENDING_FACILITY, RECEIVING_FACILITY);
        request.setHl7Order(orderMessage.getBytes());
        request.setOrderSupportRequests(new String[] {"ABN", "REQ"});        
        return request;
    }

    /**
     * buildOrderMessage: constructs a valid HL7 Order message string
     * 
     */
    private static String buildOrderMessage(String sendingApplication, String sendingFacility, String receivingFacility) {
        // return value
        String retValue = null;
        // Build an Order Message for the hl7
        Object[] msgParams = new Object[7];
        // 0 = sending application (field 3)
        // 1 = sending facility (field 4)
        // 2 = receiving facility (field 6)
        // 3 = date time (field 7)
        // 4 = message control id (field 10)
        // reverse sending/receiving
        msgParams[0] = sendingApplication;
        msgParams[1] = sendingFacility;
        msgParams[2] = receivingFacility;
        msgParams[3] = new java.util.Date();
        msgParams[4] = new Long(System.currentTimeMillis()); // new message control id 
        
        retValue = MessageFormat.format(ORDER_MESSAGE, msgParams);
        return retValue;
    }

    /**
     * The getResultsServicePort() method gets and initializes a reference to
     * the web service "port/stub".
     * 
     */
    private static OrderServicePort getOrderServicePort() throws IOException {
        OrderService service = new OrderServiceLocator();
        OrderServicePortStub port = null;

        try {

            port = (OrderServicePortStub) service.getOrderServicePort();
            port._setProperty(Stub.USERNAME_PROPERTY, USERNAME);
            port._setProperty(Stub.PASSWORD_PROPERTY, PASSWORD);
            port._setProperty(Stub.ENDPOINT_ADDRESS_PROPERTY, ENDPOINT);
            
        } catch (ServiceException e) {
            e.printStackTrace();
        }

        return port;
    }
}