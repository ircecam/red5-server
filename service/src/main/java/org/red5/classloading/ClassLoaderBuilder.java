/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2023 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.classloading;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;

/**
 * Class used to get the Servlet Class loader. The class loader returned is a child first class loader.
 *
 * <br>
 * <i>This class is based on original code from the XINS project, by Anthony Goubard (anthony.goubard@japplis.com)</i>
 *
 * @author Paul Gregoire (mondain@gmail.com)
 */
public final class ClassLoaderBuilder {

    /*
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6500212 http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6516909 http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4976356
     */

    /**
     * Load the Servlet code from the WAR file and use the current classpath for the libraries.
     */
    public static final int USE_CLASSPATH_LIB = 1;

    /**
     * Load the servlet code from the WAR file and try to find the libraries in the common red5 lib directory.
     */
    public static final int USE_RED5_LIB = 2;

    /**
     * Load the servlet code and the libraries from the WAR file. This may take some time as the libraries need to be extracted from the WAR
     * file.
     */
    public static final int USE_WAR_LIB = 3;

    /**
     * Default build uses Red5 common lib without a parent classloader.
     *
     * @return the class loader
     */
    public static ClassLoader build() {
        return ClassLoaderBuilder.build(null, USE_RED5_LIB, null);
    }

    /**
     * Gets a class loader based on mode.
     *
     * @param path
     *            the directory or file containing classes
     * @param mode
     *            the mode in which the servlet should be loaded. The possible values are
     *
     *            <pre>
     * USE_CURRENT_CLASSPATH, USE_CLASSPATH_LIB, USE_WAR_LIB
     * </pre>
     * @param parent
     *            the parent class loader or null if you want the current threads class loader
     * @return the Class loader to use to load the required class(es)
     */
    public static ClassLoader build(Path path, int mode, ClassLoader parent) {
        final Set<URL> urlList = new HashSet<>();
        // the class loader to return
        ClassLoader loader = null;
        // urls to load resources / classes from
        URL[] urls = null;
        if (mode == USE_RED5_LIB) {
            // get red5 home
            // look for red5 home as a system property
            Path homeDir = null;
            String home = System.getProperty("red5.root", System.getenv("RED5_HOME"));
            // if home is null check environmental
            if (home != null) {
                homeDir = Paths.get(home);
            } else {
                // if home is null or equal to "current" directory, look it up via this classes loader
                String classLocation = ClassLoaderBuilder.class.getProtectionDomain().getCodeSource().getLocation().toString();
                // System.out.printf("Classloader location: %s\n", classLocation);
                // snip off anything beyond the last slash
                home = classLocation.substring(0, classLocation.lastIndexOf('/'));
                homeDir = Paths.get(home);
            }
            try {
                // add red5.jar to the classpath
                Path red5jar = homeDir.resolve("red5-server.jar");
                if (!Files.exists(red5jar)) {
                    System.out.println("Red5 server jar was not found, using fallback");
                    red5jar = homeDir.resolve("red5.jar");
                } else {
                    System.out.println("Red5 server jar was found");
                }
                urlList.add(red5jar.toUri().toURL());
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
            }
            System.out.printf("URL list: %s\n", urlList);
            // get red5 lib system property, if not found build it
            Path libDir = null;
            String lib = System.getProperty("red5.lib_root");
            if (lib != null) {
                libDir = Paths.get(lib);
            } else {
                libDir = homeDir.resolve("lib");
            }
            try {
                // add lib dir
                //urlList.add(libDir.toUri().toURL());
                // get all the lib jars
                Files.walkFileTree(libDir, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        //System.out.printf("Lib file: %s%n", file.toAbsolutePath());
                        if (file.toFile().getName().endsWith(".jar")) {
                            try {
                                urlList.add(file.toUri().toURL());
                            } catch (MalformedURLException e) {
                                System.err.printf("Exception %s\n", e);
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (Exception e) {
                System.err.printf("Exception %s\n", e);
            }
            // look over the libraries and remove the old versions
            scrubURLList(urlList);
            // get config dir
            Path confDir = null;
            String conf = System.getProperty("red5.config_root");
            if (conf != null) {
                confDir = Paths.get(conf);
            } else {
                confDir = homeDir.resolve("conf");
            }
            // add config dir
            try {
                urlList.add(confDir.toUri().toURL());
            } catch (MalformedURLException e) {
                System.err.printf("Exception %s\n", e);
            }
            // get red5 plugins system property, if not found build it
            String pluginsPath = System.getProperty("red5.plugins_root");
            if (pluginsPath == null) {
                // construct the plugins path
                pluginsPath = home + "/plugins";
                // update the property
                System.setProperty("red5.plugins_root", pluginsPath);
            }
            try {
                // create the directory if it doesnt exist
                Path pluginsDir = Files.createDirectories(Paths.get(pluginsPath));
                // add the plugin directory to the path so that configs will be resolved and not have to be copied to conf
                urlList.add(pluginsDir.toUri().toURL());
                // get all the plugin jars
                Files.walkFileTree(pluginsDir, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (file.toFile().getName().endsWith(".jar")) {
                            try {
                                urlList.add(file.toUri().toURL());
                            } catch (MalformedURLException e) {
                                System.err.printf("Exception %s\n", e);
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (Exception e) {
                System.err.printf("Exception %s\n", e);
            }
            // create the url array that the classloader wants
            urls = urlList.toArray(new URL[0]);
            //System.out.printf("Selected libraries: (%s items)\n", urls.length);
            //for (URL url : urls) {
            //    System.out.println(url);
            //}
            //System.out.println();
            // instance a url classloader using the selected jars
            if (parent == null) {
                loader = new URLClassLoader(urls);
            } else {
                loader = new URLClassLoader(urls, parent);
            }
        } else {
            List<String> standardLibs = new ArrayList<String>(7);
            if (path != null) {
                try {
                    urlList.add(path.toUri().toURL());
                    URL classesURL = new URL("jar:file:" + path.toFile().getAbsolutePath().replace(File.separatorChar, '/') + "!/WEB-INF/classes/");
                    urlList.add(classesURL);
                } catch (MalformedURLException e1) {
                    e1.printStackTrace();
                }
            }
            if (mode == USE_CLASSPATH_LIB) {
                String classPath = System.getProperty("java.class.path");
                StringTokenizer stClassPath = new StringTokenizer(classPath, File.pathSeparator);
                while (stClassPath.hasMoreTokens()) {
                    String nextPath = stClassPath.nextToken();
                    if (nextPath.toLowerCase().endsWith(".jar")) {
                        standardLibs.add(nextPath.substring(nextPath.lastIndexOf(File.separatorChar) + 1));
                    }
                    try {
                        urlList.add(Paths.get(nextPath).toUri().toURL());
                    } catch (MalformedURLException e) {
                        System.err.printf("Exception %s\n", e);
                    }
                }
            }
            if (mode == USE_WAR_LIB) {
                if (path != null && path.toFile().isDirectory()) {
                    Path libDir = path.resolve("WEB-INF").resolve("lib");
                    try {
                        Files.walkFileTree(libDir, new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                if (file.toFile().getName().endsWith(".jar")) {
                                    try {
                                        urlList.add(file.toUri().toURL());
                                    } catch (MalformedURLException e) {
                                        System.err.printf("Exception %s\n", e);
                                    }
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (IOException e) {
                        System.err.printf("Exception %s\n", e);
                    }
                } else {
                    try (InputStream is = Files.newInputStream(path); JarInputStream jarStream = new JarInputStream(is)) {
                        JarEntry entry = jarStream.getNextJarEntry();
                        while (entry != null) {
                            String entryName = entry.getName();
                            if (entryName.startsWith("WEB-INF/lib/") && entryName.endsWith(".jar") && !standardLibs.contains(entryName.substring(12))) {
                                Path tempJarFile = unpack(jarStream, entryName);
                                urlList.add(tempJarFile.toUri().toURL());
                            }
                            entry = jarStream.getNextJarEntry();
                        }
                        jarStream.close();
                    } catch (IOException e) {
                        System.err.printf("Exception %s\n", e);
                    }
                }
            }
            urls = urlList.toArray(new URL[0]);
            loader = new ChildFirstClassLoader(urls, parent);
        }
        Thread.currentThread().setContextClassLoader(loader);
        // loop thru all the current urls
        // System.out.printf("Classpath for %s:\n", loader);
        // for (URL url : urls) {
        // System.out.println(url.toExternalForm());
        // }
        return loader;
    }

    /**
     * Unpack the specified entry from the JAR file.
     *
     * @param jarStream
     *            The input stream of the JAR file positioned at the entry
     * @param entryName
     *            The name of the entry to extract
     * @return The extracted file. The created file is a temporary file in the temporary directory
     * @throws IOException
     *             if the JAR file cannot be read or is incorrect
     */
    private static Path unpack(JarInputStream jarStream, String entryName) throws IOException {
        String libName = entryName.substring(entryName.lastIndexOf('/') + 1, entryName.length() - 4);
        Path tempJarFile = Files.createTempFile("tmp_" + libName, ".jar");
        try (OutputStream os = Files.newOutputStream(tempJarFile)) {
            // Transfer bytes from the JAR file to the output file
            byte[] buf = new byte[1024];
            int len;
            while ((len = jarStream.read(buf)) > 0) {
                os.write(buf, 0, len);
            }
        }
        return tempJarFile;
    }

    // Java
    /**
     * Removes older versions of libraries from a given list based on their version numbers.
     *
     * @param list Collection of URLs to clean up
     */
    private static final void scrubURLList(Collection<URL> list) {
        Set<URL> removalList = new HashSet<>();
        URL[] urls = list.toArray(new URL[0]);

        for (URL top : urls) {
            if (shouldSkip(top, removalList))
                continue;
            String topName = parseUrl(top);

            for (URL check : urls) {
                if (isRemovable(top, check, topName, removalList)) {
                    removalList.add(check);
                }
            }
        }
        list.removeAll(removalList);
    }

    /**
     * Checks if a URL should be skipped based on specific criteria.
     *
     * @param url         URL to check.
     * @param removalList Set of URLs already marked for removal.
     * @return True if the URL should be skipped, false otherwise.
     */
    private static boolean shouldSkip(URL url, Set<URL> removalList) {
        String name = parseUrl(url);
        return removalList.contains(url) || name.isEmpty() || name.startsWith("red5") || name.startsWith("grobo") || name.startsWith("junit") || name.startsWith("ivy") || name.contains("javadoc") || name.contains("sources") || name.endsWith("-");
    }

    /**
     * Determines if an element can be removed based on version comparison.
     *
     * @param top         URL of the primary element.
     * @param check       URL of the secondary element.
     * @param topName     Name of the primary element.
     * @param removalList Set of URLs already marked for removal.
     * @return True if the element can be removed, false otherwise.
     */
    private static boolean isRemovable(URL top, URL check, String topName, Set<URL> removalList) {
        if (removalList.contains(check))
            return false;

        String checkName = parseUrl(check);
        if (checkName.equals(topName) || checkName.endsWith("-") || !isSameLibraryFamily(topName, checkName)) {
            return false;
        }

        int topVersion = extractVersion(topName);
        int checkVersion = extractVersion(checkName);

        if (topVersion >= checkVersion) {
            removalList.add(check);
        } else {
            removalList.add(top);
        }
        return true;
    }

    /**
     * Checks if two libraries belong to the same family.
     *
     * @param topName   Name of the primary library.
     * @param checkName Name of the secondary library.
     * @return True if both libraries belong to the same family, false otherwise.
     */
    private static boolean isSameLibraryFamily(String topName, String checkName) {
        String topPrefix = getPrefix(topName);
        String checkPrefix = getPrefix(checkName);

        return topPrefix.equals(checkPrefix);
    }

    /**
     * Extracts the prefix from the name of a library.
     *
     * @param name Library name.
     * @return Prefix extracted from the library name.
     */
    private static String getPrefix(String name) {
        int dashIndex = name.indexOf('-');
        return name.substring(0, dashIndex != -1 ? dashIndex : 3);
    }

    /**
     * Extracts the version from a library name by removing non-numeric characters.
     *
     * @param name Library name.
     * @return Extracted version number as an integer.
     */
    private static int extractVersion(String name) {
        Pattern punct = Pattern.compile("\\p{Punct}");
        try {
            String[] versionParts = punct.split(name);
            return Integer.parseInt(versionParts[0]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Parses url and returns the jar filename stripped of the ending .jar
     *
     * @param url
     * @return
     */
    private static String parseUrl(URL url) {
        String external = url.toExternalForm().toLowerCase();
        //System.out.printf("parseUrl %s%n", external);
        // get everything after the last slash
        String[] parts = external.split("/");
        // last part
        String libName = parts[parts.length - 1];
        // strip .jar
        libName = libName.substring(0, libName.length() - 4);
        return libName;
    }

    private static String deleteAny(String str, String removalChars) {
        StringBuilder sb = new StringBuilder(str);
        // System.out.println("Before alpha delete: " + sb.toString());
        String[] chars = removalChars.split("");
        // System.out.println("Chars length: " + chars.length);
        for (String c : chars) {
            int index = -1;
            while ((index = sb.indexOf(c)) > 0) {
                sb.deleteCharAt(index);
            }
        }
        // System.out.println("After alpha delete: " + sb.toString());
        return sb.toString();
    }
}
