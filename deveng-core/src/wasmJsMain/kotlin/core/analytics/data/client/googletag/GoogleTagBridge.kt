@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package core.analytics.data.client.googletag

@JsFun("() => window.location.hostname")
internal external fun readPageHostname(): String

@JsFun(
    """(configurationJson) => {
    var configuration = JSON.parse(configurationJson);
    var consent = configuration.consent;
    var text = consent ? consent.texts : null;
    var CONSENT_GRANTED = configuration.grantedValue;
    var CONSENT_DENIED = configuration.deniedValue;
    var ANALYTICS_COOKIE_PREFIX = configuration.analyticsCookiePrefix;
    var rememberedConsent = null;
    var consentBanner = null;

    window.dataLayer = window.dataLayer || [];
    function gtag() { window.dataLayer.push(arguments); }

    function readStoredConsent() {
        if (!consent) return null;
        if (rememberedConsent !== null) return rememberedConsent;
        try {
            var stored = window.localStorage.getItem(consent.storageKey);
            if (stored === CONSENT_GRANTED || stored === CONSENT_DENIED) rememberedConsent = stored;
        } catch (error) {
            console.warn('[google tag] The analytics choice could not be read.', error);
        }
        return rememberedConsent;
    }

    function storeConsent(choice) {
        rememberedConsent = choice;
        try {
            window.localStorage.setItem(consent.storageKey, choice);
        } catch (error) {
            console.warn('[google tag] The analytics choice could not be stored.', error);
        }
    }

    function removeAnalyticsCookies() {
        document.cookie.split(';').forEach(function (cookie) {
            var cookieName = cookie.split('=')[0].trim();
            if (cookieName.indexOf(ANALYTICS_COOKIE_PREFIX) !== 0) return;
            document.cookie = cookieName + '=; Max-Age=0; path=/; domain=' + window.location.hostname;
        });
    }

    function hideConsentBanner() {
        if (consentBanner === null) return;
        consentBanner.remove();
        consentBanner = null;
    }

    function applyConsent(choice) {
        if (!consent) return;
        storeConsent(choice);
        gtag('consent', 'update', { analytics_storage: choice });
        if (choice === CONSENT_DENIED) removeAnalyticsCookies();
        hideConsentBanner();
    }

    function createConsentButton(label, extraClass, choice) {
        var button = document.createElement('button');
        button.type = 'button';
        button.className = extraClass === null ? consent.cssClassPrefix + '__button' : consent.cssClassPrefix + '__button ' + extraClass;
        button.textContent = label;
        button.addEventListener('click', function () { applyConsent(choice); });
        return button;
    }

    function createConsentBanner() {
        var banner = document.createElement('aside');
        banner.className = consent.cssClassPrefix;
        banner.setAttribute('aria-label', text.bannerLabel);

        var message = document.createElement('p');
        message.className = consent.cssClassPrefix + '__text';
        message.appendChild(document.createTextNode(text.bannerMessage));
        if (consent.privacyUrl) {
            message.appendChild(document.createTextNode(' '));
            var detailsLink = document.createElement('a');
            detailsLink.href = consent.privacyUrl;
            detailsLink.textContent = text.details;
            message.appendChild(detailsLink);
        }

        var actions = document.createElement('div');
        actions.className = consent.cssClassPrefix + '__actions';
        actions.appendChild(createConsentButton(text.decline, null, CONSENT_DENIED));
        actions.appendChild(createConsentButton(text.accept, consent.cssClassPrefix + '__button--accept', CONSENT_GRANTED));

        banner.appendChild(message);
        banner.appendChild(actions);
        return banner;
    }

    function showConsentBanner() {
        if (!consent || consentBanner !== null) return;
        consentBanner = createConsentBanner();
        // Compose makes document.body a shadow host without a slot, so a child of the body is never drawn.
        document.documentElement.appendChild(consentBanner);
    }

    gtag('consent', 'default', {
        ad_storage: CONSENT_DENIED,
        ad_user_data: CONSENT_DENIED,
        ad_personalization: CONSENT_DENIED,
        analytics_storage: readStoredConsent() === CONSENT_GRANTED ? CONSENT_GRANTED : CONSENT_DENIED
    });
    gtag('js', new Date());
    gtag('config', configuration.measurementId, {
        cookie_domain: window.location.hostname,
        send_page_view: false
    });

    var tagScript = document.createElement('script');
    tagScript.async = true;
    tagScript.src = 'https://www.googletagmanager.com/gtag/js?id=' + encodeURIComponent(configuration.measurementId);
    document.head.appendChild(tagScript);

    if (consent && consent.showBanner && readStoredConsent() === null) showConsentBanner();

    window[configuration.globalObjectName] = Object.freeze({
        trackEvent: function (eventName, parametersJson) {
            gtag('event', eventName, JSON.parse(parametersJson));
        },
        trackScreenView: function (pageLocation, pageTitle) {
            gtag('event', 'page_view', { page_location: window.location.origin + pageLocation, page_title: pageTitle });
        },
        showConsentBanner: showConsentBanner,
        grantConsent: function () { applyConsent(CONSENT_GRANTED); },
        denyConsent: function () { applyConsent(CONSENT_DENIED); }
    });
}"""
)
internal external fun installGoogleTag(configurationJson: String)

@JsFun(
    """(globalObjectName, eventName, parametersJson) => {
    var googleTag = window[globalObjectName];
    if (googleTag) googleTag.trackEvent(eventName, parametersJson);
}"""
)
internal external fun sendGoogleTagEvent(globalObjectName: String, eventName: String, parametersJson: String)

@JsFun(
    """(globalObjectName, pageLocation, pageTitle) => {
    var googleTag = window[globalObjectName];
    if (googleTag) googleTag.trackScreenView(pageLocation, pageTitle);
}"""
)
internal external fun sendGoogleTagScreenView(globalObjectName: String, pageLocation: String, pageTitle: String)
