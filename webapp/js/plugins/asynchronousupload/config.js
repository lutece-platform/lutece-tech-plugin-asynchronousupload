function getTemplateUploadedFile(fieldName, index, checkboxPrefix, jsonData, imgTag) {
	
	 var sizeDisplay = "";
     var sizeTemp;
     var octetUnit;
     var octetNumber;
     if ((typeof jsonData.files[index] != 'undefined' && jsonData.files[index].size != 'undefined' ) || (jsonData.files.size != 'undefined' )) {

         sizeTemp = (jsonData.fileCount == 1) ? jsonData.files.size : jsonData.files[index].size;

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
         sizeDisplay = " (" + Math.floor(octetNumber) + " " + octetUnit + ")";
     }
	return "<div class=\"checkbox\" id=\"_file_uploaded_" + fieldName + index + "\"><label class=\"col-xs-12 col-sm-12 col-md-12 col-lg-12\">  \
	<input type=\"checkbox\"  \
		name=\"" + checkboxPrefix + index + "\"  \
		id=\"" + checkboxPrefix + index + "\"  \
	/>  \
	&#160;" + ( (jsonData.fileCount == 1) ? jsonData.files.name : jsonData.files[index].name ) + sizeDisplay +
	"</label></div><div class=\"col-xs-12 col-sm-12 col-md-12 col-lg-12\">"+imgTag+"</div>";
}