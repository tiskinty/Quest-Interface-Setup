/**
 * An example web service client for submitting orders and getting ABN or
 * REQ documents.
 *
 * Copyright 2013 MedPlus, Inc.
 * @author MedPlus, Inc. 2012
 *
 */
package hub.sample;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.ws.soap.SOAPFaultException;

import com.medplus.orders.ObjectFactory;
import com.medplus.orders.OrderSubmissionPortType;
import com.medplus.orders.OrderSubmissionService;
import com.medplus.orders.OrderSupportServiceRequest;
import com.medplus.orders.OrderSupportServiceResponse;

/**
 * OrderSubmissionAbnReqClient submits lab order (HL7 messages) to the MedPlus Hub
 * platform with ABN/REQ document requests.  Encapsulates the sending of an HL7 order to a 
 * Quest Lab via the Hub's SOAP Web service via the JAX-WS framework.
 * 
 */
public class OrderSubmissionAbnReqClient {

    /**
     * Authentication and connection information.
     */
    private String username = "test";
    private String password = "customer1";
    private String endpoint = "https://cert.hub.care360.com/orders/submission/service";
	
    // SENDING_APPLICATION designates the application that is sending the order
    // message to Hub
    private String sendingApplication = "HUBWS";

    // SENDING_FACILITY designates the account number provided to you by Quest
    // for the business unit you are ordering tests with.  This is a Provider
    // Account name
    private String sendingFacility = "2135800";

    // RECEIVING_FACILITY designates the business unit within Quest from which
    // the labs are being ordered.  This is a Provider name
    private String receivingFacility = "HOU";

    /*
     * The HL7 Order message template - The following "template" is used in
     * constucting the Order.
     * 
     * 0 = sending application (field 3) 1 = sending facility (field 4)
     * represents the account with Quest Diagnostics 2 = receiving facility
     * (field 6) The Quest Business Unit for above account 3 = date time (field
     * 7) 4 = message control id (field 10) Unique Number identifying this order
     * message
     */
    private static final String ORDER_MESSAGE = "MSH|^~\\&|{0}|{1}||{2}|{3,date,yyyyMMddHHmm}||ORM^O01|{4,number,#}|P|2.3\r"
            + "PID|1|11111|||TEST^WIFE||19451212|M|||||3102222222||||||123456789|\r"
            + "IN1|1||AUHSC|AETNA|123 ANYSTREET^2^CHICAGO^IL^60305|||A12345||||||||TEST^HUSBAND^|2||123 ANYSTREET^^CHICAGO^IL^60305|||||||||||||||||P123456R|||||||||||T|\r"
            + "GT1|1||TEST^HUSBAND^||123 ANYSTREET^^CHICAGO^IL^60305|3102222222||19451212|M|\r"
            + "ORC|NW|{4,number,#}||||||||||OTH030^MICHIGAN^JOHN^^^^^UPIN|\r"
            + "OBR|1|{4,number,#}||^^^6399^CBC|||20051223094800|||||||||OTH030^MICHIGAN^JOHN^^^^^UPIN|||||||||||1^^^^^R|\r"
            + "DG1|1|ICD|0039|SALMONELLA INFECTION NOS|\r"
            + "OBX|1||^^^A123^SOME OBSERVATION||||||||P\r"
            + "ORC|NW|{4,number,#}||||||||||OTH030^MICHIGAN^JOHN^^^^^UPIN|\r" 
            + "OBR|1|{4,number,#}||^^^10809^TISSUE|||20051223094800|||||||||OTH030^MICHIGAN^JOHN^^^^^UPIN|||||||||||1^^^^^R|";
            

    /**
     * Sends an order to the Hub
     * @param args
     */
    public static void main(String[] args) {
        try {
            new OrderSubmissionAbnReqClient().sendOrder();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public OrderSubmissionAbnReqClient() {
        // load a properties file, if present
        Properties props = new Properties();
        try {
            props.load(new FileInputStream("orderSubmission.properties"));
            username = props.getProperty("username", username);
            password = props.getProperty("password", password);
            endpoint = props.getProperty("endpoint", endpoint);
            sendingApplication = props.getProperty("sendingApplication", sendingApplication);
            sendingFacility = props.getProperty("sendingFacility", sendingFacility);
            receivingFacility = props.getProperty("receivingFacility", receivingFacility);
            
        } catch (IOException e) {
            System.out.println("Cannot load orderSubmission.properties file.  Defaults (cert settings) will be used");
        }
    }
    
    /**
     * 
     * The sendOrder() method will:
     * 
     * 1. Set up HTTP Authentication
     * 2. Create an Order request object which contains a valid HL7 Order message 
     * 3. Submit a Lab Order calling submitOrder(). 
     * 4. Output response values to console.
     * 
     */
    private void sendOrder() {

        // -------------------------------------------------
        // STEP 1: SET UP AUTHENTICATION
        // -------------------------------------------------

        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password.toCharArray());
            }
        });

        // -------------------------------------------------
        // STEP 2: Create web service client and port
        // -------------------------------------------------

        System.out.println("Creating service client for " + endpoint);
        try {

            OrderSubmissionService service = new OrderSubmissionService(new URL(endpoint + "?wsdl"), new QName(
                    "http://medplus.com/orders", "OrderSubmissionService"));
            System.out.println("Retrieving the port from the following service: " + service);
            OrderSubmissionPortType port = service.getOrderSubmissionPortTypePort();

            // --------------------------------------------------------------
            // STEP 3: call WebService function that submits order to the
            // Hub
            // --------------------------------------------------------------
            System.out.println("Invoking the submitOrder operation on the port.");
            OrderSupportServiceRequest submitOrderRequest = getOrderSupportRequest();
            OrderSupportServiceResponse response = port.getOrderDocuments(submitOrderRequest);

            // ------------------------------------------------
            // STEP 4: Examine response coming back for the Hub
            // ------------------------------------------------
            System.out.println("Status: " + response.getStatus() + "\nTransaction ID: "
                    + response.getOrderTransactionUid() + "\nMessage Control ID: " + response.getMessageControlId()
                    + "\nResponse Message: " + response.getResponseMsg());
            if ((response.getOrderSupportDocuments()!=null) && (response.getOrderSupportDocuments().size()>0)) {
                System.out.println("Document Type: " + response.getOrderSupportDocuments().get(0).getDocumentType());
                System.out.println("Document Status: " + response.getOrderSupportDocuments().get(0).getRequestStatus());
                System.out.println("Document Message: " + response.getOrderSupportDocuments().get(0).getResponseMessage());
                System.out.println("\n" + "HL7 Order ACK: " + "\n" + new String(response.getHl7OrderAck()) + "\n\n");
                if (response.getOrderSupportDocuments().get(0).getDocumentData() != null)
                {
                    FileOutputStream fos = new FileOutputStream(response.getOrderSupportDocuments().get(0).getDocumentType() + response.getMessageControlId() + ".pdf");
                    fos.write(response.getOrderSupportDocuments().get(0).getDocumentData());
                }

            }
                
            List<String> valErrors = response.getValidationErrors();

            if (null != valErrors) {
                for (int ndx = 0; ndx < valErrors.size(); ndx++) {
                    System.out.println("\tValidation Error: " + valErrors.get(ndx) + ".");
                }
            }
        } catch (Exception e) {
            if (e.getCause() instanceof SOAPFaultException) {
                SOAPFaultException sfe = (SOAPFaultException) e.getCause();
                System.out.println(sfe.getMessage());
            }
            e.printStackTrace();
        }
    }

    /**
     * getOrderSupportRequest: construct the WebService request object using member
     * data
     * @return
     */
    private OrderSupportServiceRequest getOrderSupportRequest() {
        OrderSupportServiceRequest request = new ObjectFactory().createOrderSupportServiceRequest();
        // orderMessage is used to set the only parameter in the order request
        // object.
        String orderMessage = buildOrderMessage(sendingApplication, sendingFacility, receivingFacility);
        request.setHl7Order(orderMessage.getBytes());
        
        // request both ABN and REQ documents
        request.getOrderSupportRequests().add("ABN");
        request.getOrderSupportRequests().add("REQ");
        request.getOrderSupportRequests().add("AOE");
        return request;
    }

    /**
     * buildOrderMessage: constructs a valid HL7 Order message string
     * @param sendingApplication
     * @param sendingFacility
     * @param receivingFacility
     * @return
     */
    private String buildOrderMessage(String sendingApplication, String sendingFacility, String receivingFacility) {
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
        msgParams[4] = new Long(System.currentTimeMillis()); // new message
                                                             // control id

        retValue = MessageFormat.format(ORDER_MESSAGE, msgParams);
        return retValue;
    }

}
