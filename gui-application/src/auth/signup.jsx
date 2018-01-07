import {updateView} from "../util/view.jsx";

const templateSignup = require("./signup.handlebars");

const API_HOST = process.env.API_HOST;

export function renderSignup() {
    updateView(templateSignup({host: API_HOST}))
}