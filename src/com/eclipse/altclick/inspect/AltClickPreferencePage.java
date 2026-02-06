package com.eclipse.altclick.inspect;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class AltClickPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
    private Button enabledButton;

    public AltClickPreferencePage() {
        setTitle("AltClick Inspect");
        setDescription("配置 Alt+悬停高亮 与 Alt+左键 Inspect 快捷能力。");
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));

        enabledButton = new Button(container, SWT.CHECK);
        enabledButton.setText("启用 AltClick Inspect（Alt+悬停/Alt+左键）");
        enabledButton.setSelection(FeatureTogglePreferences.isEnabled());

        return container;
    }

    @Override
    public boolean performOk() {
        if (enabledButton != null && !enabledButton.isDisposed()) {
            FeatureTogglePreferences.setEnabled(enabledButton.getSelection());
        }
        return true;
    }

    @Override
    protected void performDefaults() {
        if (enabledButton != null && !enabledButton.isDisposed()) {
            enabledButton.setSelection(true);
        }
        super.performDefaults();
    }
}
