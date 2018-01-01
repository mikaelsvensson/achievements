import $ from "jquery";
import {updateView, getFormData} from "../util/view.jsx";
import {post, setCredentials, setToken, createOnFailHandler} from "../util/api.jsx";
import {navigateTo} from "../util/routing.jsx";
const templateSignin = require("./signin.handlebars");

export function renderSignin() {
    updateView(templateSignin());

    $('#login-submit').click(function (e) {
        const button = $(this);
        const form = button.addClass('is-loading').closest('form');

        const formData = getFormData(form);

        setCredentials(formData.email, formData.password);

        post('//localhost:8080/api/signin', formData, function (responseData, responseStatus, jqXHR) {
            button.removeClass('is-loading');
            setToken(responseData.token);

            navigateTo('minprofil');
        }, createOnFailHandler(form.find('.errors'), button));

    });
}