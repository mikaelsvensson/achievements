import {updateView} from "../util/view.jsx";
import {unsetAuth} from "../util/api.jsx";
const templateLogout = require("./logout.handlebars");
export function renderLogout() {
    updateView(templateLogout());

    unsetAuth(null);
}