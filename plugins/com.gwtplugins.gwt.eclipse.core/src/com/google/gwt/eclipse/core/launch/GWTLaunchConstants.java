/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
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
package com.google.gwt.eclipse.core.launch;

import com.google.gwt.eclipse.core.GWTPlugin;

import java.util.Arrays;
import java.util.List;

// TODO: would this be better as an enum?

/**
 * Defines custom GWT launch configuration settings.
 */
public final class GWTLaunchConstants {

  /**
   * Defines defaults for GWT launch configuration settings.
   */
  public static final class Defaults {

    public static final String LOG_LEVEL = LOG_LEVELS[2]; // INFO

    public static final boolean NOT_HEADLESS = false;

    public static final String OBFUSCATION = OUTPUT_STYLES[0]; // OBFUSCATED

    public static final String OUT_DIR_TEST = "www-test";

    public static final String WEB_MODE = "false";

    private Defaults() {
      // Not instantiable
    }
  }

  public static final String ATTR_LOG_LEVEL = GWTPlugin.PLUGIN_ID + ".LOG_LEVEL";

  public static final String ATTR_NOT_HEADLESS = GWTPlugin.PLUGIN_ID + ".NOT_HEADLESS";

  public static final String ATTR_OBFUSCATION = GWTPlugin.PLUGIN_ID + ".STYLE";

  public static final String ATTR_OUT_DIR = GWTPlugin.PLUGIN_ID + ".OUT_DIR";

  public static final String ATTR_STANDARDS_MODE = GWTPlugin.PLUGIN_ID + ".STANDARDS_MODE";

  public static final String ATTR_WEB_MODE = GWTPlugin.PLUGIN_ID + ".WEB_MODE";

  public static final String[] LOG_LEVELS = {"ERROR", "WARN", "INFO", "TRACE", "DEBUG", "SPAM",
      "ALL"};

  public static final String[] OUTPUT_STYLES = {"OBFUSCATED", "PRETTY", "DETAILED"};

  /**
   * Versions to use the embedded super dev mode legacy launcher with, GWT 2.5.0 to < 2.7. This will
   * prevent it from being added in older and newer sdks.
   */
  public static final List<String> SUPERDEVMODE_LAUNCH_LEGACY_VERSIONS = Arrays.asList("2.6.1",
      "2.6.1-rc1", "2.6.1-rc2", "2.6.0", "2.6.0-rc1", "2.6.0-rc2", "2.6.0-rc3", "2.6.0-rc4",
      "2.5.1", "2.5.1-rc1", "2.5.0", "2.5.0-rc1", "2.5.0-rc2");

  public static final String SUPERDEVMODE_LEGACY_LAUNCHER_JAR = "superdevmode-launcher-legacy.jar";

  public static final String SUPERDEVMODE_LINKER_NAME = "xsiframe";

  public static final String SUPERDEVMODE_LAUNCH_ID = GWTPlugin.PLUGIN_ID  + ".launch.id";

  private GWTLaunchConstants() {
    // Not instantiable
  }

}
