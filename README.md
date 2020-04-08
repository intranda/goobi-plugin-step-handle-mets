# Automatic Handles

Documentation on the automatic Handle request plugin

## Installation und Konfiguration

Das Plugin besteht aus zwei Dateien:

```bash
goobi-plugin-step-handle-mets.jar
plugin_intranda_step_handle_mets.xml
```

Die Datei ```goobi-plugin-step-handle-mets.jar``` enthält die Programmlogik und muss für den tomcat-Nutzer lesbar in folgendes Verzeichnis installiert werden:

```bash
/opt/digiverso/goobi/plugins/step/
```

Die Datei ```plugin_intranda_step_handle_mets.xml``` muss ebenfalls für den tomcat-Nutzer lesbar sein und in folgendes Verzeichnis installiert werden:

```bash
/opt/digiverso/goobi/config/
```

Die Datei dient zur Konfiguration des Plugins und muss wie folgt aufgebaut sein:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config_plugin>
	<config>
		<StepName>Handle METS</StepName>
		<PEMFile>/opt/digiverso/goobi/config/zertifikate/21.T11998_USER28-priv.pem</PEMFile>
		<HandleInstitutionAbbr>go</HandleInstitutionAbbr>
		<ErrorMessage>Handle Authorization file could not be found.</ErrorMessage>
	</config>
</config_plugin>
```

Im Element ```PEMFile``` 
wird der Pfad zur Private Key .PEM-Datei hinterlegt. Dieser wird vom GWDG bereitgestellt"r


Das Element ```HandleInstitutionAbbr``` 
spezifiziert der Abkürzung, mit dem die Handles gespeichert werden sollen. Handles werden im Form 

`/goobi-KundenKurzel-objektId`

gespeichert.


## Einstellungen in Goobi

Nachdem das Plugin installiert und konfiguriert wurde, kann es innerhalb eines Arbeitsschrittes von Goobi genutzt werden.

Dazu muss innerhalb der gewünschten Aufgabe das Plugin ```plugin_intranda_step_handle_mets``` eingetragen werden. Des Weiteren muss die Checkbox Automatische Aufgabe gesetzt sein.

## Arbeitsweise

Die Arbeitsweise des Plugins innerhalb des korrekt konfigurierten Workflows sieht folgendermaßen aus:

* Wenn das Plugin innerhalb des Workflows aufgerufen wurde, öffnet es die METS-Datei. 
* Für jede logische und physische Element des METS-Dateis, wird ein Handle erzeugt (vom Form `/goobi-KundenKurzel-objektId`, wo der 
objektId die des Elements ist, mit Suffix `-1`, `-2`, etc. sollte der Handle schon existieren. 
* Dieser Handle wird dann in der jeweilige Element geschrieben, unter der Metadata vom Type `"_urn"`. 

