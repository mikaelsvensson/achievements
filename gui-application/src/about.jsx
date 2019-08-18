import $ from "jquery";
import {updateView} from "./util/view.jsx";
import {initContactUsLinks} from "./util/mail.jsx";

const templateAbout = require("./about.handlebars");

export function renderAbout() {
    updateView(templateAbout());

    const $app = $('#app');
    initContactUsLinks($app);
}