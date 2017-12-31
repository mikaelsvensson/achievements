import {updateView} from "../util/view.jsx";
const templateLoginWaitForEmail = require("./login.wait-for-email.handlebars");

export function renderLoginWaitForEmail() {
    updateView(templateLoginWaitForEmail());
}