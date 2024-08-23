---
title: Automatic Handle registration
identifier: intranda_step_handle_mets
description: Step Plugin for the automatic registration for Handle IDs in METS files
published: false
---

## Introduction 
The plugin generates a Handle on the Handle server for all logical and physical elements of a METS file. This Handle is then stored within the element itself as metadata under `_urn`.

If automatic DOI assignment is installed, a new DOI will be generated and stored for each top-level logical element.

## Installation
To use the plugin, the following files need to be installed:

```bash
goobi-plugin-step-handle-mets.jar
plugin_intranda_step_handle_mets.xml
```

The file `goobi-plugin-step-handle-mets.jar` contains the program logic and must be installed in the following directory, readable by the Tomcat user:

```bash
/opt/digiverso/goobi/plugins/step/
```

The file `plugin_intranda_step_handle_mets.xml` must also be readable by the Tomcat user and installed in the following directory:

```bash
/opt/digiverso/goobi/config/
```

Once the plugin is installed and configured, it can be used within a Goobi workflow step.

To do this, the `plugin plugin_intranda_step_handle_mets` must be specified within the desired task. Additionally, the checkboxes for Metadata and Automatic task must be set.

To use the automatic DOI assignment, an additional file must be installed in the following path, so that it is readable by the Tomcat user:

```bash
/opt/digiverso/goobi/config/
```

This file serves to configure the plugin and is located in the "mappings" folder:

{{CONFIG_DESCRIPTION_PROJECT_STEP}}

## Overview and functionality
The operation of the plugin within the correctly configured workflow is as follows:

* When the plugin is called within the workflow, it opens the METS file.
* For each logical and physical element of the METS file, a Handle is generated (in the form /goobi-CustomerAbbreviation-objectId, where the objectId is that of the element, with the suffix `-1`, `-2`, etc., if the Handle already exists).
* This Handle is then written into the respective element, under the metadata type "_urn".


## Configuration
The plugin is configured in the file `plugin_intranda_step_ZZZ.xml` as shown here:

{{CONFIG_CONTENT}}

{{CONFIG_DESCRIPTION_PROJECT_STEP}}

Parameter               | Explanation
------------------------|------------------------------------
`PEMFile`          | Path to the private key .PEM file. Provided by GWDG, a copy is located in this repository in the `resources` folder.|
`HandleInstitutionAbbr`   | Abbreviation under which the Handles should be stored. |
`HandleIdPrefix`          | Prefix under which the Handles should be stored.
`HandleBase`              | Stores details of the Institution Handle Prefix.
`UserHandle`              | Stores details of the Institution Handle Prefix.
`URLPrefix`               | URL where the documents with their Handle-ID can be found.sind. |

The file `plugin_intranda_step_handle_mets.xml` must include the following additional fields for DOI assignment:

```xml
<config_plugin>
	<config>

    ...
    
    <MakeDOI>true</MakeDOI>
	<DOIMappingFile>"path/to/DOI-Mapping.xml/file"</DOIMappingFile>
	
	</config>
</config_plugin>
```

## Mapping File
In the DOI-Mapping.xml file, each <map> entry describes a mapping between a Dublin Core element and one or more metadata fields from the METS file. The mappings are set up as follows:

<map>
  <doiElt>pubdate</doiElt>
  <localElt>PublicationYear</localElt>
  <altLocalElt>PublicationYearSort</altLocalElt>
  <altLocalElt>laufzeit0</altLocalElt>
  <default>unknown</default>
</map>

| Parameter               | Description                                                                                                      |
|-------------------------|------------------------------------------------------------------------------------------------------------------|
| `<doiElt>`              | The Dublin Core element for which this mapping is defined.                                                        |
| `<localElt>`            | The name of the metadata in the METS file whose value should be used for the `<doiElt>`.                          |
| `<altLocalElt>`         | Alternative names for the metadata, which will be searched if no entry with the name `<localElt>` is found.       |
| `<default>`             | Specifies the value to be used if neither `<localElt>` nor `<altLocalElt>` provide suitable entries.              |
| `<title>`, `<author>`, `<publisher>`, `<pubdate>`, `<inst>` | These are the five mandatory and maximum allowed fields.                                                   |
