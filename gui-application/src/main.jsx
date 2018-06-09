import {updateView} from "./util/view.jsx";
import {get, isLoggedIn} from "./util/api.jsx";
import $ from "jquery";
import {navigateTo} from "./util/routing.jsx";

const templateAchievementsResult = require("./achievements/achievements.result.handlebars");

const templateMain = require("./main.handlebars");

const API_HOST = process.env.API_HOST;

const loadResult = function (responseData) {
    const container = $('#main-achievements-showcase');

    updateView(templateAchievementsResult({
        achievements: responseData.sort(function (a, b) {
            return -1 + Math.random() * 2
        }).slice(0, 3)
    }), container);

    $('#main-achievements-count').text(responseData.length)

    container.find('.achievement').click(function (e) {
        navigateTo('marken/' + this.dataset.achievementId)
    });
    container.find('span.tag').click(function (e) {
        const tagName = $(this).text();
        navigateTo('marken/vy/sok/' + encodeURIComponent('"' + tagName + '"'));
        return false; // <-- Prevent event bubbling
    });
};

export function renderMain() {
    updateView(templateMain({
        host: API_HOST,
        isLoggedIn: isLoggedIn()
    }))

    get('/api/achievements', function (responseData, responseStatus, jqXHR) {
        loadResult(responseData);
    });
}