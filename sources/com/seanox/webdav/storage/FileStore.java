/**
 *  LIZENZBEDINGUNGEN - Seanox Software Solutions ist ein Open-Source-Projekt,
 *  im Folgenden Seanox Software Solutions oder kurz Seanox genannt.
 *  Diese Software unterliegt der Version 2 der GNU General Public License.
 *
 *  WebDAV, Advanced Server Developing
 *  Copyright (C) 2013 Seanox Software Solutions
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
package com.seanox.webdav.storage;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.security.Principal;
import java.util.Map;

/**
 *  FileStore stellt einem Store f&uuml;r das lokale Dateisystem bereit.
 *  Transaktionen werden nicht unterst&uuml;tzt.<br>
 *  <br>
 *  Konfiguration:<br>
 *  <br>
 *  <table>
 *    <tr>
 *      <td>Parameter</td>
 *      <td>Beschreibung</td>
 *    </tr>
 *    <tr>
 *      <td><i>context</i></td>
 *      <td>
 *        Name vom Context im Universum vom Modul-Manager, ist zur Tennung der
 *        verwendeten Application-ClassLoader erforderlich
 *      </td>
 *    </tr>
 *    <tr>
 *      <td><i>class</i></td>
 *      <td>Klasse vom WebDAV-Connector (com.seanox.webdav.Connector)</td>
 *    </tr>
 *    <tr>
 *      <td><i>storage</i></td>
 *      <td>Klasse vom Store (com.seanox.webdav.storage.FileStore)</td>
 *    </tr>
 *    <tr>
 *      <td><i>root</i></td>
 *      <td>
 *        Wurzelverzeichnis im Store, vergleichbar mit <code>docroot</code> aus
 *        der Server-Konfiguration
 *      </td>
 *    </tr>
 *    <tr>
 *      <td><i>readonly</i></td>
 *      <td>
 *        option f&uuml;r den ausschliesslich lesenden Zugriff, f&uuml;r den
 *        schreibenden Zugriff muss <code>readonly</code> bewusst auf
 *        <code>off</code> gesetzt werden, Standard, wenn nicht angegeben ist
 *        <code>on</code>
 *      </td>
 *    </tr>
 *    <tr>
 *      <td><i>blocksize</i></td>
 *      <td>
 *        optionale Angabe f&uuml;r den Store, welche die Gr&ouml;sse der
 *        Datenbl&ouml;cke beim Datenzugriff angibt, als Standard, wenn nicht
 *        angegeben, wird der Wert aus der Server-Konfiguration verwendet
 *      </td>
 *    </tr>    
 *  </table>
 *  <br>
 *  Beispiel:
 *  <pre>
 *  [SERVER:HTTP:BAS]
 *    ...
 *    METHODS = OPTIONS HEAD GET POST GATEWAY
 *
 *  [SERVER:HTTP:REF]
 *    EXAMPLE = /example/ &gt; com.seanox.module.http.Context
 *            + [context:webdav-1]
 *            + [class:com.seanox.webdav.Connector]
 *            + [storage:com.seanox.webdav.storage.FileStore]
 *            + [root:/document] [readonly:off] [M]
 *  </pre>
 *  FileStore 1.2013.0512<br>
 *  Copyright (C) 2013 Seanox Software Solutions<br>
 *  Alle Rechte vorbehalten.
 *
 *  @author  Seanox Software Solutions
 *  @version 1.2013.0512
 */
public class FileStore extends AbstractStore {

    /**
     *  Erstellt eine Instanz vom zu verwendenden FileSystem auf Basis der
     *  &uuml;bergeben Berechtigungn und Parameter.
     *  @param  principal  Principal (wird ignoriert)
     *  @param  parameters Parameter
     *  @return die Instanz vom zu verwendenden FileSystem 
     *  @throws Exception bei fehlerhafter Einrichtung des Stores
     */    
    protected FileSystem getFileSystem(Principal principal, Map<String, String> parameters) throws Exception {

        return FileSystems.getDefault();
    }
}