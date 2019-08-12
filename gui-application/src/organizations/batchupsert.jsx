import $ from "jquery";
import {get, isLoggedIn, postFormData} from "../util/api.jsx";
import {updateView} from "../util/view.jsx";
import {initInfoAlerts, renderInfoAlert} from "../info-alert.jsx";

const templateLoading = require("../loading.handlebars");
const templateBatchUpsert = require("./batchupsert.handlebars");
const templateBatchUpsertResult = require("./batchupsert.result.handlebars");
const templateBatchUpsertPreviewResult = require("./batchupsert.preview-result.handlebars");

function getFormData(form, isDryRun) {
    const data = new FormData(form);
    if ($('#tab-textfield').is(':visible')) {
        data.delete('importUploadedFileId')
        data.delete('importFile')
    } else {
        data.delete('importRawData')
    }
    data.append('importDryRun', isDryRun ? 'true' : 'false')
    return data;
}

export function renderBatchUpsert(appPathParams) {

    get('/api/organizations/' + appPathParams[0].key + "/basic", function (responseData, responseStatus, jqXHR) {
        responseData.breadcrumbs = [
            {label: "Hem", url: '#/'},
            // {label: "Kårer", url: '#karer/'},
            {label: responseData.name, url: '#karer/' + appPathParams[0].key},
            {label: "Importera"}
        ];
        responseData.isLoggedIn = isLoggedIn();
        responseData.id = appPathParams[0].key;

        updateView(templateBatchUpsert(responseData));

        const $app = $('#app');

        $app.find('.tabs-container > .tabs a').click(function (e) {
            const tabId = e.target.dataset.tabContent;
            const containerElement = $(e.target.closest('.tabs-container'));
            containerElement.find('.tabs li').removeClass('is-active')
            e.target.parentNode.classList.add('is-active')
            containerElement.find('.tab-content').hide();
            $app.find('#' + tabId).show();
            $('#import-uploaded-file-id').val('')
        });

        initInfoAlerts($app)

        $app.find('.file-input').change(function (e) {
            const fileInput = $(e.target);
            const filePath = fileInput.val();
            if (filePath) {
                const fileName = filePath.split(/(\\|\/)/g).pop()
                fileInput.closest('label.file-label').find('> span.file-name').text(fileName)
                $('#import-uploaded-file-id').val('')
            }
        });

        $app.find('#import-preview-button').click(function (e) {
            const button = $(this);
            const form = button.closest('form');
            const data = getFormData(form[0], true);
            postFormData('/api/organizations/' + appPathParams[0].key + "/people", data, function (responseData, responseStatus, jqXHR) {
                if (responseData.uploadId) {
                    $('#import-uploaded-file-id').val(responseData.uploadId)
                }

                const props = {
                    result: responseData,
                    orgId: appPathParams[0].key
                };

                renderInfoAlert('Förhandsgransning', templateBatchUpsertPreviewResult(props), true);
            }, button);
        });

        $app.find('#import-button').click(function (e) {
            const button = $(this);
            const form = button.closest('form');
            const data = getFormData(form[0], false);
            postFormData('/api/organizations/' + appPathParams[0].key + "/people", data, function (responseData, responseStatus, jqXHR) {
                if (responseData.uploadId) {
                    $('#import-uploaded-file-id').val(responseData.uploadId)
                }

                const props = {
                    result: responseData,
                    orgId: appPathParams[0].key
                };

                renderInfoAlert('Resultat av import', templateBatchUpsertResult(props), true);
            }, button);
        });
    });
}


