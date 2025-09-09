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

$(function () {
'use strict';
var uploadButton = $('<button/>')
    .addClass('btn btn-primary')
    .prop('disabled', true)
    .text('Processing...')
    .on('click', function () {
        var $this = $(this),
            data = $this.data();
        $this
            .off('click')
            .text('Abort')
            .on('click', function () {
                $this.remove();
                data.abort();
            });
        data.submit().always(function () {
            $this.remove();
        });
    });

    // Initialize the jQuery File Upload widget & show add file(s) button(s):
    $(document).ready(function(){
        $(".file-input-text-noscript").hide();
        $(".file-input-text-js").show();
        $('.${handler_name}').each(handlerDisplayImages)
    });

   // Add file(s) button(s) onclick event listener.
    $(document).on( 'click' , '.file-input-text-js', function(e){
        e.stopImmediatePropagation();
        $(this).closest("div").find( 'input[type=file]' ).trigger('click');
    });

    function handlerDisplayImages(){
        var nof=$(this).data('nof') , 
            mfs = $(this).data('mfs'),
            msgMaxFileSize='#i18n{asynchronousupload.error.fileTooLarge}'.replace( '{0}', prettySize( mfs ) ),
            msgMaxNumberOfFiles='#i18n{asynchronousupload.error.maxNumberOfFiles}'.replace( '{0}', nof );
        
        $(this).fileupload({
            // Uncomment the following to send cross-domain cookies:
            //xhrFields: {withCredentials: true},
            dataType: 'json',
            url: '${base_url}${upload_url}',
            disableImageResize: /Android(?!.*Chrome)|Opera/
                .test(window.navigator && navigator.userAgent),
            imageMaxWidth: ${imageMaxWidth},
            imageMaxHeight: ${imageMaxHeight},
            previewMaxWidth: ${previewMaxWidth},
            previewMaxHeight: ${previewMaxHeight},
            <#if splitFile> maxChunkSize: ${maxChunkSize},</#if>
            imageCrop: false, // Force cropped images
            dropZone: $(this),
            maxNumberOfFiles: nof,
            maxFileSize: mfs,
            formData: [{name:'fieldname',value:$(this)[0].name}, {name:'asynchronousupload.handler', value:'${handler_name}'}],
            messages: {
                maxFileSize: msgMaxFileSize,
                maxNumberOfFiles: msgMaxNumberOfFiles,
            },
            singleFileUploads:false
        }).on('fileuploadprocessalways', function (e, data) {
            var nbFilesUploaded = $( '#_file_deletion_' + $(this)[0].name + ':visible > .list-file-item' ).length;
            var index = data.index,
                file = data.files[index],
                fieldName = data.formData[0].value;
            if ( nbFilesUploaded == nof ){
                updateErrorBox( 'Attention nombre maximum de fichier atteint !', fieldName )
                return false;
            }
            if (file.error) {
                updateErrorBox( file.error, fieldName )
            }
        }).on('fileuploadprogressall', function (e, data) {
            var progress = parseInt(data.loaded / data.total * 100, 10);
            var fieldName = this.name;
            var bar = $(' #progress_' + fieldName + ' .progress-bar');
            bar.html( progress + '%'  );
            bar.css( 'width', progress + '%' );

            $(' #progress_' + fieldName).show( );

            if ( progress >= 100 ){
                $(' #progress_' + fieldName).hide();
            }
        }).on('fileuploaddone', function (e, data) {
            var jsonData = {"fieldname":this.name, "asynchronousupload.handler":"${handler_name}", "result": data.result};
            formDisplayUploadedFiles${fieldname}(jsonData, '${checkBoxPrefix}');

        }).on('fileuploadfail', function (e, data) {
            var fieldName = data.formData[0].value;
            updateErrorBox( "#i18n{asynchronousupload.error.uploadFile}", fieldName );
            $(' #progress_' + fieldName).hide();
        }).prop('disabled', !$.support.fileInput)
            .parent().addClass($.support.fileInput ? undefined : 'disabled');
        this.parentNode.className=this.parentNode.className + ' fileinput-button';

        var jsonData = {"fieldname":this.name, "asynchronousupload.handler":"${handler_name}"};
        formDisplayUploadedFiles${fieldname}(jsonData, '${checkBoxPrefix}');
    };

    $('[id^="${submitPrefix}"]').click(function(event) {
        event.preventDefault( );
    });

});

/**
 * Sets the files list
 * @param jsonData data
 */
 function formDisplayUploadedFiles${fieldname}( jsonData, cbPrefix ){

     if( jsonData.result && jsonData.result.form_error )
     {
         updateErrorBox(jsonData.result.form_error, jsonData.fieldname);
     }
     else
     {
        $.getJSON('${base_url}jsp/admin/plugins/asynchronousupload/DoRemoveFile.jsp', jsonData,
        function (data) {
             var fieldName = data.field_name;
            var errorFileName=$( '#_file_error_box_' + fieldName ),
            groupFiles = errorFileName.closest('.group-files');
            groupFiles.removeClass( 'is-invalid' );
            if ( fieldName != null ) {
                if ( data.fileCount == 0 ) {
                    $( "#_file_deletion_label_" + fieldName ).hide();
                } else {
                    var strContent = "";
                    var checkboxPrefix = '${checkBoxPrefix}' + fieldName;
                    for ( var index = 0; index < data.fileCount; index++ ) {
                        var imgContent = ( (data.fileCount == 1) ? data.files.preview : data.files[index].preview );
                        strContent = strContent + getAdminTemplateUploadedFile(fieldName, index, checkboxPrefix, data, imgContent ,'${handler_name}', '${base_url}');
                    }
                    $("#_file_deletion_" + fieldName).html( strContent );
                    $("#_file_deletion_label_" + fieldName).show();

                    $(document).on('click', '.btn-rm-all', {} ,function(event) {
                        removeFile${checkBoxPrefix}( fieldName, '${handler_name}', '${base_url}');
                        return false;
                    });

                    var uploadedItems = $(document).find('.uploaded_check');
                    if ( uploadedItems.length > 0 ){
                        uploadedItems.on( 'click', function(){
                            if( $(this).prop('checked') ){
                                $('#rmAll' + fieldName ).prop('disabled','');
                            } else {
                                var upChecked =  $(document).find('.uploaded_check:checked');
                                if( upChecked.length == 0 ){
                                    $('#rmAll' + fieldName ).prop('disabled','disabled');
                                }
                            }
                        })
                    }
                }
            }
        });
     }
}

/**
 * Removes a file
 * @param action the action button name
 */
function removeFile${checkBoxPrefix}( fieldName, handlerName, baseUrl ) {
    // build indexes to remove
    var strIndexes = '';
    var indexesCount = 0;
    var checkboxPrefix = '${checkBoxPrefix}' + fieldName;
    $('[name^="' + checkboxPrefix + '"]:checked' ).each( function() {
        if (this.checked) {
            if ( indexesCount > 0 ){
                strIndexes = strIndexes + ",";
            }
            indexesCount++;
            var index = this.name.match( checkboxPrefix + "(\\d+)")[1];
            strIndexes = strIndexes + index;
        }
    });
    if ( !indexesCount ){
        return;
    }
    var jsonData = {"fieldname":fieldName, "asynchronousupload.handler":handlerName, "field_index": strIndexes};
    formDisplayUploadedFiles${fieldname}( jsonData, null, '${checkBoxPrefix}');
}


/**
 * Removes a file on click
 */
$(document).on('click', '.deleteSingleFile', function (event) {
    var index = event.currentTarget.getAttribute("index");
    var fieldName = event.currentTarget.getAttribute("fieldName")
    var handlerName = event.currentTarget.getAttribute("handlerName");
    var jsonData = {"fieldname":fieldName, "asynchronousupload.handler":handlerName, "field_index": index};
    formDisplayUploadedFiles${fieldname}(jsonData, null, '${checkBoxPrefix}');
    event.preventDefault( );
});

function updateErrorBox( errorMessage, fieldName ){
    var errorClassName = "error_" + fieldName;
    $( '.' + errorClassName ).remove()
    if ( errorMessage != null && errorMessage !='' && errorMessage !== undefined ) {
        if ( errorMessage === undefined ){ errorMessage='' };
        $( '#_file_error_box_' + fieldName ).addClass( 'is-invalid' );
		$( '#_file_error_box_' + fieldName ).after( '<div class="invalid-feedback ' + errorClassName + '">' + errorMessage + '</div>' );
		$( '#_file_error_box_' + fieldName ).show( );
    } else {
        $( '#_file_error_box_' + fieldName ).hide( );
    }
}