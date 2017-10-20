import $ from "jquery";
import styles from "bulma/css/bulma.css";
import {hashChangeHandler} from "./util/routing.jsx";

$(function () {
    $(window).on('hashchange', hashChangeHandler);

    const s = styles;

    $(window).trigger('hashchange');
});