# Handles - Automatischer DOI-Vergabe

Erweiterung zur Documentation zur Plugin zum automatische Handlevergabe.

## Beschreibung

Das Plugin erzeugt beim Handle-Server ein Handle für jeder logische und physische Element eines METS-Dateis. 
Ist die automatische DOI-Vergabe installiert, so wird für jeder top-level logisches Element ein neuer DOI erzeugt und gespeichert.


## Installation und Konfiguration

Das Plugin wird installiert wie in der Installationsbeschreibung "README.md" in dieser repro beschrieben.

Ein zusätzliches Datei ``DOI-Mapping.xml`` mussTitleDocMain für den tomcat-Nutzer lesbar sein in ein Verzeichnis installiert werden, beispielsweise neben der Config Datei:

```bash
/opt/digiverso/goobi/config/
```

Die Datei dient zur Konfiguration des Plugins und muss wie folgt aufgebaut sein:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Mapping>

	<!-- Mandatory fields: -->

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

Eine Kopie liegt in dieser Repro, im Ordner "mappings".

Die Datei ``plugin_intranda_step_handle_mets.xml``  muss für die DOI-Vergabe folgende zusätzliche Felder beinhalten:

```xml
<config_plugin>
	<config>

    ...
    
    <MakeDOI>true</MakeDOI>
	<DOIMappingFile>"path/to/DOI-Mapping.xml/file"</DOIMappingFile>
	
	</config>
</config_plugin>
```

wobie der ``DOIMappingFile`` der Pfad zur oben beschriebene Datei beinhaltet.


## Der Mapping-Datei

Der Mapping-Datei ``DOI-Mapping.xml`` soll am besten für die zu erwartenden Metadaten angepasst bzw. erweitert werden. Jeder ``<map>`` beschreibet eine Abbildung zwischen ein Dublin Core Element, der für die DOI-Vergabe notwendig ist, und ein oder mehrere Metadaten. Die Mappings sind wie folgt aufgebaut:

```xml
	<map>
		<doiElt>pubdate</doiElt>
		<localElt>PublicationYear</localElt>
		<altLocalElt>PublicationYearSort</altLocalElt>
		<altLocalElt>laufzeit0</altLocalElt>
		<default>unknown</default>
	</map>
```

Der ``<doiElt>`` ist der Dublin Core Element, für dem dieser Mapping definiert ist.

``<localElt>`` Ist der Name der Metadatum in unser METS Datei, dessen Wert für die ``<doiElt>`` genommen werden soll. In diesem Beispiel ist der ``<doiElt>`` "pubdate", dh. Datum der Veröffentlichung, und wir erwarten in unsere METS Datei ein Metadatum mit Name "PublicationYear" in dem wir dieser Datum finden. 

Sollte es kein Metadatum mit dieser Name geben, dann wird durch die Liste alternative Namen gesucht, die als ``<altLocalElt>`` Einträge spezifiziert sind. Es können beliebig viele solche Alternativen spezifiziert sein.

Sollte immer noch kein passenden Eintrag gefunden worden sein, dann wird der Wert genommen, die in ``<default>`` spezifiziert wird.

Aktuell sind die 5 Felder "title", "author", "publisher", "pubdate", "inst" sowohl notwendig als auch maximal erlaubt.


## Ablauf

Beim Erzeugen der Handle für der top-level logischer Strukturelement des METS-Dateis, werden zusätzlich zu der Handle Id und der dazugehörigen URL, die oben beschriebene 5 Felder auch in dem Handle gespeichert. Das sieht dann wie folgt aus:

```
Handle Values for: 21.T11998/goobi-go-1296243265-17
Index	Type	Timestamp	Data
1	URL	2020-04-21 12:02:30Z 	https://viewer.goobi.io/idresolver?handle=
2	TITLE	2020-04-21 12:02:30Z 	[Stammbuch Daniel Schelling]
3	AUTHORS	2020-04-21 12:02:30Z 	Daniel Schelling
4	PUBLISHER	2020-04-21 12:02:30Z 	Stadtarchiv Duderstadt
5	PUBDATE	2020-04-21 12:02:30Z 	1617
6	INST	2020-04-21 12:02:30Z 	MPG
100	HS_ADMIN	2020-04-21 12:02:30Z 	handle=21.T11998/USER28; index=300; [create hdl,delete hdl,read val,modify val,del val,add val,modify admin,del admin,add admin,list]
```

Dieser Information wird dann von der GWDG benutzt, um automatisch ein DOI zu erzeugen mit der *gleichen* ID - in diesem Fall also "21.T11998/goobi-go-1296243265-17". 
