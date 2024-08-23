---
title: Automatische Handlevergabe
identifier: intranda_step_handle_mets
description: Step Plugin für die automatische Erstellung von Handle-IDs innerhalb von METS-Dateien
published: true
---

## Einführung
Das Plugin erzeugt auf dem Handle-Server einen Handle für alle logischen und physischen Elemente einer METS-Datei. Dieser Handle wird dann in dem jeweiligen Element selbst als Metadatum `_urn` gespeichert.

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

Nachdem das Plugin installiert und konfiguriert wurde, kann es innerhalb eines Arbeitsschrittes von Goobi genutzt werden.

Dazu muss innerhalb der gewünschten Aufgabe das Plugin `plugin_intranda_step_handle_mets` eingetragen werden. Des Weiteren müssen die Checkboxen Metadaten und Automatische Aufgabe gesetzt sein.

Um die automatische DOI-Vergabe zu nutzen, muss eine zusätzliche Datei auf dem folgenden Pfad installiert werden, sodass sie für den Tomcat-Nutzer lesbar ist:

```bash
/opt/digiverso/goobi/config/
```

Die Datei dient zur Konfiguration des Plugins befindet sich im Ordner "mappings":

{{CONFIG_DESCRIPTION_PROJECT_STEP}}


## Überblick und Funktionsweise
Die Arbeitsweise des Plugins innerhalb des korrekt konfigurierten Workflows sieht folgendermaßen aus:

Wenn das Plugin innerhalb des Workflows aufgerufen wurde, öffnet es die METS-Datei.
Für jedes logische und physische Element der METS-Datei wird ein Handle erzeugt (in der Form `/goobi-KundenKürzel-objektId`, wobei die objektId die des Elements ist, mit dem Suffix `-1`, `-2` etc., falls der Handle schon existiert).
Dieser Handle wird dann in das jeweilige Element geschrieben, unter der Metadatum vom Typ `_urn`.

## Konfiguration
Die Konfiguration des Plugins erfolgt in der Datei `plugin_intranda_step_handle_mets.xml` wie hier aufgezeigt:

{{CONFIG_CONTENT}}

{{CONFIG_DESCRIPTION_PROJECT_STEP}}
Parameter               | Erläuterung
------------------------|------------------------------------
`PEMFile`          | Pfad zur Private Key .PEM-Datei. Wird vom GWDG bereitgestellt, eine Kopie liegt in dieser Repro im Ordner `resources`. |
`HandleInstitutionAbbr`   | Abkürzung, mit der die Handles gespeichert werden sollen. |
`HandleIdPrefix`          | Präfix, mit dem die Handles gespeichert werden sollen.
`HandleBase`              | Speichert Details des Institution Handle Prefix.
`UserHandle`              | Speichert Details des Institution Handle Prefix.
`URLPrefix`               | URL, unter der die Dokumente mit ihrer Handle-ID zu finden sind.

Die Datei ``plugin_intranda_step_handle_mets.xml`` muss für die DOI-Vergabe folgende zusätzliche Felder beinhalten:

```xml
<config_plugin>
	<config>

    ...
    
    <MakeDOI>true</MakeDOI>
	<DOIMappingFile>"path/to/DOI-Mapping.xml/file"</DOIMappingFile>
	
	</config>
</config_plugin>
```

`DOPMAPPINGFILE` ist in dabei der Pfad zu der `plugin_intranda_step_handle_mets.xml`.

## Mapping Datei
In der `DOI-Mapping.xml-Datei` beschreibt jeder `<map>`-Eintrag eine Zuordnung (Mapping) zwischen einem Dublin-Core-Element und einem oder mehreren Metadatenfeldern aus der METS-Datei. Die Mappings sind beispielsweise folgendermaßen aufgebaut:

```xml
	<map>
		<doiElt>pubdate</doiElt>
		<localElt>PublicationYear</localElt>
		<altLocalElt>PublicationYearSort</altLocalElt>
		<altLocalElt>laufzeit0</altLocalElt>
		<default>unknown</default>
	</map>
```

Parameter               | Erläuterung
------------------------|------------------------------------
`<doiElt>`              | Ist das Dublin Core Element, für das dieser Mapping definiert ist
`<localElt>`            | Ist der Name des Metadatums in der METS-Datei, dessen Wert für das `<doiElt>` verwendet werden soll
`<altLocalElt>`         | Sind alternative Namen für das Metadatum, die durchsucht werden, falls kein Eintrag mit dem Namen `<localElt>` gefunden wird
`<default>`             | Gibt den Wert an, der verwendet wird, falls weder `<localElt>` noch `<altLocalElt>` passende Einträge liefern
`<title>`, `<author>`, `<publisher>`, `<pubdate>`, `<inst>` | Sind die fünf Felder notwendigen und maximal erlaubten Felder |