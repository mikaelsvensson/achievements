import $ from "jquery";
import {updateView, getFormData} from "../util/view.jsx";
import {get, setCredentials} from "../util/api.jsx";
import {navigateTo} from "../util/routing.jsx";
const templateLogin = require("./login.handlebars");
export function renderLogin() {
    updateView(templateLogin())

    $('#login-submit').click(function (e) {
        const button = $(this);
        const form = button.addClass('is-loading').closest('form');

        const formData = getFormData(form);

        setCredentials(formData.username, formData.password);

        get('//localhost:8080/api/my/people', function (responseData, responseStatus, jqXHR) {
            button.removeClass('is-loading');
            navigateTo('statistik');
        });
    });
}