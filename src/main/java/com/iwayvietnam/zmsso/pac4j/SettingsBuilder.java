/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zm SSO is the the  Zimbra Collaboration Open Source Edition extension Single Sign On authentication to the Zimbra Web Client.
 * Copyright (C) 2020-present iWay Vietnam and/or its affiliates. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 * ***** END LICENSE BLOCK *****
 *
 * Zimbra Hierarchical Address Book
 *
 * Written by Nguyen Van Nguyen <nguyennv1981@gmail.com>
 */
package com.iwayvietnam.zmsso.pac4j;

import com.zimbra.cs.extension.ExtensionException;
import org.pac4j.cas.client.CasClient;
import org.pac4j.cas.config.CasConfiguration;
import org.pac4j.cas.config.CasProtocol;
import org.pac4j.config.client.PropertiesConfigFactory;
import org.pac4j.config.client.PropertiesConstants;
import org.pac4j.core.client.Client;
import org.pac4j.core.config.Config;
import org.pac4j.core.logout.handler.LogoutHandler;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.config.SAML2Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Nguyen Van Nguyen <nguyennv1981@gmail.com>
 */
public final class SettingsBuilder {
    private static String ZM_SSO_SETTINGS_FILE = "sso.pac4j.properties";
    private static String ZM_SSO_DEFAULT_CLIENT = "sso.defaultClient";
    private static String ZM_SSO_CALLBACK_URL = "sso.callbackUrl";

    private static String CAS_CALLBACK_URL = "cas.callbackUrl";
    private static String OIDC_CALLBACK_URL = "oidc.callbackUrl";
    private static String SAML_CALLBACK_URL = "saml.callbackUrl";

    private static Map<String, String> properties = new HashMap<>();
    private static Client defaultClient;

    private static Map<String, String> loadProperties(String propFileName) {
        ClassLoader classLoader = SettingsBuilder.class.getClassLoader();
        Properties prop = new Properties();
        InputStream inputStream = classLoader.getResourceAsStream(propFileName);
        try {
            prop.load(inputStream);
            for (String key: prop.stringPropertyNames()) {
                properties.put(key, prop.getProperty(key));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

    private static CasClient loadCasSetting(){
        Map<String, String> prop = loadProperties(ZM_SSO_SETTINGS_FILE);
        String loginUrl = prop.get(PropertiesConstants.CAS_LOGIN_URL);
        CasProtocol protocol = CasProtocol.valueOf(prop.get(PropertiesConstants.CAS_PROTOCOL));
        CasConfiguration cfg = new CasConfiguration(loginUrl, protocol);
        cfg.setLogoutHandler(new ZmLogoutHandler());
        CasClient client = new CasClient(cfg);
        client.setCallbackUrl(prop.get(CAS_CALLBACK_URL));
        return client;
    }

    private static OidcClient loadOidcSetting() {
        final Map<String, String> prop = loadProperties(ZM_SSO_SETTINGS_FILE);
        final OidcConfiguration cfg = new OidcConfiguration();
        cfg.setClientId(prop.get(PropertiesConstants.OIDC_ID));
        cfg.setSecret(prop.get(PropertiesConstants.OIDC_SECRET));
        cfg.setDiscoveryURI(prop.get(PropertiesConstants.OIDC_DISCOVERY_URI));
        cfg.setScope(prop.get(PropertiesConstants.OIDC_SCOPE));
        cfg.setUseNonce(true);
        cfg.setLogoutHandler(new ZmLogoutHandler());

        final OidcClient client = new OidcClient(cfg);
        client.setCallbackUrl(prop.get(OIDC_CALLBACK_URL));
        return client;
    }

    private static SAML2Client loadSamlSetting() {
        final Map<String, String> prop = loadProperties(ZM_SSO_SETTINGS_FILE);
        SAML2Configuration cfg = new SAML2Configuration(
            prop.get(PropertiesConstants.SAML_KEYSTORE_PATH),
            prop.get(PropertiesConstants.SAML_KEYSTORE_PASSWORD),
            prop.get(PropertiesConstants.SAML_PRIVATE_KEY_PASSWORD),
            prop.get(PropertiesConstants.SAML_IDENTITY_PROVIDER_METADATA_PATH)
        );
        cfg.setServiceProviderEntityId(prop.get(PropertiesConstants.SAML_SERVICE_PROVIDER_ENTITY_ID));
        cfg.setAuthnRequestBindingType(prop.get(PropertiesConstants.SAML_AUTHN_REQUEST_BINDING_TYPE));
        cfg.setLogoutHandler(new ZmLogoutHandler());
        SAML2Client client = new SAML2Client(cfg);
        client.setCallbackUrl(prop.get(SAML_CALLBACK_URL));
        return client;
    }

    public static Config build() throws ExtensionException {
        final Map<String, String> prop = loadProperties(ZM_SSO_SETTINGS_FILE);
        final LogoutHandler logoutHandler = new ZmLogoutHandler();
        final PropertiesConfigFactory factory = new PropertiesConfigFactory(prop.get(ZM_SSO_CALLBACK_URL), prop);
        final Config config =  factory.build();

        config.getClients().findClient(CasClient.class).ifPresent(client -> client.getConfiguration().setLogoutHandler(logoutHandler));
        config.getClients().findClient(OidcClient.class).ifPresent(client -> client.getConfiguration().setLogoutHandler(logoutHandler));
        config.getClients().findClient(SAML2Client.class).ifPresent(client -> client.getConfiguration().setLogoutHandler(logoutHandler));

        defaultClient = config.getClients().findClient(prop.get(ZM_SSO_DEFAULT_CLIENT)).orElseThrow(() -> new ExtensionException("No client found"));

        return config;
    }

    public static Client defaultClient() {
        return defaultClient;
    }
}
