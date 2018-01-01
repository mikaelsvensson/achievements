import $ from "jquery";
import {get, post, isLoggedIn, createOnFailHandler} from "../util/api.jsx";
import {updateView, getFormData} from "../util/view.jsx";
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
        get('//localhost:8080/api/my/profile/', function (responseData, responseStatus, jqXHR) {
            updateView(templateOrganizationsMy(responseData), $('#organizations-my'));
        });
    }

    const $app = $('#app');

    $app.find('.create-button').click(function (e) {
        const form = $(this).addClass('is-loading').closest('form');
        post('//localhost:8080/api/organizations', getFormData(form), function (responseData, responseStatus, jqXHR) {
            navigateTo('organizations/' + responseData.id);
        }, createOnFailHandler(form.find('.errors'), button));
    });

    $app.find('.search-button').click(function (e) {
        const button = $(this);
        const form = button.addClass('is-loading').closest('form');
        const url = '//localhost:8080/api/organizations?filter=' + getFormData(form).filter;
        console.log(url);
        get(url, function (responseData, responseStatus, jqXHR) {
            button.removeClass('is-loading')
            updateView(templateOrganizationsResult({organizations: responseData}), $('#organizations-search-result'));
        });
    });
}
