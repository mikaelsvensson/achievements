import {updateView} from "../util/view.jsx";
const templateSigninFailed = require("./signin.failed.handlebars");

// CUSTOMER_SUPPORT_EMAIL fetched from environment variable by Webpack during build.
const CUSTOMER_SUPPORT_EMAIL = process.env.CUSTOMER_SUPPORT_EMAIL;

export function renderSigninFailed() {
    updateView(templateSigninFailed({
        customerSupportEmail: CUSTOMER_SUPPORT_EMAIL
    }));
}