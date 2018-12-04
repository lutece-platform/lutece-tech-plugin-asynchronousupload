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

import fr.paris.lutece.plugins.asynchronousupload.util.JSONUtils;
import fr.paris.lutece.portal.service.util.AppException;
import fr.paris.lutece.portal.web.upload.MultipartHttpServletRequest;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


/**
 * AbstractAsynchronousUploadHandler.
 */
public abstract class AbstractAsynchronousUploadHandler implements IAsyncUploadHandler
{
    private static final String PARAMETER_FIELD_NAME = "fieldname";
    private static final String PARAMETER_FIELD_INDEX = "field_index";
    private static final String PARAMETER_HANDLER = "asynchronousupload.handler";
    private static final String UPLOAD_SUBMIT_PREFIX = "_upload_submit_";
    private static final String UPLOAD_DELETE_PREFIX = "_upload_delete_";
    private static final String UPLOAD_CHECKBOX_PREFIX = "_upload_checkbox_";

    /**
     * {@inheritDoc}
     */
    @Override
    public void process( HttpServletRequest request, HttpServletResponse response, JSONObject mainObject,
        List<FileItem> listFileItemsToUpload )
    {
        mainObject.clear(  );

        String strFieldName = request.getParameter( PARAMETER_FIELD_NAME );

        if ( StringUtils.isBlank( strFieldName ) )
        {
            throw new AppException( "id entry is not provided for the current file upload" );
        }

        if ( ( listFileItemsToUpload != null ) && !listFileItemsToUpload.isEmpty(  ) )
        {
            String strError = canUploadFiles( request, strFieldName, listFileItemsToUpload, request.getLocale(  ) );

            if ( strError == null )
            {
                for ( FileItem fileItem : listFileItemsToUpload )
                {
                    addFileItemToUploadedFilesList( fileItem, strFieldName, request );
                }
            }
            else
            {
                JSONUtils.buildJsonError( mainObject, strError );
            }
        }

        List<FileItem> fileItemsSession = getListUploadedFiles( strFieldName, request.getSession(  ) );

        JSONObject jsonListFileItems = JSONUtils.getUploadedFileJSON( fileItemsSession );
        mainObject.accumulateAll( jsonListFileItems );
        // add entry id to json
        mainObject.element( JSONUtils.JSON_KEY_FIELD_NAME, strFieldName );
    }

    /**
     * Checks the request parameters to see if an upload submit has been
     * called.
     *
     * @param request the HTTP request
     * @return the name of the upload action, if any. Null otherwise.
     */
    public String getUploadAction( HttpServletRequest request )
    {
        Enumeration<String> enumParamNames = request.getParameterNames(  );

        while ( enumParamNames.hasMoreElements(  ) )
        {
            String paramName = enumParamNames.nextElement(  );

            if ( paramName.startsWith( getUploadSubmitPrefix(  ) ) ||
                    paramName.startsWith( getUploadDeletePrefix(  ) ) )
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
                String strPrefix = getUploadCheckboxPrefix(  ) + strFieldName;

                // Look for the checkboxes in the request
                Enumeration<String> enumParamNames = request.getParameterNames(  );
                List<Integer> listIndexes = new ArrayList<Integer>(  );

                while ( enumParamNames.hasMoreElements(  ) )
                {
                    String strParamName = enumParamNames.nextElement(  );
                    String strParamValue = request.getParameter(strParamName);

                    if ( strParamValue.startsWith( strPrefix ) )
                    {
                        // Get the index from the name of the checkbox
                        listIndexes.add( Integer.parseInt( strParamValue.substring( strPrefix.length(  ) ) ) );
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
    public String doRemoveUploadedFile( HttpServletRequest request, String strFieldName,
        List<Integer> listIndexesFilesToRemove )
    {
        if ( StringUtils.isBlank( strFieldName ) )
        {
            return JSONUtils.buildJsonErrorRemovingFile( request ).toString(  );
        }

        if ( ( listIndexesFilesToRemove != null ) && ( listIndexesFilesToRemove.size(  ) != 0 ) )
        {
            // parse json
            JSON jsonFieldIndexes = JSONSerializer.toJSON( listIndexesFilesToRemove );

            if ( !jsonFieldIndexes.isArray(  ) )
            {
                return JSONUtils.buildJsonErrorRemovingFile( request ).toString(  );
            }

            JSONArray jsonArrayFieldIndexers = (JSONArray) jsonFieldIndexes;
            int[] tabFieldIndex = new int[jsonArrayFieldIndexers.size(  )];

            for ( int nIndex = 0; nIndex < jsonArrayFieldIndexers.size(  ); nIndex++ )
            {
                try
                {
                    tabFieldIndex[nIndex] = Integer.parseInt( jsonArrayFieldIndexers.getString( nIndex ) );
                }
                catch ( NumberFormatException nfe )
                {
                    return JSONUtils.buildJsonErrorRemovingFile( request ).toString(  );
                }
            }

            // inverse order (removing using index - remove greater first to keep order)
            Arrays.sort( tabFieldIndex );
            ArrayUtils.reverse( tabFieldIndex );

            List<FileItem> fileItemsSession = getListUploadedFiles( strFieldName, request.getSession(  ) );

            List<FileItem> listItemsToRemove = new ArrayList<FileItem>( listIndexesFilesToRemove.size(  ) );

            for ( int nFieldIndex : tabFieldIndex )
            {	
            	if(fileItemsSession.size() == 1 && nFieldIndex > 0){
            		nFieldIndex= nFieldIndex - 1;
            	}
                listItemsToRemove.add( fileItemsSession.get( nFieldIndex ) );
                removeFileItem( strFieldName, request.getSession(  ), nFieldIndex );
            }
        }

        JSONObject json = new JSONObject(  );
        json.element( JSONUtils.JSON_KEY_SUCCESS, JSONUtils.JSON_KEY_SUCCESS );

        json.accumulateAll( JSONUtils.getUploadedFileJSON( getListUploadedFiles( strFieldName, request.getSession(  ) ) ) );
        json.element( JSONUtils.JSON_KEY_FIELD_NAME, strFieldName );

        return json.toString(  );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasRemoveFlag( HttpServletRequest request, String strFieldName )
    {
        return StringUtils.isNotEmpty( request.getParameter( getUploadDeletePrefix(  ) + strFieldName ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInvoked( HttpServletRequest request )
    {
        return StringUtils.equals( getHandlerName(  ), request.getParameter( PARAMETER_HANDLER ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasAddFileFlag( HttpServletRequest request, String strFieldName )
    {
        return StringUtils.isNotEmpty( request.getParameter( getUploadSubmitPrefix(  ) + strFieldName ) );
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
            List<FileItem> listFileItem = multipartRequest.getFileList( strFieldName );

            if ( ( listFileItem != null ) && ( listFileItem.size(  ) > 0 ) )
            {
                for ( FileItem fileItem : listFileItem )
                {
                    if ( ( fileItem.getSize(  ) > 0L ) && StringUtils.isNotEmpty( fileItem.getName(  ) ) )
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
    public String getUploadSubmitPrefix(  )
    {
        return getHandlerName(  ) + UPLOAD_SUBMIT_PREFIX;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUploadDeletePrefix(  )
    {
        return getHandlerName(  ) + UPLOAD_DELETE_PREFIX;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUploadCheckboxPrefix(  )
    {
        return getHandlerName(  ) + UPLOAD_CHECKBOX_PREFIX;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] doRetrieveUploadedFile( HttpServletRequest request )
    {
        String strFieldName = request.getParameter( PARAMETER_FIELD_NAME );
        String strFieldIndex = request.getParameter( PARAMETER_FIELD_INDEX );
        int intFieldIndex;
        FileItem itemToDownload = null;
        if ( StringUtils.isNotEmpty( strFieldIndex ) &&  StringUtils.isNumeric( strFieldIndex ) )
        {
            intFieldIndex = Integer.parseInt(request.getParameter( PARAMETER_FIELD_INDEX ));
            List<FileItem> fileItemsSession = getListUploadedFiles(strFieldName, request.getSession());
            itemToDownload = fileItemsSession.get(intFieldIndex);
        }
        return itemToDownload.get();
    }
}
