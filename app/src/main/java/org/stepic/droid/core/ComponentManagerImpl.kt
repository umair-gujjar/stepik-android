package org.stepic.droid.core

import org.stepic.droid.di.AppCoreComponent
import org.stepic.droid.di.login.LoginComponent
import org.stepic.droid.di.mainscreen.MainScreenComponent
import org.stepic.droid.di.routing.RoutingComponent

class ComponentManagerImpl(private val appCoreComponent: AppCoreComponent) : ComponentManager {

    private val loginComponentMap = HashMap<String, LoginComponent>()

    override fun releaseLoginComponent(tag: String) {
        loginComponentMap.remove(tag)
    }

    override fun loginComponent(tag: String) =
            loginComponentMap.getOrPut(tag) {
                appCoreComponent
                        .loginComponentBuilder()
                        .build()
            }

    private var mainScreenComponentProp: MainScreenComponent? = null

    override fun mainFeedComponent(): MainScreenComponent {
        synchronized(this) {
            if (mainScreenComponentProp == null) {
                mainScreenComponentProp = appCoreComponent
                        .mainScreenComponentBuilder()
                        .build()
            }
            return mainScreenComponentProp!!
        }
    }

    override fun releaseMainFeedComponent() {
        synchronized(this) {
            mainScreenComponentProp = null
        }
    }


    private var routingRefCount = 0
    private var routingComponent: RoutingComponent? = null

    override fun routingComponent(): RoutingComponent {
        if (routingComponent == null) {
            routingComponent = appCoreComponent
                    .routingComponentBuilder()
                    .build()
        }

        routingRefCount++
        return routingComponent!!
    }

    override fun releaseRoutingComponent() {
        routingRefCount--
        if (routingRefCount == 0) {
            routingComponent = null
        }
        if (routingRefCount < 0) {
            throw IllegalStateException("released routing component greater than got")
        }
    }
}
