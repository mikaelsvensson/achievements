import $ from "jquery";
import {updateView} from "./util/view.jsx";
const templateStats = require("./stats.handlebars");
const templateLoading = require("./loading.handlebars");
export function renderStats() {
    updateView(templateLoading());
    $.getJSON("//localhost:8080/api/stats", function (data) {
        data.breadcrumbs = [
            {label: "Hem", url: '#/'},
            {label: "Fakta"}
        ];
        updateView(templateStats(data));
    });
}