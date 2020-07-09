import $ from "jquery";
import {get, getUserOrganization, isLoggedIn, post, put} from "../util/api.jsx";
import {getFormData, updateView} from "../util/view.jsx";

const templateOrganization = require("./organization.handlebars");
const templateOrganizationSummaryList = require("./organization.summary.result.handlebars");
const templateLoading = require("../loading.handlebars");
const templateAchievementsResult = require("../achievements/achievements.result.handlebars");

//TODO: Duplicate function
function achievementSorter(a, b) {
    if (a.achievement.name < b.achievement.name) {
        return -1;
    } else if (a.achievement.name > b.achievement.name) {
        return 1;
    } else {
        return 0;
    }
}

export function renderMyOrganization() {
    return renderOrganization([{
        key: getUserOrganization()
    }])
}

export function renderOrganization(appPathParams) {
    updateView(templateLoading());
    get('/api/organizations/' + appPathParams[0].key, function (responseData, responseStatus, jqXHR) {
        responseData.breadcrumbs = [
            {label: "Hem", url: '#/'},
            // {label: "Kårer", url: '#karer/'},
            {label: responseData.name}
        ];
        responseData.isLoggedIn = isLoggedIn();
        responseData.orgId = appPathParams[0].key;
        responseData.invite_link = window.location.protocol + '//' + window.location.host + '/#karer/' + appPathParams[0].key + '/skapa-konto';

        updateView(templateOrganization(responseData));

        const $app = $('#app');

        $('#organization-save-button').click(function (e) {
            const button = $(this);
            const form = button.closest('form');
            put('/api/organizations/' + appPathParams[0].key, getFormData(form), function (responseData, responseStatus, jqXHR) {
                renderOrganization(appPathParams);
            }, button);
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

        get('/api/organizations/' + appPathParams[0].key + "/achievement-summary", function (responseData, responseStatus, jqXHR) {
            responseData.achievements.sort(achievementSorter).forEach((achievement => {
                achievement.count_category_awarded = 0
                achievement.count_category_done = 0
                achievement.count_category_started = 0
                achievement.progress_detailed.forEach((item) => {
                    item.is_category_awarded = item.awarded
                    item.is_category_done = !item.awarded && item.percent == 100
                    item.is_category_started = !item.awarded && item.percent != 100
                    achievement.count_category_awarded += item.is_category_awarded ? 1 : 0
                    achievement.count_category_done += item.is_category_done ? 1 : 0
                    achievement.count_category_started += item.is_category_started ? 1 : 0
                })
            }));
            updateView(templateOrganizationSummaryList({
                achievements: responseData.achievements,
                org_id: appPathParams[0].key
            }), $('#achievements-summary'));
        });
    });
}