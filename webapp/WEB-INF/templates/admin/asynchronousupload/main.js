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

/* global $, window */
/* admin config */
/* Maps to manage files field errors */
var mapFileErrors = new Map();
var mapFilesNumber = new Map();

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
            maxFileSize: ${maxFileSize},
            formData: [{name:'fieldname',value:$(this)[0].name}, {name:'asynchronousupload.handler', value:'${handler_name}'}],
            messages: {
                maxFileSize: "#i18n{asynchronousupload.error.fileTooLarge}",
                maxNumberOfFiles: "#i18n{asynchronousupload.error.maxNumberOfFiles}",

            },
            singleFileUploads:false
        }).on('fileuploadprocessalways', function (e, data) {
            var index = data.index,
                file = data.files[index],
                fieldName = data.formData[0].value;
            if (file.error) {
                updateErrorBox( file.error, fieldName )
            }
        }).on('fileuploadprogressall', function (e, data) {
            var progress = parseInt(data.loaded / data.total * 100, 10);
            var fieldName = this.name;
            var bar = $(' #progress-bar_' + fieldName);
            bar.html( progress + '%'  );
            bar.css( 'width', progress + '%' );

            $(' #progress_' + fieldName).show( );

            if ( progress >= 100 )
            {
                $(' #progress_' + fieldName).hide();
            }
        }).on('fileuploaddone', function (e, data) {

            var jsonData = {"fieldname":this.name, "asynchronousupload.handler":"${handler_name}"};
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

    // prevent user from quitting the page before his upload ended.
    $(document).on('click','[value^="${deletePrefix}"]', {} ,function(event) {
        if(this.getAttribute("nojs") === null) {
        var fieldName = this.value.match("${deletePrefix}(.*)")[1];
        removeFile${checkBoxPrefix}(fieldName, '${handler_name}', '${base_url}');
        event.preventDefault( );
        }
    });

});

/**
 * Sets the files list
 * @param jsonData data
 */
 function formDisplayUploadedFiles${fieldname}( jsonData, cbPrefix ){
    $.getJSON('${base_url}jsp/site/plugins/asynchronousupload/DoRemoveFile.jsp', jsonData,
    function (data) {
        // updateErrorBox( data.form_error, fieldName );
        var fieldName = data.field_name;
        var errorFileName=$( '#_file_error_box_' + fieldName ), 
        groupFiles = errorFileName.closest('.group-files');
        //$('.invalid-feedback').remove( );
		groupFiles.removeClass( 'is-invalid' );
        if ( fieldName != null ) {
            if ( data.fileCount == 0 ) {
                $( "#_file_deletion_label_" + fieldName ).hide();
            } else {
                var strContent = "";
                var checkboxPrefix = cbPrefix + fieldName;
                for ( var index = 0; index < data.fileCount; index++ ) {
                    var imgContent = ( (data.fileCount == 1) ? data.files.preview : data.files[index].preview );
                    var imgTag = "";
                    if ( typeof (imgContent) == "string" && imgContent.length > 0) {
                        imgTag = " <img src=" + "'" + imgContent + "'" + "alt='' " + " width='${previewMaxWidth}' height='${previewMaxHeight}'/>";
                    }
                    strContent = strContent + getTemplateUploadedFile(fieldName, index, checkboxPrefix, data, imgTag,'${handler_name}', '${base_url}');
                }
                $("#_file_deletion_" + fieldName).html( strContent );
                $("#_file_deletion_label_" + fieldName).show();
            }
        }
    });
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
        if (this.checked)
        {
            if ( indexesCount > 0 )
            {
                strIndexes = strIndexes + ",";
            }
            indexesCount++;
            var index = this.name.match( checkboxPrefix + "(\\d+)")[1];
            strIndexes = strIndexes + index;
        }
    });

    if ( !indexesCount )
    {
        return;
    }

    var jsonData = {"fieldname":fieldName, "asynchronousupload.handler":handlerName, "field_index": strIndexes};
    formDisplayUploadedFiles${fieldname}(jsonData, null, '${checkBoxPrefix}');
}

/**
 * Removes a file on click
 */
$(document).on('click', '.deleteSingleFile', function (event) {
    event.preventDefault( );
    var index = event.currentTarget.getAttribute("index");
    var fieldName = event.currentTarget.getAttribute("fieldName")
    var handlerName = event.currentTarget.getAttribute("handlerName");
    var jsonData = {"fieldname":fieldName, "asynchronousupload.handler":handlerName, "field_index": index};
    formDisplayUploadedFiles${fieldname}(jsonData, null, '${checkBoxPrefix}');
    return false;
});


function updateErrorBox( errorMessage, fieldName ){
    var errorFileName=$( '#_file_error_box_' + fieldName ), 
        groupFiles = errorFileName.closest('.group-files');
    if ( errorMessage != null && errorMessage !='' && errorMessage !== undefined || mapFileErrors.size > 0 ) {
       var strContent = mapFileErrors.size > 0 ? mapFileErrors.get( fieldName ) : errorMessage;
		if( mapFilesNumber.get( fieldName ) > 1 ){
			errorFileName.after( '<div class="invalid-feedback">' + strContent + '</div>' ).show( );
        } else {
			if( !groupFiles.hasClass( 'is-invalid' ) ){
                groupFiles.addClass( 'is-invalid' ).after( '<div class="invalid-feedback">' + strContent + '</div>' ).show( );
            }
        }
        mapFileErrors.delete(fieldName);
        mapFilesNumber.delete(fieldName);
    } else {
        //$('.invalid-feedback').remove( );
		groupFiles.removeClass( 'is-invalid' );
    }

}

// function updateErrorBox( errorMessage, fieldName ){
//     if ( errorMessage != null && errorMessage !='' && errorMessage !== undefined || mapFileErrors.size > 0 ) {
//         if ( errorMessage === undefined ){ errorMessage='' };
//         var strContent = mapFileErrors.size > 0 ? mapFileErrors.get( fieldName ) : errorMessage;
// 		if( mapFilesNumber.get( fieldName ) > 1 ){
// 			$( '#_file_error_box_' + fieldName ).after( '<div class="invalid-feedback">' + strContent + '</div>' ).show( );
// 		} else {
// 			var groupFile = $( '#_file_error_box_' + fieldName ).closest('.group-files.one-file');
// 			groupFile.addClass( 'is-invalid' ).after( '<div class="invalid-feedback">' + strContent + '</div>' ).show( );
// 		}
//         mapFileErrors.delete(fieldName);
//         mapFilesNumber.delete(fieldName);
//     } else {
//         $( '#_file_error_box_' + fieldName ).hide( );
// 		var groupFile = $( '#_file_error_box_' + fieldName ).closest('.group-files.one-file');
// 		groupFile.removeClass( 'is-invalid' );
//     }
// }