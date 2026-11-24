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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.plugins.asynchronousupload.service.AsynchronousUploadHandler;
import fr.paris.lutece.portal.service.upload.MultipartItem;
import fr.paris.lutece.test.LuteceTestCase;
import fr.paris.lutece.test.mocks.MockHttpServletRequest;
import fr.paris.lutece.test.mocks.MockHttpServletResponse;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Download of a file uploaded but not yet submitted
 */
public class AsynchronousUploadAppDownloadTest extends LuteceTestCase
{
    private static final String FIELD_NAME = "attribute_download_test";
    private static final String FILE_NAME = "rapport, v2 été.pdf";
    private static final byte [ ] CONTENT = "content".getBytes( StandardCharsets.UTF_8 );

    /**
     * The header gives the name and the type of the stored file, quoted and encoded, whatever the name in the link
     */
    @Test
    public void testHeadersOfTheDownload( )
    {
        MockHttpServletRequest request = new MockHttpServletRequest( );
        AsynchronousUploadHandler handler = CDI.current( ).select( AsynchronousUploadHandler.class ).get( );
        handler.addFileItemToUploadedFilesList( new Item( ), FIELD_NAME, request );
        request.addParameter( "asynchronousupload.handler", handler.getHandlerName( ) );
        request.addParameter( "fieldname", FIELD_NAME );
        request.addParameter( "field_index", "0" );
        request.addParameter( "fileName", "other,name.txt" );
        HeaderResponse response = new HeaderResponse( );

        try
        {
            CDI.current( ).select( AsynchronousUploadApp.class ).get( ).doRetrieveAsynchronousUploadedFile( request, response );

            assertEquals( "attachment; filename=\"rapport, v2 ete.pdf\"; filename*=UTF-8''rapport%2C%20v2%20%C3%A9t%C3%A9.pdf",
                    response.getHeader( "Content-Disposition" ) );
            assertTrue( response.getContentType( ).startsWith( "application/pdf" ), response.getContentType( ) );
        }
        finally
        {
            handler.removeSessionFiles( request.getSession( ) );
        }
    }

    /**
     * Response that keeps its headers
     */
    private static class HeaderResponse extends MockHttpServletResponse
    {
        private final Map<String, String> _mapHeaders = new HashMap<>( );

        @Override
        public void setHeader( String strName, String strValue )
        {
            _mapHeaders.put( strName, strValue );
        }

        @Override
        public String getHeader( String strName )
        {
            return _mapHeaders.get( strName );
        }
    }

    private static class Item implements MultipartItem
    {
        @Override
        public String getFieldName( )
        {
            return FIELD_NAME;
        }

        @Override
        public String getName( )
        {
            return FILE_NAME;
        }

        @Override
        public String getContentType( )
        {
            return "application/pdf";
        }

        @Override
        public long getSize( )
        {
            return CONTENT.length;
        }

        @Override
        public InputStream getInputStream( )
        {
            return new ByteArrayInputStream( CONTENT );
        }

        @Override
        public byte [ ] get( )
        {
            return CONTENT;
        }

        @Override
        public void delete( )
        {
            // nothing stored
        }
    }
}
