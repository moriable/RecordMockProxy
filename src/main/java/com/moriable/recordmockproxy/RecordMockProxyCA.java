package com.moriable.recordmockproxy;

import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509ExtensionUtils;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.operator.*;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.util.encoders.Base64;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import java.util.regex.Pattern;

public class RecordMockProxyCA {
    private static X509Certificate caCert;
    private static PrivateKey caKey;
    private static Map<String, KeyStore> cache = new HashMap<>();

    private RecordMockProxyCA() {
    }

    public static void init(String caCertPath, String caPrivateKeyPath) throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        java.security.Security.addProvider(
                new org.bouncycastle.jce.provider.BouncyCastleProvider()
        );

        caCert = getCACertificate(caCertPath);
        caKey = getCAPrivateKey(caPrivateKeyPath);
    }

    public static synchronized KeyStore getKeyStoreWithCertificate(String host) throws NoSuchAlgorithmException, CertificateException, OperatorCreationException, IOException, InvalidKeyException, NoSuchProviderException, SignatureException, KeyStoreException {
        if (caCert == null) {
            throw new IllegalStateException("not init");
        }

        if (cache.containsKey(host)) {
            return cache.get(host);
        }

        KeyPair serverKeyPair = createKeyPair();
        X509Certificate serverCert = createServerCertificate(host, serverKeyPair);
        serverCert = createSignedCertificate(serverCert);

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, "password".toCharArray());

        java.security.cert.Certificate[] chain = new java.security.cert.Certificate[1];
        chain[0] = (java.security.cert.Certificate) serverCert;
        ks.setKeyEntry("proxy", serverKeyPair.getPrivate(), "password".toCharArray(), chain);

        cache.put(host, ks);

        return ks;
    }

    private static X509Certificate getCACertificate(String caCertPath) throws CertificateException, FileNotFoundException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new FileInputStream(caCertPath));
    }

    private static PrivateKey getCAPrivateKey(String caPrivateKeyPath) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        byte[] keyBytes = Files.readAllBytes(new File(caPrivateKeyPath).toPath());
        String temp = new String(keyBytes);
        String header = temp.replace("-----BEGIN RSA PRIVATE KEY-----\n", "");
        header = header.replace("-----END RSA PRIVATE KEY-----", "");
        byte[] decoded = new Base64().decode(header);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return kf.generatePrivate(spec);
    }

    private static KeyPair createKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        return kpg.generateKeyPair();
    }

    private static X509Certificate createServerCertificate(String host, KeyPair keyPair) throws IOException, OperatorCreationException, CertificateException {
        X500Name issuer = new X500Name(caCert.getSubjectDN().getName());
        BigInteger serial = new BigInteger(32, new SecureRandom());
        Date from = new Date();
        Date to = new Date(System.currentTimeMillis() + (3650 * 86400000L));
        SubjectPublicKeyInfo keyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());

        X509v3CertificateBuilder certgen = new X509v3CertificateBuilder(issuer,
                serial,
                from,
                to,
                new X500Name("CN=" + host),
                keyInfo);

        SubjectPublicKeyInfo caKeyInfo = SubjectPublicKeyInfo.getInstance(caCert.getPublicKey().getEncoded());
        DigestCalculator digCalc = new BcDigestCalculatorProvider().get(new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1));
        AuthorityKeyIdentifier authKeyId = new X509ExtensionUtils(digCalc).createAuthorityKeyIdentifier(caKeyInfo);

        certgen.addExtension(Extension.authorityKeyIdentifier, false, authKeyId);
        certgen.addExtension(Extension.subjectKeyIdentifier, false, new SubjectKeyIdentifier(keyPair.getPublic().getEncoded()));

        List<GeneralName> altNames = new ArrayList<GeneralName>();
        if (Pattern.matches("((\\d|[1-9]\\d|1\\d\\d|2[0-4]\\d|25[0-5])([.](?!$)|$)){4}", host)) {
            altNames.add(new GeneralName(GeneralName.iPAddress, host));
        } else {
            altNames.add(new GeneralName(GeneralName.dNSName, host));
        }
        GeneralNames subjectAltNames = GeneralNames.getInstance(new DERSequence((GeneralName[]) altNames.toArray(new GeneralName[] {})));
        certgen.addExtension(Extension.subjectAlternativeName, false, subjectAltNames);

        AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA512withRSA");
        AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);

        ContentSigner x509signer = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(PrivateKeyFactory.createKey(caKey.getEncoded()));
        X509CertificateHolder holder = certgen.build(x509signer);
        return new JcaX509CertificateConverter().getCertificate(holder);
    }

    private static X509Certificate createSignedCertificate(X509Certificate cert) throws CertificateException, IOException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        X500Name issuer = new X500Name(caCert.getSubjectX500Principal().getName());
        String issuerSigAlg = caCert.getSigAlgName();

        byte[] inCertBytes = cert.getTBSCertificate();
        X509CertInfo info = new X509CertInfo(inCertBytes);
        info.set(X509CertInfo.ISSUER, caCert.getSubjectDN());

        X509CertImpl outCert = new X509CertImpl(info);
        outCert.sign(caKey, issuerSigAlg);

        return outCert;
    }
}
