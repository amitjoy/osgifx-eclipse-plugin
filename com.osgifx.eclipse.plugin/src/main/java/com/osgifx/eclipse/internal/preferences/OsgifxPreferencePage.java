/*******************************************************************************
 * Copyright 2026 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.eclipse.internal.preferences;

import static com.osgifx.eclipse.internal.preferences.OsgifxPreferenceConstants.AUTO_MANAGE_RUNTIME;
import static com.osgifx.eclipse.internal.preferences.OsgifxPreferenceConstants.CUSTOM_JAVA_PATH;
import static com.osgifx.eclipse.internal.preferences.OsgifxPreferenceConstants.OSGIFX_GAV;
import static com.osgifx.eclipse.internal.preferences.OsgifxPreferenceConstants.OSGIFX_LOCAL_JAR;
import static com.osgifx.eclipse.internal.preferences.OsgifxPreferenceConstants.USE_LOCAL_JAR;

import java.io.File;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.osgifx.eclipse.internal.Activator;
import com.osgifx.eclipse.internal.util.OsgifxWorkspaceUtil;

public final class OsgifxPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private BooleanFieldEditor autoManageEditor;
    private FileFieldEditor    customJavaEditor;
    private BooleanFieldEditor useLocalJarEditor;
    private FileFieldEditor    localJarEditor;
    private StringFieldEditor  gavEditor;

    public OsgifxPreferencePage() {
        super(GRID);
        setPreferenceStore(Activator.getInstance().getPreferenceStore());
        setDescription("OSGi.fx Launcher Runtime Configuration");
    }

    @Override
    public void createFieldEditors() {
        final var parent = getFieldEditorParent();

        autoManageEditor = new BooleanFieldEditor(AUTO_MANAGE_RUNTIME, "Auto-manage Azul Zulu FX 25 (Recommended)",
                                                  parent);
        addField(autoManageEditor);

        customJavaEditor = new FileFieldEditor(CUSTOM_JAVA_PATH, "Custom Java 25 Executable:", parent);
        customJavaEditor.setFileExtensions(new String[] { "*.exe", "*" });
        addField(customJavaEditor);

        final var spacer = new Label(parent, SWT.NONE);
        spacer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

        useLocalJarEditor = new BooleanFieldEditor(USE_LOCAL_JAR, "Use local OSGi.fx JAR (Overwrites GAV)", parent);
        addField(useLocalJarEditor);

        localJarEditor = new FileFieldEditor(OSGIFX_LOCAL_JAR, "Local OSGi.fx JAR Path:", parent);
        localJarEditor.setFileExtensions(new String[] { "*.jar" });
        addField(localJarEditor);

        gavEditor = new StringFieldEditor(OSGIFX_GAV, "Maven OSGi.fx Version (GAV):", parent);
        gavEditor.setEmptyStringAllowed(false);
        addField(gavEditor);

        addCacheManagementSection(parent);
    }

    private void addCacheManagementSection(final Composite parent) {
        final var separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

        final var clearCacheButton = new Button(parent, SWT.PUSH);
        clearCacheButton.setText("Clear Downloaded Runtime Cache");
        clearCacheButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 3, 1));

        clearCacheButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                clearCache();
            }
        });
    }

    private void clearCache() {
        final var stateLocation = OsgifxWorkspaceUtil.getStateLocation();
        final var cacheDir      = new File(stateLocation, "zulu-fx-25");

        if (cacheDir.exists()) {
            deleteDirectory(cacheDir);
            MessageDialog.openInformation(getShell(), "Cache Cleared",
                    "The downloaded runtime cache has been cleared.\n"
                            + "It will be re-downloaded on the next launch.");
        } else {
            MessageDialog.openInformation(getShell(), "No Cache Found", "No downloaded runtime cache exists.");
        }
    }

    private void deleteDirectory(final File directory) {
        final var files = directory.listFiles();
        if (files != null) {
            for (final var file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    @Override
    protected void initialize() {
        super.initialize();
        updateEnablement();
    }

    @Override
    protected void checkState() {
        super.checkState();
        updateEnablement();
    }

    @Override
    public void propertyChange(org.eclipse.jface.util.PropertyChangeEvent event) {
        super.propertyChange(event);
        if (FieldEditor.VALUE.equals(event.getProperty())) {
            updateEnablement();
        }
    }

    private void updateEnablement() {
        if (autoManageEditor == null || customJavaEditor == null || useLocalJarEditor == null || localJarEditor == null
                || gavEditor == null) {
            return;
        }
        final var autoManage = autoManageEditor.getBooleanValue();
        customJavaEditor.setEnabled(!autoManage, getFieldEditorParent());

        final var useLocal = useLocalJarEditor.getBooleanValue();
        localJarEditor.setEnabled(useLocal, getFieldEditorParent());
        gavEditor.setEnabled(!useLocal, getFieldEditorParent());
    }

    @Override
    public void init(final IWorkbench workbench) {
        // No-op
    }
}
