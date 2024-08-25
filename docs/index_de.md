---
title: Automatische Handlevergabe
identifier: intranda_step_handle_mets
description: Step Plugin für die automatische Erstellung von Handle-IDs innerhalb von METS-Dateien
published: true
---

## Einführung
Das Plugin erzeugt auf dem Handle-Server der GWDG einen Handle für alle logischen und physischen Elemente einer METS-Datei. Diese Handles werden dann in dem jeweiligen Element selbst als Metadatum `_urn` gespeichert.

Ist die automatische DOI-Vergabe installiert, so wird für jedes top-level logische Element ein neuer DOI erzeugt und gespeichert.


## Installation
Um das Plugin nutzen zu können, müssen folgende Dateien installiert werden:

```bash
goobi-plugin-step-handle-mets.jar
plugin_intranda_step_handle_mets.xml
```

Die Datei `goobi-plugin-step-handle-mets.jar` enthält die Programmlogik und muss für den Tomcat-Nutzer lesbar in folgendes Verzeichnis installiert werden:

```bash
/opt/digiverso/goobi/plugins/step/
```

Die Datei `plugin_intranda_step_handle_mets.xml` muss ebenfalls für den Tomcat-Nutzer lesbar sein und in folgendes Verzeichnis installiert werden:

```bash
/opt/digiverso/goobi/config/
```

Nachdem das Plugin installiert und konfiguriert wurde, kann es innerhalb eines Arbeitsschrittes von Goobi genutzt werden. Dazu muss innerhalb der gewünschten Aufgabe das Plugin `plugin_intranda_step_handle_mets` eingetragen werden. Des Weiteren müssen die Checkboxen Metadaten und Automatische Aufgabe gesetzt sein.

Um die automatische DOI-Vergabe zu nutzen, muss eine zusätzliche Datei auf dem folgenden Pfad installiert werden, sodass sie für den Tomcat-Nutzer lesbar ist:

```bash
/opt/digiverso/goobi/config/
```

Diese Datei dient zur Konfiguration des Plugins und befindet sich im Ordner `mappings`.


## Überblick und Funktionsweise
Die Arbeitsweise des Plugins innerhalb des korrekt konfigurierten Workflows sieht folgendermaßen aus:

- Wenn das Plugin innerhalb des Workflows aufgerufen wurde, öffnet es die METS-Datei.
- Für jedes logische und physische Element der METS-Datei wird ein Handle erzeugt (in der Form `/goobi-Institution-objektId`, wobei die `objektId` diejenige der Objekt-Identifier, der ggf. mit dem Suffix `-1`, `-2` etc. ergänzt wird, falls der Handle bereits existieren sollte).
- Der generierte Handle wird dann in das jeweilige Strukturelement als der Metadatum vom Typ `_urn` geschrieben.

Beim Erzeugen der Handles für das oberste logische Strukturelement einer METS-Dateis, werden zusätzlich zu der generierten Handle-ID und der zugehörigen URL weitere Metadaten innerhalb des generierten Handles gespeichert. Das sieht dann beispielhaft wie folgt aus:		

```
Handle Values for: 21.T119876543/goobi-go-1296243265-17		
Index	Type	Timestamp	Data		
1	URL	2020-04-21 12:02:30Z 	https://viewer.goobi.io/idresolver?handle=		
2	TITLE	2020-04-21 12:02:30Z 	[Stammbuch Daniel Schelling]		
3	AUTHORS	2020-04-21 12:02:30Z 	Daniel Schelling		
4	PUBLISHER	2020-04-21 12:02:30Z 	Stadtarchiv Duderstadt		
5	PUBDATE	2020-04-21 12:02:30Z 	1617		
6	INST	2020-04-21 12:02:30Z 	MPG		
100	HS_ADMIN	2020-04-21 12:02:30Z 	handle=21.T119876543/USER1234528; index=300; [create hdl,delete hdl,read val,modify val,del val,add val,modify admin,del admin,add admin,list]		
```		

Diese Informationen werden im Falle der zusätzlichen DOI-Registriereung genutzt, um die DOI mit der gleichen ID zu erzeugen - in diesem Fall also `21.T119876543/goobi-go-1296243265-17`. 


## Konfiguration

### Hauptkonfiguration des Plugins
Die Konfiguration des Plugins erfolgt in der Datei `plugin_intranda_step_handle_mets.xml` wie hier aufgezeigt:

```xml
<config_plugin>
	<config>
		<!-- which projects to use for (can be more then one, otherwise use *) -->
		<project>*</project>
		<step>*</step>

		<PEMFile>/opt/digiverso/goobi/config/zertifikate/21.T119876543_USER1234528-priv.pem</PEMFile>
		<UserHandle>21.T119876543/USER1234528</UserHandle> 
		<HandleBase>21.T119876543</HandleBase> 
		<URLPrefix>https://viewer.goobi.io/idresolver?handle=</URLPrefix> 

		<HandleIdPrefix>goobi</HandleIdPrefix>
		<HandleInstitutionAbbr>go</HandleInstitutionAbbr>
		<ErrorMessage>Handle Authorization file could not be found.</ErrorMessage>

		<!-- config elts for DOIs -->
		<MakeDOI>true</MakeDOI>
		<DOIMappingFile>/opt/digiverso/goobi/config/doi_mapping.xml</DOIMappingFile>
		<DOIInstitutionAbbr>GOO</DOIInstitutionAbbr>

	</config>
	
	<config>
		<!-- which projects to use for (can be more then one, otherwise use *) -->
		<project>My special project</project>
		<project>Archive_Project</project>
		<step>CreateHandle</step>
		<step>intranda_step_handle_mets</step>

		<PEMFile>/opt/digiverso/goobi/config/zertifikate/21.T119876543_USER1234528-priv.pem</PEMFile>
		<UserHandle>21.T119876543/USER1234528</UserHandle> 
		<HandleBase>21.T119876543</HandleBase> 
		<URLPrefix>https://viewer.goobi.io/idresolver?handle=</URLPrefix> 

		<HandleIdPrefix>goobi</HandleIdPrefix>
		<HandleInstitutionAbbr>go</HandleInstitutionAbbr>
		<ErrorMessage>Handle Authorization file could not be found.</ErrorMessage>

		<!-- config elts for DOIs -->
		<MakeDOI>true</MakeDOI>
		<DOIMappingFile>/opt/digiverso/goobi/config/doi_mapping.xml</DOIMappingFile>
		<DOIInstitutionAbbr>GOO</DOIInstitutionAbbr>

	</config>
</config_plugin>
```

{{CONFIG_DESCRIPTION_PROJECT_STEP}}

Parameter		       | Erläuterung
------------------------|------------------------------------
`PEMFile`		       | Pfad zur Private Key .PEM-Datei. Wird von der GWDG bereitgestellt.
`HandleInstitutionAbbr` | Kürzel für die Institution
`HandleIdPrefix`		| Präfix für die Handles (z.B. für die Applikation oder das Projekt)
`HandleBase`		    | Identifier für die Institution 
`UserHandle`		    | Identifier für den Nutzer der Handle-Registrierung
`URLPrefix`		     | URL, unter der die Dokumente mit ihrer Handle-ID zu finden sein werden nach Veröffentlichung.

Die Datei `plugin_intranda_step_handle_mets.xml` muss für die DOI-Vergabe folgende zusätzlichen Konfigurationen beinhalten:

```xml
<config_plugin>
	<config>

	...

		<MakeDOI>true</MakeDOI>
		<DOIMappingFile>"path/to/DOI-Mapping.xml/file"</DOIMappingFile>

	</config>
</config_plugin>
```

Der Parameter `DOIMappingFile` definiert dabei den Pfad zu der Datei `plugin_intranda_step_handle_mets.xml`.


### Mapping-Datei
In der `DOI-Mapping.xml`-Datei beschreibt jeder `<map>`-Eintrag eine Zuordnung (Mapping) zwischen einem Dublin-Core-Element und einem oder mehreren Metadatenfeldern aus der METS-Datei. Die Datei ist folgendermaßen aufgebaut:

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

Parameter		       | Erläuterung
------------------------|------------------------------------
`<doiElt>`		      | Ist das Dublin Core Element, für das dieser Mapping definiert ist
`<localElt>`		    | Ist der Name des Metadatums in der METS-Datei, dessen Wert für das `<doiElt>` verwendet werden soll
`<altLocalElt>`		 | Sind alternative Namen für das Metadatum, die durchsucht werden, falls kein Eintrag mit dem Namen `<localElt>` gefunden wird
`<default>`		     | Gibt den Wert an, der verwendet wird, falls weder `<localElt>` noch `<altLocalElt>` passende Einträge liefern.
`<title>`, `<author>`, `<publisher>`, `<pubdate>`, `<inst>` | Hierbei handelt es sich um die derzeit einzigen fünf notwendigen und zugleich maximal erlaubten Felder für Metadaten, die zur Registrierung verwendet werden.