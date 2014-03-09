/**
 *  LIZENZBEDINGUNGEN - Seanox Software Solutions ist ein Open-Source-Projekt,
 *  im Folgenden Seanox Software Solutions oder kurz Seanox genannt.
 *  Diese Software unterliegt der Version 2 der GNU General Public License.
 *
 *  WebDAV, Advanced Server Developing
 *  Copyright (C) 2014 Seanox Software Solutions
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of version 2 of the GNU General Public License as published
 *  by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package com.seanox.webdav;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.seanox.common.Accession;
import com.seanox.common.Codec;
import com.seanox.common.Components;
import com.seanox.common.Initialize;
import com.seanox.common.Section;
import com.seanox.devwex.Generator;
import com.seanox.module.Request;
import com.seanox.module.http.Context;
import com.seanox.module.http.Process;
import com.seanox.webdav.storage.AbstractStore;
import com.seanox.webdav.storage.Store;
import com.seanox.xml.Stream;

/**
 *  Connector, stellt den Haupteinsprung und die Funktionalitaeten fuer WebDAV
 *  Level 2 zur Verf&uuml;gung. WebDAV (Web-based Distributed Authoring and
 *  Versioning) ist ein offener Standard zur Bereitstellung von Dateisystemen
 *  per HTTP. Die Konfiguration des Moduls erfolgt zentral &uuml;ber die
 *  Konfiguration des Servers und l&auml;sst sich detaliert f&uuml;r einzelne
 *  virtuelle Verzeichnisse einrichten.<br>
 *  <br>
 *  <b>Installation:</b><br>
 *  <br>
 *  Das Paket muss in den Klassenpfad von Devwex aufgenommen werden. Dazu kann
 *  dieses im Bibliotheksverzeichnis <code>./devwex/libraries</code> abgelegt
 *  werden und wird dann automatisch beim Start vom Server in den Klassenpfad
 *  &uuml;bernommen.<br>
 *  <br>
 *  <b>Konfiguration:</b><br>
 *  <br>
 *  Diese wird detailiert in den verf&uuml;gbaren Stores beschriebem.<br>
 *  <br>
 *  Die Server-Konfiguration ben&ouml;tigt die Deklaration der erforderlichen
 *  HTTP-Methode <code>OPTIONS</code>. Alle weiteren Methoden werden automatisch
 *  durch die Option <code>[X]</code> in der Modul-Konfiguration deklaraiert.
 *  Die Konfiguration der Module richtet sich dabei nach dem verwendeten Store
 *  und wird in der zugeh&ouml;rigen Dokumentation einzeln beschrieben.<br>
 *  <br>
 *  Beispiel:
 *  <pre>
 *  [SERVER:X:BAS]
 *    ...
 *    METHODS = OPTIONS HEAD GET POST
 *    ...
 *  </pre>
 *  Offene Punkte und bekannte Probleme:<br>
 *  <br>
 *  <ul>
 *    <li>
 *      Virtuelle Pfade von Modulen, welche als Referenz unter
 *      <code>(SERVER/VIRTUAL:XXX:REF)</code> eingerichtet werden, werden vom
 *      Server absolut betrachtet. Daher sollte hier auf ein endendes Slash bei
 *      der Definition virtueller Pfade geachtet werden. Im Beispiel von
 *      <code>/d</code> als virtueller Pfad, w&uuml;rde der Server bei der
 *      Anfrage nach <code>/documents</code>, <code>/d</code> als Wurzel und 
 *      <code>ocuments</code> als Pfad betrachten. Da die Wurzel nicht mit Slash
 *      endet und der Pfad nicht mit Slash beginnt w&uuml;rde diese Anfrage als
 *      ung&uuml;ltiger Pfad betrachtet und mit Status <code>NOT_FOUND</code>
 *      (404) beantwortet werden.
 *    </li>
 *    <li>
 *      Das Modul unterst&uuml;tzt keine Versionierung, die bei WebDAV
 *      m&ouml;glich ist.
 *    </li>
 *    <li>
 *      Das bewusste Sperren (LOCK) und Entsperren (UNLOCK) von Ressourcen wird
 *      nicht unterst&uuml;tzt.
 *    </li>
 *    <li>
 *      Alle Zugriffe werden als Transaktion betrachtet, die jedoch in der
 *      Ausf&uuml;hrungszeit (Timeout) nicht begrenzt werden.
 *    </li>
 *  </ul>
 *  Hinweis - diese Klasse basiert auf den Quellen der Datei WebdavServlet.java
 *  von Remy Maucherat aus dem org.apache.catalina.util Paket des WebDAV
 *  Projekts (http://webdav-servlet.sourceforge.net/index.html).<br>
 *  <br>
 *  Connector 1.2014.0202<br>
 *  Copyright (C) 2014 Seanox Software Solutions<br>
 *  Alle Rechte vorbehalten.
 *
 *  @author  Seanox Software Solutions
 *  @version 1.2014.0202
 */
public class Connector extends Context {

    /** Instanz vom zu verwendenen Store */
    private volatile Store store;
    
    /** Konfiguration des zu verwendenen Stores */
    private volatile Map<String, String> parameters;
    
    /** Liste der unterst&uuml;tzten Methoden */
    private static volatile List<String> methods;

    /** Liste der Mimetypes zum Aufl&ouml;sen von Dateiendungen */
    private static volatile Section mimetypes;

    /** Konstante f&uuml;r den Standardnamenraum des WebDav */
    private static final String DEFAULT_XML_NAMESPACE = "D";

    /** Konstante f&uuml;r die Deklaration vom Namenraum des WebDav */
    private static final String DEFAULT_XML_NAMESPACE_DECLARATION = (" xmlns:").concat(Connector.DEFAULT_XML_NAMESPACE).concat("=\"DAV:\"");

    /** Konstante f&uuml;r die Methode HEAD */
    private static final String METHOD_HEAD = "HEAD";

    /** Konstante f&uuml;r die Methode PROPFIND */
    private static final String METHOD_PROPFIND = "PROPFIND";

    /** Konstante f&uuml;r die Methode PROPPATCH */
    private static final String METHOD_PROPPATCH = "PROPPATCH";

    /** Konstante f&uuml;r die Methode MKCOL */
    private static final String METHOD_MKCOL = "MKCOL";

    /** Konstante f&uuml;r die Methode COPY */
    private static final String METHOD_COPY = "COPY";

    /** Konstante f&uuml;r die Methode MOVE */
    private static final String METHOD_MOVE = "MOVE";

    /** Konstante f&uuml;r die Methode PUT */
    private static final String METHOD_PUT = "PUT";

    /** Konstante f&uuml;r die Methode GET */
    private static final String METHOD_GET = "GET";

    /** Konstante f&uuml;r die Methode OPTIONS */
    private static final String METHOD_OPTIONS = "OPTIONS";

    /** Konstante f&uuml;r die Methode DELETE */
    private static final String METHOD_DELETE = "DELETE";

    /** Konstante f&uuml;r PROPFIND - Specify a property mask */
    private static final int FIND_BY_PROPERTY = 0;

    /** Konstante f&uuml;r PROPFIND - Display all properties */
    private static final int FIND_ALL_PROP = 1;

    /** Konstante f&uuml;r PROPFIND - Property names */
    private static final int FIND_PROPERTY_NAMES = 2;
    
    /** Konstante f&uuml;r die max. Tiefe bei unbegrenzter Verarbeitungstiefe */
    private static final int INFINITY = 3;

    /** Konstante der Anwendungsversion */
    public static final String VERSION = "@@@ant-project-version";
    
    /**
     *  Ermittelt das verf&uuml;gbare Mapping von Mimetypes aus der
     *  &uuml;bergebenen Konfiguration.
     *  @param  section Konfiguration 
     *  @return das ermittelte Mapping von Mimetypes 
     */
    @SuppressWarnings("unchecked")
    private static Section fillMimetypes(Section section) {
        
        Enumeration<String> enumeration;
        Section             mimetypes; 
        String              stream;
        String              string;
        StringTokenizer     tokenizer;
        
        //die Mimetypes werden eingerichtet
        mimetypes = new Section();

        //die ContentTypes werden ermittelt
        enumeration = section.elements();

        //die Mimetypes werden entsprechend den Dateiendungen aufgebaut
        //unvollstaendige Eintraege werden nicht beruecksichtig
        while (enumeration.hasMoreElements()) {

            //der Mimetype wird ermittelt
            string = enumeration.nextElement();

            //die Dateiendungen werden ermittelt
            tokenizer = new StringTokenizer(section.get(string));

            while (tokenizer.hasMoreTokens() && string.length() > 0) {

                stream = tokenizer.nextToken().trim();

                if (stream.length() > 0) mimetypes.set(stream, string.toLowerCase());
            }
        }

        return mimetypes;
    }
    
    /**
     *  Ermittelt die verf&uuml;gbaren HTTP-Methoden.
     *  @return die verf&uuml;gbaren HTTP-Methoden
     *  @throws Exception bei unerwarteten Laufzeitfehlern  
     */
    private static List<String> fillMethods() throws Exception {
        
        String  string;

        Field[] fields;

        int     modifiers;        
        
        //das Mapping wird initial eingerichtet
        methods = new ArrayList<String>();

        //alle Felder der Klasse werde ermittelt
        fields = Status.class.getDeclaredFields();

        for (Field field : fields) {
            
            //die Modifiers zum Feld werden ermittelt
            modifiers = field.getModifiers();

            //das Feld muss als public final int deklariert sein
            if (field.getType().equals(String.class)
                && Modifier.isFinal(modifiers)
                && Modifier.isPrivate(modifiers)
                && Modifier.isStatic(modifiers)
                && field.getName().startsWith("METHOD_")) {

                //der Name der Methode wird vereinfacht
                string = (String)field.get(null);
                string = (string == null) ? "" : string.trim().toUpperCase();

                //die Methode wird uebernommen, wenn sie nicht enthalten ist
                if (string.length() > 0 && !methods.contains(string)) methods.add(string);
            }
        }
        
        return methods;
    }

    /**
     *  Einsprung f&uuml;r die Initialisierung des Moduls.
     *  @param  options Konfiguration
     *  @throws Exception allgemein auftretende nicht behandelte Fehler
     */
    @SuppressWarnings("unchecked")
    public void initialize(Section options) throws Exception {

        Initialize initialize;
        String     storage;        
        
        initialize = com.seanox.module.Context.getConfiguration();
        
        //die Mimetypes werden geladen
        Connector.mimetypes = Connector.fillMimetypes(Section.parse(initialize.get("mimetypes")));
        
        //die Methode werden geladen
        Connector.methods = Connector.fillMethods();
        
        this.parameters = options.export();
        
        storage = options.get(AbstractStore.PARAMETER_STORAGE);

        if (storage.length() == 0) throw new IllegalArgumentException("Invalid storage defined (empty)");

        //der Store wird initial eingerichtet und registriert
        this.store = (Store)Class.forName(storage).newInstance();

        this.store.checkAuthentication();
    }

    /**
     *  R&uuml;ckgabe der Kennung des Moduls.
     *  @return die Modulkennung als String
     */
    public String getCaption() {

        return ("Seanox-WebDAV/").concat(Connector.VERSION);
    }

    /**
     *  Ermittelt den Mimetype zur angegebenen Ressource. Kann der Mimetype
     *  nicht aufgel&ouml;st werden, wird ein leerer String zur&uuml;ckgegeben.
     *  @param  resource Ressource
     *  @return der Mimetype zur angegebenen Ressource, sonst ein leerer String
     */
    private static String getMimeType(String resource) {

        int cursor;

        if (Connector.mimetypes == null) return "";

        //die Dateiendung wird ermittelt
        cursor = resource.lastIndexOf(".");
        cursor = (cursor < 0) ? 0 : cursor +1;

        //der Mimetype wird ermittelt
        return Connector.mimetypes.get(resource.substring(cursor));
    }

    /**
     *  Konvertiert relevante Sonderzeichen in UTF-8 und MIME.
     *  @param  path zu konvertierender Pfad
     *  @return der konvertierte Pfad
     */
    private static String rewriteUrl(String path) {

        return Codec.encode(Codec.encode(path, Codec.UTF8), Codec.MIME);
    }

    /**
     *  Erstellt die HTTP Signature f&uuml;r den angegebenen Status Code.
     *  @param  status Status Code
     *  @return die f&uuml;r den Status Code erstellte HTTP Signature 
     */
    private static String generateStatusSignature(int status) {

        //HINWEIS - HTTP/1.1 ist hier zwingend zu verwenden, da sonst einige
        //WebDAV Clients nicht funktionieren, da sie starr 1.1 erwarten

        return ("HTTP/1.1 ").concat(String.valueOf(status).concat(" ")
            .concat(Status.getMessage(status)).trim());
    }

    /**
     *  Geht f&uuml;r PROPFIND rekursiv durch alle Verzeichnisse.
     *  @param  path       aktueller Pfad
     *  @param  process    Process
     *  @param  xml        XML Datenstrom
     *  @param  store      Store
     *  @param  type       PROPFIND Typ
     *  @param  properties Properties
     *  @param  depth      Tiefe
     *  @throws IOException bei fehlerhaftem Zugriff auf den Store
     */
    private static void recursiveParseProperties(Process process, Stream xml,
        Store store, String path, int type, Properties properties, int depth) throws IOException {
    
        String   target;
    
        String[] names;
    
        Connector.parseProperties(process, xml, store, path, type, properties);

        if (depth <= 0) return;

        names = store.getChildrenNames(path);

        if (names != null) {

            target = path.endsWith("/") ? path : path.concat("/");

            for (String name : names) {

                Connector.recursiveParseProperties(process, xml, store, target.concat(name), type, properties, depth -1);
            }
        }
    }
    
    /**
     *  Ermittelt das &uuml;bergeordnete Verzeichnis vom angegebenen Pfad.
     *  @param  path Pfad
     *  @return das &uuml;bergeordnete Verzeichnis vom angegebenen Pfad
     */
    private static String getParentPath(String path) {
    
        int slash = path.lastIndexOf('/');
        
        if (slash > 0)   return path.substring(0, slash);
        if (slash != -1) return path.substring(0, 1);
        
        return null;
    }
    
    /**
     *  Ermittelt mit dem Prozess &uuml;bergebenen relativen Pfad.
     *  @param  process Process
     *  @return der mit dem Prozess &uuml;bergebenen relativen Pfad
     */
    private static String getProcessResourcePath(Process process) {
    
        String result;

        result = process.environment.get("PATH_INFO");
        
        if (result.length() == 0) result = "/";
        
        if (!result.startsWith("/")) result = ("/").concat(result);
        
        return Codec.decode(result, Codec.DOT);
    }

    /**
     *  Ermittelt ein (Unter)Element im &uuml;bergebenen Element.
     *  R&uuml;ckgabe das ermittelte Element, sonst <code>null</code>.
     *  @param  node zu durchsuchendes Element
     *  @param  name gesuchtes Element
     *  @return das ermittelte Element, sonst <code>null</code>
     */
    public static Node findSubElement(Node node, String name) {
        
        Node child;
    
        if (node == null) return null;
        
        child = node.getFirstChild();
        
        for (child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            
            if (child.getNodeType() != Node.ELEMENT_NODE || !child.getLocalName().equalsIgnoreCase(name)) continue;
            
            return child;
        }

        return null;
    }
    
    /**
     *  Ermittelt die Properties aus der &uuml;bergebenen Knotenstruktur.
     *  R&uuml;ckgabe die ermittelten Properties als Liste, k&ouml;nnen keine
     *  ermittelt werden, wird eine leere Liste zur&uuml;ck gegeben.
     *  @param  node Knotenstruktur
     *  @return die ermittelten Properties als Liste
     */
    private static Properties getPropertiesFromXml(Node node) {
    
        Properties properties;
        Node       entry;
        NodeList   childs;
        String     value;
        
        int        loop;
        
        properties = new Properties();
        
        childs = node.getChildNodes();

        for (loop = 0; loop < childs.getLength(); loop++) {
        
            entry = childs.item(loop);
            
            if (entry.getNodeType() != Node.ELEMENT_NODE) continue;
            
            value = entry.getNodeValue();
            
            if (value == null) value = entry.getTextContent();
            
            properties.put(entry.getLocalName(), value);
        }
        
        return properties;
    }

    /**
     *  R&uuml;ckgabe der &uuml;bermittelten Verarbeitungstiefe (Depth).
     *  @param  request Request
     *  @return die im Request &uuml;bermittelten Verarbeitungstiefe (Depth)
     */
    private static int getDepth(Request request) {
    
        String string;
    
        int    depth;
        
        string = request.getHeaderField("Depth");
        
        if (string.equals("0")) depth = 0;
        else if (string.equals("1")) depth = 1;
        else if (string.equalsIgnoreCase("infinity")) depth = Connector.INFINITY;
        else depth = Connector.INFINITY;
        
        return depth;
    }

    /**
     *  Entfernt das Slash am Ende von Pfaden, wenn diese existiert.
     *  @param  path Pfad
     *  @return der bereinigte Pfad ohne endendes Slash
     */
    private static String getCleanPath(String path) {

        return (path.endsWith("/") && path.length() > 1) ? path.substring(0, path.length() -1) : path;
    }
    
    /**
     *  Formatiert das Datum im angebenden Format und in der angegebenen Zone.
     *  R&uuml;ckgabe das formatierte Datum, im Fehlerfall ein leerer String.
     *  @param  format Formatbeschreibung
     *  @param  date   zu formatierendes Datum
     *  @param  zone   Zeitzone, <code>null</code> Standardzone
     *  @return das formatierte Datum als String, im Fehlerfall leerer String
     */
    private static String formatDate(String format, Date date, String zone) {

        SimpleDateFormat pattern;

        //die Formatierung wird eingerichtet
        pattern = new SimpleDateFormat(format, Locale.US);

        //die Zeitzone wird gegebenenfalls fuer die Formatierung gesetzt
        if (zone != null) pattern.setTimeZone(TimeZone.getTimeZone(zone));

        //die Zeitangabe wird formatiert
        return pattern.format(date);
    }

    /**
     *  Entfernt aus dem String die Optionsinformationen im Format
     *  <code>[...]</code>. R&uuml;ckgabe der String ohne endende Optionen.
     *  @param  string zu bereinigender String
     *  @return der String ohne endende Optionen
     */
    private static String cleanOptions(String string) {

        int cursor;

        string = string.trim();

        while (string.endsWith("]") && (cursor = string.lastIndexOf("[")) >= 0) {

            string = string.substring(0, cursor).trim();
        }

        return string;
    }

    /**
     *  List das mit dem Request &uuml;bergebene XML Dokument aus.
     *  @param  process Process
     *  @return das ausgelesen XML Dokument
     *  @throws IOException bei fehlerfahftem Zuriff auf die Datenstr&ouml;me
     *  @throws SAXException bei fehlerfahftr XML-Verarbeitung
     *  @throws ParserConfigurationException bei fehlerfahftr XML-Verarbeitung
     */
    private static Document readDocument(Process process) throws IOException,
        SAXException, ParserConfigurationException {

        DocumentBuilderFactory factory;
        OutputStream           buffer;

        byte[]                 bytes;

        int                    volume;
        int                    length;
        int                    size;

        if ((length = process.request.getContentLength()) <= 0) return null;

        buffer = new ByteArrayOutputStream();

        bytes = new byte[(process.blocksize <= 0) ? 65535 : process.blocksize];

        for (volume = 0; volume < length && (size = process.request.read(bytes)) >= 0;) {

            volume += size;

            buffer.write(bytes, 0, size);
        }

        factory = DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);

        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(((ByteArrayOutputStream)buffer).toByteArray()));
    }

    /**
     *  Liest den Inhalt der angegebenen Datei und gibt als ByteArray.
     *  @param  filename  Pfad der zu lesenden Datei
     *  @param  blocksize Gr&ouml;sse der Datenbl&ouml;cke in Bytes
     *  @return der gelesene Dateiinhalt als ByteArray
     *  @throws IOException bei fehlerhaftem Zugriff auf Dateien und
     *          Datenstr&ouml;me
     */
    private static byte[] readFile(String filename, int blocksize) throws IOException {

        InputStream  input;
        OutputStream buffer;

        byte[]       bytes;

        int          length;

        //initiale Einrichtung der Variablen
        buffer = new ByteArrayOutputStream();
        bytes  = new byte[blocksize];
        input  = null;

        try {

            //der Datenstrom wird eingerichtet
            input = new FileInputStream(filename);

            //der Datenstrom wird ausgelesen
            while ((length = input.read(bytes)) >= 0) buffer.write(bytes, 0, length);

        } finally {

            //der Datenstrom wird geschlossen
            try {input.close();
            } catch (Exception exception) {

                //keine Fehlerbehandlung vorgesehen
            }
        }

        return ((ByteArrayOutputStream )buffer).toByteArray();
    }

    /**
     *  Ermittelt die Dateiinformationen zur angegebenen Ressource.
     *  @param  store   Store
     *  @param  path    Ressource
     *  @param  childs  Childs
     *  @param  assign  Sortierung
     *  @param  hidden  Option zum Ausblenden von nicht sichbaren Dateien
     *  @return die Dateiinformationen zur angegebenen Ressource
     *  @throws IOException bei Fehlern im Zusammenhang mit dem Datenzugriff
     */
    private static List<String> getDirectoryChildsInfos(Store store, String path,
            String[] childs, int[] assign, boolean hidden) throws IOException {
        
        List<String>  storage;
        String        string;
        StringBuilder filter;
        
        String[]      entries;
        
        boolean       option;
        int           cursor;
        
        storage = new ArrayList<String>();
        entries = new String[6];
        filter  = new StringBuilder();
        
        //die Dateiliste wird ermittelt
        for (String child : childs) {
                
            //die Dateiinformationen werden zusammengestellt
            
            //die Eintraege werden als Array abgelegt um eine einfache und
            //flexible Zuordnung der Sortierreihenfolge zu erreichen
            //0 - base, 1 - name, 2 - sort, 3 - date, 4 - size, 5 - type

            //der Name fuer die Ausgabe wird ermittelt
            entries[1] = child;

            //der Name fuer die Sortierung wird ermittelt
            entries[2] = entries[1].toLowerCase();
            
            child = path.concat(child);
            
            //Eintraegte der Option "versteckt" werden ggf. ignoriert
            if (hidden && store.isHidden(child)) continue;

            option = store.isFolder(child);

            //der Zeitpunkt der letzten Aenderung wird ermittelt
            entries[3] = Connector.formatDate("yyyy-MM-dd HH:mm:ss", store.getLastModified(child), null);

            //die Groesse wird ermittelt, nicht aber bei Verzeichnissen
            string = (option) ? "-" : new DecimalFormat("#,###").format(store.getResourceLength(child));

            //die Groesse wird an der ersten Stelle mit dem Character
            //erweitert welches sich aus der Laenge der Groesse ergibt um
            //diese nach numerischer Groesse zu sortieren
            entries[4] = String.valueOf((char)string.length()).concat(string);

            //der Basistyp wird ermittelt
            entries[0] = option ? "directory" : "file";

            string = entries[1];
            cursor = string.lastIndexOf(".");
            string = string.substring((cursor < 0) ? string.length() : cursor +1);

            //der Dateityp wird ermittlet, nicht aber bei Verzeichnissen
            entries[5] = option ? "-" : string;
            
            filter.setLength(0);
            
            filter.append(" ").append(entries[assign[0]]);
            filter.append("\0 ").append(entries[assign[1]]);
            filter.append("\0 ").append(entries[assign[2]]);
            filter.append("\0 ").append(entries[assign[3]]);
            filter.append("\0 ").append(entries[assign[4]]);
            filter.append("\0 ").append(entries[assign[5]]);

            storage.add(filter.toString());
        }
        
        return storage;
    }
    
    /**
     *  Generiert die Pfad-Information f/uuml;r die Verzeichnisansicht.
     *  @param  generator Generator
     *  @param  path      Path
     *  @param  elements  Parameter
     *  @return die Pfad-Information f/uuml;r die Verzeichnisansicht
     */
    @SuppressWarnings("unchecked")
    private static String getDirectoryPathTemplate(Generator generator, String path, Hashtable<String, String> elements) {
        
        StringBuilder result;
        StringBuilder parent;
        
        result = new StringBuilder(); 
        parent = new StringBuilder();
        
        elements = (Hashtable<String, String>)elements.clone();

        //der Pfad wird fragmentiert. somit koennen Unterverzeichnisse als
        //einzelne Links abgebildet werden
        for (String entry : path.split("/")) {
            
            if (result.length() == 0 && entry.length() == 0) continue;
            
            parent.append("/").append(entry);

            elements.put("base", parent.toString());
            elements.put("name", entry);

            result.append(new String(generator.extract("path", elements, false)));
        }
        
        return result.toString();
    }    
    
    /**
     *  Ermittelt den Inhalt vom Template zur Dastellung von Verzeichnissen
     *  entsprechend der Server-Konfiguration als ByteArray.
     *  @param  process Process
     *  @return der Inhalt vom Template als ByteArray, wenn keine Template
     *          ermittelt werden kann ist das ByteArray leer
     *  @throws IOException bei fehlerhaftem Zugriff auf das Dateisystem
     */
    private static byte[] getDirectoryTemplateContent(Process process) throws IOException {
        
        Object  accession;
        Object  object;
        Section section;
        String  stream;
        String  string;
        
        //der Definitionsblock wird eingerichtet
        section = new Section();
        
        try {

            accession = Accession.get(process, "accession");

            //der Inhalt vom Feld references wird kopiert
            object = Context.mountField(accession, "references", true);

            //die Eintraege werden uebernommen
            Accession.storeField(object, section, "entries");
            Accession.storeField(object, section, "list");

        } catch (Exception exception) {

            string = String.valueOf(exception.getMessage());

            throw new RuntimeException(("Internal connector error (").concat(string).concat(")"));
        }

        //der individuelle INDEX wird ermittelt, eventuelle Optionen werden
        //entfernt, konnte keiner ermittelt werden wird der Standard verwendet
        string = Connector.cleanOptions(section.get("system:index"));

        //das Systemverzeichnis wird ermittelt
        try {stream = (String)Accession.get(accession, "sysroot");
        } catch (Exception exception) {stream = ".";}

        if (string.length() == 0) string = stream.concat("/index.html");
        
        return Connector.readFile(string, (process.blocksize <= 0) ? 65535 : process.blocksize);
    }
    
    /**
     *  Ermittelt zum Pfad eine existierende Default-Datei.
     *  Kann keine ermittelt werden wird <code>null</code> zur&uuml;ckgegeben.
     *  @param  process Process
     *  @param  store   Store
     *  @param  path    Path
     *  @return die ermittelte Default-Datei, sonst <code>null</code>
     *  @throws IOException bei fehlerhaftem Zugriff auf den Store
     */
    private static String getDirectoryDefault(Process process, Store store, String path) throws IOException {
        
        String          string;
        StringTokenizer tokenizer;
        
        //die Defaultdateien werden ermittelt
        tokenizer = new StringTokenizer(process.options.get("default").replace('\\', '/'));

        while (tokenizer.hasMoreTokens()) {

            string = tokenizer.nextToken().trim();

            if (string.length() <= 0 || string.indexOf('/') >= 0 ) continue;
            
            if (store.isResource(path.concat(string))) return string; 
        }
        
        return null;
    }
    
    /**
     *  Erstellt auf Basis des Templates <code>index.html</code> oder der
     *  Referenz <code>SYSTEM:INDEX</code> navigierbares HTML zum aktuellen
     *  Verzeichnis.
     *  @param  process Process
     *  @param  store   Store
     *  @return das Verzeichnis als navigierbares HTML
     *  @throws IOException beim fehlerhaften Zugriff auf den Store
     */
    @SuppressWarnings("unchecked")
    private static byte[] getDirectory(Process process, Store store) throws IOException {

        OutputStream              structure;
        Enumeration<String>       enumeration;
        Generator                 generator;
        Hashtable<String, String> elements;
        List<String>              storage;
        String                    string;
        String                    query;
        String                    path;

        String[]                  entries;
        String[]                  tokens;
        
        int[]                     assign;
        
        boolean                   hidden;
        boolean                   reverse;
        int                       cursor;

        //der Strukturpuffer wird eingerichtet
        structure = new ByteArrayOutputStream();

        //die Parameter fuer den Generator werden eingerichtet
        elements = new Hashtable<String, String>();

        //der QueryString wird ermittelt
        query = process.request.getQueryString();

        //das Template wird geladen
        generator = Generator.parse(Connector.getDirectoryTemplateContent(process));

        //alle Felder des Headers werden ermittelt
        enumeration = process.request.getHeaderFields();

        //alle Felder des Headers werden uebernommen
        while (enumeration.hasMoreElements()) {

            string = enumeration.nextElement();

            elements.put(string, process.request.getHeaderField(string));
        }

        //die Umgebungsvariablen werden uebernommen
        elements.putAll(process.environment.export());

        string = process.environment.get("path_base");

        if (!string.endsWith("/")) string = string.concat("/");

        elements.put("path-base", string);

        elements.put("path", Connector.getDirectoryPathTemplate(generator, string, elements));

        //die Sortierrichtung wird ermittelt
        cursor  = (query.length() > 0) ? (int)query.charAt(0) : 0;
        reverse = (cursor >= 'A' && cursor <= 'Z');

        //die Zuordung der Felder fuer die Sortierung wird definiert
        string = query.toLowerCase();
        cursor = cursor > 0 ? (int)string.charAt(0) : 0;

        if (cursor >= 'a' && cursor <= 'z') elements.put("sort", string.concat(reverse ? "d" : "a"));

        //die Sortierung wird aus dem Query festgelegt und erfolgt nach
        //Base, Query und Name, die Eintraege sind als Array abgelegt um eine
        //einfache und flexible Zuordnung der Sortierreihenfolge zu erreichen
        //0 - base, 1 - name, 2 - sort, 3 - date, 4 - size, 5 - type
        if (string.equals("d")) {

            //base, date, sort, size, type, name
            assign = new int[] {0, 3, 2, 4, 5, 1};

        } else if (string.equals("s")) {

            //base, size, sort, date, type, name
            assign = new int[] {0, 4, 2, 3, 5, 1};

        } else if (string.equals("t")) {

            //base, type, sort, date, size, name
            assign = new int[] {0, 5, 2, 3, 4, 1};

        } else {

            //base, sort, date, size, type, name
            assign = new int[] {0, 2, 3, 4, 5, 1};
        }

        //der Pfad wird ermittelt
        path = Connector.getProcessResourcePath(process);
        
        //die Sichbarkeit nicht sichbarer Eintraege wird ermittelt
        hidden = process.options.get("index").toUpperCase().indexOf("[S]") >= 0; 

        //die Dateiliste wird ermittelt
        entries = store.getChildrenNames(path);
        storage = Connector.getDirectoryChildsInfos(store, path, entries, assign, hidden);
                
        //die Dateiliste wird sortiert
        Collections.sort(storage);

        if (reverse) Collections.reverse(storage);
        
        entries = new String[6];

        //die Dateiinformationen werden zusammengestellt
        for (String entry : storage) {

            tokens = entry.split("\0");

            //die Eintraege werden als Array abgelegt um eine einfache und
            //flexible Zuordnung der Sortierreihenfolge zu erreichen
            //0 - base, 1 - name, 2 - sort, 3 - date, 4 - size, 5 - type

            entries[assign[0]] = tokens[0].substring(1);
            entries[assign[1]] = tokens[1].substring(1);
            entries[assign[2]] = tokens[2].substring(1);
            entries[assign[3]] = tokens[3].substring(1);
            entries[assign[4]] = tokens[4].substring(1);
            entries[assign[5]] = tokens[5].substring(1);

            //Dateien ohne Namen werden unterdrueckt
            if (entries[1].length() == 0) continue;

            elements.put("base", entries[0]);
            elements.put("name", entries[1]);
            elements.put("date", entries[3]);
            elements.put("size", entries[4].substring(1));
            elements.put("type", entries[5]);

            if (!entries[5].equals("-")) {

                string = process.mimetypes.get(entries[5]);

                if (string.length() == 0) string = process.options.get("mimetype");

            } else string = "";

            cursor = string.indexOf('/');

            elements.put("mime", (cursor < 0) ? string : string.substring(0, cursor).trim());

            string = (cursor < 0) ? "" : string.substring(cursor +1).trim();

            elements.put("code", string);
            
            string = string.replace('.', ' ');
            string = string.replace('-', ' ');

            elements.put("case", string);

            try {structure.write(generator.extract("files", elements, false));
            } catch (Exception exception) {

                //keine Fehlerbehandlung erforderlich
            }
        }

        //die generierten Fragmente werden als Element gesetzt
        elements.put("files", ((ByteArrayOutputStream)structure).toString());

        //die Elemente werden gefuellt
        generator.define("index", elements);

        return generator.extract(true);
    }

    /**
     *  Ermittelt die zul&auml;ssigen Optionen zur im Request angegebenen
     *  Ressource.
     *  @param  process Process
     *  @param  store   Store
     *  @throws IOException bei fehlerhaftem Zugriff auf Ressourcen oder
     *          Datenstr&ouml;me
     */
    private static void doOptions(Process process, Store store) throws IOException {

        String path;
        
        path = Connector.getProcessResourcePath(process);
        
        process.response.setHeaderField("DAV", "1, 2");
        process.response.setHeaderField("Allow", Connector.determineMethodsAllowed(process, store, path));
        process.response.setHeaderField("MS-Author-Via", "DAV");
    }
    
    /**
     *  Ermittelt die Eigenschaften der im Request angegebenen Ressource.
     *  @param  process Process
     *  @param  store   Store
     *  @throws IOException bei fehlerhaftem Zugriff auf Ressourcen oder
     *          Datenstr&ouml;me
     *  @throws ParserConfigurationException  bei fehlerhafter XML Verarbeitung
     *          des Request-Bodys
     */
    private static void doPropfind(Process process, Store store)
        throws IOException, ParserConfigurationException {

        Document   document;
        Element    root;
        Node       child;
        Node       node;
        NodeList   childs;
        Properties properties;
        String     path;
        Stream     stream;
        String     space;
        String     name;

        int        depth;
        int        loop;
        int        type;

        space = Connector.DEFAULT_XML_NAMESPACE;
        depth = Connector.getDepth(process.request);        
        path  = Connector.getProcessResourcePath(process);
        
        if (!store.lock(path, false)) {Connector.forceStatus(process, Status.LOCKED); return;}
        
        try {
        
            if (!store.existsObject(path)) {Connector.forceStatus(process, Status.NOT_FOUND); return;}
            
            node = null;
            path = Connector.getCleanPath(Connector.getProcessResourcePath(process));
            type = Connector.FIND_ALL_PROP;

            document = Connector.readDocument(process);

            if (document != null) {

                root = document.getDocumentElement();

                childs = root.getChildNodes();

                for (loop = 0; loop < childs.getLength(); loop++) {

                    child = childs.item(loop);

                    switch (child.getNodeType()) {

                        case Node.TEXT_NODE:

                            break;

                        case Node.ELEMENT_NODE:
                            
                            name = child.getLocalName().toLowerCase();  

                            if (name.equals("prop")) {

                                type = Connector.FIND_BY_PROPERTY;

                                node = child;
                                
                            } else  if (name.equals("propname")) {

                                type = Connector.FIND_PROPERTY_NAMES;
                                
                            } else if (name.equals("allprop")) {

                                type = Connector.FIND_ALL_PROP;
                            }

                            break;
                    }
                }

            } else type = Connector.FIND_ALL_PROP;

            properties = (type == Connector.FIND_BY_PROPERTY) ? Connector.getPropertiesFromXml(node) : null;

            process.response.setStatus(Status.MULTI_STATUS);
            process.response.setMessage(Status.getMessage(process.response.getStatus()));
            process.response.setHeaderField("Content-Type", "text/xml; charset=UTF-8");

            stream = new Stream(new BufferedOutputStream(process.response, (process.blocksize <= 0) ? 65535 : process.blocksize));
            
            stream.writeXmlHeader();
            
            stream.writeElement(space, ("multistatus").concat(Connector.DEFAULT_XML_NAMESPACE_DECLARATION), Stream.OPENING);
            
            if (depth == 0) {
            
                Connector.parseProperties(process, stream, store, path, type, properties);
                
            } else {
            
                Connector.recursiveParseProperties(process, stream, store, path, type, properties, depth);
            }
            
            stream.writeElement(space, "multistatus", Stream.CLOSING);
            
            stream.flush();
            
        } catch (SAXException exception) {
        
            Connector.forceStatus(process, Status.BAD_REQUEST);
        }
    }

    /**
     *  Setz die per XML &uuml;bermittelten Eigenschaften f&uuml;r die
     *  entsprechend im Request angegebenen Ressourcen. Im Fehlerfall wird ein
     *  Multistatus als Fehlerbericht erstellt.<br>
     *  Hinweis - die Methode ist derzeit nicht implementiert.
     *  @param  process Process
     *  @param  store   Store
     *  @throws IOException bei fehlerhaftem Zugriff auf Ressourcen oder
     *          Datenstr&ouml;me
     *  @throws ParserConfigurationException  bei fehlerhafter XML Verarbeitung
     *          des Request-Bodys
     */
    private static void doProppatch(Process process, Store store) 
            throws IOException, ParserConfigurationException {
        
        Document   document;
        Element    root;
        Node       change;
        Node       remove;
        Properties changes;
        Properties properties;
        Stream     stream;
        String     path;
        String     space;
        String     resource;
       
        int        bits;
        
        space = Connector.DEFAULT_XML_NAMESPACE;
        path  = Connector.getProcessResourcePath(process);
        
        if (!store.lock(path, false)) {Connector.forceStatus(process, Status.LOCKED); return;}
        
        try {
        
            if (!store.existsObject(path)) {Connector.forceStatus(process, Status.NOT_FOUND); return;}
            
            path = Connector.getCleanPath(Connector.getProcessResourcePath(process));
            
            resource = process.environment.get("path_absolute");

            if (!resource.startsWith("/")) resource = ("/").concat(resource);
            if (!resource.endsWith("/") && !path.startsWith("/")) resource = resource.concat("/");
            if (resource.endsWith("/") && path.startsWith("/")) resource = resource.substring(0, resource.length() -1);
            
            if (store.isFolder(path) && !path.endsWith("/")) path = path.concat("/");

            resource = resource.concat(path);

            document = Connector.readDocument(process);
            
            if (document != null) {
                
                changes = new Properties();
                
                root = document.getDocumentElement();
                
                change = Connector.findSubElement(Connector.findSubElement(root, "set"), "prop");
                remove = Connector.findSubElement(Connector.findSubElement(root, "remove"), "prop");
                                
                if (change != null) {
                    
                    properties = Connector.getPropertiesFromXml(change);
                    
                    for (String property : properties.keySet()) {
                        
                        try {
                            
                            //Microsoft Extension Properties
                            //siehe http://msdn.microsoft.com/en-us/library/jj557737%28v=office.12%29.aspx
                            if (("Win32CreationTime").equalsIgnoreCase(property)) {
                                
                                throw new Exception();
                                
                            } else if (("Win32FileAttributes").equalsIgnoreCase(property)) {
                                
                                bits = Integer.parseInt(properties.get(property), 16);
                                
                                //Win32FileAttributes
                                //siehe http://msdn.microsoft.com/en-us/library/windows/desktop/aa365535%28v=vs.85%29.aspx
                                store.setReadOnly(path, (bits & 0x01) != 0);
                                store.setHidden(path, (bits & 0x02) != 0);
                                store.setSystem(path, (bits & 0x04) != 0);
                                store.setArchive(path, (bits & 0x20) != 0);

                            } else if (("Win32LastAccessTime").equalsIgnoreCase(property)) {
                                
                                throw new Exception();
                                
                            } else if (("Win32LastModifiedTime").equalsIgnoreCase(property)) {
                                
                                throw new Exception();
                                
                            } else throw new Exception();
                            
                            changes.put(property, Connector.generateStatusSignature(Status.SUCCESS));

                        } catch (Exception exception) {
                            
                            changes.put(property, Connector.generateStatusSignature(Status.CONFLICT));
                        }
                    }
                }
                
                if (remove != null) {
                    
                    properties = Connector.getPropertiesFromXml(remove);
                    
                    for (String property : properties.keySet()) {
                        
                        changes.put(property, Connector.generateStatusSignature(Status.CONFLICT));
                    }
                }

                process.response.setStatus(Status.MULTI_STATUS);
                process.response.setMessage(Status.getMessage(process.response.getStatus()));
                process.response.setHeaderField("Content-Type", "text/xml; charset=UTF-8");
                
                stream = new Stream(new BufferedOutputStream(process.response, (process.blocksize <= 0) ? 65535 : process.blocksize));

                stream.writeXmlHeader();
                
                stream.writeElement(space, "multistatus", Stream.OPENING);
                stream.writeElement(space, "response", Stream.OPENING);
                
                stream.writeProperty(space, "href", Connector.escapeOutput(Connector.rewriteUrl(resource)));
                
                for (String property : changes.keySet()) {
                    
                    stream.writeElement(space, "propstat", Stream.OPENING);
                    stream.writeElement(space, "prop", Stream.OPENING);
                    stream.writeElement(null, property, Stream.EMPTY);
                    stream.writeElement(space, "prop", Stream.CLOSING);
                    stream.writeProperty(space, "status", changes.get(property));
                    stream.writeElement(space, "propstat", Stream.CLOSING);
                }
                
                stream.writeElement(space, "response", Stream.CLOSING);
                stream.writeElement(space, "multistatus", Stream.CLOSING);

                stream.flush();                    
                
            } else Connector.forceStatus(process, Status.UNPROCESSABLE_ENTITY);
            
        } catch (SAXException exception) {
        
            Connector.forceStatus(process, Status.BAD_REQUEST);
        }
    }
    
    /**
     *  R&uuml;ckgabe des Inhalts der per Request angeforderten Ressource.
     *  Handelt es sich dabei um ein Verzeichnis, wird dieses als navigierbares
     *  HTML an den Client gesendet.
     *  @param  process Process
     *  @param  store   Store
     *  @throws IOException bei fehlerhaftem Zugriff auf Ressourcen oder
     *          Datenstr&ouml;me
     */
    private static void doGet(Process process, Store store) throws IOException {
        
        Connector.doGet(process, store, false);
    }

    /**
     *  R&uuml;ckgabe des Inhalts der per Request angeforderten Ressource.
     *  Handelt es sich dabei um ein Verzeichnis, wird dieses als navigierbares
     *  HTML an den Client gesendet.
     *  @param  process  Process
     *  @param  store    Store
     *  @param  headonly <code>true</code>, wenn nur der Header gesendet wird   
     *  @throws IOException bei fehlerhaftem Zugriff auf Ressourcen oder
     *          Datenstr&ouml;me
     */
    private static void doGet(Process process, Store store, boolean headonly) throws IOException {
    
        InputStream input;
        String      mimetype;
        String      file;
        String      path;
        String      url;
    
        byte[]      bytes;

        boolean     control;
        int         size;
        long        volume;
        
        path = Connector.getProcessResourcePath(process);
        file = store.isFolder(path) ? Connector.getDirectoryDefault(process, store, path) : null;
        
        if (file != null) path = path.concat(file);

        if (!store.lock(path, false)) {Connector.forceStatus(process, Status.LOCKED); return;}
        
        url = process.request.getURL();

        if (store.isResource(path)) {
        
            if (url.endsWith("/") && file == null) {
        
                if (url.length() == 1) Connector.forceStatus(process, Status.NOT_FOUND);
                else Connector.forceRedirect(process, url.substring(0, url.length() -1), null);
        
            } else {

                process.response.setHeaderField("last-modified", Connector.formatDate("E, dd MMM yyyy HH:mm:ss z", store.getLastModified(path), "GMT"));

                volume = store.getResourceLength(path);
                
                if (volume > 0) process.response.setHeaderField("Content-Length", String.valueOf(volume));

                mimetype = Connector.getMimeType(path);

                if (mimetype.length() > 0) process.response.setHeaderField("Content-Type", mimetype);

                if (!headonly) {
                
                    input = store.getResourceContent(path);
                    
                    try {
                        
                        bytes = new byte[process.blocksize < 1 ? 65535 : process.blocksize];

                        while ((size = input.read(bytes, 0, bytes.length)) >= 0) {
                        
                            process.response.write(bytes, 0, size);
                        }

                    } finally {

                        input.close();
                        
                        process.response.flush();
                        process.response.close();
                    }
                }
            }
            
        } else if (!headonly && store.isFolder(path)) {
        
            //die Option INDEX ON wird ueberprueft
            control = Connector.cleanOptions(process.options.get("index")).toLowerCase().equals("on");
            
            if (control) process.response.write(Connector.getDirectory(process, store));
            else Connector.forceStatus(process, Status.FORBIDDEN);

        } else if (!store.existsObject(path)) Connector.forceStatus(process, Status.NOT_FOUND);
    }

    /**
     *  Ermittelt die Metadaten zur im Request angegebene Ressource.
     *  Im Fehlerfall wird ein Multistatus als Fehlerbericht erstellt.
     *  @param  process Process
     *  @param  store   Store
     *  @throws IOException bei fehlerhaftem Zugriff auf Ressourcen oder
     *          Datenstr&ouml;me
     */
    private static void doHead(Process process, Store store) throws IOException {
    
        Connector.doGet(process, store, true);
    }
    
    /**
     *  Erstellt das im Request angegebene Verzeichnis.
     *  Im Fehlerfall wird ein Multistatus als Fehlerbericht erstellt.
     *  @param  process Process
     *  @param  store   Store
     *  @throws IOException bei fehlerhaftem Zugriff auf Ressourcen oder
     *          Datenstr&ouml;me
     */
    private static void doMkcol(Process process, Store store) throws IOException {
    
        String parent;
        String path;
    
        path = Connector.getProcessResourcePath(process);
    
        if (!store.canWrite(path)) {Connector.forceStatus(process, Status.FORBIDDEN); return;}

        if (store.isLocked(path)) {Connector.forceStatus(process, Status.LOCKED); return;}
        
        if (store.isResource(path)) {Connector.forceStatus(process, Status.CONFLICT); return;}        
    
        if (process.request.getContentLength() > 0) {
        
            Connector.forceStatus(process, Status.NOT_IMPLEMENTED);
        
        } else {
            
            parent = Connector.getParentPath(path);
            
            if (!store.lock(path, true)) {Connector.forceStatus(process, Status.LOCKED); return;}

            if (parent != null && store.isFolder(parent)) {
            
                if (!store.existsObject(path)) {

                    store.createFolder(path);

                } else {
                
                    process.response.setHeaderField("Allow", Connector.determineMethodsAllowed(process, store, path));
                    process.response.setStatus(Status.METHOD_NOT_ALLOWED);
                    process.response.setMessage(Status.getMessage(process.response.getStatus()));
                    
                    process.response.flush();
                    process.response.close();
                }
                
            } else Connector.forceStatus(process, Status.CONFLICT);
        }
    }

    /**
     *  L&ouml;scht die angegebene Ressource.
     *  Im Fehlerfall wird ein Multistatus als Fehlerbericht erstellt.
     *  @param  process Process
     *  @param  store   Store
     *  @throws IOException bei fehlerhaftem Zugriff auf Ressourcen oder
     *          Datenstr&ouml;me
     */
    private static void doDelete(Process process, Store store) throws IOException {

        String path;
    
        path = Connector.getProcessResourcePath(process);

        if (!store.canWrite(path)) {Connector.forceStatus(process, Status.FORBIDDEN); return;}                     
        
        if (!store.lock(path, true)) {Connector.forceStatus(process, Status.LOCKED); return;}
        
        Connector.deleteResource(store, path, process);
    }

    /**
     *  Speichert die &uuml;bermittelte Ressource im angegeben Ziel.
     *  Im Fehlerfall wird ein Multistatus als Fehlerbericht erstellt.
     *  @param  process Process
     *  @param  store   Store
     *  @throws IOException bei fehlerhaftem Zugriff auf Ressourcen oder
     *          Datenstr&ouml;me
     */
    private static void doPut(Process process, Store store) throws IOException {

        String parent;
        String path;
    
        path = Connector.getProcessResourcePath(process);

        if (!store.canWrite(path)) {Connector.forceStatus(process, Status.FORBIDDEN); return;}

        if (!store.lock(path, true)) {Connector.forceStatus(process, Status.LOCKED); return;}
        
        parent = Connector.getParentPath(path);
    
        if (parent != null && store.isFolder(parent) && !store.isFolder(path)) {
        
            if (!store.existsObject(path)) {
            
                store.createResource(path);
                
                process.response.setStatus(Status.CREATED);
                process.response.setMessage(Status.getMessage(process.response.getStatus()));
                
            } else {
                
                process.response.setStatus(Status.NO_CONTENT);
                process.response.setMessage(Status.getMessage(process.response.getStatus()));
            }
            
            store.setResourceContent(path, process.request, null, null);
            
            process.response.setHeaderField("Content-Length", String.valueOf(store.getResourceLength(path)));
            
        } else Connector.forceStatus(process, Status.CONFLICT);
    }
    
    /**
     *  Kopiert die im Request angegebene Ressource in das entsprechende Ziel.
     *  Im Fehlerfall wird ein Multistatus als Fehlerbericht erstellt.
     *  @param  process Process
     *  @param  store   Store
     *  @throws IOException bei fehlerhaftem Zugriff auf Ressourcen oder
     *          Datenstr&ouml;me
     */
    private static void doCopy(Process process, Store store) throws IOException {

        String path;
    
        path = Connector.getProcessResourcePath(process);

        if (!store.lock(path, false)) {Connector.forceStatus(process, Status.LOCKED); return;}

        Connector.copyResource(process, store);
    }

    /**
     *  Verschieb die im Request angegebene Ressource in das entsprechende Ziel.
     *  Im Fehlerfall wird ein Multistatus als Fehlerbericht erstellt.
     *  @param  process Process
     *  @param  store   Store
     *  @throws IOException bei fehlerhaftem Zugriff auf Ressourcen oder
     *          Datenstr&ouml;me
     */
    private static void doMove(Process process, Store store) throws IOException {
    
        String path;
    
        path = Connector.getProcessResourcePath(process);

        if (!store.canWrite(path)) {Connector.forceStatus(process, Status.FORBIDDEN); return;}

        if (!store.lock(path, true)) {Connector.forceStatus(process, Status.LOCKED); return;}
        
        Connector.moveResource(process, store);
    }
    
    /**
     *  Ermittelt die im Request &uuml;bermittelte Destination. 
     *  @param  process Process
     *  @return die &uuml;bermittelte Destination, sonst <code>null</code>
     */
    private static String findDestination(Process process) {
        
        String absolute;
        String destination;

        int    cursor;

        destination = process.request.getHeaderField("Destination");

        if (destination == null || destination.trim().length() == 0) return null;

        //eventuelle Sonderzeichen werden dekodiert
        destination = Codec.decode(destination, Codec.MIME);
        destination = Codec.decode(destination, Codec.UTF8);

        //beginnt der Zielpfad mit Host-Angaben werden diese entfernt
        cursor = destination.indexOf("://");
        
        if (cursor >= 0) {
        
            cursor = destination.indexOf("/", cursor +4);
            
            destination = (cursor < 0) ? "/" : destination.substring(cursor);
        }
        
        //der Zielpfad wird normalisiert
        destination = Codec.decode(destination, Codec.DOT);

        //der Bezug auf den Request-Pfad wird ermittelt
        absolute = process.environment.get("PATH_ABSOLUTE");
        
        //eventuelle Bezuege auf den Request-Pfad werden entfernt
        if (destination.startsWith(absolute)) destination = destination.substring(absolute.length());
        
        //der Zielpfad wird standarisiert
        if (!destination.startsWith("/")) destination = ("/").concat(destination);
        
        return destination.trim().length() == 0 ?  null : destination;
    }

    /**
     *  Kopiert die im Request angegebenen Ressource.
     *  @param  process Process
     *  @param  store   Store
     *  @return <code>true</code>, wenn die Ressource kopiert wurde
     *  @throws IOException bei fehlerhaftem Zugriff auf Ressourcen
     */
    private static boolean copyResource(Process process, Store store)
        throws IOException {
    
        String destination;
        String source;

        destination = Connector.findDestination(process);

        if (destination == null) {
        
            Connector.forceStatus(process, Status.BAD_REQUEST);
        
            return false;
        }

        source = Connector.getProcessResourcePath(process);

        //ist die Ziel gleich der Quelle wird Status FORBIDDEN gesetzt
        if (source.equals(destination)) {Connector.forceStatus(process, Status.FORBIDDEN); return false;}

        if (!store.canWrite(destination)) {Connector.forceStatus(process, Status.FORBIDDEN); return false;}

        if (!store.lock(destination, true)) {Connector.forceStatus(process, Status.LOCKED); return false;}

        //die Quelle wird auf Existenz geprueft
        if (!store.existsObject(source)) {Connector.forceStatus(process, Status.NOT_FOUND); return false;}
        
        if (process.request.containsHeaderField("Overwrite")
                && process.request.getHeaderField("Overwrite").equalsIgnoreCase("T")
                && store.existsObject(destination)) {

            Connector.forceStatus(process, Status.PRECONDITION_FAILED);

            return false;
        }

        process.response.setStatus(Status.CREATED);
        process.response.setMessage(Status.getMessage(process.response.getStatus()));

        store.copyObject(source, destination, StandardCopyOption.REPLACE_EXISTING);

        return true;
    }
    
    /**
     *  Verschieb die im Request angegebenen Ressource.
     *  @param  process Process
     *  @param  store   Store
     *  @return <code>true</code>, wenn die Ressource verschoben wurde
     *  @throws IOException bei fehlerhaftem Zugriff auf Ressourcen
     */
    private static boolean moveResource(Process process, Store store)
        throws IOException {
    
        String destination;
        String source;

        destination = Connector.findDestination(process);

        if (destination == null) {
        
            Connector.forceStatus(process, Status.BAD_REQUEST);
        
            return false;
        }

        source = Connector.getProcessResourcePath(process);

        //ist die Ziel gleich der Quelle wird Status FORBIDDEN gesetzt
        if (source.equals(destination)) {Connector.forceStatus(process, Status.FORBIDDEN); return false;}

        if (!store.canWrite(destination)) {Connector.forceStatus(process, Status.FORBIDDEN); return false;}

        if (!store.lock(destination, true)) {Connector.forceStatus(process, Status.LOCKED); return false;}

        //die Quelle wird auf Existenz geprueft
        if (!store.existsObject(source)) {Connector.forceStatus(process, Status.NOT_FOUND); return false;}
        
        if (process.request.containsHeaderField("Overwrite")
                && process.request.getHeaderField("Overwrite").equalsIgnoreCase("T")
                && store.existsObject(destination)) {

            Connector.forceStatus(process, Status.PRECONDITION_FAILED);

            return false;
        }

        process.response.setStatus(Status.CREATED);
        process.response.setMessage(Status.getMessage(process.response.getStatus()));

        store.moveObject(source, destination, StandardCopyOption.REPLACE_EXISTING);

        return true;
    }    

    /**
     *  L&ouml;sche die per Pfad angegebene Ressource.
     *  @param  store   Store
     *  @param  path    die zu l&ouml;schende Ressource
     *  @param  process Process
     *  @throws IOException bei fehlerhaftem Zugriff auf Ressourcen oder
     *          Datenstr&ouml;me
     */
    private static void deleteResource(Store store, String path, Process process) throws IOException {

        process.response.setStatus(Status.NO_CONTENT);
        process.response.setMessage(Status.getMessage(process.response.getStatus()));

        if (store.isResource(path)) {

            store.removeObject(path);

        } else {

            if (store.isFolder(path)) {

                Connector.deleteFolder(store, path);

                store.removeObject(path);

            } else Connector.forceStatus(process, Status.NOT_FOUND);
        }
    }

    /**
     *  L&ouml;sche das Verzeichnis mit dem kompletten Inhalt.
     *  @param  store  Store
     *  @param  path   das zu l&ouml;schende Verzeichnis
     *  @throws IOException bei fehlerhaftem Zugriff auf Ressourcen oder
     *          Datenstr&ouml;me
     */
    private static void deleteFolder(Store store, String path) throws IOException {

        String   source;

        String[] children;

        int      loop;

        children = store.getChildrenNames(path);

        for (loop = children.length -1; loop >= 0; loop--) {

            source = path.concat("/").concat(children[loop]);

            if (store.isResource(source)) {

                store.removeObject(source);

            } else {

                Connector.deleteFolder(store, source);

                store.removeObject(source);
            }
        }
    }

    /**
     *  Maskiert im &uuml;bergeben String f&uuml;r die Ausgabe relevante Zeichen.
     *  @param  string zu maskierender String
     *  @return der maskierte String
     */
    private static String escapeOutput(String string) {

        string = Components.strset(string, "&", "%26");
        string = Components.strset(string, "+", "%2B");

        return string;
    }

    /**
     *  Ermittelt die mit per PROPFIND angeforderten Eigenschaften, l&ouml;st
     *  diese auf und &uuml;bermittelt diese in den &uuml;bergebenen
     *  XML-Datenstrom zum Client.
     *  @param  process    Process
     *  @param  xml        XML Datenstrom
     *  @param  store      Store
     *  @param  path       Pfad der Ressource
     *  @param  type       PROPFIND Typ
     *  @param  properties gefundene Eigenschaften
     *  @throws IOException bei fehlerhaftem Zugriff auf den Store
     */
    private static void parseProperties(Process process, Stream xml,
        Store store, String path, int type, Properties properties) throws IOException {

        Iterator<String> iterator;
        List<String>     list;
        String           creation;
        String           length;
        String           modified;
        String           resource;
        String           status;
        String           string;
        String           space;
        
        boolean          folder;

        int              cursor;
        int              bits;
        
        space = Connector.DEFAULT_XML_NAMESPACE;

        creation = Connector.formatDate("yyyy-MM-dd'T'HH:mm:ss'Z'", store.getCreationTime(path), "GMT");
        modified = Connector.formatDate("E, dd MMM yyyy HH:mm:ss z", store.getLastModified(path), "GMT");
        folder   = store.isFolder(path);
        length   = String.valueOf(store.getResourceLength(path));
        status   = Connector.generateStatusSignature(Status.SUCCESS);
        resource = process.environment.get("path_absolute");
        
        //Win32FileAttributes
        //siehe http://msdn.microsoft.com/en-us/library/windows/desktop/aa365535%28v=vs.85%29.aspx
        bits  = store.isReadOnly(path) ? 0x01 : 0;
        bits |= store.isHidden(path) ? 0x02 : 0;
        bits |= store.isSystem(path) ? 0x04 : 0;                
        bits |= store.isArchive(path) ? 0x20 : 0;

        if (!resource.startsWith("/")) resource = ("/").concat(resource);
        if (!resource.endsWith("/") && !path.startsWith("/")) resource = resource.concat("/");
        if (resource.endsWith("/") && path.startsWith("/")) resource = resource.substring(0, resource.length() -1);

        resource = resource.concat(path);

        xml.writeElement(space, "response", Stream.OPENING);
        xml.writeProperty(space, "href", Connector.escapeOutput(Connector.rewriteUrl(resource)));

        if ((cursor = (resource = path).lastIndexOf('/')) >= 0) resource = resource.substring(cursor + 1);

        resource = Connector.escapeOutput(resource);

        switch (type) {

            case Connector.FIND_ALL_PROP:

                xml.writeElement(space, "propstat", Stream.OPENING);
                xml.writeElement(space, "prop", Stream.OPENING);
                xml.writeProperty(space, "creationdate", creation);
                xml.writePropertyData(space, "displayname", resource);
                xml.writeProperty(space, "iscollection", folder ? "true" : "false");
                xml.writeProperty(space, "Win32FileAttributes", Integer.toHexString(bits));

                xml.writeProperty(space, "isreadonly", (bits & 0x01) == 0 ? "false" : "true");
                xml.writeProperty(space, "ishidden", (bits & 0x02) == 0 ? "false" : "true");
                xml.writeProperty(space, "issystem", (bits & 0x04) == 0 ? "false" : "true");
                xml.writeProperty(space, "isarchive", (bits & 0x20) == 0 ? "false" : "true");

                if (folder) {

                    xml.writeElement(space, "resourcetype", Stream.OPENING);
                    xml.writeElement(space, "collection", Stream.EMPTY);
                    xml.writeProperty(space, "getetag", store.getETag(path));
                    xml.writeElement(space, "resourcetype", Stream.CLOSING);

                } else {

                    xml.writeProperty(space, "getlastmodified", modified);
                    xml.writeProperty(space, "getcontentlength", length);
                    xml.writeProperty(space, "getetag", store.getETag(path));
                    xml.writeElement(space, "resourcetype", Stream.EMPTY);

                    string = Connector.getMimeType(path);

                    if (string.length() > 0) xml.writeProperty(space, "getcontenttype", string);
                }

                xml.writeProperty(space, "source", "");
                xml.writeElement(space, "prop", Stream.CLOSING);
                xml.writeProperty(space, "status", status);
                xml.writeElement(space, "propstat", Stream.CLOSING);

                break;

            case Connector.FIND_PROPERTY_NAMES:

                xml.writeElement(space, "propstat", Stream.OPENING);
                xml.writeElement(space, "prop", Stream.OPENING);
                xml.writeElement(space, "creationdate", Stream.EMPTY);
                xml.writeElement(space, "displayname", Stream.EMPTY);

                if (!folder) {

                    xml.writeElement(space, "getcontentlanguage", Stream.EMPTY);
                    xml.writeElement(space, "getcontentlength", Stream.EMPTY);
                    xml.writeElement(space, "getcontenttype", Stream.EMPTY);
                }

                xml.writeElement(space, "getetag", Stream.EMPTY);
                xml.writeElement(space, "getlastmodified", Stream.EMPTY);
                xml.writeElement(space, "iscollection", Stream.EMPTY);
                
                xml.writeElement(space, "isreadonly", Stream.EMPTY);
                xml.writeElement(space, "ishidden", Stream.EMPTY);
                xml.writeElement(space, "issystem", Stream.EMPTY);
                xml.writeElement(space, "isarchive", Stream.EMPTY);
                
                xml.writeElement(space, "Win32FileAttributes", Stream.EMPTY);

                xml.writeElement(space, "resourcetype", Stream.EMPTY);
                xml.writeElement(space, "source", Stream.EMPTY);
                xml.writeElement(space, "lockdiscovery", Stream.EMPTY);
                xml.writeElement(space, "prop", Stream.CLOSING);
                xml.writeProperty(space, "status", status);
                xml.writeElement(space, "propstat", Stream.CLOSING);

                break;

            case Connector.FIND_BY_PROPERTY:

                list = new ArrayList<String>();

                xml.writeElement(space, "propstat", Stream.OPENING);
                xml.writeElement(space, "prop", Stream.OPENING);
                
                for (String property : properties.keySet()) {

                    if (folder) {

                        if (property.equals("creationdate")) {
                            xml.writeProperty(space, "creationdate", creation);
                        } else if (property.equals("displayname")) {
                            xml.writePropertyData(space, "displayname", resource);
                        } else if (property.equals("getcontentlanguage")) {
                            list.add(property);
                        } else if (property.equals("getcontentlength")) {
                            list.add(property);
                        } else if (property.equals("getcontenttype")) {
                            list.add(property);
                        } else if (property.equals("getetag")) {
                            list.add(property);
                        } else if (property.equals("getlastmodified")) {
                            xml.writeProperty(space, "getlastmodified", modified);
                        } else if (property.equals("resourcetype")) {
                            xml.writeElement(space, "resourcetype", Stream.OPENING);
                            xml.writeElement(space, "collection", Stream.EMPTY);
                            xml.writeElement(space, "resourcetype", Stream.CLOSING);
                        } else if (property.equals("source")) {
                            xml.writeProperty(space, "source", "");
                        } else if (property.equals("iscollection")) {
                            xml.writeProperty(space, "iscollection", "true");
                        } else if (property.equals("isreadonly")) {
                            xml.writeProperty(space, "isreadonly", (bits & 0x01) == 0 ? "false" : "true");
                        } else if (property.equals("ishidden")) {
                            xml.writeProperty(space, "ishidden", (bits & 0x02) == 0 ? "false" : "true");
                        } else if (property.equals("issystem")) {
                            xml.writeProperty(space, "issystem", (bits & 0x04) == 0 ? "false" : "true");
                        } else if (property.equals("isarchive")) {
                            xml.writeProperty(space, "isarchive", (bits & 0x20) == 0 ? "false" : "true");
                        } else if (property.equals("Win32FileAttributes")) {
                            xml.writeProperty(space, "Win32FileAttributes", Integer.toHexString(bits));
                        } else {
                            list.add(property);
                        }

                    } else {

                        if (property.equals("creationdate")) {
                            xml.writeProperty(space, "creationdate", creation);
                        } else if (property.equals("displayname")) {
                            xml.writePropertyData(space, "displayname", resource);
                        } else if (property.equals("getcontentlanguage")) {
                            xml.writeElement(space, "getcontentlanguage", Stream.EMPTY);
                        } else if (property.equals("getcontentlength")) {
                            xml.writeProperty(space, "getcontentlength", length);
                        } else if (property.equals("getcontenttype")) {
                            xml.writeProperty(space, "getcontenttype", Connector.getMimeType(path));
                        } else if (property.equals("getetag")) {
                            xml.writeProperty(space, "getetag", store.getETag(path));
                        } else if (property.equals("getlastmodified")) {
                            xml.writeProperty(space, "getlastmodified", modified);
                        } else if (property.equals("resourcetype")) {
                            xml.writeElement(space, "resourcetype", Stream.EMPTY);
                        } else if (property.equals("source")) {
                            xml.writeProperty(space, "source", "");
                        } else if (property.equals("iscollection")) {
                            xml.writeProperty(space, "iscollection", "false");
                        } else if (property.equals("isreadonly")) {
                            xml.writeProperty(space, "isreadonly", (bits & 0x01) == 0 ? "false" : "true");
                        } else if (property.equals("ishidden")) {
                            xml.writeProperty(space, "ishidden", (bits & 0x02) == 0 ? "false" : "true");
                        } else if (property.equals("issystem")) {
                            xml.writeProperty(space, "issystem", (bits & 0x04) == 0 ? "false" : "true");
                        } else if (property.equals("isarchive")) {
                            xml.writeProperty(space, "isarchive", (bits & 0x20) == 0 ? "false" : "true");
                        } else if (property.equals("Win32FileAttributes")) {
                            xml.writeProperty(space, "Win32FileAttributes", Integer.toHexString(bits));
                        } else {
                            list.add(property);
                        }
                    }
                }

                xml.writeElement(space, "prop", Stream.CLOSING);
                xml.writeProperty(space, "status", status);
                xml.writeElement(space, "propstat", Stream.CLOSING);

                iterator = list.iterator();

                if (iterator.hasNext()) {

                    status = Connector.generateStatusSignature(Status.NOT_FOUND);

                    xml.writeElement(space, "propstat", Stream.OPENING);
                    xml.writeElement(space, "prop", Stream.OPENING);

                    while (iterator.hasNext()) {

                        xml.writeElement(null, iterator.next(), Stream.EMPTY);
                    }

                    xml.writeElement(space, "prop", Stream.CLOSING);
                    xml.writeProperty(space, "status", status);
                    xml.writeElement(space, "propstat", Stream.CLOSING);
                }

                break;
        }

        xml.writeElement(space, "response", Stream.CLOSING);
    }

    /**
     *  R&uuml;ckgabe der f&uuml;r die Ressource zul&auml;ssigen Methoden.
     *  @param  process Process
     *  @param  store   Store
     *  @param  uri     Pfad als URI
     *  @return die f&uuml;r die Ressource zul&auml;ssigen Methoden
     *  @throws IOException bei fehlerhaftem Zugriff auf die Methode
     */
    private static String determineMethodsAllowed(Process process, Store store,
        String uri) throws IOException {

        String  methods;
        String  path;

        boolean readonly;

        path = Connector.getProcessResourcePath(process);

        readonly = !store.canWrite(path);

        methods  = "OPTIONS, GET, HEAD, PROPFIND";

        if (!readonly) methods = methods.concat(", DELETE, PROPPATCH, COPY, MOVE");

        if (store.existsObject(uri)) {

            if (!readonly && store.isFolder(uri)) methods = methods.concat(", PUT");

            return methods;
        }

        methods = "OPTIONS";

        return (readonly) ? methods : methods.concat(", PUT, MKCOL");
    }
    
    /**
     *  Sendet einen Server-Status an den Client.
     *  @param  process Process
     *  @param  status  Status
     *  @throws IOException bei fehlerhaftem Zugriff auf die Datenstr&ouml;me
     */
    private static void forceStatus(Process process, int status)
        throws IOException {
        
        process.response.sendStatus(status);
    }   

    /**
     *  Sendet eine Weiterleitung an den Client.
     *  @param  process Process
     *  @param  url     Adresse der Weiterleitung
     *  @param  query   optional QueryString der Weiterleitung 
     *  @throws IOException bei fehlerhaftem Zugriff auf die Datenstr&ouml;me
     */
    private static void forceRedirect(Process process, String url, String query)
        throws IOException {
        
        if (query != null && query.length() > 0) url = url.concat("?").concat(query);
        
        process.response.sendRedirect(url);
    }    

    /**
     *  Einsprung des Service zum beenden des Moduls.
     *  @throws Exception allgemein auftretende nicht behandelte Fehler
     */
    public void destroy() throws Exception {

        if (this.store != null) this.store.close();
    }

    /**
     *  Einsprung zur Anbindung von Process im HTTP Context.
     *  Treten bei der Verarbeitung unerwartet Fehler auf, werden diese
     *  weitergereicht und f&uuml;hren beim Server zum Serverstatus
     *  <code>503 (INTERNAL SERVER ERROR)</code>.
     *  @param  process Prozess der HTTP Anbindung
     *  @throws Exception allgemeine nicht durch die Anwendung tolerierbare
     *          Laufzeitfehler
     */
    public void service(Process process) throws Exception {

        Store   store;
        String  method;
        String  context;
        String  path;
        String  query;
        String  info;
        
        boolean force;

        process.response.setProtocol("HTTP/1.0");
        process.response.setHeaderField("Connection", "close");
        process.response.setStatus(Status.SUCCESS);
        process.response.setMessage(Status.getMessage(process.response.getStatus()));
        
        //die Methode wird ermittelt
        method = process.request.getMethod().toUpperCase();
        force  = method.equals(Connector.METHOD_GET) || method.equals(Connector.METHOD_HEAD); 
        
        //der Archiveintrag wird ermittelt
        context = process.environment.get("path_absolute");
        path    = process.environment.get("path_info");
        query   = process.request.getQueryString();
        
        info = context.endsWith("/") ? path = ("/").concat(path) : path;
        
        //ggf. endende Slashes werden entfernt
        while (context.endsWith("/")) context = context.substring(0, context.length() -1);
        
        //endende Slashes werden entfernt
        while (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() -1);
        
        //ohne Pfad erfolgt eine Weiterleitung zur Wurzel vom Archiv 
        if (force && path.length() == 0) {Connector.forceRedirect(process, context.concat("/"), query); return;}
        
        if (force && !path.startsWith("/")) {Connector.forceStatus(process, Status.NOT_FOUND); return;}

        store = this.store.share(null, this.parameters);

        //ggf. Weiterleitung wenn Verzeichnis ohne endendem Slash
        if (force && store != null && store.isFolder(path) && !info.endsWith("/")) {Connector.forceRedirect(process, context.concat(path).concat("/"), query); return;}
        
        //ggf. Weiterleitung wenn Datei mit endendem Slash
        if (force && store != null && store.isResource(path) && info.endsWith("/")) {Connector.forceRedirect(process, context.concat(path.substring(0, path.length() -1)), query); return;}
        
        try {

            if (method.equals(Connector.METHOD_PROPFIND)) {
                Connector.doPropfind(process, store);
            } else if (method.equals(Connector.METHOD_PROPPATCH)) {
                Connector.doProppatch(process, store);
            } else if (method.equals(Connector.METHOD_MKCOL)) {
                Connector.doMkcol(process, store);
            } else if (method.equals(Connector.METHOD_COPY)) {
                Connector.doCopy(process, store);
            } else if (method.equals(Connector.METHOD_MOVE)) {
                Connector.doMove(process, store);
            } else if (method.equals(Connector.METHOD_PUT)) {
                Connector.doPut(process, store);
            } else if (method.equals(Connector.METHOD_GET)) {
                Connector.doGet(process, store);
            } else if (method.equals(Connector.METHOD_OPTIONS)) {
                Connector.doOptions(process, store);
            } else if (method.equals(Connector.METHOD_HEAD)) {
                Connector.doHead(process, store);
            } else if (method.equals(Connector.METHOD_DELETE)) {
                Connector.doDelete(process, store);
            } else {
                
                process.response.setStatus(Status.NOT_IMPLEMENTED);
                process.response.setMessage(Status.getMessage(process.response.getStatus()));
            }

            store.commit();

        } catch (Exception exception) {
            
            try {store.rollback();
            } catch (Throwable throwable) {
                
                //keine Fehlerbehandlung vorgesehen
            }
            
            //einfache Socketabbrueche werden als Fehler unterdrueckt
            for (StackTraceElement element : exception.getStackTrace()) {

                if (("java.net.SocketOutputStream").equals(element.getClassName())) return;
            }
        
            throw exception;
            
        } finally {
            
            if (!process.response.isCommitted()) process.response.flush();

            try {store.close();
            } catch (Throwable throwable) {
                
                //keine Fehlerbehandlung vorgesehen
            }            
        }
    }
}