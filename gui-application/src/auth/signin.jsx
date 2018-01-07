import $ from "jquery";
import {getFormData, updateView} from "../util/view.jsx";
import {createOnFailHandler, post, setCredentials, setToken} from "../util/api.jsx";
import {navigateTo} from "../util/routing.jsx";

const templateSignin = require("./signin.handlebars");

const API_HOST = process.env.API_HOST;

export function renderSignin() {
    updateView(templateSignin({host: API_HOST}));

    $('#login-submit').click(function (e) {
        const button = $(this);
        const form = button.addClass('is-loading').closest('form');

        const formData = getFormData(form);

        setCredentials(formData.email, formData.password);

        post('/api/signin', formData, function (responseData, responseStatus, jqXHR) {
            button.removeClass('is-loading');
            setToken(responseData.token);

            navigateTo('minprofil');
        }, createOnFailHandler(form.find('.errors'), button));

    });
}