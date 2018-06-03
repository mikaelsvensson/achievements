import $ from "jquery";
import {createOnFailHandler, get, isLoggedIn, post} from "../util/api.jsx";
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
            const form = button.addClass('is-loading').closest('form');
            post('/api/my/password', getFormData(form), function (responseData, responseStatus, jqXHR) {
                button.removeClass('is-loading');
                navigateTo('minprofil')
            }, createOnFailHandler(form.find('.errors'), button));
        });

    });
}
