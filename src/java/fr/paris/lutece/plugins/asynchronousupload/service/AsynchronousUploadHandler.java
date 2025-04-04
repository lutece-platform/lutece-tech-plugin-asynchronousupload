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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    /** contains uploaded file items */
    private static Map<String, Map<String, List<MultipartItem>>> _mapAsynchronousUpload = new ConcurrentHashMap<>( );

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
            String sessionId = getCustomSessionId( request.getSession( ) );
            initMap( sessionId, strFieldName );

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

        String sessionId = getCustomSessionId( session );

        initMap( sessionId, strFieldName );

        // find session-related files in the map
        Map<String, List<MultipartItem>> mapFileItemsSession = _mapAsynchronousUpload.get( sessionId );

        return mapFileItemsSession.get( strFieldName );
    }

    @Override
    public void addFileItemToUploadedFilesList( MultipartItem fileItem, String strFieldName, HttpServletRequest request )
    {
        // This is the name that will be displayed in the form. We keep
        // the original name, but clean it to make it cross-platform.
        String strFileName = UploadUtil.cleanFileName( fileItem.getName( ).trim( ) );

        String sessionId = getCustomSessionId( request.getSession( ) );
        initMap( sessionId, strFieldName );

        // Check if this file has not already been uploaded
        List<MultipartItem> uploadedFiles = getListUploadedFiles( strFieldName, request.getSession( ) );

        if ( uploadedFiles != null )
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

    private void initMap( String strSessionId, String strFieldName )
    {
        // find session-related files in the map
        Map<String, List<MultipartItem>> mapFileItemsSession = _mapAsynchronousUpload.get( strSessionId );

        // create map if not exists
        if ( mapFileItemsSession == null )
        {
            synchronized( this )
            {
                // Ignore double check locking error : assignation and instanciation of objects are separated.
                mapFileItemsSession = _mapAsynchronousUpload.computeIfAbsent( strSessionId, s -> new ConcurrentHashMap<>( ) );
            }
        }

        mapFileItemsSession.computeIfAbsent( strFieldName, s -> new ArrayList<>( ) );
    }

    @Override
    public void removeSessionFiles( HttpSession session )
    {
        String sessionId = (String) session.getAttribute( PARAM_CUSTOM_SESSION_ID );
        if ( sessionId != null )
        {
            _mapAsynchronousUpload.remove( sessionId );
        }

    }
}
