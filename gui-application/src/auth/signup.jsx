import {updateView} from "../util/view.jsx";
const templateSignup = require("./signup.handlebars");

export function renderSignup() {
    updateView(templateSignup())
}