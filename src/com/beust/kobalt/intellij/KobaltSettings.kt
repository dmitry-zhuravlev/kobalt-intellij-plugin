package com.beust.kobalt.intellij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.ProgressWindow
import com.intellij.openapi.ui.Messages
import com.intellij.util.ui.UIUtil
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import javax.swing.JComponent
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.text.Regex

class KobaltSettings(var provider: KobaltSettingsProvider) : Configurable {
    companion object {
        val logger = Logger.getInstance(KobaltSettings::class.java)
    }

    val bundle = ResourceBundle.getBundle("KobaltSettingsKt");

    var form = KobaltSettingsForm()
    var kobaltVersion = form.kobaltVersion
    var serverPort = form.kobaltServerPort
    var validateButton = form.validateButton
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
        kobaltVersion.apply {
            validateButton.addActionListener { event ->
                val progress = ProgressWindow(true, false, null, form.myPanel, null)
                progress.title = "Checking Kobalt version: ${kobaltVersion.text}"

                ApplicationManager.getApplication().executeOnPooledThread {
                    ProgressManager.getInstance().runProcess({

                        val validVersion = validateVersion(kobaltVersion.text)

                        progress.stop()

                        if (validVersion.not()) {
                            if(progress.isCanceled.not()) {
                                UIUtil.invokeLaterIfNeeded {
                                    Messages.showErrorDialog(kobaltVersion, "Unavailable version (${kobaltVersion.text})")
                                }
                            }
                        }
                    }, progress)
                }

            }
            text = provider.kobaltState.kobaltVersion
            addFocusListener(object : FocusListener {
                override fun focusGained(e: FocusEvent?) {}

                override fun focusLost(e: FocusEvent?) {
                    if(text.matches(Regex("[0-9.]+(?:-SNAPSHOT)?$")).not()) {
                        Messages.showErrorDialog(kobaltVersion, "Invalid version format: d.ddd(-SNAPSHOT)")
                    }
                }
            })
            document.addDocumentListener(object : DocumentListener {
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
        }

        serverPort.apply {
            model = SpinnerNumberModel(provider.kobaltState.kobaltServerPort, Constants.MIN_SERVER_PORT, Constants.MAX_SERVER_PORT, 1)
            editor = JSpinner.NumberEditor(serverPort, "#####")

            if (Constants.DEV_MODE.not()) {
                isEnabled = false
            } else {
                addChangeListener { event -> stateChanged = true }
            }
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

    override fun getHelpTopic(): String? = null

    fun validateVersion(version: String): Boolean {
            var fileUrl = "http://beust.com/kobalt/kobalt-$version.zip"

            var done = false
            var httpConn: HttpURLConnection? = null
            var responseCode = 0
            var cookies: String? = null
            while (!done) {
                logger.warn("loc: ${fileUrl}}")
                httpConn = URL(fileUrl).openConnection() as HttpURLConnection
                httpConn.instanceFollowRedirects = true
                if(cookies != null){
                    httpConn.setRequestProperty("Cookie", cookies);
                }
                responseCode = httpConn.responseCode
                logger.warn("resp code: ${responseCode}")
                logger.warn("--------------")
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                        || responseCode == HttpURLConnection.HTTP_MOVED_PERM
                        || responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                    fileUrl = httpConn.getHeaderField("Location")
                    httpConn.disconnect()
                } else {
                    done = true
                }
            }
            if (responseCode == HttpURLConnection.HTTP_OK && httpConn != null) {
                httpConn.disconnect();
                return true
            }
        return false
    }
}
