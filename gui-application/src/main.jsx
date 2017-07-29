import {updateView, getFormData} from "./util/view.jsx";
const templateMain = require("./main.handlebars");
export function renderMain() {
    updateView(templateMain())
}