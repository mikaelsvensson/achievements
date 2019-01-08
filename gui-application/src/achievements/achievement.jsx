import $ from "jquery";
import {get, getUserOrganization, isLoggedIn, post, remove} from "../util/api.jsx";
import {getFormData, markdown2html, updateView} from "../util/view.jsx";
import ColorHash from "color-hash";
import {toRelativeTime} from '../util/time.jsx'

const templateAchievement = require("./achievement.handlebars");
const templateAchievementRead = require("./achievement.read.handlebars");
const templateAchievementProgressHistory = require("./achievement.progress-history.handlebars");
const templateAchievementStepsList = require("./achievement.steps-list.handlebars");
const templateAchievementStepsConfig = require("./achievement.steps-config.handlebars");
const templateLoading = require("../loading.handlebars");

export function renderAchievement(appPathParams) {
    updateView(templateLoading());
    get('/api/achievements/' + appPathParams[0].key, function (achievementData, responseStatus, jqXHR) {
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
                const form = button.closest('form');
                post('/api/achievements/' + appPathParams[0].key + '/steps', getFormData(form), function (responseData, responseStatus, jqXHR) {
                    get('/api/achievements/' + appPathParams[0].key + "/steps", function (responseData, responseStatus, jqXHR) {
                        updateView(templateAchievementStepsList({
                            steps: responseData,
                            achievementId: appPathParams[0].key
                        }), $('#achievement-steps-list'));
                    });
                }, button);
            });

            $('#achievement-progress-history-button').click(function (e) {
                const button = $(this);
                get('/api/achievements/' + appPathParams[0].key + '/progress-history', function (responseData, responseStatus, jqXHR) {
                    const colorizer = new ColorHash({lightness: 0.9});
                    responseData.map(record => {
                        const secondsAgo = (new Date().getTime() - new Date(record.date_time).getTime()) / 1000;
                        record.date_time_relative = toRelativeTime(secondsAgo)
                        record.user.color = colorizer.hex(record.user.name)
                        record.person.color = colorizer.hex(record.person.name)
                        // TODO: is_completed is more reliable than completed (which is returned by the API)
                        record.data.is_completed = record.data.value === 100
                        record.data.is_started = record.data.value === 50
                        record.data.is_not_started = record.data.value === 0
                    });
                    console.log(responseData);
                    updateView(templateAchievementProgressHistory(responseData), $('#achievement-progress-history'));
                }, button);
            });

            // TODO: Perhaps populate form using any of the solutions on https://stackoverflow.com/questions/9807426/use-jquery-to-re-populate-form-with-json-data or https://stackoverflow.com/questions/7298364/using-jquery-and-json-to-populate-forms instead?
            $.each(achievementData, function (key, value) {
                $('#' + key).val(value);
            });
        }

        let showSteps = function (peopleData, steps) {
            updateView(templateAchievementStepsList({
                people: peopleData && peopleData.sort((p1, p2) => p1.name.toLowerCase() > p2.name.toLowerCase()) || [],
                steps,
                achievementId: appPathParams[0].key
            }), $('#achievement-steps-list'));

            if (isLoggedIn()) {
                get('/api/achievements/' + appPathParams[0].key + "/progress", function (progressData, responseStatus, jqXHR) {
                    const keys = Object.keys(progressData);
                    for (let i in keys) {
                        let key = keys[i];
                        const progress = progressData[key];
                        const iconNode = $("#progress-toggle-" + key + " i.mdi");
                        if (0 <= progress.value && progress.value < 50) {
                            iconNode.addClass('mdi-circle-outline');
                        } else if (50 <= progress.value && progress.value < 100) {
                            iconNode.addClass('mdi-circle-slice-4');
                        } else {
                            iconNode.addClass('mdi-circle-slice-8');
                        }
                    }
                    $(".progress-switch").click(function () {
                        const toggleButton = $(this);
                        const progressURL = '/api/achievements/' + appPathParams[0].key + '/steps/' + this.dataset.stepId + '/progress/' + this.dataset.personId;

                        // TODO: Save progress value in node dataSet instead of relying on the presens of a CSS class
                        const enablePartiallyCompleted = $('#steps-config-enable-partially-completed').find("i.mdi").hasClass('mdi-checkbox-marked');

                        // TODO: Save progress value in node dataSet instead of relying on the presens of a CSS class
                        const iconNode = toggleButton.find("i.mdi");

                        let nextProgress = 0;
                        let nextClass = 'mdi-circle-outline';
                        if (iconNode.hasClass('mdi-circle-slice-8')) {
                            nextProgress = 0;
                            nextClass = 'mdi-circle-outline';
                        } else if (iconNode.hasClass('mdi-circle-slice-4')) {
                            nextProgress = 100;
                            nextClass = 'mdi-circle-slice-8';
                        } else {
                            nextProgress = enablePartiallyCompleted ? 50 : 100;
                            nextClass = enablePartiallyCompleted ? 'mdi-circle-slice-4' : 'mdi-circle-slice-8';
                        }

                        post(progressURL, {"value": nextProgress}, function (responseData, responseStatus, jqXHR) {
                            iconNode.removeClass('mdi-circle-outline mdi-circle-slice-4 mdi-circle-slice-8');
                            iconNode.addClass(nextClass);
                        }, toggleButton);
                    });
                });
                get('/api/achievements/' + appPathParams[0].key + "/awards", function (awardedToList, responseStatus, jqXHR) {
                    for (let i in awardedToList) {
                        let personId = awardedToList[i].id;
                        $("#awarded-toggle_" + personId + " i.mdi")
                            .addClass('mdi-check-decagram')
                            .removeClass('mdi-decagram-outline');
                    }
                    $(".awarded-switch").click(function () {
                        const toggleButton = $(this);
                        const awardsURL = '/api/achievements/' + appPathParams[0].key + '/awards/' + this.dataset.personId;

                        // TODO: Save progress value in node dataSet instead of relying on the presens of a CSS class
                        const iconNode = toggleButton.find("i.mdi");

                        let nextIsAwarded = iconNode.hasClass('mdi-decagram-outline');
                        let nextClass = nextIsAwarded ? 'mdi-check-decagram' : 'mdi-decagram-outline';

                        if (nextIsAwarded) {
                            post(awardsURL, null, function (responseData, responseStatus, jqXHR) {
                                iconNode.removeClass('mdi-check-decagram mdi-decagram-outline');
                                iconNode.addClass(nextClass);
                            }, toggleButton);
                        } else {
                            remove(awardsURL, null, function (responseData, responseStatus, jqXHR) {
                                iconNode.removeClass('mdi-check-decagram mdi-decagram-outline');
                                iconNode.addClass(nextClass);
                            }, toggleButton);
                        }
                    });
                });
            }
        };

        get('/api/achievements/' + appPathParams[0].key + "/steps", function (steps, responseStatus, jqXHR) {
            showSteps(null, steps);

            if (isLoggedIn()) {
                get('/api/my/groups/', function (myGroups, responseStatus, jqXHR) {

                    updateView(templateAchievementStepsConfig({
                        groups: myGroups,
                        achievementId: appPathParams[0].key
                    }), $('#achievement-steps-config'));

                    $('#steps-config-enable-partially-completed').click(function (e) {
                        $(this)
                            .find("i.mdi")
                            .toggleClass('mdi-checkbox-blank-outline')
                            .toggleClass('mdi-checkbox-marked')
                        return false;
                    });

                    $('#app').find('#people-filter-group').change(function (e) {
                        const selectedGroupId = $(this).val();
                        if (selectedGroupId > 0) {
                            get('/api/organizations/' + getUserOrganization() + '/groups/' + selectedGroupId + '/members', function (memberships, responseStatus, jqXHR) {
                                showSteps(memberships.map(m => m.person), steps)
                            });
                        } else {
                            get('/api/my/people/', function (peopleData, responseStatus, jqXHR) {
                                showSteps(peopleData, steps)
                            });
                        }
                    });
                });
            }
        });
    });
}