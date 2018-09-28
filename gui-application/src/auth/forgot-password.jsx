import $ from "jquery";
import {getFormData, updateView} from "../util/view.jsx";
import {post} from "../util/api.jsx";

const templateForgotPassword = require("./forgot-password.handlebars");

//TODO: Rename to better match API end-point names?
export function renderForgotPassword() {
    updateView(templateForgotPassword());

    $('#forgot-password-button').click(function (e) {
        const button = $(this);
        const form = button.closest('form');

        const formData = getFormData(form);

        post('/api/my/send-set-password-link', formData, function (responseData, responseStatus, jqXHR) {
            $('#forgot-password-status').removeClass('is-hidden')
        }, button);

    });
}