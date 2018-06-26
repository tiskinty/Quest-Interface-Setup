/**
 * An example web service client for submitting patient demographics.
 * 
 * Copyright 2008 MedPlus, Inc.
 * 
 * @author MedPlus, Inc. 2008
 * 
 */
package hub.sample;

import java.io.IOException;
import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;
import javax.xml.rpc.Stub;

import com.medplus.hub.demographics.webservice.DemographicRequest;
import com.medplus.hub.demographics.webservice.DemographicResponse;
import com.medplus.hub.demographics.webservice.DemographicService;
import com.medplus.hub.demographics.webservice.DemographicServiceLocator;
import com.medplus.hub.demographics.webservice.DemographicServicePort;
import com.medplus.hub.demographics.webservice.DemographicServicePortStub;

/**
 * class PatientDemographicServiceClient sends patient demographic information to Hub Information Services.
 * 
 */

public class PatientDemographicServiceClient {
    /**
     * Replace "test" and "customer1" with the username and password Medplus has
     * assigned to you.
     * 
     * You will pass the username and password when you get the stub later in
     * this example.
     */
    private static final String USERNAME = "test";
    private static final String PASSWORD = "customer1";
    public static final String ENDPOINT = "https://cert.hub.care360.com/demographic/service";
    
    // Sending application (field 3) identifier in the HL7 ADT Message.
    public static final String SENDING_APPLICATION = "HUBWS";
    public static final String SENDING_APPLICATION_PLACEHOLDER = "SENDING_APP";
    
    // Sending facility (field 4) the account with Quest Diagnostics
    public static final String HUB_ACCOUNT = "2135800";
    public static final String HUB_ACCOUNT_PLACEHOLDER = "HUB_ACCT";
    
    // Receiving facility (field 6) The Quest Business Unit for the above account
    public static final String RECEIVING_FACILITY = "THO";
    public static final String RECEIVING_FACILITY_PLACEHOLDER = "REC_FAC";
    
    // Date/Time (field 7) in the following format: yyyyMMddHHmmssss
    public static final String DATE_TIME = "20091231093000";
    public static final String DATE_TIME_PLACEHOLDER = "DATE_TIME";
    
    // Message control ID (field 10) Unique number identifying this ADT message
    public static final String MSG_CONTROL_ID = "msgControlID123";
    public static final String MSG_CONTROL_ID_PLACEHOLDER = "MSG_CTRL_ID";
    
    public static final String ADT_MESSAGE =  
        "MSH|^~\\&|" + SENDING_APPLICATION_PLACEHOLDER + "|" + HUB_ACCOUNT_PLACEHOLDER + "||" + RECEIVING_FACILITY_PLACEHOLDER + "|" + DATE_TIME_PLACEHOLDER + "||ADT^A28|" + MSG_CONTROL_ID_PLACEHOLDER +"|P|2.3\r" 
        + "EVN|A28|199608190820\r" +
        "PID|1|pid123|^^^LH||Wally^SHERRY^M||20000101|F|PETRY^SHERRY||4690 Parkway Dr.^address line 2^Mason^OH^45040^USA|a2|^^^^86^999^9999999^99999|513-999-9999|a5|||1-FOUND|444-66-9999\r" +
        "PV1|1|O\r" +
        "GT1|1|88|Smith^John^M^JR^DR^MD||3710 Emery Lake Ln^Street line2^Cincinnati^OH^45010|^^^^1^513^8888888^1234|^^^^1^238^4444444^5678|19960708112233|M|I|8|287889999||||ABC Inc.^Limited^M |4567 Kelly Drive^address line 2^Oxford^OH^45068|55566677777|4556|FT|Guarantor Organization\r" +
        "IN1|1|INSID123^Insurance Plan ABC|INSCOID123|insuranceco|1800 Insurance Rd.^^Detroit^MI^45777||^^^^1^555^6667777^1234|3433|name|||||^19960707|||||||||||||||||||||||||||||||||T\r";

    public static void main(String[] args) {
        try {
            // Create a web service proxy
            DemographicServicePort proxy = getDemographicServicePort();
            
            // create the DemographicRequest and populate it with the ADT_MESSAGE
            DemographicRequest request = new DemographicRequest();
            request.setADTMessage(createADTMessage().getBytes());
            
            // submit the Realtime message
            DemographicResponse response = proxy.submitRealTimeADTMessage(request);
            
            // print the status
            System.out.println("Status: " + response.getStatus());
            
            // print errors if applicable
            String[] errors = response.getErrors();
            
            if(errors != null && errors.length > 0) {
                System.out.println("Errors:");
                for(int i=0; i<errors.length; i++) {
                    System.out.println(errors[i]);
                }
            }
            
            // submit the Batch message
            response = null;
            response = proxy.submitBatchADTMessage(request);
            
            // print the status
            System.out.println("Status: " + response.getStatus());
            
            // print errors if applicable
            errors = null;
            errors = response.getErrors();
            
            if(errors != null && errors.length > 0) {
                System.out.println("Errors:");
                for(int i=0; i<errors.length; i++) {
                    System.out.println(errors[i]);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ServiceException e) {
            e.printStackTrace();
        }
    }

    /**
     * The DemographicServicePort() method gets and initializes a reference to
     * the web service "port/stub".
     * 
     */
    private static DemographicServicePort getDemographicServicePort() throws IOException, ServiceException {
        DemographicService service = new DemographicServiceLocator();
        DemographicServicePortStub port = null;

        port = (DemographicServicePortStub) service.getDemographicServicePort();
        port._setProperty(Stub.USERNAME_PROPERTY, USERNAME);
        port._setProperty(Stub.PASSWORD_PROPERTY, PASSWORD);
        port._setProperty(Stub.ENDPOINT_ADDRESS_PROPERTY, ENDPOINT);
        return port;
    }
    
    private static String createADTMessage() {
    	
    	String msg = ADT_MESSAGE.replace(SENDING_APPLICATION_PLACEHOLDER, SENDING_APPLICATION);
    	msg = msg.replace(HUB_ACCOUNT_PLACEHOLDER, HUB_ACCOUNT);
    	msg = msg.replace(RECEIVING_FACILITY_PLACEHOLDER, RECEIVING_FACILITY);
    	msg = msg.replace(DATE_TIME_PLACEHOLDER, DATE_TIME);
    	msg = msg.replace(MSG_CONTROL_ID_PLACEHOLDER, MSG_CONTROL_ID);
    	return msg;
    }
}
