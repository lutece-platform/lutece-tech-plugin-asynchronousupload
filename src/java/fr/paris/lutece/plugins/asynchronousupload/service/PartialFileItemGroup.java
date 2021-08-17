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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.util.filesystem.UploadUtil;

/**
 * File item witch contains a list of partial file item
 */
public class PartialFileItemGroup implements FileItem
{
    private static final long serialVersionUID = 8696893066570050604L;
    private List<FileItem> _items;
    private SequenceInputStream _sequenceInputStream;

    /**
     * Instantiates a new normalize file item.
     *
     * @param item
     *            the item
     */
    public PartialFileItemGroup( List<FileItem> items )
    {
        _items = items;
        Vector vOut = new Vector( );

        try
        {

            for ( FileItem fileItem : items )
            {
                vOut.add( fileItem.getInputStream( ) );

            }
        }
        catch( IOException e )
        {
            AppLogService.error( "error creating Partial File item sequence inputstream", e );
        }
        Enumeration enumeration = vOut.elements( );
        _sequenceInputStream = new SequenceInputStream( enumeration );

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete( )
    {
        for ( FileItem item : _items )
        {
            item.delete( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte [ ] get( )
    {

        byte [ ] bReturn = null;
        try
        {
            bReturn = IOUtils.toByteArray( _sequenceInputStream );
        }
        catch( IOException e )
        {
            AppLogService.error( "error getting Partial File item  inputstream", e );

        }
        return bReturn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContentType( )
    {
        return _items.get( 0 ).getContentType( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFieldName( )
    {
        return _items.get( 0 ).getFieldName( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getInputStream( ) throws IOException
    {
        return _sequenceInputStream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName( )
    {
        return UploadUtil.cleanFileName( FilenameUtils.getName( _items.get( 0 ).getName( ) ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputStream getOutputStream( ) throws IOException
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize( )
    {
        return _items.stream( ).collect( Collectors.summingLong( x -> x.getSize( ) ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getString( )
    {
        return _items.get( 0 ).getString( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getString( String encoding ) throws UnsupportedEncodingException
    {
        return _items.get( 0 ).getString( encoding );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFormField( )
    {
        return _items.get( 0 ).isFormField( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInMemory( )
    {
        return _items.get( 0 ).isInMemory( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFieldName( String name )
    {
        _items.get( 0 ).setFieldName( name );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFormField( boolean state )
    {
        _items.get( 0 ).setFormField( state );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write( File file ) throws Exception
    {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileItemHeaders getHeaders( )
    {
        return _items.get( 0 ).getHeaders( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setHeaders( FileItemHeaders headers )
    {

    }
}
