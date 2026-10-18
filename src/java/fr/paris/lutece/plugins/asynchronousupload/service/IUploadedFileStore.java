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

/**
 * Storage of the content of the uploaded files outside the HTTP session. With several instances the session is written to the
 * shared session store after each request that reads it: an uploaded file kept in the session would be written again on every
 * request. The content is written once to this store and the session only keeps its id.
 */
public interface IUploadedFileStore
{
    /**
     * Tells whether the content of the uploaded files is kept outside the session
     *
     * @return true when the files are stored here, false when they stay in the session (one instance)
     */
    boolean isActive( );

    /**
     * Stores the content of an uploaded file
     *
     * @param content
     *            the content
     * @return the id of the stored content, or null when the content could not be stored and must stay in the session
     */
    String store( byte [ ] content );

    /**
     * Loads the content of an uploaded file
     *
     * @param strId
     *            the id given by {@link #store(byte[])}
     * @return the content, or null when it is no longer stored
     */
    byte [ ] load( String strId );

    /**
     * Removes the content of an uploaded file
     *
     * @param strId
     *            the id given by {@link #store(byte[])}
     */
    void delete( String strId );
}
