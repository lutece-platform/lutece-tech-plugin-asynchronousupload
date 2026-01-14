/**
 * Asynchronous Upload - Admin Main JS (Uppy implementation)
 * @fileoverview Handles file uploads using Uppy library for back-office
 */

/** @type {string} */
const defaultImg = 'themes/shared/images/none.svg';

/** @type {Map<string, Uppy>} */
const uppyInstances = new Map();

document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.${handler_name}').forEach(initUppy);

    document.querySelectorAll('.${handler_name}').forEach(input => {
        input.addEventListener('click', () => {
            document.querySelectorAll('.invalid-feedback').forEach(el => el.remove());
            document.querySelectorAll('.dropzone label').forEach(el => el.classList.remove('main-danger-color'));
        });
    });
});

/**
 * Initializes Uppy instance for a file input element
 * @param {HTMLInputElement} inputElement - The file input element
 */
function initUppy(inputElement) {
    const fieldName = inputElement.name;
    const nof = parseInt(inputElement.dataset.nof) || 1;
    const mfs = parseInt(inputElement.dataset.mfs) || 0;
    const atf = inputElement.dataset.atf || '';
    const nfu = parseInt(inputElement.dataset.nfu) || 0;

    const msgMaxFileSize = `#i18n{asynchronousupload.error.fileTooLarge}`.replace('{0}', prettySize(mfs));
    const msgMaxNumberOfFiles = `#i18n{asynchronousupload.error.maxNumberOfFiles}`.replace('{0}', nof);
    const msgAcceptFileTypes = `#i18n{asynchronousupload.error.acceptFileTypes}`.replace('{0}', atf).replace(',', ' ou ');

    if (uppyInstances.has(fieldName)) {
        return;
    }

    const uppy = new Uppy.Uppy({
        id: fieldName,
        autoProceed: true,
        restrictions: {
            maxNumberOfFiles: nof,
            maxFileSize: mfs > 0 ? mfs : null,
            allowedFileTypes: atf ? atf.split(',').map(t => t.trim()) : null
        },
        locale: {
            strings: {
                exceedsSize: msgMaxFileSize,
                youCanOnlyUploadX: msgMaxNumberOfFiles,
                youCanOnlyUploadFileTypes: msgAcceptFileTypes
            }
        }
    });

    uppy.use(Uppy.XHRUpload, {
        endpoint: '${base_url}${upload_url}',
        fieldName: fieldName,
        formData: true,
        bundle: false,
        allowedMetaFields: ['fieldname', 'asynchronousupload.handler']
    });

    uppy.use(Uppy.ThumbnailGenerator, {
        thumbnailWidth: ${previewMaxWidth},
        thumbnailHeight: ${previewMaxHeight}
    });

    uppy.on('file-added', (file) => {
        uppy.setFileMeta(file.id, {
            fieldname: fieldName,
            'asynchronousupload.handler': '${handler_name}'
        });
    });

    uppy.on('upload-progress', (file, progress) => {
        const progressBar = document.getElementById('progress-bar_' + fieldName);
        const progressContainer = document.getElementById('progress_' + fieldName);
        if (progressBar && progressContainer) {
            const percent = Math.round((progress.bytesUploaded / progress.bytesTotal) * 100);
            progressBar.textContent = percent + '%';
            progressBar.style.width = percent + '%';
            progressContainer.style.display = 'block';
        }
    });

    uppy.on('complete', (result) => {
        const progressContainer = document.getElementById('progress_' + fieldName);
        if (progressContainer) {
            progressContainer.style.display = 'none';
        }

        if (result.successful.length > 0) {
            const response = result.successful[0].response?.body;
            if (response?.form_error) {
                updateErrorBox(response.form_error, fieldName);
            } else {
                document.querySelectorAll('.select-all').forEach(el => el.classList.remove('invisible'));
                const btnSelectAll = document.getElementById('btn-select-all-${handler_name}');
                if (btnSelectAll) btnSelectAll.classList.remove('invisible');

                const jsonData = {
                    fieldname: fieldName,
                    'asynchronousupload.handler': '${handler_name}',
                    nof: nof
                };
                formDisplayUploadedFiles${fieldname}(jsonData, '${checkBoxPrefix}');
            }
        }

        uppy.cancelAll();
    });

    uppy.on('upload-error', (file, error, response) => {
        updateErrorBox(`#i18n{asynchronousupload.error.uploadFile}`, fieldName);
        const progressContainer = document.getElementById('progress_' + fieldName);
        if (progressContainer) {
            progressContainer.style.display = 'none';
        }
    });

    uppy.on('restriction-failed', (file, error) => {
        updateErrorBox(error.message, fieldName);
    });

    uppyInstances.set(fieldName, uppy);

    inputElement.addEventListener('change', (e) => {
        const files = e.target.files;
        if (!files || files.length === 0) return;

        Array.from(files).forEach(file => {
            try {
                uppy.addFile({
                    name: file.name,
                    type: file.type,
                    data: file,
                    source: 'Local',
                    isRemote: false
                });
            } catch (err) {
                if (err.isRestriction) {
                    updateErrorBox(err.message, fieldName);
                }
            }
        });

        e.target.value = '';
    });

    setupDragAndDrop(inputElement, uppy, fieldName);

    if (nfu >= nof) {
        inputElement.disabled = true;
        inputElement.closest('.dropzone')?.classList.add('inactive');
    }

    const jsonData = {
        fieldname: fieldName,
        'asynchronousupload.handler': '${handler_name}'
    };
    formDisplayUploadedFiles${fieldname}(jsonData, '${checkBoxPrefix}');
}

/**
 * Sets up drag and drop functionality for a file input
 * @param {HTMLInputElement} inputElement - The file input element
 * @param {Uppy} uppy - The Uppy instance
 * @param {string} fieldName - The field name
 */
function setupDragAndDrop(inputElement, uppy, fieldName) {
    const dropzone = inputElement.closest('.dropzone') || inputElement.parentElement;

    ['dragenter', 'dragover'].forEach(eventName => {
        dropzone.addEventListener(eventName, (e) => {
            e.preventDefault();
            e.stopPropagation();
            dropzone.classList.add('hover');
        });
    });

    ['dragleave', 'drop'].forEach(eventName => {
        dropzone.addEventListener(eventName, (e) => {
            e.preventDefault();
            e.stopPropagation();
            dropzone.classList.remove('hover');
        });
    });

    dropzone.addEventListener('drop', (e) => {
        const files = e.dataTransfer?.files;
        if (!files || files.length === 0) return;

        Array.from(files).forEach(file => {
            try {
                uppy.addFile({
                    name: file.name,
                    type: file.type,
                    data: file,
                    source: 'Local',
                    isRemote: false
                });
            } catch (err) {
                if (err.isRestriction) {
                    updateErrorBox(err.message, fieldName);
                }
            }
        });
    });

    document.addEventListener('dragover', (e) => {
        const dropZones = document.querySelectorAll('.dropzone');
        if (window.dropZoneTimeout) {
            clearTimeout(window.dropZoneTimeout);
        } else {
            dropZones.forEach(dz => dz.classList.add('in'));
        }

        const hoveredDropZone = e.target.closest('.dropzone');
        dropZones.forEach(dz => {
            if (dz !== hoveredDropZone) dz.classList.remove('hover');
        });
        if (hoveredDropZone) hoveredDropZone.classList.add('hover');

        window.dropZoneTimeout = setTimeout(() => {
            window.dropZoneTimeout = null;
            dropZones.forEach(dz => dz.classList.remove('in', 'hover'));
        }, 100);
    });
}

document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[id^="${submitPrefix}"]').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            const input = btn.closest('.form-inline, .file-input, .dropzone, div')?.querySelector('input[type=file]');
            if (input) input.click();
        }, true);
    });
});

/**
 * Fetches and displays uploaded files list
 * @param {Object} jsonData - Request parameters
 * @param {string} cbPrefix - Checkbox prefix for file selection
 */
function formDisplayUploadedFiles${fieldname}(jsonData, cbPrefix) {
    const params = new URLSearchParams(jsonData);
    fetch('${base_url}jsp/site/plugins/asynchronousupload/DoRemoveFile.jsp?' + params)
        .then(response => response.json())
        .then(data => {
            const fieldName = data.field_name;
            const errorFileName = document.getElementById('_file_error_box_' + fieldName);
            const groupFiles = errorFileName?.closest('.group-files');
            const groupInfo = groupFiles?.querySelector('.file-input.fileinput-button');
            const jFieldName = document.getElementById(fieldName);
            const parentInput = jFieldName?.parentElement;

            parentInput?.classList.remove('is-invalid');

            if (fieldName != null) {
                if (data.fileCount == 0) {
                    if (jFieldName) {
                        jFieldName.dataset.nfu = '0';
                        jFieldName.disabled = false;
                        jFieldName.classList.remove('disabled');
                        jFieldName.closest('.dropzone')?.classList.remove('inactive');
                    }
                    const deletionLabel = document.getElementById('_file_deletion_label_' + fieldName);
                    if (deletionLabel) deletionLabel.style.display = 'none';
                } else {
                    let strContent = '';
                    const checkboxPrefix = cbPrefix + fieldName;

                    for (let index = 0; index < data.fileCount; index++) {
                        const imgContent = (data.fileCount == 1) ? data.files.preview : data.files[index].preview;
                        let imgTag = '';
                        if (typeof imgContent === 'string' && imgContent.length > 0) {
                            imgTag = '<img src="' + imgContent + '" alt="" width="${previewMaxWidth}" height="${previewMaxHeight}" class="img-fluid img-thumbnail">';
                        }
                        strContent += getTemplateUploadedFile(fieldName, index, checkboxPrefix, data, imgTag, '${handler_name}', '${base_url}', `#i18n{asynchronousupload.action.delete.name}`);
                    }

                    sessionStorage.setItem(fieldName, JSON.stringify(data.files));

                    const deletionContainer = document.getElementById('_file_deletion_' + fieldName);
                    if (deletionContainer) deletionContainer.innerHTML = strContent;

                    const deletionLabel = document.getElementById('_file_deletion_label_' + fieldName);
                    if (deletionLabel) deletionLabel.style.display = 'block';

                    if (data.fileCount >= jsonData.nof) {
                        if (jFieldName) {
                            jFieldName.dataset.nfu = jsonData.nof;
                            jFieldName.dataset.nbuploadedfiles = jsonData.nof;
                            jFieldName.disabled = true;
                            jFieldName.classList.add('disabled');
                            jFieldName.closest('.dropzone')?.classList.add('inactive');
                        }
                        groupFiles?.classList.add('no-file');

                        const errMsg = document.getElementById('msg_' + fieldName);
                        if (!errMsg && jFieldName) {
                            jFieldName.setAttribute('aria-labelledby', 'msg_' + fieldName);
                            const msgHtml = '<p id="msg_' + fieldName + '" class="group-file-info text-muted p-2 mt-1"><span class="fa fa-exclamation-circle text-warning"></span> ' + `#i18n{asynchronousupload.info.maxNumberOfFiles}` + '</p>';
                            groupInfo?.insertAdjacentHTML('afterend', msgHtml);
                        }
                    } else {
                        if (jFieldName) {
                            jFieldName.dataset.nfu = data.fileCount;
                            jFieldName.dataset.nbuploadedfiles = data.fileCount;
                            jFieldName.disabled = false;
                            jFieldName.classList.remove('disabled');
                            jFieldName.closest('.main-danger-color')?.classList.remove('main-danger-color');
                            jFieldName.removeAttribute('aria-labelledby');
                            jFieldName.closest('.dropzone')?.classList.remove('inactive');
                        }
                        document.getElementById('msg_' + fieldName)?.remove();
                    }
                }
            }
        });
}

document.addEventListener('click', (event) => {
    if (event.target.closest('.deleteSingleFile')) {
        event.preventDefault();
        deleteFile(event);
    }
});

/**
 * Deletes a single uploaded file
 * @param {Event} ev - The click event
 */
function deleteFile(ev) {
    const target = ev.target.closest('.deleteSingleFile');
    const index = target.getAttribute('index');
    const fieldName = target.getAttribute('fieldname');
    const imgPreview = document.querySelector('#' + fieldName + '_preview');
    if (imgPreview) {
        imgPreview.src = defaultImg;
    }
    const itemId = target.dataset.item;
    const item = document.querySelector(itemId);
    item?.remove();

    const handlerName = target.getAttribute('handlername');
    const inputElement = document.querySelector('.${handler_name}[name="' + fieldName + '"]');
    const nof = parseInt(inputElement?.dataset.nof) || 1;

    const jsonData = {
        fieldname: fieldName,
        'asynchronousupload.handler': handlerName,
        field_index: index,
        nof: nof
    };
    formDisplayUploadedFiles${fieldname}(jsonData, '${checkBoxPrefix}');
}

/**
 * Updates the error display box for a field
 * @param {string} errorMessage - The error message to display
 * @param {string} fieldName - The field name
 */
function updateErrorBox(errorMessage, fieldName) {
    const errorClassName = 'error_' + fieldName;
    document.querySelectorAll('.' + errorClassName).forEach(el => el.remove());

    const errorBox = document.getElementById('_file_error_box_' + fieldName);
    if (errorMessage && errorMessage !== '') {
        if (errorBox) {
            errorBox.classList.add('is-invalid');
            errorBox.insertAdjacentHTML('afterend', '<div class="invalid-feedback ' + errorClassName + '" style="display:block">' + errorMessage + '</div>');
            errorBox.style.display = 'block';
        }
    } else {
        if (errorBox) errorBox.style.display = 'none';
    }
}

/**
 * Resets error messages and invalid states for a group
 * @param {HTMLElement} group - The group element to reset
 */
function reinitMsg(group) {
    document.querySelectorAll('.group-file-info').forEach(el => el.remove());
    document.querySelectorAll('.invalid-feedback').forEach(el => el.remove());
    document.querySelectorAll('.dropzone label').forEach(el => el.classList.remove('main-danger-color'));
    group?.classList.remove('is-invalid', 'no-file');
}
