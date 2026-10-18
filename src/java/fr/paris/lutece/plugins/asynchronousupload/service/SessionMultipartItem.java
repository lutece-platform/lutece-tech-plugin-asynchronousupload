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

/**
 * Uploaded file kept in the HTTP session. The file stays where the upload put it (usually a temporary file) and its content
 * is only copied when the session is serialized: nothing is copied while the session stays in memory (one instance), and
 * the file follows the session when it is replicated to another instance.
 */
public class SessionMultipartItem implements MultipartItem, Serializable
{
    private static final long serialVersionUID = 1L;

    private final String _strFieldName;
    private final String _strName;
    private final String _strContentType;
    private final long _lSize;
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
        _content = null;
    }

    private byte [ ] content( )
    {
        return ( _content != null ) ? _content : new byte [ 0];
    }

    private void writeObject( ObjectOutputStream out ) throws IOException
    {
        out.defaultWriteObject( );
        out.writeObject( ( _delegate != null ) ? _delegate.get( ) : _content );
    }

    private void readObject( ObjectInputStream in ) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject( );
        _content = (byte [ ]) in.readObject( );
    }
}
