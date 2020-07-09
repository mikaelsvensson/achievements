import {get, isLoggedIn, post} from "../util/api.jsx";
import {updateView, getFormData} from "../util/view.jsx";
import $ from "jquery";

const templateLoading = require("../loading.handlebars");
const templateOrganizationGroups = require("./groups.handlebars");
const templateOrganizationGroupsList = require("./organizations.groups-list.handlebars");

export function renderGroups(appPathParams) {
    updateView(templateLoading());
    get('/api/organizations/' + appPathParams[0].key, function (responseData, responseStatus, jqXHR) {
        responseData.breadcrumbs = [
            {label: "Hem", url: '#/'},
            {label: responseData.name, url: '#karer/' + responseData.id},
            {label: "Patruller"}
        ];
        responseData.isLoggedIn = isLoggedIn();
        responseData.orgId = responseData.id;

        updateView(templateOrganizationGroups(responseData));

        const $app = $('#app');

        $app.find('.create-group-button').click(function (e) {
            const button = $(this);
            const form = button.closest('form');
            post('/api/organizations/' + appPathParams[0].key + '/groups', getFormData(form), function (responseData, responseStatus, jqXHR) {
                get('/api/organizations/' + appPathParams[0].key + "/groups", function (responseData, responseStatus, jqXHR) {
                    updateView(templateOrganizationGroupsList({
                        groups: responseData,
                        orgId: appPathParams[0].key
                    }), $('#organization-groups-list'));
                });
            }, button);
        });

        get('/api/organizations/' + appPathParams[0].key + "/groups", function (responseData, responseStatus, jqXHR) {
            const groups = responseData.sort(function (grp1, grp2) {
                return grp1.name ? grp1.name.localeCompare(grp2.name) : 0;
            });
            updateView(templateOrganizationGroupsList({
                groups: groups,
                orgId: appPathParams[0].key
            }), $('#organization-groups-list'));
        });
    });
}