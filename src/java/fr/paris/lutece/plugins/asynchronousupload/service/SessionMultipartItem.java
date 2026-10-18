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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;

import fr.paris.lutece.portal.service.upload.MultipartItem;
import fr.paris.lutece.portal.service.util.AppLogService;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Uploaded file kept in the HTTP session. The file stays where the upload put it (usually a temporary file).
 * <ul>
 * <li>One instance: its content is only copied when the session is serialized.</li>
 * <li>Several instances ({@link IUploadedFileStore#isActive()}): its content is written once, at upload, to the store shared by
 * the instances, and the session only keeps its id. The session is written to the shared session store after each request that
 * reads it: the file is not written again each time.</li>
 * </ul>
 */
public class SessionMultipartItem implements MultipartItem, Serializable
{
    private static final long serialVersionUID = 1L;

    private static final IUploadedFileStore NO_STORE = new IUploadedFileStore( )
    {
        @Override
        public boolean isActive( )
        {
            return false;
        }

        @Override
        public String store( byte [ ] content )
        {
            return null;
        }

        @Override
        public byte [ ] load( String strId )
        {
            return null;
        }

        @Override
        public void delete( String strId )
        {
            // nothing stored
        }
    };

    private static volatile IUploadedFileStore _store;

    private final String _strFieldName;
    private final String _strName;
    private final String _strContentType;
    private final long _lSize;
    private String _strStoredId;
    private transient MultipartItem _delegate;
    private transient byte [ ] _content;

    /**
     * Constructor
     *
     * @param item
     *            the uploaded item
     */
    public SessionMultipartItem( MultipartItem item )
    {
        _delegate = item;
        _strFieldName = item.getFieldName( );
        _strName = item.getName( );
        _strContentType = item.getContentType( );
        _lSize = item.getSize( );
        IUploadedFileStore store = store( );
        if ( store.isActive( ) )
        {
            try
            {
                _strStoredId = store.store( item.get( ) );
            }
            catch( RuntimeException e )
            {
                AppLogService.error( "Uploaded file {} kept in the session: it could not be stored", _strName, e );
            }
        }
    }

    /**
     * Wraps an item unless it is already kept in the session
     *
     * @param item
     *            the item
     * @return the item to keep in the session
     */
    public static MultipartItem of( MultipartItem item )
    {
        return ( item == null || item instanceof SessionMultipartItem ) ? item : new SessionMultipartItem( item );
    }

    /**
     * Gets the store of the uploaded files
     *
     * @return the store, inactive when none is available
     */
    static IUploadedFileStore store( )
    {
        IUploadedFileStore store = _store;
        if ( store == null )
        {
            try
            {
                store = CDI.current( ).select( UploadedFileCacheService.class ).get( );
                _store = store;
            }
            catch( RuntimeException e )
            {
                AppLogService.debug( "No store for the uploaded files: they stay in the session", e );
                return NO_STORE;
            }
        }
        return store;
    }

    /**
     * Sets the store of the uploaded files (tests)
     *
     * @param store
     *            the store, null to look it up again
     */
    static void setStore( IUploadedFileStore store )
    {
        _store = store;
    }

    /**
     * Tells whether the content of this file is kept outside the session
     *
     * @return true if the content is in the store of the uploaded files
     */
    boolean isStored( )
    {
        return _strStoredId != null;
    }

    @Override
    public String getFieldName( )
    {
        return _strFieldName;
    }

    @Override
    public String getName( )
    {
        return _strName;
    }

    @Override
    public String getContentType( )
    {
        return _strContentType;
    }

    @Override
    public long getSize( )
    {
        return _lSize;
    }

    @Override
    public InputStream getInputStream( ) throws IOException
    {
        return ( _delegate != null ) ? _delegate.getInputStream( ) : new ByteArrayInputStream( content( ) );
    }

    @Override
    public byte [ ] get( ) throws UncheckedIOException
    {
        return ( _delegate != null ) ? _delegate.get( ) : content( );
    }

    @Override
    public void delete( ) throws IOException
    {
        if ( _delegate != null )
        {
            _delegate.delete( );
        }
        if ( _strStoredId != null )
        {
            store( ).delete( _strStoredId );
            _strStoredId = null;
        }
        _content = null;
    }

    private byte [ ] content( )
    {
        if ( _content != null )
        {
            return _content;
        }
        if ( _strStoredId != null )
        {
            byte [ ] content = store( ).load( _strStoredId );
            if ( content == null )
            {
                // Never an empty file in place of the uploaded one
                throw new UncheckedIOException( new IOException( "The uploaded file " + _strName + " is no longer available" ) );
            }
            return content;
        }
        return new byte [ 0];
    }

    private void writeObject( ObjectOutputStream out ) throws IOException
    {
        out.defaultWriteObject( );
        out.writeObject( ( _strStoredId != null ) ? null : get( ) );
    }

    private void readObject( ObjectInputStream in ) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject( );
        _content = (byte [ ]) in.readObject( );
    }
}
