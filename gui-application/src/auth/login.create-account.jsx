//TODO: Rename login.create-account to signup?
import $ from "jquery";
import {updateView} from "../util/view.jsx";
import {post, setToken} from "../util/api.jsx";
import {navigateTo} from "../util/routing.jsx";
const templateLoginCreateAccount = require("./login.create-account.handlebars");

export function renderLoginCreateAccount() {
    updateView(templateLoginCreateAccount())

    $('#app').find('.create-account-button').click(function (e) {
        const button = $(this);
        const form = button.addClass('is-loading').closest('form');
        const googleEmail = $('#google_email').val();
        const signUpDto = {
            new_organization_name: $('#new_organization_name').val()
        };
        if (googleEmail) {
            signUpDto.email = googleEmail;
            signUpDto.credentials_type = "google"
            signUpDto.credentials_data = $('#google_token').val()
        } else {
            signUpDto.email = $('#email').val();
            signUpDto.credentials_type = "password"
            signUpDto.credentials_data = $('#password').val();
        }
        console.log("signUpDto", signUpDto);
        post('//localhost:8080/api/organizations/signup/', signUpDto, function (responseData, responseStatus, jqXHR) {
            button.removeClass('is-loading');

            setToken(responseData.token);

            navigateTo('minprofil')
        });
    });
}