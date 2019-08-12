import $ from "jquery";
import {hashChangeHandler} from "./util/routing.jsx";
import tableStyles from "./table.css";
import tabsStyles from "./tabs.css";
import mdiStyles from "@mdi/font/css/materialdesignicons.css";
import themeFonts from "./assets/font/stylesheet.css";
import themeStyles from "./scout-theme.scss";
import {initTokenRefresh} from "./util/api.jsx";

$(function () {
    $(window).on('hashchange', hashChangeHandler);

    const ms = themeStyles;
    const tf = themeFonts;
    const ts = tableStyles;
    const tabs = tabsStyles;
    const mdis = mdiStyles;

    $(window).trigger('hashchange');

    $(document).ready(function () {
        $(".navbar-burger").click(function (e) {
            const targetMenu = this.dataset.targetMenu;
            $(this).toggleClass('is-active');
            $(document.getElementById(targetMenu)).toggleClass('is-active');
        });
    })

    initTokenRefresh();
});