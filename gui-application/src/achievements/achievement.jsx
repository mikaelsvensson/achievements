import $ from "jquery";
import {get, post, isLoggedIn} from "../util/api.jsx";
import {updateView, getFormData, markdown2html} from "../util/view.jsx";
const templateAchievement = require("./achievement.handlebars");
const templateAchievementRead = require("./achievement.read.handlebars");
const templateAchievementStepsList = require("./achievement.steps-list.handlebars");
const templateLoading = require("../loading.handlebars");

export function renderAchievement(appPathParams) {
    updateView(templateLoading());
    get('//localhost:8080/api/achievements/' + appPathParams[0].key, function (achievementData, responseStatus, jqXHR) {
        let templateData = achievementData;
        templateData.breadcrumbs = [
            {label: "Hem", url: '#/'},
            {label: "Märken", url: '#marken/'},
            {label: achievementData.name}
        ];
        templateData.isLoggedIn = isLoggedIn();

        achievementData.descriptionHtml = markdown2html(achievementData.description);

        updateView(isLoggedIn() ? templateAchievement(templateData) : templateAchievementRead(templateData));

        if (isLoggedIn()) {
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
            $.each(achievementData, function (key, value) {
                $('#' + key).val(value);
            });
        }

        let showSteps = function (peopleData, steps) {
            updateView(templateAchievementStepsList({
                people: peopleData,
                steps: steps,
                achievementId: appPathParams[0].key
            }), $('#achievement-steps-list'));

            get('//localhost:8080/api/achievements/' + appPathParams[0].key + "/progress", function (progressData, responseStatus, jqXHR) {
                const keys = Object.keys(progressData);
                for (let i in keys) {
                    let key = keys[i];
                    const progress = progressData[key];
                    $("#progress-toggle-" + key + " i.mdi").addClass(progress.completed ? 'mdi-checkbox-marked' : 'mdi-checkbox-blank-outline');
                }
                $(".progress-switch").click(function () {
                    const toggleButton = $(this);
                    toggleButton.addClass('is-loading');
                    const toggleCompletedUrl = '//localhost:8080/api/achievements/' + appPathParams[0].key + '/steps/' + this.dataset.stepId + '/progress/' + this.dataset.personId;

                    const iconNode = toggleButton.find("i.mdi");

                    const completed = (!iconNode.hasClass('mdi-checkbox-blank-outline') && !iconNode.hasClass('mdi-checkbox-marked')) || iconNode.hasClass('mdi-checkbox-blank-outline');
                    post(toggleCompletedUrl, {"completed": completed}, function (responseData, responseStatus, jqXHR) {
                        toggleButton.removeClass('is-loading');
                        iconNode.removeClass(responseData.completed ? 'mdi-checkbox-blank-outline' : 'mdi-checkbox-marked');
                        iconNode.addClass(responseData.completed ? 'mdi-checkbox-marked' : 'mdi-checkbox-blank-outline');
                    });
                });
            }, function () {
                console.log("Could not load progress");
            });
        };
        if (isLoggedIn()) {
            get('//localhost:8080/api/my/people/', function (peopleData, responseStatus, jqXHR) {
                get('//localhost:8080/api/achievements/' + appPathParams[0].key + "/steps", function (responseData, responseStatus, jqXHR) {
                    showSteps(peopleData, responseData);
                });
            }, function () {
                console.log("Could not load my people");
                get('//localhost:8080/api/achievements/' + appPathParams[0].key + "/steps", function (responseData, responseStatus, jqXHR) {
                    showSteps(null, responseData);
                });
            });
        } else {
            get('//localhost:8080/api/achievements/' + appPathParams[0].key + "/steps", function (responseData, responseStatus, jqXHR) {
                showSteps(null, responseData);
            });
        }
    });
}