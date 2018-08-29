import {get, isLoggedIn} from "../util/api.jsx";
import {updateView} from "../util/view.jsx";

const templateSignup = require("./organization.signup.handlebars");
const templateLoading = require("../loading.handlebars");

const API_HOST = process.env.API_HOST;

export function renderOrganizationSignup(appPathParams) {

    updateView(templateLoading());

    get('/api/organizations/' + appPathParams[0].key + "/basic", function (responseData, responseStatus, jqXHR) {
        responseData.breadcrumbs = [
            {label: "Hem", url: '#/'},
            // {label: "Kårer", url: '#karer/'},
            {label: responseData.name}
        ];
        responseData.isLoggedIn = isLoggedIn();
        responseData.id = appPathParams[0].key;
        responseData.host = API_HOST;

        updateView(templateSignup(responseData));

        /*
        $('#app').find('.organization-signup-button').click(function (e) {
            const button = $(this);
            const form = button.addClass('is-loading').closest('form');
            const formData = getFormData(form);

            const signUpDto = {
                email: formData.email,
                credentials_type: "password",
                credentials_data: formData.password
            };

            post('/api/organizations/' + appPathParams[0].key + '/signup', signUpDto, function (responseData, responseStatus, jqXHR) {
                button.removeClass('is-loading');

                setToken(responseData.token);

                navigateTo('minprofil')
            }, function (jqXHR, textStatus, errorThrown) {
                button.removeClass('is-loading');
                //TODO: Add this kind of error handling to all post, put and get calls.
                renderErrorBlock(jqXHR.responseJSON.message, form.find('.errors'));
            });
        });
         */
    });
}