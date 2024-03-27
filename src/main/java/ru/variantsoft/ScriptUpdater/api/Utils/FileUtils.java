package ru.variantsoft.ScriptUpdater.api.Utils;

/*
 * FileUtils.java
 * Юнит скриптера в котором находятся вспомогательные методы длря работы системы.
 * Предназначен для получения полного пути к файлу БД.
 * @author "Variant Soft Co." 2018
 */

import ru.variantsoft.ScriptUpdater.api.base.ReadProps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;


public class FileUtils {

    /*--implementation---------------------------*/
    // Поиск пути к файлу
    public static File findFileOnClassPath(final String fileName) {

        final String classpath = System.getProperty("java.class.path");
        final String pathSeparator = System.getProperty("path.separator");
        final StringTokenizer tokenizer = new StringTokenizer(classpath, pathSeparator);

        while (tokenizer.hasMoreTokens()) {
            final String pathElement = tokenizer.nextToken();
            final File directoryOrJar = new File(pathElement);
            final File absoluteDirectoryOrJar = directoryOrJar.getAbsoluteFile();

            if (absoluteDirectoryOrJar.isFile()) {
                final File target = new File(absoluteDirectoryOrJar.getParent(), fileName);
                if (target.exists())
                    return target;
            } else {
                final File target = new File(directoryOrJar, fileName);
                if (target.exists())
                    return target;
            }
        }
        return null;
    }

    public static String getFilePathInRoot(String strFileName) {
        String iStr = new File(".").getAbsolutePath();
        iStr = iStr.substring(0, iStr.length() - 1);
        return String.format("%s%s", iStr, strFileName);
    }

    public static void GetTerm(final String fname) throws IOException {
        BufferedReader bufferedReader = null;
        FileReader fr = null;

        try {
            fr = new FileReader(fname);
            bufferedReader = new BufferedReader(fr);

            String line;
            while ((line = bufferedReader.readLine()) != null)
                if (line.toUpperCase().startsWith("SET TERM")) {
                    if (ReadProps.Term.isEmpty())
                        setTerm(line);
                    else
                        break;
                }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            assert fr != null;
            fr.close();
            assert bufferedReader != null;
            bufferedReader.close();
        }
    }

    private static void setTerm(String AValue) {
        if (AValue.toUpperCase().startsWith("SET TERM") && AValue.toUpperCase().endsWith(";") && ReadProps.Term.isEmpty()) {
            ReadProps.Term = AValue.substring(AValue.indexOf(' ', AValue.indexOf(' ') + 1), AValue.indexOf(";") - 1).trim();
        }
    }

    public static String getUpdateFileName(String filePath) {
        if (filePath == null || filePath.length() == 0)
            return "";
        filePath = filePath.replaceAll("[/\\\\]+", "/");
        int len = filePath.length(),
                upCount = 0;
        while (len > 0) {
            //remove trailing separator
            if (filePath.charAt(len - 1) == '/') {
                len--;
                if (len == 0)
                    return "";
            }
            int lastInd = filePath.lastIndexOf('/', len - 1);
            String fileName = filePath.substring(lastInd + 1, len);
            switch (fileName) {
                case ".":
                    len--;
                    break;
                case "..":
                    len -= 2;
                    upCount++;
                    break;
                default:
                    if (upCount == 0)
                        return fileName;
                    upCount--;
                    len -= fileName.length();
                    break;
            }
        }
        return "";
    }
}
