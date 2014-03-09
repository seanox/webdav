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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import com.seanox.module.Response;

/**
 *  Status stellt die f&uuml;r WebDAV ben&ouml;tigten Status zur Verf&uuml;gung.
 *  Detail zu den einzelnen Status sind in der RFC 2518, 2518 enthalten.<br>
 *  <br>
 *  Hinweis - diese Klasse basiert auf den Quellen der Datei WebdavStatus.java
 *  von Marc Eaddy aus dem net.sf.webdav Paket des WebDAV Projekts
 *  (http://webdav-servlet.sourceforge.net/index.html).<br>
 *  <br>
 *  Status 1.2014.0131<br>
 *  Copyright (C) 2014 Seanox Software Solutions<br>
 *  Alle Rechte vorbehalten.
 *
 *  @author  Seanox Software Solutions
 *  @version 1.2014.0131
 */
class Status {

    /** Mapping der Status Codes */
    private static Map<Integer, String> mapping;
    
    /** Konstante f&uuml;r den Status Code SUCCESS */
    static final int SUCCESS = Response.STATUS_SUCCESS;

    /** Konstante f&uuml;r den Status Code CREATED */
    static final int CREATED = Response.STATUS_CREATED;

    /** Konstante f&uuml;r den Status Code ACCEPTED */
    static final int ACCEPTED = Response.STATUS_ACCEPTED;

    /** Konstante f&uuml;r den Status Code NO_CONTENT */
    static final int NO_CONTENT = Response.STATUS_NO_CONTENT;

    /** Konstante f&uuml;r den Status Code MOVED_PERMANENTLY */
    static final int MOVED_PERMANENTLY = Response.STATUS_MOVED_PERMANENTLY;

    /** Konstante f&uuml;r den Status Code FOUND */
    static final int FOUND = Response.STATUS_FOUND;

    /** Konstante f&uuml;r den Status Code NOT_MODIFIED */
    static final int NOT_MODIFIED = Response.STATUS_NOT_MODIFIED;

    /** Konstante f&uuml;r den Status Code BAD_REQUEST */
    static final int BAD_REQUEST = Response.STATUS_BAD_REQUEST;

    /** Konstante f&uuml;r den Status Code AUTHORIZATION_REQUIRED */
    static final int AUTHORIZATION_REQUIRED = Response.STATUS_AUTHORIZATION_REQUIRED;

    /** Konstante f&uuml;r den Status Code FORBIDDEN */
    static final int FORBIDDEN = Response.STATUS_FORBIDDEN;

    /** Konstante f&uuml;r den Status Code NOT_FOUND */
    static final int NOT_FOUND = Response.STATUS_NOT_FOUND;

    /** Konstante f&uuml;r den Status Code INTERNAL_SERVER_ERROR */
    static final int INTERNAL_SERVER_ERROR = Response.STATUS_INTERNAL_SERVER_ERROR;

    /** Konstante f&uuml;r den Status Code NOT_IMPLEMENTED */
    static final int NOT_IMPLEMENTED = Response.STATUS_NOT_IMPLEMENTED;

    /** Konstante f&uuml;r den Status Code BAD_GATEWAY */
    static final int BAD_GATEWAY = Response.STATUS_BAD_GATEWAY;

    /** Konstante f&uuml;r den Status Code SERVICE_UNAVAILABLE */
    static final int SERVICE_UNAVAILABLE = Response.STATUS_SERVICE_UNAVAILABLE;

    /** Konstante f&uuml;r den Status Code CONTINUE */
    static final int CONTINUE = 100;

    /** Konstante f&uuml;r den Status Code METHOD_NOT_ALLOWED */
    static final int METHOD_NOT_ALLOWED = Response.STATUS_METHOD_NOT_ALLOWED;

    /** Konstante f&uuml;r den Status Code CONFLICT */
    static final int CONFLICT = 409;

    /** Konstante f&uuml;r den Status Code PRECONDITION_FAILED */
    static final int PRECONDITION_FAILED = 412;

    /** Konstante f&uuml;r den Status Code REQUEST_TOO_LONG */
    static final int REQUEST_TOO_LONG = 413;

    /** Konstante f&uuml;r den Status Code UNSUPPORTED_MEDIA_TYPE */
    static final int UNSUPPORTED_MEDIA_TYPE = 415;

    /** Konstante f&uuml;r den Status Code MULTI_STATUS */
    static final int MULTI_STATUS = 207;

    /** Konstante f&uuml;r den Status Code INSUFFICIENT_SPACE_ON_RESOURCE */
    static final int INSUFFICIENT_SPACE_ON_RESOURCE = 419;
    
    /** Konstante f&uuml;r den Status Code UNPROCESSABLE_ENTITY */
    static final int UNPROCESSABLE_ENTITY = 422;

    /** Konstante f&uuml;r den Status Code LOCKED */
    static final int LOCKED = 423;
    
    static {
    
        String          result;
        String          string;
        StringTokenizer tokenizer;
    
        Field[]         fields;
    
        int             modifiers;
    
        try {
    
            //das Mapping wird initial eingerichtet
            Status.mapping = new HashMap<Integer, String>();
    
            //alle Felder der Klasse werde ermittelt
            fields = Status.class.getDeclaredFields();
            
            for (Field field : fields) {
                
                string = null;
                
                //die Modifiers zum Feld werden ermittelt
                modifiers = field.getModifiers();
    
                //das Feld muss als public final int deklariert sein
                if (!field.getType().equals(Integer.TYPE)
                    || !Modifier.isFinal(modifiers)
                    || Modifier.isPrivate(modifiers)
                    || !Modifier.isStatic(modifiers)) continue;
    
                tokenizer = new StringTokenizer(string = field.getName(), "_");

                //die Message wird aus dem Namen der Konstanten gebildet
                for (result = ""; tokenizer.hasMoreTokens();) {

                    string = tokenizer.nextToken().toLowerCase().trim();

                    if (string.length() == 0) continue;

                    string = string.substring(0, 1).toUpperCase().concat(string.substring(1));

                    result = result.concat(" ").concat(string).trim();
                }

                if (result.length() == 0) continue;

                //das Mapping wird ueber den Integer und den Text abgelegt
                Status.mapping.put(new Integer(field.getInt(null)), result);
            }
    
        } catch (Exception exception) {

            throw new RuntimeException("Status codes mapping failed", exception);
        }
    }

    /**
     *  Ermittelt den Text zum Serverstatus aus den Konstanten der Klasse.
     *  R&uuml;ckage der ermittelte Text zum Serverstatus, kann dieser nicht
     *  ermittelt werden, wird <code>Unknown</code> zur&uuml;ckgegeben.
     *  @param  status Serverstatus
     *  @return der ermittelte Text zum Serverstatus
     */
    static String getMessage(int status) {
    
        String string;
    
        string = mapping.get(new Integer(status));
    
        return string == null ? "" : string;
    }
}