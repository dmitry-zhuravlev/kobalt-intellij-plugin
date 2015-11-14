package com.beust.kobalt.intellij

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.options.Configurable
import java.util.*
import javax.swing.JComponent
import javax.swing.JSpinner
import javax.swing.JTextField
import javax.swing.SpinnerNumberModel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class KobaltSettings(var provider: KobaltSettingsProvider) : Configurable {

    val bundle = ResourceBundle.getBundle("KobaltSettingsKt");

    var form: KobaltSettingsForm = KobaltSettingsForm()
    var kobaltVersion: JTextField = form.kobaltVersion
    var serverPort: JSpinner = form.kobaltServerPort

    var stateChanged: Boolean = false

    override fun isModified(): Boolean {
        return stateChanged
    }

    override fun apply() {
        provider.kobaltState.kobaltVersion = kobaltVersion.text
        provider.kobaltState.kobaltServerPort = serverPort.value as Int
        ServiceManager.getService(KobaltProjectComponent::class.java)?.syncBuildFile()
        stateChanged = false

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

        serverPort.apply {
            model = SpinnerNumberModel(provider.kobaltState.kobaltServerPort, Constants.MIN_SERVER_PORT, Constants.MAX_SERVER_PORT, 1)
            editor = JSpinner.NumberEditor(serverPort, "#####")
            addChangeListener { event -> stateChanged = true}
        }

        return form.myPanel
    }

    override fun reset() {
        kobaltVersion.text = provider.kobaltState.kobaltVersion
        serverPort.value = provider.kobaltState.kobaltServerPort
        stateChanged = false
    }

    override fun disposeUIResources() {
    }

    override fun getDisplayName(): String? = bundle.getString("settings.kobalt.name")

    override fun getHelpTopic(): String? = bundle.getString("settings.kobalt.name")
}