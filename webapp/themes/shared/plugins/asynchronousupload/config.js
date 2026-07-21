function getTemplateUploadedFile( fieldName, index, checkboxPrefix, jsonData, imgTag, handler, deleteLabel, unit='' ) {
	let strCode='', sizeDisplay='', sizeTemp='', octetUnit='', octetNumber='', fileName='', fileDisplayName='', mimeType='', mimeTypeDisplay='';
	if ( (typeof jsonData.files[index] != 'undefined' && jsonData.files[index].size != 'undefined' ) || (jsonData.files.size != 'undefined' ) ) {
			if( jsonData.fileCount === 1 ){
				sizeTemp =jsonData.files.size;
				fileName = jsonData.files.name;
				mimeType = jsonData.files.preview;
			} else { 
				sizeTemp =jsonData.files[index].size;
				fileName = jsonData.files[index].name;
				mimeType = jsonData.files[index].preview;
			}
			fileDisplayName=fileName;
			if( mimeType != undefined ){
				mimeTypeDisplay = mimeType !='' ? mimeType.match(/[^:/]\w+(?=;|,)/)[0] : fileName.substr( ( fileName.lastIndexOf(".") + 1 ), ( fileName.length - fileName.lastIndexOf(".") ) );
				switch ( unit ) {
					case 'Mo':
						octetUnit = 'Mo';
						octetNumber = sizeTemp/(1024*1024);
						  break;
					case 'Ko':
						octetUnit = 'Ko';
						octetNumber = sizeTemp/1024;
						break;
					case 'o':
						octetUnit = "o";
						octetNumber = sizeTemp;
					  break;
					default:
					  if ( sizeTemp < 1024 ) {
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
				}
				sizeDisplay = '' + Number.parseFloat(octetNumber).toFixed(2) + ' ' + octetUnit;
		   }
		
		   if( fileDisplayName.length > 60 ){
			   fileDisplayName = fileName.substr(0,55) + '...';
		   }
		   const labelClassName = imgTag !='' ? 'image' : '';
		   
		   strCode = `<li class="files-item" id="_file_uploaded_${fieldName}${index}">
			<label class="files-item-label ${labelClassName}" for="${checkboxPrefix}${index}">
				${imgTag}
				<a class="files-item-link" title="${fileName}" href="jsp/site/plugins/asynchronousupload/DoDownloadFile.jsp?fieldname=${fieldName}&field_index=${index}&fileName=${fileName}&asynchronousupload.handler=${handler}" data-type="${mimeTypeDisplay}" data-img="">
					<span class="file-item-label">${fileDisplayName}</span>
					<span class="file-item-info">${sizeDisplay}</span>
				</a>
			</label>
			<button type="button" class="btn btn-link deleteSingleFile main-color p-0" data-item="#_file_uploaded_${fieldName}${index}" fieldname="${fieldName}" handlername="${handler}" index="${index}" title="${deleteLabel} ${fileDisplayName}" aria-label="${deleteLabel} ${fileDisplayName}"> 
				<svg class="paris-icon paris-icon-close" role="img" aria-labelledby="paris-icon-title-group" focusable="false">
					<use xlink:href="#paris-icon-close"></use>
				</svg> 
			</button>
			</li>`
		   // Store the imgTag in session storage with fieldName as key 
			storeImageTagInSession( `${fieldName}_${fileName}`, imgTag );
		   	return strCode;
		}
	}
	
	function prettySize( bytes, separator=' ', postFix=''){
	if (bytes) {
		const sizes = ['Octets', 'Ko', 'Mo', 'Go', 'To'];
		const i = Math.min(parseInt(Math.floor(Math.log(bytes) / Math.log(1024)).toString(), 10), sizes.length - 1);
		return `${(bytes / (1024 ** i)).toFixed(i ? 1 : 0)}${separator}${sizes[i]}${postFix}`;
	}
	return 'n/a';
	}

	// Add a function that add to session storage the imgTag set in getTemplateUploadedFile with fieldName as key
	function storeImageTagInSession( fieldName, imgTag ){
		if( typeof(Storage) !== "undefined" ) {
			sessionStorage.setItem( `asynchronousupload_imgtag_${fieldName}`, imgTag );
		}		
	}

