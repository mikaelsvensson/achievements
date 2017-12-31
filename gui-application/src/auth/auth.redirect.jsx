import {setToken} from "../util/api.jsx";
import {navigateTo} from "../util/routing.jsx";

export function renderRedirect(appPathParams) {
    setToken(appPathParams[0].key);

    navigateTo('minprofil');
}