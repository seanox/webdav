/**
 *  LIZENZBEDINGUNGEN - Seanox Software Solutions ist ein Open-Source-Projekt,
 *  im Folgenden Seanox Software Solutions oder kurz Seanox genannt.
 *  Diese Software unterliegt der Version 2 der GNU General Public License.
 *
 *  WebDAV, Module for Seanox Devwex
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.seanox.common.Codec;

/**
 *  Manager stellt eine Verwaltung von Sperreintr&auml;gen f&uuml;r als URI
 *  angegebene Ressourcen zur Verf&uuml;gung.<br>
 *  <br>
 *  Allgemeine Regeln f&uuml;r den Sperrmechanismus:<br>
 *  <ul>
 *    <li>
 *      Es wird zwischen einfachen und exklusiven Sperren (Locks) unterschieben.
 *    </li>
 *    <li>
 *      Die einfache Sperre registriert einen lesenden Zugriff auf eine
 *      Ressource und sch&uuml;tzt diese so vor einem schreibenden oder
 *      l&ouml;schenden Zugriff und kann mehrfach mit unterschiedlichen
 *      Transaktionskennungen (Signaturen) f&uuml;r eine Ressource vergeben
 *      werden. Sie wirkt auf alle darunter, sowie dar&uuml;ber liegenden
 *      Ressourcen und verhindert f&uuml;r diese eine exklusive Sperrung durch
 *      andere Transaktionen.
 *    </li>
 *    <li>
 *      Die exklusive Sperre registriert einen schreibenden oder l&ouml;schenden
 *      Zugriff auf eine Ressource und sch&uuml;tzt diese so vor lesenden
 *      Zugriff und kann nur einmalig f&uuml;r eine Ressource vergeben werden.
 *      Sie wirkt auf alle darunter, sowie dar&uuml;ber liegenden Ressourcen und
 *      verhindert f&uuml;r diese eine weitere exklusive Sperrung durch andere
 *      Transaktionen.
 *    </li>
 *    <li>
 *      Ein Timeout f&uuml;r eingerichtete Transaktionen/Locks w&auml;re
 *      sinnvoll, ist in dieser Version aber nicht implementiert. Ziel
 *      k&ouml;nnte eine automatische Bereinigung &uuml;berf&auml;lliger
 *      Transaktionen/Locks sein. Bei der weiteren Verwendung solcher
 *      Transaktionen/Locks m&uuml;ssten diese dann zum Fehler f&uuml;hren.
 *    </li>
 *  </ul>
 *  Manager 1.2013.0423<br>
 *  Copyright (C) 2013 Seanox Software Solutions<br>
 *  Alle Rechte vorbehalten.
 *
 *  @author  Seanox Software Solutions
 *  @version 1.2013.0423
 */
public class Manager {

    /** Verzeichnis der exklusiven gesperrten URIs */
    private volatile List<String> exclusive;

    /** Verzeichnis der registrierten Sperren (URI | Ressource) */
    private volatile Map<String, Resource> resources;

    /** Konstruktor, richtet Manager zur Haltung von Sperren ein. */
    public Manager() {
    
        this.resources = Collections.synchronizedMap(new HashMap<String, Resource>());
        this.exclusive = Collections.synchronizedList(new ArrayList<String>());
    }
    
    /**
     *  Vereinfacht den als URI &uuml;bergebenen Pfad f&uuml;r den Vergleich.
     *  Dazu wird dieser ausgeglichen, in Kleinbuchstaben ge&auml;ndert und mit
     *  Slash beendet.
     *  @param  uri Pfad als URI
     *  @return die vereinfache Pfad als URI
     */
    private static String optimizeUri(String uri) {

        uri = Codec.decode(uri, Codec.DOT).toLowerCase();

        if (uri.length() > 0 && !uri.endsWith("/")) uri = uri.concat("/");

        return uri;
    }

    /**
     *  R&uuml;ckgabe <code>true</code> wenn die bei per URI angegebene
     *  Ressource gesperrt ist. Gesetzte Sperren mit der angegebenen Signatur
     *  werden ignoriert, da diese als eine Transaktion betrachtet werden. 
     *  @param  uri       Pfad der Ressource als URI
     *  @param  signature eindeutige Transaktionskennung
     *  @return <code>true</code> bei per URI angegebener gesperrter Ressource
     */
    public synchronized boolean isLocked(String uri, String signature) {

        return this.isLocked(uri, signature, false);
    }

    /**
     *  R&uuml;ckgabe <code>true</code> wenn die bei per URI angegebene
     *  Ressource gesperrt ist. Gesetzte Sperren mit der angegebenen Signatur
     *  werden ignoriert, da diese als eine Transaktion betrachtet werden.
     *  @param  uri       Pfad der Ressource als URI
     *  @param  signature eindeutige Transaktionskennung
     *  @param  exclusive Option <code>true</code> pr&uuml;ft auf exclusive
     *          Sperrung
     *  @return <code>true</code> bei per URI angegebener gesperrter Ressource
     */
    public synchronized boolean isLocked(String uri, String signature, boolean exclusive) {
        
        Resource resource;

        if (this.resources.isEmpty()) return false;

        uri = Manager.optimizeUri(uri);

        for (String lock : exclusive ? this.exclusive : this.resources.keySet()) {

            if (uri.startsWith(lock) || lock.startsWith(uri)) {

                resource = this.resources.get(lock);

                if (resource == null || resource.isLocked(signature)) continue;

                return true;
            }
        }

        return false;
    }

    /**
     *  Setzt eine mit der &uuml;bergebenen Signature versehene Sperre f&uuml;r
     *  die per URI angegebene Ressource. Die Signatur ist ein eindeutige
     *  Transaktionskennung und dient zur Unterscheidung der einzelnen Zugriffe
     *  auf die Ressourcen. R&uuml;ckgabe <code>true</code>, wenn die Sperrung
     *  erfolgreich eingetragen wurde, <code>false</code> wenn eine bereits
     *  gesetzte Sperre die Sperrung verhindert. Gesetzte Sperren mit der
     *  angegebenen Signatur werden ignoriert, da diese als eine Transaktion
     *  betrachtet werden.
     *  @param  uri       Pfad der Ressource als URI
     *  @param  signature eindeutige Transaktionskennung
     *  @return <code>true</code> bei erfolgreich eingetragener Sperrung
     */
    public synchronized boolean lock(String uri, String signature) {

        return this.lock(uri, signature, false);
    }

    /**
     *  Setzt eine mit der &uuml;bergebenen Signature versehene Sperre f&uuml;r
     *  die per URI angegebene Ressource. Die Signatur ist ein eindeutige
     *  Transaktionskennung und dient zur Unterscheidung der einzelnen Zugriffe
     *  auf die Ressourcen. R&uuml;ckgabe <code>true</code>, wenn die Sperrung
     *  erfolgreich eingetragen wurde, <code>false</code> wenn eine bereits
     *  gesetzte Sperre die Sperrung verhindert. Gesetzte Sperren mit der
     *  angegebenen Signatur werden ignoriert, da diese als eine Transaktion
     *  betrachtet werden.
     *  @param  uri       Pfad der Ressource als URI
     *  @param  signature eindeutige Transaktionskennung
     *  @param  exclusive <code>true</code> bei exklusiver Sperrung
     *  @return <code>true</code> bei erfolgreich eingetragener Sperrung
     */
    public synchronized boolean lock(String uri, String signature, boolean exclusive) {
    
        Resource resource;
        
        if (signature == null || signature.trim().length() == 0) return false;

        if (this.isLocked(uri, signature, true)) return false;

        uri = Manager.optimizeUri(uri);

        resource = this.resources.get(uri);

        if (resource == null) resource = new Resource(uri);

        if (!resource.lock(signature, exclusive)) return false;

        this.resources.put(uri, resource);

        if (exclusive) this.exclusive.add(uri);

        return true;
    }
    
    /**
     *  Hebt die eingerichtete Sperren zur angegebenen Signatur f&uuml;r die per
     *  URI angegebene Ressource auf. Die Signatur ist ein eindeutige
     *  Transaktionskennung und dient zur Unterscheidung der einzelnen Zugriffe
     *  auf die Ressourcen.
     *  @param  uri       Pfad der Ressource als URI
     *  @param  signature eindeutige Transaktionskennung
     */
    public synchronized void unlock(String uri, String signature) {
        
        this.unlock(uri, signature, false);
    }
    
    /**
     *  Hebt ggf. alle eingerichteten Sperren zur angegebenen Signatur f&uuml;r
     *  die per URI angegebene Ressource auf. Die Signatur ist ein eindeutige
     *  Transaktionskennung und dient zur Unterscheidung der einzelnen Zugriffe
     *  auf die Ressourcen.
     *  @param  uri       Pfad der Ressource als URI
     *  @param  signature eindeutige Transaktionskennung
     *  @param clear      <code>true</code> entfernt alle Sperren zur
     *                    angegebenen Signatur 
     */
    public synchronized void unlock(String uri, String signature, boolean clear) {

        Resource resource;

        if (signature == null || signature.trim().length() == 0) return;

        uri = Manager.optimizeUri(uri);

        resource = this.resources.get(uri);

        if (resource == null) return;

        resource.unlock(signature, clear);

        if (!resource.isLocked(true) && this.exclusive.contains(uri)) this.exclusive.remove(uri);

        if (!resource.isLocked()) this.resources.remove(uri);
    }
    
    /**
     *  Ermittelt alle gesperrten Ressourcen zur einer Transaktionskennung.
     *  Liegt zur Transaktionskennung keine vor, wird <code>null</code>
     *  zur&uuml;ckgegeben. Die Signatur ist ein eindeutige Transaktionskennung
     *  und dient zur Unterscheidung der einzelnen Zugriffe auf die Ressourcen.
     *  @param  signature eindeutige Transaktionskennung
     *  @return die URIs der ermittelten Sperren, sonst <code>null</code>
     */
    public synchronized String[] getLocks(String signature) {
        
        List<String> locks;
        
        if (signature == null || signature.trim().length() == 0) return null;
        
        locks = new ArrayList<String>();
        
        for (Resource resource : this.resources.values()) {
            
            if (resource.isLocked(signature)) locks.add(resource.getUri());
        }
        
        return locks.isEmpty() ? null : locks.toArray(new String[0]);
    }

    /**
     *  R&uuml;ckgabe der formatierten Information zur Manager als String.
     *  Der Zeilenumbruch erfolgt abh&auml;ngig vom aktuellen Betriebssystem.
     *  @return die formatierte Information zur Manager als String
     */
    public String toString() {
        
        String        string;
        StringBuilder result;

        //der Zeilenumbruch wird entsprechend dem System ermittelt
        string = System.getProperty("line.separator", "\r\n");

        //das Paket der Klasse wird ermittelt
        result = new StringBuilder("[").append(getClass().getName()).append("]").append(string);
        
        result.append("  resources = ").append(this.resources.size()).append("x").append(string);
        result.append("  exclusive = ").append(this.exclusive.size()).append("x").append(string);

        return result.toString();        
    }
}