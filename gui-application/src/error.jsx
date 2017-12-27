import {updateView} from "./util/view.jsx";
const templateError = require("./error.handlebars");
export function renderError(msg, isAuthFailure, requestOrganizationId, requestUrl, responseCode, isLoggedIn) {
    updateView(templateError({
        message: msg,
        requestUrl,
        responseCode,
        isAuthFailure,
        isLoggedIn,
        organizationId: requestOrganizationId
    }));
}