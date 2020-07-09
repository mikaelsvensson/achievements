import $ from "jquery";
import {get, isLoggedIn, post, put, remove} from "../util/api.jsx";
import {navigateTo} from "../util/routing.jsx";
import {getFormData, updateView} from "../util/view.jsx";
import {unflatten} from 'flat';

const templatePerson = require("./person.handlebars");
const templatePersonAttributeList = require("./person.attribute-list.handlebars");
const templatePersonSummary = require("./person.summary.result.handlebars");
const templateLoading = require("../loading.handlebars");

export function renderPerson(appPathParams) {
    updateView(templateLoading());
    get('/api/organizations/' + appPathParams[0].key + '/people/' + appPathParams[1].key, function (responseData, responseStatus, jqXHR) {
        responseData.breadcrumbs = [
            {label: "Hem", url: '#/'},
            // {label: "Kårer", url: '#karer'},
            {label: responseData.organization.name, url: '#karer/' + appPathParams[0].key},
            {label: 'Personer', url: '#karer/' + appPathParams[0].key + '/personer'},
            {label: responseData.name}
        ];
        responseData.isLoggedIn = isLoggedIn();

        updateView(templatePerson(responseData));

        const getAttributeList = function (elementInForm) {
            const button = $(elementInForm);
            const form = button.closest('form');
            const payload = unflatten(getFormData(form));
            console.log(payload);
            return payload.attributes || [];
        };

        const updateAttributeList = function () {
            updateView(templatePersonAttributeList(responseData.attributes), $('#person-attributes-list'));

            $('#app').find('.attribute-remove-button').click(function (e) {
                responseData.attributes = getAttributeList(this);
                responseData.attributes.splice(this.dataset.index, 1);
                updateAttributeList();
            });

            $('#attribute-add-button').click(function (e) {
                responseData.attributes = getAttributeList(this);
                responseData.attributes.push({key: null, value: null});
                updateAttributeList();
            });
        };

        updateAttributeList();

        get('/api/organizations/' + appPathParams[0].key + '/people/' + appPathParams[1].key + "/achievement-summary", function (responseData, responseStatus, jqXHR) {
            responseData.achievements.forEach((achievement => {
                achievement.progress_detailed.sort((item1, item2) => item2.percent - item1.percent).forEach((item) => {
                    item.progress_class = item.percent == 100 ? 'is-success' : 'is-warning'
                })
            }));
            updateView(templatePersonSummary({
                achievements: responseData.achievements,
                org_id: appPathParams[0].key
            }), $('#achievements-summary'));
        });

        // TODO: Perhaps populate form using any of the solutions on https://stackoverflow.com/questions/9807426/use-jquery-to-re-populate-form-with-json-data or https://stackoverflow.com/questions/7298364/using-jquery-and-json-to-populate-forms instead?
        $.each(responseData, function (key, value) {
            $('#' + key).val(value);
        });

        $('#person-save-button').click(function (e) {
            const button = $(this);
            const form = button.closest('form');
            const payload = unflatten(getFormData(form));
            put('/api/organizations/' + appPathParams[0].key + '/people/' + appPathParams[1].key, payload, function (responseData, responseStatus, jqXHR) {
                renderPerson(appPathParams);
            }, button);
        });

        $('#person-send-welcome-mail-button').click(function (e) {
            const button = $(this);
            const form = button.closest('form');
            post('/api/organizations/' + appPathParams[0].key + '/people/' + appPathParams[1].key + '/mails/welcome', null, function (responseData, responseStatus, jqXHR) {
            }, button);
        });

        $('#person-delete-button').click(function (e) {
            if (confirm('Är du säker på att du vill ta bort ' + responseData.name + '?')) {
                const button = $(this);
                const form = button.closest('form');
                remove('/api/organizations/' + appPathParams[0].key + '/people/' + appPathParams[1].key, null, function (responseData, responseStatus, jqXHR) {
                    navigateTo('karer/' + appPathParams[0].key);
                }, button);
            }
        });

    });
}