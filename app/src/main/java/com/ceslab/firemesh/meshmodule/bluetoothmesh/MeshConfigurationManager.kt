package com.ceslab.firemesh.meshmodule.bluetoothmesh

import com.ceslab.firemesh.meshmodule.listener.ConfigurationStatusListener
import com.ceslab.firemesh.meshmodule.listener.NodeFeatureListener
import com.ceslab.firemesh.meshmodule.model.ConfigurationStatus
import com.ceslab.firemesh.meshmodule.model.MeshNode
import com.ceslab.firemesh.meshmodule.model.NodeFunctionality
import com.siliconlab.bluetoothmesh.adk.ErrorType
import com.siliconlab.bluetoothmesh.adk.configuration_control.CheckNodeBehaviourCallback
import com.siliconlab.bluetoothmesh.adk.configuration_control.ConfigurationControl
import com.siliconlab.bluetoothmesh.adk.configuration_control.SetNodeBehaviourCallback
import com.siliconlab.bluetoothmesh.adk.data_model.group.Group
import com.siliconlab.bluetoothmesh.adk.data_model.model.Model
import com.siliconlab.bluetoothmesh.adk.data_model.model.VendorModel
import com.siliconlab.bluetoothmesh.adk.data_model.node.Node
import com.siliconlab.bluetoothmesh.adk.functionality_binder.FunctionalityBinder
import com.siliconlab.bluetoothmesh.adk.functionality_binder.FunctionalityBinderCallback
import com.siliconlab.bluetoothmesh.adk.node_control.NodeControl
import com.siliconlab.bluetoothmesh.adk.node_control.NodeControlCallback
import com.siliconlab.bluetoothmesh.adk.notification_control.PublicationSettingsGenericCallback
import com.siliconlab.bluetoothmesh.adk.notification_control.SubscriptionControl
import com.siliconlab.bluetoothmesh.adk.notification_control.SubscriptionSettingsGenericCallback
import com.siliconlab.bluetoothmesh.adk.notification_control.settings.PublicationSettings
import com.siliconlab.bluetoothmesh.adk.notification_control.settings.SubscriptionSettings
import timber.log.Timber
import java.util.concurrent.Executors

class MeshConfigurationManager(
    private val meshNodeToConfigure: MeshNode?
) {
    private val nodeFeatureListeners: ArrayList<NodeFeatureListener> = ArrayList()
    private val configurationStatusListeners: ArrayList<ConfigurationStatusListener> = ArrayList()

    private val configurationControl: ConfigurationControl =
        ConfigurationControl(meshNodeToConfigure!!.node)
    private val nodeControl: NodeControl =
        NodeControl((meshNodeToConfigure!!.node))

    private val taskExecutor = Executors.newSingleThreadScheduledExecutor()
    private val taskList = mutableListOf<Runnable>()
    private var taskCount = 0
    private var currentTask = Runnable { }

    fun addNodeFeatureListener(nodeFeatureListener: NodeFeatureListener) {
        synchronized(nodeFeatureListeners) {
            nodeFeatureListeners.add(nodeFeatureListener)
        }
    }

    fun removeNodeFeatureListener(nodeFeatureListener: NodeFeatureListener) {
        synchronized(nodeFeatureListeners) {
            nodeFeatureListeners.remove(nodeFeatureListener)
        }
    }

    fun addConfigurationStatusListener(configurationStatusListener: ConfigurationStatusListener) {
        synchronized(configurationStatusListeners) {
            configurationStatusListeners.add(configurationStatusListener)
        }
    }

    fun removeConfigurationStatusListener(configurationStatusListener: ConfigurationStatusListener) {
        synchronized(configurationStatusListeners) {
            configurationStatusListeners.remove(configurationStatusListener)
        }
    }

    fun processChangeGroup(newGroup: Group?) {
        Timber.d("processChangeGroup: $newGroup")
        if (meshNodeToConfigure!!.node.groups.isNotEmpty()) {
            val oldGroup = meshNodeToConfigure.node.groups.iterator().next()
            taskList.addAll(
                startUnbindModelFromGroupAndClearPublicationSettings(
                    oldGroup,
                    meshNodeToConfigure.functionality
                )
            )
            taskList.add(unbindNodeFromGroup(oldGroup))
        }
        if (newGroup != null) {
            taskList.add(bindNodeToGroup(newGroup))
        }
        startTasks()
    }

    fun processChangeFunctionality(newFunctionality: NodeFunctionality.VENDOR_FUNCTIONALITY) {
        Timber.d("processChangeFunctionality: $newFunctionality")
        val group =
            meshNodeToConfigure!!.node.groups.first()
        group?.let {
            taskList.addAll(
                startBindModelToGroupAndSetPublicationSettings(
                    it,
                    newFunctionality
                )
            )
        }
        startTasks()
    }


    private fun startTasks() {
        Timber.d("startTasks")
        taskCount = taskList.size
        takeNextTask()
    }

    private fun clearTasks() {
        Timber.d("clearTasks")
        taskList.clear()
    }

    private fun takeNextTask() {
        Timber.d("takeNextTask: ${taskList.isEmpty()}")
        if (taskList.isNotEmpty()) {
            currentTask = taskList.first()
            taskList.remove(currentTask)
            taskExecutor.execute(currentTask)
        } else {
            configurationStatusListeners.forEach { listener ->
                listener.onConfigurationStatusChanged(
                    ConfigurationStatus.NODE_CONFIG_FINISHED
                )
            }
        }
    }

    private fun bindNodeToGroup(group: Group): Runnable {
        Timber.d("bindNodeToGroup")
        configurationStatusListeners.forEach { listener ->
            listener.onConfigurationStatusChanged(
                ConfigurationStatus.BIND_NODE_TO_GROUP
            )
        }
        return Runnable {
            nodeControl.bind(group, NodeControlCallbackImpl())
        }
    }

    private fun unbindNodeFromGroup(group: Group): Runnable {
        Timber.d("unbindNodeToGroup")
        configurationStatusListeners.forEach { listener ->
            listener.onConfigurationStatusChanged(
                ConfigurationStatus.UNBIND_NODE_FROM_GROUP
            )
        }
        return Runnable {
            nodeControl.unbind(group, NodeControlCallbackImpl())
        }

    }

    private fun startBindModelToGroupAndSetPublicationSettings(
        group: Group,
        functionality: NodeFunctionality.VENDOR_FUNCTIONALITY
    ): List<Runnable> {
        Timber.d("startBindModelToGroupAndSetPublicationSettings")
        val taskList = mutableListOf<Runnable>()

        NodeFunctionality.getVendorModels(
            meshNodeToConfigure!!.node,
            functionality
        ).forEach { model ->
            Timber.d("getVendorModels")
            taskList.add(0, bindModelToGroup(model, group))
            taskList.add(1, setPublicationSettings(model, group))
            taskList.add(2, addSubscriptionSettings(model, group))
        }
        return taskList
    }

    private fun startUnbindModelFromGroupAndClearPublicationSettings(
        group: Group,
        functionality: NodeFunctionality.VENDOR_FUNCTIONALITY
    ): List<Runnable> {
        Timber.d("startUnbindModelFromGroupAndClearPublicationSettings")
        val taskList = mutableListOf<Runnable>()
        NodeFunctionality.getVendorModels(
            meshNodeToConfigure!!.node,
            functionality
        ).forEach { model ->
            Timber.d("getVendorModels")
            taskList.add(0, unbindModelFromGroup(model, group))
            taskList.add(1, clearPublicationSettings(model))
            taskList.add(2, removeSubscriptionSettings(model, group))
        }
        return taskList
    }


    private fun bindModelToGroup(vendorModel: VendorModel, group: Group): Runnable {
        Timber.d("bindModelToGroup")
        configurationStatusListeners.forEach { listener ->
            listener.onConfigurationStatusChanged(
                ConfigurationStatus.BIND_MODEL_TO_GROUP
            )
        }
        return Runnable {
            val functionalityBinder = FunctionalityBinder(group)
            functionalityBinder.bindModel(vendorModel, FunctionalityBinderCallbackImpl())
        }
    }

    private fun unbindModelFromGroup(vendorModel: VendorModel, group: Group): Runnable {
        Timber.d("unbindModelFromGroup")
        configurationStatusListeners.forEach { listener ->
            listener.onConfigurationStatusChanged(
                ConfigurationStatus.UNBIND_MODEL_FROM_GROUP
            )
        }
        return Runnable {
            val functionalityBinder = FunctionalityBinder(group)
            functionalityBinder.unbindModel(vendorModel, FunctionalityBinderCallbackImpl())
        }
    }

    private fun setPublicationSettings(model: VendorModel, group: Group): Runnable {
        Timber.d("setPublicationSettings")
        configurationStatusListeners.forEach { listener ->
            listener.onConfigurationStatusChanged(
                ConfigurationStatus.SET_PUBLICATION_SETTING
            )
        }
        return Runnable {
            val subscriptionControl = SubscriptionControl(model)
            val publicationSettings = PublicationSettings(group)
            subscriptionControl.setPublicationSettings(
                publicationSettings,
                PublicationSettingsGenericCallbackImpl()
            )
        }
    }

    private fun clearPublicationSettings(model: VendorModel): Runnable {
        Timber.d("clearPublicationSettings")
        configurationStatusListeners.forEach { listener ->
            listener.onConfigurationStatusChanged(
                ConfigurationStatus.CLEAR_PUBLICATION_SETTING
            )
        }
        return Runnable {
            val subscriptionControl = SubscriptionControl(model)
            subscriptionControl.clearPublicationSettings(PublicationSettingsGenericCallbackImpl())
        }
    }


    private fun addSubscriptionSettings(model: VendorModel, group: Group): Runnable {
        Timber.d("addSubscriptionSettings")
        configurationStatusListeners.forEach { listener ->
            listener.onConfigurationStatusChanged(
                ConfigurationStatus.ADD_SUBSCRIPTION_SETTING
            )
        }
        return Runnable {
            val subscriptionControl = SubscriptionControl(model)
            val subscriptionSettings = SubscriptionSettings(group)
            subscriptionControl.addSubscriptionSettings(
                subscriptionSettings,
                SubscriptionSettingsGenericCallbackImpl()
            )
        }
    }

    private fun removeSubscriptionSettings(model: VendorModel, group: Group): Runnable {
        Timber.d("removeSubscriptionSettings")
        configurationStatusListeners.forEach { listener ->
            listener.onConfigurationStatusChanged(
                ConfigurationStatus.REMOVE_SUBSCRIPTION_SETTING
            )
        }
        return Runnable {
            val subscriptionSettings = SubscriptionSettings(group)
            val subscriptionControl = SubscriptionControl(model)

            subscriptionControl.removeSubscriptionSettings(
                subscriptionSettings,
                SubscriptionSettingsGenericCallbackImpl()
            )
        }
    }

    fun checkProxyStatus() {
        Timber.d("checkProxyStatus")
        configurationControl.checkProxyStatus(object : CheckNodeBehaviourCallbackImpl() {
            override fun success(node: Node?, enabled: Boolean) {
                super.success(node, enabled)
                nodeFeatureListeners.forEach { listener -> listener.onProxyStatusChanged(enabled) }
            }
        })
    }

    fun checkRelayStatus() {
        Timber.d("checkRelayStatus")
        configurationControl.checkRelayStatus(object : CheckNodeBehaviourCallbackImpl() {

            override fun success(node: Node?, enabled: Boolean) {
                super.success(node, enabled)
                nodeFeatureListeners.forEach { listener -> listener.onRelayStatusChanged(enabled) }
            }

        })
    }

    fun checkFriendStatus() {
        Timber.d("checkFriendStatus")
        configurationControl.checkFriendStatus(object : CheckNodeBehaviourCallbackImpl() {

            override fun success(node: Node?, enabled: Boolean) {
                super.success(node, enabled)
                nodeFeatureListeners.forEach { listener -> listener.onFriendStatusChanged(enabled) }
            }
        })
    }

    fun changeProxy(enabled: Boolean) {
        Timber.d("changeProxy: $enabled")
        configurationControl.setProxy(enabled, object : SetNodeBehaviourCallbackImpl() {
            override fun success(node: Node?, enabled: Boolean) {
                Timber.d("changeProxy success: $enabled")
                nodeFeatureListeners.forEach { listener -> listener.onProxyStatusChanged(enabled) }
                takeNextTask()
            }
        })
    }

    fun changeRelay(enabled: Boolean) {
        Timber.d("changeRelay: $enabled")
        configurationControl.setRelay(enabled, 3, 20, object : SetNodeBehaviourCallbackImpl() {
            override fun success(node: Node?, enabled: Boolean) {
                Timber.d("changeRelay success: $enabled")
                nodeFeatureListeners.forEach { listener -> listener.onRelayStatusChanged(enabled) }
                takeNextTask()
            }
        })
    }

    fun changeFriend(enabled: Boolean) {
        Timber.d("changeFriend: $enabled")
        configurationControl.setFriend(enabled, object : SetNodeBehaviourCallbackImpl() {
            override fun success(node: Node?, enabled: Boolean) {
                Timber.d("changeFriend success: $enabled")
                nodeFeatureListeners.forEach { listener -> listener.onFriendStatusChanged(enabled) }
                takeNextTask()
            }
        })
    }


    abstract inner class CheckNodeBehaviourCallbackImpl : CheckNodeBehaviourCallback {
        override fun success(node: Node?, enabled: Boolean) {
            Timber.d("checkNodeBehaviorCallBack succeed: $enabled")
            takeNextTask()
        }

        override fun error(node: Node?, error: ErrorType?) {
            Timber.e("checkNodeBehaviorCallBack error: $error")
            clearTasks()
        }
    }

    inner class NodeControlCallbackImpl : NodeControlCallback {
        override fun succeed() {
            Timber.d("nodeControlCallback succeed")
            takeNextTask()
        }

        override fun error(errorType: ErrorType?) {
            Timber.e("nodeControlCallback error: $errorType")

        }
    }

    inner class FunctionalityBinderCallbackImpl : FunctionalityBinderCallback {
        override fun succeed(succeededModels: MutableList<Model>?, group: Group?) {
            Timber.d("modelBinder succeed")
            takeNextTask()
        }

        override fun error(
            failedModels: MutableList<Model>?,
            group: Group?,
            errorType: ErrorType?
        ) {
            Timber.e("modelBinder error: $errorType")

        }
    }

    inner class PublicationSettingsGenericCallbackImpl : PublicationSettingsGenericCallback {
        override fun success(meshModel: Model?, publicationSettings: PublicationSettings?) {
            Timber.d("publicationSetting succeed")
            takeNextTask()
        }

        override fun error(meshModel: Model?, errorType: ErrorType?) {
            Timber.e("publicationSetting error: $errorType")

        }
    }

    inner class SubscriptionSettingsGenericCallbackImpl : SubscriptionSettingsGenericCallback {
        override fun success(meshModel: Model?, subscriptionSettings: SubscriptionSettings?) {
            Timber.d("subscriptionSetting succeed")
            takeNextTask()
        }

        override fun error(meshModel: Model?, errorType: ErrorType?) {
            Timber.e("subscriptionSetting error: $errorType")

        }
    }

    abstract inner class SetNodeBehaviourCallbackImpl : SetNodeBehaviourCallback {
        override fun error(node: Node?, error: ErrorType?) {
        }
    }
}