/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.test.i18n;

import org.apache.commons.io.IOUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class BundleSynchronizedMatcher extends BaseMatcher<String> {

  public static final String L10N_PATH = "/org/sonar/l10n/";

  private String bundleName;
  private SortedMap<String, String> missingKeys;
  private SortedMap<String, String> additionalKeys;

  public boolean matches(Object arg0) {
    if (!(arg0 instanceof String)) {
      return false;
    }
    bundleName = (String) arg0;

    // Find the bundle that needs to be verified
    InputStream bundleInputStream = getBundleFileInputStream(bundleName);

    // Find the default bundle which the provided one should be compared to
    InputStream defaultBundleInputStream = getDefaultBundleFileInputStream(bundleName);

    // and now let's compare!
    try {
      // search for missing keys
      missingKeys = retrieveMissingTranslations(bundleInputStream, defaultBundleInputStream);

      // and now for additional keys
      bundleInputStream = getBundleFileInputStream(bundleName);
      defaultBundleInputStream = getDefaultBundleFileInputStream(bundleName);
      additionalKeys = retrieveMissingTranslations(defaultBundleInputStream, bundleInputStream);

      // And fail only if there are missing keys
      return missingKeys.isEmpty();
    } catch (IOException e) {
      fail("An error occured while reading the bundles: " + e.getMessage());
      return false;
    } finally {
      IOUtils.closeQuietly(bundleInputStream);
      IOUtils.closeQuietly(defaultBundleInputStream);
    }
  }

  public void describeTo(Description description) {
    // report file
    File dumpFile = new File("target/l10n/" + bundleName + ".report.txt");

    // prepare message
    StringBuilder details = prepareDetailsMessage(dumpFile);
    description.appendText(details.toString());

    // print report in target directory
    printReport(dumpFile, details.toString());
  }

  private StringBuilder prepareDetailsMessage(File dumpFile) {
    StringBuilder details = new StringBuilder("\n=======================\n'");
    details.append(bundleName);
    details.append("' is not up-to-date.");
    print("\n\n Missing translations are:", missingKeys, details);
    print("\n\nThe following translations do not exist in the reference bundle:", additionalKeys, details);
    details.append("\n\nSee report file located at: ");
    details.append(dumpFile.getAbsolutePath());
    details.append("\n=======================");
    return details;
  }

  private void print(String title, SortedMap<String, String> translations, StringBuilder to) {
    if (!translations.isEmpty()) {
      to.append(title);
      for (Map.Entry<String, String> entry : translations.entrySet()) {
        to.append("\n").append(entry.getKey()).append("=").append(entry.getValue());
      }
    }
  }

  private void printReport(File dumpFile, String details) {
    if (dumpFile.exists()) {
      dumpFile.delete();
    }
    dumpFile.getParentFile().mkdirs();
    FileWriter writer = null;
    try {
      writer = new FileWriter(dumpFile);
      writer.write(details);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to write the report to 'target/l10n/" + bundleName + ".report.txt'", e);
    } finally {
      IOUtils.closeQuietly(writer);
    }
  }

  protected static SortedMap<String, String> retrieveMissingTranslations(InputStream bundle, InputStream referenceBundle) throws IOException {
    SortedMap<String, String> missingKeys = new TreeMap();

    Properties bundleProps = loadProperties(bundle);
    Properties referenceProperties = loadProperties(referenceBundle);

    for (Map.Entry<Object, Object> entry : referenceProperties.entrySet()) {
      String key = (String) entry.getKey();
      if (!bundleProps.containsKey(key)) {
        missingKeys.put(key, (String) entry.getValue());
      }
    }

    return missingKeys;
  }

  protected static Properties loadProperties(InputStream inputStream) throws IOException {
    Properties props = new Properties();
    props.load(inputStream);
    return props;
  }

  protected static InputStream getBundleFileInputStream(String bundleName) {
    InputStream bundle = BundleSynchronizedMatcher.class.getResourceAsStream(L10N_PATH + bundleName);
    assertThat("File '" + bundleName + "' does not exist in '/org/sonar/l10n/'.", bundle, notNullValue());
    return bundle;
  }

  protected static InputStream getDefaultBundleFileInputStream(String bundleName) {
    String defaultBundleName = extractDefaultBundleName(bundleName);
    InputStream bundle = BundleSynchronizedMatcher.class.getResourceAsStream(L10N_PATH + defaultBundleName);
    assertThat("Default bundle '" + defaultBundleName + "' could not be found: add a dependency to the corresponding plugin in your POM.", bundle, notNullValue());
    return bundle;
  }

  protected static String extractDefaultBundleName(String bundleName) {
    int firstUnderScoreIndex = bundleName.indexOf('_');
    assertThat("The bundle '" + bundleName + "' is a default bundle (without locale), so it can't be compared.", firstUnderScoreIndex > 0,
      is(true));
    return bundleName.substring(0, firstUnderScoreIndex) + ".properties";
  }

}
