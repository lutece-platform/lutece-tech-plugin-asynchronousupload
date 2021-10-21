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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.spring.SpringContextService;
import fr.paris.lutece.portal.service.util.AppException;
import fr.paris.lutece.util.filesystem.UploadUtil;

public class AsynchronousUploadHandler extends AbstractAsynchronousUploadHandler
{

    private static final String BEAN_NAME = "asynchronous-upload.asynchronousUploadHandler";
    private static final String HANDLER_NAME = "asynchronousUploadHandler";

    // Error messages
    private static final String ERROR_MESSAGE_UNKNOWN_ERROR = "asynchronousupload.message.unknownError";

    /** contains uploaded file items */
    private static Map<String, Map<String, List<FileItem>>> _mapAsynchronousUpload = new ConcurrentHashMap<>( );

    /**
     * Get the handler
     * 
     * @return the handler
     */
    public static AsynchronousUploadHandler getHandler( )
    {
        return SpringContextService.getBean( BEAN_NAME );
    }

    @Override
    public String canUploadFiles( HttpServletRequest request, String strFieldName, List<FileItem> listFileItemsToUpload, Locale locale )
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
    public List<FileItem> getListUploadedFiles( String strFieldName, HttpSession session )
    {
        if ( StringUtils.isBlank( strFieldName ) )
        {
            throw new AppException( "id field name is not provided for the current file upload" );
        }

        String sessionId = getCustomSessionId( session );

        initMap( sessionId, strFieldName );

        // find session-related files in the map
        Map<String, List<FileItem>> mapFileItemsSession = _mapAsynchronousUpload.get( sessionId );

        return mapFileItemsSession.get( strFieldName );
    }

    @Override
    public void addFileItemToUploadedFilesList( FileItem fileItem, String strFieldName, HttpServletRequest request )
    {
        // This is the name that will be displayed in the form. We keep
        // the original name, but clean it to make it cross-platform.
        String strFileName = UploadUtil.cleanFileName( fileItem.getName( ).trim( ) );

        String sessionId = getCustomSessionId( request.getSession( ) );
        initMap( sessionId, strFieldName );

        // Check if this file has not already been uploaded
        List<FileItem> uploadedFiles = getListUploadedFiles( strFieldName, request.getSession( ) );

        if ( uploadedFiles != null )
        {
            boolean bNew = true;

            if ( !uploadedFiles.isEmpty( ) )
            {
                Iterator<FileItem> iterUploadedFiles = uploadedFiles.iterator( );

                while ( bNew && iterUploadedFiles.hasNext( ) )
                {
                    FileItem uploadedFile = iterUploadedFiles.next( );
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
        Map<String, List<FileItem>> mapFileItemsSession = _mapAsynchronousUpload.get( strSessionId );

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
