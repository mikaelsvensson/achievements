import $ from "jquery";
import {updateView, getFormData} from "../util/view.jsx";
import {post, setCredentials, setToken, setGoogleToken} from "../util/api.jsx";
import {navigateTo} from "../util/routing.jsx";
const templateLogin = require("./login.handlebars");

function onGoogleSuccess(googleUser) {
    var id_token = googleUser.getAuthResponse().id_token;

    setGoogleToken(id_token);

    post('//localhost:8080/api/auth/token/', null, function (responseData, responseStatus, jqXHR) {
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
    updateView(templateLogin())

    //TODO: Handle issue when gapi has not yet loaded. Happens often when log-in page is reloaded.
    window.gapi.load('signin2', function () {
        window.gapi.signin2.render('googleSigninButtonContainer', {
            'scope': 'profile email',
            'longtitle': true,
            'theme': 'dark',
            'onsuccess': onGoogleSuccess,
            'onfailure': onGoogleFailure
        });
    });

    $('#login-submit').click(function (e) {
        const button = $(this);
        const form = button.addClass('is-loading').closest('form');

        const formData = getFormData(form);

        setCredentials(formData.username, formData.password);

        post('//localhost:8080/api/auth/token/', formData, function (responseData, responseStatus, jqXHR) {
            button.removeClass('is-loading');
            setCredentials(null, null);
            setToken(responseData.token);

            navigateTo('minprofil');
        });

    });
}