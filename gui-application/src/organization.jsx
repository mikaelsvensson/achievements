import $ from "jquery";
import {get, post} from "./util/api.jsx";
import {updateView, getFormData} from "./util/view.jsx";
const templateOrganization = require("./organization.handlebars");
const templateOrganizationPeopleList = require("./organizations.people-list.handlebars");
const templateLoading = require("./loading.handlebars");

export function renderOrganization(appPathParams) {
    updateView(templateLoading());
    get('//localhost:8080/api/organizations/' + appPathParams[0].key, function (responseData, responseStatus, jqXHR) {
        responseData.breadcrumbs = [
            {label: "Hem", url: '#/'},
            {label: "Organisationer", url: '#organizations/'},
            {label: responseData.name}
        ];
        updateView(templateOrganization(responseData));

        $('#app').find('.create-person-button').click(function (e) {
            const button = $(this);
            const form = button.addClass('is-loading').closest('form');
            post('//localhost:8080/api/organizations/' + appPathParams[0].key + '/people', getFormData(form), function (responseData, responseStatus, jqXHR) {
                button.removeClass('is-loading');
                get('//localhost:8080/api/organizations/' + appPathParams[0].key + "/people", function (responseData, responseStatus, jqXHR) {
                    updateView(templateOrganizationPeopleList({
                        people: responseData,
                        orgId: appPathParams[0].key
                    }), $('#organization-people-list'));
                });
            });
        });

        // TODO: Perhaps populate form using any of the solutions on https://stackoverflow.com/questions/9807426/use-jquery-to-re-populate-form-with-json-data or https://stackoverflow.com/questions/7298364/using-jquery-and-json-to-populate-forms instead?
        $.each(responseData, function (key, value) {
            $('#' + key).val(value);
        });

        get('//localhost:8080/api/organizations/' + appPathParams[0].key + "/people", function (responseData, responseStatus, jqXHR) {
            updateView(templateOrganizationPeopleList({
                people: responseData,
                orgId: appPathParams[0].key
            }), $('#organization-people-list'));
        });
    });
}