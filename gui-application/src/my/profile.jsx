import $ from "jquery";
import {get, isLoggedIn} from "../util/api.jsx";
import {updateView} from "../util/view.jsx";

const templateMyProfile = require("./profile.handlebars");
const templateLoading = require("../loading.handlebars");
const templatePersonSummary = require("../organizations/person.summary.result.handlebars");

export function renderMyProfile(appPathParams) {
    updateView(templateLoading());
    get('/api/my/profile/', function (responseData, responseStatus, jqXHR) {
        responseData.breadcrumbs = [
            {label: "Hem", url: '#/'},
            {label: "Min profil"}
        ];
        responseData.isLoggedIn = isLoggedIn();

        updateView(templateMyProfile(responseData));
    });
    get('/api/my/achievement-summary', function (responseData, responseStatus, jqXHR) {
        responseData.achievements.forEach((achievement => {
            achievement.progress_detailed.sort((item1, item2) => item2.percent - item1.percent).forEach((item) => {
                item.progress_class = item.percent == 100 ? 'is-success' : 'is-warning'
            })
        }));
        updateView(templatePersonSummary({
            achievements: responseData.achievements,
            org_id: appPathParams[0].key
        }), $('#achievements-summary'));
    });
}