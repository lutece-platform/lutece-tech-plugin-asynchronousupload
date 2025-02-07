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
package fr.paris.lutece.plugins.asynchronousupload.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.plugins.asynchronousupload.service.IAsyncUploadHandler;
import fr.paris.lutece.plugins.asynchronousupload.service.UploadCacheService;
import fr.paris.lutece.portal.service.spring.SpringContextService;
import fr.paris.lutece.portal.service.template.AppTemplateService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.portal.util.mvc.xpage.MVCApplication;
import fr.paris.lutece.util.html.HtmlTemplate;

/**
 * Upload application
 */
public class AsynchronousUploadApp extends MVCApplication
{
    private static final long serialVersionUID = -2287035947644920508L;

    // Marks
    private static final String MARK_BASE_URL = "base_url";
    private static final String MARK_UPLOAD_URL = "upload_url";
    private static final String MARK_HANDLER_NAME = "handler_name";
    private static final String MARK_SUBMIT_PREFIX = "submitPrefix";
    private static final String MARK_DELETE_PREFIX = "deletePrefix";
    private static final String MARK_CHECKBOX_PREFIX = "checkBoxPrefix";
    private static final String MARK_SPLIT_FILE = "splitFile";
    // Parameters
    private static final String PROPERTY_KEY_PREFIX = "asynchronous.upload.config.";
    private static final String PARAMETER_HANDLER = "handler";
    private static final String PARAMETER_FIELD_NAME = "fieldname";
    private static final String PARAMETER_FIELD_INDEX = "field_index";
    private static final String PARAMETER_MAX_FILE_SIZE = "maxFileSize";
    private static final String PARAMETER_IMAGE_MAX_WIDTH = "imageMaxWidth";
    private static final String PARAMETER_IMAGE_MAX_HEIGHT = "imageMaxHeight";
    private static final String PARAMETER_PREVIEW_MAX_WIDTH = "previewMaxWidth";
    private static final String PARAMETER_PREVIEW_MAX_HEIGHT = "previewMaxHeight";
    private static final String PARAMETER_MAX_CHUNK_SIZE = "maxChunkSize";

    // Templates
    private static final String TEMPLATE_MAIN_UPLOAD_JS = "skin/plugins/asynchronousupload/main.js";
    private static final String TEMPLATE_ADMIN_MAIN_UPLOAD_JS = "admin/plugins/asynchronousupload/main.js";

    // Urls
    private static final String URL_UPLOAD_SERVLET = "jsp/site/upload";

    private static final String URL_UPLOAD_BO_SERVLET = "jsp/admin/upload";

    // Constants
    private static final String CONSTANT_COMA = ",";
    // filed name
    private static final String DEFAULT_FIELD_NAME = StringUtils.EMPTY;

    /**
     * Get the main upload JavaScript file. Available HTTP parameters are :
     * <ul>
     * <li><b>handler</b> : Name of the handler that will manage the asynchronous upload.</li>
     * <li><b>maxFileSize</b> : The maximum size (in bytes) of uploaded files. Default value is 2097152</li>
     * </ul>
     * 
     * @param request
     *            The request
     * @param bContext
     *            True if front / False if admin
     * @return The content of the JavaScript file
     */
    public String getMainUploadJs( HttpServletRequest request, Boolean bContext )
    {
        String strBaseUrl = AppPathService.getBaseUrl( request );

        String strTemplate;

        String strHandlerName = request.getParameter( PARAMETER_HANDLER );
        String strMaxFileSize = request.getParameter( PARAMETER_MAX_FILE_SIZE );
        String strImageMaxWidth = request.getParameter( PARAMETER_IMAGE_MAX_WIDTH );
        String strImageMaxHeight = request.getParameter( PARAMETER_IMAGE_MAX_HEIGHT );
        String strPreviewMaxWidth = request.getParameter( PARAMETER_PREVIEW_MAX_WIDTH );
        String strPreviewMaxHeight = request.getParameter( PARAMETER_PREVIEW_MAX_HEIGHT );
        String strFieldName = request.getParameter( PARAMETER_FIELD_NAME );
        String strMaxChunkSize = request.getParameter( PARAMETER_MAX_CHUNK_SIZE );

        IAsyncUploadHandler handler = getHandler( strHandlerName );

        int nMaxFileSize = Integer.parseInt( AppPropertiesService.getProperty( PROPERTY_KEY_PREFIX + PARAMETER_MAX_FILE_SIZE ) );
        if ( StringUtils.isNumeric( strMaxFileSize ) )
        {
            nMaxFileSize = Integer.parseInt( strMaxFileSize );
        }

        int nImageMaxHeight = Integer.parseInt( AppPropertiesService.getProperty( PROPERTY_KEY_PREFIX + PARAMETER_IMAGE_MAX_HEIGHT ) );
        if ( StringUtils.isNumeric( strImageMaxHeight ) )
        {
            nImageMaxHeight = Integer.parseInt( strImageMaxHeight );
        }

        int nImageMaxWidth = Integer.parseInt( AppPropertiesService.getProperty( PROPERTY_KEY_PREFIX + PARAMETER_IMAGE_MAX_WIDTH ) );
        if ( StringUtils.isNumeric( strImageMaxWidth ) )
        {
            nImageMaxWidth = Integer.parseInt( strImageMaxWidth );
        }

        int nPreviewMaxWidth = Integer.parseInt( AppPropertiesService.getProperty( PROPERTY_KEY_PREFIX + PARAMETER_PREVIEW_MAX_WIDTH ) );
        if ( StringUtils.isNumeric( strPreviewMaxWidth ) )
        {
            nPreviewMaxWidth = Integer.parseInt( strPreviewMaxWidth );
        }

        int nPreviewMaxHeight = Integer.parseInt( AppPropertiesService.getProperty( PROPERTY_KEY_PREFIX + PARAMETER_PREVIEW_MAX_HEIGHT ) );
        if ( StringUtils.isNumeric( strPreviewMaxHeight ) )
        {
            nPreviewMaxHeight = Integer.parseInt( strPreviewMaxHeight );
        }

        int nMaxChunkSize = AppPropertiesService.getPropertyInt( PROPERTY_KEY_PREFIX + PARAMETER_MAX_CHUNK_SIZE, 0 );
        if ( StringUtils.isNumeric( strMaxChunkSize ) )
        {
            nMaxChunkSize = Integer.parseInt( strMaxChunkSize );
        }

        if ( StringUtils.isEmpty( strFieldName ) )
        {
            strFieldName = DEFAULT_FIELD_NAME;
        }

        String strKey = StringUtils.defaultString( strHandlerName ) + strBaseUrl + StringUtils.defaultString( strMaxFileSize )
                + StringUtils.defaultString( strImageMaxWidth ) + StringUtils.defaultString( strImageMaxHeight )
                + StringUtils.defaultString( strPreviewMaxWidth ) + StringUtils.defaultString( strPreviewMaxHeight )
                + StringUtils.defaultString( strMaxChunkSize ) + strFieldName;

        String strContent = (String) UploadCacheService.getInstance( ).getFromCache( strKey );

        if ( strContent != null )
        {
            return strContent;
        }

        Map<String, Object> model = new HashMap<>( );
        boolean bSplitFile = handler.isManagePartialContent( ) && nMaxChunkSize > 0;
        model.put( MARK_BASE_URL, strBaseUrl );
        model.put( MARK_HANDLER_NAME, strHandlerName );
        model.put( PARAMETER_MAX_FILE_SIZE, nMaxFileSize );
        model.put( PARAMETER_IMAGE_MAX_WIDTH, nImageMaxWidth );
        model.put( PARAMETER_IMAGE_MAX_HEIGHT, nImageMaxHeight );
        model.put( PARAMETER_PREVIEW_MAX_WIDTH, nPreviewMaxWidth );
        model.put( PARAMETER_PREVIEW_MAX_HEIGHT, nPreviewMaxHeight );
        model.put( MARK_SUBMIT_PREFIX, handler.getUploadSubmitPrefix( ) );
        model.put( MARK_DELETE_PREFIX, handler.getUploadDeletePrefix( ) );
        model.put( MARK_CHECKBOX_PREFIX, handler.getUploadCheckboxPrefix( ) );
        model.put( PARAMETER_FIELD_NAME, strFieldName );
        model.put( MARK_SPLIT_FILE, bSplitFile );
        model.put( PARAMETER_MAX_CHUNK_SIZE, nMaxChunkSize );

        if ( Boolean.TRUE.equals( bContext ) )
        {
            model.put( MARK_UPLOAD_URL, URL_UPLOAD_SERVLET );
            strTemplate = TEMPLATE_MAIN_UPLOAD_JS;
        }
        else
        {
            model.put( MARK_UPLOAD_URL, URL_UPLOAD_BO_SERVLET );
            strTemplate = TEMPLATE_ADMIN_MAIN_UPLOAD_JS;
        }
        HtmlTemplate template = AppTemplateService.getTemplate( strTemplate, getLocale( request ), model );
        strContent = template.getHtml( );
        UploadCacheService.getInstance( ).putInCache( strKey, strContent );
        return strContent;
    }

    /**
     * Removes the uploaded fileItem.
     * 
     * @param request
     *            the request
     * @return The JSON result
     */
    public String doRemoveAsynchronousUploadedFile( HttpServletRequest request )
    {
        String strFieldName = request.getParameter( PARAMETER_FIELD_NAME );

        String strFieldIndex = request.getParameter( PARAMETER_FIELD_INDEX );

        List<Integer> listIndexesFilesToRemove = new ArrayList<>( );

        if ( StringUtils.isNotEmpty( strFieldIndex ) )
        {
            for ( String strIndex : StringUtils.split( strFieldIndex, CONSTANT_COMA ) )
            {
                if ( StringUtils.isNotEmpty( strIndex ) && StringUtils.isNumeric( strIndex ) )
                {
                    listIndexesFilesToRemove.add( Integer.parseInt( strIndex ) );
                }
            }
        }

        IAsyncUploadHandler handler = getHandler( request );

        return ( handler == null ) ? StringUtils.EMPTY : handler.doRemoveUploadedFile( request, strFieldName, listIndexesFilesToRemove );
    }

    /**
     * Gets the handler
     * 
     * @param request
     *            the request
     * @return the handler found, <code>null</code> otherwise.
     * @see IAsynchronousUploadHandler#isInvoked(HttpServletRequest)
     */
    private IAsyncUploadHandler getHandler( HttpServletRequest request )
    {
        for ( IAsyncUploadHandler handler : SpringContextService.getBeansOfType( IAsyncUploadHandler.class ) )
        {
            if ( handler.isInvoked( request ) )
            {
                return handler;
            }
        }

        return null;
    }

    /**
     * Get a handler from its name
     * 
     * @param strName
     *            The name of the handler
     * @return The handler, or null if no handler was found
     */
    private IAsyncUploadHandler getHandler( String strName )
    {
        for ( IAsyncUploadHandler handler : SpringContextService.getBeansOfType( IAsyncUploadHandler.class ) )
        {
            if ( StringUtils.equals( handler.getHandlerName( ), strName ) )
            {
                return handler;
            }
        }

        return null;
    }

    /**
     * Get the uploaded fileItem.
     * 
     * @param request
     *            the request
     * @param response
     *            the response
     */
    public void doRetrieveAsynchronousUploadedFile( HttpServletRequest request, HttpServletResponse response )
    {
        IAsyncUploadHandler handler = getHandler( request );
        byte [ ] data = handler.doRetrieveUploadedFile( request );
        String strFieldName = request.getParameter( "fileName" );

        response.setHeader( "Content-length", Integer.toString( data.length ) );
        response.setHeader( "Content-Disposition", "attachment;;filename=" + strFieldName );
        try
        {
            response.getOutputStream( ).write( data, 0, data.length );
            response.getOutputStream( ).flush( );
        }
        catch( IOException e )
        {
            AppLogService.error( e );
        }
    }
}
