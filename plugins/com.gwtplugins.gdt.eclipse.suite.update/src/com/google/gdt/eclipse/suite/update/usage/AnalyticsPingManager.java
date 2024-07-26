/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.google.gdt.eclipse.suite.update.usage;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gdt.eclipse.suite.GdtPlugin;
import com.google.gdt.eclipse.suite.update.GdtExtPlugin;
import com.google.gdt.eclipse.suite.update.GdtExtPlugin.GwtMaxSdkVersionComputer;
import com.google.gdt.eclipse.suite.update.UpdateSiteURLGenerator;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.update.internal.configurator.VersionedIdentifier;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Provides methods that report plugin-specific events to Analytics. The events reported to
 * Analytics all have a category that is the name of the reporting plugin, an action that is the
 * name of one of the values of the enum {@link Action}, a label that varies with the event being
 * reported, and optionally a numeric value that varies with the event being reported.
 */
@SuppressWarnings("restriction") // VersionedIdentifier
public class AnalyticsPingManager implements PingManager {

  public enum Action {
    GWT_COMPILATION
  }

  private static final String ANALYTICS_COLLECTION_URL = "http://www.google-analytics.com/collect";
  private static final String GWT_ANALYTICS_ID = "UA-62291716-1";
  private static final String APPLICATION_NAME = "GWT Eclipse Plugin";

  // Fixed-value query parameters present in every ping, and their fixed values:
  private static final ImmutableMap<String, String> STANDARD_PARAMETERS =
      ImmutableMap.<String, String>builder()
          .put("v", "1") // Google Analytics Measurement Protocol version
          .put("tid", GWT_ANALYTICS_ID) // tracking ID
          .put("t", "event") // hit type
          .put("an", APPLICATION_NAME)
          .build();

  private final UpdateSiteURLGenerator urlGenerator;

  public AnalyticsPingManager(UpdateSiteURLGenerator urlGenerator) {
    this.urlGenerator = urlGenerator;
  }

  @Override
  public void sendCompilationPing() {
    sendPing(GdtExtPlugin.PLUGIN_ID, Action.GWT_COMPILATION, null, null);
  }

  private static void sendPing(
      String pluginName, Action action, @Nullable String label, @Nullable Integer value,
      CustomDimensionAssignment... customDimensions) {
    Map<String, String> parametersMap = Maps.newHashMap(STANDARD_PARAMETERS);
    String anonymizedClientId = GdtPlugin.getInstallationId();
    parametersMap.put("cid", anonymizedClientId);
    parametersMap.put("ec", pluginName); // category
    parametersMap.put("ea", action.toString());
    if (label != null) {
      parametersMap.put("el", label);
    }
    if (value != null) {
      parametersMap.put("ev", value.toString());
    }
    setCustomDimension(parametersMap, CustomDimensionName.ANONYMIZED_CLIENT_ID, anonymizedClientId);
    @SuppressWarnings("deprecation") // PluginVersionIdentifier.toString()
    String gwtVersion =
        new VersionedIdentifier(
            GdtExtPlugin.FEATURE_ID, GdtExtPlugin.FEATURE_VERSION.toString()).toString();
    setCustomDimension(parametersMap, CustomDimensionName.GWT_VERSION, gwtVersion);
    setCustomDimension(
        parametersMap, CustomDimensionName.ECLIPSE_VERSION, GdtPlugin.getEclipseVersion());

    if (Platform.getProduct() != null) {
      setCustomDimension(
          parametersMap, CustomDimensionName.ECLIPSE_PRODUCT_ID, Platform.getProduct().getId());
    } else {
      setCustomDimension(
          parametersMap, CustomDimensionName.ECLIPSE_PRODUCT_ID, "null");
    }

    setCustomDimension(parametersMap, CustomDimensionName.SDK_VERSIONS, getSdkVersions());
    for (CustomDimensionAssignment nameValuePair : customDimensions) {
      setCustomDimension(parametersMap, nameValuePair.getName(), nameValuePair.getValue());
    }
    sendPostRequest(parametersMap);
  }

  private static String getSdkVersions() {
    IJavaProject[] projects = GdtExtPlugin.getJavaProjects();
    String maxGwtSdkVersion = new GwtMaxSdkVersionComputer().computeMaxSdkVersion(projects);

    if (maxGwtSdkVersion == null || maxGwtSdkVersion.isEmpty()) {
      return "none";
    } else {
      return "GWT " + maxGwtSdkVersion;
    }
  }

  private static void setCustomDimension(
      Map<String, String> map, CustomDimensionName dimension, String value) {
    map.put("cd" + dimension.getIndex(), value);
  }

  private static void sendPostRequest(Map<String, String> parametersMap) {
    GdtExtPlugin.getLogger().logInfo("Sending POST request to Analytics: " + parametersMap);
    String parametersString = getParametersString(parametersMap);
    try {
      URL url = new URL(ANALYTICS_COLLECTION_URL);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setDoOutput(true);
      connection.setRequestMethod("POST");
      connection.setRequestProperty(
          "Content-Length", Integer.toString(parametersString.length()));
      GdtExtPlugin.getLogger().logInfo(
          "Analytics ping request parameters: " + parametersString);
      try {
        DataOutputStream out = new DataOutputStream(connection.getOutputStream());
        try {
          out.writeBytes(parametersString);
          out.flush();
        } finally {
          out.close();
        }
      } finally {
        logPostResponse(connection);
        connection.disconnect();
      }
    } catch (IOException e) {
      GdtExtPlugin.getLogger().logError(e, "Error trying to ping Analytics");
    }
  }

  private static String getParametersString(Map<String, String> parametersMap) {
    StringBuilder resultBuilder = new StringBuilder();
    boolean ampersandNeeded = false;
    for (Map.Entry<String, String> entry : parametersMap.entrySet()) {
      if (ampersandNeeded) {
        resultBuilder.append('&');
      } else {
        ampersandNeeded = true;
      }
      resultBuilder.append(entry.getKey());
      resultBuilder.append('=');
      resultBuilder.append(URLEncoder.encode(entry.getValue(), Charsets.UTF_8));
    }
    return resultBuilder.toString();
  }

  private static void logPostResponse(HttpURLConnection connection)  throws IOException {
    int responseCode = connection.getResponseCode();
    StringBuilder responseBuilder = new StringBuilder();
    responseBuilder.append("Analytics ping response: ");
    responseBuilder.append(responseCode);
    responseBuilder.append(": ");
    responseBuilder.append(connection.getResponseMessage());
    if (responseCode == 200) {
      responseBuilder.append('\n');
      BufferedReader responseContentReader =
          new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String inputLine = responseContentReader.readLine();
      while (inputLine != null) {
        responseBuilder.append(inputLine);
        inputLine = responseContentReader.readLine();
      }
      responseContentReader.close();
    }
    GdtExtPlugin.getLogger().logInfo(responseBuilder.toString());
  }

  // Each element of this enum corresponds to a custom dimensions defined in the Analytics web UI,
  // The constructor argument specifies the index of the custom dimension.
  private enum CustomDimensionName {
    ECLIPSE_PRODUCT_ID(1),
    ECLIPSE_VERSION(3),
    GWT_VERSION(4),
    SDK_VERSIONS(6),
    ANONYMIZED_CLIENT_ID(10);

    private int index;

    private CustomDimensionName(int index) {
      this.index = index;
    }

    public int getIndex() {
      return index;
    }
  }

  private static final class CustomDimensionAssignment {

    private final CustomDimensionName name;
    private final String value;

    public CustomDimensionAssignment(CustomDimensionName name, String value) {
      this.name = name;
      this.value = value;
    }

    public CustomDimensionName getName() {
      return name;
    }

    public String getValue() {
      return value;
    }
  }

}
