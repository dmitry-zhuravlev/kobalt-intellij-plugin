package com.beust.kobalt.intellij

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros

@State(
        name = "KobaltSettingsProvider",
        storages = arrayOf(Storage(file = "${StoragePathMacros.APP_CONFIG}/kobalt.xml"))
)
class KobaltSettingsProvider : PersistentStateComponent<KobaltSettingsProvider.State> {

    class State {
        public var kobaltVersion: String = Constants.MIN_KOBALT_VERSION
        public var kobaltServerPort: Int = Constants.DEFAULT_SERVER_PORT
    }

    var kobaltState: State = State();

    override fun loadState(state: KobaltSettingsProvider.State?) {
        if(state != null) {
            kobaltState.kobaltServerPort = state.kobaltServerPort
            kobaltState.kobaltVersion = state.kobaltVersion
        }
    }

    override fun getState(): KobaltSettingsProvider.State? = kobaltState

//    fun getInstance() = ServiceManager.getService(KobaltSettingsProvider::class.java)
}