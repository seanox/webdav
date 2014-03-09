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
package com.seanox.webdav.storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.seanox.common.Codec;
import com.seanox.webdav.storage.lock.Manager;

/**
 *  Abstrakte Klasse zur Implementierung eines Stores, welcher Methoden
 *  f&uuml;r den Zugriff auf Datenquellen kapselt.<br>
 *  <br>
 *  AbstractStore 1.2014.0202<br>
 *  Copyright (C) 2014 Seanox Software Solutions<br>
 *  Alle Rechte vorbehalten.
 *
 *  @author  Seanox Software Solutions
 *  @version 1.2014.0202
 */
public abstract class AbstractStore implements Store {

    /**  zu verwendendes FileSystem */
    protected volatile FileSystem system;

    /** &uuml;bergeordnete Instanz vom (Share)Store */
    protected volatile Store store;
    
    /** gemeinsamer Manager zur Verwaltung der Sperreintr&auml;ge (global) */
    protected volatile Manager locks;
    
    /** Wurzelverzeichnis des Stores */
    protected volatile String root;
    
    /** Principals (nicht verwendet) */
    protected volatile Principal principal;
    
    /** Konfiguration vom Store */
    protected volatile Map<String, String> parameters;
    
    /** Set der erstellten Shares (global) */
    protected volatile Set<String> shares;
    
    /** eindeutige Transaktionsnummer */
    protected volatile String signature;
    
    /** Option wenn der Store geschlossen wurde */
    protected volatile boolean closed;
    
    /** Option wenn auf den Store nur lesend zugegriffen werden darf */
    protected volatile boolean readonly;  
    
    /** Gr&ouml;sse des Datenpuffers f&uuml;r Datenzugriffe */
    protected volatile int blocksize;
    
    /** Constant for parameter STORAGE */
    public static final String PARAMETER_STORAGE = "STORAGE";
    
    /** Constant for parameter FILESYSTEM */
    public static final String PARAMETER_FILESYSTEM = "FILESYSTEM";
    
    /** Constant for parameter ROOT */
    public static final String PARAMETER_ROOT = "ROOT";
    
    /** Constant for parameter READONLY */
    public static final String PARAMETER_READONLY = "READONLY";

    /** Constant for parameter BLOCKSIZE */
    public static final String PARAMTER_BLOCKSIZE = "BLOCKSIZE";
    
    /** Constant for file attribute LastModifiedTime */
    public static final String FILE_ATTRIBUTE_LAST_MODIFIED = "lastModifiedTime";
    
    /** Constant for file attribute CreationTime */
    public static final String FILE_ATTRIBUTE_CREATION_TIME = "creationTime";
    
    /** Constant for file attribute LastAccessTime */    
    public static final String FILE_ATTRIBUTE_LAST_ACCESS_TIME = "lastAccessTime";
    
    /** Constant for file attribute ReadOnly */
    public static final String FILE_ATTRIBUTE_READONLY = "dos:readonly";
    
    /** Constant for file attribute Hidden */
    public static final String FILE_ATTRIBUTE_HIDDEN = "dos:hidden";
    
    /** Constant for file attribute System */
    public static final String FILE_ATTRIBUTE_SYSTEM = "dos:system";
    
    /** Constant for file attribute Archive */
    public static final String FILE_ATTRIBUTE_ARCHIVE = "dos:archive";

    /**
     *  Erstellt eine Kopie der Instanz f&uuml;r Transaktionen auf Basis der
     *  &uuml;bergeben Berechtigung und Parameter.
     *  @param  principal  Principal
     *  @param  parameters Parameter
     *  @return eine eigene Instanz f&uuml;r Transaktionen
     *  @throws Exception bei fehlerhafter Einrichtung des Stores
     */    
    public Store share(Principal principal, Map<String, String> parameters) throws Exception {

        AbstractStore store;
        String        string;

        if (this.closed) throw new IOException("Store already closed");
        
        synchronized (this) {

            if (this.store  == null) this.store  = this;
            if (this.shares == null) this.shares = Collections.synchronizedSet(new HashSet<String>());
            if (this.locks  == null) this.locks  = new Manager();
            if (this.system == null) this.system = this.getFileSystem(principal, parameters);
        }
        
        store = (AbstractStore)super.clone();

        store.signature  = UUID.randomUUID().toString();
        store.principal  = principal;
        store.parameters = parameters;
        
        this.shares.add(store.signature);
        
        store.system = this.system;
        store.store  = this.store;
        store.shares = this.shares;
        store.locks  = this.locks;
    
        string = parameters.get(AbstractStore.PARAMETER_READONLY);
    
        store.readonly = string == null || !string.equalsIgnoreCase("off");
    
        string = parameters.get(AbstractStore.PARAMTER_BLOCKSIZE);
    
        try {store.blocksize = Integer.valueOf(string).intValue();
        } catch (Exception exception) {

            //keine Fehlerbehandlung vorgesehen
        }

        if (store.blocksize <= 0) store.blocksize = 65535;
        
        store.root = Codec.decode(parameters.get(AbstractStore.PARAMETER_ROOT), Codec.DOT);
        
        //Slashes am Ende werden entfernt, bis auf das letzte
        while (store.root.endsWith("/") && store.root.length() > 1) store.root = store.root.substring(0, store.root.length() -1);
        
        if (store.system.equals(FileSystems.getDefault())) store.root = store.system.getPath(store.root).toAbsolutePath().toString();

        if (!store.existsObject("/")) throw new IOException("Root directory not exist");

        return store;
    }    
    
    /**
     *  Erstellt eine Instanz vom zu verwendenden FileSystem auf Basis der
     *  &uuml;bergeben Berechtigungn und Parameter.
     *  @param  principal  Principal (wird ignoriert)
     *  @param  parameters Parameter
     *  @return die Instanz vom zu verwendenden FileSystem 
     *  @throws Exception bei fehlerhafter Einrichtung des Stores
     */    
    protected abstract FileSystem getFileSystem(Principal principal, Map<String, String> parameters) throws Exception;
    
    /**
     *  Erstellt ein ETag f&uuml;r die per URI angegebenen Ressource.
     *  @param  uri Pfad der Ressource als URI
     *  @return der ermittelte ETag f&uuml;r die per URI angegebenen Ressource
     *  @throws IOException bei fehlerhaftem Zugriff auf den Store
     */
    public synchronized String getETag(String uri) throws IOException {
    
        StringBuffer string;
    
        if (this.closed) throw new IOException("Store already closed");
    
        string = new StringBuffer();
    
        string.append("W/\"");
        string.append(getResourceLength(uri));
        string.append(getLastModified(uri));
        string.append("\"");

        return string.toString();
    }    
    
    /**
     *  R&uuml;ckgabe <code>true</code> wenn auf Ressourcen nur lesemd
     *  zugeriffen werden kann., sonst <code>false</code>.
     *  @return <code>true</code> wenn auf Ressourcen nicht schreibend
     *          zugeriffen werden kann., sonst <code>false</code>
     *  @throws IOException bei fehlerhaftem Zugriff auf den Store
     */
    public synchronized boolean isReadOnly() throws IOException {
        
        if (this.closed) throw new IOException("Store already closed");

        return this.readonly;
    }
    
    /**
     *  R&uuml;ckgabe <code>true</code> wenn auf die per URI angegebenen
     *  Ressource nur lesemd zugeriffen werden kann., sonst <code>false</code>.
     *  @return <code>true</code> wenn auf die Ressource nicht schreibend
     *          zugeriffen werden kann., sonst <code>false</code>
     *  @throws IOException bei fehlerhaftem Zugriff auf den Store
     */
    public synchronized boolean isReadOnly(String uri) throws IOException {
        
        Path              path;        
        DosFileAttributes attributes;
        
        if (this.closed) throw new IOException("Store already closed");

        if (this.readonly) return true;
        
        path = this.system.getPath(this.root, uri);
        
        attributes = Files.readAttributes(path, DosFileAttributes.class);
        
        return attributes == null ? false : attributes.isReadOnly();
    }    
    
    /**
     *  Setzt das Attribut ReadOnly bei der per URI angegebene Ressource
     *  @param  uri      Pfad der Ressource als URI
     *  @param  readOnly Wert vom Attribut ReadOnly
     *  @throws IOException bei fehlerhaftem Zugriff auf das Dateisystem
     */
    public void setReadOnly(String uri, boolean readOnly) throws IOException {
        
        Path              path;
        DosFileAttributes attributes;
        
        if (this.closed) throw new IOException("Store already closed");
        
        if (this.readonly) throw new IOException("Write access forbidden");
        
        path = this.system.getPath(this.root, uri);
        
        attributes = Files.readAttributes(path, DosFileAttributes.class);
        
        if (attributes == null) throw new IOException("Method not supported.");
        
        Files.setAttribute(path, FILE_ATTRIBUTE_READONLY, Boolean.valueOf(readOnly));
    }    
    
    /**
     *  Setzt f&uuml;r diese Instanz vom Store eine Sperre f&uuml;r die per URI
     *  angegebene Ressource. R&uuml;ckgabe <code>true</code>, wenn die Sperrung
     *  erfolgreich eingetragen wurde, <code>false</code> wenn eine bereits
     *  gesetzte Sperre die Sperrung verhindert. Mit dieser Instanz vom Store
     *  gesetzte Sperren werden ignoriert, da diese als eine Transaktion
     *  betrachtet werden.
     *  @param  uri Pfad der Ressource als URI
     *  @return <code>true</code> bei erfolgreich eingetragener Sperrung
     *  @throws IOException bei fehlerhaftem Zugriff auf den Store
     */
    public synchronized boolean lock(String uri) throws IOException {
        
        if (this.closed) throw new IOException("Store already closed");

        return this.locks.lock(uri, this.signature);
    }

    /**
     *  Setzt f&uuml;r diese Instanz vom Store eine Sperre f&uuml;r die per URI
     *  angegebene Ressource. R&uuml;ckgabe <code>true</code>, wenn die Sperrung
     *  erfolgreich eingetragen wurde, <code>false</code> wenn eine bereits
     *  gesetzte Sperre die Sperrung verhindert. Mit dieser Instanz vom Store
     *  gesetzte Sperren werden ignoriert, da diese als eine Transaktion
     *  betrachtet werden.
     *  @param  uri       Pfad der Ressource als URI
     *  @param  exclusive <code>true</code> bei exklusiver Sperrung
     *  @return <code>true</code> bei erfolgreich eingetragener Sperrung
     *  @throws IOException bei fehlerhaftem Zugriff auf den Store
     */
    public synchronized boolean lock(String uri, boolean exclusive) throws IOException {
        
        if (this.closed) throw new IOException("Store already closed");

        return this.locks.lock(uri, this.signature, exclusive);
    }

    /**
     *  Hebt die unter dieser Instanz vom Store eingerichtete Sperre f&uuml;r
     *  die URI angegebene Ressource auf.
     *  @param  uri Pfad der Ressource als URI
     *  @throws IOException bei fehlerhaftem Zugriff auf den Store
     */
    public synchronized void unlock(String uri) throws IOException {
        
        if (this.closed) throw new IOException("Store already closed");
        
        this.locks.unlock(uri, this.signature);
    }

    /**
     *  R&uuml;ckgabe <code>true</code> wenn die bei per URI angegebene
     *  Ressource gesperrt ist. Mit dieser Instanz vom Store gesetzte Sperren
     *  werden ignoriert, da diese als eine Transaktion betrachtet werden. 
     *  @param  uri Pfad der Ressource als URI
     *  @return <code>true</code> bei per URI angegebener gesperrter Ressource
     *  @throws IOException bei fehlerhaftem Zugriff auf den Store
     */
    public synchronized boolean isLocked(String uri) throws IOException {
        
        if (this.closed) throw new IOException("Store already closed");

        return this.locks.isLocked(uri, this.signature);
    }

    /**
     *  Pr&uuml;ft die Autorisierung beim Zugriff auf das Dateisystem.
     *  Diese Funktion wird derzeit noch nicht unterst&uuml;tzt.
     *  @throws SecurityException bei fehlerhafter Autorisierung
     *  @throws IOException bei fehlerhaftem Zugriff auf das Dateisystem
     */
    public void checkAuthentication() throws SecurityException, IOException {
    
        if (this.closed) throw new IOException("Store already closed");
    }

    /**
     *  R&uuml;ckgabe <code>true</code> wenn die per URI angegebenen Ressource
     *  existiert, sonst <code>false</code>.
     *  @param  uri Pfad der Ressource als URI
     *  @return <code>true</code> wenn die per URI angegebenen Ressource
     *          existiert
     *  @throws IOException bei fehlerhaftem Zugriff auf das Dateisystem
     */
    public boolean existsObject(String uri) throws IOException {
        
        Path path;
    
        if (this.closed) throw new IOException("Store already closed");
        
        path = this.system.getPath(this.root, uri);
        
        return Files.exists(path);
    }    

    /**
     *  R&uuml;ckgabe <code>true</code> wenn es sich bei der per URI angegebenen
     *  Ressource um ein Verzeichnis handelt, sonst <code>false</code>.
     *  @param  uri Pfad der Ressource als URI
     *  @return <code>true</code> wenn sich bei der per URI angegebenen
     *          Ressource um ein Verzeichnis handelt, sonst <code>false</code>
     *  @throws IOException bei fehlerhaftem Zugriff auf das Dateisystem
     */
    public boolean isFolder(String uri) throws IOException {
        
        Path path;
        
        if (this.closed) throw new IOException("Store already closed");
        
        path = this.system.getPath(this.root, uri);
        
        return Files.isDirectory(path);
    }

    /**
     *  R&uuml;ckgabe <code>true</code> wenn sich bei der per URI angegebenen
     *  Ressource um eine Datei handelt, sonst <code>false</code>.
     *  @param  uri Pfad der Ressource als URI
     *  @return <code>true</code> wenn sich bei der per URI angegebenen
     *          Ressource um eine Datei handelt, sonst <code>false</code>
     *  @throws IOException bei fehlerhaftem Zugriff auf das Dateisystem
     */
    public boolean isResource(String uri) throws IOException {
        
        Path path;
        
        if (this.closed) throw new IOException("Store already closed");
        
        path = this.system.getPath(this.root, uri);
        
        return Files.isRegularFile(path);        
    }

    /**
     *  R&uuml;ckgabe <code>true</code> wenn auf die per URI angegebene
     *  Ressource lesend zugegriffern werden kann, sonst <code>false</code>.
     *  @param  uri Pfad der Ressource als URI
     *  @return <code>true</code> wenn auf die per URI angegebene Ressource
     *          lesend zugegriffern werden kann, sonst <code>false</code>
     *  @throws IOException bei fehlerhaftem Zugriff auf das Dateisystem
     */
    public boolean canRead(String uri) throws IOException {
        
        Path path;
        
        if (this.closed) throw new IOException("Store already closed");
        
        path = this.system.getPath(this.root, uri);
        
        if (!this.existsObject(uri)) path = path.getParent();
        
        return this.existsObject(uri) && Files.isReadable(path);
    }

    /**
     *  R&uuml;ckgabe <code>true</code> wenn auf die per URI angegebenen
     *  Ressource schreibend zugegriffern werden kann, sonst <code>false</code>.
     *  @param  uri Pfad der Ressource als URI
     *  @return <code>true</code> wenn auf die per URI angegebenen Ressource
     *          schreibend zugegriffern werden kann, sonst <code>false</code>
     *  @throws IOException bei fehlerhaftem Zugriff auf das Dateisystem
     */
    public boolean canWrite(String uri) throws IOException {
        
        Path path;
        
        if (this.closed) throw new IOException("Store already closed");
        
        if (this.readonly || this.isReadOnly(uri)) return false;
        
        path = this.system.getPath(this.root, uri);
        
        if (!this.existsObject(uri)) path = path.getParent();

        return Files.isWritable(path);
    }

    /**
     *  R&uuml;ckgabe <code>true</code> wenn bei der per URI angegebene
     *  Ressource das Attribut Hidden gesetzt ist, sonst <code>false</code>.
     *  @param  uri Pfad der Ressource als URI
     *  @return <code>true</code> wenn bei der per URI angegebenen Ressource das
     *          Attribut Hidden gesetzt ist, sonst <code>false</code>
     *  @throws IOException bei fehlerhaftem Zugriff auf das Dateisystem
     */
    public boolean isHidden(String uri) throws IOException {
        
        Path              path;
        DosFileAttributes attributes;
        
        if (this.closed) throw new IOException("Store already closed");

        if (this.readonly) return true;
        
        path = this.system.getPath(this.root, uri);
        
        attributes = Files.readAttributes(path, DosFileAttributes.class);
        
        return attributes == null ? false : attributes.isHidden();
    }
        
    /**
     *  Setzt das Attribut Hidden bei der per URI angegebene Ressource
     *  @param  uri    Pfad der Ressource als URI
     *  @param  hidden Wert vom Attribut Hidden
     *  @throws IOException bei fehlerhaftem Zugriff auf das Dateisystem
     */
    public void setHidden(String uri, boolean hidden) throws IOException {
        
        Path              path;
        DosFileAttributes attributes;
        
        if (this.closed) throw new IOException("Store already closed");
        
        if (this.readonly) throw new IOException("Write access forbidden");
        
        path = this.system.getPath(this.root, uri);
        
        attributes = Files.readAttributes(path, DosFileAttributes.class);
        
        if (attributes == null) throw new IOException("Method not supported.");
        
        Files.setAttribute(path, FILE_ATTRIBUTE_HIDDEN, Boolean.valueOf(hidden));
    }    
    
    /**
     *  R&uuml;ckgabe <code>true</code> wenn bei der per URI angegebene
     *  Ressource das Attribut Archiv gesetzt ist, sonst <code>false</code>.
     *  @param  uri Pfad der Ressource als URI
     *  @return <code>true</code> wenn bei der per URI angegebenen Ressource das
     *          Attribut Archiv gesetzt ist, sonst <code>false</code>
     *  @throws IOException bei fehlerhaftem Zugriff auf das Dateisystem
     */
    public boolean isArchive(String uri) throws IOException {
        
        Path              path;
        DosFileAttributes attributes;
        
        if (this.closed) throw new IOException("Store already closed");

        if (this.readonly) return true;
        
        path = this.system.getPath(this.root, uri);
        
        attributes = Files.readAttributes(path, DosFileAttributes.class);
        
        return attributes == null ? false : attributes.isArchive();
    }
    
    /**
     *  Setzt das Attribut Archive bei der per URI angegebene Ressource
     *  @param  uri     Pfad der Ressource als URI
     *  @param  archive Wert vom Attribut Archive
     *  @throws IOException bei fehlerhaftem Zugriff auf das Dateisystem
     */
    public void setArchive(String uri, boolean archive) throws IOException {
        
        Path              path;
        DosFileAttributes attributes;
        
        if (this.closed) throw new IOException("Store already closed");
        
        if (this.readonly) throw new IOException("Write access forbidden");
        
        path = this.system.getPath(this.root, uri);
        
        attributes = Files.readAttributes(path, DosFileAttributes.class);
        
        if (attributes == null) throw new IOException("Method not supported.");
        
        Files.setAttribute(path, FILE_ATTRIBUTE_ARCHIVE, Boolean.valueOf(archive));
    }    
    
    /**
     *  R&uuml;ckgabe <code>true</code> wenn bei der per URI angegebene
     *  Ressource das Attribut System gesetzt ist, sonst <code>false</code>.
     *  @param  uri Pfad der Ressource als URI
     *  @return <code>true</code> wenn bei der per URI angegebenen Ressource das
     *          Attribut System gesetzt ist, sonst <code>false</code>
     *  @throws IOException bei fehlerhaftem Zugriff auf das Dateisystem
     */
    public boolean isSystem(String uri) throws IOException {
        
        Path              path;
        DosFileAttributes attributes;
        
        if (this.closed) throw new IOException("Store already closed");

        if (this.readonly) return true;
        
        path = this.system.getPath(this.root, uri);
        
        attributes = Files.readAttributes(path, DosFileAttributes.class);
        
        return attributes == null ? false : attributes.isSystem();
    }
    
    /**
     *  Setzt das Attribut System bei der per URI angegebene Ressource
     *  @param  uri    Pfad der Ressource als URI
     *  @param  system Wert vom Attribut System
     *  @throws IOException bei fehlerhaftem Zugriff auf das Dateisystem
     */
    public void setSystem(String uri, boolean system) throws IOException {
        
        Path              path;
        DosFileAttributes attributes;
        
        if (this.closed) throw new IOException("Store already closed");
        
        if (this.readonly) throw new IOException("Write access forbidden");
        
        path = this.system.getPath(this.root, uri);
        
        attributes = Files.readAttributes(path, DosFileAttributes.class);
        
        if (attributes == null) throw new IOException("Method not supported.");
        
        Files.setAttribute(path, FILE_ATTRIBUTE_SYSTEM, Boolean.valueOf(system));
    }    
    
    /**
     *  Erstellt das per URI angegebene Verzeichnis.
     *  @param  uri Pfad des Verzeichnis als URI
     *  @throws IOException bei fehlerhaftem Zugriff auf das Dateisystem und
     *          wenn die Ressource nicht angelegt werden kann
     */
    public void createFolder(String uri) throws IOException {
        
        Path path;
    
        if (this.closed) throw new IOException("Store already closed");
    
        if (!this.canWrite(uri)) throw new IOException("Write access forbidden");
        
        path = this.system.getPath(this.root, uri);
        
        if (Files.exists(path) && Files.isDirectory(path)) return;
        
        Files.createDirectories(path);
    }

    /**
     *  Erstellt die per URI angegebene Ressource.
     *  @param  uri Pfad der Ressource als URI
     *  @throws IOException bei fehlerhaftem Zugriff auf das Dateisystem und
     *          wenn die Ressource nicht angelegt werden kann
     */
    public void createResource(String uri) throws IOException {
    
        Path path;
        
        if (this.closed) throw new IOException("Store already closed");
    
        if (!this.canWrite(uri)) throw new IOException("Write access forbidden");
        
        path = this.system.getPath(this.root, uri);
        
        Files.newOutputStream(path, StandardOpenOption.CREATE_NEW).close();
    }    
    
    /**
     *  Speichert die Daten aus dem &uuml;bergeben Datenstrom in der per URI
     *  angegebenen Ressource. Die Parameter <code>contentType</code> und
     *  <code>characterEncoding</code> werden ignoriert.
     *  @param  uri      Pfad der Ressource als URI
     *  @param  input    Dateneingangsstrom
     *  @param  type     ContentType (wird ignoriert)
     *  @param  encoding CharacterEncoding (wird ignoriert)
     *  @throws IOException bei fehlerhaftem Zugriff auf die Datenstr&ouml;me
     *          oder das Dateisystem
     */
    public void setResourceContent(String uri, InputStream input, String type, String encoding) throws IOException {
        
        Path         path;
        OutputStream output;
        
        byte[]       bytes;
        
        int          size;        
        
        if (this.closed) throw new IOException("Store already closed");
    
        if (!this.canWrite(uri)) throw new IOException("Write access forbidden");
        
        path = this.system.getPath(this.root, uri);
        
        bytes  = new byte[this.blocksize];
        output = Files.newOutputStream(path, StandardOpenOption.CREATE);
        output = new BufferedOutputStream(output);

        try {
            
            if (!(input instanceof BufferedInputStream)) input = new BufferedInputStream(input, this.blocksize);

            while ((size = input.read(bytes, 0, bytes.length)) >= 0) {
            
                output.write(bytes, 0, size);
            }
            
        } finally {
        
            try {input.close();
            } catch (Exception exception) {
        
                //keine Fehlerbehandlung vorgesehen
            }
        
            try {output.close();
            } catch (Exception exception) {

                //keine Fehlerbehandlung vorgesehen
            }
        }        
    }    
    
    /**
     *  Ermittelt das Datum der letzten &Auml;nderung der per URI angegebenen
     *  Ressource.
     *  @param  uri Pfad der Ressource als URI
     *  @return das Datum der letzten &Auml;nderung der per URI angegebenen
     *          Ressource
     *  @throws IOException bei fehlerhaftem Zugriff auf das Dateisystem
     */
    public Date getLastModified(String uri) throws IOException {
        
        FileTime time;
        Path     path;
        
        if (this.closed) throw new IOException("Store already closed");
        
        path = this.system.getPath(this.root, uri);
        
        time = Files.readAttributes(path, BasicFileAttributes.class).lastModifiedTime();
        
        if (time == null) time = Files.readAttributes(path, BasicFileAttributes.class).creationTime();
        if (time == null) time = Files.readAttributes(path, BasicFileAttributes.class).lastAccessTime();
        
        return new Date(time.toMillis());        
    }    
   
    /**
     *  Setzt das Datum der letzten &Auml;nderung der per URI angegebenen
     *  Ressource.
     *  @param  uri  Pfad der Ressource als URI
     *  @param  time Datum der letzten &Auml;nderung
     *  @throws IOException bei fehlerhaftem Zugriff auf das Dateisystem
     */
    public void setLastModified(String uri, Date time) throws IOException {
        
        Path              path;
        DosFileAttributes attributes;
        
        if (this.closed) throw new IOException("Store already closed");
        
        path = this.system.getPath(this.root, uri);
        
        attributes = Files.readAttributes(path, DosFileAttributes.class);
        
        if (attributes == null) throw new IOException("Method not supported.");        
        
        Files.setAttribute(path, AbstractStore.FILE_ATTRIBUTE_LAST_MODIFIED, FileTime.fromMillis(time.getTime()));
    }
    
    /**
     *  Ermittelt das Datum der Erstellung des per URI angegebenen Ressource.
     *  Da dieses aber mit Java nicht ermittelt werden kann, wird das Datum der
     *  letzten &Auml;nderung zur&uuml;ckgegeben.
     *  @param  uri Pfad der Ressource als URI
     *  @return das Datum der Erstellung des per URI angegebenen Ressource, da
     *          dieses aber mit Java nicht ermittelt werden kann, wird das Datum
     *          der letzten &Auml;nderung zur&uuml;ckgegeben
     *  @throws IOException bei fehlerhaftem Zugriff auf das Dateisystem
     */
    public Date getLastAccessTime(String uri) throws IOException {
        
        FileTime time;
        Path     path;
        
        if (this.closed) throw new IOException("Store already closed");
        
        path = this.system.getPath(this.root, uri);
        
        time = Files.readAttributes(path, BasicFileAttributes.class).lastAccessTime();
        
        if (time == null) time = Files.readAttributes(path, BasicFileAttributes.class).lastModifiedTime();
        if (time == null) time = Files.readAttributes(path, BasicFileAttributes.class).creationTime();
        
        return new Date(time.toMillis());
    }
    
    /**
     *  Setzt das Datum der Erstellung der per URI angegebenen Ressource.
     *  @param  uri  Pfad der Ressource als URI
     *  @param  time Datum der Erstellung
     *  @throws IOException bei fehlerhaftem Zugriff auf das Dateisystem
     */
    public void setLastAccessTime(String uri, Date time) throws IOException {
        
        Path              path;
        DosFileAttributes attributes;
        
        if (this.closed) throw new IOException("Store already closed");
        
        path = this.system.getPath(this.root, uri);
        
        attributes = Files.readAttributes(path, DosFileAttributes.class);
        
        if (attributes == null) throw new IOException("Method not supported.");
        
        Files.setAttribute(path, AbstractStore.FILE_ATTRIBUTE_LAST_ACCESS_TIME, FileTime.fromMillis(time.getTime()));
    }

    /**
     *  Ermittelt das Datum der Erstellung des per URI angegebenen Ressource.
     *  Da dieses aber mit Java nicht ermittelt werden kann, wird das Datum der
     *  letzten &Auml;nderung zur&uuml;ckgegeben.
     *  @param  uri Pfad der Ressource als URI
     *  @return das Datum der Erstellung des per URI angegebenen Ressource, da
     *          dieses aber mit Java nicht ermittelt werden kann, wird das Datum
     *          der letzten &Auml;nderung zur&uuml;ckgegeben
     *  @throws IOException bei fehlerhaftem Zugriff auf das Dateisystem
     */
    public Date getCreationTime(String uri) throws IOException {
        
        FileTime time;
        Path     path;
        
        if (this.closed) throw new IOException("Store already closed");
        
        path = this.system.getPath(this.root, uri);
        
        time = Files.readAttributes(path, BasicFileAttributes.class).creationTime();
        
        if (time == null) time = Files.readAttributes(path, BasicFileAttributes.class).lastModifiedTime();
        if (time == null) time = Files.readAttributes(path, BasicFileAttributes.class).lastAccessTime();
        
        return new Date(time.toMillis());
    }
    
    /**
     *  Setzt das Datum der Erstellung der per URI angegebenen Ressource.
     *  @param  uri  Pfad der Ressource als URI
     *  @param  time Datum der Erstellung
     *  @throws IOException bei fehlerhaftem Zugriff auf das Dateisystem
     */
    public void setCreationTime(String uri, Date time) throws IOException {
        
        Path              path;
        DosFileAttributes attributes;
        
        if (this.closed) throw new IOException("Store already closed");
        
        path = this.system.getPath(this.root, uri);
        
        attributes = Files.readAttributes(path, DosFileAttributes.class);
        
        if (attributes == null) throw new IOException("Method not supported.");
        
        Files.setAttribute(path, AbstractStore.FILE_ATTRIBUTE_CREATION_TIME, FileTime.fromMillis(time.getTime()));
    }     
    
    /**
     *  Ermittelt die Eintr&auml;ge eines Verzeichnis. R&uuml;ckgabe der
     *  Eintr&auml;ge des Verzeichnis als Liste oder <code>null</code>, wenn es
     *  sich um kein Verzeichnis handelt.
     *  @param  uri Pfad der Ressource als URI
     *  @return die Eintr&auml;ge des Verzeichnis als Liste oder
     *          <code>null</code>, wenn es sich um kein Verzeichnis handelt
     *  @throws IOException bei fehlerhaftem Zugriff auf das Dateisystem
     */
    public String[] getChildrenNames(String uri) throws IOException {

        DirectoryStream<Path> directory;
        List<String>          list;
        Path                  path;
        
        if (this.closed) throw new IOException("Store already closed");
        
        path = this.system.getPath(this.root, uri);
        
        if (!Files.isDirectory(path)) return null;

        list = new ArrayList<String>();

        directory = Files.newDirectoryStream(path);

        try {for (Path child : directory) list.add(child.getFileName().toString());
        } finally {
            
            directory.close();
        }
        
        return list.toArray(new String[0]);
    }    
    
    /**
     *  R&uuml;ckgabe vom Datenstrom der per URI angegebenen Ressource.
     *  @param  uri Pfad der Ressource als URI
     *  @throws IOException bei ferhlerhaften Zugriff auf die Ressource
     *          oder den Datenstrom
     */
    public InputStream getResourceContent(String uri) throws IOException {
        
        Path path;
        
        if (this.closed) throw new IOException("Store already closed");
    
        if (!this.canRead(uri)) throw new IOException("Read access forbidden");
        
        path = this.system.getPath(this.root, uri);
    
        return new BufferedInputStream(Files.newInputStream(path), this.blocksize);
    }    
    
    /**
     *  Ermittelt die L&auml;nge der per URI angegebenen Ressource in Bytes.
     *  @param  uri Pfad der Ressource als URI
     *  @return die L&auml;nge der per URI angegebenen Ressource in Bytes
     *  @throws IOException bei fehlerhaftem Zugriff auf das Dateisystem
     */
    public long getResourceLength(String uri) throws IOException {
        
        Path path;
        
        if (this.closed) throw new IOException("Store already closed");
        
        path = this.system.getPath(this.root, uri);
        
        if (!Files.exists(path) || Files.isDirectory(path)) return -1;

        return Files.size(path);
    }
    
    /**
     *  R&uuml;ckgabe <code>true</code> wenn die bei per URI angegebene
     *  Ressource gesperrt ist. Mit dieser Instanz vom Store gesetzte Sperren
     *  werden ignoriert, da diese als eine Transaktion betrachtet werden.
     *  @param  uri       Pfad der Ressource als URI
     *  @param  exclusive Option <code>true</code> pr&uuml;ft auf exclusive
     *          Sperrung
     *  @return <code>true</code> bei per URI angegebener gesperrter Ressource
     *  @throws IOException bei fehlerhaftem Zugriff auf den Store
     */
    public synchronized boolean isLocked(String uri, boolean exclusive) throws IOException {
        
        if (this.closed) throw new IOException("Store already closed");

        return this.locks.isLocked(uri, this.signature, exclusive);
    }    

    /**
     *  Verschiebt die per URI angegebene Ressource im Dateisystem.
     *  @param  uri         Pfad der Ressource als URI
     *  @param  destination Zielpfad der Ressource als URI
     *  @param  options     Optionen f&uuml;r das Kopieren
     *  @throws IOException wenn die Ressource nicht verschoben werden kann
     */
    public void moveObject(String uri, String destination, CopyOption... options) throws IOException {

        Path source;
        Path target;

        if (this.closed) throw new IOException("Store already closed");
        
        if (!this.canWrite(uri)) throw new IOException("Write access forbidden");
        if (!this.canWrite(destination)) throw new IOException("Write access forbidden");

        source = this.system.getPath(this.root, uri).toAbsolutePath();
        target = this.system.getPath(this.root, destination).toAbsolutePath();

        Files.createDirectories(target.getParent());

        Files.move(source, target, options);
    }    
    
    /**
     *  Kopiert die per URI angegebene Ressource im Dateisystem.
     *  @param  uri         Pfad der Ressource als URI
     *  @param  destination Zielpfad der Ressource als URI
     *  @param  options     Optionen f&uuml;r das Kopieren
     *  @throws IOException wenn die Ressource nicht kopiert werden kann
     */
    public void copyObject(String uri, String destination, CopyOption... options) throws IOException {
        
        Path source;
        Path target;

        if (this.closed) throw new IOException("Store already closed");
        
        if (!this.canWrite(uri)) throw new IOException("Write access forbidden");
        if (!this.canWrite(destination)) throw new IOException("Write access forbidden");

        source = this.system.getPath(this.root, uri).toAbsolutePath();
        target = this.system.getPath(this.root, destination).toAbsolutePath();
        
        Files.createDirectories(target.getParent());

        Files.copy(source, target, options);
    }    
    
    /**
     *  Entfernt die per URI angegebene Ressource aus dem Dateisystem.
     *  @param  uri Pfad der Ressource als URI
     *  @throws IOException wenn die Ressource nicht gel&ouml;scht werden kann
     */
    public void removeObject(String uri) throws IOException {
        
        Path path;
        
        if (this.closed) throw new IOException("Store already closed");
    
        if (!this.canWrite(uri)) throw new IOException("Write access forbidden");
        
        path = this.system.getPath(this.root, uri);

        Files.deleteIfExists(path);
    }    

    /**
     *  Veranlasst das Ausf&uuml;hren der letzten bzw. aktuellen Transaktion und
     *  bereinigt ggf. durch diese Instanz vom Store gesetzte Sperren.
     *  @throws IOException bei fehlerhaftem Zugriff auf den Store
     */
    public synchronized void commit() throws IOException {
        
        String[] locks;
    
        if (this.closed) throw new IOException("Store already closed");
        
        locks = this.locks.getLocks(this.signature);
        
        if (locks == null) return;
        
        for (String uri : locks) {
            
            this.locks.unlock(uri, this.signature, true);
        }
    }

    /**
     *  Veranlasst das R&uuml;ckspielen der letzten bzw. aktuellen Transaktion
     *  und bereinigt ggf. durch diese Instanz vom Store gesetzte Sperren.
     *  @throws IOException bei fehlerhaftem Zugriff auf den Store
     */
    public synchronized void rollback() throws IOException {
        
        String[] locks;
    
        if (this.closed) throw new IOException("Store already closed");
        
        locks = this.locks.getLocks(this.signature);
        
        if (locks == null) return;        
        
        for (String uri : locks) {
            
            this.locks.unlock(uri, this.signature, true);
        }
    }

    /**
     *  Schliesst den Store und bereinigt ggf. durch diese Instanz vom Store
     *  gesetzte Sperren.
     *  @throws IOException bei fehlerhaftem Zugriff auf den Store
     */
    public synchronized void close() throws IOException {
        
        String[] locks;
        
        try {
            
            if (this.closed) throw new IOException("Store already closed");

            this.closed = true;
            
            locks = this.locks.getLocks(this.signature);
            
            if (locks == null) return;        
            
            for (String uri : locks) {
                
                this.locks.unlock(uri, this.signature, true);
            }
            
        } finally {
            
            this.shares.remove(this.signature);
            
            if (this.equals(this.store)) this.system.close();
        }
    }    
    
    /**
     *  R&uuml;ckgabe der formatierten Information zum FileStore als String.
     *  Der Zeilenumbruch erfolgt abh&auml;ngig vom aktuellen Betriebssystem.
     *  @return die formatierte Information zum FileStore als String
     */
    public String toString() {

        String        string;
        StringBuilder result;

        //der Zeilenumbruch wird entsprechend dem System ermittelt
        string = System.getProperty("line.separator", "\r\n");

        //das Paket der Klasse wird ermittelt
        result = new StringBuilder("[").append(this.getClass().getName()).append("]").append(string);

        result.append("  system    = ").append(this.system).append(string);
        result.append("  signature = ").append(this.signature).append(string);
        result.append("  root      = ").append(this.root).append(string);
        result.append("  readonly  = ").append(this.readonly).append(string);
        result.append("  blocksize = ").append(this.blocksize).append(string);
        result.append("  closed    = ").append(this.closed).append(string);

        return result.toString();
    }    
}