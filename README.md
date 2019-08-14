# Automatische Paginierung auf Basis der Dateinamen

Dies ist eine technische Dokumentation für das Plugin zum automatischen erstellen einer Paginierung basierend auf den Dateinamen.

## Einführung

Die vorliegende Dokumentation beschreibt die Installation, Konfiguration und den Einsatz des Plugins.

Mit Hilfe dieses Plugins können METS Dateien automatisch aufbereitet und eine Grundstruktur und die Paginierung gesetzt werden.

| Details ||
|--- |--- |
| Version des Plugins| 1.0.0 |
| Identifier|intranda_step_imagename_analyse|
| Source code|- noch nicht öffentlich verfügbar -|
| Kompatibilität | Goobi workflow 3.0.10 |
| Dokumentation vom | 14.08.2019 |

## Voraussetzung

Voraussetzung für die Verwendung des Plugins ist der Einsatz von Goobi mindestens in der Version 3.0.10, die korrekte Installation und Konfiguration des Plugins sowie die korrekte Einbindung des Plugins in die gewünschten Arbeitsschritte der Workflows.

## Installation und Konfiguration

Das Plugin besteht aus zwei Dateien:

```bash
plugin_intranda_step-imagename-analyse.jar
plugin_intranda_step_imagename_analyse.xml
```

Die Datei ```plugin_intranda_step-imagename-analyse.jar``` enthält die Programmlogik und muss für den tomcat-Nutzer lesbar in folgendes Verzeichnis installiert werden:

```bash
/opt/digiverso/goobi/plugins/step/
```

Die Datei ```plugin_intranda_step_imagename_analyse.xml``` muss ebenfalls für den tomcat-Nutzer lesbar sein und in folgendes Verzeichnis installiert werden:

```bash
/opt/digiverso/goobi/config/
```

Die Datei dient zur Konfiguration des Plugins und muss wie folgt aufgebaut sein:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config>
    <skipWhenDataExists>false</skipWhenDataExists>

    <paginationRegex>.*_(\d+\w?[rv]\w?)\.\w+</paginationRegex>

    <structureList>
        <item filepart="ER" docstruct="RearCover" />
        <item filepart="SV" docstruct="FrontSection" />
        <item filepart="VD" docstruct="FrontCover" />
        <item filepart="HD" docstruct="BackCover" />
        <item filepart="VS" docstruct="Endsheet" />
        <item filepart="NS" docstruct="Postscript" />
        <item filepart="FR" docstruct="Fragment" />
        <item filepart="Fragm" docstruct="Fragment" />
    </structureList>
</config>
```

Im Element ```skipWhenDataExists``` wird definiert, wie sich das Plugin verhält, wenn bereits eine Paginierung vorhanden ist. Bei dem Wert ```true``` wird die Ausführung übersprungen, bei ```false``` wird die vorhandene Struktur und Paginierung entfernt und eine neue erzeugt.

Das Element ```paginationRegex``` enthält einen regulären Ausdruck, mit dem versucht wird, die logische Seitenzahl aus dem Dateinamen zu extrahieren. Dabei wird der Wert aus der ersten Gruppe in die METS Datei übernommen.

Falls der reguläre Ausdruck nicht erfolgreich war, wird im Anschluß geprüft, ob der Dateiname eine besondere Struktur wie Cover, Titlepage oder Contents beschreibt. Diese Struktur wird innerhalb von ```structureList``` definiert. Dabei wird in jedem ```item``` im Attribut ```filepart``` ein (Teil)string definiert, der im Dateinamen vorkommen muss und im Attribut ```docstruct``` das Strukturelement, das in dem Fall erzeugt werden soll.

## Einstellungen in Goobi

Nachdem das Plugin installiert und konfiguriert wurde, kann es innerhalb eines Arbeitsschrittes von Goobi genutzt werden.

Dazu muss innerhalb der gewünschten Aufgabe das Plugin ```intranda_step_imagename_analyse``` eingetragen werden. Des Weiteren muss die Checkbox Automatische Aufgabe gesetzt sein.

## Arbeitsweise

Die Arbeitsweise des Plugins innerhalb des korrekt konfigurierten Workflows sieht folgendermaßen aus:

* Wenn das Plugin innerhalb des Workflows aufgerufen wurde, öffnet es die METS-Datei und prüft als erstes, ob bereits eine Paginierung existiert.
* Wenn dies der Fall ist, wird basierend auf dem konfigurierten Wert in ```skipWhenDataExists``` der Schritt entweder ohne weitere Änderungen abgeschlossen, oder die vorhandene Paginierung und Strukturierung aus der METS Datei entfernt.
* Anschließend werden die Dateien aus dem master Ordner gelesen und alphanumerisch sortiert.
* Für jede Datei wird nun geprüft, ob sie dem konfigurierten regulären Ausdruck entspricht.
* Ist dies der Fall, wird eine neue Page erstellt. Die physische Reihenfolge entspricht der Sortierung im Dateisystem, die logische Seitennummer wird aus der ersten Gruppe des regulären Ausdrucks geholt.
* Wenn der reguläre Ausdruck nicht zutrifft, wird im Anschluss die Liste der konfigurierten ```item``` durchlaufen und geprüft, ob der Dateiname mit dem Ausdruck, gefolgt von einer optionalen Zahl und einer optionalen recto-verso Angabe (r oder v) endet. Wenn dies der Fall ist, wird das konfigurierte Strukturelement erzeugt und die Seite wird diesem Element zugewiesen. Durch die Angabe einer Zählung können neue Stukturelemente des selben Typs definiert werden. Haben zwei oder mehr Dateien keine oder die gleiche Zählung, werden sie dem gleichen Strukturelement zugewiesen.
* Wenn weder der reguläre Ausdruck noch die Liste der Strukturelemente auf den Dateinamen zutreffen, wird eine Page mit der logischen Sortierung "uncounted" erzeugt und ein Eintrag in das Vorgangslog geschrieben.

## Beispiele

Die folgenden Beispiele basieren auf der oben definierten Konfiguration:

|Dateiname | Ausgabe |
|---|---|
|BxSem-A02_010v.tif|Seite 010v|
|BxSem-A02_146r.tif|Seite 146r|
|BxSem-A02_NSr.tif|erste Seite des Strukturelements Postscript|
|BxSem-A02_NSv.tif|zweite Seite des Strukturelements Postscript|
|BxSem-B04_Farbkarte_Einband.jpg|nicht konfiguriert, daher keine Zuordnung möglich, wird in als "uncounted" übernommen|
|BxSem-A22_VS1r.jpg|erstes Endsheet, erste Seite|
|BxSem-A22_VS1v.jpg|erstes Endsheet, zweite Seite|
|BxSem-A22_VS2.jpg|zweites Endsheet|
|BxSem-B08_SV.jpg|einziges Bild der FrontSection|