import {updateView} from "../util/view.jsx";
import {unsetAuth} from "../util/api.jsx";
const templateLogout = require("./logout.handlebars");
export function renderLogout() {
    updateView(templateLogout());

    //TODO: Handle issue when gapi has not yet loaded. Happens often when log-in page is reloaded.
    window.gapi.load('auth2', function () {
        unsetAuth(window.gapi.auth2);
    });
}