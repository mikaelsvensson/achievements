import {updateView} from "./util/view.jsx";
import {get, isLoggedIn} from "./util/api.jsx";

const templateStats = require("./stats.handlebars");
const templateLoading = require("./loading.handlebars");
export function renderStats() {
    updateView(templateLoading());

    get("/api/stats", function (responseData, responseStatus, jqXHR) {
        responseData.breadcrumbs = [
            {label: "Hem", url: '#/'},
            {label: "Fakta"}
        ];
        responseData.isLoggedIn = isLoggedIn();
        updateView(templateStats(responseData));
    });
}