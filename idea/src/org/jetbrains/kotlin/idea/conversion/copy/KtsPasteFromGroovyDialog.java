/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.conversion.copy;

import com.intellij.CommonBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.editor.KotlinEditorOptions;

import javax.swing.*;
import java.awt.*;


@SuppressWarnings("UnusedDeclaration")
public class KtsPasteFromGroovyDialog extends DialogWrapper {
    private JPanel panel;
    private JCheckBox donTShowThisCheckBox;
    private JLabel questionLabel;
    private JButton buttonOK;

    public KtsPasteFromGroovyDialog(@NotNull Project project, boolean isPlainText) {
        super(project, true);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setTitle("Convert Code From Groovy");
        if (isPlainText) {
            questionLabel.setText("Clipboard content seems to be Groovy code. Do you want to convert it to Kts? ");
            //TODO: should we also use different set of settings?
        }
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        return panel;
    }

    @Override
    public Container getContentPane() {
        return panel;
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        setOKButtonText(CommonBundle.getYesButtonText());
        setCancelButtonText(CommonBundle.getNoButtonText());
        return new Action[] {getOKAction(), getCancelAction()};
    }

    @Override
    protected void doOKAction() {
        if (donTShowThisCheckBox.isSelected()) {
            KotlinEditorOptions.getInstance().setDonTShowConversionDialogKts2(true);
        }
        super.doOKAction();
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
