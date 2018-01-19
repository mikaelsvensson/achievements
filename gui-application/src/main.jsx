import {updateView} from "./util/view.jsx";
import {isLoggedIn} from "./util/api.jsx";

const templateMain = require("./main.handlebars");

const API_HOST = process.env.API_HOST;

export function renderMain() {
    updateView(templateMain({
        host: API_HOST,
        isLoggedIn: isLoggedIn()
    }))
}