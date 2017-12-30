import $ from "jquery";
import {get, post, isLoggedIn, setToken} from "../util/api.jsx";
import {updateView, getFormData} from "../util/view.jsx";
import {renderErrorBlock} from "../error-block.jsx";
import {navigateTo} from "../util/routing.jsx";
import {initGoogleSigninButton} from "../auth/auth.google.jsx";
const templateSignup = require("./organization.signup.handlebars");
const templateLoading = require("../loading.handlebars");

export function renderOrganizationSignup(appPathParams) {

    updateView(templateLoading());

    const onGoogleSuccess = function (googleUser) {
        var id_token = googleUser.getAuthResponse().id_token;

        var profile = googleUser.getBasicProfile();

        const signUpDto = {
            email: profile.getEmail(),
            credentials_type: "google",
            credentials_data: id_token
        };
        post('//localhost:8080/api/organizations/' + appPathParams[0].key + '/signup', signUpDto, function (responseData, responseStatus, jqXHR) {
            setToken(responseData.token);

            navigateTo('minprofil')
        }, function (jqXHR, textStatus, errorThrown) {
            console.log(jqXHR.responseJSON.message);
        });
    };

    const onGoogleFailure = function (error) {
        // TODO: Inform user of this error
        console.log(error);
    };

    initGoogleSigninButton(onGoogleSuccess, onGoogleFailure);

    get('//localhost:8080/api/organizations/' + appPathParams[0].key + "/basic", function (responseData, responseStatus, jqXHR) {
        responseData.breadcrumbs = [
            {label: "Hem", url: '#/'},
            {label: "Kårer", url: '#karer/'},
            {label: responseData.name}
        ];
        responseData.isLoggedIn = isLoggedIn();

        updateView(templateSignup(responseData));

        $('#app').find('.organization-signup-button').click(function (e) {
            const button = $(this);
            const form = button.addClass('is-loading').closest('form');
            const formData = getFormData(form);

            const signUpDto = {
                email: formData.email,
                credentials_type: "password",
                credentials_data: formData.password
            };

            post('//localhost:8080/api/organizations/' + appPathParams[0].key + '/signup', signUpDto, function (responseData, responseStatus, jqXHR) {
                button.removeClass('is-loading');

                setToken(responseData.token);

                navigateTo('minprofil')
            }, function (jqXHR, textStatus, errorThrown) {
                button.removeClass('is-loading');
                //TODO: Add this kind of error handling to all post, put and get calls.
                renderErrorBlock(jqXHR.responseJSON.message, form.find('.errors'));
            });
        });
    });
}