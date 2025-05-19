/*
 * jQuery File Upload Plugin JS Example 8.9.1
 * https://github.com/blueimp/jQuery-File-Upload
 *
 * Copyright 2010, Sebastian Tschan
 * https://blueimp.net
 *
 * Licensed under the MIT license:
 * http://www.opensource.org/licenses/MIT
 */
/*  global $, window                     */
const defaultImg = 'themes/shared/images/none.svg';
$(function (){
    'use strict';
    const uploadButton = $( '<button/>' )
        .addClass( 'visually-hidden' )
        .prop('disabled', true )
        .text( 'Processing...' )
        .on('click', function () {
            const $this = $(this),
                data = $this.data();
                $this
                    .off('click')
                    .text('Abort')
                    .on('click', function () {
                        $this.remove();
                        data.abort();
                    });
                data.submit().always( function () {
                    $this.remove();
                });
        });

    // Initialize the jQuery File Upload widget & show add file(s) button(s):
    $( '.${handler_name}' ).each( handlerDisplayImages );
    $( '.${handler_name}' ).on('click', function(){
        $( '.invalid-feedback' ).remove();
        $( '.dropzone label' ).removeClass('main-danger-color');
    });

    $(document).on( 'click', '.${handler_name}${fieldname}', {} , handlerDisplayImages );

    function handlerDisplayImages(){
        const nof=$(this).data('nof') , 
            mfs = $(this).data('mfs'),
            atf = $(this).data('atf'),
            nfu = $(this).attr('data-nfu'),
            msgMaxFileSize='#i18n{asynchronousupload.error.fileTooLarge}'.replace( '{0}', prettySize( mfs ) ),
            msgMaxNumberOfFiles='#i18n{asynchronousupload.error.maxNumberOfFiles}'.replace( '{0}', nof ),
            msgAcceptFileTypes='#i18n{asynchronousupload.error.acceptFileTypes}'.replace( '{0}', atf ).replace( ',', ' ou ' );
        $(this).fileupload({
            // Uncomment the following to send cross-domain cookies:
            // xhrFields: {withCredentials: true},
            dataType: 'json',
            url: '${base_url}${upload_url}',
            disableImageResize: /Android(?!.*Chrome)|Opera/
                .test(window.navigator && navigator.userAgent),
            imageMaxWidth: ${imageMaxWidth},
            imageMaxHeight: ${imageMaxHeight},
            previewMaxWidth: ${previewMaxWidth},
            previewMaxHeight: ${previewMaxHeight},
            imageCrop: false, // Force cropped images
            dropZone: $(this).parentsUntil('.step-group'),
            maxNumberOfFiles: nof,
            maxFileSize: mfs,
            formData: [ {name:'fieldname',value:$(this)[0].name}, {name:'asynchronousupload.handler', value:'${handler_name}'} ],
            messages: {
                maxFileSize: msgMaxFileSize,
                maxNumberOfFiles: msgMaxNumberOfFiles,
                acceptFileTypes: msgAcceptFileTypes,
            },
            singleFileUploads: false
        }).on('fileuploadprocessalways', function( e, data ){
            const index = data.index,
                file = data.files[index],
                fieldName = data.formData[0].value;
                if ( file.error ) {
                    updateErrorBox( file.error, fieldName );
                }
        }).on( 'fileuploadprogressall', function( e, data ){
            const fieldName = this.name;
            $(' #progress_' + fieldName ).show( );
            const bar = $(' #progress-bar_' + fieldName);
            let progress = parseInt( data.loaded / data.total * 100, 10);
            bar.html( progress + '%'  );
            bar.css( 'width', progress + '%' );
            if ( progress >= 100 ) {
                $(' #progress_' + fieldName).hide();
            }
        }).on( 'fileuploaddone', function( e, data ){
            const jsonData = { "fieldname": this.name, "asynchronousupload.handler":"${handler_name}", "nof": nof };
            $( '.select-all' ).removeClass( 'invisible' );
            $( '#btn-select-all-${handler_name}' ).removeClass( 'invisible' );
            const fieldName = this.name;
            if( data.result.form_error != undefined ){
                updateErrorBox( data.result.form_error, fieldName );
            } else {
                formDisplayUploadedFiles${fieldname}( jsonData, '${checkBoxPrefix}' );
            }

        }).on('fileuploadfail', function( e, data ){
            const fieldName = data.formData[0].value;
            if( mapFileErrors.get( fieldName) != undefined  ){
                mapFileErrors.set( fieldName, "#i18n{asynchronousupload.error.uploadFile}" );
                updateErrorBox( "#i18n{asynchronousupload.error.uploadFile}", fieldName );
                $(' #progress_' + fieldName).hide();
            }
        }).prop('disabled', !$.support.fileInput).parent().addClass( $.support.fileInput ? undefined : 'disabled' );
        if( !this.parentNode.classList.contains( 'fileinput-button' ) ){
            this.parentNode.className=this.parentNode.className + ' fileinput-button';
        }
        if( nfu >= nof ){
            $('#' + $(this)[0].name ).prop('disabled', true )
            $('#' + $(this)[0].name ).closest('.dropzone').addClass('inactive' )
        }
    };

    $('[id^="${submitPrefix}"]').click(function(event) {
        event.preventDefault( );
    });
    
    $(document).on('dragover', function (e) {
        const dropZones = $('.dropzone'),
            timeout = window.dropZoneTimeout,
            hoveredDropZone = $(e.target).closest( dropZones );
        if (timeout) {
            clearTimeout(timeout);
        } else {
            dropZones.addClass('in');
        }
        dropZones.not( hoveredDropZone ).removeClass( 'hover' );
        hoveredDropZone.addClass('hover');
        window.dropZoneTimeout = setTimeout(function () { 
            window.dropZoneTimeout = null; 
            dropZones.removeClass('in hover');
        }, 100);
    });

    $('.${handler_name}').each(function(){
        // _file_deletion_nIt0_attribute373
        const fieldName = $(this)[0].name, nbFiles = $(<#noparse>`_file_deletion_${fieldName}`</#noparse>).length;
        if( sessionStorage.getItem( fieldName ) != null && nbFiles === 0 ){
            const data = JSON.parse( sessionStorage.getItem( fieldName ) );
            if( data != null ){
                const jsonData = { "fieldname": fieldName, "asynchronousupload.handler":"${handler_name}", "nof": data.length };
                formDisplayUploadedFiles${fieldname}( jsonData, '${checkBoxPrefix}' );
            }
        }
    });


});

/**
 * Sets the files list
 * @param jsonData data
*/
function formDisplayUploadedFiles${fieldname}( jsonData, cbPrefix  ){
    $.getJSON('${base_url}jsp/site/plugins/asynchronousupload/DoRemoveFile.jsp', jsonData,
    function ( data ) { 
        const fieldName = data.field_name, 
            errorFileName = $( '#_file_error_box_' + fieldName ), 
            groupFiles = errorFileName.closest('.group-files'),
            groupInfo=groupFiles.find('.file-input.fileinput-button'),
            jFieldName =  $( '#' + fieldName );
        const parentInput = jFieldName.parent();
        parentInput.removeClass( 'is-invalid' )
        
       // reinitMsg( groupFiles  );
        
        if ( fieldName != null ) {
            if ( data.fileCount == 0 ) {
                jFieldName.attr( 'data-nfu', '0' );
                jFieldName.removeAttr( 'disabled' );
                jFieldName.removeClass( 'disabled' );
                jFieldName.off( "click");
                jFieldName.closest('.dropzone').removeClass('inactive' )
                $( "#_file_deletion_label_" + fieldName ).hide();
            } else {
                let strContent = '',
                    imgContent = '',
                    imgTag = '',
                    hasImage = groupFiles[0].classList.contains( 'image-file' ),
                    checkboxPrefix = cbPrefix + fieldName;
                for ( var index = 0; index < data.fileCount; index++ ) {
                    if( hasImage ){
                        imgContent = ( (data.fileCount == 1) ? data.files.preview : data.files[index].preview );
                        if ( typeof (imgContent) == "string" && imgContent.length > 0 ){
                            // imgTag = " <img src=" + "'" + imgContent + "'" + "alt='' " + " width='${previewMaxWidth}' height='${previewMaxHeight}'/>";
                            imgTag = '<img src="' + imgContent + '" alt="" width="${previewMaxWidth}" class="img-fluid img-thumbnail" >';
                        }
                    }
                    strContent = strContent + getTemplateUploadedFile( fieldName, index, checkboxPrefix, data, imgTag,'${handler_name}', '${base_url}', '#i18n{asynchronousupload.action.delete.name}');
                }

                sessionStorage.setItem( fieldName, JSON.stringify( data.files ) );

                $("#_file_deletion_" + fieldName).html( strContent );
                $("#_file_deletion_label_" + fieldName).show();

                if ( data.fileCount >= jsonData.nof ){
                    jFieldName.attr('data-nfu', jsonData.nof );
                    jFieldName.attr('data-nbuploadedfiles', jsonData.nof );
                    jFieldName.prop('disabled', true );
                    jFieldName.addClass('disabled');
                    jFieldName.closest('.dropzone').addClass('inactive' )
                    groupFiles.addClass('no-file');
                    const errMsg=$('#msg_' + fieldName );
                    if( errMsg.length === 0  ){
                        jFieldName.attr('aria-labelledby','msg_' + fieldName )
                        groupInfo.after('<p id="msg_' + fieldName + '" class="group-file-info text-muted p-2 mt-1"><span class="fa fa-exclamation-circle text-warning"></span> #i18n{asynchronousupload.info.maxNumberOfFiles}</p>');
                    }
                } else {
                    jFieldName.attr('data-nfu', data.fileCount );
                    jFieldName.attr('data-nbuploadedfiles', data.fileCount );
                    jFieldName.removeAttr( 'disabled' );
                    jFieldName.removeClass( 'disabled' );
                    jFieldName.closest('.main-danger-color').removeClass('main-danger-color' )
                    jFieldName.off( "click");
                    jFieldName.removeAttr( 'aria-labelledby' );
                    jFieldName.closest('.dropzone').removeClass('inactive' )
                    $( '#msg_' + fieldName ).remove();
                }
            }
        }

        $( document ).find('.deleteSingleFile').on('click', '.deleteSingleFile', function(event) {
            event.preventDefault( );
            deleteFile( event );
        });

        
    });
}

/**
 * Removes a file on click
 */
$( document ).on('click', '.deleteSingleFile', function(event) {
    event.preventDefault( );
    deleteFile( event );
});

function deleteFile( ev ){
    const index = ev.currentTarget.getAttribute( 'index' ); 
    const fieldName = ev.currentTarget.getAttribute( 'fieldname' );
    const imgPreview =  document.querySelector( <#noparse>`#${fieldName}_preview`</#noparse> );
    if( imgPreview != null ){
        imgPreview.src=defaultImg;
    }
    const itemId = ev.currentTarget.dataset.item;
    const item = document.querySelector( itemId );
    item.remove();
    const handlerName = ev.currentTarget.getAttribute( 'handlername' );
    const nof=$('.${handler_name}${fieldname}').data('nof');
    const jsonData = { "fieldname": fieldName, "asynchronousupload.handler": handlerName, "field_index": index, "nof": nof };
    formDisplayUploadedFiles${fieldname}( jsonData, '${checkBoxPrefix}' );
}

/**
 * Update error
 */
function updateErrorBox( errorMessage, fieldName ){
    let strContent = '';
    const errorFileName=$( '#_file_error_box_' + fieldName ), groupFiles = errorFileName.closest('.group-files');
    if( !$(groupFiles).next().hasClass('invalid-feedback') ){
        if ( errorMessage != null && errorMessage !='' && errorMessage !== undefined || mapFileErrors.size > 0 ) {
            strContent = mapFileErrors.size > 0 ? mapFileErrors.get( fieldName ) : errorMessage;
            if( mapFilesNumber.get( fieldName ) > 1 ){
                if( !groupFiles.hasClass( 'is-invalid' ) || strContent !=''  ){
                    groupFiles.after( '<div class="invalid-feedback" role="status">' + strContent + '</div>' ).show( );
                }
            } else {
                if( !groupFiles.hasClass( 'is-invalid' ) || strContent !=''  ){
                    groupFiles.addClass( 'is-invalid' ).after( '<div class="invalid-feedback" role="status">' + strContent + '</div>' ).show( );
                }
            }
            mapFileErrors.delete(fieldName);
            mapFilesNumber.delete(fieldName);
        } else {
            groupFiles.removeClass( 'is-invalid' );
        }
    }
}

/**
 * Removes clean msg
 */
function reinitMsg( group ){
    const fileInfo = $(document).find('.group-file-info'), invalidFeedback= $(document).find('.invalid-feedback');
    fileInfo.remove();
    invalidFeedback.remove();
    $( '.dropzone label' ).removeClass('main-danger-color');
    group.removeClass( 'is-invalid' ).removeClass( 'no-file' );
}
