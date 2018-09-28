import $ from "jquery";
import {updateView} from "./util/view.jsx";

const templateAbout = require("./about.handlebars");

export function renderAbout() {
    updateView(templateAbout());

    const domain = 'gmail.com';
    const user = 'minamarken';

    // TODO: Get address from process.env.CUSTOMER_SUPPORT_EMAIL instead
    $('.mail-link').attr('href', ['mailto:', user, '@', domain].join('')).text([user, '@', domain].join(''));
}