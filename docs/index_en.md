---
title: Automatic Handle Assignment
identifier: intranda_step_handle_mets
description: Step plugin for the automatic creation of Handle IDs within METS files
published: true
---

## Introduction
The plugin generates a Handle on the Handle server of the GWDG for all logical and physical elements of a METS file. These Handles are then stored in the respective element itself as metadata `_urn`.

If automatic DOI assignment is installed, a new DOI is generated and stored for each top-level logical element.

## Installation
To use the plugin, the following files must be installed:

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

Once the plugin is installed and configured, it can be used within a Goobi workflow step. To do this, add the plugin `plugin_intranda_step_handle_mets` within the desired task. Additionally, ensure that the Metadata and Automatic Task checkboxes are selected.

To utilize automatic DOI assignment, an additional file must be installed at the following path, readable by the Tomcat user:

```bash
/opt/digiverso/goobi/config/
```

This file is used to configure the plugin and is located in the `mappings` folder.

## Overview and Operation
The plugin operates within a correctly configured workflow as follows:

- When the plugin is invoked within the workflow, it opens the METS file.
- A Handle is generated for each logical and physical element of the METS file (in the form `/goobi-Institution-objectId`, where `objectId` is the object identifier, possibly supplemented with `-1`, `-2`, etc., if the Handle already exists).
- The generated Handle is then written into the respective structural element as metadata of type `_urn`.

When creating Handles for the top-level logical structural element of a METS file, additional metadata is stored alongside the generated Handle ID and its associated URL. An example of this information is as follows:

```
Handle Values for: 21.T119876543/goobi-go-1296243265-17
Index    Type   Timestamp                Data
1        URL    2020-04-21 12:02:30Z    https://viewer.goobi.io/idresolver?handle=
2        TITLE  2020-04-21 12:02:30Z    [Stammbuch Daniel Schelling]
3        AUTHORS 2020-04-21 12:02:30Z  Daniel Schelling
4        PUBLISHER 2020-04-21 12:02:30Z Stadtarchiv Duderstadt
5        PUBDATE 2020-04-21 12:02:30Z  1617
6        INST   2020-04-21 12:02:30Z    MPG
100      HS_ADMIN 2020-04-21 12:02:30Z handle=21.T119876543/USER1234528; index=300; [create hdl,delete hdl,read val,modify val,del val,add val,modify admin,del admin,add admin,list]
```

These details are used in the case of additional DOI registration to create a DOI with the same ID, for example `21.T119876543/goobi-go-1296243265-17`.


## Configuration

### Main Configuration of the Plugin
The plugin configuration is done in the file `plugin_intranda_step_handle_mets.xml` as shown below:

```xml
<config_plugin>
	<config>
		<!-- which projects to use for (can be more than one, otherwise use *) -->
		<project>*</project>
		<step>*</step>

		<PEMFile>/opt/digiverso/goobi/config/certificates/21.T119876543_USER1234528-priv.pem</PEMFile>
		<UserHandle>21.T119876543/USER1234528</UserHandle> 
		<HandleBase>21.T119876543</HandleBase> 
		<URLPrefix>https://viewer.goobi.io/idresolver?handle=</URLPrefix> 

		<HandleIdPrefix>goobi</HandleIdPrefix>
		<HandleInstitutionAbbr>go</HandleInstitutionAbbr>
		<ErrorMessage>Handle Authorization file could not be found.</ErrorMessage>

		<!-- configuration elements for DOIs -->
		<MakeDOI>true</MakeDOI>
		<DOIMappingFile>/opt/digiverso/goobi/config/doi_mapping.xml</DOIMappingFile>
		<DOIInstitutionAbbr>GOO</DOIInstitutionAbbr>

	</config>
	
	<config>
		<!-- which projects to use for (can be more than one, otherwise use *) -->
		<project>My special project</project>
		<project>Archive_Project</project>
		<step>CreateHandle</step>
		<step>intranda_step_handle_mets</step>

		<PEMFile>/opt/digiverso/goobi/config/certificates/21.T119876543_USER1234528-priv.pem</PEMFile>
		<UserHandle>21.T119876543/USER1234528</UserHandle> 
		<HandleBase>21.T119876543</HandleBase> 
		<URLPrefix>https://viewer.goobi.io/idresolver?handle=</URLPrefix> 

		<HandleIdPrefix>goobi</HandleIdPrefix>
		<HandleInstitutionAbbr>go</HandleInstitutionAbbr>
		<ErrorMessage>Handle Authorization file could not be found.</ErrorMessage>

		<!-- configuration elements for DOIs -->
		<MakeDOI>true</MakeDOI>
		<DOIMappingFile>/opt/digiverso/goobi/config/doi_mapping.xml</DOIMappingFile>
		<DOIInstitutionAbbr>GOO</DOIInstitutionAbbr>

	</config>
</config_plugin>
```

{{CONFIG_DESCRIPTION_PROJECT_STEP}}

Parameter              | Explanation
-----------------------|------------------------------------
`PEMFile`              | Path to the Private Key .PEM file provided by GWDG.
`HandleInstitutionAbbr`| Abbreviation for the institution.
`HandleIdPrefix`       | Prefix for the Handles (e.g., for the application or project).
`HandleBase`           | Identifier for the institution.
`UserHandle`           | Identifier for the user of the Handle registration.
`URLPrefix`            | URL where the documents can be found with their Handle ID after publication.

For DOI assignment, the file `plugin_intranda_step_handle_mets.xml` must include the following additional configurations:

```xml
<config_plugin>
	<config>

	...

		<MakeDOI>true</MakeDOI>
		<DOIMappingFile>"path/to/DOI-Mapping.xml/file"</DOIMappingFile>

	</config>
</config_plugin>
```

The `DOIMappingFile` parameter defines the path to the `plugin_intranda_step_handle_mets.xml` file.

### Mapping File
In the `DOI-Mapping.xml` file, each `<map>` entry describes a mapping between a Dublin Core element and one or more metadata fields from the METS file. The file is structured as follows:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Mapping>

	<map>
		<doiElt>title</doiElt>
		<localElt>TitleDocMain</localElt>
		<altLocalElt>titel</altLocalElt>
		<default>Fragment</default>
	</map>

	<map>
		<doiElt>author</doiElt>
		<localElt>Author</localElt>
		<default>MPG</default>
	</map>

	<map>
		<doiElt>publisher</doiElt>
		<localElt>Publisher</localElt>
		<altLocalElt>quelle</altLocalElt>
		<default>MPG</default>
	</map>

	<map>
		<doiElt>pubdate</doiElt>
		<localElt>PublicationYear</localElt>
		<altLocalElt>PublicationYearSort</altLocalElt>
		<altLocalElt>Sortlaufzeit0</altLocalElt>
		<default>unknown</default>
	</map>

	<map>
		<doiElt>inst</doiElt>
		<default>MPG</default>
	</map>

</Mapping>
```

Parameter              | Explanation
-----------------------|------------------------------------
`<doiElt>`             | Dublin Core element for which this mapping is defined.
`<localElt>`           | Name of the metadata in the METS file whose value should be used for `<doiElt>`.
`<altLocalElt>`        | Alternative names for the metadata, searched if no entry is found with `<localElt>`.
`<default>`            | Specifies the value to be used if neither `<localElt>` nor `<altLocalElt>` provide suitable entries.
`<title>`, `<author>`, `<publisher>`, `<pubdate>`, `<inst>` | These are currently the only five required and at the same time maximum permitted fields for metadata used for registration.
