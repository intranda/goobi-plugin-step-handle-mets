package de.intranda.goobi.plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang.SystemUtils;
import org.goobi.beans.Process;
import org.goobi.beans.Step;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginReturnValue;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IStepPluginVersion2;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import lombok.Data;
import lombok.extern.log4j.Log4j;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.ContentFile;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.DocStructType;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Prefs;
import ugh.dl.Reference;
import ugh.exceptions.DocStructHasNoTypeException;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.TypeNotAllowedAsChildException;
import ugh.exceptions.TypeNotAllowedForParentException;
import ugh.exceptions.WriteException;

@Data
@Log4j
@PluginImplementation
public class ImageNameAnalyzer implements IStepPluginVersion2 {

    private PluginGuiType pluginGuiType = PluginGuiType.NONE;

    private String title = "intranda_step_imagename_analyse";

    private PluginType type = PluginType.Step;

    private Step step;

    private Pattern imagePattern;

    private boolean skipWhenDataExists;

    private Map<String, String> docstructMap;

    public ImageNameAnalyzer() {

        XMLConfiguration config = ConfigPlugins.getPluginConfig(title);
        config.setExpressionEngine(new XPathExpressionEngine());
        String regexp = config.getString("/paginationRegex");
        skipWhenDataExists = config.getBoolean("/skipWhenDataExists", false);
        imagePattern = Pattern.compile(regexp);

        docstructMap = new HashMap<>();
        List<HierarchicalConfiguration> itemList = config.configurationsAt("/structureList/item");
        for (HierarchicalConfiguration item : itemList) {
            docstructMap.put(item.getString("@filepart"), item.getString("@docstruct"));
        }
    }

    @Override
    public PluginReturnValue run() {

        Process process = step.getProzess();
        Prefs prefs = process.getRegelsatz().getPreferences();
        DocStruct physical = null;
        DocStruct logical = null;
        List<String> orderedImageNameList = null;
        Fileformat ff = null;
        DigitalDocument digDoc = null;

        String foldername = null;
        // read image names
        try {
            foldername = process.getImagesOrigDirectory(false);
            orderedImageNameList = StorageProvider.getInstance().list(foldername);
            if (orderedImageNameList.isEmpty()) {
                // abort
                log.info(process.getTitel() + ": no images found");
                return PluginReturnValue.ERROR;
            }
        } catch (IOException | InterruptedException | SwapException | DAOException e) {
            log.error(e);
            return PluginReturnValue.ERROR;
        }

        try {
            // read mets file
            ff = process.readMetadataFile();
            digDoc = ff.getDigitalDocument();
            logical = digDoc.getLogicalDocStruct();
            if (logical.getType().isAnchor()) {
                logical = logical.getAllChildren().get(0);
            }
            physical = digDoc.getPhysicalDocStruct();
            // check if pagination was already written
            List<DocStruct> pages = physical.getAllChildren();
            if (pages != null && !pages.isEmpty()) {
                if (skipWhenDataExists) {
                    return PluginReturnValue.FINISH;
                }

                // process contains data, clear it
                for (DocStruct page : pages) {
                    ff.getDigitalDocument().getFileSet().removeFile(page.getAllContentFiles().get(0));
                    List<Reference> refs = new ArrayList<>(page.getAllFromReferences());
                    for (ugh.dl.Reference ref : refs) {
                        ref.getSource().removeReferenceTo(page);
                    }
                }
                while (physical.getAllChildren() != null && !physical.getAllChildren().isEmpty()) {
                    physical.removeChild(physical.getAllChildren().get(0));
                }
                while (logical.getAllChildren() != null && !logical.getAllChildren().isEmpty()) {
                    logical.removeChild(logical.getAllChildren().get(0));
                }
            }

        } catch (ReadException | PreferencesException | WriteException | IOException | InterruptedException | SwapException | DAOException e) {
            log.error(e);
            return PluginReturnValue.ERROR;
        }

        DocStructType pageType = prefs.getDocStrctTypeByName("page");
        MetadataType physType = prefs.getMetadataTypeByName("physPageNumber");
        MetadataType logType = prefs.getMetadataTypeByName("logicalPageNumber");
        for (int index = 0; index < orderedImageNameList.size(); index++) {
            String imageName = orderedImageNameList.get(index);
            try {
                DocStruct dsPage = digDoc.createDocStruct(pageType);

                ContentFile cf = new ContentFile();
                if (SystemUtils.IS_OS_WINDOWS) {
                    cf.setLocation("file:/" + foldername + imageName);
                } else {
                    cf.setLocation("file://" + foldername + imageName);
                }
                dsPage.addContentFile(cf);

                physical.addChild(dsPage);
                Metadata mdPhysPageNo = new Metadata(physType);
                mdPhysPageNo.setValue(String.valueOf(index + 1));
                dsPage.addMetadata(mdPhysPageNo);

                Metadata mdLogicalPageNo = new Metadata(logType);
                dsPage.addMetadata(mdLogicalPageNo);
                logical.addReferenceTo(dsPage, "logical_physical");

                // compare image name against regular expression
                Matcher matcher = imagePattern.matcher(imageName);
                if (matcher.matches()) {
                    String pagination = matcher.group(1);
                    mdLogicalPageNo.setValue(pagination);
                } else {
                    mdLogicalPageNo.setValue("uncounted");
                    // compare image name against list of known abbreviations
                    boolean match = false;
                    for (String filepart : docstructMap.keySet()) {
                        if (imageName.matches(".*_" + filepart + "\\d?\\.\\w+")) {
                            DocStructType type = prefs.getDocStrctTypeByName(docstructMap.get(filepart));

                            DocStruct ds = digDoc.createDocStruct(type);
                            logical.addChild(ds);
                            ds.addReferenceTo(dsPage, "logical_physical");

                            match = true;
                            break;
                        }
                    }
                    if (!match) {
                        // no match found, use uncounted
                        log.debug(process.getTitel() + ": no match found for image" + imageName);
                    }
                }
            } catch (TypeNotAllowedForParentException | TypeNotAllowedAsChildException | MetadataTypeNotAllowedException
                    | DocStructHasNoTypeException e) {
                log.error(e);
                return PluginReturnValue.ERROR;
            }
        }
        try {
            process.writeMetadataFile(ff);
        } catch (WriteException | PreferencesException | IOException | InterruptedException | SwapException | DAOException e) {
            log.error(e);
            return PluginReturnValue.ERROR;
        }

        return PluginReturnValue.FINISH;
    }

    @Override
    public String cancel() {
        return null;
    }

    @Override
    public boolean execute() {
        PluginReturnValue val = run();

        return val == PluginReturnValue.FINISH;
    }

    @Override
    public String finish() {
        return null;
    }

    @Override
    public String getPagePath() {
        return null;
    }

    @Override
    public void initialize(Step step, String returnPath) {
        this.step = step;
    }

    @Override
    public HashMap<String, StepReturnValue> validate() {
        return null;
    }

    @Override
    public int getInterfaceVersion() {
        return 0;
    }
}
