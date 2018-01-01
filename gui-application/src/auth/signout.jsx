import {updateView} from "../util/view.jsx";
import {unsetAuth} from "../util/api.jsx";
const templateSignout = require("./signout.handlebars");
export function renderSignout() {
    updateView(templateSignout());

    unsetAuth(null);
}