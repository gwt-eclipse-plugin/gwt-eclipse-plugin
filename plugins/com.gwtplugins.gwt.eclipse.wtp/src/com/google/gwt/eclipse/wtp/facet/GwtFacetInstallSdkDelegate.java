/*******************************************************************************
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gwt.eclipse.wtp.facet;

import java.io.FileNotFoundException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorReference;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.osgi.service.prefs.BackingStoreException;

import com.google.gdt.eclipse.core.BuilderUtilities;
import com.google.gdt.eclipse.core.sdk.UpdateProjectSdkCommand.UpdateType;
import com.google.gwt.eclipse.core.nature.GWTNature;
import com.google.gwt.eclipse.core.properties.ui.GWTProjectPropertyPage;
import com.google.gwt.eclipse.core.runtime.GWTJarsRuntime;
import com.google.gwt.eclipse.core.runtime.GWTRuntime;
import com.google.gwt.eclipse.core.sdk.GWTUpdateProjectSdkCommand;
import com.google.gwt.eclipse.core.sdk.GWTUpdateWebInfFolderCommand;
import com.google.gwt.eclipse.wtp.GwtWtpPlugin;
import com.google.gwt.eclipse.wtp.facet.data.IGwtFacetConstants;

/**
 * Install the GWT Facet.
 * <p>
 * <ol>
 * <li>Standard Projects get a SDK container.</li>
 * <li>Maven projects will need no sdk.</li>
 * </ol>
 * </p>
 * Both project types will use the GWT editors and rebuild.
 */
public final class GwtFacetInstallSdkDelegate implements IDelegate, IGwtFacetConstants {

  private IProject project;
  private IDataModel dataModel;

  @Override
  public void execute(final IProject project, final IProjectFacetVersion version, final Object config,
      final IProgressMonitor monitor) throws CoreException {
    this.project = project;

    dataModel = (IDataModel) config;

    // Standard project GWT facet install
    if (!isMavenProject(dataModel)) {
      String message = "GwtFacetInstallSdkDelegate: installing standard classpath container.";
      GwtWtpPlugin.logMessage(message);
      installGwtFacetForStandardProject();
    }

    // Need to rebuild to get GWT errors to appear
    BuilderUtilities.scheduleRebuild(project);

    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        // Use the GWT editors
        reopenFilesWithGwtEditor();
      }
    });
    
    installGwtNatureForProject();
  }

  /**
   * Support the legacy GWT operations, actions... Be sure the nature is enabled.
   * TODO change out nature for facet
   */
  private void installGwtNatureForProject() {
    try {
      GWTNature.addNatureToProject(project);
    } catch (CoreException e) {
      GwtWtpPlugin.logError("GwtFacetInstallSdkDelegate.addFacetToProject(): Error setting GWT Nature.", e);
    }
  }

  private void reopenFilesWithGwtEditor() {
    // Get the list of Java editors opened on files in this project
    IEditorReference[] openEditors = GWTProjectPropertyPage.getOpenJavaEditors(project);
    if (openEditors.length > 0) {
      GWTProjectPropertyPage.reopenWithGWTJavaEditor(openEditors);
    }
  }

  /**
   * Configure a standard GWT classpath container for Facet.
   */
  private void installGwtFacetForStandardProject() {
    GWTJarsRuntime runtime = (GWTJarsRuntime) dataModel.getProperty(GWT_SDK);
    if (runtime == null) { // no sdks have been installed.
      // TODO add a warning dialog with link to preferences
      GwtWtpPlugin
          .logMessage("No GWT sdks have been added to the preferences. Fix this by adding a GWT SDK to Eclipse preferences.");
      return;
    }

    GwtWtpPlugin.logMessage("GwtFacetInstallSdkDelegate: selected runtime version=" + runtime.getVersion());

    try {
      addGwtSdkContainer(project, runtime);
    } catch (FileNotFoundException | CoreException | BackingStoreException e) {
      String m = "GwtFacetInstallSdkDelegate.installGwtFacetForStandardProject(): was not able to add sdk container to project.";
      GwtWtpPlugin.logError(m, e);
    }
  }

  private static boolean isMavenProject(IDataModel model) {
    if (!model.isProperty(GwtWtpPlugin.USE_MAVEN_DEPS_PROPERTY_NAME)) {
      return false;
    }
    if (!model.isPropertySet(GwtWtpPlugin.USE_MAVEN_DEPS_PROPERTY_NAME)) {
      return false;
    }
    return model.getBooleanProperty(GwtWtpPlugin.USE_MAVEN_DEPS_PROPERTY_NAME);
  }

  /**
   * Add a GWT SDK container for standard project only.
   */
  private void addGwtSdkContainer(IProject project, GWTRuntime newSdk) throws FileNotFoundException, CoreException,
      BackingStoreException {
    IJavaProject javaProject = JavaCore.create(project);

    GWTUpdateWebInfFolderCommand updateWebInfCommand = new GWTUpdateWebInfFolderCommand(javaProject, newSdk);

    // Maybe there is a SDK registered already
    GWTRuntime oldSdk = GWTRuntime.findSdkFor(javaProject);

    // Named container, not individual jars
    UpdateType updateType = UpdateType.NAMED_CONTAINER;

    GWTUpdateProjectSdkCommand command = new GWTUpdateProjectSdkCommand(javaProject, oldSdk, newSdk, updateType,
        updateWebInfCommand);
    command.execute();
  }

}
