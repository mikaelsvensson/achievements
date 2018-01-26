import $ from "jquery";
import {get, isLoggedIn} from "../util/api.jsx";
import {getFormData, updateView} from "../util/view.jsx";
import {navigateTo} from "../util/routing.jsx";

const templateAchievements = require("./achievements.handlebars");
const templateAchievementsResult = require("./achievements.result.handlebars");

const loadResult = function (responseData) {
    const container = $('#achievements-search-result');

    updateView(templateAchievementsResult({achievements: responseData}), container);

    container.find('.achievement').click(function (e) {
        navigateTo('marken/' + this.dataset.achievementId)
    });
};

export function renderAchievements() {
    const data = {
        breadcrumbs: [
            {label: "Hem", url: '#/'},
            {label: "Märken"}
        ],
        isLoggedIn: isLoggedIn()
    };
    updateView(templateAchievements(data));

    get('/api/achievements', function (responseData, responseStatus, jqXHR) {
        loadResult(responseData);
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
            loadResult(responseData);
        });
    });
}
