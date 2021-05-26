function getTemplateUploadedFile( fieldName, index, checkboxPrefix, jsonData, imgTag, handler, baseUrl ) {
	var strCode='', sizeDisplay='', sizeTemp='', octetUnit='', octetNumber='', fileName='', fileDisplayName='', mimeType='', mimeTypeDisplay='';

	if ( (typeof jsonData.files[index] != 'undefined' && jsonData.files[index].size != 'undefined' ) || (jsonData.files.size != 'undefined' ) ) {

		sizeTemp = (jsonData.fileCount == 1) ? jsonData.files.size : jsonData.files[index].size;
		fileName = (jsonData.fileCount == 1) ? jsonData.files.name : jsonData.files[index].name;
		mimeType = (jsonData.fileCount == 1) ? jsonData.files.preview : jsonData.files[index].preview;
		
		fileDisplayName=fileName;

		mimeTypeDisplay = mimeType !='' ? mimeType.match(/[^:/]\w+(?=;|,)/)[0] : fileName.substr( ( fileName.lastIndexOf(".") + 1 ), ( fileName.length - fileName.lastIndexOf(".") ) );

		if (sizeTemp < 1024) {
			octetUnit = "o";
			octetNumber = sizeTemp;
		}
		else if (sizeTemp < 1024 * 1024) {
			octetUnit = "Ko";
			octetNumber = sizeTemp/1024;
		}
		else {
			octetUnit = "Mo";
			octetNumber = sizeTemp/(1024*1024);
		}
		sizeDisplay = "" + Math.floor(octetNumber) + " " + octetUnit;
   }

   if( fileDisplayName.length > 24 ){
	   fileDisplayName = fileName.substr(0,20) + '...';
   }

   strCode ='<div class="files-item" id="_file_uploaded_' + fieldName + index + '">' + 
   '<label class="files-item-label" for="' + checkboxPrefix + index + '"><input class="" type="checkbox" name="' + checkboxPrefix + index + '" id="' + checkboxPrefix + index + '">' + 
   '<a class="files-item-link" title="' + fileName + '" href="jsp/site/plugins/asynchronousupload/DoDownloadFile.jsp?fieldname=' + fieldName + '&field_index=' + index + '&fileName=' + fileName +'&asynchronousupload.handler='+ handler + '" data-type="' + mimeTypeDisplay + '" data-img="' + imgTag + '" ><span>' + fileDisplayName + '</span></a>' + 
   '<a href="#" class="text-danger deleteSingleFile" fieldName="' + fieldName + '" handlerName="' + handler + '" index="' + index + '"><i class="fa fa-times fa-fw"></i> </a><br><small>' + sizeDisplay + '</small>' +
   '</label></div>';
   return strCode;
}

function prettySize( bytes, separator=' ', postFix=''){
if (bytes) {
	const sizes = ['Octets', 'Ko', 'Mo', 'Go', 'To'];
	const i = Math.min(parseInt(Math.floor(Math.log(bytes) / Math.log(1024)).toString(), 10), sizes.length - 1);
	return `${(bytes / (1024 ** i)).toFixed(i ? 1 : 0)}${separator}${sizes[i]}${postFix}`;
}
return 'n/a';
}