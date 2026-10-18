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

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;

import fr.paris.lutece.portal.service.cache.AbstractCacheableService;
import fr.paris.lutece.portal.service.cache.CacheConfigUtil;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Keeps the content of the uploaded files in a cache shared by the instances, when the cache provider is distributed. With one
 * instance (Ehcache) the files stay in the session: nothing is stored here.
 * <p>
 * The cache is required (never disabled from the back office) and never reset: it holds files the users are still working on.
 * Each file is removed when it is deleted (removed from the form, form submitted, session destroyed); the time to live only
 * purges the files of the sessions lost without notice.
 * </p>
 */
@ApplicationScoped
public class UploadedFileCacheService extends AbstractCacheableService<String, byte [ ]> implements IUploadedFileStore
{
    private static final String CACHE_NAME = "asynchronousUploadFilesCache";
    private static final String PROPERTY_TIME_TO_LIVE = "asynchronousupload.files.store.timeToLiveSeconds";
    private static final int DEFAULT_TIME_TO_LIVE = 86400;

    private boolean _bActive;

    /**
     * Creates the cache
     */
    @PostConstruct
    public void init( )
    {
        _bRequired = true;
        _bPreventReset = true;
        _bPreventGlobalReset = true;
        // The time to live must exceed the longest session: an active session keeps its files longer than its idle timeout
        int nTimeToLive = AppPropertiesService.getPropertyInt( PROPERTY_TIME_TO_LIVE, DEFAULT_TIME_TO_LIVE );
        MutableConfiguration<String, byte [ ]> configuration = new MutableConfiguration<String, byte [ ]>( ).setTypes( String.class, byte [ ].class )
                .setExpiryPolicyFactory( CreatedExpiryPolicy.factoryOf( new Duration( TimeUnit.SECONDS, nTimeToLive ) ) );
        initCache( CACHE_NAME, configuration );
        _bActive = ( _cache != null ) && CacheConfigUtil.isDistributed( _cache );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName( )
    {
        return CACHE_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isActive( )
    {
        return _bActive;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String store( byte [ ] content )
    {
        if ( !_bActive || ( content == null ) )
        {
            return null;
        }
        String strId = UUID.randomUUID( ).toString( );
        put( strId, content );
        // A failed write is only logged by the core: the file then stays in the session
        return containsKey( strId ) ? strId : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte [ ] load( String strId )
    {
        return get( strId );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete( String strId )
    {
        remove( strId );
    }
}
