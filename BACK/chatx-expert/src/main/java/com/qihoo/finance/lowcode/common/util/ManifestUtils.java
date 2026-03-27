package com.qihoo.finance.lowcode.common.util;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * ManifestUtils
 *
 * @author fengjinfu-jk
 * date 2023/8/31
 * @version 1.0.0
 * @apiNote ManifestUtils
 */
public class ManifestUtils {
    private static Manifest manifestCache;
    public static final String VERSION = "Version";

    public static String getAttribute(String attribute) {
        Attributes mainAttributes = getMainAttributes();
        if (mainAttributes != null) {
            return mainAttributes.getValue(attribute);
        }

        return null;
    }

    public static Attributes getMainAttributes() {
        return Objects.nonNull(getJarManifest()) ? getJarManifest().getMainAttributes() : null;
    }

    public static Manifest getJarManifest() {
        if (Objects.nonNull(manifestCache)) {
            return manifestCache;
        }

        ArrayList<URL> resourceUrls = new ArrayList<>();
        Class<?> clazz = ManifestUtils.class;
        ClassLoader classLoader = clazz.getClassLoader();
        String sourceName = clazz.getName().replace('.', '/').concat(".class");
        try {
            Enumeration<URL> resources = classLoader.getResources(sourceName);
            while (resources.hasMoreElements()) {
                resourceUrls.add(resources.nextElement());
            }
            if (!resourceUrls.isEmpty()) {
                URL url = resourceUrls.get(0);
                URLConnection urlConnection = url.openConnection();
                if (urlConnection instanceof JarURLConnection) {
                    JarURLConnection jarURL = (JarURLConnection) urlConnection;
                    JarFile jarFile = jarURL.getJarFile();
                    Manifest manifest = jarFile.getManifest();
                    jarFile.close();
                    manifestCache = manifest;
                }
            }
        } catch (IOException e) {
            return manifestCache;
        }

        return manifestCache;
    }
}
