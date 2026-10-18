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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpSession;

import fr.paris.lutece.portal.service.upload.MultipartItem;
import fr.paris.lutece.test.mocks.MockHttpServletRequest;
import fr.paris.lutece.test.mocks.MockHttpSession;

/**
 * The uploaded files are kept in the HTTP session: they follow a replicated session to another instance, and go away with it
 */
public class AsynchronousUploadHandlerTest
{
    private static final String FIELD_NAME = "field";
    private static final byte [ ] CONTENT = "file content".getBytes( StandardCharsets.UTF_8 );

    @Test
    public void testUploadedFilesFollowTheReplicatedSession( ) throws Exception
    {
        HttpSession sessionNode1 = new MockHttpSession( );
        new AsynchronousUploadHandler( ).addFileItemToUploadedFilesList( new TestItem( ), FIELD_NAME, newRequest( sessionNode1 ) );

        // the session as another instance reads it: every attribute serialized and deserialized
        HttpSession sessionNode2 = replicate( sessionNode1 );
        // the files of the first instance go away (session ended on this instance): the replicated session keeps its copy
        new AsynchronousUploadHandler( ).removeSessionFiles( sessionNode1 );
        assertTrue( new AsynchronousUploadHandler( ).getListUploadedFiles( FIELD_NAME, sessionNode1 ).isEmpty( ) );

        List<MultipartItem> listFiles = new AsynchronousUploadHandler( ).getListUploadedFiles( FIELD_NAME, sessionNode2 );
        assertEquals( 1, listFiles.size( ) );
        assertEquals( "file.txt", listFiles.get( 0 ).getName( ) );
        assertEquals( CONTENT.length, listFiles.get( 0 ).getSize( ) );
        assertArrayEquals( CONTENT, listFiles.get( 0 ).get( ) );
    }

    @Test
    public void testSessionFilesAreRemovedWithTheSession( )
    {
        HttpSession session = new MockHttpSession( );
        AsynchronousUploadHandler handler = new AsynchronousUploadHandler( );
        TestItem item = new TestItem( );
        handler.addFileItemToUploadedFilesList( item, FIELD_NAME, newRequest( session ) );
        // the same file is not added twice
        handler.addFileItemToUploadedFilesList( new TestItem( ), FIELD_NAME, newRequest( session ) );
        assertEquals( 1, handler.getListUploadedFiles( FIELD_NAME, session ).size( ) );

        handler.removeSessionFiles( session );

        assertTrue( item._bDeleted );
        assertFalse( Collections.list( session.getAttributeNames( ) ).stream( ).anyMatch( s -> s.startsWith( "asynchronousupload." ) ) );
    }

    private static HttpSession replicate( HttpSession session ) throws IOException, ClassNotFoundException
    {
        HttpSession copy = new MockHttpSession( );
        for ( String strName : Collections.list( session.getAttributeNames( ) ) )
        {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream( );
            try ( ObjectOutputStream out = new ObjectOutputStream( bytes ) )
            {
                out.writeObject( session.getAttribute( strName ) );
            }
            try ( ObjectInputStream in = new ObjectInputStream( new ByteArrayInputStream( bytes.toByteArray( ) ) ) )
            {
                copy.setAttribute( strName, in.readObject( ) );
            }
        }
        return copy;
    }

    private static HttpServletRequest newRequest( HttpSession session )
    {
        return new HttpServletRequestWrapper( new MockHttpServletRequest( ) )
        {
            @Override
            public HttpSession getSession( )
            {
                return session;
            }

            @Override
            public HttpSession getSession( boolean bCreate )
            {
                return session;
            }
        };
    }

    /**
     * In-memory uploaded item, not serializable
     */
    private static class TestItem implements MultipartItem
    {
        private boolean _bDeleted;

        @Override
        public String getFieldName( )
        {
            return FIELD_NAME;
        }

        @Override
        public String getName( )
        {
            return "file.txt";
        }

        @Override
        public String getContentType( )
        {
            return "text/plain";
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
            return CONTENT.clone( );
        }

        @Override
        public void delete( )
        {
            _bDeleted = true;
        }
    }
}
