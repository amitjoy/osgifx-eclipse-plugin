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
package com.osgifx.eclipse.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import com.osgifx.eclipse.ui.dialogs.ConnectionManagerDialog;

public final class ToolbarLaunchHandler extends AbstractHandler {

    @Override
    public Object execute(final ExecutionEvent event) throws ExecutionException {
        try {
            // Ensure bundle is activated by accessing the activator
            final BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
            if (bundleContext == null) {
                throw new IllegalStateException("Bundle not activated. Please restart Eclipse.");
            }

            final Shell                   shell  = HandlerUtil.getActiveShell(event);
            final ConnectionManagerDialog dialog = new ConnectionManagerDialog(shell);
            dialog.open();
        } catch (final Exception e) {
            final Shell shell = HandlerUtil.getActiveShell(event);
            MessageDialog.openError(shell, "OSGi.fx Error", "Failed to open dialog: " + e.getMessage());
            throw new ExecutionException("Failed to open OSGi.fx dialog", e);
        }
        return null;
    }
}
