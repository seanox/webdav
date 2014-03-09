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
package com.seanox.webdav.storage.lock;

import java.util.Hashtable;
import java.util.Map;

/**
 *  Resource stellt eine Verwaltung von Sperreintr&auml;gen zu einem als URI
 *  angegeben Pfad zur Verf&uuml;gung. Das Objekt wird ausschliesslich intern
 *  vom Lock-Manager verwendet um die Sperreintr&auml;ge zu einem Pfad zu
 *  verwalten.<br>
 *  <br>
 *  Resource 1.2013.0327<br>
 *  Copyright (C) 2013 Seanox Software Solutions<br>
 *  Alle Rechte vorbehalten.
 *
 *  @author  Seanox Software Solutions
 *  @version 1.2013.0327
 */
class Resource {

    /** Transaktionskennung der exklusiven Sperrung */
    private volatile String exclusive;

    /** Verzeichnis der registrierten Sperren (Signature | Sperreintrag) */
    private volatile Map<String, Entry> locks;

    /** Pfad der Ressource als URI */
    private volatile String uri;

    /**
     *  Konstruktor, richtet die Ressource zur Haltung von Sperren f&uuml;r die
     *  den als URI angegebenen Pfad ein.
     *  @param uri Pfad als URI
     */
    Resource(String uri) {

        this.locks = new Hashtable<String, Entry>();

        this.uri = uri;
    }

    /**
     *  R&uuml;ckgabe <code>true</code>, bei Sperrung der Ressource.
     *  @return <code>true</code>, bei Sperrung der Ressource
     */
    synchronized boolean isLocked() {

        return !this.locks.isEmpty();
    }
    
    /**
     *  R&uuml;ckgabe <code>true</code>, bei Sperrung der Ressource durch die
     *  angegebene Transaktionskennung.
     *  @param  signature Transaktionskennung
     *  @return <code>true</code>, bei Sperrung der Ressource durch die
     *          angegebene Transaktionskennung
     */
    synchronized boolean isLocked(String signature) {
        
        return this.locks.get(signature) != null;
    }    

    /**
     *  R&uuml;ckgabe <code>true</code>, bei exklusiver Sperrung der Ressource.
     *  @param  exclusive <code>true</code> p&uuml;ft auf exklusive Sperrung
     *  @return <code>true</code>, bei exklusiver Sperrung der Ressource
     */
    synchronized boolean isLocked(boolean exclusive) {

        if (this.locks.isEmpty()) return false;

        return (!exclusive || (exclusive && this.exclusive != null));
    }
    
    /**
     *  R&uuml;ckgabe <code>true</code>, bei exklusiver Sperrung der Ressource
     *  durch die angegebene Transaktionskennung.
     *  @param  signature Transaktionskennung
     *  @param  exclusive <code>true</code> p&uuml;ft auf exklusive Sperrung
     *  @return <code>true</code>, bei exklusiver Sperrung der Ressource
     *          durch die angegebene Transaktionskennung.
     */
    synchronized boolean isLocked(String signature, boolean exclusive) {

        if (this.locks.isEmpty()) return false;

        return (!exclusive || (exclusive && this.exclusive != null));
    }    

    /**
     *  R&uuml;ckgabe vom Pfad der Ressource als URI.
     *  @return der Pfad der Ressource als URI
     */
    String getUri() {

        return this.uri;
    }

    /**
     *  Registriert eine weitere Sperre. Ist die Ressource bereits exclusiv
     *  gesperrt, f&uuml;hrt dies zur Ausnahme <code>Exclusiveness</code>.
     *  @param  signature Transaktionskennung der zu registrierenden Sperre
     *  @param  exclusive <code>true</code> bei exklusiver Sperrung
     *  @return <code>true</code>, wenn die Sperre gesetzt werden konnte
     *  
     */
    synchronized boolean lock(String signature, boolean exclusive) {
        
        Entry entry;

        if (this.exclusive != null && !this.exclusive.equals(signature)) return false;
        
        entry = this.locks.get(signature);
        entry = entry == null ? new Entry(this.uri, signature, exclusive) : entry.share(exclusive);
        
        this.locks.put(signature, entry);

        if (exclusive) this.exclusive = signature;
        
        return true;
    }

    /**
     *  Entfernt die angegebene Sperre zur angegebenen Transaktionskennung.
     *  @param signature Transaktionskennung
     */
    synchronized void unlock(String signature) {
        
        this.unlock(signature, false);
    }
    
    /**
     *  Entfernt ggf. alle Sperren zur angegebenen Transaktionskennung.
     *  @param signature Transaktionskennung
     *  @param clear     <code>true</code> entfernt alle Sperren zur
     *                   angegebenen Signatur 
     */
    synchronized void unlock(String signature, boolean clear) {
        
        Entry entry;

        if ((entry = this.locks.get(signature)) == null) return;
        
        while ((entry = entry.release()) != null && clear) continue;

        if (entry == null && this.exclusive != null && this.exclusive.equals(signature)) this.exclusive = null;

        if (entry == null) this.locks.remove(signature);
    }    

    /**
     *  R&uuml;ckgabe der formatierten Information zur Resource als String.
     *  Der Zeilenumbruch erfolgt abh&auml;ngig vom aktuellen Betriebssystem.
     *  @return die formatierte Information zur Resource als String
     */
    public String toString() {

        String        string;
        StringBuilder result;

        //der Zeilenumbruch wird entsprechend dem System ermittelt
        string = System.getProperty("line.separator", "\r\n");

        //das Paket der Klasse wird ermittelt
        result = new StringBuilder("[").append(this.getClass().getName()).append("]").append(string);
        
        result.append("  uri       = ").append(this.uri).append(string);
        result.append("  locks     = ").append(this.locks.size()).append("x").append(string);
        result.append("  exclusive = ").append(this.exclusive != null).append(string);

        return result.toString();
    }
}