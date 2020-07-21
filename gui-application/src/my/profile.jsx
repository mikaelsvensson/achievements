import $ from "jquery";
import {get, isLoggedIn} from "../util/api.jsx";
import {updateView} from "../util/view.jsx";

const templateMyProfile = require("./profile.handlebars");
const templateLoading = require("../loading.handlebars");
const templatePersonSummary = require("../organizations/person.summary.result.handlebars");

export function renderMyProfile(appPathParams) {
    updateView(templateLoading());
    get('/api/my/profile/', function (responseData, responseStatus, jqXHR) {
        responseData.isLoggedIn = isLoggedIn();
        responseData.isAdmin = responseData.person.role == 'admin';
        responseData.gettingStarted = {
            showYouAreAlone: responseData.getting_started.is_only_person_in_organization,
            showSetPassword: responseData.getting_started.is_password_credential_created && !responseData.getting_started.is_password_set
        }
        responseData.showGettingStarted = Object.keys(responseData.gettingStarted).some(function (prop) {
            return responseData.gettingStarted[prop] === true
        })

        updateView(templateMyProfile(responseData));

        const orgId = responseData.organization.id;

        get('/api/my/achievement-summary', function (responseData, responseStatus, jqXHR) {
            responseData.achievements.forEach((achievement => {
                achievement.progress_detailed.sort((item1, item2) => item2.percent - item1.percent).forEach((item) => {
                    item.progress_class = item.percent == 100 ? 'is-success' : 'is-warning'
                })
            }));
            updateView(templatePersonSummary({
                achievements: responseData.achievements && responseData.achievements.length > 0 ? responseData.achievements : null,
                org_id: orgId
            }), $('#achievements-summary'));
        });
    });
}