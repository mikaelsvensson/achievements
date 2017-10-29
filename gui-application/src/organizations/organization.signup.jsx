import $ from "jquery";
import {get, post, isLoggedIn, setCredentials} from "../util/api.jsx";
import {updateView, getFormData} from "../util/view.jsx";
import {renderErrorBlock} from "../error-block.jsx";
const templateSignup = require("./organization.signup.handlebars");
const templateLoading = require("../loading.handlebars");
const templateLoginCreateAccountSuccess = require("../auth/login.create-account.success.handlebars");

export function renderOrganizationSignup(appPathParams) {
    updateView(templateLoading());
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
            post('//localhost:8080/api/signup/' + appPathParams[0].key, formData, function (responseData, responseStatus, jqXHR) {
                button.removeClass('is-loading');
                setCredentials(formData.email, formData.password);

                updateView(templateLoginCreateAccountSuccess({organization: responseData.organization}), $('#login-createaccount'));
            }, function (jqXHR, textStatus, errorThrown) {
                button.removeClass('is-loading');
                renderErrorBlock(jqXHR.responseJSON.message, form.find('.errors'));
            });
        });
    });


}