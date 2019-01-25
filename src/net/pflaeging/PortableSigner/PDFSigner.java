/*
 * PDFSigner.java
 *
 * Created on 05. May 2009, 15:25
 * This File is part of PortableSigner (http://portablesigner.sf.net/)
 *  and is under the European Public License V1.1 (http://www.osor.eu/eupl)
 * (c) Peter Pfl�ging <peter@pflaeging.net>
 * Patches and bugfixes from: dzoe@users.sourceforge.net
 * 
 */
package net.pflaeging.PortableSigner;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfContentByte;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStoreException;
import java.util.Date;
import java.util.ResourceBundle;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSignatureAppearance;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfSignature;
import com.itextpdf.text.pdf.security.BouncyCastleDigest;
import com.itextpdf.text.pdf.security.DigestAlgorithms;
import com.itextpdf.text.pdf.security.ExternalDigest;
import com.itextpdf.text.pdf.security.ExternalSignature;
import com.itextpdf.text.pdf.security.MakeSignature;
import com.itextpdf.text.pdf.security.PrivateKeySignature;
import com.itextpdf.text.xml.xmp.XmpWriter;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.security.Security;
import java.util.HashMap;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 *
 * @author peter@pflaeging.net
 */
public class PDFSigner {

    private static GetPKCS12 pkcs12;
    public float ptToCm = (float) 0.0352777778;
    private String IMG_FILE = "/home/jaapg/Pictures/fontys-logo.png";

    public void doSignPDF(String pdfInputFileName,
            String pdfOutputFileName,
            String pkcs12FileName,
            String password,
            Boolean signText,
            String signLanguage,
            String sigLogo,
            Boolean finalize,
            String sigComment,
            String signReason,
            String signLocation,
            Boolean noExtraPage,
            float verticalPos,
            float leftMargin,
            float rightMargin,
            Boolean signLastPage,
            byte[] ownerPassword) throws PDFSignerException {

        try {
            // Get the keystore
            System.err.println("Position V:" + verticalPos + " L:" + leftMargin + " R:" + rightMargin);
            Rectangle signatureBlock;

            BouncyCastleProvider provider = new BouncyCastleProvider();
            Security.addProvider(provider);

            pkcs12 = new GetPKCS12(pkcs12FileName, password);

            // open the source document
            PdfReader reader = null;
            try {
//                System.out.println("Password:" + ownerPassword.toString());
                if (ownerPassword == null) {
                    reader = new PdfReader(pdfInputFileName);
                } else {
                    reader = new PdfReader(pdfInputFileName, ownerPassword);
                }
            } catch (IOException e) {

                throw new PDFSignerException(
                        java.util.ResourceBundle.getBundle(
                                "net/pflaeging/PortableSigner/i18n").getString(
                                        "CouldNotBeOpened"),
                        true,
                        e.getLocalizedMessage());
            }

            // Open the new output dosocument.
            FileOutputStream fout = null;
            try {
                fout = new FileOutputStream(pdfOutputFileName);
            } catch (FileNotFoundException e) {

                throw new PDFSignerException(
                        java.util.ResourceBundle.getBundle("net/pflaeging/PortableSigner/i18n").getString("CouldNotBeWritten"),
                        true,
                        e.getLocalizedMessage());
            }
            try {

                PdfStamper stamper = PdfStamper.createSignature(reader, fout, '\0');

                // 
                // set additional info
                HashMap<String, String> pdfInfo = reader.getInfo();
                // thanks to Markus Feisst
                String pdfInfoProducer = "";

                if (pdfInfo.get("Producer") != null) {
                    pdfInfoProducer = pdfInfo.get("Producer").toString();
                    pdfInfoProducer = pdfInfoProducer + " (signed with PortableSigner " + Version.release + ")";
                } else {
                    pdfInfoProducer = "Unknown Producer (signed with PortableSigner " + Version.release + ")";
                }
                pdfInfo.put("Producer", pdfInfoProducer);
                //System.err.print("++ Producer:" + pdfInfo.get("Producer").toString());
                stamper.setMoreInfo(pdfInfo);
                
                // add info as xmp metadata
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                XmpWriter xmp = new XmpWriter(baos, pdfInfo);
                xmp.close();
                stamper.setXmpMetadata(baos.toByteArray());

                // Create the appearance
                PdfSignatureAppearance appearance = stamper.getSignatureAppearance();
                appearance.setReason(signReason);
                appearance.setLocation(signLocation);
                appearance.setVisibleSignature(new Rectangle(100, 100, 350, 175), 1, "sig");
                appearance.setImage(Image.getInstance(IMG_FILE));
                appearance.setRenderingMode(PdfSignatureAppearance.RenderingMode.NAME_AND_DESCRIPTION);

                 if (finalize) {
                    appearance.setCertificationLevel(PdfSignatureAppearance.CERTIFIED_NO_CHANGES_ALLOWED);
                } else {
                    appearance.setCertificationLevel(PdfSignatureAppearance.NOT_CERTIFIED);
                }
                
                // Create the signature
                ExternalDigest digest = new BouncyCastleDigest();
                ExternalSignature signature
                        = new PrivateKeySignature(GetPKCS12.privateKey,DigestAlgorithms.SHA512, provider.getName());
                MakeSignature.signDetached(appearance, digest, signature, GetPKCS12.certificateChain,
                        null, null, null, 0, MakeSignature.CryptoStandard.CMS);
                
                stamper.close();

            } catch (Exception e) {
                throw new PDFSignerException(
                        java.util.ResourceBundle.getBundle("net/pflaeging/PortableSigner/i18n").getString("ErrorWhileSigningFile"),
                        true,
                        e.getLocalizedMessage());
            }

        } catch (KeyStoreException kse) {

            throw new PDFSignerException(
                    java.util.ResourceBundle.getBundle("net/pflaeging/PortableSigner/i18n").getString("ErrorCreatingKeystore"),
                    true, kse.getLocalizedMessage());

        }
    }

    /**
     * Creates a new instance of DoSignPDF
     */
    public void doSignPDFOld(String pdfInputFileName,
            String pdfOutputFileName,
            String pkcs12FileName,
            String password,
            Boolean signText,
            String signLanguage,
            String sigLogo,
            Boolean finalize,
            String sigComment,
            String signReason,
            String signLocation,
            Boolean noExtraPage,
            float verticalPos,
            float leftMargin,
            float rightMargin,
            Boolean signLastPage,
            byte[] ownerPassword) throws PDFSignerException {
        try {
            //System.out.println("-> DoSignPDF <-");
            //System.out.println("Eingabedatei: " + pdfInputFileName);
            //System.out.println("Ausgabedatei: " + pdfOutputFileName);
            //System.out.println("Signaturdatei: " + pkcs12FileName);
            //System.out.println("Signaturblock?: " + signText);
            //System.out.println("Sprache der Blocks: " + signLanguage);
            //System.out.println("Signaturlogo: " + sigLogo);
            System.err.println("Position V:" + verticalPos + " L:" + leftMargin + " R:" + rightMargin);
            Rectangle signatureBlock;

            java.security.Security.insertProviderAt(
                    new org.bouncycastle.jce.provider.BouncyCastleProvider(), 2);

            pkcs12 = new GetPKCS12(pkcs12FileName, password);

            PdfReader reader = null;
            try {
//                System.out.println("Password:" + ownerPassword.toString());
                if (ownerPassword == null) {
                    reader = new PdfReader(pdfInputFileName);
                } else {
                    reader = new PdfReader(pdfInputFileName, ownerPassword);
                }
            } catch (IOException e) {

              
                throw new PDFSignerException(
                        java.util.ResourceBundle.getBundle(
                                "net/pflaeging/PortableSigner/i18n").getString(
                                        "CouldNotBeOpened"),
                        true,
                        e.getLocalizedMessage());
            }
            FileOutputStream fout = null;
            try {
                fout = new FileOutputStream(pdfOutputFileName);
            } catch (FileNotFoundException e) {

               
                throw new PDFSignerException(
                        java.util.ResourceBundle.getBundle("net/pflaeging/PortableSigner/i18n").getString("CouldNotBeWritten"),
                        true,
                        e.getLocalizedMessage());

            }
            PdfStamper stp = null;
            try {
                Date datum = new Date(System.currentTimeMillis());

                int pages = reader.getNumberOfPages();

                Rectangle size = reader.getPageSize(pages);
                stp = PdfStamper.createSignature(reader, fout, '\0', null, true);
                HashMap<String, String> pdfInfo = reader.getInfo();
                // thanks to Markus Feisst
                String pdfInfoProducer = "";

                if (pdfInfo.get("Producer") != null) {
                    pdfInfoProducer = pdfInfo.get("Producer").toString();
                    pdfInfoProducer = pdfInfoProducer + " (signed with PortableSigner " + Version.release + ")";
                } else {
                    pdfInfoProducer = "Unknown Producer (signed with PortableSigner " + Version.release + ")";
                }
                pdfInfo.put("Producer", pdfInfoProducer);
                //System.err.print("++ Producer:" + pdfInfo.get("Producer").toString());
                stp.setMoreInfo(pdfInfo);

                // add info as xmp metadata
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                XmpWriter xmp = new XmpWriter(baos, pdfInfo);
                xmp.close();
                stp.setXmpMetadata(baos.toByteArray());
                // Create signature table
                if (signText) {
                    String greet, signator, datestr, ca, serial, special, note, urn, urnvalue;
                    int specialcount = 0;
                    int sigpage;
                    int rightMarginPT, leftMarginPT;
                    float verticalPositionPT;
                    ResourceBundle block = ResourceBundle.getBundle(
                            "net/pflaeging/PortableSigner/Signatureblock_" + signLanguage);
                    greet = block.getString("greeting");
                    signator = block.getString("signator");
                    datestr = block.getString("date");
                    ca = block.getString("issuer");
                    serial = block.getString("serial");
                    special = block.getString("special");
                    note = block.getString("note");
                    urn = block.getString("urn");
                    urnvalue = block.getString("urnvalue");

                    //sigcomment = block.getString(signLanguage + "-comment");
                    // upper y
                    float topy = size.getTop();
                    System.err.println("Top: " + topy * ptToCm);
                    // right x
                    float rightx = size.getRight();
                    System.err.println("Right: " + rightx * ptToCm);
                    if (!noExtraPage) {
                        sigpage = pages + 1;
                        stp.insertPage(sigpage, size);
                        // 30pt left, 30pt right, 20pt from top
                        rightMarginPT = 30;
                        leftMarginPT = 30;
                        verticalPositionPT = topy - 20;
                    } else {
                        if (signLastPage) {
                            sigpage = pages;
                        } else {
                            sigpage = 1;
                        }
                        System.err.println("Page: " + sigpage);
                        rightMarginPT = Math.round(rightMargin / ptToCm);
                        leftMarginPT = Math.round(leftMargin / ptToCm);
                        verticalPositionPT = topy - Math.round(verticalPos / ptToCm);
                    }
                    if (!GetPKCS12.atEgovOID.equals("")) {
                        specialcount = 1;
                    }
                    PdfContentByte content = stp.getOverContent(sigpage);

                    float[] cellsize = new float[2];
                    cellsize[0] = 100f;
                    // rightx = width of page
                    // 60 = 2x30 margins
                    // cellsize[0] = description row
                    // cellsize[1] = 0
                    // 70 = logo width
                    cellsize[1] = rightx - rightMarginPT - leftMarginPT - cellsize[0] - cellsize[1] - 70;

                    // Pagetable = Greeting, signatureblock, comment
                    // sigpagetable = outer table
                    //      consist: greetingcell, signatureblock , commentcell
                    PdfPTable signatureBlockCompleteTable = new PdfPTable(2);
                    PdfPTable signatureTextTable = new PdfPTable(2);
                    PdfPCell signatureBlockHeadingCell
                            = new PdfPCell(new Paragraph(
                                    new Chunk(greet,
                                            new Font(Font.FontFamily.HELVETICA, 12))));
                    signatureBlockHeadingCell.setPaddingBottom(5);
                    signatureBlockHeadingCell.setColspan(2);
                    signatureBlockHeadingCell.setBorderWidth(0f);
                    signatureBlockCompleteTable.addCell(signatureBlockHeadingCell);

                    // inner table start
                    // Line 1
                    signatureTextTable.addCell(
                            new Paragraph(
                                    new Chunk(signator, new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD))));
                    signatureTextTable.addCell(
                            new Paragraph(
                                    new Chunk(GetPKCS12.subject, new Font(Font.FontFamily.COURIER, 10))));
                    // Line 2
                    signatureTextTable.addCell(
                            new Paragraph(
                                    new Chunk(datestr, new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD))));
                    signatureTextTable.addCell(
                            new Paragraph(
                                    new Chunk(datum.toString(), new Font(Font.FontFamily.COURIER, 10))));
                    // Line 3
                    signatureTextTable.addCell(
                            new Paragraph(
                                    new Chunk(ca, new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD))));
                    signatureTextTable.addCell(
                            new Paragraph(
                                    new Chunk(GetPKCS12.issuer, new Font(Font.FontFamily.COURIER, 10))));
                    // Line 4
                    signatureTextTable.addCell(
                            new Paragraph(
                                    new Chunk(serial, new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD))));
                    signatureTextTable.addCell(
                            new Paragraph(
                                    new Chunk(GetPKCS12.serial.toString(), new Font(Font.FontFamily.COURIER, 10))));
                    // Line 5
                    if (specialcount == 1) {
                        signatureTextTable.addCell(
                                new Paragraph(
                                        new Chunk(special, new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD))));
                        signatureTextTable.addCell(
                                new Paragraph(
                                        new Chunk(GetPKCS12.atEgovOID, new Font(Font.FontFamily.COURIER, 10))));
                    }
                    signatureTextTable.addCell(
                            new Paragraph(
                                    new Chunk(urn, new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD))));
                    signatureTextTable.addCell(
                            new Paragraph(
                                    new Chunk(urnvalue, new Font(Font.FontFamily.COURIER, 10))));
                    signatureTextTable.setTotalWidth(cellsize);
                    System.err.println("signatureTextTable Width: " + cellsize[0] * ptToCm + " " + cellsize[1] * ptToCm);
                    // inner table end

                    signatureBlockCompleteTable.setHorizontalAlignment(PdfPTable.ALIGN_CENTER);
                    Image logo;
//                     System.out.println("Logo:" + sigLogo + ":");
                    if (sigLogo == null || "".equals(sigLogo)) {
                        logo = Image.getInstance(getClass().getResource(
                                "/net/pflaeging/PortableSigner/SignatureLogo.png"));
                    } else {
                        logo = Image.getInstance(sigLogo);
                    }

                    PdfPCell logocell = new PdfPCell();
                    logocell.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
                    logocell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
                    logocell.setImage(logo);
                    signatureBlockCompleteTable.addCell(logocell);
                    PdfPCell incell = new PdfPCell(signatureTextTable);
                    incell.setBorderWidth(0f);
                    signatureBlockCompleteTable.addCell(incell);
                    PdfPCell commentcell
                            = new PdfPCell(new Paragraph(
                                    new Chunk(sigComment,
                                            new Font(Font.FontFamily.HELVETICA, 10))));
                    PdfPCell notecell
                            = new PdfPCell(new Paragraph(
                                    new Chunk(note,
                                            new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD))));
                    //commentcell.setPaddingTop(10);
                    //commentcell.setColspan(2);
                    // commentcell.setBorderWidth(0f);
                    if (!sigComment.equals("")) {
                        signatureBlockCompleteTable.addCell(notecell);
                        signatureBlockCompleteTable.addCell(commentcell);
                    }
                    float[] cells = {70, cellsize[0] + cellsize[1]};
                    signatureBlockCompleteTable.setTotalWidth(cells);
                    System.err.println("signatureBlockCompleteTable Width: " + cells[0] * ptToCm + " " + cells[1] * ptToCm);
                    signatureBlockCompleteTable.writeSelectedRows(0, 4 + specialcount, leftMarginPT, verticalPositionPT, content);
                    System.err.println("signatureBlockCompleteTable Position " + 30 * ptToCm + " " + (topy - 20) * ptToCm);
                    signatureBlock = new Rectangle(30 + signatureBlockCompleteTable.getTotalWidth() - 20,
                            topy - 20 - 20,
                            30 + signatureBlockCompleteTable.getTotalWidth(),
                            topy - 20);
                } else {
                    signatureBlock = new Rectangle(0, 0, 0, 0); // fake definition
                }
                PdfSignatureAppearance sap = stp.getSignatureAppearance();
//                sap.setCrypto(GetPKCS12.privateKey, GetPKCS12.certificateChain, null,
//                        PdfSignatureAppearance.WINCER_SIGNED );
                PdfSignature dic = new PdfSignature(PdfName.ADOBE_PPKMS, PdfName.ETSI_CADES_DETACHED);
                //sap.setCrypto(GetPKCS12.privateKey, GetPKCS12.certificateChain, null, null);
                dic.setName("YES YES");
                dic.setReason(signReason);
                dic.setLocation(signLocation);
                sap.setCryptoDictionary(dic);
//                if (signText) {
//                    sap.setVisibleSignature(signatureBlock,
//                            pages + 1, "PortableSigner");
//                }
                if (finalize) {
                    sap.setCertificationLevel(PdfSignatureAppearance.CERTIFIED_NO_CHANGES_ALLOWED);
                } else {
                    sap.setCertificationLevel(PdfSignatureAppearance.NOT_CERTIFIED);
                }
                HashMap<PdfName, Integer> exc = new HashMap<PdfName, Integer>();
                //  exc.put(PdfName.CONTENTS, new Integer(sap. * 2 + 2));
                sap.preClose(exc);
                // stp.close();
                sap.close(dic);

              
            } catch (Exception e) {

               
                throw new PDFSignerException(
                        java.util.ResourceBundle.getBundle("net/pflaeging/PortableSigner/i18n").getString("ErrorWhileSigningFile"),
                        true,
                        e.getLocalizedMessage());

            }
        } catch (KeyStoreException kse) {

           
            throw new PDFSignerException(
                    java.util.ResourceBundle.getBundle("net/pflaeging/PortableSigner/i18n").getString("ErrorCreatingKeystore"),
                    true, kse.getLocalizedMessage());

        }
    }
}
