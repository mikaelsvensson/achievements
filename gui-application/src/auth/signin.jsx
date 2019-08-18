import $ from "jquery";
import {getFormData, updateView} from "../util/view.jsx";
import {post, setCredentials, setToken} from "../util/api.jsx";
import {navigateTo} from "../util/routing.jsx";
import {initContactUsLinks} from "../util/mail.jsx";

const templateSignin = require("./signin.handlebars");

const API_HOST = process.env.API_HOST;

export function renderSignin() {
    updateView(templateSignin({host: API_HOST}));

    const $app = $('#app');
    initContactUsLinks($app);
    $('#login-submit').click(function (e) {
        const button = $(this);
        const form = button.closest('form');

        const formData = getFormData(form);

        setCredentials(formData.email, formData.password);

        post('/api/signin', formData, function (responseData, responseStatus, jqXHR) {
            setToken(responseData.token);

            navigateTo('minprofil');
        }, button);

    });
}