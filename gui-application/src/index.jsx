import $ from "jquery";
import styles from "bulma/css/bulma.css";
const templateMain = require("./main.handlebars");
const templateOrganizations = require("./organizations.handlebars");
const templateOrganizationsResult = require("./organizations.result.handlebars");
const templateOrganization = require("./organization.handlebars");
const templateStats = require("./stats.handlebars");
const templateLoading = require("./loading.handlebars");

$(function () {
    $(window).on('hashchange', function () {
        // On every hash change the render function is called with the new hash.
        // This is how the navigation of our app happens.

        const appPath = decodeURI(window.location.hash.substr(1));

        render(appPath);
    });

    const s = styles;

    const render = function (appPath) {
        console.log('Path has changed to this: ' + appPath);
        const appPathParams = {};
        const parts = appPath.split(/\//);
        for (let i = 0; i < parts.length; i += 2) {
            appPathParams[parts[i]] = parts[i + 1] || "";
        }
        console.log(appPathParams);
        if (appPathParams.stats === '') {
            updateView(templateLoading());
            $.getJSON("//localhost:8080/api/stats", function (data) {
                data.breadcrumbs = [
                    {label: "Hem", url: '#/'},
                    {label: "Fakta"}
                ];
                updateView(templateStats(data));
            });
        } else if (appPathParams.organizations === "") {
            let data = {
                breadcrumbs: [
                    {label: "Hem", url: '#/'},
                    {label: "Organisationer"}
                ]
            };
            updateView(templateOrganizations(data));
            $('#app .create-button').click(function (e) {
                const form = $(this).addClass('is-loading').closest('form');
                post('//localhost:8080/api/organizations', getFormData(form), function (responseData, responseStatus, jqXHR) {
                    window.location.hash = '#organizations/' + responseData.id;
                });
            });
            $('#app .search-button').click(function (e) {
                const button = $(this);
                const form = button.addClass('is-loading').closest('form');
                const url = '//localhost:8080/api/organizations?filter=' + getFormData(form).filter;
                console.log(url);
                get(url, function (responseData, responseStatus, jqXHR) {
                    button.removeClass('is-loading')
                    console.log("Got back", responseData);
                    updateView(templateOrganizationsResult({organizations: responseData}), $('#organizations-search-result'));
                });
            });
        } else if (typeof appPathParams.organizations !== "undefined") {
            updateView(templateLoading());
            get('//localhost:8080/api/organizations/' + appPathParams.organizations, function (responseData, responseStatus, jqXHR) {
                responseData.breadcrumbs = [
                    {label: "Hem", url: '#/'},
                    {label: "Organisationer", url: '#organizations/'},
                    {label: responseData.name}
                ];
                updateView(templateOrganization(responseData));

                // TODO: Perhaps populate form using any of the solutions on https://stackoverflow.com/questions/9807426/use-jquery-to-re-populate-form-with-json-data or https://stackoverflow.com/questions/7298364/using-jquery-and-json-to-populate-forms instead?
                $.each(responseData, function (key, value) {
                    $('#' + key).val(value);
                });

            });
        } else {
            const data = {
                msg: "Hello"
            };
            updateView(templateMain(data))
        }
    };

    const getFormData = function ($form) {
        var unindexed_array = $form.serializeArray();
        var indexed_array = {};

        $.map(unindexed_array, function (n, i) {
            indexed_array[n['name']] = n['value'];
        });

        return indexed_array;
    };

    const post = function (url, dataObject, onSuccess) {
        $.ajax({
            url: url,
            type: "POST",
            data: JSON.stringify(dataObject),
            dataType: "json",
            contentType: "application/json; charset=utf-8",
            success: onSuccess
        });
    };
    const get = function (url, onSuccess) {
        $.ajax({
            url: url,
            type: "GET",
            dataType: "json",
            contentType: "application/json; charset=utf-8",
            success: onSuccess
        });
    };

    const updateView = function (contentNodes, container) {
        const node = container || $('#app');
        node.empty().append(contentNodes);
    };

    $(window).trigger('hashchange');
});