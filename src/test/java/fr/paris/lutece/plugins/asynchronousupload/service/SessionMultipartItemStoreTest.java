/*
 * Copyright (c) 2002-2026, City of Paris
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import fr.paris.lutece.portal.service.upload.MultipartItem;

/**
 * Uploaded files kept outside the session (several instances): the session only holds their ids
 */
public class SessionMultipartItemStoreTest
{
    private static final int SIZE = 1024 * 1024;

    @AfterEach
    public void resetStore( )
    {
        SessionMultipartItem.setStore( null );
    }

    @Test
    public void testContentStoredOnceAndSessionHoldsOnlyTheId( ) throws Exception
    {
        MemoryStore store = new MemoryStore( true );
        SessionMultipartItem.setStore( store );
        CountingItem upload = new CountingItem( content( ) );

        List<MultipartItem> list = new SessionMultipartItemList( );
        list.add( upload );

        assertEquals( 1, store._map.size( ) );
        assertEquals( 1, upload._nReads, "the content is read once, at upload" );
        assertTrue( ( (SessionMultipartItem) list.get( 0 ) ).isStored( ) );

        // The session is written after each request: the file is not
        byte [ ] serialized = serialize( list );
        serialize( list );
        assertTrue( serialized.length < 1024, "serialized size " + serialized.length );
        assertEquals( 1, upload._nReads );
        assertEquals( 1, store._nStores );

        // Another instance reads the file from the store
        List<MultipartItem> copy = deserialize( serialized );
        MultipartItem item = copy.get( 0 );
        assertEquals( "file.bin", item.getName( ) );
        assertEquals( SIZE, item.getSize( ) );
        assertArrayEquals( content( ), item.get( ) );
        assertArrayEquals( content( ), item.getInputStream( ).readAllBytes( ) );

        // Removed from the form on that instance: removed from the store
        item.delete( );
        assertTrue( store._map.isEmpty( ) );
        assertEquals( 0, item.get( ).length );
    }

    @Test
    public void testOneInstanceKeepsTheFileInTheSession( ) throws Exception
    {
        MemoryStore store = new MemoryStore( false );
        SessionMultipartItem.setStore( store );
        CountingItem upload = new CountingItem( content( ) );

        List<MultipartItem> list = new SessionMultipartItemList( );
        list.add( upload );

        assertEquals( 0, upload._nReads, "nothing is read while the session stays in memory" );
        assertTrue( store._map.isEmpty( ) );
        assertFalse( ( (SessionMultipartItem) list.get( 0 ) ).isStored( ) );
        byte [ ] serialized = serialize( list );
        assertTrue( serialized.length > SIZE );
        assertArrayEquals( content( ), deserialize( serialized ).get( 0 ).get( ) );
    }

    @Test
    public void testFailedStoreKeepsTheFileInTheSession( ) throws Exception
    {
        MemoryStore store = new MemoryStore( true );
        store._bFail = true;
        SessionMultipartItem.setStore( store );

        List<MultipartItem> list = new SessionMultipartItemList( );
        list.add( new CountingItem( content( ) ) );

        assertFalse( ( (SessionMultipartItem) list.get( 0 ) ).isStored( ) );
        assertArrayEquals( content( ), deserialize( serialize( list ) ).get( 0 ).get( ) );
    }

    @Test
    public void testExpiredFileIsAnErrorNotAnEmptyFile( ) throws Exception
    {
        MemoryStore store = new MemoryStore( true );
        SessionMultipartItem.setStore( store );
        List<MultipartItem> list = new SessionMultipartItemList( );
        list.add( new CountingItem( content( ) ) );
        List<MultipartItem> copy = deserialize( serialize( list ) );

        store._map.clear( );

        assertThrows( UncheckedIOException.class, ( ) -> copy.get( 0 ).get( ) );
        assertThrows( UncheckedIOException.class, ( ) -> copy.get( 0 ).getInputStream( ) );
    }

    private static byte [ ] content( )
    {
        byte [ ] content = new byte [ SIZE];
        for ( int i = 0; i < SIZE; i++ )
        {
            content [i] = (byte) i;
        }
        return content;
    }

    private static byte [ ] serialize( Object object ) throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream( );
        try ( ObjectOutputStream out = new ObjectOutputStream( bytes ) )
        {
            out.writeObject( object );
        }
        return bytes.toByteArray( );
    }

    private static Object deserializeObject( byte [ ] bytes ) throws IOException, ClassNotFoundException
    {
        return new ObjectInputStream( new ByteArrayInputStream( bytes ) ).readObject( );
    }

    @SuppressWarnings( "unchecked" )
    private static List<MultipartItem> deserialize( byte [ ] bytes ) throws IOException, ClassNotFoundException
    {
        return (List<MultipartItem>) deserializeObject( bytes );
    }


    /**
     * Store in memory
     */
    private static class MemoryStore implements IUploadedFileStore
    {
        private final Map<String, byte [ ]> _map = new ConcurrentHashMap<>( );
        private final boolean _bActive;
        private boolean _bFail;
        private int _nStores;

        MemoryStore( boolean bActive )
        {
            _bActive = bActive;
        }

        @Override
        public boolean isActive( )
        {
            return _bActive;
        }

        @Override
        public String store( byte [ ] content )
        {
            if ( _bFail )
            {
                return null;
            }
            _nStores++;
            String strId = "id" + _nStores;
            _map.put( strId, content.clone( ) );
            return strId;
        }

        @Override
        public byte [ ] load( String strId )
        {
            byte [ ] content = _map.get( strId );
            return ( content == null ) ? null : content.clone( );
        }

        @Override
        public void delete( String strId )
        {
            _map.remove( strId );
        }
    }

    /**
     * In-memory upload, counting the reads of its content
     */
    private static class CountingItem implements MultipartItem
    {
        private final byte [ ] _content;
        private int _nReads;

        CountingItem( byte [ ] content )
        {
            _content = content;
        }

        @Override
        public String getFieldName( )
        {
            return "field";
        }

        @Override
        public String getName( )
        {
            return "file.bin";
        }

        @Override
        public String getContentType( )
        {
            return "application/octet-stream";
        }

        @Override
        public long getSize( )
        {
            return _content.length;
        }

        @Override
        public InputStream getInputStream( )
        {
            _nReads++;
            return new ByteArrayInputStream( _content );
        }

        @Override
        public byte [ ] get( )
        {
            _nReads++;
            return _content.clone( );
        }

        @Override
        public void delete( )
        {
            // nothing to delete
        }
    }
}
