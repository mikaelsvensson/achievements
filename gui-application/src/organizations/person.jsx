import $ from "jquery";
import {get, put, isLoggedIn, createOnFailHandler} from "../util/api.jsx";
import {updateView, getFormData} from "../util/view.jsx";
const templatePerson = require("./person.handlebars");
const templatePersonSummary = require("./person.summary.result.handlebars");
const templateLoading = require("../loading.handlebars");

export function renderPerson(appPathParams) {
    updateView(templateLoading());
    get('//localhost:8080/api/organizations/' + appPathParams[0].key + '/people/' + appPathParams[1].key, function (responseData, responseStatus, jqXHR) {
        responseData.breadcrumbs = [
            {label: "Hem", url: '#/'},
            {label: "Kårer", url: '#karer'},
            {label: responseData.organization.name, url: '#karer/' + appPathParams[0].key},
            {label: responseData.name}
        ];
        responseData.isLoggedIn = isLoggedIn();

        updateView(templatePerson(responseData));

        get('//localhost:8080/api/organizations/' + appPathParams[0].key + '/people/' + appPathParams[1].key + "/achievement-summary", function (responseData, responseStatus, jqXHR) {
            responseData.achievements.forEach((achievement => {
                achievement.progress_detailed.sort((item1, item2) => item2.percent - item1.percent).forEach((item) => {
                    item.progress_class = item.percent == 100 ? 'is-success' : 'is-warning'
                })
            }));
            updateView(templatePersonSummary({
                achievements: responseData.achievements,
                org_id: appPathParams[0].key
            }), $('#achievements-summary'));
        });

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
            }, createOnFailHandler(form.find('.errors'), button));
        });

    });
}