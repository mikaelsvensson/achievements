import {updateView} from "./util/view.jsx";
const templateError = require("./error.handlebars");
export function renderError(msg, showLoginLink) {
    updateView(templateError({message: msg, showLoginLink}));
}