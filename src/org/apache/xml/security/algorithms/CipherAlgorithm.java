/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "<WebSig>" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 2001, Institute for
 * Data Communications Systems, <http://www.nue.et-inf.uni-siegen.de/>.
 * The development of this software was partly funded by the European
 * Commission in the <WebSig> project in the ISIS Programme.
 * For more information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.xml.security.algorithms;


import java.io.IOException;
import java.security.*;
import java.security.spec.*;
import java.util.Collection;
import java.util.HashMap;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.xml.transform.TransformerException;
import org.apache.xml.security.exceptions.AlgorithmAlreadyRegisteredException;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.utils.*;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.*;


/**
 *
 * @author $Author$
 */
public class CipherAlgorithm extends ElementProxy {

   /** {@link org.apache.log4j} logging facility */
   static org.apache.log4j.Category cat =
      org.apache.log4j.Category.getInstance(CipherAlgorithm.class.getName());

   /** Field algorithm stores the actual {@link javax.crypto.Cipher} */
   javax.crypto.Cipher algorithm = null;

   /** Field _alreadyInitialized */
   static boolean _alreadyInitialized = false;

   /** All available algorithm classes are registered here */
   static HashMap _algorithmHash = null;

   /** Field _algorithmURI */
   Attr _algorithmURI = null;

   /** Field _keySize */
   int _keySize = -1;

   /** Field _digestAlgorithm */
   MessageDigestAlgorithm _digestAlgorithm = null;

   /** Field _digestMethod */
   String _digestMethod = null;

   /** Field _OAEPparams */
   byte[] _OAEPparams = null;

   /**
    * Constructor for the brave who pass their own cipher algorithms and the corresponding URI.
    * @param doc
    * @param cipher
    * @param algorithmURI
    */
   private CipherAlgorithm(Document doc, Cipher cipher, String algorithmURI) {

      this._doc = doc;
      this._state = MODE_SIGN;
      this._constructionElement =
         XMLUtils.createElementInEncryptionSpace(this._doc, EncryptionConstants._TAG_ENCRYPTIONMETHOD);
      this.algorithm = cipher;
      this.setAlgorithmURI(algorithmURI);
   }

   /**
    * Constructor CipherAlgorithm
    *
    * @param element
    * @param BaseURI
    * @throws XMLSecurityException
    */
   public CipherAlgorithm(Element element, String BaseURI)
           throws XMLSecurityException {

      super(element, BaseURI);

      XMLUtils.guaranteeThatElementInEncryptionSpace(this._constructionElement,
              EncryptionConstants._TAG_ENCRYPTIONMETHOD);

      try {
         Element nscontext = XMLUtils.createDSctx(this._doc);

         nscontext.setAttribute("xmlns:ds",
                                Constants.SIGNATURESPECIFICATION_URL);
         nscontext.setAttribute("xmlns:xenc",
                                EncryptionConstants
                                   .ENCRYPTIONSPECIFICATION_URL);

         this._algorithmURI = (Attr) XPathAPI.selectSingleNode(this._constructionElement,
                                             "./@" + Constants._ATT_ALGORITHM);
         if (this._algorithmURI == null) {
            // Algorithm attribute is REQUIRED
            throw new XMLSecurityException("empty");
         }

         Text KeySizeText =
            (Text) XPathAPI.selectSingleNode(this._constructionElement,
                                             "./xenc:"
                                             + EncryptionConstants._TAG_KEYSIZE
                                             + "/text()", nscontext);

         if (KeySizeText != null) {
            this._keySize = Integer.parseInt(KeySizeText.getData());
         }

         Attr DigestMethod =
            (Attr) XPathAPI.selectSingleNode(this._constructionElement,
                                             "./ds:"
                                             + Constants._TAG_DIGESTMETHOD
                                             + "/@" + Constants._ATT_ALGORITHM,
                                             nscontext);

         if (DigestMethod != null) {
            this._digestAlgorithm =
               MessageDigestAlgorithm.getInstance(this._doc,
                                                  DigestMethod.getNodeValue());
         }

         Text oaepParam =
            (Text) XPathAPI
               .selectSingleNode(this._constructionElement, "./xenc:"
                                 + EncryptionConstants._TAG_OAEPPARAMS
                                 + "/text()", nscontext);

         if (oaepParam != null) {
            this._OAEPparams = Base64.decode(oaepParam.getData());
         }
      } catch (TransformerException ex) {
         throw new XMLSecurityException("empty", ex);
      }
   }

   /**
    * Factory method for constructing a message digest algorithm by name.
    *
    * @param doc
    * @param algorithmURI
    * @return
    * @throws XMLSecurityException
    */
   public static CipherAlgorithm getInstance(Document doc, String algorithmURI)
           throws XMLSecurityException {

      JCEMapper.ProviderIdClass algorithmID =
         JCEMapper.translateURItoJCEID(algorithmURI);
      Cipher cipher;

      try {
         cipher = Cipher.getInstance(algorithmID.getAlgorithmID(),
                                     algorithmID.getProviderId());
      } catch (java.security.NoSuchAlgorithmException ex) {
         Object[] exArgs = { algorithmID.getAlgorithmID(),
                             ex.getLocalizedMessage() };

         throw new XMLSecurityException("algorithms.NoSuchAlgorithm", exArgs,
                                        ex);
      } catch (java.security.NoSuchProviderException ex) {
         Object[] exArgs = { algorithmID.getProviderId(),
                             ex.getLocalizedMessage() };

         throw new XMLSecurityException("algorithms.NoSuchAlgorithm", exArgs,
                                        ex);
      } catch (javax.crypto.NoSuchPaddingException ex) {
         throw new XMLSecurityException("empty", ex);
      }

      return new CipherAlgorithm(doc, cipher, algorithmURI);
   }

   /**
    * Returns the actual {@link javax.crypto.Cipher} algorithm object
    *
    * @return the actual {@link javax.crypto.Cipher} algorithm object
    */
   public javax.crypto.Cipher getAlgorithm() {
      return this.algorithm;
   }

   /**
    * Proxy method for {@link javax.crypto.Cipher#getAlgorithm}
    * which is executed on the internal {@link javax.crypto.Cipher} object.
    *
    * @return the result of the {@link javax.crypto.Cipher#getAlgorithm} method
    */
   public String getJCEAlgorithmString() {
      return this.algorithm.getAlgorithm();
   }

   /**
    * Proxy method for {@link javax.crypto.Cipher#getProvider}
    * which is executed on the internal {@link javax.crypto.Cipher} object.
    *
    * @return the result of the {@link javax.crypto.Cipher#getProvider} method
    */
   public java.security.Provider getJCEProvider() {
      return this.algorithm.getProvider();
   }

   /**
    * Proxy method for {@link javax.crypto.Cipher#getBlockSize}
    * which is executed on the internal {@link javax.crypto.Cipher} object.
    *
    * @return the result of the {@link javax.crypto.Cipher#getBlockSize} method
    */
   public int getBlockSize() {
      return this.algorithm.getBlockSize();
   }

   /**
    * Proxy method for {@link javax.crypto.Cipher#getExemptionMechanism}
    * which is executed on the internal {@link javax.crypto.Cipher} object.
    *
    * @return the result of the {@link javax.crypto.Cipher#getExemptionMechanism} method
    */
   public ExemptionMechanism getExemptionMechanism() {
      return this.algorithm.getExemptionMechanism();
   }

   /**
    * Proxy method for {@link javax.crypto.Cipher#getIV}
    * which is executed on the internal {@link javax.crypto.Cipher} object.
    *
    * @return the result of the {@link javax.crypto.Cipher#getIV} method
    */
   public byte[] getIV() {
      return this.algorithm.getIV();
   }

   /**
    * Proxy method for {@link javax.crypto.Cipher#getOutputSize(int)}
    * which is executed on the internal {@link javax.crypto.Cipher} object.
    *
    * @param inputLen
    * @return the result of the {@link javax.crypto.Cipher#getOutputSize(int)} method
    */
   public int getOutputSize(int inputLen) {
      return this.algorithm.getOutputSize(inputLen);
   }

   /**
    * Proxy method for {@link javax.crypto.Cipher#update}
    * which is executed on the internal {@link javax.crypto.Cipher} object.
    *
    * @param input
    */
   public void update(byte[] input) {
      this.algorithm.update(input);
   }

   /**
    * Proxy method for {@link javax.crypto.Cipher#update}
    * which is executed on the internal {@link javax.crypto.Cipher} object.
    *
    * @param buf
    * @param offset
    * @param len
    */
   public void update(byte buf[], int offset, int len) {
      this.algorithm.update(buf, offset, len);
   }

   /**
    * Method getAlgorithmURI
    *
    * @return
    */
   public String getAlgorithmURI() {
      return this._constructionElement.getAttribute(Constants._ATT_ALGORITHM);
   }

   /**
    * Sets the algorithm's URI as used in the signature.
    *
    * @param algorithmURI is the URI of the algorithm as String
    */
   protected void setAlgorithmURI(String algorithmURI) {

      if ((this._state == MODE_SIGN) && (algorithmURI != null)) {
         this._constructionElement.setAttribute(Constants._ATT_ALGORITHM,
                                                algorithmURI);
      }
   }

   public static void providerInit() {

      if (CipherAlgorithm.cat == null) {
         CipherAlgorithm.cat =
            org.apache.log4j.Category
               .getInstance(CipherAlgorithm.class.getName());
      }

      cat.debug("Init() called");

      if (!CipherAlgorithm._alreadyInitialized) {
         CipherAlgorithm._algorithmHash = new HashMap(10);
         CipherAlgorithm._alreadyInitialized = true;
      }
   }

   public static void register(String algorithmURI, String implementingClass, String ProviderId)
           throws AlgorithmAlreadyRegisteredException {

      {
         cat.debug("Try to register " + algorithmURI + " " + implementingClass);



         // are we already registered?
         String registeredClass =
            CipherAlgorithm.getImplementingClass(algorithmURI, ProviderId);

         if ((registeredClass != null) && (registeredClass.length() != 0)) {
            Object exArgs[] = { algorithmURI, registeredClass };

            throw new AlgorithmAlreadyRegisteredException(
               "algorithm.alreadyRegistered", exArgs);
         }

         CipherAlgorithm._algorithmHash.put(algorithmURI, implementingClass);
      }
   }

   /**
    * Method getImplementingClass
    *
    * @param URI
    * @return
    */
   private static String getImplementingClass(String URI, String ProviderId) {

      if (CipherAlgorithm._algorithmHash == null) {
         return null;
      }

      return (String) CipherAlgorithm._algorithmHash.get(URI);
   }

   ///////////////////////////////////////////////////////////////////////////

   /**
    * Method main
    *
    * @param unused
    */
   public static void main(String unused[]) throws Exception {
      //J-
      final String xmlStr = "" +
      "<EncryptionMethod Algorithm='http://www.w3.org/2001/04/xmlenc#aes192-cbc' xmlns='http://www.w3.org/2001/04/xmlenc#'>" + "\n" +
      "  <KeySize>192</KeySize>" + "\n" +
      "  <DigestMethod Algorithm='http://www.w3.org/2000/09/xmldsig#sha1' xmlns='http://www.w3.org/2000/09/xmldsig#' />" + "\n" +
      "  <OAEPparams>wert</OAEPparams>" + "\n" +
      "</EncryptionMethod>" + "\n" +
      "" + "\n" +
      "";
      //J+

      javax.xml.parsers.DocumentBuilderFactory dbf =
         javax.xml.parsers.DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.parse(new java.io.ByteArrayInputStream(xmlStr.getBytes()));
      CipherAlgorithm c = new CipherAlgorithm(doc.getDocumentElement(), "file://1.xml");
      System.out.println(c.getAlgorithmURI());
   }

   static {
      org.apache.xml.security.Init.init();
   }
}
