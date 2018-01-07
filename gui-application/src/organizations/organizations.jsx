import $ from "jquery";
import {createOnFailHandler, get, isLoggedIn, post} from "../util/api.jsx";
import {getFormData, updateView} from "../util/view.jsx";
import {navigateTo} from "../util/routing.jsx";

const templateOrganizations = require("./organizations.handlebars");
const templateOrganizationsMy = require("./organizations.my.handlebars");
const templateOrganizationsResult = require("./organizations.result.handlebars");

export function renderOrganizations() {
    let data = {
        breadcrumbs: [
            {label: "Hem", url: '#/'},
            {label: "Kårer"}
        ],
        isLoggedIn: isLoggedIn()
    };
    updateView(templateOrganizations(data));

    if (isLoggedIn()) {
        get('/api/my/profile/', function (responseData, responseStatus, jqXHR) {
            updateView(templateOrganizationsMy(responseData), $('#organizations-my'));
        });
    }

    const $app = $('#app');

    $app.find('.create-button').click(function (e) {
        const form = $(this).addClass('is-loading').closest('form');
        post('/api/organizations', getFormData(form), function (responseData, responseStatus, jqXHR) {
            navigateTo('organizations/' + responseData.id);
        }, createOnFailHandler(form.find('.errors'), button));
    });

    $app.find('.search-button').click(function (e) {
        const button = $(this);
        const form = button.addClass('is-loading').closest('form');
        const url = '/api/organizations?filter=' + getFormData(form).filter;
        console.log(url);
        get(url, function (responseData, responseStatus, jqXHR) {
            button.removeClass('is-loading')
            updateView(templateOrganizationsResult({organizations: responseData}), $('#organizations-search-result'));
        });
    });
}
