import {updateView} from "../util/view.jsx";
const templateLoginFailed = require("./login.failed.handlebars");

// CUSTOMER_SUPPORT_EMAIL fetched from environment variable by Webpack during build.
const CUSTOMER_SUPPORT_EMAIL = process.env.CUSTOMER_SUPPORT_EMAIL;

export function renderLoginFailed() {
    updateView(templateLoginFailed({
        customerSupportEmail: CUSTOMER_SUPPORT_EMAIL
    }));
}