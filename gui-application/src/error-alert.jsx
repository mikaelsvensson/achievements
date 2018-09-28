import $ from "jquery";
import {updateView} from "./util/view.jsx";

const templateErrorAlert = require("./error-alert.handlebars");
const alertContainer = $(document.createElement('div')).appendTo(document.body)

const closeAlert = function (e) {
    $(this).closest('.modal').removeClass('is-active');
}

export function renderErrorAlert(msg, showLoginLink) {
    updateView(templateErrorAlert({
        message: msg,
        showLoginLink: showLoginLink || false
    }), alertContainer);

    alertContainer.find('#error-alert-button-close').click(closeAlert);
    alertContainer.find('#error-alert-button-ok').click(closeAlert);
    alertContainer.find('#error-alert-login-link').click(closeAlert);
}