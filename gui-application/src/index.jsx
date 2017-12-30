import $ from "jquery";
import styles from "bulma/css/bulma.css";
import {hashChangeHandler} from "./util/routing.jsx";
import tableStyles from "./table.css";
import mdiStyles from "mdi/css/materialdesignicons.css";

$(function () {
    $(window).on('hashchange', hashChangeHandler);

    const s = styles;
    const ts = tableStyles;
    const mdis = mdiStyles;

    $(window).trigger('hashchange');
});