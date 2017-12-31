import {setToken} from "../util/api.jsx";
import {navigateTo} from "../util/routing.jsx";

export function renderRedirect(appPathParams) {

    console.log("Got parameters:", appPathParams)

    setToken(appPathParams[0].key);

    navigateTo('minprofil');
}