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

import java.util.*;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.*;

/**
 * Unit test for javax.xml.crypto.dsig.XMLObject
 *
 */
public class XMLObjectTest extends org.junit.Assert {

    private XMLSignatureFactory factory;
    private String id = "id";
    private String mimeType = "mime";
    private String encoding = "encoding";

    public XMLObjectTest() throws Exception {
        factory = XMLSignatureFactory.getInstance
            ("DOM", new org.apache.jcp.xml.dsig.internal.dom.XMLDSigRI());
    }

    @SuppressWarnings("rawtypes")
    @org.junit.Test
    public void testConstructor() {
        // test XMLSignatureFactory.newXMLObject(List, String, String, String)
        XMLObject obj;

        obj = factory.newXMLObject(null, null, null, null);
        assertNotNull(obj);

        List<XMLStructure> list = new ArrayList<>();
        obj = factory.newXMLObject(list, null, null, null);
        assertNotNull(obj);

        String strEntry = "wrong type";
        // use raw List type to test for invalid XMLStructure entries
        List invalidList = new ArrayList();
        addEntryToRawList(invalidList, strEntry);
        try {
            factory.newXMLObject(invalidList, null, null, null);
            fail("Should raise a CCE for content containing " +
                 "invalid, i.e. non-XMLStructure, entries");
        } catch (ClassCastException cce) {
        } catch (Exception ex) {
            fail("Should raise a CCE for content with invalid entries " +
                 "instead of " + ex);
        }
        list.add(new TestUtils.MyOwnXMLStructure());
        obj = factory.newXMLObject(list, id, mimeType, encoding);
        assertNotNull(obj);
        assertNotNull(obj.getContent());
        assertTrue(Arrays.equals(obj.getContent().toArray(), list.toArray()));
        assertEquals(obj.getId(), id);
        assertEquals(obj.getMimeType(), mimeType);
        assertEquals(obj.getEncoding(), encoding);

        @SuppressWarnings("unchecked")
        List<XMLStructure> unmodifiable = obj.getContent();
        try {
            unmodifiable.add(new TestUtils.MyOwnXMLStructure());
            fail("Should return an unmodifiable List object");
        } catch (UnsupportedOperationException uoe) {}
        assertTrue(Arrays.equals(unmodifiable.toArray(), list.toArray()));
    }

    @org.junit.Test
    public void testisFeatureSupported() {
        List<XMLStructure> list = new ArrayList<>();
        list.add(new TestUtils.MyOwnXMLStructure());
        XMLObject obj = factory.newXMLObject(list, id, mimeType, encoding);
        try {
            obj.isFeatureSupported(null);
            fail("Should raise a NPE for null feature");
        } catch (NullPointerException npe) {}

        assertTrue(!obj.isFeatureSupported("not supported"));
    }

    @SuppressWarnings({
     "unchecked", "rawtypes"
    })
    private static void addEntryToRawList(List list, Object entry) {
        list.add(entry);
    }
}
