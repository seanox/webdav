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

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *  Entry stellt einen Datenobjekt zur Haltlung von Informationen fuer eine
 *  Datensperre zur Verfuegung. Das Objekt wird ausschliesslich intern von
 *  Resource verwendet um die Sperreintraege zu einem Pfad zu verwalten.<br>
 *  <br>
 *  Entry 1.2013.0516<br>
 *  Copyright (C) 2013 Seanox Software Solutions<br>
 *  Alle Rechte vorbehalten.
 *
 *  @author  Seanox Software Solutions
 *  @version 1.2013.0516
 */
class Entry {

    /** Pfad als URI des Sperreintrags */
    private String uri;

    /** eindeutige Transaktionskennung vom Sperreintrag */
    private String signature;

    /** Zeitpunkt der Einrichtung des Sperreintrag */
    private long timing;

    /** Option f&uuml;r eine exclusiven Sperreintrag */
    private boolean exclusive;
    
    /** Anzahl von Sperreintr&auml;gen zur Transaktionskennung */
    private int count;

    /**
     *  Konstruktor, richtet den Sperreintrag ein.
     *  @param uri       Pfad als URI
     *  @param signature eindeutige Transaktionskennung
     *  @param exclusive Option <code>true</code> f&uuml;r eine exclusive Sperre
     */
    Entry(String uri, String signature, boolean exclusive) {

        this.exclusive = exclusive;
        this.signature = signature;
        this.uri       = uri;

        this.timing = System.currentTimeMillis();
    }
    
    /**
     *  Weitet den Sperreintrag um eine weitere Nutzung aus.
     *  @return die aktuelle Instanz vom Sperreintrag 
     */
    synchronized Entry share() {

        this.count++;

        return this;
    }

    /**
     *  Weitet den Sperreintrag ggf. um eine weitere exklusive Nutzung aus.
     *  @param  exclusive <code>true</code> exklusive Nutzung
     *  @return die aktuelle Instanz vom Sperreintrag 
     */
    synchronized Entry share(boolean exclusive) {

        if (exclusive) this.exclusive = true;

        this.count++;

        return this;
    }
    
    /**
     *  Hebt die Nutzung eines Sperreintrags auf. Als R&uuml;ckgabe erfolgt die
     *  aktuelle Instanz vom Sperreintrag, wenn eine weitere Nutzung besteht.
     *  Liegt keine weitere vor, wird <code>null</code> zur&uuml;ckgegeben. 
     *  @return die aktuelle Instanz vom Sperreintrag, wenn eine weitere Nutzung
     *          besteht, sonst <code>null</code>
     */
    synchronized Entry release() {

        this.count--;

        return (this.count <= 0) ? null : this;
    }

    /**
     *  R&uuml;ckgabe vom Pfad des Sperreintrags als URI.
     *  @return der Pfad des Sperreintrags als URI
     */
    String getUri() {

        return this.uri;
    }

    /**
     *  R&uuml;ckgabe der eindeutigen Transaktionskennung vom Sperreintrag.
     *  @return die eindeutige Transaktionskennung vom Sperreintrag
     */
    String getSignature() {

        return this.signature;
    }

    /**
     *  R&uuml;ckgabe vom Zeitpunkt der Einrichtung des Sperreintrag.
     *  @return der Zeitpunkt der Einrichtung des Sperreintrag
     */
    long getTiming() {

        return this.timing;
    }

    /**
     *  R&uuml;ckgabe der Option <code>exclusive</code>vom Sperreintrag.
     *  @return die Option <code>exclusive</code>vom Sperreintrag
     */
    boolean isExclusive() {

        return this.exclusive;
    }

    /**
     *  R&uuml;ckgabe der formatierten Information zum Entry als String.
     *  Der Zeilenumbruch erfolgt abh&auml;ngig vom aktuellen Betriebssystem.
     *  @return die formatierte Information zur Entry als String
     */
    public String toString() {

        String        string;
        StringBuilder result;

        //der Zeilenumbruch wird entsprechend dem System ermittelt
        string = System.getProperty("line.separator", "\r\n");

        //das Paket der Klasse wird ermittelt
        result = new StringBuilder("[").append(this.getClass().getName()).append("]").append(string);
        
        result.append("  uri       = ").append(this.uri).append(string);
        result.append("  signature = ").append(this.signature).append(string);
        result.append("  count     = ").append(this.count).append(string);
        result.append("  timing    = ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(this.timing))).append(string);
        result.append("  exclusive = ").append(this.exclusive).append(string);

        return result.toString();
    }
}