import $ from "jquery";
import {createOnFailHandler, get, isLoggedIn, post, put, remove} from "../util/api.jsx";
import {getFormData, updateView} from "../util/view.jsx";

const templateGroup = require("./group.handlebars");
const templateLoading = require("../loading.handlebars");
const templateSearchPeopleList = require("./group.people-list.handlebars");
const templateMembershipsList = require("./group.memberships-list.handlebars");

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

        const $app = $('#app');
        let refreshMembershipsList = function () {
            get('/api/organizations/' + appPathParams[0].key + '/groups/' + appPathParams[1].key + '/members', function (responseData, responseStatus, jqXHR) {
                const container = $('#memberships-result');
                updateView(templateMembershipsList({
                    memberships: responseData,
                    orgId: appPathParams[0].key
                }), container);

                container.find('.memberships-remove-button').click(function (e) {
                    const url = '/api/organizations/' + appPathParams[0].key + '/groups/' + appPathParams[1].key + '/members/' + this.dataset.personId;
                    remove(url, {group: null, person: null}, function (responseData, responseStatus, jqXHR) {
                        refreshMembershipsList();
                    });
                });
            });
        };

        refreshMembershipsList();

        $app.find('#memberships-search-button').click(function (e) {
            const button = $(this);
            const form = button.addClass('is-loading').closest('form');
            const url = '/api/organizations/' + appPathParams[0].key + '/people?filter=' + getFormData(form).filter;
            get(url, function (responseData, responseStatus, jqXHR) {
                button.removeClass('is-loading')
                const container = $('#memberships-search-result');
                updateView(templateSearchPeopleList({people: responseData}), container);

                container.find('.memberships-add-button').click(function (e) {
                    const url = '/api/organizations/' + appPathParams[0].key + '/groups/' + appPathParams[1].key + '/members/' + this.dataset.personId;
                    post(url, {group: null, person: null}, function (responseData, responseStatus, jqXHR) {
                        refreshMembershipsList();
                    });
                });
            });
        });


    });
}