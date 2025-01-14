package de.intranda.goobi.plugins;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.goobi.beans.Process;
import org.goobi.beans.Step;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.plugin.interfaces.IStepPlugin;

//import uk.gov.nationalarchives.droid.core.signature.FileFormat;
import de.sub.goobi.config.ConfigPlugins;
import lombok.extern.log4j.Log4j;
import net.handle.hdllib.HandleException;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Prefs;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.fileformats.mets.MetsMods;

/**
 * Plugin for registering Handles for documents
 */

@PluginImplementation
@Log4j
public class METSHandlePlugin implements IStepPlugin, IPlugin {

    private static final String PLUGIN_NAME = "intranda_step_handle_mets";
    private static final String PEM_FILE = "/opt/digiverso/goobi/config/zertifikate/21.T11998_USER28-priv.pem";

    private Process process;
    private Step step;
    private String returnPath;
    private Prefs prefs;

    private MetadataType urn;
    private SubnodeConfiguration config;

    @Override
    public PluginType getType() {
        return PluginType.Step;
    }

    @Override
    public String getTitle() {
        return PLUGIN_NAME;
    }

    public String getDescription() {
        return PLUGIN_NAME;
    }

    //Testing:
    public static void main(String[] args) throws Exception {

        METSHandlePlugin plug = new METSHandlePlugin();
        String rulesetExample = "/opt/digiverso/goobi/test/klassik.xml";
        String xmlExample = "/opt/digiverso/goobi/test/meta.xml";
        String xmlOut = "/opt/digiverso/goobi/test/doiTEST.xml";
        String strConfig = "/opt/digiverso/goobi/config/plugin_intranda_step_handle_mets.xml";
        String strHandlesToRemove = "/home/joel/git/rechtsgeschichte/for_mpi/handles/removeHandles.txt";

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

            //remove specified handles:
            if (args.length == 1) {

                strHandlesToRemove = args[0];
                plug.removeHandles(strHandlesToRemove);
            }

            //            //read the metatdata
            //            Fileformat fileformat = new MetsMods(plug.prefs);
            //            fileformat.read(xmlExample);
            //
            //            DigitalDocument digitalDocument = fileformat.getDigitalDocument();
            //            DocStruct logical = digitalDocument.getLogicalDocStruct();
            //            DocStruct physical = digitalDocument.getPhysicalDocStruct();

            //            String strId = plug.getId(logical);

            //add handles to each physical and logical element
            //            String strLogicalHandle = plug.addHandle(logical, strId, true);

            //already carried out: "21.T11998/goobi-go-1296243265-1"; 
            // http://hdl.handle.net/21.T11998/goobi-go-1296243265-1

            //            plug.addHandle(physical, strId, false);

            //            //Add DOI?
            //            if (plug.config.getBoolean("MakeDOI")) {
            //
            //                plug.addDOI(logical, strLogicalHandle);
            //            }

            //            fileformat.write(xmlOut);

        } catch (Exception e) {

            log.error(e.getMessage(), e);
        }
    }

    /**
     * Sets up the config file
     */
    @Override
    public void initialize(Step step1, String returnPath) {

        this.returnPath = returnPath;
        this.step = step1;

        String projectName = step.getProzess().getProjekt().getTitel();

        XMLConfiguration xmlConfig = ConfigPlugins.getPluginConfig(PLUGIN_NAME);
        xmlConfig.setExpressionEngine(new XPathExpressionEngine());
        xmlConfig.setReloadingStrategy(new FileChangedReloadingStrategy());

        SubnodeConfiguration myconfig = null;

        // order of configuration is:
        // 1.) project name and step name matches
        // 2.) step name matches and project is *
        // 3.) project name matches and step name is *
        // 4.) project name and step name are *
        try {
            myconfig = xmlConfig.configurationAt("//config[./project = '" + projectName + "'][./step = '" + step.getTitel() + "']");
        } catch (IllegalArgumentException e) {
            try {
                myconfig = xmlConfig.configurationAt("//config[./project = '*'][./step = '" + step.getTitel() + "']");
            } catch (IllegalArgumentException e1) {
                try {
                    myconfig = xmlConfig.configurationAt("//config[./project = '" + projectName + "'][./step = '*']");
                } catch (IllegalArgumentException e2) {
                    myconfig = xmlConfig.configurationAt("//config[./project = '*'][./step = '*']");
                }
            }
        }

        this.config = myconfig;

        this.returnPath = returnPath;
        this.process = step.getProzess();
        prefs = process.getRegelsatz().getPreferences();
        this.urn = prefs.getMetadataTypeByName("_urn");
    }

    /**
     * Carry out the plugin: - get the current digital document - for each physical and logical element of the document, create and register a handle
     * - write the handles into the MetsMods file for the document
     * 
     */
    @Override
    public boolean execute() {

        try {
            //read the metatdata
            Fileformat fileformat = process.readMetadataFile();

            DigitalDocument digitalDocument = fileformat.getDigitalDocument();
            DocStruct logical = digitalDocument.getLogicalDocStruct();
            DocStruct physical = digitalDocument.getPhysicalDocStruct();

            String strId = getId(logical);

            //add handles to each physical and logical element
            Boolean boMakeDOI = config.getBoolean("MakeDOI");
            String strLogicalHandle = addHandle(logical, strId, boMakeDOI);
            String strPhysicalHandle = addHandle(physical, strId, false);

            //and save the metadata again.
            process.writeMetadataFile(fileformat);

        } catch (Exception e) {

            log.error(e.getMessage(), e);
        }

        return false;
    }

    /**
     * Returns the CatalogIDDigital for the struct
     */
    private String getId(DocStruct logical) {

        List<Metadata> lstMetadata = logical.getAllMetadata();

        if (lstMetadata != null) {
            for (Metadata metadata : lstMetadata) {
                if (metadata.getType().getName().equals("CatalogIDDigital")) {
                    return metadata.getValue();
                }
            }
        }

        //otherwise:
        return null;
    }

    /**
     * check if Metadata handle exists if not, create handle and save it under "_urn" in the docstruct.
     * 
     * @return Returns the handle.
     */
    public String addHandle(DocStruct docstruct, String strId, Boolean boMakeDOI)
            throws HandleException, IOException, MetadataTypeNotAllowedException {

        HandleClient handler = new HandleClient(config);

        if (docstruct.getAllChildren() != null) {
            // run recursive through all children
            for (DocStruct ds : docstruct.getAllChildren()) {
                return addHandle(ds, strId, false);
            }
        } else {

            //already has a handle?
            String strHandle = getHandle(docstruct);
            if (strHandle == null) {
                //if not, make one.
                if (boMakeDOI) {
                    handler.setDOIMappingFile(config.getString("DOIMappingFile", null));
                }

                //Handle looks like 
                String strKundenKurz = config.getString("HandleInstitutionAbbr");
                String strIdPrefix = config.getString("HandleIdPrefix");
                String strPostfix = "";

                if (strIdPrefix != null && !strIdPrefix.isEmpty()) {
                    strPostfix = strIdPrefix + "-";
                }
                if (strKundenKurz != null && !strKundenKurz.isEmpty()) {
                    strPostfix = strPostfix + strKundenKurz + "-";
                }

                strHandle = handler.makeURLHandleForObject(strId, strPostfix, boMakeDOI, docstruct);
                setHandle(docstruct, strHandle);
            }

            return strHandle;
        }

        return null;
    }

    /**
     * If the element already has a handle, saved in the "_urn" metadatum, return it, otherwise return null.
     */
    private String getHandle(DocStruct docstruct) {

        List<? extends Metadata> lstURN = docstruct.getAllMetadataByType(urn);

        if (lstURN.size() == 1) {
            return lstURN.get(0).getValue();
        }

        //otherwise
        return null;
    }

    /**
     * Add metadata to the element containing the handle.
     */
    private void setHandle(DocStruct docstruct, String strHandle) throws MetadataTypeNotAllowedException {
        Metadata md = new Metadata(urn);
        md.setValue(strHandle);
        docstruct.addMetadata(md);
    }

    private void removeHandles(String strHandlesToRemove) throws HandleException, IOException {

        HandleClient handler = new HandleClient(config);
        List<String> lstHandlesToRemove = Files.readAllLines(Paths.get(strHandlesToRemove));

        for (String strHandle : lstHandlesToRemove) {
            if (strHandle != null) {

                handler.remove(strHandle);
            }

        }
    }

    @Override
    public String cancel() {
        return returnPath;
    }

    @Override
    public String finish() {
        return returnPath;
    }

    @Override
    public HashMap<String, StepReturnValue> validate() {
        return null;
    }

    @Override
    public Step getStep() {
        return null;
    }

    @Override
    public PluginGuiType getPluginGuiType() {
        return PluginGuiType.NONE;
    }

    @Override
    public String getPagePath() {
        return null;
    }

}
