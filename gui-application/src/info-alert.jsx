import $ from "jquery";
import {updateView} from "./util/view.jsx";

const templateInfoAlert = require("./info-alert.handlebars");
const templateLongInfoAlert = require("./info-alert-long.handlebars");
const infoContainer = $(document.createElement('div')).appendTo(document.body)

const closeAlert = function (e) {
    $(this).closest('.modal').removeClass('is-active');
    $(document).off('keyup', escKeyHandler)
}

const escKeyHandler = function (e) {
    if (e.keyCode === 27) { // ESC
        closeAlert.apply(e.data.thisReference)
    }
}

export function renderInfoAlert(title, content, long = false) {
    if (long) {
        updateView(templateLongInfoAlert({
            buttonLabel: 'Okej',
            title: title
        }), infoContainer);
    } else {
        updateView(templateInfoAlert({
            buttonLabel: 'Okej',
            title: title
        }), infoContainer);
    }

    updateView(content, infoContainer.find('.info-alert-content'));

    infoContainer.find('.info-alert-button-close').click(closeAlert);
    infoContainer.find('.info-alert-button-ok').click(closeAlert);
    infoContainer.find('.action-close-modal').click(closeAlert);

    $(document).on('keyup', {thisReference: infoContainer.find('.modal')[0]}, escKeyHandler);
}

export function initInfoAlerts($root) {
    $root.find('.info-alert-link').click(function (e) {
        const templateName = e.target.dataset.template;
        const long = e.target.dataset.long || 'false';
        const template = require('./' + templateName);
        const isLongFormat = !!long && long !== 'false';

        renderInfoAlert($(e.target).text(), template, isLongFormat);
    });
}