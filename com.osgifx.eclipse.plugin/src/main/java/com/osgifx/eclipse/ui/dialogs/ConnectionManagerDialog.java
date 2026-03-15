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
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
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
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
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

    private ConnectionProfileStore  profileStore;
    private VolatileConfigWriter    configWriter;
    private List<ConnectionProfile> profiles;
    private ConnectionProfile       selectedProfile;

    // UI Components
    private TableViewer profileList;
    private Composite   rightPanel;
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
    private CTabFolder tabFolder;
    private CTabItem   socketTab;
    private CTabItem   mqttTab;

    // Validation
    private final Map<Control, ControlDecoration> decorations = new HashMap<>();
    private Button                                connectButton;
    private ResourceManager                       resourceManager;

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
        resourceManager = new LocalResourceManager(JFaceResources.getResources(), getShell());
        initializeFonts();
        initializeColors();
        getShell().setText("OSGi.fx Connection Manager");
        setTitle("OSGi.fx Connection Manager");
        setMessage("Create and manage connection profiles for OSGi.fx diagnostic tool");
        getShell().setSize(780, 680);
        initializeStores();
        loadProfiles();
        updatePanelState();
    }

    private void initializeFonts() {
        final var display    = Display.getCurrent();
        final var systemFont = JFaceResources.getDefaultFont();
        final var fontData   = systemFont.getFontData();

        // Header font (larger, bold)
        final var headerFontData = new FontData(fontData[0].getName(), fontData[0].getHeight() + 2, SWT.BOLD);
        headerFont = new Font(display, headerFontData);

        // Bold font
        final var boldFontData = new FontData(fontData[0].getName(), fontData[0].getHeight(), SWT.BOLD);
        boldFont = new Font(display, boldFontData);
    }

    private void initializeColors() {
        final var display = Display.getCurrent();
        successColor = new Color(display, 46, 125, 50); // Green
        errorColor   = new Color(display, 198, 40, 40); // Red
        neutralColor = new Color(display, 97, 97, 97);  // Gray
    }

    private void initializeStores() {
        final var stateLocation = OsgifxWorkspaceUtil.getStateLocation();
        final var storeFile     = new File(stateLocation, "connections.json");

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
        final var container = (Composite) super.createDialogArea(parent);

        final var mainComposite = new Composite(container, SWT.NONE);
        mainComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        mainComposite.setLayout(GridLayoutFactory.swtDefaults().numColumns(2).equalWidth(false).margins(MARGIN, MARGIN)
                .spacing(SPACING, SPACING).create());

        createProfileListSection(mainComposite);
        createDetailSection(mainComposite);

        return container;
    }

    private void createProfileListSection(final Composite parent) {
        final var leftPanel = new Composite(parent, SWT.NONE);
        leftPanel.setLayoutData(GridDataFactory.fillDefaults().grab(false, true).hint(220, SWT.DEFAULT).create());
        leftPanel.setLayout(GridLayoutFactory.swtDefaults().spacing(0, SPACING).create());

        // Header
        final var headerLabel = new Label(leftPanel, SWT.NONE);
        headerLabel.setText("Connection Profiles");
        headerLabel.setFont(headerFont);
        headerLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

        // Separator
        final var separator = new Label(leftPanel, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

        // Profile list
        profileList = new TableViewer(leftPanel, SWT.BORDER | SWT.V_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION);
        profileList.setContentProvider(ArrayContentProvider.getInstance());
        profileList.setLabelProvider(new ConnectionProfileLabelProvider());

        final var table = profileList.getTable();
        table.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
        new TableColumn(table, SWT.NONE).setWidth(200);

        profileList.addSelectionChangedListener(new ProfileSelectionListener());

        // ToolBar for actions
        final var toolBar = new ToolBar(leftPanel, SWT.FLAT | SWT.RIGHT);
        toolBar.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

        final var sharedImages = PlatformUI.getWorkbench().getSharedImages();

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
        rightPanel.setLayout(GridLayoutFactory.swtDefaults().spacing(0, SPACING).create());

        // Header with status
        final var headerComposite = new Composite(rightPanel, SWT.NONE);
        headerComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        headerComposite.setLayout(GridLayoutFactory.swtDefaults().numColumns(2).equalWidth(false).create());

        final var detailsHeader = new Label(headerComposite, SWT.NONE);
        detailsHeader.setText("Connection Details");
        detailsHeader.setFont(headerFont);
        detailsHeader.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

        // Status indicator
        statusLabel = new Label(headerComposite, SWT.RIGHT);
        statusLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).align(SWT.END, SWT.CENTER).create());

        // Separator
        final var separator = new Label(rightPanel, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

        // Profile name field
        final var nameComposite = new Composite(rightPanel, SWT.NONE);
        nameComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        nameComposite.setLayout(
                GridLayoutFactory.swtDefaults().numColumns(2).equalWidth(false).spacing(SPACING, 0).create());

        createFormLabel(nameComposite, "Profile Name:");
        nameField = createFormText(nameComposite, "Enter profile name...");
        addValidation(nameField, "Profile name is required");

        // Last connected info
        lastConnectedLabel = new Label(rightPanel, SWT.NONE);
        lastConnectedLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        lastConnectedLabel.setForeground(neutralColor);

        // Tab folder for SOCKET/MQTT
        tabFolder = new CTabFolder(rightPanel, SWT.BORDER | SWT.FLAT | SWT.TOP);
        tabFolder.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
        tabFolder.setSimple(false);
        tabFolder.setUnselectedCloseVisible(false);

        // SOCKET Tab
        socketTab = new CTabItem(tabFolder, SWT.NONE);
        socketTab.setText("  Socket  ");
        final var socketComposite = createSocketTabContent(tabFolder);
        socketTab.setControl(socketComposite);

        // MQTT Tab
        mqttTab = new CTabItem(tabFolder, SWT.NONE);
        mqttTab.setText("  MQTT  ");
        final var mqttComposite = createMqttTabContent(tabFolder);
        mqttTab.setControl(mqttComposite);

        tabFolder.setSelection(0);
        tabFolder.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                validateFields();
            }
        });

        // Connect button
        connectButton = createConnectButton(rightPanel);
        connectButton.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).indent(0, SPACING).create());

        getShell().setDefaultButton(connectButton);
    }

    private Composite createSocketTabContent(final CTabFolder parent) {
        final var scrolled = new ScrolledComposite(parent, SWT.V_SCROLL);
        scrolled.setExpandHorizontal(true);
        scrolled.setExpandVertical(true);

        final var composite = new Composite(scrolled, SWT.NONE);
        composite.setLayout(GridLayoutFactory.swtDefaults().numColumns(2).equalWidth(false).margins(MARGIN, MARGIN)
                .spacing(SPACING, 8).create());

        // Connection Settings
        createSectionHeader(composite, "Connection Settings", 2);

        createFormLabel(composite, "Host:");
        hostField = createFormText(composite, "localhost");
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

        createFormLabel(composite, "Truststore Path:");
        final var truststoreComposite = new Composite(composite, SWT.NONE);
        truststoreComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        truststoreComposite
                .setLayout(GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false).spacing(5, 0).create());

        truststorePathField = new Text(truststoreComposite, SWT.BORDER);
        truststorePathField.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

        final var browseButton = new Button(truststoreComposite, SWT.PUSH);
        browseButton.setText("Browse...");
        browseButton.addListener(SWT.Selection, e -> browseTruststore());

        createFormLabel(composite, "Truststore Password:");
        truststorePasswordField = createFormPassword(composite);

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
        final var scrolled = new ScrolledComposite(parent, SWT.V_SCROLL);
        scrolled.setExpandHorizontal(true);
        scrolled.setExpandVertical(true);

        final var composite = new Composite(scrolled, SWT.NONE);
        composite.setLayout(GridLayoutFactory.swtDefaults().numColumns(2).equalWidth(false).margins(MARGIN, MARGIN)
                .spacing(SPACING, 8).create());

        // Broker Settings
        createSectionHeader(composite, "Broker Settings", 2);

        createFormLabel(composite, "Broker Server:");
        serverField = createFormText(composite, "broker.hivemq.com");
        addValidation(serverField, "Broker server is required");

        createFormLabel(composite, "Broker Port:");
        mqttPortSpinner = createFormSpinner(composite, 1, 65535, 1883);

        createFormLabel(composite, "Timeout (ms):");
        mqttTimeoutSpinner = createFormSpinner(composite, 1000, 60000, 10000);
        mqttTimeoutSpinner.setIncrement(1000);

        createFormLabel(composite, "Client ID:");
        clientIdField = createFormText(composite, "osgifx-client");
        addValidation(clientIdField, "Client ID is required");

        // Authentication
        createSectionHeader(composite, "Authentication", 2);

        createFormLabel(composite, "Username:");
        usernameField = createFormText(composite, "");

        createFormLabel(composite, "Password:");
        mqttPasswordField = createFormPassword(composite);

        // Topic Settings
        createSectionHeader(composite, "Topic Configuration", 2);

        createFormLabel(composite, "Publish Topic:");
        pubTopicField = createFormText(composite, "osgifx/pub");
        addValidation(pubTopicField, "Publish topic is required");

        createFormLabel(composite, "Subscribe Topic:");
        subTopicField = createFormText(composite, "osgifx/sub");
        addValidation(subTopicField, "Subscribe topic is required");

        createFormLabel(composite, "LWT Topic:");
        lwtTopicField = createFormText(composite, "osgifx/lwt");

        // OAuth2
        createSectionHeader(composite, "OAuth2 Token Configuration (Optional)", 2);

        createFormLabel(composite, "Auth Server URL:");
        authServerUrlField = createFormText(composite, "");

        createFormLabel(composite, "Client ID:");
        oauthClientIdField = createFormText(composite, "");

        createFormLabel(composite, "Client Secret:");
        clientSecretField = createFormPassword(composite);

        createFormLabel(composite, "Audience:");
        audienceField = createFormText(composite, "");

        createFormLabel(composite, "Scope:");
        scopeField = createFormText(composite, "");

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
        final var header = new Label(parent, SWT.NONE);
        header.setText(title);
        header.setFont(boldFont);
        header.setLayoutData(GridDataFactory.fillDefaults().span(columns, 1).grab(true, false).indent(0, 10).create());

        final var separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(GridDataFactory.fillDefaults().span(columns, 1).grab(true, false).create());
    }

    private void addValidation(final Text text, final String message) {
        final var decoration = new ControlDecoration(text, SWT.TOP | SWT.LEFT);
        decoration.setDescriptionText(message);
        decoration.setImage(
                FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
        decoration.hide();

        decorations.put(text, decoration);

        text.addModifyListener(e -> validateFields());
    }

    private void validateFields() {
        var isValid = true;

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
            final var control    = entry.getKey();
            final var decoration = entry.getValue();
            if (control instanceof Text) {
                if (((Text) control).getText().trim().isEmpty()) {
                    decoration.show();
                } else {
                    decoration.hide();
                }
            }
        }

        if (connectButton != null && !connectButton.isDisposed()) {
            connectButton.setEnabled(isValid);
        }
        final var okButton = getButton(OK);
        if (okButton != null && !okButton.isDisposed()) {
            okButton.setEnabled(isValid);
        }
    }

    private void updatePanelState() {
        final var isSelection = selectedProfile != null;
        setEnabledRecursive(rightPanel, isSelection);
        duplicateItem.setEnabled(isSelection);
        removeItem.setEnabled(isSelection);

        if (!isSelection) {
            decorations.values().forEach(ControlDecoration::hide);
            if (connectButton != null && !connectButton.isDisposed()) {
                connectButton.setEnabled(false);
            }
            final var okButton = getButton(OK);
            if (okButton != null && !okButton.isDisposed()) {
                okButton.setEnabled(false);
            }
        }
    }

    private void setEnabledRecursive(final Composite composite, final boolean enabled) {
        composite.setEnabled(enabled);
        for (final Control child : composite.getChildren()) {
            child.setEnabled(enabled);
            if (child instanceof Composite) {
                setEnabledRecursive((Composite) child, enabled);
            }
        }
    }

    private void createFormLabel(final Composite parent, final String text) {
        final var label = new Label(parent, SWT.NONE);
        label.setText(text);
        label.setLayoutData(
                GridDataFactory.swtDefaults().hint(LABEL_WIDTH, SWT.DEFAULT).align(SWT.END, SWT.CENTER).create());
    }

    private Text createFormText(final Composite parent, final String defaultValue) {
        final var text = new Text(parent, SWT.BORDER | SWT.SINGLE);
        text.setText(defaultValue);
        text.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).hint(200, SWT.DEFAULT).create());
        return text;
    }

    private Text createFormPassword(final Composite parent) {
        final var text = new Text(parent, SWT.BORDER | SWT.SINGLE | SWT.PASSWORD);
        text.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).hint(200, SWT.DEFAULT).create());
        return text;
    }

    private Spinner createFormSpinner(final Composite parent, final int min, final int max, final int value) {
        final var spinner = new Spinner(parent, SWT.BORDER);
        spinner.setMinimum(min);
        spinner.setMaximum(max);
        spinner.setSelection(value);
        spinner.setLayoutData(GridDataFactory.swtDefaults().align(SWT.BEGINNING, SWT.CENTER).create());
        return spinner;
    }

    private Button createConnectButton(final Composite parent) {
        final var button = new Button(parent, SWT.PUSH);
        button.setText("  \u25B6  Connect  ");
        button.setFont(boldFont);
        button.addListener(SWT.Selection, e -> connect());
        return button;
    }

    private void addProfile() {
        final var profile = new ConnectionProfile("New Connection", "SOCKET");
        profiles.add(profile);
        profileStore.add(profile);
        profileList.refresh();
        profileList.setSelection(new StructuredSelection(profile));
        updatePanelState();
        nameField.setFocus();
        nameField.selectAll();
    }

    private void removeProfile() {
        if (selectedProfile == null) {
            return;
        }
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
        if (selectedProfile == null) {
            return;
        }
        profileStore.duplicate(selectedProfile.id);
        profiles = profileStore.loadAll();
        profileList.setInput(profiles);
        profileList.refresh();
        updatePanelState();
    }

    private void browseTruststore() {
        final var dialog = new FileDialog(getShell(), SWT.OPEN);
        dialog.setFilterExtensions(new String[] { "*.jks", "*.p12", "*.*" });
        final var path = dialog.open();
        if (path != null) {
            truststorePathField.setText(path);
        }
    }

    private void connect() {
        saveCurrentProfile();

        if (selectedProfile == null) {
            return;
        }

        try {
            final var zuluDownloader = new AzulZuluDownloader();
            if (!zuluDownloader.isRuntimeAvailable()) {
                zuluDownloader.schedule();
                zuluDownloader.join();
            }

            final var scriptDownloader = new RunOsgiFxDownloader();
            if (!scriptDownloader.isScriptAvailable()) {
                scriptDownloader.download();
            }

            final Path configPath = configWriter.writeHeadlessConfig(selectedProfile);

            // Get preferences
            final var preferences = OsgifxWorkspaceUtil.getPreferenceStore();
            final var useLocal    = preferences.getBoolean(USE_LOCAL_JAR);
            final var localJar    = preferences.getString(OSGIFX_LOCAL_JAR);
            final var gav         = preferences.getString(OSGIFX_GAV);

            final var launcher = new OsgifxProcessLauncher(selectedProfile, configPath,
                                                           zuluDownloader.getJavaExecutablePath(),
                                                           scriptDownloader.getScriptPath(), gav,
                                                           useLocal ? localJar : null);

            launcher.schedule();

            selectedProfile.lastConnected = Instant.now().toString();
            selectedProfile.lastStatus    = "SUCCESS";
            profileStore.update(selectedProfile);

            close();
        } catch (final Exception e) {
            if (selectedProfile != null) {
                selectedProfile.lastStatus = "FAILURE";
                profileStore.update(selectedProfile);
            }
            org.eclipse.jface.dialogs.MessageDialog.openError(getShell(), "Connection Error",
                    "Failed to launch OSGi.fx: " + e.getMessage());
        }
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
            }
        }

        profileStore.update(selectedProfile);
    }

    private void clearFields() {
        nameField.setText("");
        hostField.setText("localhost");
        portSpinner.setSelection(4567);
        timeoutSpinner.setSelection(10000);
        passwordField.setText("");
        truststorePathField.setText("");
        truststorePasswordField.setText("");
        serverField.setText("broker.hivemq.com");
        mqttPortSpinner.setSelection(1883);
        mqttTimeoutSpinner.setSelection(10000);
        clientIdField.setText("osgifx-client");
        usernameField.setText("");
        mqttPasswordField.setText("");
        pubTopicField.setText("osgifx/pub");
        subTopicField.setText("osgifx/sub");
        lwtTopicField.setText("osgifx/lwt");
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
                final var instant   = Instant.parse(selectedProfile.lastConnected);
                final var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
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
            saveCurrentProfile();

            final var selection = (StructuredSelection) event.getSelection();
            selectedProfile = (ConnectionProfile) selection.getFirstElement();

            updatePanelState();

            if (selectedProfile != null) {
                populateFields(selectedProfile);
            } else {
                clearFields();
            }
        }

        private void populateFields(final ConnectionProfile profile) {
            nameField.setText(profile.name != null ? profile.name : "");

            if ("MQTT".equals(profile.type)) {
                tabFolder.setSelection(mqttTab);
                serverField.setText(profile.server != null ? profile.server : "broker.hivemq.com");
                mqttPortSpinner.setSelection(profile.mqttPort > 0 ? profile.mqttPort : 1883);
                mqttTimeoutSpinner.setSelection(profile.mqttTimeout > 0 ? profile.mqttTimeout : 10000);
                clientIdField.setText(profile.clientId != null ? profile.clientId : "osgifx-client");
                usernameField.setText(profile.username != null ? profile.username : "");
                mqttPasswordField.setText(profile.mqttPassword != null ? profile.mqttPassword : "");
                pubTopicField.setText(profile.pubTopic != null ? profile.pubTopic : "osgifx/pub");
                subTopicField.setText(profile.subTopic != null ? profile.subTopic : "osgifx/sub");
                lwtTopicField.setText(profile.lwtTopic != null ? profile.lwtTopic : "osgifx/lwt");

                if (profile.tokenConfig != null) {
                    authServerUrlField.setText(
                            profile.tokenConfig.authServerURL != null ? profile.tokenConfig.authServerURL : "");
                    oauthClientIdField
                            .setText(profile.tokenConfig.clientId != null ? profile.tokenConfig.clientId : "");
                    clientSecretField
                            .setText(profile.tokenConfig.clientSecret != null ? profile.tokenConfig.clientSecret : "");
                    audienceField.setText(profile.tokenConfig.audience != null ? profile.tokenConfig.audience : "");
                    scopeField.setText(profile.tokenConfig.scope != null ? profile.tokenConfig.scope : "");
                }
            } else {
                tabFolder.setSelection(socketTab);
                hostField.setText(profile.host != null ? profile.host : "localhost");
                portSpinner.setSelection(profile.port > 0 ? profile.port : 4567);
                timeoutSpinner.setSelection(profile.timeout > 0 ? profile.timeout : 10000);
                passwordField.setText(profile.password != null ? profile.password : "");
                truststorePathField.setText(profile.trustStorePath != null ? profile.trustStorePath : "");
                truststorePasswordField.setText(profile.trustStorePassword != null ? profile.trustStorePassword : "");
            }

            updateStatusDisplay();
            validateFields();
        }
    }

    private final class ConnectionProfileLabelProvider extends LabelProvider {
        @Override
        public String getText(final Object element) {
            if (element instanceof ConnectionProfile) {
                final var profile = (ConnectionProfile) element;
                final var icon    = getStatusIcon(profile.lastStatus);
                return icon + " " + profile.name;
            }
            return super.getText(element);
        }

        @Override
        public Image getImage(final Object element) {
            if (element instanceof ConnectionProfile) {
                final var profile    = (ConnectionProfile) element;
                final var type       = profile.type;
                final var iconPath   = "MQTT".equals(type) ? "icons/mqtt_connection.png"
                        : "icons/socket_connection.png";
                final var descriptor = AbstractUIPlugin.imageDescriptorFromPlugin("com.osgifx.eclipse.plugin",
                        iconPath);
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
    protected boolean isResizable() {
        return true;
    }
}
