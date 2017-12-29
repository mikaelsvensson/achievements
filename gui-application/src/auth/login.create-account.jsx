import $ from "jquery";
import {updateView, getFormData} from "../util/view.jsx";
import {post, setCredentials} from "../util/api.jsx";
const templateLoginCreateAccount = require("./login.create-account.handlebars");
const templateLoginCreateAccountSuccess = require("./login.create-account.success.handlebars");
export function renderLoginCreateAccount() {
    updateView(templateLoginCreateAccount())

    $('#app').find('.create-account-button').click(function (e) {
        const button = $(this);
        const form = button.addClass('is-loading').closest('form');
        const formData = getFormData(form);
        //TODO: Signup API does no longer work like this...
        post('//localhost:8080/api/signup/', formData, function (responseData, responseStatus, jqXHR) {
            button.removeClass('is-loading');
            setCredentials(formData.person_name, formData.user_password);

            updateView(templateLoginCreateAccountSuccess({organization: responseData.organization}), $('#login-createaccount'));
        });
    });
}