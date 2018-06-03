import {updateView} from "../util/view.jsx";

const templateSigninFailed = require("./signin.failed.handlebars");

// CUSTOMER_SUPPORT_EMAIL fetched from environment variable by Webpack during build.
const CUSTOMER_SUPPORT_EMAIL = process.env.CUSTOMER_SUPPORT_EMAIL;

function getMessage(code) {
    switch (code) {
        case 'unspecified':
            return 'Ett fel uppstod när du försökte logga in.';
        case 'unknown-user':
            return 'Din e-postadress har tyvärr inte kopplats till kåren ännu. Kontakta scoutledare så att de kan göra det åt dig.';
        case 'unknown-organization':
            return 'Du försökte logga in hos en kår som inte finns.';
        case 'organization-exists':
            return 'Du försökte skapa en kår som redan finns.';
        case 'invalid-input':
            return 'Vi fick data vi inte förstod.';
        case 'system-error':
            return 'Vi ber om ursäkt, men det verkar som ett underligt fel har inträffat och det är inte ditt fel.';
        case 'invalid-credentials':
            return 'Du verkar ha stavat fel på din e-postadress eller på ditt lösenord.';
        default:
            return 'Ett fel inträffade.';
    }
}

export function renderSigninFailed(appPathParams) {
    updateView(templateSigninFailed({
        customerSupportEmail: CUSTOMER_SUPPORT_EMAIL,
        message: getMessage(appPathParams[0].key)
    }));
}