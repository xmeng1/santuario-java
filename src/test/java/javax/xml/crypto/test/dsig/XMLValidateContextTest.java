/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
/*
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 */
package javax.xml.crypto.test.dsig;

import javax.xml.crypto.*;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Unit test for javax.xml.crypto.dsig.XMLValidateContext
 *
 */
public class XMLValidateContextTest extends org.junit.Assert {

    private XMLValidateContext defContext;
    private KeySelector[] KEY_SELECTORS;

    public XMLValidateContextTest() throws Exception {
        // set up the default XMLValidateContext
        SecretKey sk = new SecretKeySpec(new byte[8], "DES");
        defContext = new DOMValidateContext(sk, TestUtils.newDocument());

        // set up the key selectors
        KEY_SELECTORS = new KeySelector[1];
        KEY_SELECTORS[0] = KeySelector.singletonKeySelector(sk);
    }

    @org.junit.Test
    public void testsetngetKeySelector() throws Exception {
        defContext.setKeySelector(null);
        assertNull(defContext.getKeySelector());

        for (int i = 0; i < KEY_SELECTORS.length; i++) {
            defContext.setKeySelector(KEY_SELECTORS[i]);
            assertEquals(defContext.getKeySelector(), KEY_SELECTORS[i]);
        }
    }

    @org.junit.Test
    public void testsetngetBaseURI() throws Exception {
        assertNull(defContext.getBaseURI());

        String uri = "http://www.w3.org/2000/09/xmldsig#";
        defContext.setBaseURI(uri);
        assertEquals(defContext.getBaseURI(), uri);
        defContext.setBaseURI(null);
        assertNull(defContext.getBaseURI());
    }

    @org.junit.Test
    public void testsetngetProperty() throws Exception {
        String name = "key";
        assertNull(defContext.getProperty(name));
        try {
            defContext.setProperty(null, null);
            fail("Should raise a NPE with a null name");
        } catch (NullPointerException npe) {
        } catch (Exception ex) {
            fail("Should raise a NPE instead of " + ex);
        }
        String value1 = "value#1";
        String value2 = "value#2";
        assertNull(defContext.setProperty(name, value1));
        assertEquals(defContext.getProperty(name), value1);
        assertEquals(defContext.setProperty(name, value2), value1);
        assertEquals(defContext.getProperty(name), value2);
        assertEquals(defContext.setProperty(name, null), value2);
        assertNull(defContext.getProperty(name));
    }

    @org.junit.Test
    public void testsetngetURIDereferencer() throws Exception {
        assertNull(defContext.getURIDereferencer());
        byte[] data = "simpleDereferencer".getBytes();
        URIDereferencer deref = new TestUtils.OctetStreamURIDereferencer(data);

        defContext.setURIDereferencer(deref);
        assertEquals(defContext.getURIDereferencer(), deref);
        defContext.setURIDereferencer(null);
        assertNull(defContext.getURIDereferencer());
    }
}
