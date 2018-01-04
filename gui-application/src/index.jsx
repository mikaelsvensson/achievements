import $ from "jquery";
import {hashChangeHandler} from "./util/routing.jsx";
import tableStyles from "./table.css";
import mdiStyles from "mdi/css/materialdesignicons.css";
import themeStyles from "./scout-theme.scss";

$(function () {
    $(window).on('hashchange', hashChangeHandler);

    const ms = themeStyles;
    const ts = tableStyles;
    const mdis = mdiStyles;

    $(window).trigger('hashchange');
});