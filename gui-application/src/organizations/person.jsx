import $ from "jquery";
import {get, post, put, isLoggedIn} from "../util/api.jsx";
import {updateView, getFormData} from "../util/view.jsx";
const templatePerson = require("./person.handlebars");
const templateLoading = require("../loading.handlebars");

export function renderPerson(appPathParams) {
    updateView(templateLoading());
    get('//localhost:8080/api/organizations/' + appPathParams[0].key + '/people/' + appPathParams[1].key, function (responseData, responseStatus, jqXHR) {
        responseData.breadcrumbs = [
            {label: "Hem", url: '#/'},
            {label: "Organisationer", url: '#organizations'},
            {label: appPathParams[0].key, url: '#organizations/' + appPathParams[0].key},
            {label: responseData.name}
        ];
        responseData.isLoggedIn = isLoggedIn();

        updateView(templatePerson(responseData));

        // TODO: Perhaps populate form using any of the solutions on https://stackoverflow.com/questions/9807426/use-jquery-to-re-populate-form-with-json-data or https://stackoverflow.com/questions/7298364/using-jquery-and-json-to-populate-forms instead?
        $.each(responseData, function (key, value) {
            $('#' + key).val(value);
        });

        $('#person-save-button').click(function (e) {
            const button = $(this);
            const form = button.addClass('is-loading').closest('form');
            put('//localhost:8080/api/organizations/' + appPathParams[0].key + '/people/' + appPathParams[1].key, getFormData(form), function (responseData, responseStatus, jqXHR) {
                button.removeClass('is-loading');
                renderPerson(appPathParams);
            });
        });

    });
}