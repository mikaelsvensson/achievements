import {get, isLoggedIn, post} from "../util/api.jsx";
import {updateView, getFormData} from "../util/view.jsx";
import $ from "jquery";

const templateLoading = require("../loading.handlebars");
const templateOrganizationPeople = require("./people.handlebars");
const templateOrganizationPeopleList = require("./organizations.people-list.handlebars");
const templateOrganizationPeopleListConfig = require("./organizations.people-list-config.handlebars");
const templateOrganizationGroupsList = require("./organizations.groups-list.handlebars");

function loadPeople (orgId, groupId) {
    get('/api/organizations/' + orgId + "/people?group=" + groupId, function (responseData, responseStatus, jqXHR) {
        updateView(templateOrganizationPeopleList({
            people: responseData.sort(function (a, b) {
                return a.name ? a.name.localeCompare(b.name) : 0;
            }),
            orgId: orgId
        }), $('#organization-people-list'));
    });
}

export function renderPeople(appPathParams) {
    updateView(templateLoading());
    get('/api/organizations/' + appPathParams[0].key, function (responseData, responseStatus, jqXHR) {
        responseData.breadcrumbs = [
            {label: "Hem", url: '#/'},
            {label: responseData.name, url: '#karer/' + responseData.id},
            {label: "Personer"}
        ];
        responseData.isLoggedIn = isLoggedIn();
        responseData.orgId = responseData.id;

        updateView(templateOrganizationPeople(responseData));

        const $app = $('#app');

        loadPeople(appPathParams[0].key, '')

        get('/api/organizations/' + appPathParams[0].key + "/groups", function (responseData, responseStatus, jqXHR) {
            const groups = responseData.sort(function (grp1, grp2) {
                return grp1.name ? grp1.name.localeCompare(grp2.name) : 0;
            });
            updateView(templateOrganizationGroupsList({
                groups: groups,
                orgId: appPathParams[0].key
            }), $('#organization-groups-list'));

            updateView(templateOrganizationPeopleListConfig({
                groups: groups
            }), $('#organization-people-list-config'));

            $('#app').find('#organization-people-list-filter-group').change(function (e) {
                const selectedGroupId = $(this).val();
                loadPeople(appPathParams[0].key, selectedGroupId)
            });
        })

        $app.find('.create-person-button').click(function (e) {
            const button = $(this);
            const form = button.closest('form');
            post('/api/organizations/' + appPathParams[0].key + '/people', getFormData(form), function (responseData, responseStatus, jqXHR) {
                get('/api/organizations/' + appPathParams[0].key + "/people", function (responseData, responseStatus, jqXHR) {
                    updateView(templateOrganizationPeopleList({
                        people: responseData,
                        orgId: appPathParams[0].key
                    }), $('#organization-people-list'));
                });
            }, button);
        });
    });
}