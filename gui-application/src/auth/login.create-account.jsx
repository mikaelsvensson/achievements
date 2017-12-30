//TODO: Rename login.create-account to signup?
import $ from "jquery";
import {updateView} from "../util/view.jsx";
import {post, setToken} from "../util/api.jsx";
import {navigateTo} from "../util/routing.jsx";
import {initGoogleSigninButton} from "./auth.google.jsx";

const templateLoginCreateAccount = require("./login.create-account.handlebars");

function onGoogleSuccess(googleUser) {
    var id_token = googleUser.getAuthResponse().id_token;

    var profile = googleUser.getBasicProfile();
    $('#google_email').val(profile.getEmail());
    $('#google_token').val(id_token);
}
function onGoogleFailure(error) {
    // TODO: Inform user of this error
    console.log(error);
}
export function renderLoginCreateAccount() {
    updateView(templateLoginCreateAccount())

    initGoogleSigninButton(onGoogleSuccess, onGoogleFailure);

    $('#app').find('.create-account-button').click(function (e) {
        const button = $(this);
        const form = button.addClass('is-loading').closest('form');
        const googleEmail = $('#google_email').val();
        const signUpDto = {
            new_organization_name: $('#new_organization_name').val()
        };
        if (googleEmail) {
            signUpDto.email = googleEmail;
            signUpDto.identity_provider = "google"
            signUpDto.identity_provider_data = $('#google_token').val()
        } else {
            signUpDto.email = $('#email').val();
            signUpDto.identity_provider = "password"
            signUpDto.identity_provider_data = $('#password').val();
        }
        console.log("signUpDto", signUpDto);
        post('//localhost:8080/api/organizations/signup/', signUpDto, function (responseData, responseStatus, jqXHR) {
            button.removeClass('is-loading');

            setToken(responseData.token);

            navigateTo('minprofil')
        });
    });
}