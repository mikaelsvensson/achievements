import $ from "jquery";
import {get, post, isLoggedIn, setCredentials} from "../util/api.jsx";
import {updateView, getFormData} from "../util/view.jsx";
const templateSignup = require("./organization.signup.handlebars");
const templateLoading = require("../loading.handlebars");
const templateLoginCreateAccountSuccess = require("../auth/login.create-account.success.handlebars");

export function renderOrganizationSignup(appPathParams) {
    // updateView(templateLoading());
    // get('//localhost:8080/api/organizations/' + appPathParams[0].key, function (responseData, responseStatus, jqXHR) {
    //     responseData.breadcrumbs = [
    //         {label: "Hem", url: '#/'},
    //         {label: "Organisationer", url: '#organizations/'},
    //         {label: responseData.name}
    //     ];
    //     responseData.isLoggedIn = isLoggedIn();
    //
    //     updateView(templateSignup(responseData));
    // });
        updateView(templateSignup({}));

    $('#app').find('.organization-signup-button').click(function (e) {
        const button = $(this);
        const form = button.addClass('is-loading').closest('form');
        const formData = getFormData(form);
        formData.existing_organization_id = appPathParams[0].key;
        post('//localhost:8080/api/signup/', formData, function (responseData, responseStatus, jqXHR) {
            button.removeClass('is-loading');
            setCredentials(formData.person_name, formData.user_password);

            updateView(templateLoginCreateAccountSuccess({organization: responseData.organization}), $('#login-createaccount'));
        });
    });

}