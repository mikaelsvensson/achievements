import $ from "jquery";
import {get, isLoggedIn, sortBy} from "../util/api.jsx";
import {getFormData, updateView} from "../util/view.jsx";
import {navigateTo} from "../util/routing.jsx";

const templateAchievements = require("./achievements.handlebars");
const templateAchievementsResult = require("./achievements.result.handlebars");

const loadResult = function (responseData) {
    const container = $('#achievements-search-result');

    responseData.sort(sortBy('name'));

    updateView(templateAchievementsResult({achievements: responseData}), container);

    container.find('.achievement').click(function (e) {
        navigateTo('marken/' + this.dataset.achievementId)
    });
    container.find('span.tag').click(function (e) {
        const tagName = $(this).text();
        navigateTo('marken/vy/sok/' + encodeURIComponent('"' + tagName + '"'));
        return false; // <-- Prevent event bubbling
    });
};

export function renderAchievements(appPathParams) {
    const data = {
        breadcrumbs: [
            {label: "Hem", url: '#/'},
            {label: "Märken"}
        ],
        isLoggedIn: isLoggedIn()
    };
    updateView(templateAchievements(data));

    if (appPathParams[1]) {
        get('/api/achievements?filter=' + appPathParams[1].key, function (responseData, responseStatus, jqXHR) {
            loadResult(responseData);
        });
        $('#achievements-search-filter').val(appPathParams[1].key)
    } else {
        get('/api/achievements', function (responseData, responseStatus, jqXHR) {
            loadResult(responseData);
        });
    }


    // $app.find('.create-button').click(function (e) {
    //     const form = $(this).addClass('is-loading').closest('form');
    //     post('/api/achievements', getFormData(form), function (responseData, responseStatus, jqXHR) {
    //         navigateTo('marken/' + responseData.id);
    //     });
    // });
    const $searchButton = $('#app').find('.search-button');
    const $form = $searchButton.closest('form');

    const submitter = function () {
        const searchQuery = getFormData($form).filter;
        navigateTo('marken/vy/sok/' + encodeURIComponent(searchQuery));
        return false;
    }

    $form.submit(submitter)
    $searchButton.click(submitter);
}
