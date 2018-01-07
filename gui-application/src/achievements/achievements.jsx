import $ from "jquery";
import {get, isLoggedIn} from "../util/api.jsx";
import {getFormData, updateView} from "../util/view.jsx";

const templateAchievements = require("./achievements.handlebars");
const templateAchievementsResult = require("./achievements.result.handlebars");

export function renderAchievements() {
    let data = {
        breadcrumbs: [
            {label: "Hem", url: '#/'},
            {label: "Märken"}
        ],
        isLoggedIn: isLoggedIn()
    };
    updateView(templateAchievements(data));

    get('/api/achievements', function (responseData, responseStatus, jqXHR) {
        updateView(templateAchievementsResult({achievements: responseData}), $('#achievements-search-result'));
    });

    const $app = $('#app');
    // $app.find('.create-button').click(function (e) {
    //     const form = $(this).addClass('is-loading').closest('form');
    //     post('/api/achievements', getFormData(form), function (responseData, responseStatus, jqXHR) {
    //         navigateTo('marken/' + responseData.id);
    //     });
    // });
    $app.find('.search-button').click(function (e) {
        const button = $(this);
        const form = button.addClass('is-loading').closest('form');
        get('/api/achievements?filter=' + getFormData(form).filter, function (responseData, responseStatus, jqXHR) {
            button.removeClass('is-loading');

            updateView(templateAchievementsResult({achievements: responseData}), $('#achievements-search-result'));
        });
    });
}
