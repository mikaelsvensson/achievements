import $ from "jquery";
import {createOnFailHandler, get, isLoggedIn, post, put} from "../util/api.jsx";
import {getFormData, updateView} from "../util/view.jsx";

const templateOrganization = require("./organization.handlebars");
const templateOrganizationPeopleList = require("./organizations.people-list.handlebars");
const templateOrganizationGroupsList = require("./organizations.groups-list.handlebars");
const templateOrganizationSummaryList = require("./organization.summary.result.handlebars");
const templateLoading = require("../loading.handlebars");
const templateAchievementsResult = require("../achievements/achievements.result.handlebars");

export function renderOrganization(appPathParams) {
    updateView(templateLoading());
    get('/api/organizations/' + appPathParams[0].key, function (responseData, responseStatus, jqXHR) {
        responseData.breadcrumbs = [
            {label: "Hem", url: '#/'},
            {label: "Kårer", url: '#karer/'},
            {label: responseData.name}
        ];
        responseData.isLoggedIn = isLoggedIn();
        responseData.orgId = appPathParams[0].key;
        responseData.invite_link = window.location.protocol + '//' + window.location.host + '/#karer/' + appPathParams[0].key + '/skapa-konto';

        updateView(templateOrganization(responseData));

        const $app = $('#app');
        $app.find('.create-person-button').click(function (e) {
            const button = $(this);
            const form = button.addClass('is-loading').closest('form');
            post('/api/organizations/' + appPathParams[0].key + '/people', getFormData(form), function (responseData, responseStatus, jqXHR) {
                button.removeClass('is-loading');
                get('/api/organizations/' + appPathParams[0].key + "/people", function (responseData, responseStatus, jqXHR) {
                    updateView(templateOrganizationPeopleList({
                        people: responseData,
                        orgId: appPathParams[0].key
                    }), $('#organization-people-list'));
                });
            }, createOnFailHandler(form.find('.errors'), button));
        });

        $app.find('.create-group-button').click(function (e) {
            const button = $(this);
            const form = button.addClass('is-loading').closest('form');
            post('/api/organizations/' + appPathParams[0].key + '/groups', getFormData(form), function (responseData, responseStatus, jqXHR) {
                button.removeClass('is-loading');
                get('/api/organizations/' + appPathParams[0].key + "/groups", function (responseData, responseStatus, jqXHR) {
                    updateView(templateOrganizationPeopleList({
                        groups: responseData,
                        orgId: appPathParams[0].key
                    }), $('#organization-groups-list'));
                });
            }, createOnFailHandler(form.find('.errors'), button));
        });

        $('#organization-save-button').click(function (e) {
            const button = $(this);
            const form = button.addClass('is-loading').closest('form');
            put('/api/organizations/' + appPathParams[0].key, getFormData(form), function (responseData, responseStatus, jqXHR) {
                button.removeClass('is-loading');
                renderOrganization(appPathParams);
            }, createOnFailHandler(form.find('.errors'), button));
        });

        /*
                $app.find('.search-button').click(function (e) {
                    const button = $(this);
                    const form = button.addClass('is-loading').closest('form');
                    const url = '/api/achievements?filter=' + getFormData(form).filter;
                    console.log(url);
                    get(url, function (responseData, responseStatus, jqXHR) {
                        button.removeClass('is-loading')
                        updateView(templateAchievementsResult({achievements: responseData}), $('#achievements-search-result'));
                    });
                });
        */

        // TODO: Perhaps populate form using any of the solutions on https://stackoverflow.com/questions/9807426/use-jquery-to-re-populate-form-with-json-data or https://stackoverflow.com/questions/7298364/using-jquery-and-json-to-populate-forms instead?
        $.each(responseData, function (key, value) {
            $('#' + key).val(value);
        });

        get('/api/organizations/' + appPathParams[0].key + "/people", function (responseData, responseStatus, jqXHR) {
            updateView(templateOrganizationPeopleList({
                people: responseData.sort(function (a, b) {
                    return a.name ? a.name.localeCompare(b.name) : 0;
                }),
                orgId: appPathParams[0].key
            }), $('#organization-people-list'));
        });

        get('/api/organizations/' + appPathParams[0].key + "/groups", function (responseData, responseStatus, jqXHR) {
            updateView(templateOrganizationGroupsList({
                groups: responseData.sort(function (grp1, grp2) {
                    return grp1.name ? grp1.name.localeCompare(grp2.name) : 0;
                }),
                orgId: appPathParams[0].key
            }), $('#organization-groups-list'));
        });

        get('/api/organizations/' + appPathParams[0].key + "/achievement-summary", function (responseData, responseStatus, jqXHR) {
            responseData.achievements.forEach((achievement => {
                achievement.progress_detailed.sort((item1, item2) => item2.percent - item1.percent).forEach((item) => {
                    item.progress_class = item.percent == 100 ? 'is-success' : 'is-warning'
                })
            }));
            updateView(templateOrganizationSummaryList({
                achievements: responseData.achievements,
                org_id: appPathParams[0].key
            }), $('#achievements-summary'));

            $('.modal-achievement-summary-details-button').click(function (e) {
                const $dialog = $(document.getElementById(this.dataset.elementRefId));
                $dialog.addClass('is-active');
                $dialog.find('div.modal-background').click(function (e) {
                    $(this).parent().removeClass('is-active');
                });
                $dialog.find('button.modal-close').click(function (e) {
                    $(this).parent().removeClass('is-active');
                });
            });
        });
    });
}