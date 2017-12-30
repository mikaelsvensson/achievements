import {updateView} from "../util/view.jsx";
import {unsetAuth} from "../util/api.jsx";
import {googleSignOut} from "./auth.google.jsx";
const templateLogout = require("./logout.handlebars");
export function renderLogout() {
    updateView(templateLogout());

    googleSignOut();

    unsetAuth(null);
}