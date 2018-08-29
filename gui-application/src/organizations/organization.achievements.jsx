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
                achievement.progress_detailed.sort(personSorter).forEach((item) => {
                    item.progress_class = item.percent == 100 ? 'is-success' : 'is-warning'
                    item.is_done = item.percent == 100
                })
            }));
            updateView(templateOrganizationAchievements(responseData));
        });
    });
}