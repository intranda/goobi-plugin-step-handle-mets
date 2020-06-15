package de.intranda.goobi.plugins;

import java.io.File;
import java.io.IOException;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;

import net.handle.hdllib.HandleException;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Prefs;
import ugh.fileformats.mets.MetsMods;

public class TestHandles {

 // "/home/joel/Handles/zertifikate/21.T11998_USER28-priv.pem";
    //    public static String strPathCertPEM = "/home/joel/Handles/zertifikate/21.T11998_USER28-cert.pem";

    //for testing:
    private static String rulesetExample = "/opt/digiverso/goobi/test/klassik.xml";
    private static String xmlExample = "/opt/digiverso/goobi/test/meta.xml";
    private static String xmlOut = "/opt/digiverso/goobi/test/doiTEST.xml";
    
    private static String strConfig = "/opt/digiverso/goobi/config/plugin_intranda_step_handle_mets.xml"
            
    public TestHandles() {
        // TODO Auto-generated constructor stub
    }

    

    //Testing:
    public static void main(String[] args) throws Exception {

        METSHandlePlugin plug = new METSHandlePlugin();

        try {

            XMLConfiguration xmlConfig = new XMLConfiguration(new File(strConfig));
            xmlConfig.setExpressionEngine(new XPathExpressionEngine());
            xmlConfig.setReloadingStrategy(new FileChangedReloadingStrategy());

            SubnodeConfiguration myconfig = null;
            myconfig = xmlConfig.configurationAt("//config[./project = '*'][./step = '*']");

            plug.config = myconfig;

            plug.prefs = new Prefs();
            plug.prefs.loadPrefs(rulesetExample);
            plug.urn = plug.prefs.getMetadataTypeByName("_urn");

            //read the metatdata
            Fileformat fileformat = new MetsMods(plug.prefs);
            fileformat.read(xmlExample);

            DigitalDocument digitalDocument = fileformat.getDigitalDocument();
            DocStruct logical = digitalDocument.getLogicalDocStruct();
            DocStruct physical = digitalDocument.getPhysicalDocStruct();

            String strId = plug.getId(logical);

            //add handles to each physical and logical element
            String strLogicalHandle = plug.addHandle(logical, strId, true);
            
            //already carried out: "21.T11998/goobi-go-1296243265-1"; 
            // http://hdl.handle.net/21.T11998/goobi-go-1296243265-1
            
            plug.addHandle(physical, strId);

            //            //Add DOI?
            //            if (plug.config.getBoolean("MakeDOI")) {
            //
            //                plug.addDOI(logical, strLogicalHandle);
            //            }

            fileformat.write(xmlOut);

        } catch (Exception e) {

            log.error(e.getMessage(), e);
        }
    }

    
    //-----------------Testing:---------------------
    public static void testClient(String[] args) throws HandleException, IOException {

        HandleClient test = new HandleClient(args[0]);

        //        test.resolveRequest("300:200/23");
        //        test.resolveRequest("45678/1");
        //        test.resolveRequest("200/23");
        //      test.resolveRequest(21.T11998/TEST015c2702c8-d2ca-40b7-9220-400225b19cc3);
        //        test.resolveRequest("10.1594/GFZ.ISDC.CHAMP/CH-ME-2-PLP");
        //
        //        test.resolveRequest("20.1000/100");
        //        test.resolveRequest("21.T11998/TEMPLATEHANDLE");
        //        test.resolveRequest(strUserHandle);
        //
        //        String strUniqueHandle = test.newURLHandle("21.T11998/TEST02", "https://stackoverflow.com/", false);
        //        //        test.changleHandleURL(strUniqueHandle, "https://www.theguardian.com/international");
        //        test.resolveRequest(strUniqueHandle);
        //
        test.resolveRequest("21.T11998/TEST02");
        //        test.changleHandleURL("21.T11998/TEST02", "https://www.theguardian.com/international");
        //        test.resolveRequest("21.T11998/TEST02");

        //        AddValueRequest 
    }
}
