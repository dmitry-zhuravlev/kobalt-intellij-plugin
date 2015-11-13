package com.beust.kobalt.intellij

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.options.Configurable
import java.util.*
import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class KobaltSettings(var provider: KobaltSettingsProvider) : Configurable {

    val bundle = ResourceBundle.getBundle("KobaltSettingsKt");

    var form: KobaltSettingsForm = KobaltSettingsForm()
    var kobaltVersion: JTextField = form.kobaltVersion

    var stateChanged: Boolean = false

    override fun isModified(): Boolean {
        return stateChanged
    }

    override fun apply() {
        provider.kobaltState.kobaltVersion = kobaltVersion.text
        stateChanged = false
        ServiceManager.getService(KobaltProjectComponent::class.java)?.syncBuildFile()
    }

    override fun createComponent(): JComponent? {
        kobaltVersion.text = provider.kobaltState.kobaltVersion
        kobaltVersion.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent?) {
                stateChanged = true
            }

            override fun insertUpdate(e: DocumentEvent?) {
                changedUpdate(e)
            }

            override fun removeUpdate(e: DocumentEvent?) {
                changedUpdate(e)
            }
        })

        return form.myPanel
    }

    override fun reset() {
        kobaltVersion.text = provider.kobaltState.kobaltVersion
        stateChanged = false
    }

    override fun disposeUIResources() {
    }

    override fun getDisplayName(): String? = bundle.getString("settings.kobalt.name")

    override fun getHelpTopic(): String? = bundle.getString("settings.kobalt.name")
}