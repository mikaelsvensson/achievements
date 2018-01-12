import $ from "jquery";
import {createOnFailHandler, get, isLoggedIn, put} from "../util/api.jsx";
import {getFormData, updateView} from "../util/view.jsx";

const templateGroup = require("./group.handlebars");
const templateLoading = require("../loading.handlebars");

export function renderGroup(appPathParams) {
    updateView(templateLoading());
    get('/api/organizations/' + appPathParams[0].key + '/groups/' + appPathParams[1].key, function (responseData, responseStatus, jqXHR) {
        responseData.breadcrumbs = [
            {label: "Hem", url: '#/'},
            {label: "Kårer", url: '#karer'},
            {label: responseData.organization.name, url: '#karer/' + appPathParams[0].key},
            {label: responseData.name}
        ];
        responseData.isLoggedIn = isLoggedIn();

        updateView(templateGroup(responseData));

        // TODO: Perhaps populate form using any of the solutions on https://stackoverflow.com/questions/9807426/use-jquery-to-re-populate-form-with-json-data or https://stackoverflow.com/questions/7298364/using-jquery-and-json-to-populate-forms instead?
        $.each(responseData, function (key, value) {
            $('#' + key).val(value);
        });

        $('#group-save-button').click(function (e) {
            const button = $(this);
            const form = button.addClass('is-loading').closest('form');
            const payload = getFormData(form);
            put('/api/organizations/' + appPathParams[0].key + '/groups/' + appPathParams[1].key, payload, function (responseData, responseStatus, jqXHR) {
                button.removeClass('is-loading');
                renderGroup(appPathParams);
            }, createOnFailHandler(form.find('.errors'), button));
        });

    });
}