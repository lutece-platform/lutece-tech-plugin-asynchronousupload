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

   strCode ="<div class=\"checkbox\" id=\"_file_uploaded_" + fieldName + index + "\"><label class=\"col-xs-12 col-sm-12 col-md-12 col-lg-12\">  \
	<input type=\"checkbox\"  \
		name=\"" + checkboxPrefix + index + "\"  \
		id=\"" + checkboxPrefix + index + "\"  \
	/>  \
	&#160;" + ( (jsonData.fileCount == 1) ? jsonData.files.name : jsonData.files[index].name ) + sizeDisplay +
	"</label></div><div class=\"col-xs-12 col-sm-12 col-md-12 col-lg-12\">"+imgTag+"</div>";
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