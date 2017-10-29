import $ from "jquery";
import styles from "bulma/css/bulma.css";
import {hashChangeHandler} from "./util/routing.jsx";
import tableStyles from "./table.css";

$(function () {
    $(window).on('hashchange', hashChangeHandler);

    const s = styles;
    const ts = tableStyles;

    $(window).trigger('hashchange');
});