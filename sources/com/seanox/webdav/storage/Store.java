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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.security.Principal;
import java.util.Date;
import java.util.Map;

/**
 *  Interface for simple implementation of any store for the WebDav.<br>
 *  <br>
 *  This file based on the BasicWebdavStore from Oliver Zeigermann, that was
 *  part of the Webdav Construcktion Kit from slide IWebdavStorage.<br>
 *  <br>
 *  Store 1.2014.0202<br>
 *  Copyright (C) 2014 Seanox Software Solutions<br>
 *  Alle Rechte vorbehalten.
 *
 *  @author  Seanox Software Solutions
 *  @version 1.2014.0202
 */
public interface Store extends Cloneable {
    
    /**
     *  Indicates that a new request or transaction with this store involved has
     *  been started. The request will be terminated by either {@link #commit()}
     *  or {@link #rollback()}. If only non-read methods have been called, the
     *  request will be terminated by a {@link #commit()}. This method will be
     *  called by (@link WebdavStoreAdapter} at the beginning of each request.
     *  @param  principal  the principal that started this request or
     *                     <code>null</code> if there is non available
     *  @param  parameters Map containing the parameters
     *  @return an own instance for transaction 
     *  @throws Exception if something goes wrong on the store level
     */
    public Store share(Principal principal, Map<String, String> parameters) throws Exception;

    /**
     *  Checks if authentication information passed in
     *  {@link #share(Principal, Map)} is valid. If not throws an
     *  exception.
     *  @throws SecurityException if authentication is not valid
     *  @throws IOException if something goes wrong on the store level
     */
    public void checkAuthentication() throws SecurityException, IOException;

    /**
     *  Indicates that all changes done inside this request shall be made
     *  permanent and any transactions, connections and other temporary
     *  resources shall be terminated.
     *  @throws IOException if something goes wrong on the store level
     */
    public void commit() throws IOException;

    /**
     *  Indicates that all changes done inside this request shall be undone and
     *  any transactions, connections and other temporary resources shall be
     *  terminated.
     *  @throws IOException if something goes wrong on the store level
     */
    public void rollback() throws IOException;

    /**
     *  Checks if there is an object at the position specified by
     *  <code>uri</code>.
     *  @param  uri URI of the object to check
     *  @return <code>true</code> if the object at <code>uri</code> exists
     *  @throws IOException if something goes wrong on the store level
     */
    public boolean existsObject(String uri) throws IOException;

    /**
     *  Checks if there is an object at the position specified by
     *  <code>uri</code> and if so if it is a folder.
     *  @param  uri URI of the object to check
     *  @return <code>true</code> if the object at <code>uri</code> exists and
     *          is a folder
     *  @throws IOException if something goes wrong on the store level
     */
    public boolean isFolder(String uri) throws IOException;

    /**
     *  Checks if there is an object at the position specified by
     *  <code>uri</code> and if so if it is a content resource.
     *  @param  uri URI of the object to check
     *  @return <code>true</code> if the object at <code>uri</code> exists
     *          and is a content resource
     *  @throws IOException if something goes wrong on the store level
     */
    public boolean isResource(String uri) throws IOException;

    /**
     *  Creates a folder at the position specified by <code>uri</code>.
     *  If the resource exists but is a resource rather than a regular folder,
     *  does not exist but cannot be created, or any other reason then a
     *  <code>IOException</code> is thrown.
     *  @param  uri URI of the folder
     *  @throws IOException if something goes wrong on the store level
     */
    public void createFolder(String uri) throws IOException;

    /**
     *  Creates a content resource at the position specified by
     *  <code>resourceUri</code>. If the resource exists but is a folder rather
     *  than a regular resource, does not exist but cannot be created, or cannot
     *  be opened for any other reason then a <code>IOException</code> is thrown.
     *  @param  uri URI of the content resource
     *  @throws IOException if something goes wrong on the store level
     */
    public void createResource(String uri) throws IOException;

    /**
     *  Sets / stores the content of the resource specified by <code>uri</code>.
     *  @param uri      URI of the resource where the content will be stored
     *  @param content  input stream from which the content will be read from
     *  @param type     content type of the resource or <code>null</code> if
     *                  unknown
     *  @param encoding character encoding of the resource or <code>null</code>
     *                  if unknown or not applicable
     *  @throws IOException if something goes wrong on the store level
     */
    public void setResourceContent(String uri, InputStream content,
        String type, String encoding) throws IOException;

    /**
     *  Gets the date of the last modiciation of the object specified by
     *  <code>uri</code>.
     *  @param  uri URI of the object, i.e. content resource or folder
     *  @return date of last modification, <code>null</code> declares this
     *          value as invalid and asks the adapter to try to set it from the
     *          properties if possible
     *  @throws IOException if something goes wrong on the store level
     */
    public Date getLastModified(String uri) throws IOException;
    
    /**
     *  Sets the attribute "lastModified" for the resource at "path".
     *  @param  uri  what resource to check for read only
     *  @param  time value of attribute "LastModified"
     *  @throws IOException if something goes wrong on the store level
     */
    public void setLastModified(String uri, Date time) throws IOException;

    /**
     *  Gets the time of creation of the object specified by <code>uri</code>.
     *  @param  uri URI of the object, i.e. content resource or folder
     *  @return date of creation, <code>null</code> declares this value as
     *          invalid and asks the adapter to try to set it from the
     *          properties if possible
     *  @throws IOException if something goes wrong on the store level
     */
    public Date getCreationTime(String uri) throws IOException;
    
    /**
     *  Sets the attribute "CreationTime" for the resource at "path".
     *  @param  uri  what resource to check for read only
     *  @param  time value of attribute "CreationTime"
     *  @throws IOException if something goes wrong on the store level
     */
    public void setCreationTime(String uri, Date time) throws IOException;
    
    /**
     *  Gets the time of last access of the object specified by <code>uri</code>.
     *  @param  uri  URI of the object, i.e. content resource or folder
     *  @return time of creation, <code>null</code> declares this value as
     *          invalid and asks the adapter to try to set it from the
     *          properties if possible
     *  @throws IOException if something goes wrong on the store level
     */
    public Date getLastAccessTime(String uri) throws IOException;
    
    /**
     *  Sets the attribute "LastAccessTime" for the resource at "path".
     *  @param  uri  what resource to check for read only
     *  @param  time value of attribute "LastAccessTime"
     *  @throws IOException if something goes wrong on the store level
     */
    public void setLastAccessTime(String uri, Date time) throws IOException;    

    /**
     *  Gets the names of the children of the folder specified by <code>uri</code>.
     *  @param  uri URI of the folder
     *  @return array containing names of the children or null if it is no folder
     *  @throws IOException if something goes wrong on the store level
     */
    public String[] getChildrenNames(String uri) throws IOException;

    /**
     *  Gets the content of the resource specified by <code>resourceUri</code>.
     *  @param  uri   URI of the content resource
     *  @return input stream you can read the content of the resource from
     *  @throws IOException if something goes wrong on the store level
     */
    public InputStream getResourceContent(String uri) throws IOException;

    /**
     *  Gets the length of the content resource specified by <code>uri</code>.
     *  @param  uri URI of the content resource
     *  @return length of the resource in bytes, <code>-1</code> declares this
     *          value as invalid and asks the adapter to try to set it from the
     *          properties if possible
     *  @throws IOException if something goes wrong on the store level
     */
    public long getResourceLength(String uri) throws IOException;

    /**
     *  Gets the eTag of the resource specified by <code>uri</code>.
     *  @param  uri URI of the content resource
     *  @return eTag of the resource in combination of the content and last
     *          modiciation of the resource specified by <code>uri</code>
     *  @throws IOException if something goes wrong on the store level
     */
    public String getETag(String uri) throws IOException;

    /**
     *  Move or rename a object specified by <code>uri</code> to a destination.
     *  @param  uri URI of the object, i.e. content resource or folder
     *  @param  destination the path to the destination
     *  @param  options options specifying how the move should be done  
     *  @throws IOException if something goes wrong on the store level
     */
    public void moveObject(String uri, String destination, CopyOption... options) throws IOException;    

    /**
     *  Copy a object specified by <code>uri</code> to a destination.
     *  @param  uri URI of the object, i.e. content resource or folder
     *  @param  destination the path to the destination
     *  @param  options options specifying how the move should be done  
     *  @throws IOException if something goes wrong on the store level
     */
    public void copyObject(String uri, String destination, CopyOption... options) throws IOException;

    /**
     *  Removes the object specified by <code>uri</code>.
     *  If this uri denotes a folder, then the folder must be empty in order to
     *  be removed. 
     *  @param  uri URI of the object, i.e. content resource or folder
     *  @throws IOException if something goes wrong on the store level
     */
    public void removeObject(String uri) throws IOException;
    

    /**
     *  Trys to lock the resource at "path".
     *  @param  uri what resource to lock
     *  @return <code>true</code> if the resource at path was successfully
     *          locked, false if an existing lock prevented this
     *  @throws IOException if something goes wrong on the store level
     */
    public boolean lock(String uri) throws IOException;

    /**
     *  Trys to lock the resource at "path".
     *  @param  uri       what resource to lock
     *  @param  exclusive if the lock should be exclusive (or shared)
     *  @return <code>true</code> if the resource at path was successfully
     *          locked, false if an existing lock prevented this
     *  @throws IOException if something goes wrong on the store level
     */
    public boolean lock(String uri, boolean exclusive) throws IOException;

    /**
     *  Unlocks all resources at "path" (and all subfolders if existing).
     *  That have the same signature.
     *  @param  uri what resource to unlock
     *  @throws IOException if something goes wrong on the store level
     */
    public void unlock(String uri) throws IOException;

    /**
     *  Check to see if a resource is currently write locked. The method will
     *  look at the "If" header to make sure the client has give the appropriate
     *  lock tokens.
     *  @param  uri what resource to check of lock
     *  @return <code>true</code> if the resource is locked (and no appropriate
     *          lock token has been found for at least one of the non-shared
     *          locks which are present on the resource).
     *  @throws IOException if something goes wrong on the store level
     */
    public boolean isLocked(String uri) throws IOException;

    /**
     *  Check to see if a resource is currently write locked. The method will
     *  look at the "If" header to make sure the client has give the appropriate
     *  lock tokens.
     *  @param  uri       what resource to check of lock
     *  @param  exclusive if the lock should be exclusive (or shared)
     *  @return <code>true</code> if the resource is locked (and no appropriate
     *          lock token has been found for at least one of the non-shared
     *          locks which are present on the resource).
     *  @throws IOException if something goes wrong on the store level
     */
    public boolean isLocked(String uri, boolean exclusive) throws IOException;

    /**
     *  Check whether the resource at "path" is hidden.
     *  @param  uri what resource to check for hidden
     *  @return <code>true</code> if and only if the resource specified by this
     *          path exists <em>and</em> is hidden for the application;
     *          <code>false</code> otherwise
     *  @throws IOException if something goes wrong on the store level
     */
    public boolean isHidden(String uri) throws IOException;
    
    /**
     *  Sets the attribute "Hidden" for the resource at "path".
     *  @param  uri    what resource to check for hidden
     *  @param  hidden value of attribute "Hidden"
     *  @throws IOException if something goes wrong on the store level
     */
    public void setHidden(String uri, boolean hidden) throws IOException;
    
    /**
     *  Check whether the resource at "path" is archive.
     *  @param  uri what resource to check for archive
     *  @return <code>true</code> if and only if the resource specified by this
     *          path exists <em>and</em> is archive for the application;
     *          <code>false</code> otherwise
     *  @throws IOException if something goes wrong on the store level
     */
    public boolean isArchive(String uri) throws IOException;
    
    /**
     *  Sets the attribute "Archive" for the resource at "path".
     *  @param  uri     what resource to check for archive
     *  @param  archive value of attribute "Archive"
     *  @throws IOException if something goes wrong on the store level
     */
    public void setArchive(String uri, boolean archive) throws IOException;
    
    /**
     *  Check whether the resource at "path" is system.
     *  @param  uri what resource to check for system
     *  @return <code>true</code> if and only if the resource specified by this
     *          path exists <em>and</em> is system for the application;
     *          <code>false</code> otherwise
     *  @throws IOException if something goes wrong on the store level
     */
    public boolean isSystem(String uri) throws IOException;
    
    /**
     *  Sets the attribute "System" for the resource at "path".
     *  @param  uri    what resource to check for system
     *  @param  system value of attribute "System"
     *  @throws IOException if something goes wrong on the store level
     */
    public void setSystem(String uri, boolean system) throws IOException;

    /**
     *  Check whether the application can read the resource at "path".
     *  @param  uri what resource to check for read
     *  @return <code>true</code> if and only if the resource specified by this
     *          path exists <em>and</em> can be read by the application;
     *          <code>false</code> otherwise
     *  @throws IOException if something goes wrong on the store level
     */
    public boolean canRead(String uri) throws IOException;

    /**
     *  Check whether the application can write the resource at "path".
     *  @param  uri what resource to check for write
     *  @return <code>true</code> if and only if the resource specified by this
     *          path exists <em>and</em> can be write by the application;
     *          <code>false</code> otherwise
     *  @throws IOException if something goes wrong on the store level
     */
    public boolean canWrite(String uri) throws IOException;

    /**
     *  Check whether the application can write at store.
     *  @return <code>true</code> if application can not write at store;
     *          <code>false</code> otherwise
     *  @throws IOException if something goes wrong on the store level
     */
    public boolean isReadOnly() throws IOException;
    
    /**
     *  Check whether the resource at "path" is read only.
     *  @param  uri what resource to check for read only
     *  @return <code>true</code> the resource is read only;
     *          <code>false</code> otherwise
     *  @throws IOException if something goes wrong on the store level
     */
    public boolean isReadOnly(String uri) throws IOException;

    /**
     *  Sets the attribute "ReadOnly" for the resource at "path".
     *  @param  uri      what resource to check for read only
     *  @param  readOnly value of attribute "ReadOnly"
     *  @throws IOException if something goes wrong on the store level
     */
    public void setReadOnly(String uri, boolean readOnly) throws IOException;
    
    /**
     *  Close the store and discards all changes, and not confirmed transactions.
     *  @throws IOException if something goes wrong on the store level
     */
    public void close() throws IOException;
}