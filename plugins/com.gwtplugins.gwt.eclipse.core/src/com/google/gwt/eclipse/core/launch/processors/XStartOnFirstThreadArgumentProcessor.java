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
package com.google.gwt.eclipse.core.launch.processors;

import com.google.gdt.eclipse.core.launch.ILaunchConfigurationProcessor;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;
import com.google.gwt.eclipse.core.util.Util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import java.util.List;

/**
 * Processes the "-XstartOnFirstThread" argument.
 */
public class XStartOnFirstThreadArgumentProcessor implements
    ILaunchConfigurationProcessor {

  private static final String ARG_XSTART_ON_FIRST_THREAD = "-XstartOnFirstThread";
  private static final String ATTR_XSTART_ON_FIRST_THREAD =
      "org.eclipse.jdt.launching.ATTR_USE_START_ON_FIRST_THREAD";

  /**
   * Returns <code>true</code> if -XstartOnFirstThread needs to be added as a VM
   * argument.
   * 
   * @param javaProject if <code>null</code>
   */
  private static boolean needsStartOnFirstThreadHack(IJavaProject javaProject, 
      ILaunchConfiguration config)
      throws CoreException {

    if (!Util.isPlatformMac()) {
      // Non-Mac platforms don't need the hack
      return false;
    }

    GWTRuntime sdk = GWTRuntime.findSdkFor(javaProject);
    if (sdk == null || !sdk.validate().isOK()) {
      // We assume it does since the typical configuration (latest GWT SDK with
      // remote UI) requires it.

      // This also covers AppEngine-only projects, which do need the flag.

      GWTPluginLog.logWarning(String.format(
          "Could not check whether the launch configuration \"%1$s\" needs the -XstartOnFirstThread argument, assuming it does",
          config.getName()));
      return true;
    }

    /*
     * We're using a platform-independent version of GWT, which means we don't
     * need the hack. Except that the hack prevents us from ending in the OSX
     * dock, so we do want it.
     * 
     * If the user wants to use the Swing UI (by setting USE_REMOTE_UI = false)
     * or if the launch config is using a main type that will bring up the Swing
     * UI, then the hack is not needed.
     */
    boolean userWantsToUseSwingUi = RemoteUiArgumentProcessor.isUseRemoteUiEnvVarFalse(config);
    boolean usingOldMainTypes = GwtLaunchConfigurationProcessorUtilities.isGwtShell(config)
        || GwtLaunchConfigurationProcessorUtilities.isHostedMode(config);
    if (userWantsToUseSwingUi || usingOldMainTypes) {
      return false;
    } else {
      return true;
    }
  }

  public void update(ILaunchConfigurationWorkingCopy launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs)
      throws CoreException {
    
    // As of Eclipse Kepler(4.3), the use of -XStartOnFirstThread is a built-in launch config
    // attribute so it is not necessary to make it an explicit argument. This prevents issues when
    // sharing a launch config file across multiple OS platforms.
    Bundle bundle = Platform.getBundle("org.eclipse.platform");
    if (bundle != null) {
      Version bundleVersion = bundle.getVersion();
      if (bundleVersion.getMajor() == 4 && bundleVersion.getMinor() == 3) {
        updateEclipse43(launchConfig, javaProject, programArgs, vmArgs);
      } else {
        updateNonEclipse43(launchConfig, javaProject, programArgs, vmArgs);
      }
    }
  }

  public String validate(ILaunchConfiguration launchConfig, IJavaProject javaProject,
      List<String> programArgs, List<String> vmArgs) throws CoreException {
    return null;
  }

  private void updateEclipse43(ILaunchConfigurationWorkingCopy launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs)
      throws CoreException {

    boolean startOnFirstThread = launchConfig.getAttribute(ATTR_XSTART_ON_FIRST_THREAD, false);

    if (needsStartOnFirstThreadHack(javaProject, launchConfig)) {
      /*
       * If we're on a mac, we might need the -XstartOnFirstThread attribute set to true in order to work
       * around a UI threading issue with the Mac platform.
       * 
       * We need to do this whether we're running GWT by itself, GWT + GAE, or GAE by itself.
       */
      if (!startOnFirstThread) {
        launchConfig.setAttribute(ATTR_XSTART_ON_FIRST_THREAD, true);
        launchConfig.doSave();
      }
    } else if (startOnFirstThread) {
      launchConfig.removeAttribute(ATTR_XSTART_ON_FIRST_THREAD);
      launchConfig.doSave();
    }
  }

  private void updateNonEclipse43(ILaunchConfigurationWorkingCopy launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs)
      throws CoreException {

    int argIndex = vmArgs.indexOf(ARG_XSTART_ON_FIRST_THREAD);

    if (needsStartOnFirstThreadHack(javaProject, launchConfig)) {
      /*
       * If we're on a mac, we might need the -XstartOnFirstThread argument in order
       * to work around a UI threading issue with the Mac platform.
       *  
       * We need to do this whether we're running GWT by itself, GWT + GAE, or
       * GAE by itself.
       */
      if (argIndex == -1) {
        vmArgs.add(0, ARG_XSTART_ON_FIRST_THREAD);
      }
    } else if (argIndex >= 0) {
      vmArgs.remove(argIndex);
    }
  }
}
