import {updateView} from "./util/view.jsx";
import {setCredentials} from "./util/api.jsx";
const templateLogout = require("./logout.handlebars");
export function renderLogout() {
    updateView(templateLogout());

    setCredentials("", "");
}