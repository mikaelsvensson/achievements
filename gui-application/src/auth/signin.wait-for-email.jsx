import {updateView} from "../util/view.jsx";
const templateSigninWaitForEmail = require("./signin.wait-for-email.handlebars");

export function renderSigninWaitForEmail() {
    updateView(templateSigninWaitForEmail());
}