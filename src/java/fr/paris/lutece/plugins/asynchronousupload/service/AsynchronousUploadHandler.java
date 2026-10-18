/*
 * Copyright (c) 2002-2021, City of Paris
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

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fr.paris.lutece.portal.service.util.AppLogService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.upload.MultipartItem;
import fr.paris.lutece.portal.service.util.AppException;
import fr.paris.lutece.util.filesystem.UploadUtil;

@ApplicationScoped
@Named( "asynchronous-upload.asynchronousUploadHandler" )
public class AsynchronousUploadHandler extends AbstractAsynchronousUploadHandler
{

    private static final String HANDLER_NAME = "asynchronousUploadHandler";

    // Error messages
    private static final String ERROR_MESSAGE_UNKNOWN_ERROR = "asynchronousupload.message.unknownError";

    /**
     * Session attribute holding the uploaded files, by field name. The files are kept in the session, not in this instance: a
     * replicated session brings them to the instance that handles the next request, and they go away with the session.
     */
    private static final String SESSION_ATTRIBUTE_UPLOADED_FILES = "asynchronousupload.asynchronousUploadedFiles";

    /**
     * Get the handler
     * 
     * <p>This method is deprecated and is provided for backward compatibility only. 
     * For new code, use dependency injection with {@code @Inject} to obtain the 
     * {@link AsynchronousUploadHandler} instance instead.</p>
     * 
     * 
     * @return the handler
     * 
     * @deprecated Use {@code @Inject} to obtain the {@link AsynchronousUploadHandler} 
     * instance. This method will be removed in future versions.
     */
    @Deprecated
    public static AsynchronousUploadHandler getHandler( )
    {
    	return CDI.current( ).select( AsynchronousUploadHandler.class ) .get( );
    }

    @Override
    public String canUploadFiles( HttpServletRequest request, String strFieldName, List<MultipartItem> listFileItemsToUpload, Locale locale )
    {
        if ( StringUtils.isNotBlank( strFieldName ) )
        {
            getListUploadedFiles( strFieldName, request.getSession( ) );

            return null;
        }
        return I18nService.getLocalizedString( ERROR_MESSAGE_UNKNOWN_ERROR, locale );
    }

    @Override
    public List<MultipartItem> getListUploadedFiles( String strFieldName, HttpSession session )
    {
        if ( StringUtils.isBlank( strFieldName ) )
        {
            throw new AppException( "id field name is not provided for the current file upload" );
        }

        return getSessionFiles( session ).computeIfAbsent( strFieldName, s -> new SessionMultipartItemList( ) );
    }

    @Override
    public void addFileItemToUploadedFilesList( MultipartItem fileItem, String strFieldName, HttpServletRequest request )
    {
        // This is the name that will be displayed in the form. We keep
        // the original name, but clean it to make it cross-platform.
        String strFileName = UploadUtil.cleanFileName( fileItem.getName( ).trim( ) );

        // Check if this file has not already been uploaded
        List<MultipartItem> uploadedFiles = getListUploadedFiles( strFieldName, request.getSession( ) );

        if ( uploadedFiles == null )
        {
            return;
        }

        // Concurrent uploads of the same session to this field on this instance share the list
        synchronized( uploadedFiles )
        {
            boolean bNew = true;

            if ( !uploadedFiles.isEmpty( ) )
            {
                Iterator<MultipartItem> iterUploadedFiles = uploadedFiles.iterator( );

                while ( bNew && iterUploadedFiles.hasNext( ) )
                {
                    MultipartItem uploadedFile = iterUploadedFiles.next( );
                    String strUploadedFileName = UploadUtil.cleanFileName( uploadedFile.getName( ).trim( ) );
                    // If we find a file with the same name and the same
                    // length, we consider that the current file has
                    // already been uploaded
                    bNew = !( StringUtils.equals( strUploadedFileName, strFileName ) && ( uploadedFile.getSize( ) == fileItem.getSize( ) ) );
                }
            }

            if ( bNew )
            {
                uploadedFiles.add( fileItem );
            }
        }
    }

    @Override
    public String getHandlerName( )
    {
        return HANDLER_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeSessionFiles( HttpSession session )
    {
        @SuppressWarnings( "unchecked" )
        Map<String, List<MultipartItem>> mapFileItemsSession = (Map<String, List<MultipartItem>>) session.getAttribute( SESSION_ATTRIBUTE_UPLOADED_FILES );

        if ( mapFileItemsSession == null )
        {
            return;
        }

        for ( List<MultipartItem> fileItems : mapFileItemsSession.values( ) )
        {
            deleteFiles( fileItems );
        }

        session.removeAttribute( SESSION_ATTRIBUTE_UPLOADED_FILES );
    }

    private void deleteFiles( List<MultipartItem> fileItems )
    {
        for ( MultipartItem fileItem : fileItems )
        {
            try
            {
                fileItem.delete( );
            }
            catch ( IOException e )
            {
                AppLogService.error( "Unable to delete uploaded file " + fileItem.getName( ), e );
            }
        }
    }

    /**
     * Gets the uploaded files of the session, by field name. The map is modified in place: the session must be written back
     * after each request that reads it (OpenLiberty sessionCache writeContents=GET_AND_SET_ATTRIBUTES)
     *
     * @param session
     *            the session
     * @return the uploaded files, by field name
     */
    @SuppressWarnings( "unchecked" )
    private Map<String, List<MultipartItem>> getSessionFiles( HttpSession session )
    {
        Map<String, List<MultipartItem>> mapFileItemsSession = (Map<String, List<MultipartItem>>) session.getAttribute( SESSION_ATTRIBUTE_UPLOADED_FILES );

        if ( mapFileItemsSession == null )
        {
            // Concurrent uploads of the same session on this instance must share one map
            synchronized( this )
            {
                mapFileItemsSession = (Map<String, List<MultipartItem>>) session.getAttribute( SESSION_ATTRIBUTE_UPLOADED_FILES );
                if ( mapFileItemsSession == null )
                {
                    mapFileItemsSession = new ConcurrentHashMap<>( );
                    session.setAttribute( SESSION_ATTRIBUTE_UPLOADED_FILES, mapFileItemsSession );
                }
            }
        }

        return mapFileItemsSession;
    }
}
