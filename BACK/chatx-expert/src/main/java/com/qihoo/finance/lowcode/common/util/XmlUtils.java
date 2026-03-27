package com.qihoo.finance.lowcode.common.util;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;

/**
 * XmlUtils
 *
 * @author fengjinfu-jk
 * date 2023/8/31
 * @version 1.0.0
 * @apiNote XmlUtils
 */
public class XmlUtils {

    public static Element loadUrlXml(String xmlUrl) {
        try {
            URL url = new URL(xmlUrl);
            //打开链接
            URLConnection conn = url.openConnection();
            //拿下网址的输入流
            InputStream is = conn.getInputStream();
            //2、创建一盒XML读取对象
            SAXReader sr = new SAXReader();
            //3、通过读取对象 读取xml数据吗，并返回文档对象
            Document doc = sr.read(is);
            //4、获取根节点
            return doc.getRootElement();
        } catch (Exception e) {
            return null;
        }
    }


    public static String pretty(String xmlString) {
        return pretty(xmlString, 4, true);
    }

    /**
     * 格式化xml
     *
     * @param xmlString         xml内容
     * @param indent            向前缩进多少空格
     * @param ignoreDeclaration 是否忽略描述
     * @return 格式化后的xml
     */
    public static String pretty(String xmlString, int indent, boolean ignoreDeclaration) {

        try {
            InputSource src = new InputSource(new StringReader(xmlString));
            org.w3c.dom.Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", indent);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, ignoreDeclaration ? "yes" : "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            Writer out = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(out));
            return out.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error occurs when pretty-printing xml:\n" + xmlString, e);
        }
    }
}
