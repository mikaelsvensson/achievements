import $ from "jquery";
import {getFormData, updateView} from "../util/view.jsx";
import {createOnFailHandler, post} from "../util/api.jsx";

const templateForgotPassword = require("./forgot-password.handlebars");

//TODO: Rename to better match API end-point names?
export function renderForgotPassword() {
    updateView(templateForgotPassword());

    $('#forgot-password-button').click(function (e) {
        const button = $(this);
        const form = button.addClass('is-loading').closest('form');

        const formData = getFormData(form);

        post('/api/my/send-set-password-link', formData, function (responseData, responseStatus, jqXHR) {
            button.removeClass('is-loading');
            $('#forgot-password-status').removeClass('is-hidden')
        }, createOnFailHandler(form.find('.errors'), button));

    });
}