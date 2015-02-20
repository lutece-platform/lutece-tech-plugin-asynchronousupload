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
package fr.paris.lutece.plugins.asynchronousupload.web;

import fr.paris.lutece.plugins.asynchronousupload.service.IAsyncUploadHandler;
import fr.paris.lutece.plugins.asynchronousupload.service.UploadCacheService;
import fr.paris.lutece.portal.service.spring.SpringContextService;
import fr.paris.lutece.portal.service.template.AppTemplateService;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.portal.util.mvc.xpage.MVCApplication;
import fr.paris.lutece.portal.web.upload.IAsynchronousUploadHandler;
import fr.paris.lutece.util.html.HtmlTemplate;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;



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
    
 
    // Parameters
    private static final String PARAMETER_HANDLER = "handler";
    private static final String PARAMETER_FIELD_NAME = "fieldname";
    private static final String PARAMETER_FIELD_INDEX = "field_index";
    private static final String PARAMETER_MAX_FILE_SIZE = "maxFileSize";
    private static final String PARAMETER_IMAGE_MAX_WIDTH = "imageMaxWidth";
    private static final String PARAMETER_IMAGE_MAX_HEIGHT = "imageMaxHeight";
    private static final String PARAMETER_PREVIEW_MAX_WIDTH = "previewMaxWidth";
    private static final String PARAMETER_PREVIEW_MAX_HEIGHT = "previewMaxHeight";
   
    // Templates
    private static final String TEMPLATE_MAIN_UPLOAD_JS = "skin/plugins/asynchronousupload/main.js";

    // Urls
    private static final String URL_UPLOAD_SERVLET = "jsp/site/upload";

    // Constants
    private static final int DEFAULT_MAX_FILE_SIZE = 2097152;
    private static final String CONSTANT_COMA = ",";
    // The maximum height of resized images:
    private static final int DEFAULT_IMAGE_MAX_HEIGHT = 1080;
    // The maximum width of resized images:
    private static final int DEFAULT_IMAGE_MAX_WIDTH = 1080;
    // The maximum height of preview images:
    private static final int DEFAULT_PREVIEW_MAX_HEIGHT = 80;
    // The maximum width of preview images:
    private static final int DEFAULT_PREVIEW_MAX_WIDTH = 80;
    //filed name
    private static final String   DEFAULT_FIELD_NAME = StringUtils.EMPTY;
    /**
     * Get the main upload JavaScript file. Available HTTP parameters are :
     * <ul>
     * <li><b>handler</b> : Name of the handler that will manage the
     * asynchronous upload.</li>
     * <li><b>maxFileSize</b> : The maximum size (in bytes) of uploaded files.
     * Default value is 2097152</li>
     * </ul>
     * @param request The request
     * @return The content of the JavaScript file
     */
    public String getMainUploadJs( HttpServletRequest request )
    {
        String strBaseUrl = AppPathService.getBaseUrl( request );

        String strHandlerName = request.getParameter( PARAMETER_HANDLER );
        String strMaxFileSize = request.getParameter( PARAMETER_MAX_FILE_SIZE );
        String strImageMaxWidth = request.getParameter( PARAMETER_IMAGE_MAX_WIDTH );
        String strImageMaxHeight = request.getParameter( PARAMETER_IMAGE_MAX_HEIGHT );
        String strPreviewMaxWidth = request.getParameter( PARAMETER_PREVIEW_MAX_WIDTH );
        String strPreviewMaxHeight = request.getParameter( PARAMETER_PREVIEW_MAX_HEIGHT );
        String strFieldName = request.getParameter( PARAMETER_FIELD_NAME );

        IAsyncUploadHandler handler = getHandler( strHandlerName );

        int nMaxFileSize,nImageMaxHeight, nImageMaxWidth, nPreviewMaxWidth, nPreviewMaxHeight ;

        if ( StringUtils.isNotEmpty( strMaxFileSize ) && StringUtils.isNumeric( strMaxFileSize ) )
        {
            nMaxFileSize = Integer.parseInt( strMaxFileSize );
        }
        else
        {
            nMaxFileSize = DEFAULT_MAX_FILE_SIZE;
        }

        if ( StringUtils.isNotEmpty( strImageMaxHeight ) && StringUtils.isNumeric( strImageMaxHeight ) )
        {
        	nImageMaxHeight = Integer.parseInt( strImageMaxHeight );
        }
        else
        {
        	nImageMaxHeight = DEFAULT_IMAGE_MAX_HEIGHT;
        }
        

        if ( StringUtils.isNotEmpty( strImageMaxWidth ) && StringUtils.isNumeric( strImageMaxWidth ) )
        {
        	nImageMaxWidth = Integer.parseInt( strImageMaxWidth );
        }
        else
        {
        	nImageMaxWidth = DEFAULT_IMAGE_MAX_WIDTH;
        }
        if ( StringUtils.isNotEmpty( strPreviewMaxWidth ) && StringUtils.isNumeric( strPreviewMaxWidth ) )
        {
        	nPreviewMaxWidth = Integer.parseInt( strPreviewMaxWidth );
        }
        else
        {
        	nPreviewMaxWidth = DEFAULT_PREVIEW_MAX_HEIGHT;
        }
        

        if ( StringUtils.isNotEmpty( strPreviewMaxHeight ) && StringUtils.isNumeric( strPreviewMaxHeight ) )
        {
        	nPreviewMaxHeight = Integer.parseInt( strPreviewMaxHeight );
        }
        else
        {
        	nPreviewMaxHeight = DEFAULT_PREVIEW_MAX_WIDTH;
        }
        
        if ( !StringUtils.isNotEmpty( strFieldName )  )
        {
        	strFieldName = DEFAULT_FIELD_NAME;
        }
        
        String strKey = ( ( strHandlerName != null ) ? strHandlerName : StringUtils.EMPTY ) + strBaseUrl +
            ( ( strMaxFileSize == null ) ? StringUtils.EMPTY : strMaxFileSize ) 
            + ( ( strImageMaxWidth == null ) ? StringUtils.EMPTY : strImageMaxWidth )
            + ( ( strImageMaxHeight == null ) ? StringUtils.EMPTY : strImageMaxHeight )
            + ( ( strPreviewMaxWidth == null ) ? StringUtils.EMPTY : strPreviewMaxWidth )
            + ( ( strPreviewMaxHeight == null ) ? StringUtils.EMPTY : strPreviewMaxHeight )	
            +( ( strFieldName == null ) ? StringUtils.EMPTY : strFieldName );

        String strContent = (String) UploadCacheService.getInstance(  ).getFromCache( strKey );

        if ( strContent == null )
        {
            Map<String, Object> model = new HashMap<String, Object>(  );

            model.put( MARK_BASE_URL, strBaseUrl );
            model.put( MARK_UPLOAD_URL, URL_UPLOAD_SERVLET );
            model.put( MARK_HANDLER_NAME, strHandlerName );
            model.put( PARAMETER_MAX_FILE_SIZE, nMaxFileSize );
            model.put( PARAMETER_IMAGE_MAX_WIDTH, nImageMaxWidth );
            model.put( PARAMETER_IMAGE_MAX_HEIGHT, nImageMaxHeight );
            model.put( PARAMETER_PREVIEW_MAX_WIDTH, nPreviewMaxWidth );
            model.put( PARAMETER_PREVIEW_MAX_HEIGHT, nPreviewMaxHeight );  
            model.put( MARK_SUBMIT_PREFIX, handler.getUploadSubmitPrefix(  ) );
            model.put( MARK_DELETE_PREFIX, handler.getUploadDeletePrefix(  ) );
            model.put( MARK_CHECKBOX_PREFIX, handler.getUploadCheckboxPrefix(  ) );
            model.put( PARAMETER_FIELD_NAME, strFieldName );

            HtmlTemplate template = AppTemplateService.getTemplate( TEMPLATE_MAIN_UPLOAD_JS, request.getLocale(  ),
                    model );
            strContent = template.getHtml(  );
            UploadCacheService.getInstance(  ).putInCache( strKey, strContent );
        }

        return strContent;
    }

    /**
     * Removes the uploaded fileItem.
     * @param request the request
     * @return The JSON result
     */
    public String doRemoveAsynchronousUploadedFile( HttpServletRequest request )
    {
        String strFieldName = request.getParameter( PARAMETER_FIELD_NAME );

        String strFieldIndex = request.getParameter( PARAMETER_FIELD_INDEX );
        

        List<Integer> listIndexesFilesToRemove = new ArrayList<Integer>(  );

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

        return ( handler == null ) ? StringUtils.EMPTY
                                   : handler.doRemoveUploadedFile( request, strFieldName, listIndexesFilesToRemove );
    }
    

    /**
     * Gets the handler
     * @param request the request
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
     * @param strName The name of the handler
     * @return The handler, or null if no handler was found
     */
    private IAsyncUploadHandler getHandler( String strName )
    {
        for ( IAsyncUploadHandler handler : SpringContextService.getBeansOfType( IAsyncUploadHandler.class ) )
        {
            if ( StringUtils.equals( handler.getHandlerName(  ), strName ) )
            {
                return handler;
            }
        }

        return null;
    }
}
