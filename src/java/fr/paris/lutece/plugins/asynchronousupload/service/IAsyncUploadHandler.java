/*
 * Copyright (c) 2002-2014, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.asynchronousupload.service;

import fr.paris.lutece.portal.web.upload.IAsynchronousUploadHandler;

import org.apache.commons.fileupload.FileItem;

import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;


/**
 * Interface for asynchronous upload handlers used by entries of type upload.
 * Handler must be declared as Spring beans.
 */
public interface IAsyncUploadHandler extends IAsynchronousUploadHandler
{
    /**
     * Get the upload submit prefix
     * @return The upload submit prefix
     */
    String getUploadSubmitPrefix(  );

    /**
     * Get the upload delete prefix
     * @return The upload delete prefix
     */
    String getUploadDeletePrefix(  );

    /**
     * Get the upload checkbox prefix
     * @return The upload checkbox prefix
     */
    String getUploadCheckboxPrefix(  );

    /**
     * Check if the file can be uploaded or not.
     * This method will check the size of each file and the number max of files
     * that can be uploaded.
     * @param request The request
     * @param strFieldName the field name
     * @param listFileItemsToUpload the list of files to upload
     * @param locale the locale
     * @return The error if any, or null if the files can be uploaded.
     */
    String canUploadFiles( HttpServletRequest request, String strFieldName, List<FileItem> listFileItemsToUpload,
        Locale locale );

    /**
     * Get the list of uploaded files for a given field name
     * @param request the request
     * @param strFieldName the field name
     * @return The list of uploaded files
     */
    List<FileItem> getListUploadedFiles( HttpServletRequest request, String strFieldName );

    /**
     * Remove file Item
     * @param strFieldName the field name
     * @param strSessionId The id of the session of the current user
     * @param nIndex the index
     */
    void removeFileItem( String strFieldName, String strSessionId, int nIndex );

    /**
     * Gets the fileItem for the entry and the given session.
     * @param strFieldName the field name
     * @param strSessionId The id of the session of the current user
     * @return the fileItem found, <code>null</code> otherwise.
     */
    List<FileItem> getFileItems( String strFieldName, String strSessionId );

    /**
     * Add file item to the list of uploaded files
     * @param fileItem the file item
     * @param strIdEntry the id entry
     * @param request the request
     */
    void addFileItemToUploadedFile( FileItem fileItem, String strIdEntry, HttpServletRequest request );

    /**
     * Check if the request has a remove flag for a given entry.
     * @param request The request
     * @param strFieldName The field name
     * @return True if the request has a remove flag for the given entry, false
     *         otherwise
     */
    boolean hasRemoveFlag( HttpServletRequest request, String strFieldName );

    /**
     * Do remove a file of a given entry if a flag is present in the request.
     * Files are selected from the request.
     * @param request The request
     * @param strFieldName The field name
     */
    void doRemoveFile( HttpServletRequest request, String strFieldName );

    /**
     * Do remove a list of files from uploaded files.
     * @param request The request
     * @param strFieldName The field name of the files to remove
     * @param listIndexesFilesToRemove The list of indexes of files to remove
     * @return The result JSON
     */
    public String doRemoveUploadedFile( HttpServletRequest request, String strFieldName,
        List<Integer> listIndexesFilesToRemove );

    /**
     * Get the name of the handler. The name of the handler must be unique. The
     * handler name must only contain alphabetic characters (no point, no space,
     * no coma, no #, ...).
     * @return The name of the handler
     */
    public String getHandlerName(  );
}
