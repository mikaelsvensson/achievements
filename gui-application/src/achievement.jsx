import $ from "jquery";
import {get, post} from "./util/api.jsx";
import {updateView, getFormData} from "./util/view.jsx";
const templateAchievement = require("./achievement.handlebars");
const templateAchievementStepsList = require("./achievement.steps-list.handlebars");
const templateLoading = require("./loading.handlebars");

export function renderAchievement(appPathParams) {
    updateView(templateLoading());
    get('//localhost:8080/api/achievements/' + appPathParams[0].key, function (responseData, responseStatus, jqXHR) {
        responseData.breadcrumbs = [
            {label: "Hem", url: '#/'},
            {label: "Märken och bedrifter", url: '#achievements/'},
            {label: responseData.name}
        ];

        updateView(templateAchievement(responseData));

        $('#app').find('.create-step-button').click(function (e) {
            const button = $(this);
            const form = button.addClass('is-loading').closest('form');
            post('//localhost:8080/api/achievements/' + appPathParams[0].key + '/steps', getFormData(form), function (responseData, responseStatus, jqXHR) {
                button.removeClass('is-loading');
                get('//localhost:8080/api/achievements/' + appPathParams[0].key + "/steps", function (responseData, responseStatus, jqXHR) {
                    updateView(templateAchievementStepsList({
                        steps: responseData,
                        achievementId: appPathParams[0].key
                    }), $('#achievement-steps-list'));
                });
            });
        });

        // TODO: Perhaps populate form using any of the solutions on https://stackoverflow.com/questions/9807426/use-jquery-to-re-populate-form-with-json-data or https://stackoverflow.com/questions/7298364/using-jquery-and-json-to-populate-forms instead?
        $.each(responseData, function (key, value) {
            $('#' + key).val(value);
        });

        get('//localhost:8080/api/achievements/' + appPathParams[0].key + "/steps", function (responseData, responseStatus, jqXHR) {
            updateView(templateAchievementStepsList({
                steps: responseData,
                achievementId: appPathParams[0].key
            }), $('#achievement-steps-list'));
        });
    });
}