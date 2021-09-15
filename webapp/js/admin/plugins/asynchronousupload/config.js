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
		sImg = '<svg class="bd-placeholder-img" width="40" height="40" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Placeholder: ' + mimeTypeDisplay + '" preserveAspectRatio="xMidYMid slice" focusable="false"><title>Placeholder</title><rect width="100%" height="100%" fill="#9e9e9e"></rect><text x="15%" y="50%" fill="#fff" dy=".3em" style="text-transform:uppercase;">' + mimeTypeDisplay + '</text></svg>';
	}
	var strName = ( (jsonData.fileCount == 1) ? jsonData.files.name : jsonData.files[index].name );
   	strCode ="<div class=\"list-file-item\"><figure>" + sImg + "</figure><div><label for=\"" + checkboxPrefix + index + "\"><input type=\"checkbox\" class=\"uploaded_check\" name=\"" + checkboxPrefix + index + "\" id=\"" + checkboxPrefix + index + "\" ><span class=\"truncate\" title=\"" + strName + "\">" + strName + "</span><small>[ " + sizeDisplay + " ]</small></label></div><div><a href=\"#\" class=\"btn btn-danger button is-danger is-small deleteSingleFile\" fieldName=\"" + fieldName + "\" handlerName=\"" + handler + "\" index=\"" + index + "\"><i class=\"fa fa-trash fa-fw\"></i></a></div></div>";
   	return strCode;
}