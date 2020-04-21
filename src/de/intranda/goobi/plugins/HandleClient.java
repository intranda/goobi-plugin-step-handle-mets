package de.intranda.goobi.plugins;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jdom2.JDOMException;

import net.handle.hdllib.AbstractMessage;
import net.handle.hdllib.AbstractResponse;
import net.handle.hdllib.AdminRecord;
import net.handle.hdllib.AuthenticationInfo;
import net.handle.hdllib.CreateHandleRequest;
import net.handle.hdllib.CreateHandleResponse;
import net.handle.hdllib.Encoder;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleResolver;
import net.handle.hdllib.HandleValue;
import net.handle.hdllib.ModifyValueRequest;
import net.handle.hdllib.PublicKeyAuthenticationInfo;
import net.handle.hdllib.ResolutionRequest;
import net.handle.hdllib.ResolutionResponse;
import net.handle.hdllib.SecretKeyAuthenticationInfo;
import net.handle.hdllib.Util;
import ugh.dl.DocStruct;
import lombok.extern.log4j.Log4j;

@Log4j
public class HandleClient {

    //----------------Static fields------------------

    private String strPathPrivatePEM; 

    private static String strUserHandle = "21.T11998/USER28";
    private static String strHandleBase = "21.T11998";
    private static String strURLPrefix = "https://viewer.goobi.io/idresolver?handle=";
    private static int ADMIN_INDEX = 300; //NOT 28!
    private static int ADMIN_RECORD_INDEX = 100;
    private static int URL_RECORD_INDEX = 1;

    private static int TITLE_INDEX = 2;
    private static int AUTHORS_INDEX = 3;
    private static int PUBLISHER_INDEX = 4;
    private static int PUBDATE_INDEX = 5;
    private static int INST_INDEX = 6;

    //----------------Non-Static fields--------------

    private PrivateKey privKey;
    private PublicKeyAuthenticationInfo authInfo;

    private String strDOIMappingFile;



    //---------------Ctor: get private key------------------
    public HandleClient(String strPathPrivatePEM) throws HandleException, IOException {

        this.strPathPrivatePEM = strPathPrivatePEM;
        this.privKey = getPemPrivateKey();
        this.authInfo = new PublicKeyAuthenticationInfo(Util.encodeString(strUserHandle), ADMIN_INDEX, privKey);
    }

    //Given an object with specified ID, make a handle "id_xyz" with URL given in getURLForHandle.
    //Returns the new Handle.
    public String makeURLHandleForObject(String strObjectId, String strKundenKurz, Boolean boMakeDOI, DocStruct docstruct)
            throws HandleException {

        BasicDoi basicDOI = null;
        if (boMakeDOI) {
            try {
                MakeDOI makeDOI = new MakeDOI(strDOIMappingFile);
                basicDOI = makeDOI.getBasicDoi(docstruct);
            } catch (Exception e) {
                throw new HandleException(0, e.getMessage());
            }

        }

        String strNewHandle = newURLHandle(strHandleBase + "/goobi-" + strKundenKurz + "-" + strObjectId, strURLPrefix, true, boMakeDOI, basicDOI);
        String strNewURL = getURLForHandle(strNewHandle);

        if (changleHandleURL(strNewHandle, strNewURL)) {
            return strNewHandle;
        } else {
            throw new HandleException(HandleException.INTERNAL_ERROR, "Failed to create new Handle for " + strObjectId);
        }

    }

    //Make a new handle with specified URL.
    //If boMintNewSuffix, add a suffix guar initConfig(myconfig);anteeing uniquness.
    //Retuns the new handle.
    public String newURLHandle(String strNewHandle, String url, Boolean boMintNewSuffix, Boolean boMakeDOI, BasicDoi basicDOI)
            throws HandleException {

        if (!boMintNewSuffix && isHandleRegistered(strNewHandle)) {
            resolveRequest(strNewHandle);
            return strNewHandle;
        }

        //create a unique suffix?
        if (boMintNewSuffix) {

            int iCount = 0;
            String strTestHandle = strNewHandle;

            while (isHandleRegistered(strTestHandle)) {

                strTestHandle = strNewHandle + "-" + iCount;
                iCount++;

                if (iCount > 1000) {
                    throw new HandleException(HandleException.INTERNAL_ERROR, "Registry query always returning true: " + strNewHandle);
                }
            }

            //test handle ok:
            strNewHandle = strTestHandle;
        }

        // Define the admin record for the handle we want to create
        AdminRecord admin = createAdminRecord(strUserHandle, ADMIN_INDEX);

        // Make a create-handle request.
        HandleValue values[] = { new HandleValue(ADMIN_RECORD_INDEX, // unique index
                Util.encodeString("HS_ADMIN"), // handle value type
                Encoder.encodeAdminRecord(admin)), //data

                new HandleValue(URL_RECORD_INDEX, // unique index
                        "URL", // handle value type
                        url) }; //data

        //add DOI info?
        if (boMakeDOI) {
            HandleValue valuesDOI[] = getHandleValuesFromDOI(basicDOI);
            values = (HandleValue[]) ArrayUtils.addAll(values, valuesDOI);
        }

        // Create the request to send and the resolver to send it
        CreateHandleRequest request = new CreateHandleRequest(Util.encodeString(strNewHandle), values, authInfo);

        HandleResolver resolver = new HandleResolver();
        AbstractResponse response;

        // Let the resolver process the request
        response = resolver.processRequest(request);

        // Check the response to see if operation was successful
        if (response.responseCode == AbstractMessage.RC_SUCCESS) {

            log.info(response);

            byte[] btHandle = ((CreateHandleResponse) response).handle;
            String strFinalHandle = Util.decodeString(btHandle);
            log.info("Handle created: " + Util.decodeString(btHandle));

            return strFinalHandle;

        } else {
            throw new HandleException(HandleException.INTERNAL_ERROR, "Failed trying to create a new handle at the server, response was" + response);
        }
    }

    private String getURLForHandle(String strHandle) {

        return strURLPrefix + strHandle;
    }

    public void resolveRequest(String strHandle) throws HandleException {

        // Get the UTF8 encoding of the desired handle.
        byte bytesHandle[] = Util.encodeString(strHandle);

        // Create a resolution request.
        // (without specifying any types, indexes)
        ResolutionRequest request = new ResolutionRequest(bytesHandle, null, null, authInfo);

        HandleResolver resolver = new HandleResolver();

        AbstractResponse response = resolver.processRequest(request);

        // Check the response to see if the operation was successful.
        if (response.responseCode == AbstractMessage.RC_SUCCESS) {

            // The resolution was successful, so we'll cast the response
            // and get the handle values.
            HandleValue values[] = ((ResolutionResponse) response).getHandleValues();
            for (int i = 0; i < values.length; i++) {
                log.info(String.valueOf(values[i]));
            }
        } else {
            log.error(AbstractResponse.getResponseCodeMessage(response.responseCode) + " : " + strHandle);
        }

    }

    //Change the URL for the handle. Returns true if successful, falso otherwise
    public Boolean changleHandleURL(String handle, String newUrl) throws HandleException {

        if (StringUtils.isEmpty(handle) || StringUtils.isEmpty(newUrl))
            throw new IllegalArgumentException("handle and URL cannot be empty");

        log.info("update Handle: " + handle + " new URL: " + newUrl);

        try {
            int timestamp = (int) (System.currentTimeMillis() / 1000);
            HandleValue handleNew = new HandleValue(URL_RECORD_INDEX, "URL", newUrl);
            handleNew.setTimestamp(timestamp);

            // Make a create-handle request.
            HandleValue values[] = { handleNew };

            ModifyValueRequest req = new ModifyValueRequest(Util.encodeString(handle), values, authInfo);

            HandleResolver resolver = new HandleResolver();
            AbstractResponse response = resolver.processRequest(req);

            String msg = AbstractMessage.getResponseCodeMessage(response.responseCode);

            log.info("response code from Handle request: " + msg);

            if (response.responseCode != AbstractMessage.RC_SUCCESS) {
                return false;
            }

        } catch (HandleException e) {
            String message = "tried to update handle " + handle + " but failed: [" + e.getCode() + "] " + e.getMessage();
            log.error(message);
            throw e;
        }

        return true;
    }

    /**
     * Create the NA admin record for a new handle. The NA admin is provided all permissions bar ADD_NA and DELETE_NA
     * 
     * @return AdminRecord an AdminRecord object representing the NA handle admin
     * @param handle the NA admin handle in byte form
     * @param idx the handle index the of the NA handle's HS_VLIST entry
     */
    public AdminRecord createAdminRecord(String handle, int idx) {
        return new AdminRecord(Util.encodeString(handle), idx, AdminRecord.PRM_ADD_HANDLE, AdminRecord.PRM_DELETE_HANDLE, AdminRecord.PRM_NO_ADD_NA,
                AdminRecord.PRM_NO_DELETE_NA, AdminRecord.PRM_READ_VALUE, AdminRecord.PRM_MODIFY_VALUE, AdminRecord.PRM_REMOVE_VALUE,
                AdminRecord.PRM_ADD_VALUE, AdminRecord.PRM_MODIFY_ADMIN, AdminRecord.PRM_REMOVE_ADMIN, AdminRecord.PRM_ADD_ADMIN,
                AdminRecord.PRM_LIST_HANDLES);
    }

    public PrivateKey getPemPrivateKey() throws HandleException, IOException {

        File f = new File(strPathPrivatePEM);
        FileInputStream fis = new FileInputStream(f);
        DataInputStream dis = new DataInputStream(fis);
        byte[] keyBytes = new byte[(int) f.length()];
        dis.readFully(keyBytes);
        dis.close();

        String temp = new String(keyBytes);
        String privKeyPEM = temp.replace("-----BEGIN PRIVATE KEY-----", "");
        privKeyPEM = privKeyPEM.replace("-----END PRIVATE KEY-----", "");
        privKeyPEM = privKeyPEM.replaceAll("\r\n", "");

        byte[] decoded = Base64.getDecoder().decode(privKeyPEM);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);

        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);

        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new IOException("Failed to generate private key", e);
        }
    }

    //registered?

    public boolean isHandleRegistered(String handle) throws HandleException {

        boolean handleRegistered = false;
        ResolutionRequest req = buildResolutionRequest(handle);
        AbstractResponse response = null;
        HandleResolver resolver = new HandleResolver();
        try {
            response = resolver.processRequest(req);
        } catch (HandleException ex) {
            log.error("Caught exception trying to process lookup request");
            ex.printStackTrace();
            throw ex;
        }
        if ((response != null && response.responseCode == AbstractMessage.RC_SUCCESS)) {
            log.info("Handle " + handle + " registered.");
            handleRegistered = true;
        }
        return handleRegistered;
    }

    private ResolutionRequest buildResolutionRequest(final String handle) throws HandleException {

        //find auth info for the whole domain:
        String handlePrefix = handle.substring(0, handle.indexOf("/"));
        PublicKeyAuthenticationInfo auth = new PublicKeyAuthenticationInfo(Util.encodeString(handlePrefix), 300, privKey);

        byte[][] types = null;
        int[] indexes = null;
        ResolutionRequest req = new ResolutionRequest(Util.encodeString(handle), types, indexes, auth);
        req.certify = false;
        req.cacheCertify = true;
        req.authoritative = false;
        req.ignoreRestrictedValues = true;
        return req;
    }

    public Boolean addDOI(DocStruct physical, String handle) throws JDOMException, IOException, HandleException {

        if (StringUtils.isEmpty(handle))
            throw new IllegalArgumentException("URL cannot be empty");

        log.info("update Handle: " + handle + " Making DOI.");

        try {
            MakeDOI makeDOI = new MakeDOI(strDOIMappingFile);
            BasicDoi basicDOI = makeDOI.getBasicDoi(physical);

            // Make a create-handle request.
            HandleValue values[] = getHandleValuesFromDOI(basicDOI);

            ModifyValueRequest req = new ModifyValueRequest(Util.encodeString(handle), values, authInfo);

            HandleResolver resolver = new HandleResolver();
            AbstractResponse response = resolver.processRequest(req);

            String msg = AbstractMessage.getResponseCodeMessage(response.responseCode);

            log.info("response code from Handle request: " + msg);

            if (response.responseCode != AbstractMessage.RC_SUCCESS) {
                return false;
            }

        } catch (HandleException e) {
            String message = "tried to update handle " + handle + " but failed: [" + e.getCode() + "] " + e.getMessage();
            log.error(message);
            throw e;
        }

        return true;
    }

    private HandleValue[] getHandleValuesFromDOI(BasicDoi basicDOI) throws HandleException {

        ArrayList<HandleValue> values = new ArrayList<HandleValue>();

        for (Pair<String, List<String>> pair : basicDOI.getValues()) {
            int index = getIndex(pair.getLeft());

            for (String strValue : pair.getRight()) {
                values.add(new HandleValue(index, pair.getLeft(), strValue));
            }
        }

        int timestamp = (int) (System.currentTimeMillis() / 1000);

        for (HandleValue handleValue : values) {
            handleValue.setTimestamp(timestamp);
        }

        return values.toArray(new HandleValue[0]);
    }

    /**
     * Get the index in the handle for the specified field type.
     * 
     * @param strType
     * @return
     * @throws HandleException
     */
    private int getIndex(String strType) throws HandleException {

        switch (strType) {
            case "TITLE":
                return TITLE_INDEX;

            case "AUTHORS":
                return AUTHORS_INDEX;

            case "PUBLISHER":
                return PUBLISHER_INDEX;

            case "PUBDATE":
                return PUBDATE_INDEX;

            case "INST":
                return INST_INDEX;

            default:
                throw new HandleException(0);
        }
    }

    //Setter
    public void setDOIMappingFile(String strMappingFile) {

        this.strDOIMappingFile = strMappingFile;
    }

}
