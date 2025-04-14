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

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import fr.paris.lutece.portal.service.upload.MultipartItem;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.util.filesystem.UploadUtil;

/**
 * File item witch contains a list of partial file item
 */
public class PartialFileItemGroup implements MultipartItem
{
    private static final long serialVersionUID = 8696893066570050604L;
    private List<MultipartItem> _items;
    private SequenceInputStream _sequenceInputStream;

    /**
     * Instantiates a new normalize file item.
     *
     * @param item
     *            the item
     */
    public PartialFileItemGroup( List<MultipartItem> items )
    {
        _items = items;
        List<InputStream> vOut = new ArrayList<>( );

        try
        {

            for ( MultipartItem fileItem : items )
            {
                vOut.add( fileItem.getInputStream( ) );

            }
        }
        catch( IOException e )
        {
            AppLogService.error( "error creating Partial File item sequence inputstream", e );
        }
        _sequenceInputStream = new SequenceInputStream( Collections.enumeration( vOut ) );

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete( ) throws IOException
    {
        for ( MultipartItem item : _items )
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
    public long getSize( )
    {
        return _items.stream( ).collect( Collectors.summingLong( MultipartItem::getSize ) );
    }

}
