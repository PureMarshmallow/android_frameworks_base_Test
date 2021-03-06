/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.security.net.config;

import android.content.Context;
import android.test.AndroidTestCase;
import android.test.MoreAsserts;
import android.util.ArraySet;
import android.util.Pair;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;

public class XmlConfigTests extends AndroidTestCase {

    public void testEmptyConfigFile() throws Exception {
        XmlConfigSource source = new XmlConfigSource(getContext(), R.xml.empty_config);
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertFalse(appConfig.hasPerDomainConfigs());
        NetworkSecurityConfig config = appConfig.getConfigForHostname("");
        assertNotNull(config);
        // Check defaults.
        assertTrue(config.isCleartextTrafficPermitted());
        assertFalse(config.isHstsEnforced());
        assertFalse(config.getTrustAnchors().isEmpty());
        PinSet pinSet = config.getPins();
        assertTrue(pinSet.pins.isEmpty());
        // Try some connections.
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertConnectionSucceeds(context, "developer.android.com", 443);
        TestUtils.assertUrlConnectionSucceeds(context, "google.com", 443);
    }

    public void testEmptyAnchors() throws Exception {
        XmlConfigSource source = new XmlConfigSource(getContext(), R.xml.empty_trust);
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertFalse(appConfig.hasPerDomainConfigs());
        NetworkSecurityConfig config = appConfig.getConfigForHostname("");
        assertNotNull(config);
        // Check defaults.
        assertTrue(config.isCleartextTrafficPermitted());
        assertFalse(config.isHstsEnforced());
        assertTrue(config.getTrustAnchors().isEmpty());
        PinSet pinSet = config.getPins();
        assertTrue(pinSet.pins.isEmpty());
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionFails(context, "android.com", 443);
        TestUtils.assertConnectionFails(context, "developer.android.com", 443);
        TestUtils.assertUrlConnectionFails(context, "google.com", 443);
    }

    public void testBasicDomainConfig() throws Exception {
        XmlConfigSource source = new XmlConfigSource(getContext(), R.xml.domain1);
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertTrue(appConfig.hasPerDomainConfigs());
        NetworkSecurityConfig config = appConfig.getConfigForHostname("");
        assertNotNull(config);
        // Check defaults.
        assertTrue(config.isCleartextTrafficPermitted());
        assertFalse(config.isHstsEnforced());
        assertTrue(config.getTrustAnchors().isEmpty());
        PinSet pinSet = config.getPins();
        assertTrue(pinSet.pins.isEmpty());
        // Check android.com.
        config = appConfig.getConfigForHostname("android.com");
        assertTrue(config.isCleartextTrafficPermitted());
        assertFalse(config.isHstsEnforced());
        assertFalse(config.getTrustAnchors().isEmpty());
        pinSet = config.getPins();
        assertTrue(pinSet.pins.isEmpty());
        // Try connections.
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertConnectionFails(context, "developer.android.com", 443);
        TestUtils.assertUrlConnectionFails(context, "google.com", 443);
        TestUtils.assertUrlConnectionSucceeds(context, "android.com", 443);
    }

    public void testBasicPinning() throws Exception {
        XmlConfigSource source = new XmlConfigSource(getContext(), R.xml.pins1);
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertTrue(appConfig.hasPerDomainConfigs());
        // Check android.com.
        NetworkSecurityConfig config = appConfig.getConfigForHostname("android.com");
        PinSet pinSet = config.getPins();
        assertFalse(pinSet.pins.isEmpty());
        // Try connections.
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertUrlConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertConnectionSucceeds(context, "google.com", 443);
    }

    public void testExpiredPin() throws Exception {
        XmlConfigSource source = new XmlConfigSource(getContext(), R.xml.expired_pin);
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertTrue(appConfig.hasPerDomainConfigs());
        // Check android.com.
        NetworkSecurityConfig config = appConfig.getConfigForHostname("android.com");
        PinSet pinSet = config.getPins();
        assertFalse(pinSet.pins.isEmpty());
        // Try connections.
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertUrlConnectionSucceeds(context, "android.com", 443);
    }

    public void testOverridesPins() throws Exception {
        XmlConfigSource source = new XmlConfigSource(getContext(), R.xml.override_pins);
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertTrue(appConfig.hasPerDomainConfigs());
        // Check android.com.
        NetworkSecurityConfig config = appConfig.getConfigForHostname("android.com");
        PinSet pinSet = config.getPins();
        assertFalse(pinSet.pins.isEmpty());
        // Try connections.
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertUrlConnectionSucceeds(context, "android.com", 443);
    }

    public void testBadPin() throws Exception {
        XmlConfigSource source = new XmlConfigSource(getContext(), R.xml.bad_pin);
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertTrue(appConfig.hasPerDomainConfigs());
        // Check android.com.
        NetworkSecurityConfig config = appConfig.getConfigForHostname("android.com");
        PinSet pinSet = config.getPins();
        assertFalse(pinSet.pins.isEmpty());
        // Try connections.
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionFails(context, "android.com", 443);
        TestUtils.assertUrlConnectionFails(context, "android.com", 443);
        TestUtils.assertConnectionSucceeds(context, "google.com", 443);
    }

    public void testMultipleDomains() throws Exception {
        XmlConfigSource source = new XmlConfigSource(getContext(), R.xml.multiple_domains);
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertTrue(appConfig.hasPerDomainConfigs());
        NetworkSecurityConfig config = appConfig.getConfigForHostname("android.com");
        assertTrue(config.isCleartextTrafficPermitted());
        assertFalse(config.isHstsEnforced());
        assertFalse(config.getTrustAnchors().isEmpty());
        PinSet pinSet = config.getPins();
        assertTrue(pinSet.pins.isEmpty());
        // Both android.com and google.com should use the same config
        NetworkSecurityConfig other = appConfig.getConfigForHostname("google.com");
        assertEquals(config, other);
        // Try connections.
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertConnectionSucceeds(context, "google.com", 443);
        TestUtils.assertConnectionFails(context, "developer.android.com", 443);
        TestUtils.assertUrlConnectionSucceeds(context, "android.com", 443);
    }

    public void testMultipleDomainConfigs() throws Exception {
        XmlConfigSource source = new XmlConfigSource(getContext(), R.xml.multiple_configs);
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertTrue(appConfig.hasPerDomainConfigs());
        // Should be two different config objects
        NetworkSecurityConfig config = appConfig.getConfigForHostname("android.com");
        NetworkSecurityConfig other = appConfig.getConfigForHostname("google.com");
        MoreAsserts.assertNotEqual(config, other);
        // Try connections.
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertConnectionSucceeds(context, "google.com", 443);
        TestUtils.assertUrlConnectionSucceeds(context, "android.com", 443);
    }

    public void testIncludeSubdomains() throws Exception {
        XmlConfigSource source = new XmlConfigSource(getContext(), R.xml.subdomains);
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertTrue(appConfig.hasPerDomainConfigs());
        // Try connections.
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertConnectionSucceeds(context, "developer.android.com", 443);
        TestUtils.assertUrlConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertUrlConnectionSucceeds(context, "developer.android.com", 443);
        TestUtils.assertConnectionFails(context, "google.com", 443);
    }

    public void testAttributes() throws Exception {
        XmlConfigSource source = new XmlConfigSource(getContext(), R.xml.attributes);
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertFalse(appConfig.hasPerDomainConfigs());
        NetworkSecurityConfig config = appConfig.getConfigForHostname("");
        assertTrue(config.isHstsEnforced());
        assertFalse(config.isCleartextTrafficPermitted());
    }

    public void testResourcePemCertificateSource() throws Exception {
        XmlConfigSource source = new XmlConfigSource(getContext(), R.xml.resource_anchors_pem);
        ApplicationConfig appConfig = new ApplicationConfig(source);
        // Check android.com.
        NetworkSecurityConfig config = appConfig.getConfigForHostname("android.com");
        assertTrue(config.isCleartextTrafficPermitted());
        assertFalse(config.isHstsEnforced());
        assertEquals(2, config.getTrustAnchors().size());
        // Try connections.
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertConnectionFails(context, "developer.android.com", 443);
        TestUtils.assertUrlConnectionFails(context, "google.com", 443);
        TestUtils.assertUrlConnectionSucceeds(context, "android.com", 443);
    }

    public void testResourceDerCertificateSource() throws Exception {
        XmlConfigSource source = new XmlConfigSource(getContext(), R.xml.resource_anchors_der);
        ApplicationConfig appConfig = new ApplicationConfig(source);
        // Check android.com.
        NetworkSecurityConfig config = appConfig.getConfigForHostname("android.com");
        assertTrue(config.isCleartextTrafficPermitted());
        assertFalse(config.isHstsEnforced());
        assertEquals(2, config.getTrustAnchors().size());
        // Try connections.
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertConnectionFails(context, "developer.android.com", 443);
        TestUtils.assertUrlConnectionFails(context, "google.com", 443);
        TestUtils.assertUrlConnectionSucceeds(context, "android.com", 443);
    }

    public void testNestedDomainConfigs() throws Exception {
        XmlConfigSource source = new XmlConfigSource(getContext(), R.xml.nested_domains);
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertTrue(appConfig.hasPerDomainConfigs());
        NetworkSecurityConfig parent = appConfig.getConfigForHostname("android.com");
        NetworkSecurityConfig child = appConfig.getConfigForHostname("developer.android.com");
        MoreAsserts.assertNotEqual(parent, child);
        MoreAsserts.assertEmpty(parent.getPins().pins);
        MoreAsserts.assertNotEmpty(child.getPins().pins);
        // Check that the child inherited the cleartext value and anchors.
        assertFalse(child.isCleartextTrafficPermitted());
        MoreAsserts.assertNotEmpty(child.getTrustAnchors());
        // Test connections.
        SSLContext context = TestUtils.getSSLContext(source);
        TestUtils.assertConnectionSucceeds(context, "android.com", 443);
        TestUtils.assertConnectionSucceeds(context, "developer.android.com", 443);
    }

    public void testNestedDomainConfigsOverride() throws Exception {
        XmlConfigSource source = new XmlConfigSource(getContext(), R.xml.nested_domains_override);
        ApplicationConfig appConfig = new ApplicationConfig(source);
        assertTrue(appConfig.hasPerDomainConfigs());
        NetworkSecurityConfig parent = appConfig.getConfigForHostname("android.com");
        NetworkSecurityConfig child = appConfig.getConfigForHostname("developer.android.com");
        MoreAsserts.assertNotEqual(parent, child);
        assertTrue(parent.isCleartextTrafficPermitted());
        assertFalse(child.isCleartextTrafficPermitted());
    }

    private void testBadConfig(int configId) throws Exception {
        try {
            XmlConfigSource source = new XmlConfigSource(getContext(), configId);
            ApplicationConfig appConfig = new ApplicationConfig(source);
            appConfig.getConfigForHostname("android.com");
            fail("Bad config " + getContext().getResources().getResourceName(configId)
                    + " did not fail to parse");
        } catch (RuntimeException e) {
            MoreAsserts.assertAssignableFrom(XmlConfigSource.ParserException.class,
                    e.getCause());
        }
    }

    public void testBadConfig0() throws Exception {
        testBadConfig(R.xml.bad_config0);
    }

    public void testBadConfig1() throws Exception {
        testBadConfig(R.xml.bad_config1);
    }

    public void testBadConfig2() throws Exception {
        testBadConfig(R.xml.bad_config2);
    }

    public void testBadConfig3() throws Exception {
        testBadConfig(R.xml.bad_config3);
    }

    public void testBadConfig4() throws Exception {
        testBadConfig(R.xml.bad_config4);
    }

    public void testBadConfig5() throws Exception {
        testBadConfig(R.xml.bad_config4);
    }
}
