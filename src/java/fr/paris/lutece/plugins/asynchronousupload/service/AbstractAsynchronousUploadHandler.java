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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.paris.lutece.plugins.asynchronousupload.util.JSONUtils;
import fr.paris.lutece.portal.service.upload.MultipartItem;
import fr.paris.lutece.portal.service.util.AppException;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.web.upload.MultipartHttpServletRequest;

/**
 * AbstractAsynchronousUploadHandler.
 */
public abstract class AbstractAsynchronousUploadHandler implements IAsyncUploadHandler
{
    protected static final String PARAM_CUSTOM_SESSION_ID = "CUSTOM_SESSION";
    private static final String PARAMETER_FIELD_NAME = "fieldname";
    private static final String PARAMETER_FIELD_INDEX = "field_index";
    private static final String PARAMETER_HANDLER = "asynchronousupload.handler";
    private static final String UPLOAD_SUBMIT_PREFIX = "_upload_submit_";
    private static final String UPLOAD_DELETE_PREFIX = "_upload_delete_";
    private static final String UPLOAD_CHECKBOX_PREFIX = "_upload_checkbox_";
    private static final String KEY_FORM_ERROR = "form_error";
    private static final String KEY_FILE_SIZE = "fileSize";
    private static final String KEY_FILE_NAME = "fileName";
    private static final String KEY_FIELD_NAME = "field_name";
    private static final String KEY_FILES = "files";
    private static final String HEADER_CONTENT_RANGE = "Content-Range";
    private static final String REGEXP_CONTENT_RANGE_HEADER = "bytes (\\d*)-(\\d*)\\/(\\d*)";

    @Override
    public void process( HttpServletRequest request, HttpServletResponse response, Map<String, Object> map, List<MultipartItem> listFileItemsToUpload )
    {
        map.clear( );
        String strFieldName = request.getParameter( PARAMETER_FIELD_NAME );

        if ( StringUtils.isBlank( strFieldName ) )
        {
            throw new AppException( "id entry is not provided for the current file upload" );
        }

        if ( CollectionUtils.isNotEmpty( listFileItemsToUpload ) )
        {
            String strError = canUploadFiles( request, strFieldName, listFileItemsToUpload, request.getLocale( ) );
            if ( strError == null )
            {
                // manage chunk file if there is not multiple file
                if ( isManagePartialContent( ) && isRequestContainsPartialContent( request ) )
                {
                    if ( listFileItemsToUpload.size( ) == 1 )
                    {
                        addFileItemToPartialUploadedFilesList( listFileItemsToUpload.get( 0 ), strFieldName, request );
                        if ( isRequestContainsLastPartialContent( request ) )
                        {
                            PartialFileItemGroup partialFileItemGroup = new PartialFileItemGroup(
                                    getListPartialUploadedFiles( strFieldName, request.getSession( ) ) );
                            addFileItemToUploadedFilesList( partialFileItemGroup, strFieldName, request );
                        }
                    }
                    else
                    {
                        AppLogService.error( "AbstractAsynchronousUploadHandler.process : -Chunk files with  multiple file selected do not deal" );
                        map.put( KEY_FORM_ERROR, "Chunk files with  multiple file selected do not deal" );
                    }
                }
                else
                {
                    for ( MultipartItem fileItem : listFileItemsToUpload )
                    {
                        addFileItemToUploadedFilesList( fileItem, strFieldName, request );
                    }
                }
            }
            else
            {
                map.put( KEY_FORM_ERROR, strError );
            }
        }
        map.put( KEY_FIELD_NAME, strFieldName );

        List<MultipartItem> fileItemsSession = getListUploadedFiles( strFieldName, request.getSession( ) );
        List<Map<String, Object>> listJsonFileMap = new ArrayList<>( );
        map.put( KEY_FILES, listJsonFileMap );

        for ( MultipartItem fileItem : fileItemsSession )
        {
            Map<String, Object> jsonFileMap = new HashMap<>( );
            jsonFileMap.put( KEY_FILE_NAME, fileItem.getName( ) );
            jsonFileMap.put( KEY_FILE_SIZE, fileItem.getSize( ) );
            listJsonFileMap.add( jsonFileMap );
        }

    }

    /**
     * Checks the request parameters to see if an upload submit has been called.
     *
     * @param request
     *            the HTTP request
     * @return the name of the upload action, if any. Null otherwise.
     */
    public String getUploadAction( HttpServletRequest request )
    {
        Enumeration<String> enumParamNames = request.getParameterNames( );

        while ( enumParamNames.hasMoreElements( ) )
        {
            String paramName = enumParamNames.nextElement( );

            if ( paramName.startsWith( getUploadSubmitPrefix( ) ) || paramName.startsWith( getUploadDeletePrefix( ) ) )
            {
                return paramName;
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doRemoveFile( HttpServletRequest request, String strFieldName )
    {
        if ( hasRemoveFlag( request, strFieldName ) )
        {
            HttpSession session = request.getSession( false );

            if ( session != null )
            {
                // Some previously uploaded files were deleted
                // Build the prefix of the associated checkboxes
                String strPrefix = getUploadCheckboxPrefix( ) + strFieldName;

                // Look for the checkboxes in the request
                Enumeration<String> enumParamNames = request.getParameterNames( );
                List<Integer> listIndexes = new ArrayList<>( );

                while ( enumParamNames.hasMoreElements( ) )
                {
                    String strParamName = enumParamNames.nextElement( );
                    String strParamValue = request.getParameter( strParamName );

                    if ( strParamValue.startsWith( strPrefix ) )
                    {
                        // Get the index from the name of the checkbox
                        listIndexes.add( Integer.parseInt( strParamValue.substring( strPrefix.length( ) ) ) );
                    }
                }

                Collections.sort( listIndexes );
                Collections.reverse( listIndexes );

                for ( int nIndex : listIndexes )
                {
                    removeFileItem( strFieldName, session, nIndex );
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String doRemoveUploadedFile( HttpServletRequest request, String strFieldName, List<Integer> listIndexesFilesToRemove )
    {
        if ( StringUtils.isBlank( strFieldName ) )
        {
            return JSONUtils.buildJsonErrorRemovingFile( request ).toString( );
        }

        ObjectMapper mapper = new ObjectMapper( );
        if ( CollectionUtils.isNotEmpty( listIndexesFilesToRemove ) )
        {
            // parse json
            JsonNode jsonFieldIndexes = mapper.valueToTree( listIndexesFilesToRemove );

            if ( !jsonFieldIndexes.isArray( ) )
            {
                return JSONUtils.buildJsonErrorRemovingFile( request ).toString( );
            }

            ArrayNode jsonArrayFieldIndexers = (ArrayNode) jsonFieldIndexes;
            int [ ] tabFieldIndex = new int [ jsonArrayFieldIndexers.size( )];

            for ( int nIndex = 0; nIndex < jsonArrayFieldIndexers.size( ); nIndex++ )
            {
                try
                {
                    tabFieldIndex [nIndex] = Integer.parseInt( jsonArrayFieldIndexers.get( nIndex ).asText( ) );
                }
                catch( NumberFormatException nfe )
                {
                    return JSONUtils.buildJsonErrorRemovingFile( request ).toString( );
                }
            }

            // inverse order (removing using index - remove greater first to keep order)
            Arrays.sort( tabFieldIndex );
            ArrayUtils.reverse( tabFieldIndex );

            List<MultipartItem> fileItemsSession = getListUploadedFiles( strFieldName, request.getSession( ) );

            List<MultipartItem> listItemsToRemove = new ArrayList<>( listIndexesFilesToRemove.size( ) );

            for ( int nFieldIndex : tabFieldIndex )
            {
                if ( fileItemsSession.size( ) == 1 && nFieldIndex > 0 )
                {
                    nFieldIndex = nFieldIndex - 1;
                }
                listItemsToRemove.add( fileItemsSession.get( nFieldIndex ) );
                removeFileItem( strFieldName, request.getSession( ), nFieldIndex );
            }
        }

        ObjectNode json = mapper.createObjectNode( );
        json.put( JSONUtils.JSON_KEY_SUCCESS, JSONUtils.JSON_KEY_SUCCESS );

        json.setAll( JSONUtils.getUploadedFileJSON( getListUploadedFiles( strFieldName, request.getSession( ) ) ) );
        json.put( JSONUtils.JSON_KEY_FIELD_NAME, strFieldName );

        return json.toString( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasRemoveFlag( HttpServletRequest request, String strFieldName )
    {
        return StringUtils.isNotEmpty( request.getParameter( getUploadDeletePrefix( ) + strFieldName ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInvoked( HttpServletRequest request )
    {
        return StringUtils.equals( getHandlerName( ), request.getParameter( PARAMETER_HANDLER ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasAddFileFlag( HttpServletRequest request, String strFieldName )
    {
        return StringUtils.isNotEmpty( request.getParameter( getUploadSubmitPrefix( ) + strFieldName ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addFilesUploadedSynchronously( HttpServletRequest request, String strFieldName )
    {
        if ( request instanceof MultipartHttpServletRequest && hasAddFileFlag( request, strFieldName ) )
        {
            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
            List<MultipartItem> listMultipartItem = multipartRequest.getFileList( strFieldName );

            if ( CollectionUtils.isNotEmpty( listMultipartItem ) )
            {
                for ( MultipartItem fileItem : listMultipartItem )
                {
                    if ( ( fileItem.getSize( ) > 0L ) && StringUtils.isNotEmpty( fileItem.getName( ) ) )
                    {
                        addFileItemToUploadedFilesList( fileItem, strFieldName, request );
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUploadSubmitPrefix( )
    {
        return getHandlerName( ) + UPLOAD_SUBMIT_PREFIX;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUploadDeletePrefix( )
    {
        return getHandlerName( ) + UPLOAD_DELETE_PREFIX;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUploadCheckboxPrefix( )
    {
        return getHandlerName( ) + UPLOAD_CHECKBOX_PREFIX;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte [ ] doRetrieveUploadedFile( HttpServletRequest request )
    {
        String strFieldName = request.getParameter( PARAMETER_FIELD_NAME );
        String strFieldIndex = request.getParameter( PARAMETER_FIELD_INDEX );
        int intFieldIndex;
        MultipartItem itemToDownload = null;
        if ( StringUtils.isNotEmpty( strFieldIndex ) && StringUtils.isNumeric( strFieldIndex ) )
        {
            intFieldIndex = Integer.parseInt( request.getParameter( PARAMETER_FIELD_INDEX ) );
            List<MultipartItem> fileItemsSession = getListUploadedFiles( strFieldName, request.getSession( ) );
            itemToDownload = fileItemsSession.get( intFieldIndex );
        }
        if ( itemToDownload == null )
        {
            return new byte [ 0];
        }
        return itemToDownload.get( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MultipartItem> getListPartialUploadedFiles( String strFieldName, HttpSession session )
    {
        AppLogService.error( "the Upload Handler do not manage partial content files " );
        return new ArrayList<>( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addFileItemToPartialUploadedFilesList( MultipartItem fileItem, String strFieldName, HttpServletRequest request )
    {
        AppLogService.error( "the Upload Handler do not manage partial content files " );
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public boolean isManagePartialContent( )
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAllFileItem( HttpSession session )
    {
    }

    /**
     * return true if the content of the request is partial
     * 
     * @param request
     *            the request
     * @return true if the content of the request is partial
     */
    private boolean isRequestContainsPartialContent( HttpServletRequest request )
    {
        return request.getHeader( HEADER_CONTENT_RANGE ) != null;

    }

    /**
     * return true if the request contain the last partial content of the file
     * 
     * @param request
     * @return
     */
    private boolean isRequestContainsLastPartialContent( HttpServletRequest request )
    {

        boolean bLastPartialContent = false;
        String strContentRange = request.getHeader( HEADER_CONTENT_RANGE );
        Pattern r = Pattern.compile( REGEXP_CONTENT_RANGE_HEADER );
        Matcher m = r.matcher( strContentRange );

        if ( m.find( ) )
        {

            String strLatsBytes = m.group( 2 );
            String strTotalBytes = m.group( 3 );
            int nTotalBytes = Integer.parseInt( strTotalBytes );
            int nLastBytes = Integer.parseInt( strLatsBytes );
            if ( nTotalBytes - nLastBytes == 1 )
            {
                bLastPartialContent = true;

            }

        }
        return bLastPartialContent;
    }

    protected String getCustomSessionId( HttpSession session )
    {
        String sessionId = (String) session.getAttribute( PARAM_CUSTOM_SESSION_ID );
        if ( sessionId == null )
        {
            sessionId = UUID.randomUUID( ).toString( );
            session.setAttribute( PARAM_CUSTOM_SESSION_ID, sessionId );
        }
        return sessionId;
    }

    @Override
    public void removeFileItem( String strFieldName, HttpSession session, int nIndex )
    {
        // Remove the file (this will also delete the file physically)
        List<MultipartItem> uploadedFiles = getListUploadedFiles( strFieldName, session );

        if ( ( uploadedFiles != null ) && !uploadedFiles.isEmpty( ) && ( uploadedFiles.size( ) > nIndex ) )
        {
            // Remove the object from the Hashmap
            MultipartItem fileItem = uploadedFiles.remove( nIndex );
            try
            {
            	fileItem.delete( );
            }
            catch( IOException e )
            {
            	AppLogService.error( e.getMessage( ), e );
            }
            
        }
    }
}
