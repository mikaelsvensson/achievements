import $ from "jquery";
import {get, isLoggedIn, post, setOneTimePassword, setToken} from "../util/api.jsx";
import {getFormData, updateView} from "../util/view.jsx";
import {navigateTo} from "../util/routing.jsx";

const templateLoading = require("../loading.handlebars");
const templateMyPassword = require("./password.handlebars");

export function renderMyPassword(appPathParams) {
    updateView(templateLoading());
    get('/api/my/profile/', function (responseData, responseStatus, jqXHR) {
        responseData.isLoggedIn = isLoggedIn();
        responseData.breadcrumbs = [
            {label: "Hem", url: '#/'},
            {label: "Min profil", url: '#minprofil'},
            {label: "Lösenord"}
        ];

        updateView(templateMyPassword(responseData));

        $('#set-password-button').click(function (e) {
            const button = $(this);
            const form = button.closest('form');
            post('/api/my/password', getFormData(form), function (responseData, responseStatus, jqXHR) {
                navigateTo('minprofil')
            }, button);
        });

    });
}

export function renderPasswordForgotten(appPathParams) {
    const params = {
        is_password_credential_created: false,
        isLoggedIn: isLoggedIn(),
        breadcrumbs: [
            {label: "Hem", url: '#/'},
            {label: "Lösenord"}
        ]
    }

    updateView(templateMyPassword(params));

    $('#set-password-button').click(function (e) {
        const button = $(this);
        const form = button.closest('form');
        setOneTimePassword(appPathParams[0].key)
        post('/api/my/password', getFormData(form), function (responseData, responseStatus, jqXHR) {
            if (responseData.token) {
                setToken(responseData.token);
                navigateTo('minprofil')
            }
        }, button);
    });
}
