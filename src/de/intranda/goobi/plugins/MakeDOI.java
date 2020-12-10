package de.intranda.goobi.plugins;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Year;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
//import org.wiztools.xsdgen.ParseException;

import ugh.dl.DocStruct;
import ugh.dl.Metadata;

/**
 * TODO: Please document this class
 */
public class MakeDOI {

    /**
     * Static entry point
     * 
     * @param args
     * @throws IOException
     * @throws ParseException
     * @throws ConfigurationException
     * @throws JDOMException
     */
    public static void main(String[] args) throws IOException, ConfigurationException, JDOMException {
        System.out.println("Start DOI");

        MakeDOI makeDoi = new MakeDOI(args[0]);

        makeDoi.saveXMLStructure("/home/joel/XML/orig.xml", "/home/joel/XML/doi_final.xml", "handle/number");

        System.out.println("Finished");
    }

    //The mapping document
    private Document mapping;

    //dictionary of mappings
    private HashMap<String, Element> doiMappings;

    /**
     * ctor
     * 
     * @throws IOException
     * @throws JDOMException
     */
    public MakeDOI(String strMappingFile) throws JDOMException, IOException {

        SAXBuilder builder = new SAXBuilder();
        File xmlFile = new File(strMappingFile);

        this.mapping = (Document) builder.build(xmlFile);
        this.doiMappings = new HashMap<String, Element>();

        Element rootNode = mapping.getRootElement();

        for (Element elt : rootNode.getChildren()) {

            doiMappings.put(elt.getChildText("doiElt"), elt);
        }
    }

    /**
     * Given the root elt of the xml file which we are examining, find the text of the entry correspoinding to the DOI element specified
     * 
     * @param strDoiElt
     * @param root
     * @return
     */
    public List<String> getValues(String strDoiElt, Element root) {

        Element eltMap = doiMappings.get(strDoiElt);
        if (eltMap == null) {
            return null;
        }

        ArrayList<String> lstValues = new ArrayList<String>();

        //set up the default value:
        String strDefault = eltMap.getChildText("default");
        ArrayList<String> lstDefault = new ArrayList<String>();
        if (!strDefault.isEmpty()) {
            lstDefault.add(strDefault);
        }

        //try to find the local value:
        String strLocalElt = eltMap.getChildText("localElt");

        //no local value set? then return default:
        if (strLocalElt.isEmpty()) {
            return lstDefault;
        }

        //otherwise
        List<String> lstLocalValues = getValueRecursive(root, strLocalElt);

        if (!lstLocalValues.isEmpty()) {

            return lstLocalValues;
        }

        //could not find first choice? then try alternatives
        for (Element eltAlt : eltMap.getChildren("altLocalElt")) {

            lstLocalValues = getValueRecursive(root, eltAlt.getText());

            if (!lstLocalValues.isEmpty()) {

                return lstLocalValues;
            }
        }

        //otherwise just return default
        return lstDefault;
    }

    /**
     * Find all child elements with the specified name, and return a list of all their values. Not ethis will STOP at the first level at which it
     * finds a hit: if there are "title" elements at level 2 it will return all of them, and will NOT continue ti look for "title" elts at lower
     * levels.
     * 
     * @param root
     * @param strLocalElt
     * @return
     */
    private List<String> getValueRecursive(Element root, String strLocalElt) {

        ArrayList<String> lstValues = new ArrayList<String>();

        //if we find the correct named element, do NOT include its children in the search:
        if (root.getName() == strLocalElt) {
            lstValues.add(root.getText());
            return lstValues;
        }

        //recursive:
        for (Element eltChild : root.getChildren()) {

            lstValues.addAll(getValueRecursive(eltChild, strLocalElt));
        }

        return lstValues;
    }

    /**
     * Get the xml in strXmlFilePath, create a DOI file, and save it at strSave.
     * 
     * @param strXmlFilePath
     * @param strSave
     * @throws JDOMException
     * @throws IOException
     */
    public void saveXMLStructure(String strXmlFilePath, String strSave, String strDOI) throws JDOMException, IOException {

        Document docEAD = new Document();
        Element rootNew = new Element("resource");
        docEAD.setRootElement(rootNew);

        makeHeader(rootNew, strDOI);

        SAXBuilder builder = new SAXBuilder();
        File xmlFile = new File(strXmlFilePath);

        Document document = (Document) builder.build(xmlFile);
        Element rootNode = document.getRootElement();

        //mandatory fields:
        addMandatoryFields(rootNew);

        //        //optional
        //        addOptionalFields(rootNew);

        //now save:
        XMLOutputter outter = new XMLOutputter();
        outter.setFormat(Format.getPrettyFormat().setIndent("    "));
        outter.output(rootNew, new FileWriter(new File(strSave)));
    }

    /**
     * set the resource attribute, and the identifier and creators nodes
     * 
     * @param root
     * @param strDOI
     */
    private void makeHeader(Element root, String strDOI) {

        Namespace sNS = Namespace.getNamespace("xxxxxxxxx", "http://datacite.org/schema/kernel-4");

        root.setAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", sNS);
        root.setAttribute("schemaLocation", "http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4.2/metadata.xsd", sNS);

        //DOI
        Element ident = new Element("identifier");
        ident.setAttribute("identifierType", "DOI");
        ident.addContent(strDOI);
        root.addContent(ident);

        //Creators
        Element creators = new Element("creators");
        Element creator = new Element("creator");
        List<String> lstCreatorNames = getValues("creatorName", root);

        for (String strCreatorName : lstCreatorNames) {
            Element creatorName = new Element("creatorName");
            creatorName.addContent(strCreatorName);
            creator.addContent(creatorName);
        }

        creators.addContent(creator);
        root.addContent(creators);

    }

    /**
     * TODO: Please document this method
     */
    private void addElements(Element parent, String strEltName) {

        List<String> lstValues = getValues(strEltName, parent);
        for (String strValue : lstValues) {
            Element elt = new Element(strEltName);
            elt.addContent(strValue);
            parent.addContent(elt);
        }
    }

    /**
     * Add the Title, Publisher, PublicationYear and ResourceType fields
     * 
     * @param rootNew
     * @param rootNode
     */
    private void addMandatoryFields(Element rootNew) {

        //title
        Element titles = new Element("titles");
        addElements(titles, "title");
        rootNew.addContent(titles);

        //publisher
        Element publisher = new Element("publisher").setAttribute("xml:lang", "de");
        publisher.addContent("Stadtarchiv Duderstadt");
        rootNew.addContent(publisher);

        //PublicationYear
        Element pubYear = new Element("publicationYear");
        pubYear.addContent(String.valueOf(Year.now().getValue()));
        rootNew.addContent(pubYear);

        //resourceType
        Element resourceType = new Element("resourceType").setAttribute("resourceTypeGeneral", "Text");
        resourceType.addContent("Archive text");
        rootNew.addContent(resourceType);
    }

    /**
     * TODO: Please document this method
     */
    private void makeControl(Element rootEAD) {

        Element cont = new Element("control");
        cont.addContent(new Element("recordid").setText("0"));
        Element eltDesc = (new Element("filedesc"));
        eltDesc.addContent(new Element("titlestmt").addContent(new Element("titleproper")));
        cont.addContent(eltDesc);
        Element eltMS = new Element("maintenancestatus").setAttribute("value", "new");
        cont.addContent(eltMS);
        cont.addContent(new Element("maintenanceagency").addContent(new Element("agencyname").addContent("Duderstadt")));
        Element eltMH = new Element("maintenancehistory");
        Element eltME = new Element("maintenanceevent");
        eltME.addContent(new Element("eventtype").setAttribute("value", "unknown"));
        eltME.addContent(new Element("eventdatetime"));
        eltME.addContent(new Element("agenttype").setAttribute("value", "unknown"));
        eltME.addContent(new Element("agent"));
        eltMH.addContent(eltME);
        cont.addContent(eltMH);

        rootEAD.addContent(cont);
    }

    /**
     * TODO: Please document this method
     */
    private Element makeArchiveDesc(Element rootEAD) {

        Element arch = new Element("archdesc");

        arch.setAttribute("level", "collection");
        //        arch.setAttribute("type", "findbuch");

        Element eltDid = new Element("did");
        Element eltRepo = new Element("repository");

        eltRepo.addContent(new Element("corpname").addContent(new Element("part").addContent("Stadtarchiv Duderstadt")));
        eltRepo.addContent(new Element("address").addContent(new Element("addressline").addContent("Christian-Blank-Straße 1, 37115 Duderstadt")));

        eltDid.addContent(eltRepo);
        arch.addContent(eltDid);

        rootEAD.addContent(arch);
        return arch;
    }

    /**
     * Given the root of an xml tree, get the basic DOI info.
     * 
     * @param physical
     * @return
     */
    public BasicDoi getBasicDoi(DocStruct physical) {

        BasicDoi doi = new BasicDoi();

        doi.TITLE = getValues("title", physical);
        doi.AUTHORS = getValues("author", physical);
        doi.PUBLISHER = getValues("publisher", physical);
        doi.PUBDATE = getValues("pubdate", physical);
        doi.INST = getValues("inst", physical);

        return doi;
    }

    /**
     * TODO: Please document this method
     */
    private List<String> getValues(String strDoiElt, DocStruct struct) {

        ArrayList<String> lstDefault = new ArrayList<String>();
        String strLocalElt = strDoiElt;

        Element eltMap = doiMappings.get(strDoiElt);
        if (eltMap != null) {

            //set up the default value:
            String strDefault = eltMap.getChildText("default");
            if (!strDefault.isEmpty()) {
                lstDefault.add(strDefault);
            }

            //try to find the local value:
            strLocalElt = eltMap.getChildText("localElt");
        }

        List<String> lstLocalValues = getValuesForField(struct, strLocalElt);

        if (!lstLocalValues.isEmpty()) {

            return lstLocalValues;
        }

        if (eltMap != null) {
            //could not find first choice? then try alternatives
            for (Element eltAlt : eltMap.getChildren("altLocalElt")) {

                lstLocalValues = getValuesForField(struct, eltAlt.getText());

                if (!lstLocalValues.isEmpty()) {

                    return lstLocalValues;
                }
            }
        }

        //otherwise just return default
        return lstDefault;
    }

    /**
     * TODO: Please document this method
     */
    private List<String> getValuesForField(DocStruct struct, String strLocalElt) {

        ArrayList<String> lstValues = new ArrayList<String>();

        for (Metadata mdata : struct.getAllMetadata()) {

            if (mdata.getType().getName().equalsIgnoreCase(strLocalElt)) {

                lstValues.add(mdata.getValue());
            }
        }

        return lstValues;
    }

}

// can this be removed?

//    /**
//     * Goes through the tree looking for the relevant values for various elements. The element "titles" is mandatory, all others are optional.
//     * 
//     * @param rootNode
//     * @return
//     */
//    private HashMap<String, Element> getElements(Element rootNode) {
//
//        HashMap<String, Element> dict = new HashMap<String, Element>();
//
//        //first a default title: (this will be overwritten, if a title can be found)
//        Element titles = new Element("titles");
//        Element title = new Element("title").addContent("Fragment");
//        titles.addContent(title);
//        dict.put("titles", titles);
//
//        List<Element> lstElts = rootNode.getChildren();
//        if (lstElts.isEmpty()) {
//            return dict;
//        }
//
//        for (Element node : lstElts) {
//
//            if (node.getChildren().isEmpty()) {
//                continue;
//            }
//
//            Element eltNew = new Element("c");
//            eltNew.setAttribute("id", getNextId(""));
//
//            dict.put("id", eltNew);
//            String strNode = node.getName();
//            String strLevel = "series";
//
//            String strTitle = "";
//            String strId = "";
//
//            Boolean boStop = false;
//            List<Element> lstDidChildren = new ArrayList<Element>();
//
//            switch (strNode) {
//                case "urkunden":
//                case "fragmente":
//                case "amtsbuecher":
//                case "akten":
//
//                    strTitle = strNode;
//                    strId = node.getChildText("zaehlung");
//                    break;
//
//                case "bestand":
//
//                    //bestand appears as a leaf of the node elt :/
//                    if (node.getChildText("bestand") == null) {
//                        break;
//                    }
//                    strTitle = node.getChildText("bestand");
//                    strId = "Verzeichnis " + node.getChildText("verzeichnis");
//                    strLevel = "collection";
//                    //                    lstDidChildren.addAll(getBestandInfo(node));
//                    break;
//
//                case "urkunde":
//
//                    strTitle = node.getChildText("id");
//                    strId = "bestellnr " + node.getChildText("bestellnr");
//                    strLevel = "recordgrp";
//                    if (node.getChild("id") != null) {
//                        eltNew.setAttribute("id", getNextId(node.getChildText("id")));
//                    }
//                    lstDidChildren.addAll(getUrkundeInfo(node));
//
//                    boStop = true;
//                    break;
//
//                case "fragment":
//
//                    strTitle = node.getChildText("id");
//                    strId = "bestellnr " + node.getChildText("bestellnr");
//                    strLevel = "recordgrp";
//                    if (node.getChild("id") != null) {
//                        eltNew.setAttribute("id", getNextId(node.getChildText("id")));
//                    }
//                    lstDidChildren.addAll(getFragmentInfo(node));
//                    boStop = true;
//                    break;
//
//                case "reihen":
//
//                    strTitle = "reihen";
//                    strId = node.getChildText("reihe");
//                    strLevel = "recordgrp";
//                    break;
//
//                case "amtsbuch":
//
//                    strTitle = node.getChildText("titel");
//                    strId = node.getChildText("id");
//                    strLevel = "recordgrp";
//                    if (node.getChild("id") != null) {
//                        eltNew.setAttribute("id", getNextId(node.getChildText("id")));
//                    }
//                    lstDidChildren.addAll(getAmtsbuchInfo(node));
//                    boStop = true;
//                    break;
//
//                case "systematik":
//
//                    strTitle = "systematik " + node.getChildText("systematik");
//                    strId = node.getChildText("systematik");
//                    strLevel = "subseries";
//                    break;
//
//                case "klassifikation":
//
//                    strTitle = "klassifikation " + node.getChildText("klassifikation");
//                    strId = node.getChildText("klass_nr");
//                    strLevel = "subseries";
//                    break;
//
//                case "akte":
//
//                    strTitle = node.getChildText("titel");
//                    strId = node.getChildText("id");
//                    strLevel = "recordgrp";
//                    if (node.getChild("id") != null) {
//                        eltNew.setAttribute("id", getNextId(node.getChildText("id")));
//                    }
//                    lstDidChildren.addAll(getAkteInfo(node));
//                    boStop = true;
//                    break;
//
//                case "folio":
//
//                    //                    //           addFolio(eltNew, node);
//                    //                    eltNew.setAttribute("id", node.getChildText("datei"));
//                    //                    strLevel = "otherlevel";
//
//                    break;
//
//                default:
//                    break;
//            }
//
//            //            eltNew.setAttribute("level", strLevel);
//
//            eltNew.setAttribute("otherlevel", strNode);
//
//            if (strTitle != "") {
//
//                Element eltDid = new Element("did");
//                eltDid.addContent(new Element("unitid").setText(strId));
//                eltDid.addContent(new Element("unittitle").setText(strTitle));
//
//                if (node.getChild("laufzeit1") != null) {
//                    eltDid.addContent(new Element("unitdate").setText(node.getChildText("laufzeit1")));
//                }
//                if (node.getChild("laufzeit2") != null) {
//                    eltDid.addContent(new Element("unitdate").setText(node.getChildText("laufzeit2")));
//                }
//
//                //add extra info:
//                eltDid.addContent(lstDidChildren);
//
//                eltNew.addContent(eltDid);
//
//                if (!boStop) {
//                    iCurrentLevel++;
//                    addChildNodes(eltNew, node);
//                    iCurrentLevel--;
//                }
//                //                rootNew.addContent(new Element(strNode).addContent(node.getText()));
//            }
//
//        }
//
//    }
//
//    private List<Element> getUrkundeInfo(Element node) {
//
//        List<Element> lstInfo = new ArrayList<Element>();
//
//        //druck
//        if (node.getChild("druck") != null) {
//            Element eltPhys = new Element("physdesc");
//            eltPhys.setText(node.getChildText("druck"));
//            eltPhys.setAttribute("localtype", "druck");
//
//            lstInfo.add(eltPhys);
//        }
//
//        //Elements for the Abstract:--------------
//        //regest
//        Element eltAbstr = new Element("abstract");
//        if (node.getChild("regest") != null) {
//            eltAbstr.setText(node.getChildText("regest"));
//            eltAbstr.setAttribute("localtype", "regest");
//        }
//
//        //Orte:
//        addOrtsreg(node, eltAbstr);
//
//        //Persons:
//        addPersreg(node, eltAbstr);
//
//        //Sachreg:
//        addSachreg(node, eltAbstr);
//
//        //------------------------------------------
//
//        //aus_ort
//        if (node.getChild("aus_ort") != null) {
//            Element eltAus = new Element("origination");
//            Element eltOrig = new Element("name");
//            Element eltPart = new Element("part");
//            eltPart.setText(node.getChildText("aus_ort"));
//            eltOrig.addContent(eltPart);
//            eltAus.addContent(eltOrig);
//            eltAus.setAttribute("localtype", "aus_ort");
//
//            lstInfo.add(eltAus);
//        }
//
//        //ausfertigung
//        if (node.getChild("ausfertigung") != null) {
//            Element eltAus = new Element("materialspec");
//            eltAus.setText(node.getChildText("ausfertigung"));
//            eltAus.setAttribute("localtype", "ausfertigung");
//
//            lstInfo.add(eltAus);
//        }
//
//        //stoff
//        if (node.getChild("stoff") != null) {
//            Element eltAus = new Element("physdesc");
//            eltAus.setText(node.getChildText("stoff"));
//            eltAus.setAttribute("localtype", "stoff");
//
//            lstInfo.add(eltAus);
//        }
//
//        //zusatz
//        if (node.getChild("zusatz") != null) {
//            Element eltAus = new Element("didnote");
//            eltAus.setText(node.getChildText("zusatz"));
//            eltAus.setAttribute("localtype", "zusatz");
//
//            lstInfo.add(eltAus);
//        }
//
//        lstInfo.add(eltAbstr);
//
//        return lstInfo;
//
//    }
//
//    private List<Element> getFragmentInfo(Element node) {
//
//        List<Element> lstInfo = new ArrayList<Element>();
//
//        //druck
//        if (node.getChild("druck") != null) {
//            Element eltPhys = new Element("physdesc");
//            eltPhys.setText(node.getChildText("druck"));
//            eltPhys.setAttribute("localtype", "druck");
//
//            lstInfo.add(eltPhys);
//        }
//
//        //herkunft
//        if (node.getChild("herkunft") != null) {
//            Element eltAus = new Element("origination");
//
//            Element eltOrig = new Element("name");
//            Element eltPart = new Element("part");
//            eltPart.setText(node.getChildText("herkunft"));
//            eltOrig.addContent(eltPart);
//            eltAus.addContent(eltOrig);
//            eltAus.setAttribute("localtype", "herkunft");
//
//            lstInfo.add(eltAus);
//        }
//        //quelle
//        if (node.getChild("quelle") != null) {
//            Element eltAus = new Element("origination");
//
//            Element eltOrig = new Element("name");
//            Element eltPart = new Element("part");
//            eltPart.setText(node.getChildText("quelle"));
//            eltOrig.addContent(eltPart);
//            eltAus.addContent(eltOrig);
//            eltAus.setAttribute("localtype", "quelle");
//
//            lstInfo.add(eltAus);
//        }
//
//        //ausfertigung
//        if (node.getChild("ausfertigung") != null) {
//            Element eltAus = new Element("materialspec");
//            eltAus.setText(node.getChildText("ausfertigung"));
//            eltAus.setAttribute("localtype", "ausfertigung");
//
//            lstInfo.add(eltAus);
//        }
//
//        //bemerkung
//        if (node.getChild("bemerkung") != null) {
//            Element eltAus = new Element("didnote");
//            eltAus.setText(node.getChildText("bemerkung"));
//            eltAus.setAttribute("localtype", "bemerkung");
//
//            lstInfo.add(eltAus);
//        }
//
//        return lstInfo;
//
//    }
//
//    private List<Element> getAmtsbuchInfo(Element node) {
//
//        List<Element> lstInfo = new ArrayList<Element>();
//
//        //form
//        if (node.getChild("form") != null) {
//            Element eltPhys = new Element("physdesc");
//            eltPhys.setText(node.getChildText("form"));
//            eltPhys.setAttribute("localtype", "form");
//
//            lstInfo.add(eltPhys);
//        }
//        //umfang
//        if (node.getChild("umfang") != null) {
//            Element eltAus = new Element("physdesc");
//            eltAus.setText(node.getChildText("umfang"));
//            eltAus.setAttribute("localtype", "umfang");
//
//            lstInfo.add(eltAus);
//        }
//        //format
//        if (node.getChild("format") != null) {
//            Element eltAus = new Element("physdesc");
//            eltAus.setText(node.getChildText("format"));
//            eltAus.setAttribute("localtype", "format");
//
//            lstInfo.add(eltAus);
//        }
//        //einband
//        if (node.getChild("einband") != null) {
//            Element eltAus = new Element("physdesc");
//            eltAus.setText(node.getChildText("einband"));
//            eltAus.setAttribute("localtype", "einband");
//
//            lstInfo.add(eltAus);
//        }
//
//        //bemerkung
//        if (node.getChild("bemerkung") != null) {
//            Element eltAus = new Element("didnote");
//            eltAus.setText(node.getChildText("bemerkung"));
//            eltAus.setAttribute("localtype", "bemerkung");
//
//            lstInfo.add(eltAus);
//        }
//
//        //zustand
//        if (node.getChild("zustand") != null) {
//            Element eltAus = new Element("materialspec");
//            eltAus.setText(node.getChildText("zustand"));
//            eltAus.setAttribute("localtype", "zustand");
//
//            lstInfo.add(eltAus);
//        }
//
//        //schreiber        
//        if (node.getChild("schreiber") != null) {
//            Element eltAbstr = new Element("abstract");
//            Element eltName = new Element("persname");
//            Element eltPart = new Element("part");
//            eltPart.setText(node.getChildText("schreiber"));
//            eltPart.setAttribute("localtype", "schreiber");
//
//            eltName.addContent(eltPart);
//            eltAbstr.addContent(eltName);
//            lstInfo.add(eltAbstr);
//        }
//
//        return lstInfo;
//
//    }
//
//    private List<Element> getAkteInfo(Element node) {
//
//        List<Element> lstInfo = new ArrayList<Element>();
//
//        //aktennr
//        if (node.getChild("aktennr") != null) {
//            Element eltPhys = new Element("materialspec");
//            eltPhys.setText(node.getChildText("aktennr"));
//            eltPhys.setAttribute("localtype", "aktennr");
//
//            lstInfo.add(eltPhys);
//        }
//
//        //druck
//        if (node.getChild("druck") != null) {
//            Element eltPhys = new Element("physdesc");
//            eltPhys.setText(node.getChildText("druck"));
//            eltPhys.setAttribute("localtype", "druck");
//
//            lstInfo.add(eltPhys);
//        }
//
//        //Elements for the Abstract:--------------
//        //enthaelt
//        Element eltAbstr = new Element("abstract");
//        if (node.getChild("enthaelt") != null) {
//            eltAbstr.setText(node.getChildText("enthaelt"));
//            eltAbstr.setAttribute("localtype", "enthaelt");
//        } else if (node.getChild("darin") != null) {
//            eltAbstr.setText(node.getChildText("darin"));
//            eltAbstr.setAttribute("localtype", "darin");
//        }
//
//        //Orte:
//        addOrtsreg(node, eltAbstr);
//
//        //Persons:
//        addPersreg(node, eltAbstr);
//
//        //Sachreg:
//        addSachreg(node, eltAbstr);
//
//        //vorsignatur
//        if (node.getChild("vorsignatur") != null) {
//            Element eltVorsig = node.getChild("vorsignatur");
//            if (eltVorsig.getChild("vs1") != null) {
//
//                Element eltVS1 = new Element("num");
//                eltVS1.addContent(eltVorsig.getChildText("vs1"));
//                eltVS1.setAttribute("localtype", "vs1");
//                eltAbstr.addContent(eltVS1);
//            }
//            if (eltVorsig.getChild("vs2") != null) {
//
//                Element eltVS2 = new Element("num");
//                eltVS2.addContent(eltVorsig.getChildText("vs2"));
//                eltVS2.setAttribute("localtype", "vs1");
//                eltAbstr.addContent(eltVS2);
//            }
//        }
//
//        //------------------------------------------
//
//        //anmerkung
//        if (node.getChild("anmerkung") != null) {
//            Element eltAus = new Element("didnote");
//            eltAus.setText(node.getChildText("anmerkung"));
//            eltAus.setAttribute("localtype", "anmerkung");
//
//            lstInfo.add(eltAus);
//        }
//
//        lstInfo.add(eltAbstr);
//
//        return lstInfo;
//
//    }
//
//    private void addOrtsreg(Element node, Element eltAbstract) {
//
//        if (node.getChildren("ortsreg") == null || node.getChildren("ortsreg").isEmpty()) {
//            return;
//        }
//
//        //otherwise:
//        for (Element eltOrt : node.getChildren("ortsreg")) {
//
//            Element eltOrts = new Element("geogname");
//            Element eltPart = new Element("part").addContent(eltOrt.getChildText("orte"));
//            eltOrts.addContent(eltPart);
//            eltAbstract.addContent(eltOrts);
//        }
//    }
//
//    private void addPersreg(Element node, Element eltAbstract) {
//
//        if (node.getChildren("persreg") == null || node.getChildren("persreg").isEmpty()) {
//            return;
//        }
//
//        //otherwise:
//        for (Element eltChild : node.getChildren("persreg")) {
//
//            Element eltPers = new Element("persname");
//
//            if (eltChild.getChild("name") != null) {
//
//                eltPers.addContent(new Element("part").addContent(eltChild.getChildText("name")).setAttribute("localtype", "name"));
//            }
//            if (eltChild.getChild("vorname") != null) {
//
//                eltPers.addContent(new Element("part").addContent(eltChild.getChildText("vorname")).setAttribute("localtype", "vorname"));
//            }
//            if (eltChild.getChild("namenszusatz") != null) {
//
//                eltPers.addContent(new Element("part").addContent(eltChild.getChildText("namenszusatz")).setAttribute("localtype", "namenszusatz"));
//            }
//            if (eltChild.getChild("wohnort") != null) {
//
//                eltPers.addContent(new Element("part").addContent(eltChild.getChildText("wohnort")).setAttribute("localtype", "wohnort"));
//            }
//            if (eltChild.getChild("amt") != null) {
//
//                eltPers.addContent(new Element("part").addContent(eltChild.getChildText("amt")).setAttribute("localtype", "amt"));
//            }
//            if (eltChild.getChild("aliasname") != null) {
//
//                eltPers.addContent(new Element("part").addContent(eltChild.getChildText("aliasname")).setAttribute("localtype", "aliasname"));
//            }
//            if (eltChild.getChild("beruf") != null) {
//
//                eltPers.addContent(new Element("part").addContent(eltChild.getChildText("beruf")).setAttribute("localtype", "beruf"));
//            }
//            if (eltChild.getChild("bemerkung") != null) {
//
//                eltPers.addContent(new Element("part").addContent(eltChild.getChildText("bemerkung")).setAttribute("localtype", "bemerkung"));
//            }
//
//            eltAbstract.addContent(eltPers);
//        }
//    }
//
//    private void addSachreg(Element node, Element eltAbstract) {
//
//        if (node.getChildren("sachreg") == null || node.getChildren("sachreg").isEmpty()) {
//            return;
//        }
//
//        //otherwise:
//        for (Element eltReg : node.getChildren("sachreg")) {
//
//            for (Element eltSach : eltReg.getChildren("sachbegriffe")) {
//                Element eltOrts = new Element("subject");
//                Element eltPart = new Element("part").addContent(eltSach.getText());
//                eltOrts.addContent(eltPart);
//                eltAbstract.addContent(eltOrts);
//            }
//        }
//    }

//    private void addFolio(Element eltNew, Element nodeFolio) {
//
//        Element eltFile = new Element("c");
//        eltFile.setAttribute("level", "file");
//        eltFile.setAttribute("id", getNextId());
//
//        for (Element eltChild : nodeFolio.getChildren()) {
//
//            if (eltChild.getChildren().isEmpty()) {
//                continue;
//            }
//
//            Element eltDid = new Element("did");
//            eltDid.addContent(new Element("unittitle").setText(getFolioText(eltChild)));
//            eltFile.addContent(eltDid);
//        }
//
//        eltNew.addContent(eltFile);
//
//    }
//
//    private String getFolioText(Element nodeFolioChild) {
//
//        if (nodeFolioChild.getName().equals("recht")) {
//            return "recht " + nodeFolioChild.getChildText("nummer");
//        }
//
//        if (nodeFolioChild.getName().equals("eintrag")) {
//            return "eintrag " + nodeFolioChild.getChildText("nummer");
//        }
//
//        if (nodeFolioChild.getName().equals("rubrum")) {
//            return "rubrum " + nodeFolioChild.getChildText("rubrum");
//        }
//
//        if (nodeFolioChild.getChild("text") != null) {
//            return nodeFolioChild.getChild("text").getText();
//        }
//
//        return nodeFolioChild.getName();
//    }
//}
