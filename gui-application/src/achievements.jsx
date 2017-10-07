import $ from "jquery";
import {get, post} from "./util/api.jsx";
import {updateView, getFormData} from "./util/view.jsx";
const templateAchievements = require("./achievements.handlebars");
const templateAchievementsResult = require("./achievements.result.handlebars");

export function renderAchievements() {
    let data = {
        breadcrumbs: [
            {label: "Hem", url: '#/'},
            {label: "Märken och bedrifter"}
        ]
    };
    updateView(templateAchievements(data));
    $('#app .create-button').click(function (e) {
        const form = $(this).addClass('is-loading').closest('form');
        post('//localhost:8080/api/achievements', getFormData(form), function (responseData, responseStatus, jqXHR) {
            window.location.hash = '#achievements/' + responseData.id;
        });
    });
    $('#app .search-button').click(function (e) {
        const button = $(this);
        const form = button.addClass('is-loading').closest('form');
        const url = '//localhost:8080/api/achievements?filter=' + getFormData(form).filter;
        console.log(url);
        get(url, function (responseData, responseStatus, jqXHR) {
            button.removeClass('is-loading')
            updateView(templateAchievementsResult({achievements: responseData}), $('#achievements-search-result'));
        });
    });
}
