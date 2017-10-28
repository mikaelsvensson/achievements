import {updateView} from "./util/view.jsx";
const templateErrorBlock = require("./error-block.handlebars");
export function renderErrorBlock(msg, container) {
    updateView(templateErrorBlock({message: msg}), container);
}