import $ from "jquery";
import {get, post, put, isLoggedIn} from "../util/api.jsx";
import {updateView, getFormData} from "../util/view.jsx";
import {navigateTo} from "../util/routing.jsx";
const templateOrganization = require("./organization.handlebars");
const templateOrganizationPeopleList = require("./organizations.people-list.handlebars");
const templateOrganizationSummaryList = require("./organization.summary.result.handlebars");
const templateLoading = require("../loading.handlebars");
const templateAchievementsResult = require("../achievements/achievements.result.handlebars");

export function renderOrganization(appPathParams) {
    updateView(templateLoading());
    get('//localhost:8080/api/organizations/' + appPathParams[0].key, function (responseData, responseStatus, jqXHR) {
        responseData.breadcrumbs = [
            {label: "Hem", url: '#/'},
            {label: "Kårer", url: '#karer/'},
            {label: responseData.name}
        ];
        responseData.isLoggedIn = isLoggedIn();

        updateView(templateOrganization(responseData));

        const $app = $('#app');
        $app.find('.create-person-button').click(function (e) {
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

        $('#organization-save-button').click(function (e) {
            const button = $(this);
            const form = button.addClass('is-loading').closest('form');
            put('//localhost:8080/api/organizations/' + appPathParams[0].key, getFormData(form), function (responseData, responseStatus, jqXHR) {
                button.removeClass('is-loading');
                renderOrganization(appPathParams);
            });
        });

        $app.find('.search-button').click(function (e) {
            const button = $(this);
            const form = button.addClass('is-loading').closest('form');
            const url = '//localhost:8080/api/achievements?filter=' + getFormData(form).filter;
            console.log(url);
            get(url, function (responseData, responseStatus, jqXHR) {
                button.removeClass('is-loading')
                updateView(templateAchievementsResult({achievements: responseData}), $('#achievements-search-result'));
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

        get('//localhost:8080/api/organizations/' + appPathParams[0].key + "/achievement-summary", function (responseData, responseStatus, jqXHR) {
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