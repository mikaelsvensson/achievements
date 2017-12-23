import {get, isLoggedIn} from "../util/api.jsx";
import {updateView} from "../util/view.jsx";
const templateMyProfile = require("./profile.handlebars");
const templateLoading = require("../loading.handlebars");

export function renderMyProfile(appPathParams) {
    updateView(templateLoading());
    get('//localhost:8080/api/my/profile/', function (responseData, responseStatus, jqXHR) {
        responseData.breadcrumbs = [
            {label: "Hem", url: '#/'},
            {label: "Min profil"}
        ];
        responseData.isLoggedIn = isLoggedIn();

        updateView(templateMyProfile(responseData));
    });
}