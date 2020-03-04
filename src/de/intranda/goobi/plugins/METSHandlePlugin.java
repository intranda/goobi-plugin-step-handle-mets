package de.intranda.goobi.plugins;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import net.handle.hdllib.HandleException;
import net.xeoh.plugins.base.annotations.PluginImplementation;

import org.goobi.beans.Step;
import org.apache.commons.configuration.XMLConfiguration;
import org.goobi.beans.LogEntry;
import org.goobi.beans.Process;
import org.goobi.beans.Ruleset;
import org.goobi.production.enums.LogType;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.plugin.interfaces.IStepPlugin;

import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Prefs;
import ugh.dl.Reference;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.WriteException;
import ugh.fileformats.mets.MetsMods;
import uk.gov.nationalarchives.droid.core.signature.FileFormat;
import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.enums.StepEditType;
import de.sub.goobi.helper.enums.StepStatus;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.metadaten.MetadatenHelper;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.RulesetManager;
import de.sub.goobi.persistence.managers.StepManager;
import lombok.extern.log4j.Log4j;

@PluginImplementation
@Log4j
public class METSHandlePlugin implements IStepPlugin, IPlugin {

    private String title = "intranda_step_handle_mets";
    private static final String PLUGIN_NAME = "HandleMETS";
    private static final String PEM_FILE = "home/joel/Handles/zertifikate/21.T11998_USER28-priv.pem";

    //for testing:
    private static String rulesetExample = "/opt/digiverso/goobi/test/klassik.xml";
    private static String xmlExample = "/opt/digiverso/goobi/test/meta.xml";
    private static String xmlOut = "/opt/digiverso/goobi/test/metadataTEST.xml";

    private Process process;
    private Step step;
    private String returnPath;
    private Prefs prefs;

    private MetadataType urn;

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

        try {
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
            plug.addHandle(logical, strId);
            plug.addHandle(physical, strId);

            fileformat.write(xmlOut);

        } catch (Exception e) {

            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void initialize(Step step, String returnPath) {

        this.returnPath = returnPath;
        this.step = step;
        this.process = step.getProzess();
        prefs = process.getRegelsatz().getPreferences();
        this.urn = prefs.getMetadataTypeByName("_urn");
    }

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
            addHandle(logical, strId);
            addHandle(physical, strId);

            //and save the metadata again.
            process.writeMetadataFile(fileformat);

        } catch (Exception e) {

            log.error(e.getMessage(), e);
        }

        return false;
    }

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

    // check if Metadata handle exists
    // if not, create handle and save it under "_urn" in the docstruct.
    public void addHandle(DocStruct docstruct, String strId) throws HandleException, IOException, MetadataTypeNotAllowedException {

        XMLConfiguration config = ConfigPlugins.getPluginConfig(title);
        String strPEMFile = config.getString("PEMFile", PEM_FILE);
        HandleClient handler = new HandleClient(strPEMFile);

        if (docstruct.getAllChildren() != null) {
            // run recursive through all children
            for (DocStruct ds : docstruct.getAllChildren()) {
                addHandle(ds, strId);
            }
        } else {

            //already has a handle?
            String strHandle = getHandle(docstruct);
            if (strHandle == null) {
                //if not, make one.
                strHandle = handler.makeURLHandleForObject(strId);
                setHandle(docstruct, strHandle);
            }
        }

    }

    //If the element already has a handle, return it, otherwise return null.
    private String getHandle(DocStruct docstruct) {

        List<? extends Metadata> lstURN = docstruct.getAllMetadataByType(urn);

        if (lstURN.size() == 1) {
            return lstURN.get(0).getValue();
        }

        //otherwise
        return null;
    }

    //Add metadata to the element containing the handle.
    private void setHandle(DocStruct docstruct, String strHandle) throws MetadataTypeNotAllowedException {

        Metadata md = new Metadata(urn);
        md.setValue(strHandle);
        docstruct.addMetadata(md);
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
