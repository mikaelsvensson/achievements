import $ from "jquery";
import {createOnFailHandler, get, isLoggedIn, putCsv} from "../util/api.jsx";
import {updateView} from "../util/view.jsx";

const templateLoading = require("../loading.handlebars");
const templateBatchUpsert = require("./batchupsert.handlebars");
const templateBatchUpsertResult = require("./batchupsert.result.handlebars");

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

        $app.find('#import-button').click(function (e) {
            const button = $(this);
            const form = button.addClass('is-loading').closest('form');
            const data = $app.find('#import-data').val();
            putCsv('/api/organizations/' + appPathParams[0].key + "/people", data, function (responseData, responseStatus, jqXHR) {
                button.removeClass('is-loading')
                const props = {
                    result: responseData,
                    orgId: appPathParams[0].key
                };

                updateView(templateBatchUpsertResult(props), $('#batchupsert-result'));
            }, createOnFailHandler(form.find('.errors'), button));
        });
    });
}


