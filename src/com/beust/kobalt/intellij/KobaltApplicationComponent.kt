package com.beust.kobalt.intellij

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.StatusBarProgress
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.util.concurrent.*

/**
 * Our main application component, which just sees if our kobalt.jar file needs to be downloaded.
 *
 * @author Cedric Beust <cedric@beust.com>
 * @since 10 23, 2015
 */
public class KobaltApplicationComponent : ApplicationComponent {
    override fun getComponentName() = "kobalt.ApplicationComponent"

    companion object {
        val latestKobaltVersion: Future<String>
            get() {
                val callable = Callable<String> {
                    var result = "0"
                    try {
                        val ins = URL(DistributionDownloader.RELEASE_URL).openConnection().inputStream
                        @Suppress("UNCHECKED_CAST")
                        val reader = BufferedReader(InputStreamReader(ins))
                        val jo = JsonParser().parse(reader) as JsonArray
                        if (jo.size() > 0) {
                            var versionName = (jo.get(0) as JsonObject).get("name").asString
                            if (versionName == null || versionName.isBlank()) {
                                versionName = (jo.get(0) as JsonObject).get("tag_name").asString
                            }
                            if (versionName != null) {
                                result = versionName
                            }
                        }
                    } catch(ex: IOException) {
                        DistributionDownloader.warn("Couldn't load the release URL: ${DistributionDownloader.RELEASE_URL}")
                    }
                    result
                }
                return Executors.newFixedThreadPool(1).submit(callable)
            }

        val version: String by lazy {
            try {
                latestKobaltVersion.get(2, TimeUnit.SECONDS)
            } catch(ex: TimeoutException) {
                Constants.MIN_KOBALT_VERSION
            }

        }
    }

    override fun initComponent() {
        val provider = ServiceManager.getService(KobaltSettingsProvider::class.java)
        var progress = StatusBarProgress().apply {
            start()
            text = "Downloading Kobalt $version"
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            ProgressManager.getInstance().runProcess( {
                DistributionDownloader().install(version!!, progress)
            }, progress)
        }
    }

    override fun disposeComponent() {
    }

}
