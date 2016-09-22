/*******************************************************************************
 * Copyright 2014 Google Inc. All Rights Reserved.
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
package com.google.gwt.eclipse.core.test.swtbot;


/**
 * Test GWT super dev mode debug configurations using a Maven project.
 *
 * Overrides are for easy running for local testing.
 */
public class DebugConfigurationSuperDevModeAsMavenTest extends DebugConfigurationSuperDevModeTest {

  @Override
  public void testShortcutUsingDefaults() {
    super.testShortcutUsingDefaults();
  }

  /**
   * Instead create a Maven project, not a standard package project
   */
  @Override
  protected void givenProjectIsCreated() {
    givenMavenGwtProjectIsCreated(PROJECT_NAME);
  }

}
