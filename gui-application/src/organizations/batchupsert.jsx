import $ from "jquery";
import {createOnFailHandler, get, isLoggedIn, postFormData} from "../util/api.jsx";
import {updateView} from "../util/view.jsx";

const templateLoading = require("../loading.handlebars");
const templateBatchUpsert = require("./batchupsert.handlebars");
const templateBatchUpsertResult = require("./batchupsert.result.handlebars");
const templateBatchUpsertPreviewResult = require("./batchupsert.preview-result.handlebars");

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
            containerElement.find('.content').hide();
            $app.find('#' + tabId).show();
        });

        $app.find('#import-preview-button').click(function (e) {
            const button = $(this);
            const form = button.addClass('is-loading').closest('form');
            const data = new FormData(form[0]);
            data.append('importDryRun', 'true')
            postFormData('/api/organizations/' + appPathParams[0].key + "/people", data, function (responseData, responseStatus, jqXHR) {
                button.removeClass('is-loading')

                if (responseData.uploadId) {
                    $('#import-uploaded-file-id').val(responseData.uploadId)
                }

                const props = {
                    result: responseData,
                    orgId: appPathParams[0].key
                };

                updateView(templateBatchUpsertPreviewResult(props), $('#batchupsert-preview-result'));
            }, createOnFailHandler(form.find('.errors'), button));
        });

        $app.find('#import-button').click(function (e) {
            const button = $(this);
            const form = button.addClass('is-loading').closest('form');
            const data = new FormData(form[0]);
            postFormData('/api/organizations/' + appPathParams[0].key + "/people", data, function (responseData, responseStatus, jqXHR) {
                button.removeClass('is-loading')

                if (responseData.uploadId) {
                    $('#import-uploaded-file-id').val(responseData.uploadId)
                }

                const props = {
                    result: responseData,
                    orgId: appPathParams[0].key
                };

                updateView(templateBatchUpsertResult(props), $('#batchupsert-result'));
            }, createOnFailHandler(form.find('.errors'), button));
        });
    });
}


