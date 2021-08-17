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
package fr.paris.lutece.plugins.asynchronousupload.util;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;

import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.util.file.FileUtil;
import net.sf.json.JSONObject;

/**
 * Provides json utility methods for forms
 *
 */
public final class JSONUtils
{
    /**
     * JSON key for field name
     */
    public static final String JSON_KEY_FIELD_NAME = "field_name";

    /**
     * JSON key to describe a success
     */
    public static final String JSON_KEY_SUCCESS = "success";
    private static final String JSON_KEY_FILE_NAME = "name";
    private static final String JSON_KEY_FILE_SIZE = "size";
    private static final String JSON_KEY_FILE_PREVIEW = "preview";
    private static final String JSON_KEY_FORM_ERROR = "form_error";
    private static final String JSON_KEY_UPLOADED_FILES = "files";
    private static final String JSON_KEY_FILE_COUNT = "fileCount";

    // PROPERTIES
    private static final String PROPERTY_MESSAGE_ERROR_REMOVING_FILE = "form.message.error.removingFile";

    /**
     * Empty constructor
     */
    private JSONUtils( )
    {
        // nothing
    }

    /**
     * Builds a json object for the file item list. Key is {@link #JSON_KEY_UPLOADED_FILES}, value is the array of uploaded file.
     * 
     * @param listFileItem
     *            the fileItem list
     * @return the json
     */
    public static JSONObject getUploadedFileJSON( List<FileItem> listFileItem )
    {
        JSONObject json = new JSONObject( );

        if ( listFileItem != null )
        {
            for ( FileItem fileItem : listFileItem )
            {
                JSONObject jsonObject = new JSONObject( );
                jsonObject.element( JSON_KEY_FILE_NAME, fileItem.getName( ) );
                jsonObject.element( JSON_KEY_FILE_PREVIEW, getPreviewImage( fileItem ) );
                jsonObject.element( JSON_KEY_FILE_SIZE, fileItem.getSize( ) );
                json.accumulate( JSON_KEY_UPLOADED_FILES, jsonObject );
            }

            json.element( JSON_KEY_FILE_COUNT, listFileItem.size( ) );
        }
        else
        {
            // no file
            json.element( JSON_KEY_FILE_COUNT, 0 );
        }

        return json;
    }

    /**
     * Builds a json object with the error message.
     * 
     * @param request
     *            the request
     * @return the json object.
     */
    public static JSONObject buildJsonErrorRemovingFile( HttpServletRequest request )
    {
        JSONObject json = new JSONObject( );

        json.element( JSON_KEY_FORM_ERROR, I18nService.getLocalizedString( PROPERTY_MESSAGE_ERROR_REMOVING_FILE, request.getLocale( ) ) );

        return json;
    }

    /**
     * Builds a json object with the error message.
     * 
     * @param json
     *            the JSON
     * @param strMessage
     *            the error message
     */
    public static void buildJsonError( JSONObject json, String strMessage )
    {
        if ( json != null )
        {
            json.accumulate( JSON_KEY_FORM_ERROR, strMessage );
        }
    }

    private static String getPreviewImage( FileItem fileItem )
    {

        if ( FileUtil.hasImageExtension( fileItem.getName( ) ) )
        {

            return "data:image/png;base64," + DatatypeConverter.printBase64Binary( fileItem.get( ) );
        }

        return StringUtils.EMPTY;
    }
}
