import $ from "jquery";
import {updateView, getFormData} from "./util/view.jsx";
import {post, setCredentials} from "./util/api.jsx";
const templateLoginCreateAccount = require("./login.create-account.handlebars");
const templateLoginCreateAccountSuccess = require("./login.create-account.success.handlebars");
export function renderLoginCreateAccount() {
    updateView(templateLoginCreateAccount())

    $('#app').find('.create-account-button').click(function (e) {
        const button = $(this);
        const form = button.addClass('is-loading').closest('form');
        post('//localhost:8080/api/signup/', getFormData(form), function (responseData, responseStatus, jqXHR) {
            button.removeClass('is-loading');
            setCredentials(form.username, form.password);

            updateView(templateLoginCreateAccountSuccess({organization: responseData.organization}), $('#login-createaccount'));
        });
    });
}