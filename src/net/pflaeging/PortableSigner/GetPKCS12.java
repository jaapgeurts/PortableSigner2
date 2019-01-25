/*
 * GetPKCS12.java
 *
 * Created on 15. November 2006, 11:18
 * This File is part of PortableSigner (http://portablesigner.sf.net/)
 *  and is under the European Public License V1.1 (http://www.osor.eu/eupl)
 * (c) Peter Pfl�ging <peter@pflaeging.net>
 */
package net.pflaeging.PortableSigner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.util.NoSuchElementException;
import java.util.Enumeration;

/**
 *
 * @author pfp
 */
public class GetPKCS12 {

    public static PrivateKey privateKey;

    public static Certificate[] certificateChain;
    public static String subject;
    public static java.math.BigInteger serial;
    public static java.util.Date notBefore, notAfter;
    public static String issuer;
    public static String atEgovOID;

    X509Certificate x509cert;

    /**
     * Creates a new instance of GetPKCS12
     */
    public GetPKCS12(String pkcs12FileName,
            String pkcs12Password) throws KeyStoreException {
        KeyStore ks = null;
        FileInputStream fis = null;
        if (pkcs12Password == null) {
            pkcs12Password = "";
        }
        try {
            ks = KeyStore.getInstance("pkcs12");
            fis = new FileInputStream(pkcs12FileName);
            ks.load(fis, pkcs12Password
                    .toCharArray());
        } catch (NoSuchAlgorithmException e) {
            throw new KeyStoreException(java.util.ResourceBundle.getBundle("net/pflaeging/PortableSigner/i18n").getString("ErrorReadingCertificateAlgorithm"));
        } catch (CertificateException e) {
            throw new KeyStoreException(java.util.ResourceBundle.getBundle("net/pflaeging/PortableSigner/i18n").getString("ErrorReadingCertificate"));
        } catch (FileNotFoundException e) {
            throw new KeyStoreException(java.util.ResourceBundle.getBundle("net/pflaeging/PortableSigner/i18n").getString("ErrorReadingCertificateNotAccessible"));
        } catch (IOException e) {
            throw new KeyStoreException(java.util.ResourceBundle.getBundle("net/pflaeging/PortableSigner/i18n").getString("ErrorReadingCertificateIO"));
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                // ignore or print a message
            }
        }

        if (ks != null) {
            String alias = "";
            try {
//      Maybe not only one cert in file! Thanks to Markus Feisst
                Enumeration aliases = ks.aliases();

                alias = (String) aliases.nextElement();

                while (aliases.hasMoreElements() && !ks.isKeyEntry(alias)) {
                    alias = (String) aliases.nextElement();
                }

                privateKey = (PrivateKey) ks.getKey(alias, pkcs12Password.toCharArray());
            } catch (NoSuchElementException e) {
                throw new KeyStoreException(java.util.ResourceBundle.getBundle("net/pflaeging/PortableSigner/i18n").getString("ErrorReadingCertificateNoKey"));
            } catch (NoSuchAlgorithmException e) {
                throw new KeyStoreException(java.util.ResourceBundle.getBundle("net/pflaeging/PortableSigner/i18n").getString("ErrorReadingCertificateAlgorythm"));
            } catch (UnrecoverableKeyException e) {
                throw new KeyStoreException(java.util.ResourceBundle.getBundle("net/pflaeging/PortableSigner/i18n").getString("ErrorReadingCertificateAlgorythm"));
            }
            certificateChain = ks.getCertificateChain(alias);
            x509cert = (X509Certificate) ks.getCertificate(alias);
            subject = x509cert.getSubjectX500Principal().toString();
            serial = x509cert.getSerialNumber();
            notBefore = x509cert.getNotBefore();
            notAfter = x509cert.getNotAfter();
            issuer = x509cert.getIssuerX500Principal().toString();
            java.util.ResourceBundle oid
                    = java.util.ResourceBundle.getBundle("net/pflaeging/PortableSigner/SpecialOID");
            atEgovOID = "";

            for (Enumeration<String> o = oid.getKeys(); o.hasMoreElements();) {
                String element = o.nextElement();
                // System.out.println(element + ":" + oid.getString(element));
                java.util.Collection<String> bCert = x509cert.getNonCriticalExtensionOIDs();
                if (bCert != null) {
                    if (bCert.contains(element)) {
                        if (!atEgovOID.equals("")) {
                            atEgovOID += ", ";
                        }
                        atEgovOID += oid.getString(element) + " (OID=" + element + ")";
                    }
                }
            }
        }

    }

}
