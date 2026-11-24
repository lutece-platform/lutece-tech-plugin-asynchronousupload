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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import fr.paris.lutece.plugins.asynchronousupload.service.IAsyncUploadHandler;
import fr.paris.lutece.plugins.asynchronousupload.service.UploadCacheService;
import fr.paris.lutece.portal.service.upload.MultipartItem;
import fr.paris.lutece.portal.service.template.AppTemplateService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.portal.util.mvc.xpage.MVCApplication;
import fr.paris.lutece.util.html.HtmlTemplate;

/**
 * Upload application
 */
@RequestScoped
@Named
public class AsynchronousUploadApp extends MVCApplication
{
    private static final long serialVersionUID = -2287035947644920508L;

    // Marks
    private static final String PARAMETER_FILE_NAME = "fileName";
    private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    private static final String CONTENT_TYPE_DEFAULT = "application/octet-stream";
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

    /** a field or handler name written in the script: it is part of a function name, so only identifier characters */
    private static final Pattern PATTERN_IDENTIFIER = Pattern.compile( "[A-Za-z0-9_]+" );
    
    @Inject
    private UploadCacheService _uploadCacheService;
    
    @Inject
    private Instance<IAsyncUploadHandler> _asyncUploadHandler;

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
        if ( handler == null )
        {
            AppLogService.debug( "No asynchronous upload handler named {}", strHandlerName );
            return StringUtils.EMPTY;
        }

        int nMaxFileSize = Integer.parseInt( AppPropertiesService.getProperty( PROPERTY_KEY_PREFIX + PARAMETER_MAX_FILE_SIZE ) );
        nMaxFileSize = parseIntOption( strMaxFileSize, nMaxFileSize );

        int nImageMaxHeight = Integer.parseInt( AppPropertiesService.getProperty( PROPERTY_KEY_PREFIX + PARAMETER_IMAGE_MAX_HEIGHT ) );
        nImageMaxHeight = parseIntOption( strImageMaxHeight, nImageMaxHeight );

        int nImageMaxWidth = Integer.parseInt( AppPropertiesService.getProperty( PROPERTY_KEY_PREFIX + PARAMETER_IMAGE_MAX_WIDTH ) );
        nImageMaxWidth = parseIntOption( strImageMaxWidth, nImageMaxWidth );

        int nPreviewMaxWidth = Integer.parseInt( AppPropertiesService.getProperty( PROPERTY_KEY_PREFIX + PARAMETER_PREVIEW_MAX_WIDTH ) );
        nPreviewMaxWidth = parseIntOption( strPreviewMaxWidth, nPreviewMaxWidth );

        int nPreviewMaxHeight = Integer.parseInt( AppPropertiesService.getProperty( PROPERTY_KEY_PREFIX + PARAMETER_PREVIEW_MAX_HEIGHT ) );
        nPreviewMaxHeight = parseIntOption( strPreviewMaxHeight, nPreviewMaxHeight );

        int nMaxChunkSize = AppPropertiesService.getPropertyInt( PROPERTY_KEY_PREFIX + PARAMETER_MAX_CHUNK_SIZE, 0 );
        nMaxChunkSize = parseIntOption( strMaxChunkSize, nMaxChunkSize );

        if ( StringUtils.isEmpty( strFieldName ) || !PATTERN_IDENTIFIER.matcher( strFieldName ).matches( ) )
        {
            strFieldName = DEFAULT_FIELD_NAME;
        }
        if ( strHandlerName != null && !PATTERN_IDENTIFIER.matcher( strHandlerName ).matches( ) )
        {
            strHandlerName = null;
        }

        String strKey = ( Boolean.TRUE.equals( bContext ) ? "FO|" : "BO|" ) + getLocale( request ) + '|'
                + StringUtils.defaultString( strHandlerName ) + '|' + strBaseUrl + '|' + nMaxFileSize + '|' + nImageMaxWidth + '|'
                + nImageMaxHeight + '|' + nPreviewMaxWidth + '|' + nPreviewMaxHeight + '|' + nMaxChunkSize + '|' + strFieldName;

        String strContent = _uploadCacheService.get( strKey );

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
        _uploadCacheService.put( strKey, strContent );
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
        for ( IAsyncUploadHandler handler : _asyncUploadHandler.stream( ).toList( ) )
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
        for ( IAsyncUploadHandler handler : _asyncUploadHandler.stream( ).toList( ) )
        {
            if ( StringUtils.equals( handler.getHandlerName( ), strName ) )
            {
                return handler;
            }
        }

        return null;
    }

    /**
     * Read a numeric option of the script: a value that is not a number of the int range keeps the default
     *
     * @param strValue
     *            the value of the request parameter
     * @param nDefault
     *            the default value
     * @return the value of the option
     */
    private static int parseIntOption( String strValue, int nDefault )
    {
        if ( StringUtils.isNumeric( strValue ) )
        {
            return NumberUtils.toInt( strValue, nDefault );
        }
        return nDefault;
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
        if ( handler == null )
        {
            response.setStatus( HttpServletResponse.SC_BAD_REQUEST );
            return;
        }
        byte [ ] data = handler.doRetrieveUploadedFile( request );
        // the name and the type of the stored file, not the ones of the link: the request parameter is only a fallback
        MultipartItem item = handler.getUploadedFileItem( request );
        String strFileName = ( item != null && StringUtils.isNotEmpty( item.getName( ) ) ) ? item.getName( ) : request.getParameter( PARAMETER_FILE_NAME );
        String strContentType = ( item != null && StringUtils.isNotEmpty( item.getContentType( ) ) ) ? item.getContentType( ) : CONTENT_TYPE_DEFAULT;

        response.setContentType( strContentType );
        response.setHeader( "Content-length", Integer.toString( data.length ) );
        response.setHeader( HEADER_CONTENT_DISPOSITION, buildContentDisposition( strFileName ) );
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

    /**
     * Build the Content-Disposition header of a download (RFC 6266): a quoted ASCII name for the old clients and the UTF-8 name encoded as in RFC
     * 5987
     *
     * @param strFileName
     *            the name of the file, may be null
     * @return the value of the header
     */
    private static String buildContentDisposition( String strFileName )
    {
        String strName = Objects.toString( strFileName, StringUtils.EMPTY ).replaceAll( "[\\r\\n\"\\\\]", "" );
        String strAsciiName = Normalizer.normalize( strName, Normalizer.Form.NFD ).replaceAll( "\\p{M}", "" ).replaceAll( "[^\\x20-\\x7E]", "_" );
        String strEncodedName = URLEncoder.encode( strName, StandardCharsets.UTF_8 ).replace( "+", "%20" ).replace( "*", "%2A" );
        return "attachment; filename=\"" + strAsciiName + "\"; filename*=UTF-8''" + strEncodedName;
    }
}
