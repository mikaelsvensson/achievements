import {get, isLoggedIn} from "../util/api.jsx";
import {updateView} from "../util/view.jsx";

const templateOrganizationAchievements = require("./organization.achievements.handlebars");
const templateOrganizationSummaryList = require("./organization.summary.result.handlebars");
const templateLoading = require("../loading.handlebars");

function achievementSorter(a, b) {
    if (a.achievement.name < b.achievement.name) {
        return -1;
    } else if (a.achievement.name > b.achievement.name) {
        return 1;
    } else {
        return 0;
    }
}

function personSorter(a, b) {
    if (a.person.name < b.person.name) {
        return -1;
    } else if (a.person.name > b.person.name) {
        return 1;
    } else {
        return 0;
    }
}

export function renderOrganizationAchievements(appPathParams) {
    updateView(templateLoading());

    get('/api/organizations/' + appPathParams[0].key, function (orgResponseData, responseStatus, jqXHR) {

        get('/api/organizations/' + appPathParams[0].key + "/achievement-summary", function (responseData, responseStatus, jqXHR) {

            responseData.breadcrumbs = [
                {label: "Hem", url: '#/'},
                // {label: "Kårer", url: '#karer'},
                {label: orgResponseData.name, url: '#karer/' + appPathParams[0].key},
                {label: "Märken"}
            ];
            responseData.isLoggedIn = isLoggedIn();
            responseData.orgId = appPathParams[0].key;
            responseData.orgName = orgResponseData.name;
            responseData.achievements.sort(achievementSorter).forEach((achievement => {
                achievement.count_category_awarded = 0
                achievement.count_category_done = 0
                achievement.count_category_started = 0
                achievement.progress_detailed.sort(personSorter).forEach((item) => {
                    item.is_category_awarded = item.awarded
                    item.is_category_done = !item.awarded && item.percent == 100
                    item.is_category_started = !item.awarded && item.percent != 100
                    achievement.count_category_awarded += item.is_category_awarded ? 1 : 0
                    achievement.count_category_done += item.is_category_done ? 1 : 0
                    achievement.count_category_started += item.is_category_started ? 1 : 0
                })
            }));
            updateView(templateOrganizationAchievements(responseData));
        });
    });
}