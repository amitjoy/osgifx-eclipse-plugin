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
package com.osgifx.eclipse.ui.dialogs;

import static com.osgifx.eclipse.internal.preferences.OsgifxPreferenceConstants.OSGIFX_GAV;
import static com.osgifx.eclipse.internal.preferences.OsgifxPreferenceConstants.OSGIFX_LOCAL_JAR;
import static com.osgifx.eclipse.internal.preferences.OsgifxPreferenceConstants.USE_LOCAL_JAR;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.osgifx.eclipse.internal.downloader.AzulZuluDownloader;
import com.osgifx.eclipse.internal.downloader.RunOsgiFxDownloader;
import com.osgifx.eclipse.internal.launcher.OsgifxProcessLauncher;
import com.osgifx.eclipse.internal.storage.ConnectionProfile;
import com.osgifx.eclipse.internal.storage.ConnectionProfileStore;
import com.osgifx.eclipse.internal.storage.VolatileConfigWriter;
import com.osgifx.eclipse.internal.util.OsgifxWorkspaceUtil;

public final class ConnectionManagerDialog extends TitleAreaDialog {

    private static final int MARGIN      = 15;
    private static final int SPACING     = 10;
    private static final int LABEL_WIDTH = 130;

    private static final Set<String> connectingProfiles = ConcurrentHashMap.newKeySet();

    private ConnectionProfileStore  profileStore;
    private VolatileConfigWriter    configWriter;
    private List<ConnectionProfile> profiles;
    private ConnectionProfile       selectedProfile;

    // UI Components
    private TableViewer profileList;
    private Composite   rightPanel;
    private StackLayout detailStackLayout;
    private Composite   emptyComposite;
    private Composite   formComposite;
    private ToolItem    addItem;
    private ToolItem    duplicateItem;
    private ToolItem    removeItem;
    private Text        nameField;
    private Label       statusLabel;
    private Label       lastConnectedLabel;

    // SOCKET fields
    private Text    hostField;
    private Spinner portSpinner;
    private Spinner timeoutSpinner;
    private Text    passwordField;
    private Text    truststorePathField;
    private Text    truststorePasswordField;

    // MQTT fields
    private Text    serverField;
    private Spinner mqttPortSpinner;
    private Spinner mqttTimeoutSpinner;
    private Text    clientIdField;
    private Text    usernameField;
    private Text    mqttPasswordField;
    private Text    pubTopicField;
    private Text    subTopicField;
    private Text    lwtTopicField;

    // OAuth2 fields
    private Text authServerUrlField;
    private Text oauthClientIdField;
    private Text clientSecretField;
    private Text audienceField;
    private Text scopeField;

    // Tab folder for protocol switching
    private CTabFolder        tabFolder;
    private CTabItem          socketTab;
    private CTabItem          mqttTab;
    private ConnectionProfile newlyCreatedProfile;

    // Validation
    private final Map<Control, ControlDecoration> decorations = new HashMap<>();
    private Button                                connectButton;
    private Button                                saveButton;
    private ResourceManager                       resourceManager;
    private boolean                               isDirty;
    private boolean                               ignoreModifyListeners;
    private boolean                               ignoreSelectionChange;
    private ConnectionProfile                     snapshotProfile;

    // Fonts and colors
    private Font  headerFont;
    private Font  boldFont;
    private Color successColor;
    private Color errorColor;
    private Color neutralColor;

    public ConnectionManagerDialog(final Shell parentShell) {
        super(parentShell);
    }

    @Override
    public void create() {
        super.create();
        initializeFonts();
        initializeColors();

        getShell().setText("OSGi.fx Connection Manager");
        setTitle("OSGi.fx Connection Manager");
        setMessage("Create and manage connection profiles for OSGi.fx diagnostic tool");

        // Phase 1: Asset Integration & Header Branding
        final ImageDescriptor logoDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin("com.osgifx.eclipse.plugin",
                "icons/logo_64.png");
        setTitleImage(resourceManager.createImage(logoDescriptor));

        getShell().setSize(800, 700);
        initializeStores();
        loadProfiles();
        updatePanelState();
    }

    private void initializeFonts() {
        final Display    display    = Display.getCurrent();
        final Font       systemFont = JFaceResources.getDefaultFont();
        final FontData[] fontData   = systemFont.getFontData();

        // Header font (larger, bold)
        final FontData headerFontData = new FontData(fontData[0].getName(), fontData[0].getHeight() + 2, SWT.BOLD);
        headerFont = new Font(display, headerFontData);

        // Bold font
        final FontData boldFontData = new FontData(fontData[0].getName(), fontData[0].getHeight(), SWT.BOLD);
        boldFont = new Font(display, boldFontData);
    }

    private void initializeColors() {
        final Display display = Display.getCurrent();
        successColor = new Color(display, 46, 125, 50); // Green
        errorColor   = new Color(display, 198, 40, 40); // Red
        neutralColor = new Color(display, 97, 97, 97);  // Gray
    }

    private void initializeStores() {
        final File stateLocation = OsgifxWorkspaceUtil.getStateLocation();
        final File storeFile     = new File(stateLocation, "connections.json");

        profileStore = new ConnectionProfileStore(storeFile);
        configWriter = new VolatileConfigWriter(stateLocation);
    }

    private void loadProfiles() {
        profiles = profileStore.loadAll();
        profileList.setInput(profiles);
        if (!profiles.isEmpty()) {
            profileList.setSelection(new StructuredSelection(profiles.get(0)));
        }
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        final Composite container = (Composite) super.createDialogArea(parent);
        resourceManager = new LocalResourceManager(JFaceResources.getResources(), getShell());

        // Phase 2: Structural Layout Upgrade (SashForm master-detail)
        final SashForm mainSash = new SashForm(container, SWT.HORIZONTAL | SWT.SMOOTH);
        mainSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        mainSash.setSashWidth(5);

        createProfileListSection(mainSash);
        createDetailSection(mainSash);

        mainSash.setWeights(new int[] { 30, 70 });

        return container;
    }

    private void createProfileListSection(final Composite parent) {
        final Composite leftPanel = new Composite(parent, SWT.NONE);
        leftPanel.setLayoutData(GridDataFactory.fillDefaults().grab(false, true).hint(220, SWT.DEFAULT).create());
        leftPanel.setLayout(GridLayoutFactory.swtDefaults().spacing(0, SPACING).create());

        // Header
        final Label headerLabel = new Label(leftPanel, SWT.NONE);
        headerLabel.setText("Connection Profiles");
        headerLabel.setFont(headerFont);
        headerLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

        // Separator
        final Label separator = new Label(leftPanel, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

        // Profile list
        profileList = new TableViewer(leftPanel, SWT.BORDER | SWT.V_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION);
        profileList.setContentProvider(ArrayContentProvider.getInstance());
        profileList.setLabelProvider(new ConnectionProfileLabelProvider());

        final Table table = profileList.getTable();
        table.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
        new TableColumn(table, SWT.NONE).setWidth(200);

        profileList.addSelectionChangedListener(new ProfileSelectionListener());

        // ToolBar for actions
        final ToolBar toolBar = new ToolBar(leftPanel, SWT.FLAT | SWT.RIGHT);
        toolBar.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

        final ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();

        addItem = new ToolItem(toolBar, SWT.PUSH);
        addItem.setImage(sharedImages.getImage(ISharedImages.IMG_OBJ_ADD));
        addItem.setToolTipText("Add Profile");
        addItem.addListener(SWT.Selection, e -> addProfile());

        duplicateItem = new ToolItem(toolBar, SWT.PUSH);
        duplicateItem.setImage(sharedImages.getImage(ISharedImages.IMG_TOOL_COPY));
        duplicateItem.setToolTipText("Duplicate Profile");
        duplicateItem.addListener(SWT.Selection, e -> duplicateProfile());

        removeItem = new ToolItem(toolBar, SWT.PUSH);
        removeItem.setImage(sharedImages.getImage(ISharedImages.IMG_TOOL_DELETE));
        removeItem.setToolTipText("Remove Profile");
        removeItem.addListener(SWT.Selection, e -> removeProfile());
    }

    private void createDetailSection(final Composite parent) {
        rightPanel = new Composite(parent, SWT.NONE);
        rightPanel.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
        detailStackLayout = new StackLayout();
        rightPanel.setLayout(detailStackLayout);

        createEmptyStateComposite(rightPanel);
        createFormComposite(rightPanel);

        detailStackLayout.topControl = emptyComposite;
    }

    private void createEmptyStateComposite(final Composite parent) {
        emptyComposite = new Composite(parent, SWT.NONE);
        emptyComposite.setLayout(GridLayoutFactory.fillDefaults().spacing(0, 20).margins(40, 40).create());

        // Center container
        final Composite centerContainer = new Composite(emptyComposite, SWT.NONE);
        centerContainer
                .setLayoutData(GridDataFactory.fillDefaults().grab(true, true).align(SWT.CENTER, SWT.CENTER).create());
        centerContainer.setLayout(GridLayoutFactory.fillDefaults().spacing(0, 15).create());

        // Icon
        final Label           iconLabel      = new Label(centerContainer, SWT.CENTER);
        final ImageDescriptor iconDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin("com.osgifx.eclipse.plugin",
                "icons/icon@2x.png");
        final Image           iconImage      = resourceManager.createImage(iconDescriptor);
        iconLabel.setImage(iconImage);
        iconLabel
                .setLayoutData(GridDataFactory.fillDefaults().grab(true, false).align(SWT.CENTER, SWT.CENTER).create());

        // Text
        final Label infoLabel = new Label(centerContainer, SWT.CENTER | SWT.WRAP);
        infoLabel.setText("Select a profile from the left or click '+' to create a new connection.");
        infoLabel.setFont(boldFont);
        infoLabel.setForeground(neutralColor);
        infoLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).hint(300, SWT.DEFAULT)
                .align(SWT.CENTER, SWT.CENTER).create());
    }

    private void createFormComposite(final Composite parent) {
        formComposite = new Composite(parent, SWT.NONE);
        formComposite.setLayout(GridLayoutFactory.swtDefaults().spacing(0, SPACING).create());

        // Header with status
        final Composite headerComposite = new Composite(formComposite, SWT.NONE);
        headerComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        headerComposite.setLayout(GridLayoutFactory.swtDefaults().numColumns(2).equalWidth(false).create());

        final Label detailsHeader = new Label(headerComposite, SWT.NONE);
        detailsHeader.setText("Connection Details");
        detailsHeader.setFont(headerFont);
        detailsHeader.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

        // Status indicator
        statusLabel = new Label(headerComposite, SWT.RIGHT);
        statusLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).align(SWT.END, SWT.CENTER).create());

        // Separator
        final Label separator = new Label(formComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

        // Profile name field
        final Composite nameComposite = new Composite(formComposite, SWT.NONE);
        nameComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        nameComposite.setLayout(
                GridLayoutFactory.swtDefaults().numColumns(2).equalWidth(false).spacing(SPACING, 0).create());

        createFormLabel(nameComposite, "Profile Name:");
        nameField = createFormText(nameComposite, "Enter profile name...");
        nameField.addModifyListener(e -> {
            if (ignoreModifyListeners) {
                return;
            }
            if (selectedProfile != null) {
                selectedProfile.name = nameField.getText();
                markDirty();
                profileList.update(selectedProfile, null);
            }
        });
        addValidation(nameField, "Profile name is required");

        // Last connected info
        lastConnectedLabel = new Label(formComposite, SWT.NONE);
        lastConnectedLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        lastConnectedLabel.setForeground(neutralColor);

        // Tab folder for SOCKET/MQTT
        tabFolder = new CTabFolder(formComposite, SWT.BORDER | SWT.FLAT | SWT.TOP);
        tabFolder.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
        tabFolder.setSimple(true);
        tabFolder.setBorderVisible(false);
        tabFolder.setUnselectedCloseVisible(false);

        // SOCKET Tab
        socketTab = new CTabItem(tabFolder, SWT.NONE);
        socketTab.setText("Socket");
        final ImageDescriptor socketDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin("com.osgifx.eclipse.plugin",
                "icons/socket_connection.png");
        socketTab.setImage(resourceManager.createImage(socketDescriptor));

        final Composite socketComposite = createSocketTabContent(tabFolder);
        socketTab.setControl(socketComposite);

        // MQTT Tab
        mqttTab = new CTabItem(tabFolder, SWT.NONE);
        mqttTab.setText("MQTT");
        final ImageDescriptor mqttDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin("com.osgifx.eclipse.plugin",
                "icons/mqtt_connection.png");
        mqttTab.setImage(resourceManager.createImage(mqttDescriptor));

        final Composite mqttComposite = createMqttTabContent(tabFolder);
        mqttTab.setControl(mqttComposite);

        tabFolder.setSelection(0);
        tabFolder.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (ignoreModifyListeners) {
                    return;
                }
                if (selectedProfile != null) {
                    final String  type  = tabFolder.getSelection() == socketTab ? "SOCKET" : "MQTT";
                    final boolean isNew = selectedProfile == newlyCreatedProfile;
                    // Only allow tab switching for newly created profiles
                    if (!type.equals(selectedProfile.type)) {
                        if (isNew) {
                            selectedProfile.type = type;
                            markDirty();
                        } else {
                            // Reset to previous tab if it's an existing profile
                            ignoreModifyListeners = true;
                            tabFolder.setSelection("SOCKET".equals(selectedProfile.type) ? socketTab : mqttTab);
                            ignoreModifyListeners = false;
                        }
                    }
                }
                validateFields();
            }
        });

        // Action Buttons Composite (Save & Connect side-by-side)
        final Composite actionButtonsComposite = new Composite(formComposite, SWT.NONE);
        actionButtonsComposite
                .setLayoutData(GridDataFactory.fillDefaults().grab(true, false).indent(0, SPACING).create());
        actionButtonsComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(true).create());

        // Save Button
        saveButton = new Button(actionButtonsComposite, SWT.PUSH);
        saveButton.setText("  \u2714  Save  ");
        saveButton.setFont(boldFont);
        saveButton.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        saveButton.setEnabled(false); // Disabled by default until dirty
        saveButton.addListener(SWT.Selection, e -> {
            saveCurrentProfile();
            // Flash a quick UI feedback (optional but good UX)
            statusLabel.setForeground(successColor);
            statusLabel.setText("Profile Saved");
        });

        // Connect button
        connectButton = createConnectButton(actionButtonsComposite);
        connectButton.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

        getShell().setDefaultButton(connectButton);
    }

    private Composite createSocketTabContent(final CTabFolder parent) {
        final ScrolledComposite scrolled = new ScrolledComposite(parent, SWT.V_SCROLL);
        scrolled.setExpandHorizontal(true);
        scrolled.setExpandVertical(true);

        final Composite composite = new Composite(scrolled, SWT.NONE);
        composite.setLayout(GridLayoutFactory.swtDefaults().numColumns(2).equalWidth(false).margins(MARGIN, MARGIN)
                .spacing(SPACING, 8).create());

        // Connection Settings
        createSectionHeader(composite, "Connection Settings", 2);

        createFormLabel(composite, "Host:");
        hostField = createFormText(composite, "e.g. localhost");
        addValidation(hostField, "Host is required");

        createFormLabel(composite, "Port:");
        portSpinner = createFormSpinner(composite, 1, 65535, 4567);

        createFormLabel(composite, "Timeout (ms):");
        timeoutSpinner = createFormSpinner(composite, 1000, 60000, 10000);
        timeoutSpinner.setIncrement(1000);

        // Security Settings
        createSectionHeader(composite, "Security Settings", 2);

        createFormLabel(composite, "Password:");
        passwordField = createFormPassword(composite);

        final List<Control> socketControls = List.of(hostField, portSpinner, timeoutSpinner, passwordField);
        socketControls.forEach(c -> c.addListener(SWT.Modify, e -> markDirty()));
        socketControls.forEach(c -> c.addListener(SWT.Selection, e -> markDirty()));

        createFormLabel(composite, "Truststore Path:");
        final Composite truststoreComposite = new Composite(composite, SWT.NONE);
        truststoreComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        truststoreComposite
                .setLayout(GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false).spacing(5, 0).create());

        truststorePathField = new Text(truststoreComposite, SWT.BORDER);
        truststorePathField.setMessage("e.g. /path/to/truststore.jks");
        truststorePathField.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

        final Button browseButton = new Button(truststoreComposite, SWT.PUSH);
        browseButton.setText("Browse...");
        browseButton.addListener(SWT.Selection, e -> browseTruststore());

        createFormLabel(composite, "Truststore Password:");
        truststorePasswordField = createFormPassword(composite);

        truststorePathField.addListener(SWT.Modify, e -> markDirty());
        truststorePasswordField.addListener(SWT.Modify, e -> markDirty());

        scrolled.setContent(composite);
        scrolled.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(final ControlEvent e) {
                scrolled.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            }
        });

        return scrolled;
    }

    private Composite createMqttTabContent(final CTabFolder parent) {
        final ScrolledComposite scrolled = new ScrolledComposite(parent, SWT.V_SCROLL);
        scrolled.setExpandHorizontal(true);
        scrolled.setExpandVertical(true);

        final Composite composite = new Composite(scrolled, SWT.NONE);
        composite.setLayout(GridLayoutFactory.swtDefaults().numColumns(2).equalWidth(false).margins(MARGIN, MARGIN)
                .spacing(SPACING, 8).create());

        // Broker Settings
        createSectionHeader(composite, "Broker Settings", 2);

        createFormLabel(composite, "Broker Server:");
        serverField = createFormText(composite, "e.g. broker.hivemq.com");
        addValidation(serverField, "Broker server is required");

        createFormLabel(composite, "Broker Port:");
        mqttPortSpinner = createFormSpinner(composite, 1, 65535, 1883);

        createFormLabel(composite, "Timeout (ms):");
        mqttTimeoutSpinner = createFormSpinner(composite, 1000, 60000, 10000);
        mqttTimeoutSpinner.setIncrement(1000);

        createFormLabel(composite, "Client ID:");
        clientIdField = createFormText(composite, "e.g. osgifx-client");
        addValidation(clientIdField, "Client ID is required");

        // Authentication
        createSectionHeader(composite, "Authentication", 2);

        createFormLabel(composite, "Username:");
        usernameField = createFormText(composite, "e.g. user123");

        createFormLabel(composite, "Password:");
        mqttPasswordField = createFormPassword(composite);

        // Topic Settings
        createSectionHeader(composite, "Topic Configuration", 2);

        createFormLabel(composite, "Publish Topic:");
        pubTopicField = createFormText(composite, "e.g. osgifx/pub");
        addValidation(pubTopicField, "Publish topic is required");

        createFormLabel(composite, "Subscribe Topic:");
        subTopicField = createFormText(composite, "e.g. osgifx/sub");
        addValidation(subTopicField, "Subscribe topic is required");

        createFormLabel(composite, "LWT Topic:");
        lwtTopicField = createFormText(composite, "e.g. osgifx/lwt");

        // OAuth2
        createSectionHeader(composite, "OAuth2 Token Configuration (Optional)", 2);

        createFormLabel(composite, "Auth Server URL:");
        authServerUrlField = createFormText(composite, "e.g. https://auth.example.com");

        createFormLabel(composite, "Client ID:");
        oauthClientIdField = createFormText(composite, "e.g. my-client-id");

        createFormLabel(composite, "Client Secret:");
        clientSecretField = createFormPassword(composite);

        createFormLabel(composite, "Audience:");
        audienceField = createFormText(composite, "e.g. https://api.example.com");

        createFormLabel(composite, "Scope:");
        scopeField = createFormText(composite, "e.g. openid profile email");

        final List<Control> mqttControls = List.of(serverField, mqttPortSpinner, mqttTimeoutSpinner, clientIdField,
                usernameField, mqttPasswordField, pubTopicField, subTopicField, lwtTopicField, authServerUrlField,
                oauthClientIdField, clientSecretField, audienceField, scopeField);
        mqttControls.forEach(c -> c.addListener(SWT.Modify, e -> markDirty()));
        mqttControls.forEach(c -> c.addListener(SWT.Selection, e -> markDirty()));

        scrolled.setContent(composite);
        scrolled.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(final ControlEvent e) {
                scrolled.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            }
        });

        return scrolled;
    }

    private void createSectionHeader(final Composite parent, final String title, final int columns) {
        final Label header = new Label(parent, SWT.NONE);
        header.setText(title);
        header.setFont(boldFont);
        header.setLayoutData(GridDataFactory.fillDefaults().span(columns, 1).grab(true, false).indent(0, 10).create());

        final Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(GridDataFactory.fillDefaults().span(columns, 1).grab(true, false).create());
    }

    private void addValidation(final Text text, final String message) {
        final ControlDecoration decoration = new ControlDecoration(text, SWT.TOP | SWT.LEFT);
        decoration.setDescriptionText(message);
        decoration.setImage(
                FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
        decoration.hide();

        decorations.put(text, decoration);

        text.addModifyListener(e -> validateFields());
    }

    private void validateFields() {
        boolean isValid = true;

        // Common validation
        if (nameField.getText().trim().isEmpty()) {
            isValid = false;
        }

        // Contextual validation
        if (tabFolder.getSelection() == socketTab) {
            if (hostField.getText().trim().isEmpty()) {
                isValid = false;
            }
        } else if (tabFolder.getSelection() == mqttTab) {
            if (serverField.getText().trim().isEmpty() || clientIdField.getText().trim().isEmpty()
                    || pubTopicField.getText().trim().isEmpty() || subTopicField.getText().trim().isEmpty()) {
                isValid = false;
            }
        }

        // Update decorations
        for (final Map.Entry<Control, ControlDecoration> entry : decorations.entrySet()) {
            final Control           control    = entry.getKey();
            final ControlDecoration decoration = entry.getValue();
            if (control instanceof Text) {
                if (((Text) control).getText().trim().isEmpty()) {
                    decoration.show();
                } else {
                    decoration.hide();
                }
            }
        }

        final boolean isConnecting = selectedProfile != null && connectingProfiles.contains(selectedProfile.id);

        if (connectButton != null && !connectButton.isDisposed()) {
            if (isConnecting) {
                connectButton.setEnabled(false);
                connectButton.setText("  Connecting...  ");
            } else {
                connectButton.setEnabled(isValid && !isDirty);
                connectButton.setText("  \u25B6  Connect  ");
            }
        }
        final Button okButton = getButton(OK);
        if (okButton != null && !okButton.isDisposed()) {
            okButton.setEnabled(isValid);
        }
    }

    private void updatePanelState() {
        final boolean isSelection = selectedProfile != null;

        // Phase 4: Toggle the StackLayout
        if (detailStackLayout != null) {
            detailStackLayout.topControl = isSelection ? formComposite : emptyComposite;
            rightPanel.layout();
        }

        duplicateItem.setEnabled(isSelection);
        removeItem.setEnabled(isSelection);

        if (!isSelection) {
            decorations.values().forEach(ControlDecoration::hide);
            if (connectButton != null && !connectButton.isDisposed()) {
                connectButton.setEnabled(false);
            }
            if (saveButton != null && !saveButton.isDisposed()) {
                saveButton.setEnabled(false);
            }
            final Button okButton = getButton(OK);
            if (okButton != null && !okButton.isDisposed()) {
                okButton.setEnabled(false);
            }
        } else {
            final Button okButton = getButton(OK);
            if (okButton != null && !okButton.isDisposed()) {
                okButton.setEnabled(true);
            }
            if (connectButton != null && !connectButton.isDisposed()) {
                final boolean isConnecting = connectingProfiles.contains(selectedProfile.id);
                if (isConnecting) {
                    connectButton.setEnabled(false);
                    connectButton.setText("  Connecting...  ");
                } else {
                    connectButton.setEnabled(!isDirty);
                    connectButton.setText("  \u25B6  Connect  ");
                }
            }
            if (saveButton != null && !saveButton.isDisposed()) {
                saveButton.setEnabled(isDirty);
            }
        }
    }

    private void createFormLabel(final Composite parent, final String text) {
        final Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        label.setLayoutData(
                GridDataFactory.swtDefaults().hint(LABEL_WIDTH, SWT.DEFAULT).align(SWT.END, SWT.CENTER).create());
    }

    private Text createFormText(final Composite parent, final String placeholder) {
        final Text text = new Text(parent, SWT.BORDER | SWT.SINGLE);
        text.setMessage(placeholder);
        text.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).hint(200, SWT.DEFAULT).create());
        return text;
    }

    private Text createFormPassword(final Composite parent) {
        final Text text = new Text(parent, SWT.BORDER | SWT.SINGLE | SWT.PASSWORD);
        text.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).hint(200, SWT.DEFAULT).create());
        return text;
    }

    private Spinner createFormSpinner(final Composite parent, final int min, final int max, final int value) {
        final Spinner spinner = new Spinner(parent, SWT.BORDER);
        spinner.setMinimum(min);
        spinner.setMaximum(max);
        spinner.setSelection(value);
        spinner.setLayoutData(GridDataFactory.swtDefaults().align(SWT.BEGINNING, SWT.CENTER).create());
        return spinner;
    }

    private Button createConnectButton(final Composite parent) {
        final Button button = new Button(parent, SWT.PUSH);
        button.setText("  \u25B6  Connect  ");
        button.setFont(boldFont);
        button.addListener(SWT.Selection, e -> {
            if (handleDirtyState()) {
                connect();
            }
        });
        return button;
    }

    private void addProfile() {
        if (!handleDirtyState()) {
            return;
        }
        final ConnectionProfile profile = new ConnectionProfile("New Connection", "SOCKET");
        newlyCreatedProfile = profile;
        profiles.add(profile);
        profileList.refresh();
        profileList.setSelection(new StructuredSelection(profile));
        isDirty         = true;
        snapshotProfile = null; // Forces dirty because there is no snapshot yet
        profileList.refresh(profile);
        updatePanelState();
        nameField.setFocus();
        nameField.selectAll();
    }

    private void removeProfile() {
        if (selectedProfile == null) {
            return;
        }
        final boolean confirmed = MessageDialog.openConfirm(getShell(), "Remove Profile",
                "Are you sure you want to remove the profile '" + selectedProfile.name + "'?");
        if (!confirmed) {
            return;
        }
        isDirty = false; // Prevent prompt when selection changes during removal
        profiles.remove(selectedProfile);
        profileStore.remove(selectedProfile.id);
        profileList.refresh();
        if (!profiles.isEmpty()) {
            profileList.setSelection(new StructuredSelection(profiles.get(0)));
        } else {
            selectedProfile = null;
            clearFields();
        }
        updatePanelState();
    }

    private void duplicateProfile() {
        if (selectedProfile == null || !handleDirtyState()) {
            return;
        }
        final ConnectionProfile copy = cloneProfile(selectedProfile);
        copy.id            = UUID.randomUUID().toString();
        copy.name          = selectedProfile.name + " (Copy)";
        copy.lastConnected = null;
        copy.lastStatus    = null;

        newlyCreatedProfile = copy;
        profiles.add(copy);

        profileList.refresh();
        profileList.setSelection(new StructuredSelection(copy));
        isDirty         = true;
        snapshotProfile = null; // Forces dirty because there is no snapshot yet
        profileList.refresh(copy);
        updatePanelState();
        nameField.setFocus();
        nameField.selectAll();
    }

    private void browseTruststore() {
        final FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
        dialog.setFilterExtensions(new String[] { "*.jks", "*.p12", "*.*" });
        final String path = dialog.open();
        if (path != null) {
            truststorePathField.setText(path);
        }
    }

    private void connect() {
        if (selectedProfile == null) {
            return;
        }

        final ConnectionProfile profileToConnect = selectedProfile;

        connectingProfiles.add(profileToConnect.id);

        connectButton.setEnabled(false);
        connectButton.setText("  Connecting...  ");

        final Job job = new Job("Launching OSGi.fx: " + profileToConnect.name) {
            @Override
            protected IStatus run(final IProgressMonitor monitor) {
                try {
                    final AzulZuluDownloader zuluDownloader = new AzulZuluDownloader();
                    if (!zuluDownloader.isRuntimeAvailable()) {
                        zuluDownloader.schedule();
                        zuluDownloader.join();
                        final IStatus result = zuluDownloader.getResult();
                        if (result != null && !result.isOK()) {
                            throw new Exception(result.getMessage());
                        }
                    }

                    final RunOsgiFxDownloader scriptDownloader = new RunOsgiFxDownloader();
                    if (!scriptDownloader.isScriptAvailable()) {
                        scriptDownloader.download();
                    }

                    final Path configPath = configWriter.writeHeadlessConfig(profileToConnect);

                    final IPreferenceStore preferences = OsgifxWorkspaceUtil.getPreferenceStore();
                    final boolean          useLocal    = preferences.getBoolean(USE_LOCAL_JAR);
                    final String           localJar    = preferences.getString(OSGIFX_LOCAL_JAR);
                    final String           gav         = preferences.getString(OSGIFX_GAV);

                    final OsgifxProcessLauncher launcher = new OsgifxProcessLauncher(profileToConnect, configPath,
                                                                                     zuluDownloader
                                                                                             .getJavaExecutablePath(),
                                                                                     scriptDownloader.getScriptPath(),
                                                                                     gav, useLocal ? localJar : null);

                    launcher.schedule();
                    launcher.join();

                    final IStatus launchStatus = launcher.getResult();

                    if (launchStatus.isOK()) {
                        Display.getDefault().asyncExec(() -> {
                            profileToConnect.lastConnected = Instant.now().toString();
                            profileToConnect.lastStatus    = "SUCCESS";
                            profileStore.update(profileToConnect);

                            if (getShell() != null && !getShell().isDisposed()) {
                                close();
                            }
                        });
                    } else {
                        throw new Exception(launchStatus.getMessage());
                    }

                    return Status.OK_STATUS;
                } catch (final Exception e) {
                    Display.getDefault().asyncExec(() -> {
                        profileToConnect.lastStatus = "FAILURE";
                        profileStore.update(profileToConnect);

                        if (getShell() != null && !getShell().isDisposed()) {
                            final Path logFile = OsgifxProcessLauncher.getLogFile(profileToConnect.id);
                            String     msg     = "Failed to launch OSGi.fx. " + e.getMessage();
                            if (logFile != null && Files.exists(logFile)) {
                                msg += "\n\nLog file created at: " + logFile.toAbsolutePath();
                            }
                            MessageDialog.openError(getShell(), "Connection Error", msg);
                        }
                    });
                    return Status.CANCEL_STATUS;
                } finally {
                    connectingProfiles.remove(profileToConnect.id);
                    Display.getDefault().asyncExec(() -> {
                        if (connectButton != null && !connectButton.isDisposed() && selectedProfile != null
                                && profileToConnect.id.equals(selectedProfile.id)) {
                            connectButton.setText("  \u25B6  Connect  ");
                            connectButton.setEnabled(true);
                        }
                    });
                }
            }
        };
        job.setUser(true);
        job.schedule();
    }

    private ConnectionProfile cloneProfile(final ConnectionProfile profile) {
        if (profile == null) {
            return null;
        }
        final ConnectionProfile clone = new ConnectionProfile();
        clone.id                 = profile.id;
        clone.name               = profile.name;
        clone.type               = profile.type;
        clone.host               = profile.host;
        clone.port               = profile.port;
        clone.timeout            = profile.timeout;
        clone.password           = profile.password;
        clone.trustStorePath     = profile.trustStorePath;
        clone.trustStorePassword = profile.trustStorePassword;
        clone.server             = profile.server;
        clone.mqttPort           = profile.mqttPort;
        clone.mqttTimeout        = profile.mqttTimeout;
        clone.clientId           = profile.clientId;
        clone.username           = profile.username;
        clone.mqttPassword       = profile.mqttPassword;
        clone.pubTopic           = profile.pubTopic;
        clone.subTopic           = profile.subTopic;
        clone.lwtTopic           = profile.lwtTopic;
        if (profile.tokenConfig != null) {
            clone.tokenConfig               = new ConnectionProfile.TokenConfig();
            clone.tokenConfig.authServerURL = profile.tokenConfig.authServerURL;
            clone.tokenConfig.clientId      = profile.tokenConfig.clientId;
            clone.tokenConfig.clientSecret  = profile.tokenConfig.clientSecret;
            clone.tokenConfig.audience      = profile.tokenConfig.audience;
            clone.tokenConfig.scope         = profile.tokenConfig.scope;
        }
        clone.lastConnected = profile.lastConnected;
        clone.lastStatus    = profile.lastStatus;
        return clone;
    }

    private void saveCurrentProfile() {
        if (selectedProfile == null) {
            return;
        }

        selectedProfile.name = nameField.getText();

        if (tabFolder.getSelection() == socketTab) {
            selectedProfile.type               = "SOCKET";
            selectedProfile.host               = hostField.getText();
            selectedProfile.port               = portSpinner.getSelection();
            selectedProfile.timeout            = timeoutSpinner.getSelection();
            selectedProfile.password           = passwordField.getText();
            selectedProfile.trustStorePath     = truststorePathField.getText();
            selectedProfile.trustStorePassword = truststorePasswordField.getText();
        } else {
            selectedProfile.type         = "MQTT";
            selectedProfile.server       = serverField.getText();
            selectedProfile.mqttPort     = mqttPortSpinner.getSelection();
            selectedProfile.mqttTimeout  = mqttTimeoutSpinner.getSelection();
            selectedProfile.clientId     = clientIdField.getText();
            selectedProfile.username     = usernameField.getText();
            selectedProfile.mqttPassword = mqttPasswordField.getText();
            selectedProfile.pubTopic     = pubTopicField.getText();
            selectedProfile.subTopic     = subTopicField.getText();
            selectedProfile.lwtTopic     = lwtTopicField.getText();

            if (!authServerUrlField.getText().isEmpty()) {
                selectedProfile.tokenConfig               = new ConnectionProfile.TokenConfig();
                selectedProfile.tokenConfig.authServerURL = authServerUrlField.getText();
                selectedProfile.tokenConfig.clientId      = oauthClientIdField.getText();
                selectedProfile.tokenConfig.clientSecret  = clientSecretField.getText();
                selectedProfile.tokenConfig.audience      = audienceField.getText();
                selectedProfile.tokenConfig.scope         = scopeField.getText();
            } else {
                selectedProfile.tokenConfig = null;
            }
        }

        if (selectedProfile == newlyCreatedProfile) {
            profileStore.add(selectedProfile);
        } else {
            profileStore.update(selectedProfile);
        }
        isDirty             = false;
        newlyCreatedProfile = null;
        snapshotProfile     = cloneProfile(selectedProfile);
        profileList.refresh(selectedProfile);

        // Disable the save button since we just saved
        if (saveButton != null && !saveButton.isDisposed()) {
            saveButton.setEnabled(false);
        }
    }

    private boolean isProfileDirty() {
        if (selectedProfile == null) {
            return false;
        }
        if (selectedProfile == newlyCreatedProfile) {
            return true;
        }
        if (snapshotProfile == null) {
            return false;
        }

        // Compare basic info
        if (!nameField.getText().equals(snapshotProfile.name)) {
            return true;
        }

        if (tabFolder.getSelection() == socketTab) {
            if (!"SOCKET".equals(snapshotProfile.type)) {
                return true;
            }
            if (!hostField.getText().equals(snapshotProfile.host != null ? snapshotProfile.host : "")) {
                return true;
            }
            if (portSpinner.getSelection() != snapshotProfile.port) {
                return true;
            }
            if (timeoutSpinner.getSelection() != snapshotProfile.timeout) {
                return true;
            }
            if (!passwordField.getText().equals(snapshotProfile.password != null ? snapshotProfile.password : "")) {
                return true;
            }
            if (!truststorePathField.getText()
                    .equals(snapshotProfile.trustStorePath != null ? snapshotProfile.trustStorePath : "")) {
                return true;
            }
            if (!truststorePasswordField.getText()
                    .equals(snapshotProfile.trustStorePassword != null ? snapshotProfile.trustStorePassword : "")) {
                return true;
            }
        } else {
            if (!"MQTT".equals(snapshotProfile.type)) {
                return true;
            }
            if (!serverField.getText().equals(snapshotProfile.server != null ? snapshotProfile.server : "")) {
                return true;
            }
            if (mqttPortSpinner.getSelection() != snapshotProfile.mqttPort) {
                return true;
            }
            if (mqttTimeoutSpinner.getSelection() != snapshotProfile.mqttTimeout) {
                return true;
            }
            if (!clientIdField.getText().equals(snapshotProfile.clientId != null ? snapshotProfile.clientId : "")) {
                return true;
            }
            if (!usernameField.getText().equals(snapshotProfile.username != null ? snapshotProfile.username : "")) {
                return true;
            }
            if (!mqttPasswordField.getText()
                    .equals(snapshotProfile.mqttPassword != null ? snapshotProfile.mqttPassword : "")) {
                return true;
            }
            if (!pubTopicField.getText().equals(snapshotProfile.pubTopic != null ? snapshotProfile.pubTopic : "")) {
                return true;
            }
            if (!subTopicField.getText().equals(snapshotProfile.subTopic != null ? snapshotProfile.subTopic : "")) {
                return true;
            }
            if (!lwtTopicField.getText().equals(snapshotProfile.lwtTopic != null ? snapshotProfile.lwtTopic : "")) {
                return true;
            }

            // OAuth2 comparison
            if (authServerUrlField.getText().isEmpty()) {
                if (snapshotProfile.tokenConfig != null) {
                    return true;
                }
            } else {
                if (snapshotProfile.tokenConfig == null) {
                    return true;
                }
                if (!authServerUrlField.getText()
                        .equals(snapshotProfile.tokenConfig.authServerURL != null
                                ? snapshotProfile.tokenConfig.authServerURL
                                : "")) {
                    return true;
                }
                if (!oauthClientIdField.getText().equals(
                        snapshotProfile.tokenConfig.clientId != null ? snapshotProfile.tokenConfig.clientId : "")) {
                    return true;
                }
                if (!clientSecretField.getText()
                        .equals(snapshotProfile.tokenConfig.clientSecret != null
                                ? snapshotProfile.tokenConfig.clientSecret
                                : "")) {
                    return true;
                }
                if (!audienceField.getText().equals(
                        snapshotProfile.tokenConfig.audience != null ? snapshotProfile.tokenConfig.audience : "")) {
                    return true;
                }
                if (!scopeField.getText()
                        .equals(snapshotProfile.tokenConfig.scope != null ? snapshotProfile.tokenConfig.scope : "")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void markDirty() {
        if (ignoreModifyListeners || selectedProfile == null) {
            return;
        }
        final boolean nowDirty = isProfileDirty();
        if (isDirty != nowDirty) {
            isDirty = nowDirty;
            profileList.update(selectedProfile, null);

            // Toggle the Save button dynamically
            if (saveButton != null && !saveButton.isDisposed()) {
                saveButton.setEnabled(isDirty);
            }
        }
    }

    private boolean handleDirtyState() {
        if (!isDirty || selectedProfile == null) {
            return true;
        }
        final MessageDialog dialog = new MessageDialog(getShell(), "Save Changes", null,
                                                       "The current connection profile has unsaved changes. Do you want to save them?",
                                                       MessageDialog.QUESTION, new String[] { "Yes", "No", "Cancel" },
                                                       0);
        final int           result = dialog.open();
        if (result == 0) { // Yes
            saveCurrentProfile();
            return true;
        }
        if (result == 1) { // No
            if (selectedProfile == newlyCreatedProfile) {
                profiles.remove(selectedProfile);
                newlyCreatedProfile = null;
                selectedProfile     = null;
                clearFields();
            } else if (snapshotProfile != null) {
                // Restore the eagerly mutated name
                selectedProfile.name = snapshotProfile.name;
                // Reload the UI widgets to wipe out the discarded typed text
                populateFields(selectedProfile);
            }
            isDirty = false;
            profileList.refresh();
            return true;
        }
        return false; // Cancel or Close
    }

    private void clearFields() {
        nameField.setText("");
        hostField.setText("");
        portSpinner.setSelection(4567);
        timeoutSpinner.setSelection(10000);
        passwordField.setText("");
        truststorePathField.setText("");
        truststorePasswordField.setText("");
        serverField.setText("");
        mqttPortSpinner.setSelection(1883);
        mqttTimeoutSpinner.setSelection(10000);
        clientIdField.setText("");
        usernameField.setText("");
        mqttPasswordField.setText("");
        pubTopicField.setText("");
        subTopicField.setText("");
        lwtTopicField.setText("");
        authServerUrlField.setText("");
        oauthClientIdField.setText("");
        clientSecretField.setText("");
        audienceField.setText("");
        scopeField.setText("");
        statusLabel.setText("");
        lastConnectedLabel.setText("");
    }

    private void updateStatusDisplay() {
        if (selectedProfile == null) {
            return;
        }

        // Update status indicator
        if ("SUCCESS".equals(selectedProfile.lastStatus)) {
            statusLabel.setForeground(successColor);
            statusLabel.setText("\u2713 Connected");
        } else if ("FAILURE".equals(selectedProfile.lastStatus)) {
            statusLabel.setForeground(errorColor);
            statusLabel.setText("\u2717 Connection Failed");
        } else {
            statusLabel.setForeground(neutralColor);
            statusLabel.setText("\u25CB Never Connected");
        }

        // Update last connected time
        if (selectedProfile.lastConnected != null) {
            try {
                final Instant           instant   = Instant.parse(selectedProfile.lastConnected);
                final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .withZone(ZoneId.systemDefault());
                lastConnectedLabel.setText("Last connected: " + formatter.format(instant));
            } catch (final Exception e) {
                lastConnectedLabel.setText("");
            }
        } else {
            lastConnectedLabel.setText("");
        }
    }

    private final class ProfileSelectionListener implements ISelectionChangedListener {
        @Override
        public void selectionChanged(final SelectionChangedEvent event) {
            if (ignoreSelectionChange) {
                return;
            }
            if (!handleDirtyState()) {
                ignoreSelectionChange = true;
                profileList.setSelection(new StructuredSelection(selectedProfile));
                ignoreSelectionChange = false;
                return;
            }

            final StructuredSelection selection = (StructuredSelection) event.getSelection();
            selectedProfile = (ConnectionProfile) selection.getFirstElement();

            updatePanelState();

            if (selectedProfile != null) {
                if (selectedProfile != newlyCreatedProfile) {
                    newlyCreatedProfile = null;
                    updatePanelState(); // Re-update to lock the tabs
                }
                populateFields(selectedProfile);
            } else {
                clearFields();
            }
        }
    }

    private void populateFields(final ConnectionProfile profile) {
        ignoreModifyListeners = true;
        try {
            nameField.setText(profile.name != null ? profile.name : "");

            if ("MQTT".equals(profile.type)) {
                tabFolder.setSelection(mqttTab);
                serverField.setText(profile.server != null ? profile.server : "");
                mqttPortSpinner.setSelection(profile.mqttPort > 0 ? profile.mqttPort : 1883);
                mqttTimeoutSpinner.setSelection(profile.mqttTimeout > 0 ? profile.mqttTimeout : 10000);
                clientIdField.setText(profile.clientId != null ? profile.clientId : "");
                usernameField.setText(profile.username != null ? profile.username : "");
                mqttPasswordField.setText(profile.mqttPassword != null ? profile.mqttPassword : "");
                pubTopicField.setText(profile.pubTopic != null ? profile.pubTopic : "");
                subTopicField.setText(profile.subTopic != null ? profile.subTopic : "");
                lwtTopicField.setText(profile.lwtTopic != null ? profile.lwtTopic : "");

                if (profile.tokenConfig != null) {
                    authServerUrlField.setText(
                            profile.tokenConfig.authServerURL != null ? profile.tokenConfig.authServerURL : "");
                    oauthClientIdField
                            .setText(profile.tokenConfig.clientId != null ? profile.tokenConfig.clientId : "");
                    clientSecretField
                            .setText(profile.tokenConfig.clientSecret != null ? profile.tokenConfig.clientSecret : "");
                    audienceField.setText(profile.tokenConfig.audience != null ? profile.tokenConfig.audience : "");
                    scopeField.setText(profile.tokenConfig.scope != null ? profile.tokenConfig.scope : "");
                } else {
                    authServerUrlField.setText("");
                    oauthClientIdField.setText("");
                    clientSecretField.setText("");
                    audienceField.setText("");
                    scopeField.setText("");
                }
            } else {
                tabFolder.setSelection(socketTab);
                hostField.setText(profile.host != null ? profile.host : "");
                portSpinner.setSelection(profile.port > 0 ? profile.port : 4567);
                timeoutSpinner.setSelection(profile.timeout > 0 ? profile.timeout : 10000);
                passwordField.setText(profile.password != null ? profile.password : "");
                truststorePathField.setText(profile.trustStorePath != null ? profile.trustStorePath : "");
                truststorePasswordField.setText(profile.trustStorePassword != null ? profile.trustStorePassword : "");
            }
            snapshotProfile = cloneProfile(profile);
            isDirty         = newlyCreatedProfile == profile;
        } finally {
            ignoreModifyListeners = false;
        }

        updateStatusDisplay();
        validateFields();
    }

    private final class ConnectionProfileLabelProvider extends LabelProvider {

        private final ImageDescriptor mqttDescriptor;
        private final ImageDescriptor socketDescriptor;

        public ConnectionProfileLabelProvider() {
            mqttDescriptor   = AbstractUIPlugin.imageDescriptorFromPlugin("com.osgifx.eclipse.plugin",
                    "icons/mqtt_connection.png");
            socketDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin("com.osgifx.eclipse.plugin",
                    "icons/socket_connection.png");
        }

        @Override
        public String getText(final Object element) {
            if (element instanceof ConnectionProfile) {
                final ConnectionProfile profile = (ConnectionProfile) element;
                final String            icon    = getStatusIcon(profile.lastStatus);
                final String            dirty   = isDirty && profile == selectedProfile ? " *" : "";
                return icon + " " + profile.name + dirty;
            }
            return super.getText(element);
        }

        @Override
        public Image getImage(final Object element) {
            if (element instanceof ConnectionProfile) {
                final ConnectionProfile profile    = (ConnectionProfile) element;
                final ImageDescriptor   descriptor = "MQTT".equals(profile.type) ? mqttDescriptor : socketDescriptor;
                return resourceManager.createImage(descriptor);
            }
            return super.getImage(element);
        }

        private String getStatusIcon(final String status) {
            if ("SUCCESS".equals(status)) {
                return "\u2713"; // Checkmark
            } else if ("FAILURE".equals(status)) {
                return "\u2717"; // X mark
            }
            return "\u25CB"; // Empty circle
        }
    }

    @Override
    public boolean close() {
        if (headerFont != null) {
            headerFont.dispose();
        }
        if (boldFont != null) {
            boldFont.dispose();
        }
        if (successColor != null) {
            successColor.dispose();
        }
        if (errorColor != null) {
            errorColor.dispose();
        }
        if (neutralColor != null) {
            neutralColor.dispose();
        }
        decorations.values().forEach(ControlDecoration::dispose);
        decorations.clear();
        if (resourceManager != null) {
            resourceManager.dispose();
        }
        return super.close();
    }

    @Override
    protected void okPressed() {
        saveCurrentProfile();
        super.okPressed();
    }

    @Override
    protected void cancelPressed() {
        if (!handleDirtyState()) {
            return; // User clicked 'Cancel' on the dirty prompt, abort closing
        }
        super.cancelPressed();
    }

    @Override
    protected void handleShellCloseEvent() {
        if (!handleDirtyState()) {
            return; // User clicked 'Cancel' on the dirty prompt, abort window X closure
        }
        super.handleShellCloseEvent();
    }

    @Override
    protected boolean isResizable() {
        return true;
    }
}
