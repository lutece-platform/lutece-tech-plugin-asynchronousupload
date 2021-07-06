function getAdminTemplateUploadedFile( fieldName, index, checkboxPrefix, jsonData, imgContent, handler ) {
	var strCode='', sizeDisplay='', sizeTemp='', octetUnit='', octetNumber='', fileName='', fileDisplayName='', mimeType='', mimeTypeDisplay='', sImg='';
	if ( (typeof jsonData.files[index] != 'undefined' && jsonData.files[index].size != 'undefined' ) || (jsonData.files.size != 'undefined' ) ) {
		sizeTemp = (jsonData.fileCount == 1) ? jsonData.files.size : jsonData.files[index].size;
		fileName = (jsonData.fileCount == 1) ? jsonData.files.name : jsonData.files[index].name;
		mimeType = (jsonData.fileCount == 1) ? jsonData.files.preview : jsonData.files[index].preview;
		fileDisplayName=fileName;
		mimeTypeDisplay = mimeType !='' ? mimeType.match(/[^:/]\w+(?=;|,)/)[0] : fileName.substr( ( fileName.lastIndexOf(".") + 1 ), ( fileName.length - fileName.lastIndexOf(".") ) );
		if (sizeTemp < 1024) {
			octetUnit = "o";
			octetNumber = sizeTemp;
		} else if (sizeTemp < 1024 * 1024) {
			octetUnit = "Ko";
			octetNumber = sizeTemp/1024;
		} else {
			octetUnit = "Mo";
			octetNumber = sizeTemp/(1024*1024);
		}
		sizeDisplay = "" + Math.floor(octetNumber) + " " + octetUnit;
   }

   if( fileDisplayName.length > 24 ){
	   fileDisplayName = fileName.substr(0,20) + '...';
   }

	if ( typeof (imgContent) == "string" && imgContent.length > 0) {
		sImg = '<img src=' + '"' + imgContent + '"' + 'alt="" style="width:40px;" />';
	} else {
		sImg='<div aria-hidden="true" class="icon-type-item" data-type="' + mimeTypeDisplay + '"></div>';
	}

	var strName = ( (jsonData.fileCount == 1) ? jsonData.files.name : jsonData.files[index].name );
   strCode ="<div class=\"list-group-item list-file-item\" id=\"_file_uploaded_" + fieldName + index + "\"><label for=\"" + checkboxPrefix + index + "\"><input type=\"checkbox\" class=\"uploaded_check\" name=\"" + checkboxPrefix + index + "\" id=\"" + checkboxPrefix + index + "\" ><figure>" + sImg + "<figcaption><span class=\"truncate\" title=\"" + strName + "\">" + strName + "</span><small>[ " + sizeDisplay + " ]</small></figcaption></figure></label><a href=\"#\" class=\"btn btn-danger deleteSingleFile\" fieldName=\"" + fieldName + "\" handlerName=\"" + handler + "\" index=\"" + index + "\"><i class=\"fa fa-trash fa-fw\"></i></a></div>";
   return strCode;
}
