import {get, isLoggedIn} from "../util/api.jsx";
import {updateView} from "../util/view.jsx";

const templateSignup = require("./organization.signup.handlebars");
const templateLoading = require("../loading.handlebars");

const API_HOST = process.env.API_HOST;

export function renderOrganizationSignup(appPathParams) {

    updateView(templateLoading());

    get('/api/organizations/' + appPathParams[0].key + "/basic", function (responseData, responseStatus, jqXHR) {
        responseData.breadcrumbs = [
            {label: "Hem", url: '#/'},
            {label: responseData.name}
        ];
        responseData.isLoggedIn = isLoggedIn();
        responseData.id = appPathParams[0].key;
        responseData.host = API_HOST;

        updateView(templateSignup(responseData));
    });
}