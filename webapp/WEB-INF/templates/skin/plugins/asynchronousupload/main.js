<@cTpl>
/**
 * Asynchronous Upload - Skin Main JS (Uppy implementation)
 * @fileoverview Handles file uploads using Uppy library for front-office
 */

/* This script may be included several times on the same page (explicit <script> in a page template
   and loader of addRequiredJsFiles in upload_commons.html). Document-level listeners must be
   bound once, otherwise one click on "delete" sends two removal requests and removes two files. */
var asyncUploadFirstLoad = !window['__asyncUploadBound_${handler_name}'];
window['__asyncUploadBound_${handler_name}'] = true;

/** @type {Map<string, Uppy>} */
var uppyInstances = window['__uppyInstances_${handler_name}'] || (window['__uppyInstances_${handler_name}'] = new Map());

/**
 * Runs a callback once the DOM is parsed. A script inserted dynamically (loader of addRequiredJsFiles)
 * may run after DOMContentLoaded has fired: the callback then runs at once.
 * @param {Function} callback - The callback
 */
function onDomReady(callback) {
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', callback, { once: true });
    } else {
        callback();
    }
}

/**
 * Runs a callback once the DOM is parsed and the Uppy library is loaded. Uppy may be inserted
 * dynamically by the loader of addRequiredJsFiles, which fires luteceAsyncUploadReady once loaded.
 * @param {Function} callback - The callback
 */
function onUppyReady(callback) {
    onDomReady(() => {
        if (window.Uppy) {
            callback();
        } else {
            window.addEventListener('luteceAsyncUploadReady', callback, { once: true });
        }
    });
}

if (asyncUploadFirstLoad) {
onUppyReady(() => {
    document.querySelectorAll('.file-input-text-noscript').forEach(el => el.style.display = 'none');
    document.querySelectorAll('.file-input-text-js').forEach(el => el.style.display = 'inline');
    document.querySelectorAll('.${handler_name}').forEach(initUppy);
});

document.addEventListener('click', (e) => {
    if (e.target.closest('.file-input-text-js')) {
        e.stopImmediatePropagation();
        const input = e.target.closest('div')?.querySelector('input[type=file]');
        input?.click();
    }
});
}

/**
 * Initializes Uppy instance for a file input element
 * @param {HTMLInputElement} inputElement - The file input element
 */
function initUppy(inputElement) {
    const fieldName = inputElement.name;
    const mfs = ${maxFileSize};

    const msgMaxFileSize = `#i18n{asynchronousupload.error.fileTooLarge}`;
    const msgMaxNumberOfFiles = `#i18n{asynchronousupload.error.maxNumberOfFiles}`;

    if (uppyInstances.has(fieldName)) {
        return;
    }

    const uppy = new Uppy.Uppy({
        id: fieldName,
        autoProceed: true,
        restrictions: {
            maxFileSize: mfs > 0 ? mfs : null
        },
        locale: {
            strings: {
                exceedsSize: msgMaxFileSize,
                youCanOnlyUploadX: msgMaxNumberOfFiles
            }
        }
    });

    // Files selected together are sent in ONE request. Sent one request per file
    // (bundle:false, 5 in parallel by default) the core DosGuardFilter rejects every upload that
    // follows another one from the same IP within lutece.upload.dosguard.minInterval (2 s), so only
    // one file out of a multiple selection survived, and silently.
    uppy.use(Uppy.XHRUpload, {
        endpoint: '${base_url}${upload_url}',
        fieldName: fieldName,
        formData: true,
        bundle: true,
        limit: 1,
        allowedMetaFields: ['fieldname', 'asynchronousupload.handler']
    });
    // bundled requests take their metadata from the global meta
    uppy.setMeta({
        fieldname: fieldName,
        'asynchronousupload.handler': '${handler_name}'
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

        const failed = result.failed ? result.failed.length : 0;
        if (result.successful.length > 0) {
            const response = result.successful[0].response?.body;
            if (response?.form_error) {
                updateErrorBox(response.form_error, fieldName);
            } else {
                const jsonData = {
                    fieldname: fieldName,
                    'asynchronousupload.handler': '${handler_name}'
                };
                const refresh = formDisplayUploadedFiles${fieldname}(jsonData, '${checkBoxPrefix}');
                // keep the failure visible: the list refresh above resets the error box
                if (failed > 0 && refresh && refresh.then) {
                    refresh.then(() => updateErrorBox(`#i18n{asynchronousupload.error.uploadFile}`, fieldName));
                }
            }
        } else if (failed > 0) {
            updateErrorBox(`#i18n{asynchronousupload.error.uploadFile}`, fieldName);
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
    inputElement.parentNode.classList.add('fileinput-button');

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
        if (inputElement.disabled) return; // maximum number of files reached
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
}

if (asyncUploadFirstLoad) {
onDomReady(() => {
    document.querySelectorAll('[id^="${submitPrefix}"]').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            const input = btn.closest('.form-inline, .file-input, div')?.querySelector('input[type=file]');
            if (input) input.click();
        }, true);
    });
});

document.addEventListener('click', (e) => {
    if (e.target.matches('[value^="${deletePrefix}"]') && !e.target.hasAttribute('nojs')) {
        e.preventDefault();
        const fieldName = e.target.value.match('${deletePrefix}(.*)')[1];
        removeFile${checkBoxPrefix}(fieldName, '${handler_name}', '${base_url}');
    }
});
}

/**
 * Fetches and displays uploaded files list
 * @param {Object} jsonData - Request parameters
 * @param {string} cbPrefix - Checkbox prefix for file selection
 */
function formDisplayUploadedFiles${fieldname}(jsonData, cbPrefix) {
    const params = new URLSearchParams(jsonData);
    return fetch('${base_url}jsp/site/plugins/asynchronousupload/DoRemoveFile.jsp?' + params)
        .then(response => response.json())
        .then(data => {
            const fieldName = data.field_name;
            updateErrorBox(data.form_error, fieldName);

            if (fieldName != null) {
                syncUploadLimit(fieldName, data.fileCount);
                if (data.fileCount == 0) {
                    const deletionContainer = document.getElementById('_file_deletion_' + fieldName);
                    if (deletionContainer) deletionContainer.innerHTML = '';

                    const deletionLabel = document.getElementById('_file_deletion_label_' + fieldName);
                    if (deletionLabel) deletionLabel.style.display = 'none';
                } else {
                    let strContent = '';
                    const checkboxPrefix = cbPrefix + fieldName;

                    for (let index = 0; index < data.fileCount; index++) {
                        const imgContent = (data.fileCount == 1) ? data.files.preview : data.files[index].preview;
                        let imgTag = '';
                        if (typeof imgContent === 'string' && imgContent.length > 0) {
                            imgTag = " <img src='" + imgContent + "' alt='' width='${previewMaxWidth}' height='${previewMaxHeight}'/>";
                        }
                        strContent += getTemplateUploadedFile(fieldName, index, checkboxPrefix, data, imgTag, '${handler_name}', `#i18n{asynchronousupload.action.delete.name}`);
                    }

                    const deletionContainer = document.getElementById('_file_deletion_' + fieldName);
                    if (deletionContainer) deletionContainer.innerHTML = strContent;

                    const deletionLabel = document.getElementById('_file_deletion_label_' + fieldName);
                    if (deletionLabel) deletionLabel.style.display = 'block';
                }
            }
        });
}

/**
 * Removes selected files via checkbox selection
 * @param {string} fieldName - The field name
 * @param {string} handlerName - The upload handler name
 * @param {string} baseUrl - The base URL
 */
function removeFile${checkBoxPrefix}(fieldName, handlerName, baseUrl) {
    let strIndexes = '';
    let indexesCount = 0;
    const checkboxPrefix = '${checkBoxPrefix}' + fieldName;

    document.querySelectorAll('[name^="' + checkboxPrefix + '"]:checked').forEach(checkbox => {
        if (checkbox.checked) {
            if (indexesCount > 0) {
                strIndexes += ',';
            }
            indexesCount++;
            const match = checkbox.name.match(checkboxPrefix + '(\\d+)');
            if (match) {
                strIndexes += match[1];
            }
        }
    });

    if (!indexesCount) {
        return;
    }

    const jsonData = {
        fieldname: fieldName,
        'asynchronousupload.handler': handlerName,
        field_index: strIndexes
    };
    formDisplayUploadedFiles${fieldname}(jsonData, '${checkBoxPrefix}');
}

if (asyncUploadFirstLoad) {
document.addEventListener('click', (event) => {
    if (event.target.closest('.deleteSingleFile')) {
        event.preventDefault();
        const target = event.target.closest('.deleteSingleFile');
        const index = target.getAttribute('index');
        const fieldName = target.getAttribute('fieldName');
        const handlerName = target.getAttribute('handlerName');
        const jsonData = {
            fieldname: fieldName,
            'asynchronousupload.handler': handlerName,
            field_index: index
        };
        formDisplayUploadedFiles${fieldname}(jsonData, '${checkBoxPrefix}');
    }
});
}

/**
 * Reflects whether the maximum number of files (data-nof) is reached for a field.
 * The server only sets the "no-file" state at render time;
 * after an asynchronous upload or removal the dropzone must be updated client side too.
 * @param {string} fieldName - The field name
 * @param {number} fileCount - Number of files currently uploaded for the field
 */
function syncUploadLimit(fieldName, fileCount) {
    const input = document.querySelector('input[type="file"][name="' + fieldName + '"]') || document.getElementById(fieldName);
    if (!input) return;
    const count = parseInt(fileCount, 10) || 0;
    const nof = parseInt(input.dataset.nof, 10);
    const limitReached = nof > 0 && count >= nof;
    input.dataset.nbuploadedfiles = count;
    input.disabled = limitReached;
    input.classList.toggle('disabled', limitReached);
    const group = input.closest('.group-files');
    if (group) group.classList.toggle('no-file', limitReached);
    syncUploadLimitMessage(input, fieldName, limitReached, nof);
    // let form validators re-evaluate the field (clears a stale "required" error once files exist)
    input.dispatchEvent(new Event('input', { bubbles: true }));
}

/**
 * Shows, under the dropzone, why it no longer accepts files once the maximum number of
 * files is reached (same message as the back office), and removes it when a file is deleted.
 * The message is a status live region filled after insertion, so screen readers announce it.
 * @param {HTMLInputElement} input - The file input element
 * @param {string} fieldName - The field name
 * @param {boolean} limitReached - Whether the maximum number of files is reached
 * @param {number} nof - The maximum number of files, inserted in the message ({0})
 */
function syncUploadLimitMessage(input, fieldName, limitReached, nof) {
    const msgId = 'msg_' + fieldName;
    let msg = document.getElementById(msgId);
    if (!limitReached) {
        msg?.remove();
        return;
    }
    if (msg) return;
    msg = document.createElement('p');
    msg.id = msgId;
    msg.className = 'group-file-info';
    msg.setAttribute('role', 'status');
    (input.closest('.file-input') || input).insertAdjacentElement('afterend', msg);
    requestAnimationFrame(() => {
        msg.innerHTML = '<span class="ti ti-info-circle" aria-hidden="true"></span> ' + `#i18n{asynchronousupload.info.maxNumberOfFiles}`.replace('{0}', nof);
    });
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
</@cTpl>
