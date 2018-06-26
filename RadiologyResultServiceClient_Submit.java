/**
 * An example web service client for submitting a radiology result.
 *
 * Copyright 2008 MedPlus, Inc.
 * @author MedPlus, Inc. 2008
 *
 */
package hub.sample;

import java.io.IOException;
import java.text.MessageFormat;

import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;
import javax.xml.rpc.soap.SOAPFaultException;

import com.medplus.hub.radiology.webservice.RadiologyResult;
import com.medplus.hub.radiology.webservice.RadiologyResultResponse;
import com.medplus.hub.radiology.webservice.RadiologyResultService;
import com.medplus.hub.radiology.webservice.RadiologyResultServiceLocator;
import com.medplus.hub.radiology.webservice.RadiologyResultServicePort;
import com.medplus.hub.radiology.webservice.RadiologyResultServicePortStub;

/**
 * class RadiologyResultServiceClient_Submit submits a radiology result (HL7 message) to the MedPlus Hub
 * platform.  
 * 
 */
public class RadiologyResultServiceClient_Submit {

    /**
     * You will pass the username and password when you get the stub later in
     * this example. Via the stub, you'll make the method calls to the Radiology Results
     * Web Service to submit radiology results.
     * 
     * Username and password will be provided to you by MedPlus as part of interface development.
     */
    private static final String USERNAME = "test";
    private static final String PASSWORD = "customer1";
    private static final String ENDPOINT = "https://cert.hub.care360.com/radiology/result/service";

    // SENDING_APPLICATION designates the application that is sending the radiology result
    // message to Hub
    private static final String SENDING_APPLICATION = "HUBWS";

    // SENDING_FACILITY designates the service provider name assigned to you by MedPlus.
    private static final String SENDING_FACILITY = "2135800";

    // RECEIVING_FACILITY designates a provider account name that will be receiving the result.
    // Provider accounts will be determined and provided by MedPlus as part of interface development.
    private static final String RECEIVING_FACILITY = "THO";

    /*
     * The HL7 radiology result message template - The following "template" is used in
     * constructing the radiology result.
     * 
     * 0 = sending application (field 3) 
     * 1 = sending facility (field 4) represents the account with Quest Diagnostics 
     * 2 = receiving facility (field 6) The Quest Business Unit for above account 
     * 3 = date time (field 7) 
     * 4 = message control id (field 10) Unique Number identifying this radiology result message
     * 
     */    
    private static final String RADIOLOGY_RESULT_MESSAGE = "MSH|^~\\&|{0}|{1}||{2}|{3,date,yyyyMMddHHmm}||ORU^R01|{4,number,#}|P|2.3\r"
	+ "PID|||123456789^^^F|123456789|TEST^WIFE||19560101|F|||||(206)783-4||*ENGLISH^E|S||9918210003|123456789\r"
	+ "OBR||{4,number,#}|{4,number,#}|30070^MR CHEST W/O CONTRAST^RAD||20100514095201|20100514095201|20100514095201||||||||OTH030^DOCTOR^A^^^^^UPIN||||||20100514095201|||F|||||||999999&Transcriptionist&The&A|999999&Transcriptionist&The&A||999999&<None>\r"
        + "OBX|1|FT|RAD|| ||||||F|||||999999\r"
        + "OBX|2|FT|RAD||The findings: Frontal view of the chest compared to earlier studies of||||||F|||||999999\r"
        + "OBX|3|FT|RAD||12/11/2003 and 12/31/2003.||||||F|||||999999\r"
        + "OBX|4|FT|RAD|| ||||||F|||||999999\r"
        + "OBX|5|FT|RAD||Again, the study was obtained and a poor degree of inspiration. There||||||F|||||999999\r"
        + "OBX|6|FT|RAD||is a mild prominence of the interstitial markings particularly at the||||||F|||||999999\r"
        + "OBX|7|FT|RAD||bases and these could easily represent atelectasis. The cardiac||||||F|||||999999\r"
        + "OBX|8|FT|RAD||silhouette is borderline normal. Hilar and mediastinal silhouettes are||||||F|||||999999\r"
        + "OBX|9|FT|RAD||normal for this degree of inspiration. There is no effusion or||||||F|||||999999\r"
        + "OBX|10|FT|RAD||pneumothorax.||||||F|||||999999\r"
        + "OBX|11|FT|RAD|| ||||||F|||||999999\r"
        + "OBX|12|FT|RAD||Impression:||||||F|||||999999\r"
        + "OBX|13|FT|RAD|| ||||||F|||||999999\r"
        + "OBX|14|FT|RAD||Poor inspiration with compression of pulmonary markings. No definite||||||F|||||999999\r"
        + "OBX|15|FT|RAD||acute process is identified||||||F|||||999999\r";    



    public static void main(String[] args) {        
        // Submit the radiology result
        submitRadiologyResult(); 
    }

    /**
     * 
     * The submitRadiologyResult() method will:
     * 
     * 1. Create a proxy for making SOAP calls 
     * 2. Create a Radiology Result request object which contains a valid HL7 Radiology Result message 
     * 3. Submit a Radiology Result calling submitRadiologyResult(). 
     * 4. Output response values to console.
     * 
     */
    private static void submitRadiologyResult() {
        RadiologyResultServicePort proxy;

        try {
            // -------------------------------------------------
            // STEP 1: CREATE WEB SERVICE PROXY WITH CREDENTIALS
            // -------------------------------------------------
            proxy = getRadiologyResultServicePort();

            RadiologyResult radiologyResult = null;
            RadiologyResultResponse radiologyResultResponse = null;
            
            // ------------------------------------------------------------------
            // STEP 2: call function that will create the WebService Radiology 
            // Result Object
            // ------------------------------------------------------------------
            radiologyResult = getWebServiceRadiologyResult();
            
            // --------------------------------------------------------------
            // STEP 3: call WebService function that submits radiology result
            // to the Hub
            // --------------------------------------------------------------
            radiologyResultResponse = proxy.submitRadiologyResult(radiologyResult);

            // ------------------------------------------------
            // STEP 4: Examine response coming back for the Hub
            // ------------------------------------------------
            System.out.println("Status: " + radiologyResultResponse.getStatus() + 
                               "\nMessage Control ID: " + radiologyResultResponse.getMessageControlId() +
                               "\nResponse Message: " + radiologyResultResponse.getResponseMsg());                

            String[] valErrors = radiologyResultResponse.getValidationErrors();
            
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
     * getWebServiceRadiologyResult: construct the WebService request object using member data
     * 
     */
    private static RadiologyResult getWebServiceRadiologyResult() {
        RadiologyResult retval = new RadiologyResult();
        // radiologyResultMessage is used to set the only parameter in the radiology result request object.
        String radiologyResultMessage = buildRadiologyResultMessage(SENDING_APPLICATION, SENDING_FACILITY, RECEIVING_FACILITY);
        retval.setHl7RadiologyResult(radiologyResultMessage.getBytes());
        return retval;
    }

    /**
     * buildRadiologyResultMessage: constructs a valid HL7 Radiology Result message string
     * 
     */
    private static String buildRadiologyResultMessage(String sendingApplication, String sendingFacility, String receivingFacility) {
        // return value
        String retValue = null;
        // Build an Radiology Result Message for the hl7
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
        
        retValue = MessageFormat.format(RADIOLOGY_RESULT_MESSAGE, msgParams);
        return retValue;
    }

    /**
     * The getResultsServicePort() method gets and initializes a reference to
     * the web service "port/stub".
     * 
     */
    private static RadiologyResultServicePort getRadiologyResultServicePort() throws IOException {
        RadiologyResultService service = new RadiologyResultServiceLocator();
        RadiologyResultServicePortStub port = null;

        try {

            port = (RadiologyResultServicePortStub) service.getRadiologyResultServicePort();
            port._setProperty(Stub.USERNAME_PROPERTY, USERNAME);
            port._setProperty(Stub.PASSWORD_PROPERTY, PASSWORD);
            port._setProperty(Stub.ENDPOINT_ADDRESS_PROPERTY, ENDPOINT);
            
        } catch (ServiceException e) {
            e.printStackTrace();
        }

        return port;
    }
}

