import $ from "jquery";
import {updateView, getFormData} from "../util/view.jsx";
import {post, setCredentials, setToken, setGoogleToken} from "../util/api.jsx";
import {navigateTo} from "../util/routing.jsx";
import {initGoogleSigninButton} from "./auth.google.jsx";
const templateLogin = require("./login.handlebars");

function onGoogleSuccess(googleUser) {
    var id_token = googleUser.getAuthResponse().id_token;

    setGoogleToken(id_token);

    post('//localhost:8080/api/signin', null, function (responseData, responseStatus, jqXHR) {
        setGoogleToken(null);

        setToken(responseData.token);

        navigateTo('minprofil');
    });
}

function onGoogleFailure(error) {
    // TODO: Inform user of this error
    console.log(error);
}

export function renderLogin() {
    updateView(templateLogin());

    initGoogleSigninButton(onGoogleSuccess, onGoogleFailure);

    $('#login-submit').click(function (e) {
        const button = $(this);
        const form = button.addClass('is-loading').closest('form');

        const formData = getFormData(form);

        setCredentials(formData.email, formData.password);

        post('//localhost:8080/api/signin', formData, function (responseData, responseStatus, jqXHR) {
            button.removeClass('is-loading');
            setToken(responseData.token);

            navigateTo('minprofil');
        });

    });
}